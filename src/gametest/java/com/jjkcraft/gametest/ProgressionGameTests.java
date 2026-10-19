package com.jjkcraft.gametest;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Witch;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import com.jjkcraft.JjkCraft;
import com.jjkcraft.core.progress.ProgressionMath;
import com.jjkcraft.energy.CursedEnergy;
import com.jjkcraft.sorcerer.Awakening;
import com.jjkcraft.sorcerer.GrowthPath;
import com.jjkcraft.sorcerer.Progression;

public final class ProgressionGameTests {
	@GameTest
	public void promotionNeedsXpControlAndTrials(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.fresh(helper);
		Progression.addXp(player, 300);
		helper.assertValueEqual(Progression.get(player).grade(), JjkCraft.id("grade_4"), "still Grade 4 with only experience");

		for (int i = 0; i < 10; i++) {
			Progression.recordCast(player, 5, true);
		}

		helper.assertValueEqual(Progression.get(player).grade(), JjkCraft.id("grade_4"), "still Grade 4 without the trial");
		Progression.addCounter(player, ProgressionMath.curseKillKey(0), 5);
		helper.assertValueEqual(Progression.get(player).grade(), JjkCraft.id("grade_3"), "promoted to Grade 3");
		helper.assertTrue(Progression.get(player).trials().contains("jjkcraft:defeat_grade_4"), "trial recorded");
		helper.assertValueEqual(CursedEnergy.max(player), 140.0, "Grade 3 capacity");
		helper.assertTrue(Math.abs(CursedEnergy.current(player) - 140) < 1e-6, "promotion refills CE");
		helper.succeed();
	}

	@GameTest
	public void specialGradeOnlyThroughTheRitual(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.fresh(helper);
		Progression.setGrade(player, JjkCraft.id("grade_1"));
		Progression.addXp(player, 5_000_000);
		Progression.addCounter(player, ProgressionMath.curseKillKey(6), 500);
		helper.assertValueEqual(Progression.get(player).grade(), JjkCraft.id("grade_1"), "grinding never reaches Special Grade");

		Progression.addCounter(player, ProgressionMath.UNBOUND_RITUAL, 1);
		helper.assertValueEqual(Progression.get(player).grade(), JjkCraft.id("special_grade"), "the ritual unlocks Special Grade");
		helper.assertTrue(Progression.get(player).specialUnlocked(), "unlock flag stored");
		helper.succeed();
	}

	@GameTest
	public void heavenlyRestrictionTradesEnergyForBody(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.fresh(helper);
		double baseSpeed = player.getAttributeValue(Attributes.MOVEMENT_SPEED);
		double baseDamage = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
		Awakening.awaken(player, true);

		helper.assertTrue(Progression.get(player).heavenlyRestricted(), "rolled Heavenly Restriction");
		helper.assertValueEqual(CursedEnergy.max(player), 0.0, "no CE");
		helper.assertFalse(CursedEnergy.trySpend(player, 1), "cannot spend CE");
		helper.assertTrue(player.getAttributeValue(Attributes.MOVEMENT_SPEED) > baseSpeed, "faster");
		helper.assertTrue(player.getAttributeValue(Attributes.ATTACK_DAMAGE) >= baseDamage + 2, "stronger");
		helper.assertTrue(player.getAttributeValue(Attributes.ARMOR) >= 4, "tougher");

		Awakening.reset(player);
		helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.MOVEMENT_SPEED) - baseSpeed) < 1e-9, "bonuses removed after reset");
		helper.succeed();
	}

	@GameTest
	public void growthPointsAreLimitedAndApply(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.fresh(helper);
		Progression.addXp(player, 400);
		helper.assertValueEqual(Progression.unspentPoints(player.registryAccess(), Progression.get(player)), 2, "two points from 400 experience");
		helper.assertTrue(Progression.spendGrowth(player, GrowthPath.OUTPUT), "first point");
		helper.assertTrue(Progression.spendGrowth(player, GrowthPath.OUTPUT), "second point");
		helper.assertFalse(Progression.spendGrowth(player, GrowthPath.OUTPUT), "no third point");
		helper.assertTrue(Math.abs(CursedEnergy.max(player) - 106) < 1e-9, "Output adds 3% capacity per point");
		helper.succeed();
	}

	@GameTest
	public void killingCursedCapableMobsGivesExperience(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.fresh(helper);
		Witch witch = helper.spawn(EntityTypes.WITCH, 1, 1, 1);
		Progression.onKill(player, witch);
		helper.assertValueEqual(Progression.get(player).gradeXp(), ProgressionMath.killXp(0), "witch counts as Grade 4");
		helper.succeed();
	}
}
