package com.jjkcraft.culling.rule;

import java.util.Locale;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.StringRepresentable;

import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;

import com.jjkcraft.JjkCraft;
import com.jjkcraft.config.JjkConfig;
import com.jjkcraft.core.culling.CullingMath;
import com.jjkcraft.culling.CullingGame;

/**
 * The validated set of Culling Game rule effects ({@code jjkcraft:culling_effect}). Each is a small
 * piece of code; rules only combine them and supply the numbers.
 */
public final class RuleEffects {
	public interface Effect {
		Type<?> type();

		void apply(RuleEvent event, Identifier rule);
	}

	public record Type<T extends Effect>(MapCodec<T> codec) {
	}

	public static final ResourceKey<Registry<Type<?>>> KEY = ResourceKey.createRegistryKey(JjkCraft.id("culling_effect"));
	public static final Registry<Type<?>> REGISTRY = FabricRegistryBuilder.create(KEY).buildAndRegister();
	public static final Codec<Effect> CODEC = REGISTRY.byNameCodec().dispatch("type", Effect::type, Type::codec);

	/** Where a point value comes from. */
	public enum Scale implements StringRepresentable {
		/** Just {@code amount}. */
		FLAT,
		/** The configured value of a sorcerer. */
		SORCERER,
		/** The configured value of a non-sorcerer. */
		NON_SORCERER,
		/** The victim's own value: sorcerer or non-sorcerer. */
		VICTIM,
		/** A curse's value by grade. */
		CURSE;

		public static final Codec<Scale> CODEC = StringRepresentable.fromEnum(Scale::values);

		@Override
		public String getSerializedName() {
			return name().toLowerCase(Locale.ROOT);
		}

		public int base(RuleEvent event) {
			var config = JjkConfig.server().cullingGame;
			return switch (this) {
				case FLAT -> 0;
				case SORCERER -> config.sorcererPoints;
				case NON_SORCERER -> config.nonSorcererPoints;
				case VICTIM -> event.victimCurse ? CullingMath.curseValue(event.curseRank, event.curseSpecial)
						: event.victimSorcerer ? config.sorcererPoints : config.nonSorcererPoints;
				case CURSE -> CullingMath.curseValue(event.curseRank, event.curseSpecial);
			};
		}
	}

