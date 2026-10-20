package com.jjkcraft.gametest;

import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.item.ItemEntity;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import com.jjkcraft.JjkCraft;
import com.jjkcraft.block.JjkBlocks;
import com.jjkcraft.curse.CurseDefinition;
import com.jjkcraft.curse.CurseEntities;
import com.jjkcraft.curse.CurseEntity;
import com.jjkcraft.curse.Curses;
import com.jjkcraft.curse.mechanic.CurseMechanics;
import com.jjkcraft.curse.mechanic.GateMechanic;
import com.jjkcraft.curse.mechanic.TitheMechanic;
import com.jjkcraft.curse.mechanic.MimicMechanic;
import com.jjkcraft.curse.mechanic.DoorframeMechanic;
import com.jjkcraft.effect.JjkEffects;
import com.jjkcraft.energy.CursedEnergy;
import com.jjkcraft.fear.Fear;
import com.jjkcraft.grade.Grades;
import com.jjkcraft.network.payload.SensePayload;
import com.jjkcraft.sense.Sensing;
import com.jjkcraft.sorcerer.Progression;
import com.jjkcraft.technique.CastFailure;
import com.jjkcraft.technique.TechniqueService;
import com.jjkcraft.technique.Techniques;

public final class CurseGameTests {
	/** Spawns a curse the way the world does (stats, mechanic setup), then skips its emergence. */
	private static <T extends CurseEntity> T curse(GameTestHelper helper, EntityType<T> type, double x, double y, double z) {
		T curse = helper.spawn(type, x, y, z, EntitySpawnReason.MOB_SUMMONED);
		curse.getEntityData().set(CurseEntity.EMERGING, 0);
		return curse;
	}

