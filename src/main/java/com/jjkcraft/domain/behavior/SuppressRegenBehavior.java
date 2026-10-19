package com.jjkcraft.domain.behavior;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.world.entity.LivingEntity;

import com.jjkcraft.domain.ActiveDomain;
import com.jjkcraft.domain.DomainService;

/**
 * Smothers the CE regen of targets inside. A Simple Domain doesn't shield from this: it is the
 * domain's air, not a hit.
 *
 * @param multiplier regen multiplier for targets inside (0 stops regen)
 */
public record SuppressRegenBehavior(double multiplier) implements DomainBehavior {
	public static final MapCodec<SuppressRegenBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.doubleRange(0, 1).optionalFieldOf("multiplier", 0.0).forGetter(SuppressRegenBehavior::multiplier)
	).apply(i, SuppressRegenBehavior::new));

	@Override
	public DomainBehaviorType<?> type() {
		return DomainBehaviors.SUPPRESS_REGEN;
	}

	@Override
	public double regenMultiplier(ActiveDomain domain, LivingEntity entity) {
		return multiplier;
	}

	/** True while {@code entity} sits inside any domain that stops its regen (HUD, tests). */
	public static boolean suppressed(LivingEntity entity) {
		return DomainService.all().stream().anyMatch(d -> d.contains(entity) && d.isEnemy(entity)
				&& d.definition().behaviors().stream().anyMatch(b -> b instanceof SuppressRegenBehavior s && s.multiplier < 1));
	}
}
