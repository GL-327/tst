package com.jjkcraft.modulo.mission;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

import com.jjkcraft.JjkCraft;
import com.jjkcraft.config.JjkConfig;
import com.jjkcraft.core.RateLimiter;
import com.jjkcraft.curse.spawn.CurseSpawner;
import com.jjkcraft.modulo.Modulo;
import com.jjkcraft.modulo.ModuloPayloads;
import com.jjkcraft.modulo.ModuloProgress;
import com.jjkcraft.modulo.MulEnergy;
import com.jjkcraft.modulo.NaunaxDescent;
import com.jjkcraft.modulo.data.MissionDefinition;
import com.jjkcraft.modulo.data.MissionStage;
import com.jjkcraft.modulo.data.ModuloRegistries;
import com.jjkcraft.modulo.data.SiteDefinition;
import com.jjkcraft.modulo.entity.ModuloBossEntity;
import com.jjkcraft.modulo.entity.ModuloEntities;
import com.jjkcraft.modulo.entity.ModuloNpcEntity;
import com.jjkcraft.modulo.world.SiteBuilder;
import com.jjkcraft.sorcerer.Progression;

/**
 * Runs the Modulo missions (docs/MODULO.md 3.7): one chain per player, stage by stage, each stage a
 * checkpoint. Sites are built and peopled when someone on one of their stages comes near; bosses
 * fight everyone on the same stage at the site. All decisions are made here, on the server.
 */
public final class Missions {
	/** The mission and stage a player is on. */
	public record Current(Identifier id, MissionDefinition definition, int stageIndex, MissionStage stage) {
	}

	/** Tag on entities a stage's {@code spawns} brought, so they can be counted and cleared. */
	public static final String SPAWN_TAG = "jjkcraft_modulo_spawn";
	private static final int TICK_STEP = 10;
	private static final double NEAR = 40;
	private static final Map<UUID, String> INTRO_SHOWN = new HashMap<>();
	private static final RateLimiter CHOOSE_LIMIT = new RateLimiter(2, 2);

