package com.jjkcraft.gametest;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.animal.cow.Cow;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import com.jjkcraft.JjkCraft;
import com.jjkcraft.attachment.JjkAttachments;
import com.jjkcraft.energy.CursedEnergy;
import com.jjkcraft.sorcerer.SorcererLifecycle;
import com.jjkcraft.sorcerer.SorcererProgress;

public final class EnergyGameTests {
	private static final double EPS = 1e-6;

	@GameTest
	public void spendDeductsAndFailsWhenShort(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.fresh(helper);
		helper.assertValueEqual(CursedEnergy.max(player), 100.0, "grade 4 max CE");
		CursedEnergy.set(player, 50);
		helper.assertTrue(CursedEnergy.trySpend(player, 30), "first spend succeeds");
		helper.assertTrue(Math.abs(CursedEnergy.current(player) - 20) < EPS, "20 CE left");
		helper.assertFalse(CursedEnergy.trySpend(player, 30), "second spend fails");
		helper.assertTrue(Math.abs(CursedEnergy.current(player) - 20) < EPS, "failed spend changes nothing");
		helper.succeed();
	}

	@GameTest(maxTicks = 60)
	public void spendingBelowThresholdOverexerts(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.fresh(helper);
		CursedEnergy.set(player, 15);
		helper.assertTrue(CursedEnergy.trySpend(player, 10), "spend succeeds");
		helper.assertTrue(CursedEnergy.isOverexerted(player), "player is overexerted");
		double atStart = CursedEnergy.current(player);
		helper.runAfterDelay(20, () -> {
			helper.assertTrue(Math.abs(CursedEnergy.current(player) - atStart) < EPS, "no regen while overexerted");
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 200)
	public void energyRegeneratesOverTime(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.fresh(helper);
		CursedEnergy.set(player, 0);
		helper.runAfterDelay(40, () -> {
			double now = CursedEnergy.current(player);
			helper.assertTrue(now > 2.9 && now < 100, "regenerated about 3 CE in 2 s, got " + now);
			helper.succeed();
		});
	}

	@GameTest
	public void profiledMobHasEnergyAndOthersDoNot(GameTestHelper helper) {
		Witch witch = helper.spawn(EntityTypes.WITCH, 1, 1, 1);
		Cow cow = helper.spawn(EntityTypes.COW, 1, 1, 1);
		helper.assertValueEqual(CursedEnergy.max(witch), 60.0, "witch capacity from energy_profile/minor_casters");
		helper.assertFalse(CursedEnergy.isCapable(cow), "cows have no CE");
		helper.assertTrue(CursedEnergy.trySpend(witch, 20), "witch can spend");
		helper.assertTrue(Math.abs(CursedEnergy.current(witch) - 40) < EPS, "witch has 40 left");
		helper.succeed();
	}

	@GameTest
	public void gradeRaisesCapacity(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.fresh(helper);
		player.setAttached(JjkAttachments.PROGRESS, SorcererProgress.DEFAULT.withGrade(JjkCraft.id("grade_1"), 0));
		helper.assertValueEqual(CursedEnergy.max(player), 360.0, "grade 1 max CE");
		player.setAttached(JjkAttachments.PROGRESS, SorcererProgress.DEFAULT.withHeavenlyRestriction(true));
		helper.assertValueEqual(CursedEnergy.max(player), 0.0, "Heavenly Restriction has no CE");
		helper.succeed();
	}

	@GameTest
	public void deathKeepsProgressAndRefillsHalf(GameTestHelper helper) {
		ServerPlayer oldPlayer = TestPlayers.fresh(helper);
		ServerPlayer newPlayer = TestPlayers.fresh(helper);
		SorcererProgress progress = SorcererProgress.DEFAULT.withGrade(JjkCraft.id("grade_2"), 500);
		oldPlayer.setAttached(JjkAttachments.PROGRESS, progress);
		CursedEnergy.set(oldPlayer, 3);

		SorcererLifecycle.copyOnDeath(oldPlayer, newPlayer);

		helper.assertValueEqual(newPlayer.getAttached(JjkAttachments.PROGRESS), progress, "progress after death (KEEP)");
		double expected = CursedEnergy.max(newPlayer) * 0.5;
		helper.assertTrue(Math.abs(CursedEnergy.current(newPlayer) - expected) < EPS, "respawn CE is 50% of max");
		helper.succeed();
	}
}
