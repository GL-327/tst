package com.jjkcraft.network.payload;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import com.jjkcraft.JjkCraft;

/** Server to client: play the promotion ceremony. */
public record PromotionPayload(Identifier from, Identifier to) implements CustomPacketPayload {
	public static final Type<PromotionPayload> TYPE = new Type<>(JjkCraft.id("promotion"));

	public static final StreamCodec<ByteBuf, PromotionPayload> CODEC = StreamCodec.composite(
			Identifier.STREAM_CODEC, PromotionPayload::from,
			Identifier.STREAM_CODEC, PromotionPayload::to,
			PromotionPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
