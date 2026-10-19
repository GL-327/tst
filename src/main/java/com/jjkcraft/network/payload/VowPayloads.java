package com.jjkcraft.network.payload;

import java.util.List;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import com.jjkcraft.JjkCraft;
import com.jjkcraft.vow.VowState;

/** The Binding Vow payloads. */
public final class VowPayloads {
	private VowPayloads() {
	}

	/** Client to server: swear a vow. Focus and term are ordinals; the server validates everything. */
	public record Commit(List<Identifier> vows, int focus, int term) implements CustomPacketPayload {
		public static final Type<Commit> TYPE = new Type<>(JjkCraft.id("vow_commit"));
		public static final StreamCodec<ByteBuf, Commit> CODEC = StreamCodec.composite(
				Identifier.STREAM_CODEC.apply(ByteBufCodecs.list(8)), Commit::vows,
				ByteBufCodecs.VAR_INT, Commit::focus,
				ByteBufCodecs.VAR_INT, Commit::term,
				Commit::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Client to server: renounce the current vow. */
	public record Renounce() implements CustomPacketPayload {
		public static final Renounce INSTANCE = new Renounce();
		public static final Type<Renounce> TYPE = new Type<>(JjkCraft.id("vow_renounce"));
		public static final StreamCodec<ByteBuf, Renounce> CODEC = StreamCodec.unit(INSTANCE);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Server to client: the player's vow state. */
	public record Sync(VowState state) implements CustomPacketPayload {
		public static final Type<Sync> TYPE = new Type<>(JjkCraft.id("vow_sync"));
		public static final StreamCodec<ByteBuf, Sync> CODEC = ByteBufCodecs.fromCodec(VowState.CODEC).map(Sync::new, Sync::state);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}
}
