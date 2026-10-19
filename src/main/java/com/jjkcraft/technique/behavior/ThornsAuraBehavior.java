package com.jjkcraft.technique.behavior;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;

import com.jjkcraft.damage.JjkDamageTypes;
import com.jjkcraft.network.payload.TechniqueFxPayload;
import com.jjkcraft.technique.CastContext;
import com.jjkcraft.technique.CastResult;
import com.jjkcraft.technique.TechniqueBehavior;
import com.jjkcraft.technique.TechniqueBehaviorType;
import com.jjkcraft.technique.TechniqueBehaviors;
import com.jjkcraft.technique.TechniqueService;

/**
 * A burning skin (toggle): while it is up, anyone who strikes the caster in melee takes
 * {@code damage} back and catches fire. The caster's own CE pays the upkeep.
 */
public record ThornsAuraBehavior(double damage, int burnSeconds) implements TechniqueBehavior {
	public static final MapCodec<ThornsAuraBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.doubleRange(0, 1000).fieldOf("damage").forGetter(ThornsAuraBehavior::damage),
			Codec.intRange(0, 60).optionalFieldOf("burn_seconds", 3).forGetter(ThornsAuraBehavior::burnSeconds)
	).apply(i, ThornsAuraBehavior::new));

	/** Answers melee blows against anyone wearing the aura. */
	public static void init() {
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, dealt, taken, blocked) -> retaliate(entity, source));
	}

	private static void retaliate(LivingEntity wearer, DamageSource source) {
		if (!(wearer.level() instanceof ServerLevel level) || !(source.getDirectEntity() instanceof LivingEntity attacker) || attacker == wearer
				|| source.getEntity() != attacker
				|| !(source.is(net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK) || source.is(net.minecraft.world.damagesource.DamageTypes.MOB_ATTACK))) {
			// Only plain melee blows: never techniques, so two auras can't trade burns forever.
			return;
		}

		TechniqueService.active(wearer).ifPresent(active -> {
			if (active.behavior() instanceof ThornsAuraBehavior aura) {
				attacker.hurtServer(level, level.damageSources().source(JjkDamageTypes.TECHNIQUE, wearer), (float) (aura.damage() * active.context().power()));
				attacker.igniteForSeconds(aura.burnSeconds());
			}
		});
	}

	@Override
	public TechniqueBehaviorType<?> type() {
		return TechniqueBehaviors.THORNS_AURA;
	}

	@Override
	public CastResult cast(CastContext ctx) {
		ctx.fx(TechniqueFxPayload.Phase.CHANNEL_START, ctx.caster().position().add(0, 1, 0));
		return CastResult.done(ctx.caster().position());
	}

	@Override
	public boolean tick(CastContext ctx, int ticksActive) {
		if (ticksActive % 10 == 0) {
			ctx.fx(TechniqueFxPayload.Phase.FIELD, ctx.caster().position().add(0, 0.1, 0), new net.minecraft.world.phys.Vec3(1.1, 10, 0), java.util.List.of());
		}

		return true;
	}
}
