package com.jjkcraft.curse.mechanic;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import com.jjkcraft.curse.CurseEntity;
import com.jjkcraft.particle.JjkParticles;
import com.jjkcraft.sound.JjkSounds;

/**
 * Cracked Bell (Grade 2): a hanging bell that floats over the fight and tolls. It swings back
 * first (a clear wind-up), then the toll sends a shockwave rolling out along the ground. Anyone
 * standing on the ground when the wave front passes is hit and thrown back; jump over it. Right
 * after each toll the bell is still ringing: it can't attack and takes double damage.
 *
 * MECH_A: 1 while swinging back. MECH_B: wave radius in tenths of a block while a wave rolls (-1 none).
 */
public final class BellMechanic implements CurseMechanic {
	private static final int WAVE_COLOR = 0xD9C9A0;

	@Override
	public void tick(CurseEntity curse, ServerLevel level) {
		rollWave(curse, level);

		if (curse.mechA() == 1) {
			curse.getNavigation().stop();
			if (MechanicUtil.countDown(curse, "swing") <= 0) {
				toll(curse, level);
			}

			return;
		}

		MechanicUtil.countDown(curse, "ringing");
		LivingEntity target = curse.getTarget();
		if (MechanicUtil.countDown(curse, "toll_cooldown") <= 0 && target != null && curse.distanceTo(target) <= curse.parameter("wave_radius", 10)) {
			curse.setMechA(1);
			MechanicUtil.set(curse, "swing", (int) curse.parameter("swing_ticks", 26));
			curse.triggerAnim("action", "cast");
			curse.brain().delayCasting(50);
			MechanicUtil.sound(level, curse, JjkSounds.CHAIN, 0.7f, 0.5f);
		}
	}

	private void toll(CurseEntity curse, ServerLevel level) {
		curse.setMechA(0);
		MechanicUtil.sound(level, curse, JjkSounds.CURSE_TOLL, 2.5f, 1f);
		// The wave starts at the ground under the bell.
		Vec3 origin = curse.position();
		for (int i = 0; i < 12 && origin.y > level.getMinY(); i++) {
			if (!level.getBlockState(net.minecraft.core.BlockPos.containing(origin).below()).isAir()) {
				break;
			}

			origin = origin.subtract(0, 1, 0);
		}

		curse.memory.put("wave_origin", origin);
		curse.memory.put("wave_hit", new HashSet<UUID>());
		MechanicUtil.set(curse, "wave_age", 0);
		curse.setMechB(0);
		MechanicUtil.set(curse, "ringing", (int) curse.parameter("ringing_ticks", 50));
		MechanicUtil.set(curse, "toll_cooldown", (int) curse.parameter("toll_interval", 140));
		MechanicUtil.particles(level, JjkParticles.of(JjkParticles.DOMAIN_SHARD, WAVE_COLOR, 1.2f), curse.position().add(0, curse.getBbHeight() * 0.4, 0), 16, 0.5, 0.1);
	}

	@SuppressWarnings("unchecked")
	private void rollWave(CurseEntity curse, ServerLevel level) {
		if (!(curse.memory.get("wave_origin") instanceof Vec3 origin) || !(curse.memory.get("wave_hit") instanceof Set<?> hitRaw)) {
			return;
		}

		Set<UUID> hit = (Set<UUID>) hitRaw;
		double maxRadius = curse.parameter("wave_radius", 10);
		double speed = curse.parameter("wave_speed", 0.45);
		int age = MechanicUtil.get(curse, "wave_age") + 1;
		MechanicUtil.set(curse, "wave_age", age);
		double radius = age * speed;
		if (radius > maxRadius) {
			curse.memory.remove("wave_origin");
			curse.memory.remove("wave_hit");
			curse.setMechB(-1);
			return;
		}

		curse.setMechB((int) Math.round(radius * 10));
		MechanicUtil.ring(level, JjkParticles.of(JjkParticles.DOMAIN_SHARD, WAVE_COLOR, 0.7f), origin, radius, Math.max(10, (int) (radius * 7)));

		float damage = (float) curse.parameter("wave_damage", 6);
		double knockback = curse.parameter("wave_knockback", 1.0);
		for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(origin, origin).inflate(maxRadius + 1, 3, maxRadius + 1),
				e -> e.isAlive() && !(e instanceof CurseEntity) && !hit.contains(e.getUUID()))) {
			if (victim instanceof net.minecraft.world.entity.player.Player p && (p.isCreative() || p.isSpectator())) {
				continue;
			}

			double dx = victim.getX() - origin.x;
			double dz = victim.getZ() - origin.z;
			double distance = Math.sqrt(dx * dx + dz * dz);
			// Only the wave front, only on the ground, only near the wave's height.
			if (Math.abs(distance - radius) > 0.7 || !victim.onGround() || Math.abs(victim.getY() - origin.y) > 1.5) {
				continue;
			}

			hit.add(victim.getUUID());
			victim.hurtServer(level, MechanicUtil.attack(level, curse), damage);
			MechanicUtil.push(curse, level, victim, origin, knockback);
		}
	}

	@Override
	public float onHurt(CurseEntity curse, ServerLevel level, DamageSource source, float amount) {
		return MechanicUtil.get(curse, "ringing") > 0 ? amount * (float) curse.parameter("ringing_damage_taken", 2) : amount;
	}

	@Override
	public boolean canAttack(CurseEntity curse) {
		// No swings while winding up or while still ringing: its own blow would knock targets over the wave.
		return curse.mechA() == 0 && MechanicUtil.get(curse, "ringing") == 0;
	}
}
