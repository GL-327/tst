package com.jjkcraft.domain.behavior;

import java.util.List;
import java.util.Locale;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

import com.jjkcraft.domain.ActiveDomain;
import com.jjkcraft.domain.DomainService;
import com.jjkcraft.effect.JjkEffects;
import com.jjkcraft.energy.CursedEnergy;

/**
 * The gamble: on every spin the domain rolls one weighted outcome. The same winning outcome
 * several spins in a row is a jackpot: the caster refills their CE and is briefly hard to kill.
 *
 * @param spinInterval  ticks between rolls
 * @param outcomes      what a roll can do, by weight
 * @param jackpotStreak identical winning rolls in a row for a jackpot
 * @param jackpotTicks  duration of the jackpot's regeneration and resistance
 */
public record GambleBehavior(int spinInterval, List<Outcome> outcomes, int jackpotStreak, int jackpotTicks) implements DomainBehavior {
	public enum Kind implements StringRepresentable {
		/** Damage every target. */
		DAMAGE,
		/** Heal the caster. */
		HEAL,
		/** Restore the caster's CE. */
		ENERGY,
		/** Stun every target for {@code amount} ticks. */
		STUN,
		/** Nothing happens. */
		WHIFF;

		public static final Codec<Kind> CODEC = StringRepresentable.fromEnum(Kind::values);

		@Override
		public String getSerializedName() {
			return name().toLowerCase(Locale.ROOT);
		}
	}

	public record Outcome(Kind kind, int weight, double amount) {
		public static final Codec<Outcome> CODEC = RecordCodecBuilder.create(i -> i.group(
				Kind.CODEC.fieldOf("kind").forGetter(Outcome::kind),
				Codec.intRange(1, 10_000).fieldOf("weight").forGetter(Outcome::weight),
				Codec.doubleRange(0, 10_000).optionalFieldOf("amount", 0.0).forGetter(Outcome::amount)
		).apply(i, Outcome::new));
	}

	public static final MapCodec<GambleBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.intRange(1, 1200).fieldOf("spin_interval").forGetter(GambleBehavior::spinInterval),
			Outcome.CODEC.listOf(1, 16).fieldOf("outcomes").forGetter(GambleBehavior::outcomes),
			Codec.intRange(2, 10).optionalFieldOf("jackpot_streak", 3).forGetter(GambleBehavior::jackpotStreak),
			Codec.intRange(0, 2400).optionalFieldOf("jackpot_ticks", 200).forGetter(GambleBehavior::jackpotTicks)
	).apply(i, GambleBehavior::new));

	@Override
	public DomainBehaviorType<?> type() {
		return DomainBehaviors.GAMBLE;
	}

	@Override
	public void tick(ActiveDomain domain) {
		if (domain.activeTicks() % spinInterval != 0) {
			return;
		}

		int index = roll(domain);
		Outcome outcome = outcomes.get(index);
		apply(domain, outcome);
		domain.event(DomainService.Event.GAMBLE, index);

		int last = domain.memory().get("gamble_last") instanceof Integer l ? l : -1;
		int streak = (last == index && outcome.kind() != Kind.WHIFF) ? (domain.memory().get("gamble_streak") instanceof Integer s ? s : 1) + 1 : 1;
		domain.memory().put("gamble_last", index);
		domain.memory().put("gamble_streak", streak);
		if (streak >= jackpotStreak) {
			jackpot(domain);
			domain.memory().put("gamble_streak", 0);
			domain.memory().put("gamble_last", -1);
		}
	}

	int roll(ActiveDomain domain) {
		int total = outcomes.stream().mapToInt(Outcome::weight).sum();
		int roll = domain.level().getRandom().nextInt(total);
		for (int i = 0; i < outcomes.size(); i++) {
			roll -= outcomes.get(i).weight();
			if (roll < 0) {
				return i;
			}
		}

		return outcomes.size() - 1;
	}

	private static void apply(ActiveDomain domain, Outcome outcome) {
		double amount = outcome.amount() * domain.power();
		switch (outcome.kind()) {
			case DAMAGE -> domain.targets().forEach(t -> domain.hurt(t, amount));
			case HEAL -> domain.caster().heal((float) amount);
			case ENERGY -> CursedEnergy.restore(domain.caster(), amount);
			case STUN -> {
				for (LivingEntity target : domain.targets()) {
					target.addEffect(new MobEffectInstance(JjkEffects.STUNNED, (int) Math.round(amount), 0, false, true), domain.caster());
				}
			}
			case WHIFF -> {
			}
		}
	}

	private void jackpot(ActiveDomain domain) {
		LivingEntity caster = domain.caster();
		CursedEnergy.fill(caster);
		caster.addEffect(new MobEffectInstance(MobEffects.REGENERATION, jackpotTicks, 2, false, true));
		caster.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, jackpotTicks, 1, false, true));
		domain.event(DomainService.Event.JACKPOT, jackpotTicks);
	}
}
