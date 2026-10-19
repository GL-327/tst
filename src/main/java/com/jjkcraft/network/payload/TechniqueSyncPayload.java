package com.jjkcraft.network.payload;

import java.util.Optional;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import com.jjkcraft.JjkCraft;
import com.jjkcraft.technique.TechniqueState;

/**
 * Server to client: the player's techniques (known, loadout, mastery, cooldowns) and the channel or
 * toggle technique currently active, if any. Sent when they change.
 */
public record TechniqueSyncPayload(TechniqueState state, Optional<Identifier> active) implements CustomPacketPayload {
	public static final Type<TechniqueSyncPayload> TYPE = new Type<>(JjkCraft.id("technique_sync"));

	public static final StreamCodec<ByteBuf, TechniqueSyncPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.fromCodec(TechniqueState.CODEC), TechniqueSyncPayload::state,
			ByteBufCodecs.optional(Identifier.STREAM_CODEC), TechniqueSyncPayload::active,
			TechniqueSyncPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
