package com.jjkcraft.domain.behavior;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

import com.jjkcraft.domain.ActiveDomain;
import com.jjkcraft.domain.DomainService;

/**
 * Perfect repetition: one recitation strikes every target, and repeats exactly {@code repeats}
 * times, no more and no less. After the last one the domain closes itself.
 *
 * @param repeats  how many times the recitation lands
 * @param interval ticks between recitations
 * @param damage   damage per recitation at power 1
 * @param effect   optional effect each recitation applies
 * @param effectTicks duration of that effect
 */
public record RepetitionBehavior(int repeats, int interval, double damage, Optional<Holder<MobEffect>> effect, int effectTicks)
		implements DomainBehavior {
	public static final MapCodec<RepetitionBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.intRange(1, 100).fieldOf("repeats").forGetter(RepetitionBehavior::repeats),
			Codec.intRange(1, 1200).fieldOf("interval").forGetter(RepetitionBehavior::interval),
			Codec.doubleRange(0, 1000).fieldOf("damage").forGetter(RepetitionBehavior::damage),
			BuiltInRegistries.MOB_EFFECT.holderByNameCodec().optionalFieldOf("effect").forGetter(RepetitionBehavior::effect),
			Codec.intRange(0, 1200).optionalFieldOf("effect_ticks", 40).forGetter(RepetitionBehavior::effectTicks)
	).apply(i, RepetitionBehavior::new));

	private static final String KEY = "recitations";

	@Override
	public DomainBehaviorType<?> type() {
		return DomainBehaviors.REPETITION;
	}

	public static int done(ActiveDomain domain) {
		return domain.memory().get(KEY) instanceof Integer n ? n : 0;
	}

	@Override
	public void tick(ActiveDomain domain) {
		int done = done(domain);
		if (done >= repeats) {
			domain.requestClose();
			return;
		}

		if (domain.activeTicks() == 0 || domain.activeTicks() % interval != 0) {
			return;
		}

		for (LivingEntity target : domain.targets()) {
			domain.hurt(target, damage * domain.power());
			effect.ifPresent(e -> target.addEffect(new MobEffectInstance(e, effectTicks, 0, false, true), domain.caster()));
		}

		done++;
		domain.memory().put(KEY, done);
		domain.event(DomainService.Event.REPEAT, done);
		if (done >= repeats) {
			domain.requestClose();
		}
	}
}