	/**
	 * A target for curses to hit: a husk (no sunburn) held in place by slowness. It keeps its AI so
	 * physics runs and it stands on the ground; husks don't attack curses.
	 */
	private static Zombie dummy(GameTestHelper helper, double x, double y, double z) {
		Zombie husk = helper.spawn(EntityTypes.HUSK, new Vec3(x, y, z));
		husk.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 20 * 60, 9, false, false));
		return husk;
	}

	@GameTest
	public void everyCurseLoadsWithValidReferences(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		List<Curses.Entry> all = Curses.all(level.registryAccess());
		helper.assertTrue(all.size() >= 20, "the brief's 20 curses load, got " + all.size());
		for (Curses.Entry entry : all) {
			CurseDefinition def = entry.definition();
			helper.assertTrue(Grades.get(level.registryAccess(), def.grade()).isPresent(), entry.id() + " grade");
			helper.assertTrue(CurseMechanics.exists(def.mechanic()), entry.id() + " mechanic " + def.mechanic());
			EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(def.entity());
			helper.assertTrue(CurseEntities.ALL.contains(type), entry.id() + " entity type is a curse");
			for (var technique : def.techniques()) {
				helper.assertTrue(Techniques.get(level.registryAccess(), technique).isPresent(), entry.id() + " technique " + technique);
			}

			var table = level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, type.getDefaultLootTable().orElseThrow().identifier()));
			helper.assertTrue(table != LootTable.EMPTY, entry.id() + " has a loot table");
		}

		helper.succeed();
	}

	@GameTest
	public void spawnAppliesDataStats(GameTestHelper helper) {
		CurseEntity hymnal = helper.spawn(CurseEntities.HOLLOW_HYMNAL, 2, 2, 2, EntitySpawnReason.MOB_SUMMONED);
		helper.assertTrue(hymnal.isEmerging(), "emerges on spawn");
		helper.assertTrue(Math.abs(hymnal.getMaxHealth() - 16) < 1e-6, "health from data, got " + hymnal.getMaxHealth());
		helper.assertTrue(Math.abs(CursedEnergy.max(hymnal) - 40) < 1e-6, "CE capacity from data, got " + CursedEnergy.max(hymnal));
		helper.assertValueEqual(hymnal.grade(), JjkCraft.id("grade_4"), "grade from data");
		CurseEntity oni = curse(helper, CurseEntities.ASH_ONI, 5, 1, 5);
		helper.assertTrue(Math.abs(oni.getAttributeBaseValue(Attributes.ATTACK_DAMAGE) - 14) < 1e-6, "oni attack from data");
		helper.succeed();
	}

	@GameTest
	public void dataResistancesApply(GameTestHelper helper) {
		CurseEntity oni = curse(helper, CurseEntities.ASH_ONI, 2, 1, 2);
		float before = oni.getHealth();
		oni.hurtServer(helper.getLevel(), helper.getLevel().damageSources().inFire(), 6);
		helper.assertTrue(oni.getHealth() == before, "fire does nothing to the Ash Oni");
		helper.succeed();
	}

	@GameTest(maxTicks = 200)
	public void hymnalSingsAndStuns(GameTestHelper helper) {
		CurseEntity hymnal = curse(helper, CurseEntities.HOLLOW_HYMNAL, 2, 1, 2);
		Zombie zombie = dummy(helper, 2, 1, 4);
		hymnal.setTarget(zombie);
		helper.succeedWhen(() -> helper.assertTrue(zombie.hasEffect(JjkEffects.STUNNED), "zombie stunned by the hymn"));
	}

	@GameTest
	public void hitToTheMouthSilencesHymnal(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.fresh(helper);
		CurseEntity hymnal = curse(helper, CurseEntities.HOLLOW_HYMNAL, 2, 2, 2);
		hymnal.setYRot(0);
		hymnal.setYHeadRot(0);
		hymnal.setXRot(0);
		Vec3 front = hymnal.position().add(0, 0, 2);
		player.setPos(front.x, front.y, front.z);
		hymnal.hurtServer(helper.getLevel(), helper.getLevel().damageSources().playerAttack(player), 4);
		helper.assertValueEqual(hymnal.mechA(), 2, "silenced");
		helper.assertTrue(Math.abs(hymnal.getHealth() - 10) < 1e-3, "silencing hit deals 1.5x, health " + hymnal.getHealth());
		helper.succeed();
	}

	@GameTest
	public void ledgerTalliesDeaths(GameTestHelper helper) {
		CurseEntity ledger = curse(helper, CurseEntities.LEDGER_CRAWLER, 2, 1, 2);
		for (int i = 0; i < 3; i++) {
			dummy(helper, 4, 1, 1 + i).kill(helper.getLevel());
		}

		helper.assertValueEqual(ledger.mechA(), 3, "three marks");
		helper.assertTrue(Math.abs(ledger.mechanic().attackMultiplier(ledger, ledger) - 1.45) < 1e-6, "15% per mark");
		helper.succeed();
	}

	@GameTest(maxTicks = 80)
	public void stiltIsStrongOnlyAgainstAirbornePrey(GameTestHelper helper) {
		CurseEntity stilt = curse(helper, CurseEntities.PALE_STILT, 2, 1, 2);
		Zombie prey = dummy(helper, 2, 1, 5);
		stilt.setTarget(prey);
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(prey.onGround(), "prey on the ground"))
				.thenIdle(2)
				.thenExecute(() -> {
					helper.assertValueEqual(stilt.mechA(), 0, "weak while the prey is grounded");
					helper.assertTrue(Math.abs(stilt.mechanic().attackMultiplier(stilt, prey) - 0.6) < 1e-6, "feeble blows");
					prey.setDeltaMovement(0, 0.9, 0);
					prey.needsSync = true;
				})
				.thenWaitUntil(() -> helper.assertValueEqual(stilt.mechA(), 1, "empowered once the prey leaves the ground"))
				.thenExecute(() -> {
					helper.assertTrue(Math.abs(stilt.mechanic().attackMultiplier(stilt, prey) - 2.0) < 1e-6, "double blows on airborne prey");
					float taken = stilt.mechanic().onHurt(stilt, helper.getLevel(), helper.getLevel().damageSources().generic(), 10);
					helper.assertTrue(Math.abs(taken - 5) < 1e-4, "hard to hurt while empowered, took " + taken);
				})
				.thenSucceed();
	}

	@GameTest(maxTicks = 120)
	public void echoMimicReplaysYourActions(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.fresh(helper);
		CurseEntity echo = curse(helper, CurseEntities.ECHO_MIMIC, 3, 1, 3);
		Zombie target = dummy(helper, 3, 1, 6);
		echo.setTarget(target);
		TechniqueService.grant(player, JjkCraft.id("grudge_bolt"));
		helper.assertValueEqual(TechniqueService.cast(player, JjkCraft.id("grudge_bolt"), 0), CastFailure.NONE, "player casts");
		com.jjkcraft.sorcerer.PlayerActions.jumped(player);
		helper.assertValueEqual(com.jjkcraft.curse.mechanic.EchoMechanic.held(echo), 0, "it only listens to whoever it hunts");

		echo.setTarget(null);
		TechniqueService.resetCooldowns(player);
		TechniqueService.cast(player, JjkCraft.id("grudge_bolt"), 0);
		com.jjkcraft.sorcerer.PlayerActions.jumped(player);
		helper.assertValueEqual(com.jjkcraft.curse.mechanic.EchoMechanic.held(echo), 2, "cast and jump recorded");
		helper.assertTrue(echo.getTarget() == player, "it turns on the one it heard");
		echo.setTarget(target);
		double before = CursedEnergy.current(echo);
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertValueEqual(com.jjkcraft.curse.mechanic.EchoMechanic.held(echo), 0, "both actions played back"))
				.thenExecute(() -> helper.assertTrue(CursedEnergy.current(echo) < before + 5, "the copied bolt cost it CE"))
				.thenSucceed();
	}

	@GameTest(maxTicks = 120)
	public void saltLanternBeamBurnsItsTarget(GameTestHelper helper) {
		CurseEntity lantern = curse(helper, CurseEntities.SALT_LANTERN, 2, 1, 2);
		Zombie target = dummy(helper, 2, 1, 7);
		lantern.setTarget(target);
		float max = target.getHealth();
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertValueEqual(lantern.mechA(), 1, "gathers light first"))
				.thenWaitUntil(() -> helper.assertTrue(target.getHealth() < max, "the beam lands"))
				.thenExecute(() -> {
					helper.assertValueEqual(lantern.mechA(), 0, "light spent");
					lantern.setMechB(20);
					lantern.mechanic().onHurt(lantern, helper.getLevel(), helper.getLevel().damageSources().generic(), 8);
					helper.assertValueEqual(lantern.mechB(), -1, "a heavy blow snuffs the next shot");
				})
				.thenSucceed();
	}

	@GameTest
	public void tidewashTakesMoreWhenDry(GameTestHelper helper) {
		CurseEntity tide = curse(helper, CurseEntities.TIDEWASH, 2, 1, 2);
		tide.setMechA(0);
		float dry = tide.mechanic().onHurt(tide, helper.getLevel(), helper.getLevel().damageSources().generic(), 10);
		tide.setMechA(1);
		float wet = tide.mechanic().onHurt(tide, helper.getLevel(), helper.getLevel().damageSources().generic(), 10);
		helper.assertTrue(Math.abs(dry - 12.5f) < 1e-4 && Math.abs(wet - 10f) < 1e-4, "dry 12.5, wet 10; got " + dry + ", " + wet);
		helper.succeed();
	}

	@GameTest(maxTicks = 200)
	public void thornRootsBindTheMarkedTarget(GameTestHelper helper) {
		CurseEntity thorn = curse(helper, CurseEntities.THORN_MATRIARCH, 1, 1, 1);
		Zombie zombie = dummy(helper, 1, 1, 5);
		thorn.setTarget(zombie);
		helper.succeedWhen(() -> {
			helper.assertTrue(zombie.hasEffect(JjkEffects.BOUND), "zombie bound by roots");
			BlockPos at = zombie.blockPosition();
			boolean roots = BlockPos.betweenClosedStream(at.offset(-2, -1, -2), at.offset(2, 1, 2))
					.anyMatch(p -> helper.getLevel().getBlockState(p).is(JjkBlocks.CURSED_ROOT));
			helper.assertTrue(roots, "roots ring the mark");
		});
	}

	@GameTest(maxTicks = 200)
	public void bellWaveHitsGroundedTargets(GameTestHelper helper) {
		CurseEntity bell = curse(helper, CurseEntities.CRACKED_BELL, 2, 2, 2);
		bell.setNoGravity(true);
		Zombie zombie = dummy(helper, 2, 1, 6);
		bell.setTarget(zombie);
		float max = zombie.getHealth();
		helper.succeedWhen(() -> {
			// The wave itself, not a swing of the bell: the zombie is in the rolling wave's hit list.
			helper.assertTrue(bell.memory.get("wave_hit") instanceof Set<?> hit && hit.contains(zombie.getUUID()), "wave hit the zombie");
			helper.assertTrue(zombie.getHealth() < max, "and hurt it");
		});
	}

	@GameTest
	public void saintIsDisguisedUntilStruck(GameTestHelper helper) {
		CurseEntity saint = curse(helper, CurseEntities.REDACTED_SAINT, 2, 1, 2);
		helper.assertTrue(saint.isDisguised(), "disguised on spawn");
		helper.assertFalse(saint.mechanic().canAttack(saint), "can't attack while disguised");
		saint.hurtServer(helper.getLevel(), helper.getLevel().damageSources().generic(), 1);
		helper.assertFalse(saint.isDisguised(), "revealed when struck");
		helper.assertTrue(saint.mechanic().attackMultiplier(saint, saint) == 2.0, "first strike doubled");
		helper.succeed();
	}

	@GameTest
	public void oniStaggersUnderHeavyBlows(GameTestHelper helper) {
		CurseEntity oni = curse(helper, CurseEntities.ASH_ONI, 2, 1, 2);
		for (int i = 0; i < 3; i++) {
			oni.mechanic().onHurt(oni, helper.getLevel(), helper.getLevel().damageSources().generic(), 12);
		}

		helper.assertTrue(com.jjkcraft.curse.mechanic.OniMechanic.staggered(oni), "36 damage at once staggers it");
		helper.assertFalse(oni.mechanic().canAct(oni), "a staggered oni can't act");
		float taken = oni.mechanic().onHurt(oni, helper.getLevel(), helper.getLevel().damageSources().generic(), 10);
		helper.assertTrue(Math.abs(taken - 15) < 1e-4, "and takes 1.5x, took " + taken);
		helper.succeed();
	}

	@GameTest(maxTicks = 200)
	public void oniChargeSweepsAside(GameTestHelper helper) {
		// Both stay on the test structure's floor (about 8 blocks across).
		CurseEntity oni = curse(helper, CurseEntities.ASH_ONI, 1, 1, 1);
		Zombie target = dummy(helper, 1, 1, 7);
		oni.setTarget(target);
		float max = target.getHealth();
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(target.getHealth() < max && target.getRemainingFireTicks() > 0, "the sweep burns and throws"))
				.thenSucceed();
	}

	@GameTest(maxTicks = 200)
	public void gateShowsBarAndCallsSummons(GameTestHelper helper) {
		CurseEntity gate = curse(helper, CurseEntities.GRAVEBOUND_GATE, 4, 1, 4);
		Zombie zombie = dummy(helper, 4, 1, 8);
		gate.setTarget(zombie);
		helper.assertTrue(gate.isPersistenceRequired(), "the gate never despawns");
		helper.succeedWhen(() -> {
			helper.assertTrue(gate.memory.get("bar") instanceof ServerBossEvent, "boss bar");
			long summons = helper.getLevel().getEntitiesOfClass(CurseEntity.class, gate.getBoundingBox().inflate(16), c -> GateMechanic.isMinionOf(gate, c)).size();
			helper.assertTrue(summons > 0, "summoned at least one curse");
		});
	}

	@GameTest
	public void killingCurseAdvancesCodex(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.fresh(helper);
		CurseEntity hymnal = curse(helper, CurseEntities.HOLLOW_HYMNAL, 2, 1, 2);
		hymnal.hurtServer(helper.getLevel(), helper.getLevel().damageSources().playerAttack(player), 1000);
		helper.assertValueEqual(Progression.get(player).counter(Curses.codexKey(JjkCraft.id("hollow_hymnal"))), 1, "codex counter");
		helper.succeed();
	}

	@GameTest
	public void sensingFindsCursesInRange(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.fresh(helper);
		CurseEntity near = curse(helper, CurseEntities.WICK_MOTH, 3, 1, 3);
		List<SensePayload.Sensed> sensed = Sensing.sense(player, 16);
		helper.assertTrue(sensed.stream().anyMatch(s -> s.entityId() == near.getId() && s.curse()), "curse sensed");
		helper.assertTrue(sensed.stream().noneMatch(s -> s.entityId() == near.getId() && s.color() == 0), "sensed with a grade color");
		helper.assertTrue(Sensing.sense(player, 0.5).isEmpty(), "nothing in a tiny range");
		helper.succeed();
	}

	@GameTest
	public void fearAddsAndStores(GameTestHelper helper) {
		BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
		double before = Fear.stored(helper.getLevel(), pos);
		Fear.add(helper.getLevel(), pos, 30);
		helper.assertTrue(Math.abs(Fear.stored(helper.getLevel(), pos) - Math.min(100, before + 30)) < 0.5, "stored fear rose by 30");
		helper.assertTrue(Fear.at(helper.getLevel(), pos) >= Fear.stored(helper.getLevel(), pos), "total includes stored");
		helper.succeed();
	}

	private static void door(GameTestHelper helper, BlockPos at, boolean open) {
		BlockState lower = Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER).setValue(DoorBlock.OPEN, open);
		helper.setBlock(at, lower);
		helper.setBlock(at.above(), lower.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
	}

	@GameTest(maxTicks = 240,environment="jjkcraft:door_ambush")
	public void doorframeHunterHidesInADoorAndAmbushes(GameTestHelper helper) {
		BlockPos at = new BlockPos(5, 1, 5);
		door(helper, at, false);
		CurseEntity hunter = curse(helper, CurseEntities.DOORFRAME_HUNTER, 3.5, 1, 5.5);
		// This scenario tests concealment, not random strolling away from the nearby frame.
		hunter.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).setBaseValue(0);
		// Other tests share the world: a stray bolt or blast from next door would reveal the hunter and
		// restart its calm count, so the test keeps it unhurt and unprovoked. The damage check below
		// calls the mechanic directly.
		hunter.setPermanentlyInvulnerable(true);
		helper.startSequence()
				.thenWaitUntil(() -> {
					String target = hunter.getTarget() == null ? "none" : hunter.getTarget().getType().toString();
					hunter.setTarget(null);
					hunter.setLastHurtByMob(null);
					helper.assertValueEqual(hunter.mechA(), DoorframeMechanic.HIDING, "it steps into the frame (target was " + target + ")");
				})
				.thenExecute(() -> {
					helper.assertTrue(hunter.isInvisible(), "and vanishes");
					helper.assertTrue(hunter.getBlockX() == helper.absolutePos(at).getX() && hunter.getBlockZ() == helper.absolutePos(at).getZ(), "inside the door's frame");
					door(helper, at, true);
				})
				.thenWaitUntil(() -> helper.assertValueEqual(hunter.mechA(), DoorframeMechanic.OUT, "opening the door springs it"))
				.thenExecute(() -> {
					helper.assertFalse(hunter.isInvisible(), "visible again");
					helper.assertTrue(Math.abs(hunter.mechanic().attackMultiplier(hunter, hunter) - 1.8) < 1e-6, "its first blow is an ambush");
					helper.setBlock(at, Blocks.AIR);
					helper.setBlock(at.above(), Blocks.AIR);
					float taken = hunter.mechanic().onHurt(hunter, helper.getLevel(), helper.getLevel().damageSources().generic(), 10);
					helper.assertTrue(Math.abs(taken - 13) < 1e-3, "away from every door it is exposed, took " + taken);
				})
				.thenSucceed();
	}

	@GameTest(maxTicks = 120)
	public void gildedMimicWaitsAsAChestThenGrabs(GameTestHelper helper) {
		CurseEntity mimic = curse(helper, CurseEntities.GILDED_MIMIC, 2, 1, 2);
		Zombie victim = dummy(helper, 2, 1, 5);
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(mimic.isDisguised(), "it waits as a chest"))
				.thenExecute(() -> {
					helper.assertFalse(mimic.mechanic().canAct(mimic), "a chest does nothing");
					MimicMechanic.spring(mimic, helper.getLevel(), victim);
					helper.assertFalse(mimic.isDisguised(), "it springs");
					helper.assertTrue(MimicMechanic.grabbed(mimic) == victim, "and its tongue grabs");
				})
				.thenIdle(10)
				.thenExecute(() -> {
					helper.assertTrue(victim.distanceTo(mimic) < 3, "the victim is dragged in, at " + victim.distanceTo(mimic));
					mimic.mechanic().onHurt(mimic, helper.getLevel(), helper.getLevel().damageSources().generic(), 7);
					helper.assertTrue(MimicMechanic.grabbed(mimic) == null, "a hard hit breaks the grip");
				})
				.thenSucceed();
	}

	@GameTest(maxTicks = 200,padding=32,environment="jjkcraft:tithe_payment")
	public void titheBurrowerStealsAndIsPaidOff(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.fresh(helper);
		player.getInventory().setItem(9, new ItemStack(Items.DIAMOND, 5));
		CurseEntity burrower = curse(helper, CurseEntities.TITHE_BURROWER, 3, 1, 3);
        for(int x=-12;x<=18;x++)for(int z=-12;z<=18;z++)helper.setBlock(new BlockPos(x,0,z),net.minecraft.world.level.block.Blocks.STONE);

		helper.startSequence()
				.thenIdle(2)
				.thenExecute(() -> {
					TitheMechanic.setWanted(burrower, Items.EMERALD);
					burrower.mechanic().onAttack(burrower, helper.getLevel(), player);
					helper.assertTrue(player.getInventory().getItem(9).isEmpty(), "the diamonds were stolen");
					helper.assertValueEqual(burrower.stash().size(), 1, "into its stash");
					helper.assertValueEqual(burrower.mechA(), 1, "and it burrowed");
					helper.assertTrue(burrower.mechanic().onHurt(burrower, helper.getLevel(), helper.getLevel().damageSources().generic(), 10) == 0,
							"untouchable underground");
				})
				.thenWaitUntil(() -> helper.assertValueEqual(burrower.mechA(), 0, "it surfaces again"))
				.thenExecute(() -> {
					// Payment is the subject here; hold both participants after the random resurfacing.
					// A resurface can leave the tiny GameTest structure's entity-ticking chunk.
					Vec3 paymentSpot=Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(3,1,3)));
					burrower.teleportTo(paymentSpot.x,paymentSpot.y,paymentSpot.z);
					burrower.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0);
					burrower.setDeltaMovement(Vec3.ZERO);
					ItemEntity tithe = new ItemEntity(helper.getLevel(), burrower.getX(), burrower.getY() + 0.2, burrower.getZ(), new ItemStack(Items.EMERALD));
					tithe.setNoGravity(true);tithe.setDeltaMovement(Vec3.ZERO);
					helper.getLevel().addFreshEntity(tithe);
				})
				.thenWaitUntil(() -> helper.assertTrue(burrower.isRemoved(), "paid, it leaves"))
				.thenExecute(() -> helper.assertTrue(!helper.getLevel().getEntitiesOfClass(ItemEntity.class, burrower.getBoundingBox().inflate(6),
						item -> item.getItem().is(Items.DIAMOND) && item.getItem().getCount() == 5).isEmpty(), "and gives everything back"))
				.thenSucceed();
	}

	@GameTest
	public void titheBurrowerStashSpillsWhenItDies(GameTestHelper helper) {
		CurseEntity burrower = curse(helper, CurseEntities.TITHE_BURROWER, 3, 1, 3);
		burrower.stash().add(new ItemStack(Items.GOLD_INGOT, 3));
		burrower.hurtServer(helper.getLevel(), helper.getLevel().damageSources().generic(), 10_000f);
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertFalse(helper.getLevel().getEntitiesOfClass(ItemEntity.class,
						new net.minecraft.world.phys.AABB(helper.absolutePos(new BlockPos(3, 1, 3))).inflate(4),
						item -> item.getItem().is(Items.GOLD_INGOT)).isEmpty(), "the stash spills out"))
				.thenSucceed();
	}
}
