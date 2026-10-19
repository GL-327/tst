package com.jjkcraft.network.payload;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import com.jjkcraft.JjkCraft;
import com.jjkcraft.technique.CastFailure;

/** Server to client: why a cast was refused, so the hotbar can flash the slot and show a reason. */
public record CastFeedbackPayload(int slot, CastFailure failure) implements CustomPacketPayload {
	public static final Type<CastFeedbackPayload> TYPE = new Type<>(JjkCraft.id("cast_feedback"));

	public static final StreamCodec<ByteBuf, CastFeedbackPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, CastFeedbackPayload::slot,
			CastFailure.STREAM_CODEC, CastFeedbackPayload::failure,
			CastFeedbackPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
