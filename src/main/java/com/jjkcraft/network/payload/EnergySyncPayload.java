package com.jjkcraft.network.payload;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import com.jjkcraft.JjkCraft;
import com.jjkcraft.core.energy.EnergyState;
import com.jjkcraft.network.JjkStreamCodecs;

/**
 * Server to client: the player's own CE. Sent only when the state, max or regen modifier changes;
 * the client predicts regen in between with the same math as the server.
 */
public record EnergySyncPayload(EnergyState state, double max, double regenModifier) implements CustomPacketPayload {
	public static final Type<EnergySyncPayload> TYPE = new Type<>(JjkCraft.id("energy_sync"));

	public static final StreamCodec<ByteBuf, EnergySyncPayload> CODEC = StreamCodec.composite(
			JjkStreamCodecs.ENERGY_STATE, EnergySyncPayload::state,
			ByteBufCodecs.DOUBLE, EnergySyncPayload::max,
			ByteBufCodecs.DOUBLE, EnergySyncPayload::regenModifier,
			EnergySyncPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
