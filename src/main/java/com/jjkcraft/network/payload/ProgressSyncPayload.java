package com.jjkcraft.network.payload;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import com.jjkcraft.JjkCraft;
import com.jjkcraft.sorcerer.SorcererProgress;

/** Server to client: the player's own progression. Sent when it changes. */
public record ProgressSyncPayload(SorcererProgress progress) implements CustomPacketPayload {
	public static final Type<ProgressSyncPayload> TYPE = new Type<>(JjkCraft.id("progress_sync"));

	public static final StreamCodec<ByteBuf, ProgressSyncPayload> CODEC =
			ByteBufCodecs.fromCodec(SorcererProgress.CODEC).map(ProgressSyncPayload::new, ProgressSyncPayload::progress);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
