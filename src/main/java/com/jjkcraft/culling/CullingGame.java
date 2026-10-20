package com.jjkcraft.culling;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import com.jjkcraft.JjkCraft;
import com.jjkcraft.config.JjkConfig;
import com.jjkcraft.config.JjkServerConfig;
import com.jjkcraft.core.culling.CullingMath;
import com.jjkcraft.core.progress.ProgressionMath;
import com.jjkcraft.culling.CullingState.Colony;
import com.jjkcraft.culling.CullingState.Participant;
import com.jjkcraft.culling.entity.CullingEntities;
import com.jjkcraft.culling.entity.EntrantEntity;
import com.jjkcraft.culling.entity.KoganeEntity;
import com.jjkcraft.culling.rule.CullingRule;
import com.jjkcraft.culling.rule.RuleEffects;
import com.jjkcraft.culling.rule.RuleEvent;
import com.jjkcraft.culling.rule.RuleTriggers;
import com.jjkcraft.curse.CurseEntity;
import com.jjkcraft.curse.mechanic.WardenMechanic;
import com.jjkcraft.curse.spawn.CurseSpawner;
import com.jjkcraft.damage.JjkDamageTypes;
import com.jjkcraft.energy.CursedEnergy;
import com.jjkcraft.grade.GradeDefinition;
import com.jjkcraft.grade.Grades;
import com.jjkcraft.network.JjkNetworking;
import com.jjkcraft.network.payload.CullingPayloads;
import com.jjkcraft.registry.JjkRegistries;
import com.jjkcraft.sorcerer.Awakening;
import com.jjkcraft.sorcerer.Progression;
import com.jjkcraft.sorcerer.SorcererProgress;
import com.jjkcraft.sound.JjkSounds;
import com.jjkcraft.technique.TechniqueService;
import com.jjkcraft.technique.TechniqueState;

/**
 * The Culling Game. Server authoritative: it raises colonies (barrier-enclosed regions with a
 * Kogane at the centre), turns whoever walks in after the barrier rose into a participant,
 * keeps participants inside, scores kills, runs the day clock and the entry window, and offers
 * every event to the rules in force. What happens is up to the rules: this class only builds events
 * and carries out the small, validated set of effects ({@link RuleEffects}).
 */
public final class CullingGame {
	private static final UUID GAME = new UUID(0, 0);
	/** How close to the Kogane a participant must stand to negotiate. */
	public static final double MASTER_RANGE = 8;

	/** viewer -> (target -> revealed until tick). Not saved: a reveal is short. */
	private static final Map<UUID, Map<UUID, Long>> REVEALS = new HashMap<>();
	private static long leaderRevealedUntil = -1;
	private static final Set<UUID> NEAR_MASTER = new HashSet<>();
	private static final Map<UUID, Integer> SENT = new HashMap<>();
	private static final Map<UUID, Integer> WARNED = new HashMap<>();
	private static final Map<UUID, Long> HELD_NOTICE = new HashMap<>();
	/** Entities last seen as (would-be) participants, so effects can reach them by id. */
	private static final Map<UUID, LivingEntity> SEEN = new HashMap<>();

	private CullingGame() {
	}

