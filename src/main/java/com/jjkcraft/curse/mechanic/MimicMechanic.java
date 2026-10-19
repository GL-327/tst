package com.jjkcraft.curse.mechanic;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import com.jjkcraft.curse.CurseEntity;
import com.jjkcraft.effect.JjkEffects;
import com.jjkcraft.particle.JjkParticles;

/**
 * Gilded Mimic (Grade 3): a chest that is not a chest. Disguised, it waits; step within reach,
 * or study it up close, and it springs: a tongue lashes out, grabs the nearest victim and drags
 * them in while the mimic backs away. Hit it hard during the grab to break free. Lose it for long
 * enough and it settles back into a chest.
 *
 * MECH_A: 0 disguised, 1 awake, 2 winding up a lash, 3 grabbing. MECH_B: the grabbed entity's id
 * (-1 for none).
 */
public final class MimicMechanic implements CurseMechanic {
	public static final int DISGUISED = 0;
	public static final int AWAKE = 1;
	public static final int WINDUP = 2;
	public static final int GRABBING = 3;
	private static final int TONGUE = 0xD9A441;

	@Override
	public void onSpawn(CurseEntity curse, ServerLevel level) {
		curse.setDisguised(true);
		curse.setMechA(DISGUISED);
		curse.setMechB(-1);
	}

	@Override
	public void tick(CurseEntity curse, ServerLevel level) {
		switch (curse.mechA()) {
			case DISGUISED -> waitAsChest(curse, level);
			case GRABBING -> grab(curse, level);
			case WINDUP -> windup(curse, level);
			default -> awake(curse, level);
		}
	}

	private void waitAsChest(CurseEntity curse, ServerLevel level) {
		curse.setDeltaMovement(0, Math.min(0, curse.getDeltaMovement().y), 0);
		curse.getNavigation().stop();
		double wake = curse.parameter("wake_distance", 2.5);
		double look = curse.parameter("look_distance", 5);
		for (Player player : level.players()) {
			if (player.isSpectator() || player.isCreative()) {
				continue;
			}

			double d = player.distanceTo(curse);
			if (d <= wake || (d <= look && MechanicUtil.isLookingAt(player, curse, look))) {
				spring(curse, level, player);
				return;
			}
		}
	}

	/** Wakes and lashes at {@code victim} at once. Public for tests. */
	public static void spring(CurseEntity curse, ServerLevel level, LivingEntity victim) {
		curse.setDisguised(false);
		curse.setTarget(victim);
		MechanicUtil.set(curse, "lost", 0);
		level.playSound(null, curse.getX(), curse.getY(), curse.getZ(), SoundEvents.CHEST_OPEN, SoundSource.HOSTILE, 1.2f, 0.5f);
		MechanicUtil.particles(level, JjkParticles.of(JjkParticles.CURSED_SPARK, TONGUE, 0.9f), curse.position().add(0, 0.6, 0), 20, 0.4, 0.1);
		startGrab(curse, level, victim);
	}

	private void awake(CurseEntity curse, ServerLevel level) {
		LivingEntity target = curse.getTarget();
		if (target == null || !target.isAlive()) {
			int lost = MechanicUtil.get(curse, "lost") + 1;
			MechanicUtil.set(curse, "lost", lost);
			if (lost >= curse.parameter("settle_ticks", 240)) {
				// Settles back into a chest where it stands.
				curse.setDisguised(true);
				curse.setMechA(DISGUISED);
				level.playSound(null, curse.getX(), curse.getY(), curse.getZ(), SoundEvents.CHEST_CLOSE, SoundSource.HOSTILE, 1f, 0.7f);
			}

			return;
		}

		MechanicUtil.set(curse, "lost", 0);
		if (MechanicUtil.countDown(curse, "lash") <= 0 && curse.distanceTo(target) <= curse.parameter("grab_range", 6)
				&& curse.getSensing().hasLineOfSight(target)) {
			curse.setMechA(WINDUP);
			MechanicUtil.set(curse, "windup", (int) curse.parameter("lash_windup", 12));
			level.playSound(null, curse.getX(), curse.getY(), curse.getZ(), SoundEvents.CHEST_LOCKED, SoundSource.HOSTILE, 1f, 0.6f);
		}
	}

