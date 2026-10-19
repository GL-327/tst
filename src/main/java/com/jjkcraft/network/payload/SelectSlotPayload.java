package com.jjkcraft.network.payload;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import com.jjkcraft.JjkCraft;

/** Client to server: select a hotbar slot for the cast key. */
public record SelectSlotPayload(int slot) implements CustomPacketPayload {
	public static final Type<SelectSlotPayload> TYPE = new Type<>(JjkCraft.id("select_slot"));

	public static final StreamCodec<ByteBuf, SelectSlotPayload> CODEC = ByteBufCodecs.VAR_INT.map(SelectSlotPayload::new, SelectSlotPayload::slot);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
