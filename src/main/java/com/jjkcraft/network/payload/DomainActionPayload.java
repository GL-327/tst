package com.jjkcraft.network.payload;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import com.jjkcraft.JjkCraft;

/** Client to server: the domain keys. The server decides everything else. */
public record DomainActionPayload(Action action) implements CustomPacketPayload {
	public enum Action {
		/** Expand the domain with a barrier, or close it if it is open. */
		EXPAND,
		/** Expand without a barrier (sneak + key), or close it if it is open. */
		EXPAND_BARRIERLESS,
		/** Raise or drop a Simple Domain. */
		SIMPLE
	}

	public static final Type<DomainActionPayload> TYPE = new Type<>(JjkCraft.id("domain_action"));

	public static final StreamCodec<ByteBuf, DomainActionPayload> CODEC = ByteBufCodecs.idMapper(
			i -> Action.values()[Math.floorMod(i, Action.values().length)], Action::ordinal).map(DomainActionPayload::new, DomainActionPayload::action);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
