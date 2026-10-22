package com.jjkcraft.gametest;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import com.jjkcraft.JjkCraft;
import com.jjkcraft.core.progress.ProgressionMath;
import com.jjkcraft.damage.DamageModifiers;
import com.jjkcraft.effect.JjkEffects;
import com.jjkcraft.energy.CursedEnergy;
import com.jjkcraft.grade.Grades;
import com.jjkcraft.sorcerer.Awakening;
import com.jjkcraft.sorcerer.Progression;
import com.jjkcraft.technique.CastFailure;
import com.jjkcraft.technique.TechniqueDefinition;
import com.jjkcraft.technique.TechniqueService;
import com.jjkcraft.technique.Techniques;
import com.jjkcraft.technique.field.SpatialFields;

public final class TechniqueGameTests {
	private static final double EPS = 1e-6;

	/** A player at the test origin looking along +Z (yaw 0), facing a still zombie three blocks ahead. */
	private static Zombie target(GameTestHelper helper, ServerPlayer player, double distance) {
		BlockPos base = helper.absolutePos(new BlockPos(1, 1, 1));
		player.setPos(base.getX() + 0.5, base.getY(), base.getZ() + 0.5);
		player.setYRot(0);
		player.setXRot(0);
		player.setYHeadRot(0);
		Zombie zombie = helper.spawn(EntityTypes.ZOMBIE, new Vec3(1.5, 1, 1.5 + distance));
		zombie.setNoAi(true);
		return zombie;
	}

	@GameTest
	public void everyTechniqueLoadsWithValidGrade(GameTestHelper helper) {
		List<Techniques.Entry> all = Techniques.all(helper.getLevel().registryAccess());
		helper.assertTrue(all.size() >= 15, "at least 15 techniques loaded, got " + all.size());
		for (Techniques.Entry entry : all) {
			TechniqueDefinition def = entry.definition();
			helper.assertTrue(Grades.get(helper.getLevel().registryAccess(), def.grade()).isPresent(),
					entry.id() + " references a missing grade");
			helper.assertTrue(def.icon().getPath().startsWith("textures/gui/technique/"), entry.id() + " icon path");
		}

		helper.succeed();
	}

	@GameTest
	public void strikeSpendsEnergyDamagesAndCoolsDown(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.fresh(helper);
		Zombie zombie = target(helper, player, 2.5);
		TechniqueService.grant(player, JjkCraft.id("ink_iron_palm"));
		float before = zombie.getHealth();

		helper.assertValueEqual(TechniqueService.cast(player, JjkCraft.id("ink_iron_palm"), 0), CastFailure.NONE, "first cast");
		// A Black Flash (a few percent per strike) refunds 15% of max CE on top.
		boolean flash = Progression.get(player).counters().getOrDefault(com.jjkcraft.blackflash.BlackFlash.COUNTER, 0) > 0;
		double expected = flash ? Math.min(100, 90 + 15) : 90;
		helper.assertTrue(Math.abs(CursedEnergy.current(player) - expected) < EPS, "palm costs 10 CE, have " + CursedEnergy.current(player));
		helper.assertTrue(zombie.getHealth() < before, "zombie took damage");
		helper.assertValueEqual(TechniqueService.cast(player, JjkCraft.id("ink_iron_palm"), 0), CastFailure.ON_COOLDOWN, "second cast");
		helper.assertValueEqual(Progression.get(player).castsLanded(), 1, "landed cast recorded for Control");
		helper.succeed();
	}

	@GameTest
	public void serverRefusesInvalidCasts(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.fresh(helper);
		helper.assertValueEqual(TechniqueService.cast(player, JjkCraft.id("grudge_bolt"), 0), CastFailure.NOT_KNOWN, "unknown technique");

		TechniqueService.grant(player, JjkCraft.id("hollow_purple"));
		helper.assertValueEqual(TechniqueService.cast(player, JjkCraft.id("hollow_purple"), 0), CastFailure.GRADE_TOO_LOW, "grade check");

		TechniqueService.grant(player, JjkCraft.id("grudge_bolt"));
		CursedEnergy.set(player, 5);
		helper.assertValueEqual(TechniqueService.cast(player, JjkCraft.id("grudge_bolt"), 0), CastFailure.NOT_ENOUGH_ENERGY, "CE check");
		helper.assertTrue(Math.abs(CursedEnergy.current(player) - 5) < 0.1, "refused cast spends nothing");

		Awakening.awaken(player, true);
		CursedEnergy.set(player, 100);
		helper.assertValueEqual(TechniqueService.cast(player, JjkCraft.id("grudge_bolt"), 0), CastFailure.HEAVENLY_RESTRICTION, "HR check");
		helper.succeed();
	}

	@GameTest
	public void chainsBindTheTarget(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.fresh(helper);
		Zombie zombie = target(helper, player, 4);
		TechniqueService.grant(player, JjkCraft.id("grave_chains"));
		helper.assertValueEqual(TechniqueService.cast(player, JjkCraft.id("grave_chains"), 0), CastFailure.NONE, "cast");
		helper.assertTrue(zombie.hasEffect(JjkEffects.BOUND), "zombie is bound");
		helper.succeed();
	}

	@GameTest
	public void dontMoveHaltsEveryoneInSight(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.fresh(helper);
		Zombie zombie = target(helper, player, 3);
		TechniqueService.grant(player, JjkCraft.id("cursed_speech_dont_move"));
		helper.assertValueEqual(TechniqueService.cast(player, JjkCraft.id("cursed_speech_dont_move"), 0), CastFailure.NONE, "cast");
		helper.assertTrue(zombie.hasEffect(JjkEffects.BOUND), "zombie obeys");
		helper.succeed();
	}

