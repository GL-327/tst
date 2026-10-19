package com.jjkcraft.network.payload;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import com.jjkcraft.JjkCraft;

/** Client to server: quick-cast a known technique from the technique wheel. */
public record CastTechniquePayload(Identifier technique) implements CustomPacketPayload {
	public static final Type<CastTechniquePayload> TYPE = new Type<>(JjkCraft.id("cast_technique"));

	public static final StreamCodec<ByteBuf, CastTechniquePayload> CODEC =
			Identifier.STREAM_CODEC.map(CastTechniquePayload::new, CastTechniquePayload::technique);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
