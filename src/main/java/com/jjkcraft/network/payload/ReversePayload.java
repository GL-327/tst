package com.jjkcraft.network.payload;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import com.jjkcraft.JjkCraft;

/**
 * Server to client: the player's Reverse Cursed Technique state. {@code awakenedNow} plays the
 * awakening moment once.
 */
public record ReversePayload(boolean unlocked, boolean autoHeal, boolean healing, boolean awakenedNow) implements CustomPacketPayload {
	public static final Type<ReversePayload> TYPE = new Type<>(JjkCraft.id("reverse"));

	public static final StreamCodec<ByteBuf, ReversePayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.BOOL, ReversePayload::unlocked,
			ByteBufCodecs.BOOL, ReversePayload::autoHeal,
			ByteBufCodecs.BOOL, ReversePayload::healing,
			ByteBufCodecs.BOOL, ReversePayload::awakenedNow,
			ReversePayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
