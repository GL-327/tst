package com.jjkcraft.technique.behavior;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import com.jjkcraft.damage.JjkDamageTypes;
import com.jjkcraft.effect.JjkEffects;
import com.jjkcraft.network.payload.TechniqueFxPayload;
import com.jjkcraft.sorcerer.Progression;
import com.jjkcraft.technique.CastContext;
import com.jjkcraft.technique.CastResult;
import com.jjkcraft.technique.TechniqueBehavior;
import com.jjkcraft.technique.TechniqueBehaviorType;
import com.jjkcraft.technique.TechniqueBehaviors;

/**
 * Cursed speech: a shouted command that every enemy in range and in sight obeys for a moment.
 * Commanding stronger targets strains the caster's throat (backlash damage).
 *
 * @param command  STOP (freeze), KNOCKDOWN (slam down) or LEAP (launch up)
 * @param radius   range of the shout
 * @param duration ticks the command holds
 * @param damage   damage dealt by the command (KNOCKDOWN)
 * @param strain   backlash per target; multiplied when the target outranks the caster
 */
public record CursedSpeechBehavior(Command command, double radius, int duration, double damage, double strain) implements TechniqueBehavior {
	public static final MapCodec<CursedSpeechBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Command.CODEC.fieldOf("command").forGetter(CursedSpeechBehavior::command),
			Codec.doubleRange(1, 64).fieldOf("radius").forGetter(CursedSpeechBehavior::radius),
			Codec.intRange(1, 600).optionalFieldOf("duration", 40).forGetter(CursedSpeechBehavior::duration),
			Codec.doubleRange(0, 1000).optionalFieldOf("damage", 0.0).forGetter(CursedSpeechBehavior::damage),
			Codec.doubleRange(0, 100).optionalFieldOf("strain", 0.5).forGetter(CursedSpeechBehavior::strain)
	).apply(i, CursedSpeechBehavior::new));

	public enum Command implements StringRepresentable {
		STOP("stop"), KNOCKDOWN("knockdown"), LEAP("leap");

		public static final Codec<Command> CODEC = StringRepresentable.fromEnum(Command::values);

		private final String name;

		Command(String name) {
			this.name = name;
		}

		@Override
		public String getSerializedName() {
			return name;
		}
	}

	@Override
	public TechniqueBehaviorType<?> type() {
		return TechniqueBehaviors.CURSED_SPEECH;
	}

	@Override
	public CastResult cast(CastContext ctx) {
		List<LivingEntity> obeyed = new ArrayList<>();
		int casterRank = Progression.gradeRank(ctx.level().registryAccess(), ctx.caster());
		double backlash = 0;
		for (LivingEntity target : ctx.enemiesInRadius(ctx.caster().position(), radius)) {
			if (!ctx.caster().hasLineOfSight(target)) {
				continue;
			}

			int duration = (int) Math.round(this.duration * Math.min(2.0, ctx.power()));
			switch (command) {
				case STOP -> target.addEffect(new MobEffectInstance(JjkEffects.BOUND, duration, 0, false, true), ctx.caster());
				case KNOCKDOWN -> {
					target.setDeltaMovement(target.getDeltaMovement().multiply(0.2, 0, 0.2).add(0, -1.2, 0));
					target.needsSync = true;
					target.addEffect(new MobEffectInstance(JjkEffects.BOUND, duration / 2, 0, false, true), ctx.caster());
					ctx.hurt(target, damage * ctx.power(), false);
				}
				case LEAP -> {
					target.setDeltaMovement(target.getDeltaMovement().multiply(0.3, 0, 0.3).add(0, 1.1, 0));
					target.needsSync = true;
				}
			}

			int targetRank = Progression.gradeRank(ctx.level().registryAccess(), target);
			backlash += strain * Math.max(0.5, targetRank - casterRank + 1);
			obeyed.add(target);
		}

		if (backlash > 0) {
			ctx.caster().hurtServer(ctx.level(), ctx.level().damageSources().source(JjkDamageTypes.BACKLASH, ctx.caster()), (float) backlash);
		}

		ctx.fx(TechniqueFxPayload.Phase.CAST, ctx.eye(), new Vec3(radius, 0, 0), obeyed);
		return CastResult.hit(List.copyOf(obeyed), ctx.eye());
	}
}
