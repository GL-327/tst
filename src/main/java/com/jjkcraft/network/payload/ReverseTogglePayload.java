package com.jjkcraft.network.payload;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import com.jjkcraft.JjkCraft;

/** Client to server: toggle automatic Reverse Cursed Technique healing. */
public record ReverseTogglePayload() implements CustomPacketPayload {
	public static final ReverseTogglePayload INSTANCE = new ReverseTogglePayload();
	public static final Type<ReverseTogglePayload> TYPE = new Type<>(JjkCraft.id("reverse_toggle"));
	public static final StreamCodec<ByteBuf, ReverseTogglePayload> CODEC = StreamCodec.unit(INSTANCE);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
