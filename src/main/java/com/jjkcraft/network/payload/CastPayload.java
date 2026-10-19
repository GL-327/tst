package com.jjkcraft.network.payload;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import com.jjkcraft.JjkCraft;

/**
 * Client to server: the cast key went down or up. The server decides everything else.
 *
 * @param slot    hotbar slot, or -1 for the selected slot
 * @param pressed true on press, false on release (ends channels)
 */
public record CastPayload(int slot, boolean pressed) implements CustomPacketPayload {
	public static final Type<CastPayload> TYPE = new Type<>(JjkCraft.id("cast"));

	public static final StreamCodec<ByteBuf, CastPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, CastPayload::slot,
			ByteBufCodecs.BOOL, CastPayload::pressed,
			CastPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