	/** Gives the actor points: {@code (scale + amount) * multiplier}. */
	public record AwardPoints(Scale scale, int amount, double multiplier) implements Effect {
		public static final MapCodec<AwardPoints> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
				Scale.CODEC.optionalFieldOf("scale", Scale.FLAT).forGetter(AwardPoints::scale),
				Codec.intRange(-10_000, 10_000).optionalFieldOf("amount", 0).forGetter(AwardPoints::amount),
				Codec.doubleRange(0, 100).optionalFieldOf("multiplier", 1.0).forGetter(AwardPoints::multiplier)
		).apply(i, AwardPoints::new));

		@Override
		public Type<?> type() {
			return AWARD_POINTS;
		}

		@Override
		public void apply(RuleEvent event, Identifier rule) {
			int points = (int) Math.round((scale.base(event) + amount) * multiplier);
			if (points != 0) {
				CullingGame.addPoints(event.level.getServer(), event.actor, points, rule);
			}
		}
	}

	/** Takes points from the actor (never below zero). */
	public record DeductPoints(Scale scale, int amount) implements Effect {
		public static final MapCodec<DeductPoints> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
				Scale.CODEC.optionalFieldOf("scale", Scale.FLAT).forGetter(DeductPoints::scale),
				Codec.intRange(0, 10_000).optionalFieldOf("amount", 0).forGetter(DeductPoints::amount)
		).apply(i, DeductPoints::new));

		@Override
		public Type<?> type() {
			return DEDUCT_POINTS;
		}

		@Override
		public void apply(RuleEvent event, Identifier rule) {
			int points = scale.base(event) + amount;
			if (points > 0) {
				CullingGame.addPoints(event.level.getServer(), event.actor, -points, rule);
			}
		}
	}

	/** The actor becomes a participant of the colony they entered. */
	public record Join() implements Effect {
		@Override
		public Type<?> type() {
			return JOIN;
		}

		@Override
		public void apply(RuleEvent event, Identifier rule) {
			if (event.actorEntity != null) {
				CullingGame.join(event.actorEntity, event.colony);
			}
		}
	}

	/** The actor is held inside their colony's barrier. */
	public record Confine() implements Effect {
		@Override
		public Type<?> type() {
			return CONFINE;
		}

		@Override
		public void apply(RuleEvent event, Identifier rule) {
			if (event.actorEntity != null) {
				CullingGame.confine(event.actorEntity, event.colony);
			}
		}
	}

	/** The actor's innate technique is taken (unless the server's missed-entry penalty is NONE). */
	public record RemoveTechnique() implements Effect {
		@Override
		public Type<?> type() {
			return REMOVE_TECHNIQUE;
		}

		@Override
		public void apply(RuleEvent event, Identifier rule) {
			CullingGame.removeTechnique(event);
		}
	}

	/** Moves {@code event.amount} points, in whole steps, from the actor to the target. */
	public record TransferPoints(int step) implements Effect {
		public static final MapCodec<TransferPoints> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
				Codec.intRange(1, 1000).optionalFieldOf("step", 1).forGetter(TransferPoints::step)
		).apply(i, TransferPoints::new));

		@Override
		public Type<?> type() {
			return TRANSFER_POINTS;
		}

		@Override
		public void apply(RuleEvent event, Identifier rule) {
			CullingGame.transfer(event, step, rule);
		}
	}

	/** For {@code cost} points the actor learns the target's name, colony, score and position. */
	public record Reveal(int cost, int seconds) implements Effect {
		public static final MapCodec<Reveal> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
				Codec.intRange(0, 10_000).optionalFieldOf("cost", 1).forGetter(Reveal::cost),
				Codec.intRange(1, 86_400).optionalFieldOf("seconds", 60).forGetter(Reveal::seconds)
		).apply(i, Reveal::new));

		@Override
		public Type<?> type() {
			return REVEAL;
		}

		@Override
		public void apply(RuleEvent event, Identifier rule) {
			CullingGame.reveal(event, cost, seconds, rule);
		}
	}

	/** For {@code cost} points the actor moves to another colony; a new entrant takes their place. */
	public record Substitute(int cost) implements Effect {
		public static final MapCodec<Substitute> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
				Codec.intRange(0, 10_000).optionalFieldOf("cost", 30).forGetter(Substitute::cost)
		).apply(i, Substitute::new));

		@Override
		public Type<?> type() {
			return SUBSTITUTE;
		}

		@Override
		public void apply(RuleEvent event, Identifier rule) {
			CullingGame.substitute(event, cost, rule);
		}
	}

	/** For {@code cost} points the actor leaves the game for good. */
	public record Withdraw(int cost) implements Effect {
		public static final MapCodec<Withdraw> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
				Codec.intRange(0, 10_000).optionalFieldOf("cost", 100).forGetter(Withdraw::cost)
		).apply(i, Withdraw::new));

		@Override
		public Type<?> type() {
			return WITHDRAW;
		}

		@Override
		public void apply(RuleEvent event, Identifier rule) {
			CullingGame.withdraw(event, cost, rule);
		}
	}

	/** The actor's cursed energy is capped at {@code fraction} of its maximum until they score again. */
	public record CapEnergy(double fraction) implements Effect {
		public static final MapCodec<CapEnergy> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
				Codec.doubleRange(0.05, 1).optionalFieldOf("fraction", 0.5).forGetter(CapEnergy::fraction)
		).apply(i, CapEnergy::new));

		@Override
		public Type<?> type() {
			return CAP_ENERGY;
		}

		@Override
		public void apply(RuleEvent event, Identifier rule) {
			CullingGame.stagnate(event, fraction);
		}
	}

	/** Everyone learns where the leader stands for {@code seconds}. */
	public record RevealLeader(int seconds) implements Effect {
		public static final MapCodec<RevealLeader> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
				Codec.intRange(1, 86_400).optionalFieldOf("seconds", 120).forGetter(RevealLeader::seconds)
		).apply(i, RevealLeader::new));

		@Override
		public Type<?> type() {
			return REVEAL_LEADER;
		}

		@Override
		public void apply(RuleEvent event, Identifier rule) {
			CullingGame.revealLeader(event, seconds);
		}
	}

	/** A Culling Warden is sent to the colony. */
	public record SummonWarden() implements Effect {
		@Override
		public Type<?> type() {
			return SUMMON_WARDEN;
		}

		@Override
		public void apply(RuleEvent event, Identifier rule) {
			CullingGame.summonWarden(event);
		}
	}

	/** A message to the actor ({@code broadcast}: to every participant). */
	public record Message(String key, boolean broadcast) implements Effect {
		public static final MapCodec<Message> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
				Codec.STRING.fieldOf("key").forGetter(Message::key),
				Codec.BOOL.optionalFieldOf("broadcast", false).forGetter(Message::broadcast)
		).apply(i, Message::new));

		@Override
		public Type<?> type() {
			return MESSAGE;
		}

		@Override
		public void apply(RuleEvent event, Identifier rule) {
			CullingGame.message(event, key, broadcast);
		}
	}

	/** The actor spends points to have the Kogane add the rule they asked for. */
	public record Negotiate() implements Effect {
		@Override
		public Type<?> type() {
			return NEGOTIATE;
		}

		@Override
		public void apply(RuleEvent event, Identifier rule) {
			CullingGame.negotiate(event);
		}
	}

	public static final Type<AwardPoints> AWARD_POINTS = register("award_points", AwardPoints.CODEC);
	public static final Type<DeductPoints> DEDUCT_POINTS = register("deduct_points", DeductPoints.CODEC);
	public static final Type<Join> JOIN = register("join", MapCodec.unit(new Join()));
	public static final Type<Confine> CONFINE = register("confine", MapCodec.unit(new Confine()));
	public static final Type<RemoveTechnique> REMOVE_TECHNIQUE = register("remove_technique", MapCodec.unit(new RemoveTechnique()));
	public static final Type<TransferPoints> TRANSFER_POINTS = register("transfer_points", TransferPoints.CODEC);
	public static final Type<Reveal> REVEAL = register("reveal", Reveal.CODEC);
	public static final Type<Substitute> SUBSTITUTE = register("substitute", Substitute.CODEC);
	public static final Type<Withdraw> WITHDRAW = register("withdraw", Withdraw.CODEC);
	public static final Type<CapEnergy> CAP_ENERGY = register("cap_energy", CapEnergy.CODEC);
	public static final Type<RevealLeader> REVEAL_LEADER = register("reveal_leader", RevealLeader.CODEC);
	public static final Type<SummonWarden> SUMMON_WARDEN = register("summon_warden", MapCodec.unit(new SummonWarden()));
	public static final Type<Message> MESSAGE = register("message", Message.CODEC);
	public static final Type<Negotiate> NEGOTIATE = register("negotiate", MapCodec.unit(new Negotiate()));

	private RuleEffects() {
	}

	public static void init() {
		// Loads the class so the static fields register.
	}

	public static <T extends Effect> Type<T> register(String name, MapCodec<T> codec) {
		return Registry.register(REGISTRY, JjkCraft.id(name), new Type<>(codec));
	}
}
