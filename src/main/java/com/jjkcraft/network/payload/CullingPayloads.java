package com.jjkcraft.network.payload;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import com.jjkcraft.JjkCraft;
import com.jjkcraft.culling.CullingState;

/** Culling Game packets: the player's view of the game, short events, and requests. */
public final class CullingPayloads {
	private CullingPayloads() {
	}

	/** The viewer's own standing. Status: none, participant, resident, withdrawn, eliminated. */
	public record Me(String status, int colony, int points, int earned, long deadline, boolean nearMaster, boolean stagnant) {
		public static final Me NONE = new Me("none", -1, 0, 0, -1, false, false);
		public static final Codec<Me> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.STRING.fieldOf("status").forGetter(Me::status),
				Codec.INT.fieldOf("colony").forGetter(Me::colony),
				Codec.INT.fieldOf("points").forGetter(Me::points),
				Codec.INT.fieldOf("earned").forGetter(Me::earned),
				Codec.LONG.fieldOf("deadline").forGetter(Me::deadline),
				Codec.BOOL.fieldOf("near_master").forGetter(Me::nearMaster),
				Codec.BOOL.fieldOf("stagnant").forGetter(Me::stagnant)
		).apply(i, Me::new));

		public boolean participant() {
			return status.equals("participant");
		}
	}

	public record ColonyView(int id, String name, ResourceKey<Level> dimension, BlockPos center, int radius, int participants, int points) {
		public static final Codec<ColonyView> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.INT.fieldOf("id").forGetter(ColonyView::id),
				Codec.STRING.fieldOf("name").forGetter(ColonyView::name),
				Level.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(ColonyView::dimension),
				BlockPos.CODEC.fieldOf("center").forGetter(ColonyView::center),
				Codec.INT.fieldOf("radius").forGetter(ColonyView::radius),
				Codec.INT.fieldOf("participants").forGetter(ColonyView::participants),
				Codec.INT.fieldOf("points").forGetter(ColonyView::points)
		).apply(i, ColonyView::new));
	}

	/**
	 * A scoreboard line. Name and colony are empty / -1 unless the viewer may know them (their own
	 * colony, revealed, or the leader while the leader is revealed).
	 */
	public record Entry(UUID id, String name, int colony, int points, boolean sorcerer, boolean npc, boolean you, boolean leader,
			Optional<BlockPos> pos) {
		public static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
				UUIDUtil.CODEC.fieldOf("id").forGetter(Entry::id),
				Codec.STRING.fieldOf("name").forGetter(Entry::name),
				Codec.INT.fieldOf("colony").forGetter(Entry::colony),
				Codec.INT.fieldOf("points").forGetter(Entry::points),
				Codec.BOOL.fieldOf("sorcerer").forGetter(Entry::sorcerer),
				Codec.BOOL.fieldOf("npc").forGetter(Entry::npc),
				Codec.BOOL.fieldOf("you").forGetter(Entry::you),
				Codec.BOOL.fieldOf("leader").forGetter(Entry::leader),
				BlockPos.CODEC.optionalFieldOf("pos").forGetter(Entry::pos)
		).apply(i, Entry::new));

		public boolean known() {
			return !name.isEmpty();
		}
	}

	public record RuleView(Identifier id, Optional<String> addedBy, long expiresAt) {
		public static final Codec<RuleView> CODEC = RecordCodecBuilder.create(i -> i.group(
				Identifier.CODEC.fieldOf("id").forGetter(RuleView::id),
				Codec.STRING.optionalFieldOf("added_by").forGetter(RuleView::addedBy),
				Codec.LONG.fieldOf("expires_at").forGetter(RuleView::expiresAt)
		).apply(i, RuleView::new));
	}

	/**
	 * Everything the map, scoreboard, rule list and HUD panel show. {@code config}: entry window in
	 * days, sorcerer points, non-sorcerer points, rule cost.
	 */
	public record View(boolean running, boolean ended, long startedAt, int day, Me me, List<Integer> config, List<ColonyView> colonies,
			List<Entry> board, List<RuleView> rules, List<CullingState.LogLine> log, List<CullingState.Standing> results) {
		public static final View EMPTY = new View(false, false, 0, 0, Me.NONE, List.of(19, 5, 1, 100), List.of(), List.of(), List.of(), List.of(),
				List.of());
		public static final Codec<View> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.BOOL.fieldOf("running").forGetter(View::running),
				Codec.BOOL.fieldOf("ended").forGetter(View::ended),
				Codec.LONG.fieldOf("started_at").forGetter(View::startedAt),
				Codec.INT.fieldOf("day").forGetter(View::day),
				Me.CODEC.fieldOf("me").forGetter(View::me),
				Codec.INT.listOf().fieldOf("config").forGetter(View::config),
				ColonyView.CODEC.listOf().fieldOf("colonies").forGetter(View::colonies),
				Entry.CODEC.listOf().fieldOf("board").forGetter(View::board),
				RuleView.CODEC.listOf().fieldOf("rules").forGetter(View::rules),
				CullingState.LogLine.CODEC.listOf().fieldOf("log").forGetter(View::log),
				CullingState.Standing.CODEC.listOf().fieldOf("results").forGetter(View::results)
		).apply(i, View::new));

		public int configValue(int index, int fallback) {
			return index < config.size() ? config.get(index) : fallback;
		}
	}

	/** Server to client: the viewer's whole view of the game. */
	public record Sync(View view) implements CustomPacketPayload {
		public static final Type<Sync> TYPE = new Type<>(JjkCraft.id("culling_sync"));
		public static final StreamCodec<ByteBuf, Sync> CODEC = ByteBufCodecs.fromCodec(View.CODEC).map(Sync::new, Sync::view);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/**
	 * Server to client: something to show at once. Kinds: joined, points, rule_added, refused,
	 * deadline_warning, started, ended, open_negotiation.
	 */
	public record Event(String kind, List<String> args) implements CustomPacketPayload {
		public static final Type<Event> TYPE = new Type<>(JjkCraft.id("culling_event"));
		public static final StreamCodec<ByteBuf, Event> CODEC = StreamCodec.composite(
				ByteBufCodecs.STRING_UTF8, Event::kind,
				ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(16)), Event::args,
				Event::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/**
	 * Client to server: a request a rule must allow. {@code action} is a {@code RuleEvent.Action}
	 * name; {@code target} a participant; {@code amount} for transfers; {@code rule} for negotiation;
	 * {@code colony} for substitution.
	 */
	public record Action(String action, Optional<UUID> target, int amount, Optional<Identifier> rule, int colony) implements CustomPacketPayload {
		public static final Type<Action> TYPE = new Type<>(JjkCraft.id("culling_action"));
		public static final StreamCodec<ByteBuf, Action> CODEC = StreamCodec.composite(
				ByteBufCodecs.stringUtf8(32), Action::action,
				ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC), Action::target,
				ByteBufCodecs.VAR_INT, Action::amount,
				ByteBufCodecs.optional(Identifier.STREAM_CODEC), Action::rule,
				ByteBufCodecs.VAR_INT, Action::colony,
				Action::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}
}
