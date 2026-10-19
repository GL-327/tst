package com.jjkcraft.domain.behavior;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import com.jjkcraft.domain.ActiveDomain;
import com.jjkcraft.domain.DomainService;
import com.jjkcraft.effect.JjkEffects;

/**
 * The information flood. In surges, every target inside is overloaded: slowed, unable to cast, and
 * (for players) their view and HUD drown in noise for a few seconds. Mobs simply freeze.
 *
 * @param surgeInterval ticks between surges
 * @param surgeDuration ticks each surge lasts
 */
public record OverloadBehavior(int surgeInterval, int surgeDuration) implements DomainBehavior {
	public static final MapCodec<OverloadBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.intRange(1, 1200).fieldOf("surge_interval").forGetter(OverloadBehavior::surgeInterval),
			Codec.intRange(1, 1200).fieldOf("surge_duration").forGetter(OverloadBehavior::surgeDuration)
	).apply(i, OverloadBehavior::new));

	@Override
	public DomainBehaviorType<?> type() {
		return DomainBehaviors.OVERLOAD;
	}

	@Override
	public void open(ActiveDomain domain) {
		surge(domain);
	}

	@Override
	public void tick(ActiveDomain domain) {
		if (domain.activeTicks() > 0 && domain.activeTicks() % surgeInterval == 0) {
			surge(domain);
		}
	}

	private void surge(ActiveDomain domain) {
		int ticks = (int) Math.round(surgeDuration * Math.min(1.5, domain.power()));
		int hit = 0;
		for (LivingEntity target : domain.targets()) {
			target.addEffect(new MobEffectInstance(JjkEffects.OVERLOADED, ticks, 0, false, true), domain.caster());
			if (!(target instanceof Player)) {
				target.addEffect(new MobEffectInstance(JjkEffects.STUNNED, ticks, 0, false, true), domain.caster());
			}

			hit++;
		}

		domain.event(DomainService.Event.PULSE, hit);
	}
}