	private Missions() {
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(Missions::tick);
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> checkpoint(handler.getPlayer()));
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			INTRO_SHOWN.remove(handler.getPlayer().getUUID());
			RateLimiter.forgetEverywhere(handler.getPlayer().getUUID());
		});
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			if (!alive) {
				checkpoint(newPlayer);
			}
		});
	}

	/** Back to the start of the current stage, after a death or a relog. */
	private static void checkpoint(ServerPlayer player) {
		ModuloProgress progress = Modulo.progress(player);
		if (progress.counter() != 0) {
			Modulo.setProgress(player, progress.atCheckpoint());
		}

		INTRO_SHOWN.remove(player.getUUID());
	}

	// ---------------------------------------------------------------- where a player stands

	public static Optional<Current> current(ServerPlayer player) {
		if (!Modulo.enabled()) {
			return Optional.empty();
		}

		ModuloProgress progress = Modulo.progress(player);
		if (!progress.boarded() && !NaunaxDescent.eligible(player)) {
			return Optional.empty();
		}

		List<ModuloRegistries.Mission> missions = ModuloRegistries.missions(player.registryAccess());
		if (progress.mission().isPresent() && !progress.hasCompleted(progress.mission().get())) {
			for (ModuloRegistries.Mission mission : missions) {
				if (mission.id().equals(progress.mission().get()) && progress.stage() < mission.definition().stages().size()) {
					return Optional.of(new Current(mission.id(), mission.definition(), progress.stage(), mission.definition().stages().get(progress.stage())));
				}
			}
		}

		for (ModuloRegistries.Mission mission : missions) {
			if (!progress.hasCompleted(mission.id())) {
				return Optional.of(new Current(mission.id(), mission.definition(), 0, mission.definition().stages().getFirst()));
			}
		}

		return Optional.empty();
	}

	public static boolean finishedAll(ServerPlayer player) {
		List<ModuloRegistries.Mission> missions = ModuloRegistries.missions(player.registryAccess());
		ModuloProgress progress = Modulo.progress(player);
		return !missions.isEmpty() && missions.stream().allMatch(m -> progress.hasCompleted(m.id()));
	}

	/** Whether the player's current stage is {@code stage} of {@code mission}. */
	public static boolean onStage(ServerPlayer player, Identifier mission, int stage) {
		return current(player).filter(c -> c.id().equals(mission) && c.stageIndex() == stage).isPresent();
	}

	/** Numbers shown in the objective: progress counts, seconds left. */
	public static List<String> objectiveArgs(ServerPlayer player, ModuloProgress progress, MissionStage stage) {
		return switch (stage) {
			case MissionStage.Defend defend -> List.of(Integer.toString(Math.max(0, defend.seconds() - progress.counter() / 20)));
			case MissionStage.MakeMulEnergy make -> List.of(Integer.toString(progress.counter()), Integer.toString((int) make.amount()));
			case MissionStage.Deliver deliver -> List.of(Integer.toString(Math.min(deliver.count(), count(player, deliver.item()))), Integer.toString(deliver.count()));
			case MissionStage.Talk talk -> List.of(Integer.toString(progress.counter()), Integer.toString(talk.lines().size()));
			default -> List.of();
		};
	}

	private static int count(ServerPlayer player, Identifier itemId) {
		Item item = BuiltInRegistries.ITEM.getValue(itemId);
		int total = 0;
		for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
			if (stack.is(item)) {
				total += stack.getCount();
			}
		}

		return total;
	}

	private static Optional<SiteDefinition> site(MinecraftServer server, MissionStage stage) {
		return stage.base().site().flatMap(id -> ModuloRegistries.site(server.registryAccess(), id));
	}

	private static boolean atSite(ServerPlayer player, SiteDefinition site, double extra) {
		return player.level().dimension() == site.dimension() && site.contains(player.blockPosition(), extra);
	}

	// ---------------------------------------------------------------- the clock

	private static void tick(MinecraftServer server) {
		if (server.getTickCount() % TICK_STEP != 0 || !Modulo.enabled()) {
			return;
		}

		update(server, List.copyOf(server.getPlayerList().getPlayers()));
		if (server.getTickCount() % 200 == 0) {
			tidy(server);
		}
	}

	/**
	 * One step of the mission clock (every {@value #TICK_STEP} ticks) for these players: stage
	 * checks, sites, spawns, bosses, companions. Public so GameTests can drive it for fake players.
	 */
	public static void update(MinecraftServer server, List<ServerPlayer> players) {
		Map<Identifier, List<ServerPlayer>> bySite = new HashMap<>();
		for (ServerPlayer player : players) {
			companion(player);
			Optional<Current> found = current(player);
			if (found.isEmpty()) {
				continue;
			}

			Current current = found.get();
			ModuloProgress progress = Modulo.progress(player);
			if (progress.mission().isEmpty() || !progress.mission().get().equals(current.id())) {
				Modulo.setProgress(player, progress.starting(current.id()));
				progress = Modulo.progress(player);
			}

			MissionStage stage = current.stage();
			if (stage.base().when().isPresent() && !stage.base().when().get().matches(progress.flags())) {
				complete(player, current, false);
				continue;
			}

			intro(player, current);
			Optional<SiteDefinition> site = site(server, stage);
			if (site.isPresent() && atSite(player, site.get(), NEAR)) {
				bySite.computeIfAbsent(stage.base().site().get(), k -> new ArrayList<>()).add(player);
			}

			switch (stage) {
				case MissionStage.Travel travel -> {
					if (site.isPresent() && player.level().dimension() == site.get().dimension()) {
						BlockPos target = travel.marker().map(site.get()::marker).orElse(site.get().center());
						if (player.blockPosition().distSqr(target) <= travel.radius() * travel.radius()) {
							complete(player, current, true);
						}
					}
				}
				case MissionStage.Defend defend -> {
					if (site.isPresent() && atSite(player, site.get(), 8)) {
						Modulo.setProgress(player, progress.withCounter(progress.counter() + TICK_STEP));
						if (defend.boss().isEmpty() && progress.counter() + TICK_STEP >= defend.seconds() * 20) {
							complete(player, current, true);
						}
					}
				}
				case MissionStage.MakeMulEnergy make -> {
					if (progress.counter() >= make.amount()) {
						complete(player, current, true);
					}
				}
				case MissionStage.Sequence sequence -> {
					if (progress.counter() == 0 && JjkConfig.server().modulo.epilogue) {
						Modulo.send(player, new ModuloPayloads.Event("sequence", List.of(sequence.sequence())));
					}

					int elapsed = progress.counter() + TICK_STEP;
					Modulo.setProgress(player, progress.withCounter(elapsed));
					if (!JjkConfig.server().modulo.epilogue || elapsed >= sequence.seconds() * 20) {
						Modulo.setProgress(player, Modulo.progress(player).withEpilogueSeen());
						complete(player, current, true);
					}
				}
				default -> {
				}
			}
		}

		bySite.forEach((siteId, here) -> keepSite(server, siteId, here));
	}

	/** Builds a site and keeps its characters, spawns and boss present for the players on its stages. */
	private static void keepSite(MinecraftServer server, Identifier siteId, List<ServerPlayer> players) {
		Optional<SiteDefinition> built = SiteBuilder.ensure(server, siteId);
		if (built.isEmpty()) {
			return;
		}

		SiteDefinition site = built.get();
		ServerLevel level = server.getLevel(site.dimension());
		if (level == null) {
			return;
		}

		AABB area = new AABB(site.center()).inflate(site.radius() + 8, 24, site.radius() + 8);
		for (SiteDefinition.Npc npc : site.npcs()) {
			boolean present = !level.getEntitiesOfClass(ModuloNpcEntity.class, area,
					e -> e.character().equals(npc.character()) && e.role() == ModuloNpcEntity.Role.SITE).isEmpty();
			if (!present) {
				ModuloEntities.spawnNpc(level, npc.character(), ModuloNpcEntity.Role.SITE, null, siteId, site.center().offset(npc.offset()));
			}
		}

		// One boss, spawns and allies per mission stage played here.
		Map<String, List<ServerPlayer>> byStage = new HashMap<>();
		Map<String, Current> stages = new HashMap<>();
		for (ServerPlayer player : players) {
			current(player).ifPresent(c -> {
				String key = c.id() + "#" + c.stageIndex();
				byStage.computeIfAbsent(key, k -> new ArrayList<>()).add(player);
				stages.put(key, c);
			});
		}

		for (Map.Entry<String, List<ServerPlayer>> entry : byStage.entrySet()) {
			Current current = stages.get(entry.getKey());
			MissionStage stage = current.stage();
			List<ServerPlayer> here = entry.getValue().stream().filter(p -> atSite(p, site, 8)).toList();
			if (here.isEmpty()) {
				continue;
			}

			for (MissionStage.Spawn spawn : stage.base().spawns()) {
				keepSpawns(level, site, spawn);
			}

			Optional<Identifier> bossId = switch (stage) {
				case MissionStage.Defeat defeat -> Optional.of(defeat.boss());
				case MissionStage.Defend defend -> defend.boss();
				default -> Optional.empty();
			};
			if (bossId.isEmpty()) {
				continue;
			}

			List<ModuloBossEntity> bosses = level.getEntitiesOfClass(ModuloBossEntity.class, area.inflate(16), b -> bossId.get().equals(b.bossId())
					&& current.id().equals(b.mission()) && b.stage() == current.stageIndex());
			if (bosses.isEmpty()) {
				ModuloEntities.spawnBoss(level, bossId.get(), site.marker("boss"), siteId, current.id(), current.stageIndex(), here.size());
				if (stage instanceof MissionStage.Defeat defeat) {
					int i = 0;
					for (String ally : defeat.allies()) {
						ModuloEntities.spawnNpc(level, ally, ModuloNpcEntity.Role.ALLY, null, siteId, site.marker("boss").offset(-3 - i * 2, 0, 3));
						i++;
					}
				}
			} else {
				bosses.forEach(boss -> boss.rescale(here.size()));
			}
		}
	}

	private static void keepSpawns(ServerLevel level, SiteDefinition site, MissionStage.Spawn spawn) {
		AABB area = new AABB(site.center()).inflate(spawn.radius() + 4, 16, spawn.radius() + 4);
		EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(spawn.entity());
		long have = level.getEntitiesOfClass(Entity.class, area, e -> e.getType() == type && e.entityTags().contains(SPAWN_TAG) && e.isAlive()).size();
		for (long i = have; i < spawn.count(); i++) {
			double angle = level.getRandom().nextDouble() * Math.PI * 2;
			double distance = spawn.radius() * (0.4 + level.getRandom().nextDouble() * 0.6);
			BlockPos at = site.center().offset((int) (Math.cos(angle) * distance), 0, (int) (Math.sin(angle) * distance));
			Entity entity = CurseSpawner.spawnOne(level, spawn.entity(), at);
			if (entity == null) {
				entity = type.create(level, EntitySpawnReason.EVENT);
				if (entity == null) {
					continue;
				}

				entity.snapTo(at.getX() + 0.5, at.getY() - (type == ModuloEntities.WANCHA_OCTOPUS ? 2 : 0), at.getZ() + 0.5, 0, 0);
				level.addFreshEntity(entity);
			}

			entity.addTag(SPAWN_TAG);
		}
	}

	/** Removes site characters and spawns nobody is near any more. */
	private static void tidy(MinecraftServer server) {
		for (ServerLevel level : Arrays.asList(server.getLevel(Modulo.NAUNAX), server.getLevel(Modulo.TOKYO))) {
			if (level == null) {
				continue;
			}

			for (Entity entity : level.getAllEntities()) {
				boolean ours = entity instanceof ModuloNpcEntity npc && npc.role() != ModuloNpcEntity.Role.COMPANION || entity.entityTags().contains(SPAWN_TAG)
						|| entity instanceof ModuloBossEntity;
				if (ours && level.getNearestPlayer(entity, 64) == null) {
					entity.discard();
				}
			}
		}
	}

	/** Keeps Maru beside a player who recruited him, while they are in the expansion. */
	private static void companion(ServerPlayer player) {
		if (!Modulo.progress(player).companion() || !Modulo.isModuloLevel(player.level()) || player.isSpectator()) {
			return;
		}

		// While a player duels Maru himself, their companion steps aside.
		boolean duel = current(player).filter(c -> c.stage() instanceof MissionStage.Defeat defeat && defeat.boss().getPath().equals("maru_duel")).isPresent();
		if (duel) {
			player.level().getEntitiesOfClass(ModuloNpcEntity.class, player.getBoundingBox().inflate(64), npc -> npc.ownedBy(player)).forEach(Entity::discard);
			return;
		}

		ServerLevel level = player.level();
		boolean present = !level.getEntitiesOfClass(ModuloNpcEntity.class, player.getBoundingBox().inflate(48), npc -> npc.ownedBy(player)).isEmpty();
		if (!present) {
			ModuloEntities.spawnNpc(level, "maru", ModuloNpcEntity.Role.COMPANION, player.getUUID(), null, player.blockPosition().offset(1, 0, 1));
		}
	}

	private static void intro(ServerPlayer player, Current current) {
		String key = current.id() + "#" + current.stageIndex();
		if (key.equals(INTRO_SHOWN.get(player.getUUID()))) {
			return;
		}

		INTRO_SHOWN.put(player.getUUID(), key);
		List<MissionStage.Line> lines = current.stage().base().intro();
		for (int i = 0; i < lines.size(); i++) {
			MissionStage.Line line = lines.get(i);
			Modulo.send(player, new ModuloPayloads.Dialogue(line.speaker(), line.text(), i, lines.size(), List.of()));
		}
	}

	// ---------------------------------------------------------------- events

	/** A player right-clicked a character. */
	public static void onTalk(ServerPlayer player, ModuloNpcEntity npc) {
		if (npc.ownedBy(player) && player.isShiftKeyDown()) {
			// Maru's Harmony takes his player to the ship.
			if (com.jjkcraft.modulo.ModuloTravel.toShip(player)) {
				player.sendSystemMessage(Component.translatable("message.jjkcraft.modulo.harmony"), true);
			}

			return;
		}

		Optional<Current> found = current(player);
		if (found.isPresent()) {
			Current current = found.get();
			ModuloProgress progress = Modulo.progress(player);
			switch (current.stage()) {
				case MissionStage.Talk talk when talk.npc().equals(npc.character()) -> {
					int index = Math.min(progress.counter(), talk.lines().size() - 1);
					MissionStage.Line line = talk.lines().get(index);
					Modulo.send(player, new ModuloPayloads.Dialogue(line.speaker(), line.text(), index, talk.lines().size(), List.of()));
					Modulo.setProgress(player, progress.withCounter(index + 1));
					if (index + 1 >= talk.lines().size()) {
						complete(player, current, true);
					}

					return;
				}
				case MissionStage.Deliver deliver when deliver.npc().equals(npc.character()) -> {
					if (progress.counter() == 0) {
						if (count(player, deliver.item()) < deliver.count()) {
							Modulo.send(player, new ModuloPayloads.Dialogue(npc.character(), "dialogue.jjkcraft.modulo.not_yet", 0, 1, List.of()));
							return;
						}

						take(player, deliver.item(), deliver.count());
					}

					if (deliver.lines().isEmpty()) {
						complete(player, current, true);
						return;
					}

					int index = Math.min(progress.counter(), deliver.lines().size() - 1);
					MissionStage.Line line = deliver.lines().get(index);
					Modulo.send(player, new ModuloPayloads.Dialogue(line.speaker(), line.text(), index, deliver.lines().size(), List.of()));
					Modulo.setProgress(player, progress.withCounter(index + 1));
					if (index + 1 >= deliver.lines().size()) {
						complete(player, current, true);
					}

					return;
				}
				case MissionStage.Choice choice when choice.npc().equals(npc.character()) -> {
					List<String> options = choice.options().stream().map(o -> "option.jjkcraft.modulo." + choice.flag() + "." + o).toList();
					Modulo.send(player, new ModuloPayloads.Dialogue(npc.character(), choice.base().objective(), 0, 1, options));
					return;
				}
				default -> {
				}
			}
		}

		Modulo.send(player, new ModuloPayloads.Dialogue(npc.character(), "npc.jjkcraft.modulo." + npc.character() + ".idle", 0, 1, List.of()));
	}

	/** A player picked an option at a choice stage. Checked: right stage, a valid option, the character near, rate limited. */
	public static void onChoose(ServerPlayer player, int option) {
		if (!CHOOSE_LIMIT.tryAcquire(player.getUUID(), player.level().getGameTime())) {
			return;
		}

		Optional<Current> found = current(player);
		if (found.isEmpty() || !(found.get().stage() instanceof MissionStage.Choice choice) || option < 0 || option >= choice.options().size()) {
			return;
		}

		boolean near = !player.level().getEntitiesOfClass(ModuloNpcEntity.class, player.getBoundingBox().inflate(8),
				npc -> npc.character().equals(choice.npc())).isEmpty();
		if (!near) {
			return;
		}

		Modulo.setProgress(player, Modulo.progress(player).withFlag(choice.flag(), choice.options().get(option)));
		complete(player, found.get(), true);
	}

	/** Mul energy was made: counts toward a make_mul_energy stage. */
	public static void onMulEnergyMade(ServerPlayer player, double amount) {
		Optional<Current> found = current(player);
		if (found.isPresent() && found.get().stage() instanceof MissionStage.MakeMulEnergy) {
			ModuloProgress progress = Modulo.progress(player);
			Modulo.setProgress(player, progress.withCounter(progress.counter() + (int) Math.ceil(amount)));
		}
	}

	/** A boss fell or yielded: everyone on its stage at its site wins the stage. */
	public static void onBossDefeated(ModuloBossEntity boss) {
		if (boss.level() instanceof ServerLevel level) {
			onBossDefeated(boss, List.copyOf(level.players()));
		}
	}

	/** {@link #onBossDefeated(ModuloBossEntity)} for the given candidates (GameTests pass their fake players). */
	public static void onBossDefeated(ModuloBossEntity boss, List<ServerPlayer> candidates) {
		if (!(boss.level() instanceof ServerLevel level) || boss.mission() == null) {
			return;
		}

		Optional<SiteDefinition> site = boss.site() == null ? Optional.empty() : ModuloRegistries.site(level.registryAccess(), boss.site());
		for (ServerPlayer player : candidates) {
			boolean there = site.map(s -> atSite(player, s, NEAR)).orElse(player.distanceToSqr(boss) < NEAR * NEAR);
			if (there) {
				current(player).filter(c -> c.id().equals(boss.mission()) && c.stageIndex() == boss.stage()).ifPresent(c -> complete(player, c, true));
			}
		}

		AABB area = boss.getBoundingBox().inflate(64);
		for (ModuloNpcEntity ally : level.getEntitiesOfClass(ModuloNpcEntity.class, area, n -> n.role() == ModuloNpcEntity.Role.ALLY)) {
			ally.discard();
		}

		for (Entity spawn : level.getEntitiesOfClass(Entity.class, area, e -> e.entityTags().contains(SPAWN_TAG))) {
			spawn.discard();
		}
	}

	// ---------------------------------------------------------------- progress

	/** The current stage is done: the next is the checkpoint, and after the last the mission's rewards. */
	public static void complete(ServerPlayer player, Current current, boolean announce) {
		ModuloProgress progress = Modulo.progress(player).nextStage();
		Modulo.setProgress(player, progress);
		if (announce) {
			Modulo.send(player, new ModuloPayloads.Event("stage_done", List.of(current.stage().base().objective())));
			player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 0.6f, 1.4f);
		}

		if (progress.stage() < current.definition().stages().size()) {
			return;
		}

		reward(player, current.definition().rewards());
		Modulo.setProgress(player, Modulo.progress(player).finishing(current.id()));
		Modulo.send(player, new ModuloPayloads.Event("mission_done", List.of(current.definition().title())));
		player.sendSystemMessage(Component.translatable("message.jjkcraft.modulo.mission_done", Component.translatable(current.definition().title())));
		JjkCraft.LOGGER.info("{} finished Modulo mission {}", player.getName().getString(), current.id());
	}

	private static void reward(ServerPlayer player, MissionDefinition.Rewards rewards) {
		for (MissionDefinition.ItemReward item : rewards.items()) {
			ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.getValue(item.item()), item.count());
			if (!player.getInventory().add(stack)) {
				player.spawnAtLocation(player.level(), stack);
			}
		}

		if (rewards.gradeXp() > 0) {
			Progression.addXp(player, rewards.gradeXp());
		}

		if (rewards.mulEnergy() > 0) {
			MulEnergy.add(player, rewards.mulEnergy());
		}

		if (rewards.companion()) {
			Modulo.setProgress(player, Modulo.progress(player).withCompanion(true));
			player.sendSystemMessage(Component.translatable("message.jjkcraft.modulo.companion"));
		}

		rewards.advancement().ifPresent(id -> {
			AdvancementHolder holder = player.level().getServer().getAdvancements().get(id);
			if (holder != null) {
				AdvancementProgress done = player.getAdvancements().getOrStartProgress(holder);
				for (String criterion : done.getRemainingCriteria()) {
					player.getAdvancements().award(holder, criterion);
				}
			} else {
				JjkCraft.LOGGER.warn("Modulo reward names unknown advancement {}", id);
			}
		});
	}

	private static void take(ServerPlayer player, Identifier itemId, int amount) {
		Item item = BuiltInRegistries.ITEM.getValue(itemId);
		int left = amount;
		for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
			if (left <= 0) {
				break;
			}

			if (stack.is(item)) {
				int taken = Math.min(left, stack.getCount());
				stack.shrink(taken);
				left -= taken;
			}
		}
	}

	/** Sets a player's place in the chain (commands and tests): {@code mission} at {@code stage}, earlier missions done. */
	public static void jumpTo(ServerPlayer player, Identifier mission, int stage) {
		List<Identifier> done = new ArrayList<>();
		for (ModuloRegistries.Mission m : ModuloRegistries.missions(player.registryAccess())) {
			if (m.id().equals(mission)) {
				break;
			}

			done.add(m.id());
		}

		ModuloProgress progress = Modulo.progress(player);
		ModuloProgress next = new ModuloProgress(true, progress.returnTo(), List.copyOf(done), Optional.of(mission), stage, 0,
				progress.companion() || !done.isEmpty(), progress.epilogueSeen(), progress.flags());
		Modulo.setProgress(player, next);
		INTRO_SHOWN.remove(player.getUUID());
	}
}
