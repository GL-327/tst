package com.jjkcraft.gametest;

import java.util.UUID;

import com.mojang.authlib.GameProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;

import net.fabricmc.fabric.api.entity.FakePlayer;

/**
 * Fresh server players for GameTests. Fabric caches fake players by profile, so each call uses a
 * new UUID to keep tests from sharing attachment data.
 */
final class TestPlayers {
	private TestPlayers() {
	}

	static ServerPlayer fresh(GameTestHelper helper) {
		UUID uuid = UUID.randomUUID();
		ServerPlayer player = FakePlayer.get(helper.getLevel(), new GameProfile(uuid, "jjk" + uuid.toString().substring(0, 8)));
		BlockPos pos = helper.absolutePos(BlockPos.ZERO.above());
		player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
		return player;
	}
}
