package com.jjkcraft.sorcerer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.jjkcraft.JjkCraft;
import com.jjkcraft.config.DeathPolicy;
import com.jjkcraft.config.JjkServerConfig;

class DeathPolicyTest {
	private static final SorcererProgress PROGRESS = SorcererProgress.DEFAULT
			.withGrade(JjkCraft.id("grade_2"), 1000)
			.withTrial("jjkcraft:defeat_grade_3");

	@Test
	void keepChangesNothing() {
		assertEquals(PROGRESS, SorcererLifecycle.applyPolicy(PROGRESS, DeathPolicy.KEEP, new JjkServerConfig.Death(), 900));
	}

	@Test
	void partialLosesXpInsideTheCurrentGradeOnly() {
		JjkServerConfig.Death config = new JjkServerConfig.Death();
		config.partialXpLossPercent = 40;
		// 1000 total, grade floor 900: 100 above the floor, 40% of that is lost.
		SorcererProgress after = SorcererLifecycle.applyPolicy(PROGRESS, DeathPolicy.PARTIAL, config, 900);
		assertEquals(960, after.gradeXp());
		assertEquals(PROGRESS.grade(), after.grade());
		assertEquals(PROGRESS.trials(), after.trials());
	}

	@Test
	void partialNeverDropsBelowTheGradeFloor() {
		JjkServerConfig.Death config = new JjkServerConfig.Death();
		config.partialXpLossPercent = 100;
		assertEquals(900, SorcererLifecycle.applyPolicy(PROGRESS, DeathPolicy.PARTIAL, config, 900).gradeXp());
	}

	@Test
	void resetTechniquesKeepsProgress() {
		assertEquals(PROGRESS, SorcererLifecycle.applyPolicy(PROGRESS, DeathPolicy.RESET_TECHNIQUES, new JjkServerConfig.Death(), 900));
	}
}
