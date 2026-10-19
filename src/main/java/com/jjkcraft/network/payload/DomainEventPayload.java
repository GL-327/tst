package com.jjkcraft.network.payload;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import com.jjkcraft.JjkCraft;

/**
 * Server to client: a moment in a domain (it opened, a clash, a pulse, a roll), for the title,
 * toasts and effects. {@code kind} is an ordinal of {@code DomainService.Event}.
 */
public record DomainEventPayload(int instance, int kind, int value) implements CustomPacketPayload {
	public static final Type<DomainEventPayload> TYPE = new Type<>(JjkCraft.id("domain_event"));

	public static final StreamCodec<ByteBuf, DomainEventPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, DomainEventPayload::instance,
			ByteBufCodecs.VAR_INT, DomainEventPayload::kind,
			ByteBufCodecs.VAR_INT, DomainEventPayload::value,
			DomainEventPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
