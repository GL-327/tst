package com.jjkcraft.curse.mechanic;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import com.jjkcraft.JjkCraft;
import com.jjkcraft.curse.CurseEntity;
import com.jjkcraft.particle.JjkParticles;
import com.jjkcraft.sound.JjkSounds;

/**
 * Pale Stilt (Grade 3): a too-tall thing that feeds on the moment its prey leaves the ground. While
 * its target is airborne (jumping, falling, knocked up) it grows strong: faster, much harder
 * hitting, hard to hurt, and its long arms snatch from five blocks away. While the target keeps
 * both feet down it weakens: slow, feeble blows, easy to wound. Fight it on the ground; never jump
 * near it. (It pairs badly for it with a vow against jumping.)
 *
 * MECH_A: 1 while empowered by an airborne target.
 */
public final class StiltMechanic implements CurseMechanic {
	private static final Identifier SPEED = JjkCraft.id("stilt_speed");

	@Override
	public void tick(CurseEntity curse, ServerLevel level) {
		LivingEntity target = curse.getTarget();
		boolean empowered = target != null && target.isAlive() && MechanicUtil.airborne(target);
		if (empowered && curse.mechA() == 0) {
			MechanicUtil.sound(level, curse, JjkSounds.CURSE_AMBIENT, 1.2f, 1.6f);
		}

		curse.setMechA(empowered ? 1 : 0);
		AttributeInstance speed = curse.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed != null) {
			double amount = target == null ? 0 : empowered ? curse.parameter("airborne_speed", 0.5) : curse.parameter("grounded_speed", -0.3);
			speed.addOrUpdateTransientModifier(new AttributeModifier(SPEED, amount, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
		}

		if (!empowered) {
			return;
		}

		if (curse.tickCount % 3 == 0) {
			MechanicUtil.particles(level, JjkParticles.of(JjkParticles.CURSED_SPARK, 0xF2EEE6, 0.7f), curse.getEyePosition(), 3, 0.3, 0.02);
		}

		// The snatch: long arms reach airborne prey well beyond normal reach.
		double reach = curse.parameter("snatch_reach", 5);
		if (MechanicUtil.countDown(curse, "snatch") <= 0 && curse.distanceTo(target) <= reach && curse.getSensing().hasLineOfSight(target)) {
			curse.brain().aimAt(target);
			curse.doHurtTarget(level, target);
			MechanicUtil.set(curse, "snatch", (int) curse.parameter("snatch_interval", 30));
		}
	}

	@Override
	public double attackMultiplier(CurseEntity curse, LivingEntity target) {
		return MechanicUtil.airborne(target) ? curse.parameter("airborne_multiplier", 2.0) : curse.parameter("grounded_multiplier", 0.6);
	}

	@Override
	public float onHurt(CurseEntity curse, ServerLevel level, DamageSource source, float amount) {
		return curse.mechA() == 1 ? amount * (float) curse.parameter("airborne_damage_taken", 0.5)
				: amount * (float) curse.parameter("grounded_damage_taken", 1.25);
	}

	@Override
	public void onRemoved(CurseEntity curse) {
		AttributeInstance speed = curse.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed != null) {
			speed.removeModifier(SPEED);
		}
	}
}
