package com.jjkcraft.curse.mechanic;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import com.jjkcraft.curse.CurseEntity;
import com.jjkcraft.effect.JjkEffects;
import com.jjkcraft.particle.JjkParticles;
import com.jjkcraft.sound.JjkSounds;

/**
 * Hollow Hymnal (Grade 4): a floating choir mask. Every few seconds it draws breath (its mouth
 * glows, a clear telegraph) and sings: everyone in the sound zone is stunned. A melee hit to its
 * mouth (from the front) silences it for ten seconds, and a silenced Hymnal takes extra damage.
 *
 * MECH_A: 0 idle, 1 drawing breath, 2 silenced.
 */
public final class HymnalMechanic implements CurseMechanic {
	@Override
	public void tick(CurseEntity curse, ServerLevel level) {
		int state = curse.mechA();
		if (state == 2) {
			if (MechanicUtil.countDown(curse, "silence") <= 0) {
				curse.setMechA(0);
			}

			return;
		}

		if (state == 1) {
			if (curse.tickCount % 4 == 0) {
				MechanicUtil.particles(level, JjkParticles.of(JjkParticles.CE_AURA, 0xBEA0DC, 0.9f), curse.getEyePosition(), 3, 0.2, 0.02);
			}

			if (MechanicUtil.countDown(curse, "charge") <= 0) {
				sing(curse, level);
			}

			return;
		}

		LivingEntity target = curse.getTarget();
		int radius = (int) curse.parameter("hymn_radius", 5);
		if (MechanicUtil.countDown(curse, "cooldown") <= 0 && target != null && target.distanceTo(curse) < radius * 1.6) {
			curse.setMechA(1);
			MechanicUtil.set(curse, "charge", (int) curse.parameter("hymn_charge", 24));
			curse.triggerAnim("action", "cast");
			curse.brain().delayCasting(40);
		}
	}

	private void sing(CurseEntity curse, ServerLevel level) {
		double radius = curse.parameter("hymn_radius", 5);
		int stun = (int) curse.parameter("hymn_stun", 30);
		for (LivingEntity victim : MechanicUtil.victims(level, curse, radius)) {
			victim.addEffect(new MobEffectInstance(JjkEffects.STUNNED, stun, 0, false, true), curse);
		}

		MechanicUtil.sound(level, curse, JjkSounds.SPEECH, 1.4f, 0.6f);
		MechanicUtil.ring(level, JjkParticles.of(JjkParticles.DOMAIN_SHARD, 0xBEA0DC, 1f), curse.position(), radius, 32);
		curse.setMechA(0);
		MechanicUtil.set(curse, "cooldown", (int) curse.parameter("hymn_interval", 120));
	}

	@Override
	public float onHurt(CurseEntity curse, ServerLevel level, DamageSource source, float amount) {
		if (curse.mechA() == 2) {
			return amount * 1.5f;
		}

		// A melee hit from the front lands on the mouth.
		if (source.getDirectEntity() instanceof LivingEntity attacker && source.getDirectEntity() == source.getEntity()) {
			Vec3 toAttacker = attacker.position().subtract(curse.position()).multiply(1, 0, 1).normalize();
			Vec3 facing = curse.getLookAngle().multiply(1, 0, 1).normalize();
			if (toAttacker.dot(facing) > Math.cos(Math.toRadians(50))) {
				curse.setMechA(2);
				MechanicUtil.set(curse, "silence", (int) curse.parameter("silence_duration", 200));
				MechanicUtil.sound(level, curse, JjkSounds.CURSE_HURT, 1f, 1.6f);
				MechanicUtil.particles(level, JjkParticles.of(JjkParticles.CURSED_SPARK, 0xFFFFFF, 1f), curse.getEyePosition(), 10, 0.3, 0.1);
				return amount * 1.5f;
			}
		}

		return amount;
	}
}
