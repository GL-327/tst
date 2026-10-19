package com.jjkcraft.network.payload;

import java.util.Optional;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import com.jjkcraft.JjkCraft;

/**
 * Server to client: the player's own domain state for the domain meter.
 *
 * @param known          their domain, if any
 * @param burnoutUntil   game time their burnout ends
 * @param activeInstance their open domain's instance id, or -1
 * @param simpleActive   whether their Simple Domain is up
 * @param canSimple      whether their grade allows a Simple Domain
 */
public record DomainSelfPayload(Optional<Identifier> known, long burnoutUntil, int activeInstance, boolean simpleActive, boolean canSimple)
		implements CustomPacketPayload {
	public static final Type<DomainSelfPayload> TYPE = new Type<>(JjkCraft.id("domain_self"));

	public static final StreamCodec<ByteBuf, DomainSelfPayload> CODEC = StreamCodec.composite(
			Identifier.STREAM_CODEC.apply(ByteBufCodecs::optional), DomainSelfPayload::known,
			ByteBufCodecs.VAR_LONG, DomainSelfPayload::burnoutUntil,
			ByteBufCodecs.VAR_INT, DomainSelfPayload::activeInstance,
			ByteBufCodecs.BOOL, DomainSelfPayload::simpleActive,
			ByteBufCodecs.BOOL, DomainSelfPayload::canSimple,
			DomainSelfPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