	private void windup(CurseEntity curse, ServerLevel level) {
		int left = MechanicUtil.get(curse, "windup") - 1;
		MechanicUtil.set(curse, "windup", left);
		curse.getNavigation().stop();
		LivingEntity target = curse.getTarget();
		if (left % 3 == 0) {
			MechanicUtil.particles(level, JjkParticles.of(JjkParticles.CURSED_SPARK, TONGUE, 0.6f), curse.position().add(0, 0.8, 0), 4, 0.3, 0.02);
		}

		if (left <= 0) {
			if (target != null && target.isAlive() && curse.distanceTo(target) <= curse.parameter("grab_range", 6) + 1) {
				startGrab(curse, level, target);
			} else {
				curse.setMechA(AWAKE);
			}
		}
	}

	private static void startGrab(CurseEntity curse, ServerLevel level, LivingEntity victim) {
		curse.setMechA(GRABBING);
		curse.setMechB(victim.getId());
		MechanicUtil.set(curse, "grab", (int) curse.parameter("grab_ticks", 60));
		MechanicUtil.set(curse, "grab_hurt", 0);
		MechanicUtil.set(curse, "lash", (int) curse.parameter("lash_interval", 160));
		level.playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.SLIME_ATTACK, SoundSource.HOSTILE, 1f, 0.6f);
	}

	/** The victim currently held, if any. Public for tests. */
	public static LivingEntity grabbed(CurseEntity curse) {
		Entity entity = curse.mechA() == GRABBING ? curse.level().getEntity(curse.mechB()) : null;
		return entity instanceof LivingEntity living ? living : null;
	}

	private void grab(CurseEntity curse, ServerLevel level) {
		LivingEntity victim = grabbed(curse);
		int left = MechanicUtil.get(curse, "grab") - 1;
		MechanicUtil.set(curse, "grab", left);
		if (victim == null || !victim.isAlive() || left <= 0 || victim.distanceTo(curse) > curse.parameter("grab_range", 6) + 4) {
			release(curse);
			return;
		}

		Vec3 mouth = curse.position().add(0, curse.getBbHeight() * 0.6, 0);
		// A steady drag along the ground (a one-off pull every tick would launch them).
		Vec3 toward = mouth.subtract(victim.position()).multiply(1, 0, 1);
		if (toward.lengthSqr() > 1.0) {
			Vec3 drag = toward.normalize().scale(curse.parameter("drag_strength", 0.35));
			victim.setDeltaMovement(drag.x, Math.min(victim.getDeltaMovement().y, 0.05), drag.z);
			victim.needsSync = true;
		}
		victim.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 10, 2, false, false));
		MechanicUtil.line(level, JjkParticles.of(JjkParticles.CURSED_SPARK, TONGUE, 0.5f), mouth, victim.position().add(0, victim.getBbHeight() * 0.5, 0), 8);

		// It backs away, dragging its catch from the others.
		Vec3 away = curse.position().subtract(victim.position()).multiply(1, 0, 1);
		if (away.lengthSqr() > 1e-4) {
			curse.setDeltaMovement(away.normalize().scale(curse.parameter("back_speed", 0.12)).add(0, curse.getDeltaMovement().y, 0));
		}

		if (left % 20 == 0) {
			victim.hurtServer(level, MechanicUtil.attack(level, curse), (float) curse.parameter("grab_damage", 2));
		}
	}

	private static void release(CurseEntity curse) {
		curse.setMechA(AWAKE);
		curse.setMechB(-1);
	}

	@Override
	public boolean canAct(CurseEntity curse) {
		return curse.mechA() == AWAKE;
	}

	@Override
	public float onHurt(CurseEntity curse, ServerLevel level, DamageSource source, float amount) {
		if (curse.mechA() == DISGUISED) {
			if (source.getEntity() instanceof LivingEntity attacker) {
				spring(curse, level, attacker);
			}

			return amount * (float) curse.parameter("chest_damage_taken", 0.5);
		}

		if (curse.mechA() == GRABBING) {
			int hurt = MechanicUtil.get(curse, "grab_hurt") + Math.round(amount);
			MechanicUtil.set(curse, "grab_hurt", hurt);
			if (hurt >= curse.parameter("break_damage", 6)) {
				// Struck hard enough: it lets go and reels.
				release(curse);
				curse.addEffect(new MobEffectInstance(JjkEffects.STUNNED, 20, 0, false, true));
				level.playSound(null, curse.getX(), curse.getY(), curse.getZ(), SoundEvents.CHEST_CLOSE, SoundSource.HOSTILE, 1.2f, 1.4f);
			}
		}

		return amount;
	}
}
