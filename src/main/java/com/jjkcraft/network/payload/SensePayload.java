package com.jjkcraft.network.payload;

import java.util.List;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import com.jjkcraft.JjkCraft;

/**
 * Server to client: what the player's sensing picks up right now. {@code active} false means
 * sensing is off (or not possible) and the client should clear its outlines and radar.
 */
public record SensePayload(boolean active, float range, List<Sensed> sensed) implements CustomPacketPayload {
	public static final Type<SensePayload> TYPE = new Type<>(JjkCraft.id("sense"));

	/** One presence: the entity, its grade color, its grade rank and whether it is a curse. */
	public record Sensed(int entityId, int color, int rank, boolean curse) {
		public static final StreamCodec<ByteBuf, Sensed> CODEC = StreamCodec.composite(
				ByteBufCodecs.VAR_INT, Sensed::entityId,
				ByteBufCodecs.INT, Sensed::color,
				ByteBufCodecs.VAR_INT, Sensed::rank,
				ByteBufCodecs.BOOL, Sensed::curse,
				Sensed::new);
	}

	public static final StreamCodec<ByteBuf, SensePayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.BOOL, SensePayload::active,
			ByteBufCodecs.FLOAT, SensePayload::range,
			Sensed.CODEC.apply(ByteBufCodecs.list(64)), SensePayload::sensed,
			SensePayload::new);

	public static SensePayload off() {
		return new SensePayload(false, 0, List.of());
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
