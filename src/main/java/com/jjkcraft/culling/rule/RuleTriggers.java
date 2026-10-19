package com.jjkcraft.culling.rule;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;

import com.jjkcraft.JjkCraft;

/**
 * The validated set of Culling Game rule triggers ({@code jjkcraft:culling_trigger}). A rule clause
 * names one; it fires when an event of its kind matches its fields.
 */
public final class RuleTriggers {
	public interface Trigger {
		Type<?> type();

		boolean matches(RuleEvent event);
	}

	public record Type<T extends Trigger>(MapCodec<T> codec) {
	}

	public static final ResourceKey<Registry<Type<?>>> KEY = ResourceKey.createRegistryKey(JjkCraft.id("culling_trigger"));
	public static final Registry<Type<?>> REGISTRY = FabricRegistryBuilder.create(KEY).buildAndRegister();
	public static final Codec<Trigger> CODEC = REGISTRY.byNameCodec().dispatch("type", Trigger::type, Type::codec);

	/** A participant ended a life (or a curse) inside a colony. */
	public record Kill(RuleEvent.Victim victim, RuleEvent.Cause cause, double minDistance, boolean nightOnly) implements Trigger {
		public static final MapCodec<Kill> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
				RuleEvent.Victim.CODEC.optionalFieldOf("victim", RuleEvent.Victim.PARTICIPANT).forGetter(Kill::victim),
				RuleEvent.Cause.CODEC.optionalFieldOf("cause", RuleEvent.Cause.ANY).forGetter(Kill::cause),
				Codec.doubleRange(0, 256).optionalFieldOf("min_distance", 0.0).forGetter(Kill::minDistance),
				Codec.BOOL.optionalFieldOf("night_only", false).forGetter(Kill::nightOnly)
		).apply(i, Kill::new));

		@Override
		public Type<?> type() {
			return KILL;
		}

		@Override
		public boolean matches(RuleEvent event) {
			return event.kind == RuleEvent.Kind.KILL && event.victimMatches(victim)
					&& (cause == RuleEvent.Cause.ANY || cause == event.cause)
					&& event.distance >= minDistance && (!nightOnly || event.night);
		}
	}

	/** An event with no fields of its own: enter, leave, deadline, added. */
	public record Plain(RuleEvent.Kind kind) implements Trigger {
		@Override
		public Type<?> type() {
			return switch (kind) {
				case ENTER -> ENTER;
				case LEAVE -> LEAVE;
				case DEADLINE -> DEADLINE;
				default -> ADDED;
			};
		}

		@Override
		public boolean matches(RuleEvent event) {
			return event.kind == kind;
		}
	}

	/** A participant has gone {@code days} days without scoring. */
	public record Idle(int days) implements Trigger {
		public static final MapCodec<Idle> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
				Codec.intRange(1, 1000).fieldOf("days").forGetter(Idle::days)
		).apply(i, Idle::new));

		@Override
		public Type<?> type() {
			return IDLE;
		}

		@Override
		public boolean matches(RuleEvent event) {
			return event.kind == RuleEvent.Kind.IDLE && event.idleDays >= days;
		}
	}

	/** Every {@code days} game days, at the turn of the day. */
	public record Timer(int days) implements Trigger {
		public static final MapCodec<Timer> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
				Codec.intRange(1, 1000).optionalFieldOf("days", 1).forGetter(Timer::days)
		).apply(i, Timer::new));

		@Override
		public Type<?> type() {
			return TIMER;
		}

		@Override
		public boolean matches(RuleEvent event) {
			return event.kind == RuleEvent.Kind.TIMER && event.day > 0 && event.day % days == 0;
		}
	}

	/** A participant's points rose to {@code points} or past it. */
	public record PointsReached(int points) implements Trigger {
		public static final MapCodec<PointsReached> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
				Codec.intRange(1, 1_000_000).fieldOf("points").forGetter(PointsReached::points)
		).apply(i, PointsReached::new));

		@Override
		public Type<?> type() {
			return POINTS_REACHED;
		}

		@Override
		public boolean matches(RuleEvent event) {
			return event.kind == RuleEvent.Kind.POINTS && event.pointsBefore < points && event.pointsAfter >= points;
		}
	}

	/** A participant asked for {@code action}; only rules with this trigger allow it. */
	public record Action(RuleEvent.Action action) implements Trigger {
		public static final MapCodec<Action> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
				RuleEvent.Action.CODEC.fieldOf("action").forGetter(Action::action)
		).apply(i, Action::new));

		@Override
		public Type<?> type() {
			return ACTION;
		}

		@Override
		public boolean matches(RuleEvent event) {
			return event.kind == RuleEvent.Kind.ACTION && event.action == action;
		}
	}

	public static final Type<Kill> KILL = register("kill", Kill.CODEC);
	public static final Type<Plain> ENTER = register("enter", MapCodec.unit(new Plain(RuleEvent.Kind.ENTER)));
	public static final Type<Plain> LEAVE = register("leave", MapCodec.unit(new Plain(RuleEvent.Kind.LEAVE)));
	public static final Type<Plain> DEADLINE = register("deadline", MapCodec.unit(new Plain(RuleEvent.Kind.DEADLINE)));
	public static final Type<Plain> ADDED = register("added", MapCodec.unit(new Plain(RuleEvent.Kind.ADDED)));
	public static final Type<Idle> IDLE = register("idle", Idle.CODEC);
	public static final Type<Timer> TIMER = register("timer", Timer.CODEC);
	public static final Type<PointsReached> POINTS_REACHED = register("points_reached", PointsReached.CODEC);
	public static final Type<Action> ACTION = register("action", Action.CODEC);

	private RuleTriggers() {
	}

	public static void init() {
		// Loads the class so the static fields register.
	}

	public static <T extends Trigger> Type<T> register(String name, MapCodec<T> codec) {
		return Registry.register(REGISTRY, JjkCraft.id(name), new Type<>(codec));
	}
}
