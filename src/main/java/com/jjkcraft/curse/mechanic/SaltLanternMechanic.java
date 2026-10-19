package com.jjkcraft.curse.mechanic;

import java.util.Optional;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import com.jjkcraft.curse.CurseEntity;
import com.jjkcraft.energy.CursedEnergy;
import com.jjkcraft.particle.JjkParticles;
import com.jjkcraft.sound.JjkSounds;

/**
 * Salt Lantern (Grade 3): its lantern gathers bitter light and fires it as a beam. The light burns
 * cursed energy: the more CE a target carries, the worse it burns, and it drains some. Someone with
 * no CE barely feels it. A thin aiming line shows where it will fire; the aim locks just before
 * the shot, so stepping aside works. A raised shield facing the beam, or any block in the way,
 * stops it. One heavy blow while it gathers light snuffs the shot.
 *
 * MECH_A: 1 while gathering light. MECH_B: charge ticks left (-1 when idle).
 */
public final class SaltLanternMechanic implements CurseMechanic {
	private static final int LIGHT_COLOR = 0xF4E9B0;

	@Override
	public void tick(CurseEntity curse, ServerLevel level) {
		int charge = curse.mechB();
		LivingEntity target = curse.getTarget();
		if (charge > 0) {
			curse.getNavigation().stop();
			int lock = (int) curse.parameter("aim_lock_ticks", 8);
			if (target != null && charge > lock) {
				curse.memory.put("aim", target.position().add(0, target.getBbHeight() * 0.6, 0));
				curse.brain().aimAt(target);
			}

			Vec3 aim = (Vec3) curse.memory.get("aim");
			if (aim != null && charge % 2 == 0) {
				MechanicUtil.line(level, JjkParticles.of(JjkParticles.CURSED_SPARK, LIGHT_COLOR, 0.25f), lantern(curse), aim, 16);
			}

			curse.setMechB(charge - 1);
			if (charge == 1 && aim != null) {
				fire(curse, level, aim);
			}

			return;
		}

		if (MechanicUtil.countDown(curse, "beam_cooldown") <= 0 && target != null && curse.distanceTo(target) <= curse.parameter("beam_range", 16)
				&& curse.getSensing().hasLineOfSight(target)) {
			curse.setMechA(1);
			curse.setMechB((int) curse.parameter("charge_ticks", 30));
			curse.memory.put("aim", target.position().add(0, target.getBbHeight() * 0.6, 0));
			curse.triggerAnim("action", "cast");
			curse.brain().delayCasting(40);
			MechanicUtil.sound(level, curse, JjkSounds.HEAL, 0.8f, 0.5f);
		}
	}

	private static Vec3 lantern(CurseEntity curse) {
		return curse.position().add(0, curse.getBbHeight() * 0.6, 0);
	}

	private void fire(CurseEntity curse, ServerLevel level, Vec3 aim) {
		curse.setMechA(0);
		curse.setMechB(-1);
		MechanicUtil.set(curse, "beam_cooldown", (int) curse.parameter("beam_interval", 100));
		Vec3 from = lantern(curse);
		Vec3 dir = aim.subtract(from).normalize();
		double range = curse.parameter("beam_range", 16) + 2;
		Vec3 end = from.add(dir.scale(range));

		// Blocks stop the light.
		HitResult block = level.clip(new ClipContext(from, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, curse));
		if (block.getType() != HitResult.Type.MISS) {
			end = block.getLocation();
		}

		MechanicUtil.line(level, JjkParticles.of(JjkParticles.REVERSE_GLOW, LIGHT_COLOR, 0.9f), from, end, (int) (from.distanceTo(end) * 3));
		MechanicUtil.sound(level, curse, JjkSounds.BOLT_FIRE, 1.2f, 1.5f);

		Vec3 beamEnd = end;
		Optional<LivingEntity> hit = level.getEntitiesOfClass(LivingEntity.class, new AABB(from, beamEnd).inflate(0.6),
						e -> e != curse && e.isAlive() && !(e instanceof CurseEntity) && !(e instanceof Player p && (p.isCreative() || p.isSpectator()))
								&& e.getBoundingBox().inflate(0.4).clip(from, beamEnd).isPresent())
				.stream().min((a, b) -> Double.compare(a.distanceToSqr(from), b.distanceToSqr(from)));
		if (hit.isEmpty()) {
			return;
		}

		LivingEntity victim = hit.get();
		// A raised shield facing the lantern takes the light.
		if (victim.isBlocking() && victim.getLookAngle().dot(dir) < -0.3) {
			MechanicUtil.particles(level, JjkParticles.of(JjkParticles.CURSED_SPARK, LIGHT_COLOR, 1f), victim.getEyePosition(), 12, 0.3, 0.1);
			MechanicUtil.sound(level, curse, JjkSounds.BARRIER, 1f, 1.6f);
			return;
		}

		double ce = CursedEnergy.current(victim);
		float damage = (float) (curse.parameter("beam_damage", 2) + ce * curse.parameter("burn_per_ce", 0.05));
		victim.hurtServer(level, level.damageSources().indirectMagic(curse, curse), damage);
		if (ce > 0) {
			CursedEnergy.drain(victim, CursedEnergy.max(victim) * curse.parameter("drain_fraction", 0.08));
			victim.igniteForSeconds((float) curse.parameter("burn_seconds", 2));
		}
	}

	@Override
	public float onHurt(CurseEntity curse, ServerLevel level, DamageSource source, float amount) {
		if (curse.mechB() > 0 && amount >= curse.parameter("snuff_threshold", 6)) {
			curse.setMechA(0);
			curse.setMechB(-1);
			MechanicUtil.set(curse, "beam_cooldown", (int) curse.parameter("snuff_ticks", 80));
			MechanicUtil.sound(level, curse, JjkSounds.CURSE_HURT, 1f, 0.6f);
			level.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE, curse.getX(), curse.getY() + curse.getBbHeight() * 0.6,
					curse.getZ(), 10, 0.15, 0.15, 0.15, 0.02);
		}

		return amount;
	}

	@Override
	public boolean canAttack(CurseEntity curse) {
		return curse.mechB() <= 0;
	}
}