	public static void init() {
		RuleTriggers.init();
		RuleEffects.init();
		ServerTickEvents.END_SERVER_TICK.register(CullingGame::tick);
		ServerLivingEntityEvents.AFTER_DEATH.register(CullingGame::onDeath);
		Awakening.addHook((player, heavenly) -> onAwakened(player, heavenly));
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> onJoin(handler.getPlayer()));
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			SENT.remove(handler.getPlayer().getUUID());
			NEAR_MASTER.remove(handler.getPlayer().getUUID());
			SEEN.remove(handler.getPlayer().getUUID());
			HELD_NOTICE.remove(handler.getPlayer().getUUID());
			REVEALS.remove(handler.getPlayer().getUUID());
			com.jjkcraft.core.RateLimiter.forgetEverywhere(handler.getPlayer().getUUID());
		});
		ServerPlayNetworking.registerGlobalReceiver(CullingPayloads.Action.TYPE, (payload, context) -> onAction(context.player(), payload));
		CursedEnergy.addCapacityModifier(CullingGame::capacity);
		WardenMechanic.addRule(CullingGame::bindsWarden);
		WardenMechanic.addEdictSource(CullingGame::wardenEdict);
	}

	public static boolean enabled() {
		return JjkConfig.server().systems.cullingGame;
	}

	private static JjkServerConfig.CullingGame config() {
		return JjkConfig.server().cullingGame;
	}

	public static CullingState state(MinecraftServer server) {
		return server.getDataStorage().computeIfAbsent(CullingState.TYPE);
	}

	public static long now(MinecraftServer server) {
		return server.overworld().getGameTime();
	}

	// ---------------------------------------------------------------- rules

	public record RuleEntry(Identifier id, CullingRule rule) {
	}

	public static Optional<Registry<CullingRule>> rules(MinecraftServer server) {
		return server.registryAccess().lookup(JjkRegistries.CULLING_RULE);
	}

	public static Optional<CullingRule> rule(MinecraftServer server, Identifier id) {
		return rules(server).flatMap(r -> r.getOptional(id));
	}

	public static List<RuleEntry> allRules(MinecraftServer server) {
		return rules(server).map(r -> r.listElements()
				.map(h -> new RuleEntry(h.key().identifier(), h.value()))
				.sorted((a, b) -> a.rule().order() != b.rule().order() ? Integer.compare(a.rule().order(), b.rule().order())
						: a.id().toString().compareTo(b.id().toString()))
				.toList()).orElse(List.of());
	}

	/** Offers an event to every rule in force. Returns how many clauses answered it. */
	public static int fire(RuleEvent event) {
		MinecraftServer server = event.level.getServer();
		CullingState state = state(server);
		int matched = 0;
		for (CullingState.ActiveRule active : List.copyOf(state.rules)) {
			if (event.kind == RuleEvent.Kind.ADDED && !active.id().equals(event.rule)) {
				continue;
			}

			CullingRule rule = rule(server, active.id()).orElse(null);
			if (rule == null) {
				continue;
			}

			for (CullingRule.Clause clause : rule.clauses()) {
				if (clause.when().matches(event)) {
					matched++;
					for (RuleEffects.Effect effect : clause.then()) {
						effect.apply(event, active.id());
					}
				}
			}
		}

		return matched;
	}

	// ---------------------------------------------------------------- lifecycle

	/** Starts the game: base rules, colonies on a ring around {@code around}, entry windows. */
	public static boolean start(ServerLevel level, BlockPos around, int count, int radius, int distance) {
		MinecraftServer server = level.getServer();
		CullingState state = state(server);
		if (state.running) {
			return false;
		}

		long now = now(server);
		despawnAll(server, state);
		state.colonies.clear();
		state.participants.clear();
		state.deadlines.clear();
		state.rules.clear();
		state.log.clear();
		state.results.clear();
		state.running = true;
		state.ended = false;
		state.startedAt = now;
		state.lastDay = 0;
		state.joinedEver = 0;
		REVEALS.clear();
		leaderRevealedUntil = -1;
		for (RuleEntry entry : allRules(server)) {
			if (entry.rule().base()) {
				state.rules.add(new CullingState.ActiveRule(entry.id(), Optional.empty(), now, CullingState.ActiveRule.NEVER));
			}
		}

		double phase = level.getRandom().nextDouble() * Math.PI * 2;
		double[][] ring = CullingMath.ring(count, count == 1 ? 0 : distance, phase);
		for (double[] offset : ring) {
			int x = around.getX() + (int) Math.round(offset[0]);
			int z = around.getZ() + (int) Math.round(offset[1]);
			int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
			raise(level, new BlockPos(x, y, z), radius, null);
		}

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (canBeHunted(player)) {
				state.deadlines.put(player.getUUID(), CullingMath.deadline(now, config().entryWindowDays));
			}
		}

		state.log(now, "culling.jjkcraft.log.started", String.valueOf(state.colonies.size()));
		state.changed();
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			player.sendSystemMessage(Component.translatable("message.jjkcraft.culling.started", state.colonies.size(), config().entryWindowDays));
			JjkNetworking.send(player, new CullingPayloads.Event("started", List.of(String.valueOf(state.colonies.size()))));
		}

		return true;
	}

	/** Raises one colony around {@code center}. Whoever is inside right now is a resident, free to leave. */
	public static Colony raise(ServerLevel level, BlockPos center, int radius, @Nullable String name) {
		CullingState state = state(level.getServer());
		int id = state.nextColony++;
		String colonyName = name != null ? name : "colony.jjkcraft.name_" + (id % 12);
		List<UUID> residents = new ArrayList<>();
		Colony probe = new Colony(id, colonyName, level.dimension(), center, radius, 0, List.of(), 0);
		for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class,
				new net.minecraft.world.phys.AABB(center).inflate(radius, radius + CullingMath.WALL, radius))) {
			if (probe.contains(level.dimension(), entity.getX(), entity.getY(), entity.getZ())) {
				residents.add(entity.getUUID());
			}
		}

		Colony colony = new Colony(id, colonyName, level.dimension(), center, radius, now(level.getServer()), residents, 0);
		colony.pending = config().entrantsPerColony;
		state.colonies.add(colony);
		state.log(now(level.getServer()), "culling.jjkcraft.log.colony", colonyName);
		state.changed();
		level.playSound(null, center, JjkSounds.BARRIER, SoundSource.AMBIENT, 4f, 0.5f);
		return colony;
	}

	/** Ends the game: standings, rewards for those online, barriers down. */
	public static void end(MinecraftServer server, String reason) {
		CullingState state = state(server);
		if (!state.running) {
			return;
		}

		long now = now(server);
		List<Participant> ranking = state.ranking();
		state.results.clear();
		for (int i = 0; i < Math.min(10, ranking.size()); i++) {
			Participant p = ranking.get(i);
			state.results.add(new CullingState.Standing(p.name, p.points, p.colony));
		}

		for (int i = 0; i < ranking.size(); i++) {
			Participant p = ranking.get(i);
			ServerPlayer player = p.npc ? null : server.getPlayerList().getPlayer(p.id);
			if (player != null) {
				int xp = CullingMath.finalXp(p.points, i + 1);
				Progression.addXp(player, xp);
				player.sendSystemMessage(Component.translatable("message.jjkcraft.culling.final", i + 1, p.points, xp));
			}
		}

		despawnAll(server, state);
		state.running = false;
		state.ended = true;
		state.deadlines.clear();
		state.colonies.clear();
		state.log(now, "culling.jjkcraft.log.ended", "culling.jjkcraft.end." + reason);
		state.changed();
		String winner = state.results.isEmpty() ? "-" : state.results.getFirst().name();
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			player.sendSystemMessage(Component.translatable("message.jjkcraft.culling.ended", Component.translatable("culling.jjkcraft.end." + reason), winner));
			JjkNetworking.send(player, new CullingPayloads.Event("ended", List.of(reason, winner)));
		}
	}

	/** Clears everything, as if the game had never run (commands, tests). */
	public static void reset(MinecraftServer server) {
		CullingState state = state(server);
		despawnAll(server, state);
		state.running = false;
		state.ended = false;
		state.colonies.clear();
		state.participants.clear();
		state.deadlines.clear();
		state.rules.clear();
		state.log.clear();
		state.results.clear();
		state.nextColony = 0;
		state.joinedEver = 0;
		REVEALS.clear();
		WARNED.clear();
		SEEN.clear();
		leaderRevealedUntil = -1;
		state.changed();
	}

	/**
	 * Moves the game clock {@code days} days forward by shifting every stored time back: the start,
	 * deadlines, scores and rules. The next clock tick then plays out the days that passed.
	 */
	public static void advance(MinecraftServer server, int days) {
		CullingState state = state(server);
		long shift = days * CullingMath.DAY;
		state.startedAt -= shift;
		state.deadlines.replaceAll((id, at) -> at - shift);
		for (Participant participant : state.participants.values()) {
			participant.lastScoredAt -= shift;
		}

		state.rules.replaceAll(rule -> new CullingState.ActiveRule(rule.id(), rule.addedBy(), rule.addedAt() - shift,
				rule.lapses() ? rule.expiresAt() - shift : CullingState.ActiveRule.NEVER));
		state.changed();
	}

	/** Runs the day clock now instead of on the next second (tests). */
	public static void runClock(MinecraftServer server) {
		CullingState state = state(server);
		if (state.running) {
			clock(server, state, now(server));
		}
	}

	private static void despawnAll(MinecraftServer server, CullingState state) {
		List<Entity> gone = new ArrayList<>();
		for (ServerLevel level : server.getAllLevels()) {
			for (Entity entity : level.getAllEntities()) {
				if (entity instanceof KoganeEntity || entity instanceof EntrantEntity) {
					gone.add(entity);
				}
			}
		}

		gone.forEach(Entity::discard);

		for (Colony colony : state.colonies) {
			colony.master = null;
		}
	}

	// ---------------------------------------------------------------- ticking

	private static void tick(MinecraftServer server) {
		if (!enabled()) {
			return;
		}

		CullingState state = state(server);
		int tick = server.getTickCount();
		if (state.running) {
			long now = now(server);
			if (tick % 5 == 0) {
				for (ServerPlayer player : server.getPlayerList().getPlayers()) {
					updatePresence(player);
				}
			}

			if (tick % 20 == 0) {
				clock(server, state, now);
				upkeep(server, state, now);
			}
		}

		if (tick % 10 == 0) {
			syncAll(server, state, tick % 100 == 0);
		}
	}

	/** Membership and the barrier for one entity: entering, leaving, the Kogane nearby. Public for tests. */
	public static void updatePresence(LivingEntity entity) {
		if (!(entity.level() instanceof ServerLevel level) || !entity.isAlive()) {
			return;
		}

		CullingState state = state(level.getServer());
		if (!state.running) {
			return;
		}

		UUID id = entity.getUUID();
		SEEN.put(id, entity);
		Participant participant = state.participant(id);
		if (participant != null && participant.active()) {
			Colony own = state.colony(participant.colony).orElse(null);
			if (own != null && !own.contains(level.dimension(), entity.getX(), entity.getY(), entity.getZ())) {
				RuleEvent event = new RuleEvent(RuleEvent.Kind.LEAVE, level, id);
				event.actorEntity = entity;
				event.colony = own.id;
				fire(event);
			}
		} else if (participant == null && canJoin(entity)) {
			state.colonyAt(level.dimension(), entity.getX(), entity.getY(), entity.getZ()).ifPresent(colony -> {
				if (!colony.residents.contains(id)) {
					RuleEvent event = new RuleEvent(RuleEvent.Kind.ENTER, level, id);
					event.actorEntity = entity;
					event.colony = colony.id;
					fire(event);
				}
			});
		}

		if (entity instanceof ServerPlayer player) {
			boolean near = nearMaster(player);
			if (near != NEAR_MASTER.contains(id)) {
				if (near) {
					NEAR_MASTER.add(id);
				} else {
					NEAR_MASTER.remove(id);
				}

				SENT.remove(id);
			}
		}
	}

	private static boolean canJoin(LivingEntity entity) {
		if (entity instanceof ServerPlayer player) {
			return !player.isSpectator() && !player.isCreative();
		}

		return entity instanceof EntrantEntity;
	}

	/** The day clock: deadlines, expiring rules, timers, stagnation, reinforcements and the end. */
	private static void clock(MinecraftServer server, CullingState state, long now) {
		ServerLevel overworld = server.overworld();
		for (UUID id : List.copyOf(state.deadlines.keySet())) {
			ServerPlayer player = server.getPlayerList().getPlayer(id);
			if (player != null) {
				checkDeadline(player);
			}
		}

		if (state.rules.removeIf(rule -> rule.lapses() && rule.expiresAt() <= now)) {
			state.log(now, "culling.jjkcraft.log.rule_lapsed");
			state.changed();
		}

		int day = CullingMath.day(now, state.startedAt);
		if (day <= state.lastDay) {
			return;
		}

		for (int d = state.lastDay + 1; d <= day; d++) {
			RuleEvent timer = new RuleEvent(RuleEvent.Kind.TIMER, overworld, GAME);
			timer.day = d;
			fire(timer);
			for (Participant participant : List.copyOf(state.participants.values())) {
				int idle = (int) ((now - participant.lastScoredAt) / CullingMath.DAY);
				if (participant.active() && idle >= 1) {
					RuleEvent event = new RuleEvent(RuleEvent.Kind.IDLE, overworld, participant.id);
					event.actorEntity = find(server, participant.id) instanceof LivingEntity living ? living : null;
					event.idleDays = idle;
					event.colony = participant.colony;
					fire(event);
				}
			}

			for (Colony colony : state.colonies) {
				colony.pending = Math.min(colony.pending + config().entrantsPerDay, config().maxEntrantsPerColony);
			}
		}

		state.lastDay = day;
		state.participants.values().removeIf(p -> p.npc && !p.active() && now - p.joinedAt > 3 * CullingMath.DAY);
		state.log(now, "culling.jjkcraft.log.day", String.valueOf(day + 1));
		state.changed();
		if (CullingMath.shouldEnd(day, config().endAfterDays, state.standing(), state.joinedEver, config().endWhenPlayersLeft)) {
			end(server, config().endAfterDays > 0 && day >= config().endAfterDays ? "days" : "last_standing");
		}
	}

	/** The entry window for one player: a warning at 3 and 1 days, the rules' penalty when it closes. Public for tests. */
	public static void checkDeadline(ServerPlayer player) {
		MinecraftServer server = player.level().getServer();
		CullingState state = state(server);
		Long deadline = state.deadlines.get(player.getUUID());
		if (!state.running || deadline == null) {
			return;
		}

		int left = CullingMath.daysLeft(now(server), deadline);
		if (left <= 0) {
			state.deadlines.remove(player.getUUID());
			WARNED.remove(player.getUUID());
			RuleEvent event = new RuleEvent(RuleEvent.Kind.DEADLINE, player.level(), player.getUUID());
			event.actorEntity = player;
			fire(event);
			state.changed();
		} else if ((left == 3 || left == 1) && WARNED.getOrDefault(player.getUUID(), -1) != left) {
			WARNED.put(player.getUUID(), left);
			player.sendSystemMessage(Component.translatable("message.jjkcraft.culling.deadline", left));
			JjkNetworking.send(player, new CullingPayloads.Event("deadline_warning", List.of(String.valueOf(left))));
		}
	}

	/** Every second: a Kogane at each loaded colony centre, and entrants waiting to come in. */
	private static void upkeep(MinecraftServer server, CullingState state, long now) {
		for (Colony colony : state.colonies) {
			ServerLevel level = server.getLevel(colony.dimension);
			if (level == null || !level.isLoaded(colony.center)) {
				continue;
			}

			if (colony.master == null || !(level.getEntity(colony.master) instanceof KoganeEntity)) {
				KoganeEntity master = CullingEntities.KOGANE.create(level, EntitySpawnReason.EVENT);
				if (master != null) {
					master.setColony(colony.id);
					BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, colony.center);
					master.snapTo(ground.getX() + 0.5, ground.getY() + 1.5, ground.getZ() + 0.5, 0, 0);
					level.addFreshEntity(master);
					colony.master = master.getUUID();
				}
			}

			int present = 0;
			for (Participant participant : state.participants.values()) {
				if (participant.npc && participant.active() && participant.colony == colony.id) {
					present++;
				}
			}

			for (int i = 0; i < 2 && colony.pending > 0 && present < config().maxEntrantsPerColony; i++) {
				if (spawnEntrant(level, colony, level.getRandom().nextInt(100) < config().sorcererEntrantPercent) == null) {
					break;
				}

				colony.pending--;
				present++;
				state.setDirty();
			}
		}
	}

	/** Brings one entrant into a colony at a loaded spot. Public for commands and tests. */
	public static @Nullable EntrantEntity spawnEntrant(ServerLevel level, Colony colony, boolean sorcerer) {
		RandomSource random = level.getRandom();
		for (int attempt = 0; attempt < 6; attempt++) {
			double angle = random.nextDouble() * Math.PI * 2;
			double distance = Math.sqrt(random.nextDouble()) * colony.radius * 0.6;
			int x = colony.center.getX() + (int) Math.round(Math.cos(angle) * distance);
			int z = colony.center.getZ() + (int) Math.round(Math.sin(angle) * distance);
			BlockPos column = new BlockPos(x, colony.center.getY(), z);
			if (!level.isLoaded(column)) {
				continue;
			}

			BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column);
			if (!colony.contains(level.dimension(), ground.getX() + 0.5, ground.getY(), ground.getZ() + 0.5)) {
				continue;
			}

			EntrantEntity entrant = CullingEntities.ENTRANT.create(level, EntitySpawnReason.EVENT);
			if (entrant == null) {
				return null;
			}

			entrant.snapTo(ground.getX() + 0.5, ground.getY(), ground.getZ() + 0.5, random.nextFloat() * 360f, 0);
			entrant.setup(sorcerer, colony.id, random);
			level.addFreshEntity(entrant);
			join(entrant, colony.id);
			return entrant;
		}

		return null;
	}

	// ---------------------------------------------------------------- membership

	/** Makes {@code entity} a participant of {@code colonyId}. */
	public static void join(LivingEntity entity, int colonyId) {
		if (!(entity.level() instanceof ServerLevel level)) {
			return;
		}

		MinecraftServer server = level.getServer();
		CullingState state = state(server);
		Colony colony = state.colony(colonyId).orElse(null);
		if (colony == null || state.participant(entity.getUUID()) != null) {
			return;
		}

		long now = now(server);
		Participant participant = new Participant(entity.getUUID(), entity.getName().getString(), entity instanceof EntrantEntity, isSorcerer(entity),
				colonyId, 0, 0, now, now, CullingState.Status.ACTIVE, 1.0);
		state.participants.put(participant.id, participant);
		state.deadlines.remove(participant.id);
		SEEN.put(participant.id, entity);
		state.joinedEver++;
		if (entity instanceof ServerPlayer player) {
			state.log(now, "culling.jjkcraft.log.joined", participant.name, colony.name);
			player.sendSystemMessage(Component.translatable("message.jjkcraft.culling.joined", Component.translatableWithFallback(colony.name, colony.name)));
			JjkNetworking.send(player, new CullingPayloads.Event("joined", List.of(colony.name)));
			level.playSound(null, player.getX(), player.getY(), player.getZ(), JjkSounds.BARRIER, SoundSource.PLAYERS, 1f, 1.6f);
		}

		state.changed();
	}

	/** A sorcerer: an awakened player with a technique (not Heavenly Restricted), or a sorcerer entrant. */
	public static boolean isSorcerer(LivingEntity entity) {
		if (entity instanceof Player player) {
			SorcererProgress progress = Progression.get(player);
			return progress.awakened() && !progress.heavenlyRestricted();
		}

		return entity instanceof EntrantEntity entrant && entrant.isSorcerer();
	}

	private static boolean canBeHunted(ServerPlayer player) {
		SorcererProgress progress = Progression.get(player);
		return progress.awakened() && !progress.heavenlyRestricted();
	}

	/** Holds a participant inside their colony: pushed back at the barrier, brought back from afar. */
	public static void confine(LivingEntity entity, int colonyId) {
		if (!(entity.level() instanceof ServerLevel level)) {
			return;
		}

		MinecraftServer server = level.getServer();
		Colony colony = state(server).colony(colonyId).orElse(null);
		ServerLevel home = colony == null ? null : server.getLevel(colony.dimension);
		if (home == null) {
			return;
		}

		double cx = colony.center.getX() + 0.5;
		double cz = colony.center.getZ() + 0.5;
		double dx = entity.getX() - cx;
		double dz = entity.getZ() - cz;
		double distance = Math.sqrt(dx * dx + dz * dz);
		if (level != home || distance > colony.radius + 24) {
			// Far away (respawned elsewhere, or another dimension): back to the centre.
			BlockPos ground = home.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, colony.center);
			entity.teleportTo(home, cx + 2, ground.getY(), cz + 2, Set.of(), entity.getYRot(), entity.getXRot(), true);
		} else if (distance > colony.radius - 0.6) {
			double scale = (colony.radius - 1.2) / Math.max(distance, 1e-3);
			Vec3 inward = new Vec3(-dx, 0, -dz).normalize();
			moveTo(entity, cx + dx * scale, entity.getY(), cz + dz * scale);
			entity.setDeltaMovement(inward.scale(0.4).add(0, 0.1, 0));
		} else {
			// Above the dome: brought down under it.
			double roof = colony.center.getY() + CullingMath.roofAt(distance, colony.radius);
			moveTo(entity, entity.getX(), roof - 2.5, entity.getZ());
			entity.setDeltaMovement(0, -0.3, 0);
		}

		entity.needsSync = true;
		if (entity instanceof ServerPlayer player) {
			long now = now(server);
			if (now - HELD_NOTICE.getOrDefault(player.getUUID(), -100L) > 60) {
				HELD_NOTICE.put(player.getUUID(), now);
				player.sendSystemMessage(Component.translatable("message.jjkcraft.culling.held"), true);
			}
		}
	}

	private static void moveTo(LivingEntity entity, double x, double y, double z) {
		if (entity instanceof ServerPlayer player) {
			player.teleportTo(x, y, z);
		} else {
			entity.setPos(x, y, z);
		}
	}

	private static void onAwakened(ServerPlayer player, boolean heavenly) {
		CullingState state = state(player.level().getServer());
		if (state.running && !heavenly && state.participant(player.getUUID()) == null) {
			state.deadlines.put(player.getUUID(), CullingMath.deadline(now(player.level().getServer()), config().entryWindowDays));
			player.sendSystemMessage(Component.translatable("message.jjkcraft.culling.window", config().entryWindowDays));
			state.changed();
		}
	}

	private static void onJoin(ServerPlayer player) {
		if (!enabled()) {
			return;
		}

		MinecraftServer server = player.level().getServer();
		CullingState state = state(server);
		if (!state.running && !state.ended && config().autoStart && canBeHunted(player)) {
			ServerLevel overworld = server.overworld();
			start(overworld, overworld.getRespawnData().globalPos().pos(), config().colonies, config().colonyRadius, config().colonyDistance);
		}

		if (state.running && canBeHunted(player) && state.participant(player.getUUID()) == null && !state.deadlines.containsKey(player.getUUID())) {
			state.deadlines.put(player.getUUID(), CullingMath.deadline(now(server), config().entryWindowDays));
			state.changed();
		}

		SENT.remove(player.getUUID());
	}

	// ---------------------------------------------------------------- kills and points

	/** Hardcore/spirit interception does not emit vanilla AFTER_DEATH, so it reports the kill here. */
	public static void recordSoulDeath(LivingEntity victim,DamageSource source) { onDeath(victim,source); }

	private static void onDeath(LivingEntity victim, DamageSource source) {
		if (!enabled() || !(victim.level() instanceof ServerLevel level)) {
			return;
		}

		MinecraftServer server = level.getServer();
		CullingState state = state(server);
		if (!state.running) {
			return;
		}

		long now = now(server);
		Participant leader = state.leader().orElse(null);
		Participant fallen = state.active(victim.getUUID());
		if (fallen != null) {
			if (fallen.npc) {
				fallen.status = CullingState.Status.ELIMINATED;
			} else {
				int before = fallen.points;
				fallen.points = CullingMath.afterDeath(fallen.points, config().deathPointLossPercent);
				state.log(now, "culling.jjkcraft.log.fell", fallen.name, String.valueOf(before - fallen.points));
			}

			state.changed();
		}

		LivingEntity killer = source.getEntity() instanceof LivingEntity living ? living : null;
		if(killer instanceof net.minecraft.world.entity.OwnableEntity ownable && ownable.getOwner() instanceof LivingEntity owner) killer=owner;
		if(killer instanceof CurseEntity controlled && com.jjkcraft.awakening.ControlledCurses.controlled(controlled)) {
			try { killer=server.getPlayerList().getPlayer(UUID.fromString(controlled.getAttachedOrCreate(com.jjkcraft.awakening.ControlledCurses.OWNER))); } catch(IllegalArgumentException ignored) { killer=null; }
		}
		Participant scorer = killer == null ? null : state.active(killer.getUUID());
		if (scorer == null || killer == victim) {
			return;
		}

		Colony colony = state.colony(scorer.colony).orElse(null);
		if (colony == null || !colony.contains(level.dimension(), victim.getX(), victim.getY(), victim.getZ())) {
			return;
		}

		RuleEvent event = new RuleEvent(RuleEvent.Kind.KILL, level, killer.getUUID());
		event.actorEntity = killer;
		event.target = victim.getUUID();
		event.targetEntity = victim;
		event.colony = colony.id;
		event.victimParticipant = fallen != null;
		event.victimSorcerer = fallen != null && fallen.sorcerer;
		event.victimLeader = fallen != null && leader != null && leader.id.equals(fallen.id);
		if (victim instanceof CurseEntity) {
			event.victimCurse = true;
			event.curseRank = Progression.gradeRank(level.registryAccess(), victim);
			event.curseSpecial = Progression.gradeOf(victim).flatMap(id -> Grades.get(level.registryAccess(), id))
					.map(GradeDefinition::special).orElse(false);
		}

		event.cause = source.is(JjkDamageTypes.TECHNIQUE) || source.is(JjkDamageTypes.TECHNIQUE_PIERCING) || source.is(JjkDamageTypes.BLACK_FLASH)
				|| source.is(JjkDamageTypes.SURE_HIT) ? RuleEvent.Cause.TECHNIQUE
				: source.is(DamageTypeTags.IS_PROJECTILE) ? RuleEvent.Cause.PROJECTILE : RuleEvent.Cause.MELEE;
		event.distance = killer.distanceTo(victim);
		event.night = level.isDarkOutside();
		fire(event);
	}

	/** Adds (or with a negative amount, takes) points. Public for commands and tests. */
	public static void addPoints(MinecraftServer server, UUID id, int delta, @Nullable Identifier rule) {
		CullingState state = state(server);
		Participant participant = state.active(id);
		if (participant == null || delta == 0) {
			return;
		}

		long now = now(server);
		int before = participant.points;
		participant.points = Math.max(0, participant.points + delta);
		ServerPlayer player = participant.npc ? null : find(server, id) instanceof ServerPlayer p ? p : null;
		if (delta > 0) {
			participant.earned += delta;
			participant.lastScoredAt = now;
			if (participant.energyCap < 1) {
				LivingEntity entity = player != null ? player : find(server, id) instanceof LivingEntity living ? living : null;
				if (entity != null) {
					CursedEnergy.checkpoint(entity);
				}

				participant.energyCap = 1;
				if (player != null) {
					player.sendSystemMessage(Component.translatable("message.jjkcraft.culling.unstuck"), true);
				}
			}

			if (player != null) {
				Progression.addCounter(player, ProgressionMath.CULLING_POINTS, delta);
			}
		}

		if (player != null) {
			JjkNetworking.send(player, new CullingPayloads.Event("points", List.of(String.valueOf(participant.points - before),
					rule == null ? "" : rule.toString())));
		}

		state.changed();
		if (participant.points != before) {
			RuleEvent event = new RuleEvent(RuleEvent.Kind.POINTS, server.overworld(), id);
			event.actorEntity = player;
			event.colony = participant.colony;
			event.pointsBefore = before;
			event.pointsAfter = participant.points;
			fire(event);
		}
	}

	/** Moves points without counting them as earned (ledgers, transfers). Public for items and tests. */
	public static void movePoints(MinecraftServer server, UUID id, int delta) {
		CullingState state = state(server);
		Participant participant = state.active(id);
		if (participant == null || delta == 0) {
			return;
		}

		participant.points = Math.max(0, participant.points + delta);
		state.changed();
	}

	/** Whether some rule in force lets a participant ask for {@code action}. */
	public static boolean allows(MinecraftServer server, RuleEvent.Action action) {
		CullingState state = state(server);
		for (CullingState.ActiveRule active : state.rules) {
			if (rule(server, active.id()).map(r -> r.allows(action)).orElse(false)) {
				return true;
			}
		}

		return false;
	}

	// ---------------------------------------------------------------- effects

	public static void removeTechnique(RuleEvent event) {
		if ("NONE".equals(config().missedEntryPenalty) || !(event.actorEntity instanceof ServerPlayer player)) {
			return;
		}

		TechniqueState before = TechniqueService.get(player);
		Optional<Identifier> innate = before.innate();
		innate.ifPresent(id -> TechniqueService.revoke(player, id));
		TechniqueState after = TechniqueService.get(player);
		TechniqueService.set(player, new TechniqueState(Optional.empty(), after.learned(), after.loadout(), after.selected(), after.mastery(),
				after.cooldowns()));
		state(player.level().getServer()).log(now(player.level().getServer()), "culling.jjkcraft.log.stripped", player.getName().getString());
		player.sendSystemMessage(Component.translatable("message.jjkcraft.culling.stripped"));
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(), JjkSounds.OVEREXERT, SoundSource.PLAYERS, 1.2f, 0.5f);
	}

	public static void transfer(RuleEvent event, int step, Identifier rule) {
		MinecraftServer server = event.level.getServer();
		CullingState state = state(server);
		Participant from = state.active(event.actor);
		Participant to = event.target == null ? null : state.active(event.target);
		if (from == null || to == null || from == to) {
			refuse(event, "culling.jjkcraft.refused.no_target");
			return;
		}

		int amount = CullingMath.transferable(event.amount, step, from.points);
		if (amount <= 0) {
			refuse(event, "culling.jjkcraft.refused.too_few");
			return;
		}

		from.points -= amount;
		to.points += amount;
		to.lastScoredAt = now(server);
		state.log(now(server), "culling.jjkcraft.log.transfer", from.name, to.name, String.valueOf(amount));
		notify(server, from.id, Component.translatable("message.jjkcraft.culling.gave", amount, to.name));
		notify(server, to.id, Component.translatable("message.jjkcraft.culling.received", amount, from.name));
		state.changed();
	}

	public static void reveal(RuleEvent event, int cost, int seconds, Identifier rule) {
		MinecraftServer server = event.level.getServer();
		CullingState state = state(server);
		Participant viewer = state.active(event.actor);
		Participant target = event.target == null ? null : state.participant(event.target);
		if (viewer == null || target == null) {
			refuse(event, "culling.jjkcraft.refused.no_target");
			return;
		}

		if (viewer.points < cost) {
			refuse(event, "culling.jjkcraft.refused.too_few");
			return;
		}

		viewer.points -= cost;
		REVEALS.computeIfAbsent(viewer.id, k -> new HashMap<>()).put(target.id, now(server) + seconds * 20L);
		String colony = state.colony(target.colony).map(c -> c.name).orElse("-");
		notify(server, viewer.id, Component.translatable("message.jjkcraft.culling.revealed", target.name,
				Component.translatableWithFallback(colony, colony), target.points));
		state.changed();
	}

	public static void substitute(RuleEvent event, int cost, Identifier rule) {
		MinecraftServer server = event.level.getServer();
		CullingState state = state(server);
		Participant participant = state.active(event.actor);
		Colony target = state.colony(event.colony).orElse(null);
		if (participant == null || target == null || target.id == participant.colony) {
			refuse(event, "culling.jjkcraft.refused.no_colony");
			return;
		}

		if (participant.points < cost) {
			refuse(event, "culling.jjkcraft.refused.too_few");
			return;
		}

		Colony old = state.colony(participant.colony).orElse(null);
		participant.points -= cost;
		participant.colony = target.id;
		if (old != null) {
			old.pending++;
		}

		state.log(now(server), "culling.jjkcraft.log.substitute", participant.name, target.name);
		state.changed();
		if (event.actorEntity != null) {
			confine(event.actorEntity, target.id);
		}

		notify(server, participant.id, Component.translatable("message.jjkcraft.culling.moved", Component.translatableWithFallback(target.name, target.name)));
	}

	public static void withdraw(RuleEvent event, int cost, Identifier rule) {
		MinecraftServer server = event.level.getServer();
		CullingState state = state(server);
		Participant participant = state.active(event.actor);
		if (participant == null) {
			refuse(event, "culling.jjkcraft.refused.not_participant");
			return;
		}

		if (participant.points < cost) {
			refuse(event, "culling.jjkcraft.refused.too_few");
			return;
		}

		participant.points -= cost;
		participant.status = CullingState.Status.WITHDRAWN;
		participant.energyCap = 1;
		state.log(now(server), "culling.jjkcraft.log.withdrew", participant.name);
		notify(server, participant.id, Component.translatable("message.jjkcraft.culling.withdrew"));
		state.changed();
	}

	public static void stagnate(RuleEvent event, double fraction) {
		MinecraftServer server = event.level.getServer();
		CullingState state = state(server);
		Participant participant = state.active(event.actor);
		if (participant == null || participant.energyCap <= fraction) {
			return;
		}

		LivingEntity entity = event.actorEntity != null ? event.actorEntity : find(server, participant.id) instanceof LivingEntity l ? l : null;
		if (entity != null) {
			CursedEnergy.checkpoint(entity);
		}

		participant.energyCap = fraction;
		notify(server, participant.id, Component.translatable("message.jjkcraft.culling.stagnant", Math.round(fraction * 100)));
		state.changed();
	}

	public static void revealLeader(RuleEvent event, int seconds) {
		MinecraftServer server = event.level.getServer();
		CullingState state = state(server);
		state.leader().ifPresent(leader -> {
			leaderRevealedUntil = now(server) + seconds * 20L;
			String colony = state.colony(leader.colony).map(c -> c.name).orElse("-");
			for (Participant participant : state.participants.values()) {
				if (participant.active()) {
					notify(server, participant.id, Component.translatable("message.jjkcraft.culling.leader", leader.name,
							Component.translatableWithFallback(colony, colony), leader.points));
				}
			}

			state.changed();
		});
	}

	public static void summonWarden(RuleEvent event) {
		MinecraftServer server = event.level.getServer();
		CullingState state = state(server);
		int colonyId = event.colony >= 0 ? event.colony : Optional.ofNullable(state.participant(event.actor)).map(p -> p.colony).orElse(-1);
		Colony colony = state.colony(colonyId).orElse(null);
		ServerLevel level = colony == null ? null : server.getLevel(colony.dimension);
		if (level == null) {
			return;
		}

		BlockPos spot = colony.center.offset(Mth.floor(colony.radius * 0.3), 0, 0);
		BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spot);
		CurseEntity warden = CurseSpawner.spawnOne(level, JjkCraft.id("culling_warden"), ground);
		if (warden != null) {
			warden.setPersistenceRequired();
			state.log(now(server), "culling.jjkcraft.log.warden", colony.name);
			state.changed();
		}
	}

	/** A message; its arguments are the actor's name, colony and points when the actor is a participant. */
	public static void message(RuleEvent event, String key, boolean broadcast) {
		MinecraftServer server = event.level.getServer();
		CullingState state = state(server);
		Participant actor = state.participant(event.actor);
		Component text = actor == null ? Component.translatable(key)
				: Component.translatable(key, actor.name, state.colony(actor.colony).map(c -> (Component) Component.translatableWithFallback(c.name, c.name))
						.orElse(Component.literal("-")), actor.points);
		if (broadcast) {
			for (Participant participant : state.participants.values()) {
				if (participant.active()) {
					notify(server, participant.id, text);
				}
			}
		} else {
			notify(server, event.actor, text);
		}
	}

	/** The Kogane hears a proposal: validates it, takes the points and adds the rule. */
	public static void negotiate(RuleEvent event) {
		MinecraftServer server = event.level.getServer();
		CullingState state = state(server);
		Participant participant = state.active(event.actor);
		if (participant == null) {
			refuse(event, "culling.jjkcraft.refused.not_participant");
			return;
		}

		if (event.actorEntity instanceof ServerPlayer player && !nearMaster(player)) {
			refuse(event, "culling.jjkcraft.refused.far");
			return;
		}

		Identifier id = event.rule;
		CullingRule rule = id == null ? null : rule(server, id).orElse(null);
		if (rule == null || rule.base()) {
			refuse(event, "culling.jjkcraft.refused.unknown");
			return;
		}

		if (state.hasRule(id)) {
			refuse(event, "culling.jjkcraft.refused.active");
			return;
		}

		for (CullingState.ActiveRule active : state.rules) {
			CullingRule other = rule(server, active.id()).orElse(null);
			if (rule.conflicts().contains(active.id()) || (other != null && other.conflicts().contains(id))) {
				refuse(event, "culling.jjkcraft.refused.conflict");
				return;
			}
		}

		int cost = rule.effectiveCost();
		if (participant.points < cost) {
			refuse(event, "culling.jjkcraft.refused.too_few");
			return;
		}

		long now = now(server);
		participant.points -= cost;
		state.rules.add(new CullingState.ActiveRule(id, Optional.of(participant.name), now,
				rule.durationDays() > 0 ? now + rule.durationDays() * CullingMath.DAY : CullingState.ActiveRule.NEVER));
		state.log(now, "culling.jjkcraft.log.rule_added", participant.name, rule.text());
		state.changed();
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (state.active(player.getUUID()) != null) {
				player.sendSystemMessage(Component.translatable("message.jjkcraft.culling.rule_added", participant.name, Component.translatable(rule.text())));
				JjkNetworking.send(player, new CullingPayloads.Event("rule_added", List.of(id.toString(), participant.name)));
			}
		}

		RuleEvent added = new RuleEvent(RuleEvent.Kind.ADDED, event.level, event.actor);
		added.actorEntity = event.actorEntity;
		added.colony = participant.colony;
		added.rule = id;
		fire(added);
	}

	private static void refuse(RuleEvent event, String key) {
		ServerPlayer player = event.level.getServer().getPlayerList().getPlayer(event.actor);
		if (player != null) {
			player.sendSystemMessage(Component.translatable(key));
			JjkNetworking.send(player, new CullingPayloads.Event("refused", List.of(key)));
		}
	}

	private static void notify(MinecraftServer server, UUID id, Component message) {
		ServerPlayer player = server.getPlayerList().getPlayer(id);
		if (player != null) {
			player.sendSystemMessage(message);
		}
	}

	// ---------------------------------------------------------------- requests

	/** Requests from the map: a handful a second at most. */
	private static final com.jjkcraft.core.RateLimiter LIMITER = new com.jjkcraft.core.RateLimiter(4, 6);

	private static void onAction(ServerPlayer player, CullingPayloads.Action payload) {
		if (!LIMITER.tryAcquire(player.getUUID(), player.level().getGameTime())) {
			return;
		}
		if (payload.action().equals("enter")) { enterColony(player,payload.colony()); return; }
		if (payload.action().equals("begin")) {
			if(enabled() && player.isAlive() && !player.isSpectator() && Progression.get(player).awakened() && !state(player.level().getServer()).running)
				start(player.level().getServer().overworld(),player.level().getServer().overworld().getRespawnData().globalPos().pos(),config().colonies,config().colonyRadius,config().colonyDistance);
			return;
		}

		RuleEvent.Action action;
		try {
			action = RuleEvent.Action.valueOf(payload.action().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException e) {
			return;
		}

		request(player, action, payload.target().orElse(null), payload.amount(), payload.rule().orElse(null), payload.colony());
	}

	/** Explicitly accepts the colony's rules before travel; spectators and existing entrants cannot use it to escape. */
	public static boolean enterColony(ServerPlayer player,int colonyId) {
		var server=player.level().getServer();var game=state(server);
		if(!enabled() || !game.running || !player.isAlive() || player.isSpectator() || game.active(player.getUUID())!=null)return false;
		var colony=game.colony(colonyId).orElse(null);var level=colony==null?null:server.getLevel(colony.dimension);if(level==null)return false;
		level.getChunkAt(colony.center);var ground=level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,colony.center);
		game.participants.remove(player.getUUID());colony.residents.remove(player.getUUID());
		player.teleportTo(level,ground.getX()+2.5,ground.getY()+1,ground.getZ()+2.5,Set.of(),player.getYRot(),0,true);
		join(player,colonyId);SENT.remove(player.getUUID());return game.active(player.getUUID())!=null;
	}

	/** A participant's request: allowed only if some rule in force answers it. Public for tests. */
	public static boolean request(ServerPlayer player, RuleEvent.Action action, @Nullable UUID target, int amount, @Nullable Identifier rule, int colony) {
		CullingState state = state(player.level().getServer());
		Participant participant = state.active(player.getUUID());
		RuleEvent event = new RuleEvent(RuleEvent.Kind.ACTION, player.level(), player.getUUID());
		if (!state.running || participant == null) {
			refuse(event, "culling.jjkcraft.refused.not_participant");
			return false;
		}

		event.actorEntity = player;
		event.action = action;
		event.target = target;
		event.targetEntity = target == null ? null : find(player.level().getServer(), target) instanceof LivingEntity l ? l : null;
		event.amount = amount;
		event.rule = rule;
		event.colony = action == RuleEvent.Action.SUBSTITUTE ? colony : participant.colony;
		if (fire(event) == 0) {
			refuse(event, "culling.jjkcraft.refused.no_rule");
			return false;
		}

		return true;
	}

	public static void openNegotiation(ServerPlayer player, KoganeEntity master) {
		CullingState state = state(player.level().getServer());
		if (!state.running) {
			player.sendSystemMessage(Component.translatable("culling.jjkcraft.refused.not_running"), true);
			return;
		}

		SENT.remove(player.getUUID());
		NEAR_MASTER.add(player.getUUID());
		syncAll(player.level().getServer(), state, false);
		JjkNetworking.send(player, new CullingPayloads.Event("open_negotiation", List.of(String.valueOf(master.colony()))));
	}

	// ---------------------------------------------------------------- hooks

	private static double capacity(LivingEntity entity, double capacity) {
		if (!(entity.level() instanceof ServerLevel level)) {
			return capacity;
		}

		CullingState state = state(level.getServer());
		Participant participant = state.running ? state.participant(entity.getUUID()) : null;
		return participant != null && participant.active() ? capacity * participant.energyCap : capacity;
	}

	/** Whether a rule in force binds a Warden standing in a colony. */
	private static Boolean bindsWarden(CurseEntity curse) {
		if (!(curse.level() instanceof ServerLevel level)) {
			return false;
		}

		CullingState state = state(level.getServer());
		if (!state.running || state.colonyAt(level.dimension(), curse.getX(), curse.getY(), curse.getZ()).isEmpty()) {
			return false;
		}

		for (CullingState.ActiveRule active : state.rules) {
			if (rule(level.getServer(), active.id()).map(CullingRule::bindsWarden).orElse(false)) {
				return true;
			}
		}

		return false;
	}

	/** The Edict named by the newest rule in force that names one, for a Warden inside a colony. */
	private static WardenMechanic.@Nullable Edict wardenEdict(CurseEntity curse) {
		if (!(curse.level() instanceof ServerLevel level)) {
			return null;
		}

		CullingState state = state(level.getServer());
		if (!state.running || state.colonyAt(level.dimension(), curse.getX(), curse.getY(), curse.getZ()).isEmpty()) {
			return null;
		}

		for (int i = state.rules.size() - 1; i >= 0; i--) {
			Optional<WardenMechanic.Edict> edict = rule(level.getServer(), state.rules.get(i).id()).flatMap(CullingRule::edict);
			if (edict.isPresent()) {
				return edict.get();
			}
		}

		return null;
	}

	/** Entrants fight participants of their own colony who stand inside it. */
	public static boolean isRival(EntrantEntity self, LivingEntity target) {
		if (target == self || !(self.level() instanceof ServerLevel level)) {
			return false;
		}

		CullingState state = state(level.getServer());
		Participant me = state.active(self.getUUID());
		Participant them = state.active(target.getUUID());
		if (me == null || them == null || me.colony != them.colony) {
			return false;
		}

		if (target instanceof EntrantEntity && self.getRandom().nextInt(3) != 0) {
			return false;
		}

		return state.colony(me.colony).map(c -> c.contains(level.dimension(), target.getX(), target.getY(), target.getZ())).orElse(false);
	}

	public static boolean nearMaster(ServerPlayer player) {
		CullingState state = state(player.level().getServer());
		// Every participant has a personal Kogane; M can negotiate away from the centre NPC.
		if(state.running && player.isAlive() && !player.isSpectator() && state.active(player.getUUID())!=null)return true;
		Optional<Colony> colony = state.colonyAt(player.level().dimension(), player.getX(), player.getY(), player.getZ());
		if (colony.isEmpty() || colony.get().master == null) {
			return false;
		}

		Entity master = player.level().getEntity(colony.get().master);
		return master != null && master.distanceToSqr(player) <= MASTER_RANGE * MASTER_RANGE;
	}

	private static @Nullable Entity find(MinecraftServer server, UUID id) {
		ServerPlayer player = server.getPlayerList().getPlayer(id);
		if (player != null) {
			return player;
		}

		LivingEntity seen = SEEN.get(id);
		if (seen != null && !seen.isRemoved()) {
			return seen;
		}

		for (ServerLevel level : server.getAllLevels()) {
			Entity entity = level.getEntity(id);
			if (entity != null) {
				return entity;
			}
		}

		return null;
	}

	// ---------------------------------------------------------------- sync

	private static void syncAll(MinecraftServer server, CullingState state, boolean force) {
		boolean reveals = force && (!REVEALS.isEmpty() || leaderRevealedUntil > now(server));
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			Integer sent = SENT.get(player.getUUID());
			if (sent == null || sent != state.version || reveals) {
				SENT.put(player.getUUID(), state.version);
				JjkNetworking.send(player, new CullingPayloads.Sync(view(server, state, player)));
			}
		}
	}

	/** What {@code viewer} may see of the game. Public for tests. */
	public static CullingPayloads.View view(MinecraftServer server, CullingState state, ServerPlayer viewer) {
		long now = now(server);
		UUID id = viewer.getUUID();
		Participant mine = state.participant(id);
		String status = mine == null ? "none" : mine.active() ? "participant" : mine.status.getSerializedName();
		if (mine == null) {
			for (Colony colony : state.colonies) {
				if (colony.residents.contains(id)) {
					status = "resident";
					break;
				}
			}
		}

		CullingPayloads.Me me = new CullingPayloads.Me(status, mine == null ? -1 : mine.colony, mine == null ? 0 : mine.points,
				mine == null ? 0 : mine.earned, state.deadlines.getOrDefault(id, -1L), NEAR_MASTER.contains(id), mine != null && mine.energyCap < 1);
		var config = config();
		List<Integer> numbers = List.of(config.entryWindowDays, config.sorcererPoints, config.nonSorcererPoints, config.ruleCost);

		List<CullingPayloads.ColonyView> colonies = new ArrayList<>();
		for (Colony colony : state.colonies) {
			int count = 0;
			int points = 0;
			for (Participant participant : state.participants.values()) {
				if (participant.active() && participant.colony == colony.id) {
					count++;
					points += participant.points;
				}
			}

			colonies.add(new CullingPayloads.ColonyView(colony.id, colony.name, colony.dimension, colony.center, colony.radius, count, points));
		}

		Map<UUID, Long> mineReveals = REVEALS.getOrDefault(id, Map.of());
		Participant leader = state.leader().orElse(null);
		boolean leaderShown = leaderRevealedUntil > now;
		List<CullingPayloads.Entry> board = new ArrayList<>();
		List<Participant> ranking = state.ranking();
		for (int i = 0; i < ranking.size(); i++) {
			Participant p = ranking.get(i);
			boolean you = p.id.equals(id);
			if (board.size() >= 40 && !you) {
				continue;
			}

			boolean isLeader = leader != null && leader.id.equals(p.id);
			boolean revealed = mineReveals.getOrDefault(p.id, -1L) > now || (isLeader && leaderShown);
			boolean known = you || revealed || (mine != null && mine.active() && mine.colony == p.colony);
			Optional<BlockPos> pos = Optional.empty();
			if (revealed) {
				Entity entity = find(server, p.id);
				if (entity != null) {
					pos = Optional.of(entity.blockPosition());
				}
			}

			board.add(new CullingPayloads.Entry(p.id, known ? p.name : "", known ? p.colony : -1, p.points, p.sorcerer, p.npc, you, isLeader, pos));
		}

		List<CullingPayloads.RuleView> rules = new ArrayList<>();
		for (CullingState.ActiveRule rule : state.rules) {
			rules.add(new CullingPayloads.RuleView(rule.id(), rule.addedBy(), rule.expiresAt()));
		}

		List<CullingState.LogLine> log = state.log.subList(Math.max(0, state.log.size() - 10), state.log.size());
		return new CullingPayloads.View(state.running, state.ended, state.startedAt, CullingMath.day(now, state.startedAt), me, numbers, colonies, board,
				rules, List.copyOf(log), List.copyOf(state.results));
	}
}
