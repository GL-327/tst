package com.jjkcraft.network;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import com.jjkcraft.core.energy.EnergyParams;
import com.jjkcraft.core.energy.EnergyState;

/** Stream codecs for the mod's core value types. */
public final class JjkStreamCodecs {
	public static final StreamCodec<ByteBuf, EnergyState> ENERGY_STATE = StreamCodec.composite(
			ByteBufCodecs.DOUBLE, EnergyState::current,
			ByteBufCodecs.VAR_LONG, EnergyState::lastUpdateTick,
			ByteBufCodecs.VAR_LONG, EnergyState::overexertedUntil,
			ByteBufCodecs.VAR_LONG, EnergyState::lastActionTick,
			EnergyState::new);

	public static final StreamCodec<ByteBuf, EnergyParams> ENERGY_PARAMS = StreamCodec.composite(
			ByteBufCodecs.DOUBLE, EnergyParams::regenFractionPerSecond,
			ByteBufCodecs.DOUBLE, EnergyParams::idleMultiplier,
			ByteBufCodecs.VAR_INT, EnergyParams::idleDelayTicks,
			ByteBufCodecs.DOUBLE, EnergyParams::overexertThreshold,
			ByteBufCodecs.VAR_INT, EnergyParams::overexertTicks,
			EnergyParams::new);

	private JjkStreamCodecs() {
	}
}
