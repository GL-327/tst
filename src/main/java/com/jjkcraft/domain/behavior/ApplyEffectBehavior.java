package com.jjkcraft.domain.behavior;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

import com.jjkcraft.domain.ActiveDomain;

/**
 * Applies a status effect to every target inside on a fixed beat.
 *
 * @param effect    any status effect id
 * @param amplifier effect level minus one
 * @param duration  ticks per application
 * @param interval  ticks between applications
 */
public record ApplyEffectBehavior(Holder<MobEffect> effect, int amplifier, int duration, int interval) implements DomainBehavior {
	public static final MapCodec<ApplyEffectBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			BuiltInRegistries.MOB_EFFECT.holderByNameCodec().fieldOf("effect").forGetter(ApplyEffectBehavior::effect),
			Codec.intRange(0, 9).optionalFieldOf("amplifier", 0).forGetter(ApplyEffectBehavior::amplifier),
			Codec.intRange(1, 1200).fieldOf("duration").forGetter(ApplyEffectBehavior::duration),
			Codec.intRange(1, 1200).optionalFieldOf("interval", 20).forGetter(ApplyEffectBehavior::interval)
	).apply(i, ApplyEffectBehavior::new));

	@Override
	public DomainBehaviorType<?> type() {
		return DomainBehaviors.APPLY_EFFECT;
	}

	@Override
	public void tick(ActiveDomain domain) {
		if (domain.activeTicks() % interval != 0) {
			return;
		}

		for (LivingEntity target : domain.targets()) {
			target.addEffect(new MobEffectInstance(effect, duration, amplifier, false, true), domain.caster());
		}
	}
}
