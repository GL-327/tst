package com.jjkcraft.network.payload;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import com.jjkcraft.JjkCraft;
import com.jjkcraft.sorcerer.GrowthPath;

/** Client to server: spend one growth point. The server checks that a point is available. */
public record SpendGrowthPayload(GrowthPath path) implements CustomPacketPayload {
	public static final Type<SpendGrowthPayload> TYPE = new Type<>(JjkCraft.id("spend_growth"));

	public static final StreamCodec<ByteBuf, SpendGrowthPayload> CODEC =
			GrowthPath.STREAM_CODEC.map(SpendGrowthPayload::new, SpendGrowthPayload::path);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
