package com.jjkcraft.network.payload;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import com.jjkcraft.JjkCraft;

/** Client to server: turn cursed-energy sensing on or off. */
public record SenseTogglePayload(boolean on) implements CustomPacketPayload {
	public static final Type<SenseTogglePayload> TYPE = new Type<>(JjkCraft.id("sense_toggle"));

	public static final StreamCodec<ByteBuf, SenseTogglePayload> CODEC = ByteBufCodecs.BOOL.map(SenseTogglePayload::new, SenseTogglePayload::on);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
