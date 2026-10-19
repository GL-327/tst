package com.jjkcraft.domain.behavior;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.world.entity.LivingEntity;

import com.jjkcraft.domain.ActiveDomain;
import com.jjkcraft.domain.DomainService;

/**
 * Damages every target inside on a fixed beat: the cutting field.
 *
 * @param interval ticks between pulses
 * @param damage   damage per pulse at power 1
 */
public record SureHitDamageBehavior(int interval, double damage) implements DomainBehavior {
	public static final MapCodec<SureHitDamageBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.intRange(1, 1200).fieldOf("interval").forGetter(SureHitDamageBehavior::interval),
			Codec.doubleRange(0, 1000).fieldOf("damage").forGetter(SureHitDamageBehavior::damage)
	).apply(i, SureHitDamageBehavior::new));

	@Override
	public DomainBehaviorType<?> type() {
		return DomainBehaviors.SURE_HIT_DAMAGE;
	}

	@Override
	public void tick(ActiveDomain domain) {
		if (domain.activeTicks() % interval != 0) {
			return;
		}

		int hits = 0;
		for (LivingEntity target : domain.targets()) {
			if (domain.hurt(target, damage * domain.power())) {
				hits++;
			}
		}

		domain.event(DomainService.Event.PULSE, hits);
	}
}
