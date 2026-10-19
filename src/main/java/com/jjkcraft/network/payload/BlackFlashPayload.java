package com.jjkcraft.network.payload;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import com.jjkcraft.JjkCraft;

/**
 * Server to client: a Black Flash landed. The striker's own client flashes the frame and shows the
 * zone; everyone nearby sees the hit freeze.
 *
 * @param zoneUntil  game time the striker's zone ends
 * @param zoneStacks how deep in the zone the striker is
 */
public record BlackFlashPayload(int striker, int target, float multiplier, long zoneUntil, int zoneStacks) implements CustomPacketPayload {
	public static final Type<BlackFlashPayload> TYPE = new Type<>(JjkCraft.id("black_flash"));

	public static final StreamCodec<ByteBuf, BlackFlashPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, BlackFlashPayload::striker,
			ByteBufCodecs.VAR_INT, BlackFlashPayload::target,
			ByteBufCodecs.FLOAT, BlackFlashPayload::multiplier,
			ByteBufCodecs.VAR_LONG, BlackFlashPayload::zoneUntil,
			ByteBufCodecs.VAR_INT, BlackFlashPayload::zoneStacks,
			BlackFlashPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
