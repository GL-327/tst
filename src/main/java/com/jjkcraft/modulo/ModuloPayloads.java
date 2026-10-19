package com.jjkcraft.modulo;

import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import com.jjkcraft.JjkCraft;

/** Modulo packets: the player's view of the expansion, dialogue, short events, and the one request (a choice). */
public final class ModuloPayloads {
	private ModuloPayloads() {
	}

	/**
	 * What the HUD shows.
	 *
	 * @param enabled       the expansion is on
	 * @param mulEnergy     the player's Mul energy
	 * @param mulMax        the most they can hold
	 * @param missionTitle  translation key of the current mission's title (empty: none)
	 * @param missionOrder  its position (1-based) and the number of missions
	 * @param objective     translation key of the current objective
	 * @param args          arguments for the objective (progress numbers)
	 * @param target        where the objective is (a site or marker), if anywhere
	 * @param siteName      translation key of that place's name
	 * @param descent       the Naunax Descent's ramp foot while it is on and the player may board
	 * @param descentEnds   game time it ends
	 * @param companion     Maru is the player's companion
	 * @param inModulo      the player is in one of the expansion's dimensions
	 */
	public record View(boolean enabled, double mulEnergy, double mulMax, String missionTitle, List<Integer> missionOrder, String objective, List<String> args,
			Optional<GlobalPos> target, String siteName, Optional<BlockPos> descent, long descentEnds, boolean companion, boolean inModulo) {
		public static final View EMPTY = new View(false, 0, 1, "", List.of(0, 0), "", List.of(), Optional.empty(), "", Optional.empty(), 0, false, false);
		public static final Codec<View> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.BOOL.fieldOf("enabled").forGetter(View::enabled),
				Codec.DOUBLE.fieldOf("mul").forGetter(View::mulEnergy),
				Codec.DOUBLE.fieldOf("mul_max").forGetter(View::mulMax),
				Codec.STRING.fieldOf("title").forGetter(View::missionTitle),
				Codec.INT.listOf(2, 2).fieldOf("order").forGetter(View::missionOrder),
				Codec.STRING.fieldOf("objective").forGetter(View::objective),
				Codec.STRING.listOf(0, 8).fieldOf("args").forGetter(View::args),
				GlobalPos.CODEC.optionalFieldOf("target").forGetter(View::target),
				Codec.STRING.fieldOf("site").forGetter(View::siteName),
				BlockPos.CODEC.optionalFieldOf("descent").forGetter(View::descent),
				Codec.LONG.fieldOf("descent_ends").forGetter(View::descentEnds),
				Codec.BOOL.fieldOf("companion").forGetter(View::companion),
				Codec.BOOL.fieldOf("in_modulo").forGetter(View::inModulo)
		).apply(i, View::new));
	}

	/** Server to client: the player's whole view. */
	public record Sync(View view) implements CustomPacketPayload {
		public static final Type<Sync> TYPE = new Type<>(JjkCraft.id("modulo_sync"));
		public static final StreamCodec<ByteBuf, Sync> CODEC = ByteBufCodecs.fromCodec(View.CODEC).map(Sync::new, Sync::view);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/**
	 * Server to client: a line of dialogue. {@code options} are translation keys when the line asks
	 * for a choice (answered with {@link Choose}).
	 */
	public record Dialogue(String speaker, String text, int index, int total, List<String> options) implements CustomPacketPayload {
		public static final Type<Dialogue> TYPE = new Type<>(JjkCraft.id("modulo_dialogue"));
		public static final StreamCodec<ByteBuf, Dialogue> CODEC = StreamCodec.composite(
				ByteBufCodecs.stringUtf8(64), Dialogue::speaker,
				ByteBufCodecs.stringUtf8(256), Dialogue::text,
				ByteBufCodecs.VAR_INT, Dialogue::index,
				ByteBufCodecs.VAR_INT, Dialogue::total,
				ByteBufCodecs.stringUtf8(128).apply(ByteBufCodecs.list(4)), Dialogue::options,
				Dialogue::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/**
	 * Server to client: something to show at once. Kinds: {@code stage_done}, {@code mission_done},
	 * {@code descent}, {@code sequence} (args: the sequence id), {@code adapted} (args: entity id, steps).
	 */
	public record Event(String kind, List<String> args) implements CustomPacketPayload {
		public static final Type<Event> TYPE = new Type<>(JjkCraft.id("modulo_event"));
		public static final StreamCodec<ByteBuf, Event> CODEC = StreamCodec.composite(
				ByteBufCodecs.stringUtf8(32), Event::kind,
				ByteBufCodecs.stringUtf8(256).apply(ByteBufCodecs.list(8)), Event::args,
				Event::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Client to server: the option picked at a choice stage (index into its options). */
	public record Choose(int option) implements CustomPacketPayload {
		public static final Type<Choose> TYPE = new Type<>(JjkCraft.id("modulo_choose"));
		public static final StreamCodec<ByteBuf, Choose> CODEC = ByteBufCodecs.VAR_INT.map(Choose::new, Choose::option);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}
}