	@GameTest(maxTicks = 40)
	public void boltFliesAndHits(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.fresh(helper);
		Zombie zombie = target(helper, player, 5);
		float before = zombie.getHealth();
		TechniqueService.grant(player, JjkCraft.id("grudge_bolt"));
		helper.assertValueEqual(TechniqueService.cast(player, JjkCraft.id("grudge_bolt"), 0), CastFailure.NONE, "cast");
		helper.succeedWhen(() -> helper.assertTrue(zombie.getHealth() < before, "bolt reached the zombie"));
	}

	@GameTest
	public void barrierBlocksFrontalDamage(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.fresh(helper);
		Zombie zombie = target(helper, player, 2);
		TechniqueService.grant(player, JjkCraft.id("lacquer_ward"));
		helper.assertValueEqual(TechniqueService.cast(player, JjkCraft.id("lacquer_ward"), 0), CastFailure.NONE, "raise barrier");
		helper.assertTrue(TechniqueService.active(player).isPresent(), "barrier is channelled");

		double ceBefore = CursedEnergy.current(player);
		float through = DamageModifiers.apply(helper.getLevel(), player, helper.getLevel().damageSources().mobAttack(zombie), 10f);
		helper.assertTrue(Math.abs(through - 1.6f) < 0.01, "buffed starter barrier blocks 84% of frontal damage, got " + through);
		helper.assertTrue(ceBefore - CursedEnergy.current(player) > 4, "blocking costs CE");

		TechniqueService.stop(player);
		helper.assertTrue(TechniqueService.active(player).isEmpty(), "barrier lowered");
		float unblocked = DamageModifiers.apply(helper.getLevel(), player, helper.getLevel().damageSources().mobAttack(zombie), 10f);
		helper.assertTrue(Math.abs(unblocked - 10f) < 0.01, "no barrier, no reduction");
		helper.succeed();
	}

	@GameTest(maxTicks = 60)
	public void healingChannelTurnsEnergyIntoHealth(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.fresh(helper);
		Progression.setGrade(player, JjkCraft.id("grade_3"));
		CursedEnergy.fill(player);
		player.setHealth(8);
		com.jjkcraft.reverse.ReverseTechnique.set(player,com.jjkcraft.reverse.ReverseTechnique.state(player).withUnlocked(true),false);
        TechniqueService.grant(player, JjkCraft.id("reverse_cursed_technique"));
		helper.assertValueEqual(TechniqueService.cast(player, JjkCraft.id("reverse_cursed_technique"), 0), CastFailure.NONE, "start healing");
		helper.runAfterDelay(20, () -> {
			helper.assertTrue(player.getHealth() > 10, "healed, health " + player.getHealth());
			helper.assertTrue(CursedEnergy.current(player) < 135, "healing spent CE");
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 40)
	public void attractAndRepelCollideIntoABurst(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.fresh(helper);
		Zombie zombie = target(helper, player, 6);
		Progression.setGrade(player, JjkCraft.id("grade_3"));
		CursedEnergy.fill(player);
		TechniqueService.grant(player, JjkCraft.id("lapse_blue"));
		TechniqueService.grant(player, JjkCraft.id("reversal_red"));
		float before = zombie.getHealth();
		helper.assertValueEqual(TechniqueService.cast(player, JjkCraft.id("lapse_blue"), 0), CastFailure.NONE, "attract");
		helper.assertValueEqual(TechniqueService.cast(player, JjkCraft.id("reversal_red"), 1), CastFailure.NONE, "repel");
		helper.startSequence().thenWaitUntil(()->helper.assertTrue(zombie.getHealth() < before - 10, "traveling fields arrive and collide, health " + zombie.getHealth())).thenExecute(()->helper.assertTrue(SpatialFields.active(helper.getLevel()).stream().noneMatch(f->f.caster()==player), "own fields consumed by the burst")).thenSucceed();
	}

	@GameTest
	public void awakeningRollsAnInnateTechnique(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.fresh(helper);
		Awakening.awaken(player, false);
		helper.assertTrue(TechniqueService.get(player).innate().isPresent(), "innate technique rolled");
		helper.assertTrue(TechniqueService.get(player).slot(0).isPresent(), "innate technique slotted");
		helper.succeed();
	}

	@GameTest
	public void masteryCountsTowardTrials(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.fresh(helper);
		Zombie zombie = target(helper, player, 2.5);
		TechniqueService.grant(player, JjkCraft.id("ink_iron_palm"));
		TechniqueService.set(player, TechniqueService.get(player).withMasteryAdded(JjkCraft.id("ink_iron_palm"), 59));
		TechniqueService.cast(player, JjkCraft.id("ink_iron_palm"), 0);
		helper.assertTrue(TechniqueService.isMastered(player, JjkCraft.id("ink_iron_palm"),
				Techniques.get(helper.getLevel().registryAccess(), JjkCraft.id("ink_iron_palm")).orElseThrow()), "mastered");
		helper.assertValueEqual(Progression.get(player).counter(ProgressionMath.MASTERED), 1, "mastered counter");
		helper.assertTrue(zombie.getHealth() < zombie.getMaxHealth(), "the mastering hit landed");
		helper.succeed();
	}
}
