package com.jjkcraft.network.payload;

import java.util.Optional;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import com.jjkcraft.JjkCraft;

/** Client to server: put a known technique in a hotbar slot, or clear the slot. */
public record SetLoadoutPayload(int slot, Optional<Identifier> technique) implements CustomPacketPayload {
	public static final Type<SetLoadoutPayload> TYPE = new Type<>(JjkCraft.id("set_loadout"));

	public static final StreamCodec<ByteBuf, SetLoadoutPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, SetLoadoutPayload::slot,
			ByteBufCodecs.optional(Identifier.STREAM_CODEC), SetLoadoutPayload::technique,
			SetLoadoutPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
