package com.jjkcraft.gametest;

import net.minecraft.gametest.framework.GameTestHelper;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import com.jjkcraft.JjkCraft;

public final class SmokeGameTest {
	@GameTest
	public void modInitialized(GameTestHelper helper) {
		helper.assertTrue(JjkCraft.isInitialized(), "jjkcraft common entrypoint did not run");
		helper.succeed();
	}
}
