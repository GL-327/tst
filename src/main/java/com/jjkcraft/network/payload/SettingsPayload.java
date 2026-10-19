package com.jjkcraft.network.payload;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import com.jjkcraft.JjkCraft;
import com.jjkcraft.core.energy.EnergyParams;
import com.jjkcraft.network.JjkStreamCodecs;

/** Server to client: the server config values that client prediction and UI need. Sent on join and on config reload. */
public record SettingsPayload(EnergyParams energy, boolean domainsEnabled, boolean cullingEnabled) implements CustomPacketPayload {
	public static final Type<SettingsPayload> TYPE = new Type<>(JjkCraft.id("settings"));

	public static final StreamCodec<ByteBuf, SettingsPayload> CODEC = StreamCodec.composite(
			JjkStreamCodecs.ENERGY_PARAMS, SettingsPayload::energy,
			ByteBufCodecs.BOOL, SettingsPayload::domainsEnabled,
			ByteBufCodecs.BOOL, SettingsPayload::cullingEnabled,
			SettingsPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
