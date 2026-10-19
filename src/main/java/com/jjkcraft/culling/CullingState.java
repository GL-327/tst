package com.jjkcraft.culling;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import com.jjkcraft.JjkCraft;
import com.jjkcraft.core.culling.CullingMath;
import com.jjkcraft.registry.LegacyIds;

/**
 * The Culling Game's saved state: one per server (global saved data). Colonies, participants and
 * their points, entry deadlines, the rules in force and a short log. {@link CullingGame} is the only
 * writer.
 */
public final class CullingState extends SavedData {
	public enum Status implements StringRepresentable {
		ACTIVE, WITHDRAWN, ELIMINATED;

		public static final Codec<Status> CODEC = StringRepresentable.fromEnum(Status::values);

		@Override
		public String getSerializedName() {
			return name().toLowerCase(Locale.ROOT);
		}
	}

	/** A barrier-enclosed region. Residents were inside when it rose and may come and go. */
	public static final class Colony {
		public static final Codec<Colony> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.INT.fieldOf("id").forGetter(c -> c.id),
				Codec.STRING.fieldOf("name").forGetter(c -> c.name),
				Level.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(c -> c.dimension),
				BlockPos.CODEC.fieldOf("center").forGetter(c -> c.center),
				Codec.INT.fieldOf("radius").forGetter(c -> c.radius),
				Codec.LONG.fieldOf("raised_at").forGetter(c -> c.raisedAt),
				UUIDUtil.CODEC.listOf().optionalFieldOf("residents", List.of()).forGetter(c -> List.copyOf(c.residents)),
				Codec.INT.optionalFieldOf("pending", 0).forGetter(c -> c.pending)
		).apply(i, Colony::new));

		public final int id;
		public final String name;
		public final ResourceKey<Level> dimension;
		public final BlockPos center;
		public final int radius;
		public final long raisedAt;
		public final Set<UUID> residents;
		/** Entrants waiting to be brought in once the centre is loaded. */
		public int pending;
		/** The Kogane standing at the centre right now (not saved; respawned when missing). */
		public @Nullable UUID master;

		public Colony(int id, String name, ResourceKey<Level> dimension, BlockPos center, int radius, long raisedAt, List<UUID> residents,
				int pending) {
			this.id = id;
			this.name = name;
			this.dimension = dimension;
			this.center = center;
			this.radius = radius;
			this.raisedAt = raisedAt;
			this.residents = new HashSet<>(residents);
			this.pending = pending;
		}

		public boolean contains(ResourceKey<Level> level, double x, double y, double z) {
			return level.equals(dimension) && CullingMath.inside(x - (center.getX() + 0.5), y - center.getY(), z - (center.getZ() + 0.5), radius);
		}
	}

	/** A player or entrant in the game. */
	public static final class Participant {
		public static final Codec<Participant> CODEC = RecordCodecBuilder.create(i -> i.group(
				UUIDUtil.CODEC.fieldOf("id").forGetter(p -> p.id),
				Codec.STRING.fieldOf("name").forGetter(p -> p.name),
				Codec.BOOL.fieldOf("npc").forGetter(p -> p.npc),
				Codec.BOOL.fieldOf("sorcerer").forGetter(p -> p.sorcerer),
				Codec.INT.fieldOf("colony").forGetter(p -> p.colony),
				Codec.INT.fieldOf("points").forGetter(p -> p.points),
				Codec.INT.fieldOf("earned").forGetter(p -> p.earned),
				Codec.LONG.fieldOf("joined_at").forGetter(p -> p.joinedAt),
				Codec.LONG.fieldOf("last_scored_at").forGetter(p -> p.lastScoredAt),
				Status.CODEC.fieldOf("status").forGetter(p -> p.status),
				Codec.DOUBLE.optionalFieldOf("energy_cap", 1.0).forGetter(p -> p.energyCap)
		).apply(i, Participant::new));

		public final UUID id;
		public String name;
		public final boolean npc;
		public boolean sorcerer;
		public int colony;
		public int points;
		public int earned;
		public final long joinedAt;
		public long lastScoredAt;
		public Status status;
		/** Below 1 while stagnant: a cap on max CE until they score. */
		public double energyCap;

		public Participant(UUID id, String name, boolean npc, boolean sorcerer, int colony, int points, int earned, long joinedAt, long lastScoredAt,
				Status status, double energyCap) {
			this.id = id;
			this.name = name;
			this.npc = npc;
			this.sorcerer = sorcerer;
			this.colony = colony;
			this.points = points;
			this.earned = earned;
			this.joinedAt = joinedAt;
			this.lastScoredAt = lastScoredAt;
			this.status = status;
			this.energyCap = energyCap;
		}

		public boolean active() {
			return status == Status.ACTIVE;
		}
	}

	/** A rule in force. Base rules have no adder and never expire ({@link #NEVER}). */
	public record ActiveRule(Identifier id, Optional<String> addedBy, long addedAt, long expiresAt) {
		public static final long NEVER = Long.MAX_VALUE;

		public boolean lapses() {
			return expiresAt != NEVER;
		}

		public static final Codec<ActiveRule> CODEC = RecordCodecBuilder.create(i -> i.group(
				LegacyIds.CULLING_RULE_ID.fieldOf("id").forGetter(ActiveRule::id),
				Codec.STRING.optionalFieldOf("added_by").forGetter(ActiveRule::addedBy),
				Codec.LONG.fieldOf("added_at").forGetter(ActiveRule::addedAt),
				Codec.LONG.optionalFieldOf("expires_at", NEVER).forGetter(ActiveRule::expiresAt)
		).apply(i, ActiveRule::new));
	}

	/** A line in the game's log: a translation key and its arguments. */
	public record LogLine(long at, String key, List<String> args) {
		public static final Codec<LogLine> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.LONG.fieldOf("at").forGetter(LogLine::at),
				Codec.STRING.fieldOf("key").forGetter(LogLine::key),
				Codec.STRING.listOf().optionalFieldOf("args", List.of()).forGetter(LogLine::args)
		).apply(i, LogLine::new));
	}

	/** A final standing, kept after the game ends. */
	public record Standing(String name, int points, int colony) {
		public static final Codec<Standing> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.STRING.fieldOf("name").forGetter(Standing::name),
				Codec.INT.fieldOf("points").forGetter(Standing::points),
				Codec.INT.fieldOf("colony").forGetter(Standing::colony)
		).apply(i, Standing::new));
	}

	private record Deadline(UUID id, long at) {
		static final Codec<Deadline> CODEC = RecordCodecBuilder.create(i -> i.group(
				UUIDUtil.CODEC.fieldOf("id").forGetter(Deadline::id),
				Codec.LONG.fieldOf("at").forGetter(Deadline::at)
		).apply(i, Deadline::new));
	}

	public static final Codec<CullingState> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.BOOL.fieldOf("running").forGetter(s -> s.running),
			Codec.BOOL.optionalFieldOf("ended", false).forGetter(s -> s.ended),
			Codec.LONG.fieldOf("started_at").forGetter(s -> s.startedAt),
			Codec.INT.optionalFieldOf("last_day", 0).forGetter(s -> s.lastDay),
			Codec.INT.optionalFieldOf("next_colony", 0).forGetter(s -> s.nextColony),
			Codec.INT.optionalFieldOf("joined_ever", 0).forGetter(s -> s.joinedEver),
			Colony.CODEC.listOf().optionalFieldOf("colonies", List.of()).forGetter(s -> s.colonies),
			Participant.CODEC.listOf().optionalFieldOf("participants", List.of()).forGetter(s -> List.copyOf(s.participants.values())),
			Deadline.CODEC.listOf().optionalFieldOf("deadlines", List.of())
					.forGetter(s -> s.deadlines.entrySet().stream().map(e -> new Deadline(e.getKey(), e.getValue())).toList()),
			ActiveRule.CODEC.listOf().optionalFieldOf("rules", List.of()).forGetter(s -> s.rules),
			LogLine.CODEC.listOf().optionalFieldOf("log", List.of()).forGetter(s -> s.log),
			Standing.CODEC.listOf().optionalFieldOf("results", List.of()).forGetter(s -> s.results)
	).apply(i, CullingState::new));

	public static final SavedDataType<CullingState> TYPE = new SavedDataType<>(JjkCraft.id("culling_game"), CullingState::new, CODEC, null);

	public static final int LOG_SIZE = 24;

	public boolean running;
	public boolean ended;
	public long startedAt;
	public int lastDay;
	public int nextColony;
	public int joinedEver;
	public final List<Colony> colonies;
	public final Map<UUID, Participant> participants;
	/** Awakened sorcerers who have not joined yet: when their window closes. */
	public final Map<UUID, Long> deadlines;
	public final List<ActiveRule> rules;
	public final List<LogLine> log;
	public final List<Standing> results;
	/** Bumped on every change clients would see; drives resyncs. Not saved. */
	public int version;

	public CullingState() {
		this(false, false, 0, 0, 0, 0, List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
	}

	private CullingState(boolean running, boolean ended, long startedAt, int lastDay, int nextColony, int joinedEver, List<Colony> colonies,
			List<Participant> participants, List<Deadline> deadlines, List<ActiveRule> rules, List<LogLine> log, List<Standing> results) {
		this.running = running;
		this.ended = ended;
		this.startedAt = startedAt;
		this.lastDay = lastDay;
		this.nextColony = nextColony;
		this.joinedEver = joinedEver;
		this.colonies = new ArrayList<>(colonies);
		this.participants = new LinkedHashMap<>();
		for (Participant participant : participants) {
			this.participants.put(participant.id, participant);
		}

		this.deadlines = new LinkedHashMap<>();
		for (Deadline deadline : deadlines) {
			this.deadlines.put(deadline.id(), deadline.at());
		}

		this.rules = new ArrayList<>(rules);
		this.log = new ArrayList<>(log);
		this.results = new ArrayList<>(results);
	}

	public void changed() {
		version++;
		setDirty();
	}

	public Optional<Colony> colony(int id) {
		for (Colony colony : colonies) {
			if (colony.id == id) {
				return Optional.of(colony);
			}
		}

		return Optional.empty();
	}

	public Optional<Colony> colonyAt(ResourceKey<Level> level, double x, double y, double z) {
		for (Colony colony : colonies) {
			if (colony.contains(level, x, y, z)) {
				return Optional.of(colony);
			}
		}

		return Optional.empty();
	}

	public @Nullable Participant participant(UUID id) {
		return participants.get(id);
	}

	public @Nullable Participant active(UUID id) {
		Participant participant = participants.get(id);
		return participant != null && participant.active() ? participant : null;
	}

	public boolean hasRule(Identifier id) {
		for (ActiveRule rule : rules) {
			if (rule.id().equals(id)) {
				return true;
			}
		}

		return false;
	}

	/** Active participants, highest score first (earlier joiners win ties). */
	public List<Participant> ranking() {
		List<Participant> list = new ArrayList<>();
		for (Participant participant : participants.values()) {
			if (participant.active()) {
				list.add(participant);
			}
		}

		list.sort(Comparator.comparingInt((Participant p) -> -p.points).thenComparingLong(p -> p.joinedAt));
		return list;
	}

	/** The leader: the active participant with the most points, if anyone has scored. */
	public Optional<Participant> leader() {
		List<Participant> ranking = ranking();
		return ranking.isEmpty() || ranking.getFirst().points <= 0 ? Optional.empty() : Optional.of(ranking.getFirst());
	}

	public int standing() {
		int count = 0;
		for (Participant participant : participants.values()) {
			if (participant.active()) {
				count++;
			}
		}

		return count;
	}

	public void log(long at, String key, String... args) {
		log.add(new LogLine(at, key, List.of(args)));
		while (log.size() > LOG_SIZE) {
			log.removeFirst();
		}
	}
}
