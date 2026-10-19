package com.jjkcraft.curse.mechanic;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import com.jjkcraft.curse.CurseEntity;
import com.jjkcraft.particle.JjkParticles;
import com.jjkcraft.sound.JjkSounds;

/**
 * Ferryman of Unfinished Roads (Special Grade): an arena boss that keeps a shore where it was
 * found. On a timer it opens the crossing: for several seconds a grey current runs across the
 * arena toward the shore and drags everyone with it. Whoever reaches the shore is ferried: hurt
 * badly, withered and thrown back into the water. Walk or sprint against the current; between
 * crossings, strike. The boss bar counts down to the next crossing. Below half health the
 * crossings come sooner and pull harder.
 *
 * MECH_A: 1 while the crossing is open. MECH_B: seconds to the next crossing.
 */
public final class FerrymanMechanic implements CurseMechanic {
	private static final int MIST = 0xB8C4C8;

	@Override
	public void onSpawn(CurseEntity curse, ServerLevel level) {
		curse.setPersistenceRequired();
		curse.memory.put("shore", curse.position());
		MechanicUtil.set(curse, "next_crossing", (int) curse.parameter("first_crossing", 200));
	}

	/** The shore the Ferryman keeps (where it was found). */
	public static Vec3 shore(CurseEntity curse) {
		if (!(curse.memory.get("shore") instanceof Vec3 shore)) {
			curse.memory.put("shore", curse.position());
			return curse.position();
		}

		return shore;
	}

	private static boolean second(CurseEntity curse) {
		return curse.getHealth() / curse.getMaxHealth() <= 0.5;
	}

	@Override
	public void tick(CurseEntity curse, ServerLevel level) {
		Vec3 shore = shore(curse);
		double arena = curse.parameter("arena_radius", 22);
		boolean open = curse.mechA() == 1;
		int left = MechanicUtil.get(curse, open ? "crossing" : "next_crossing");
		Component title = Component.translatable(open ? "boss.jjkcraft.ferryman.crossing" : "boss.jjkcraft.ferryman.next",
				curse.getDisplayName(), Math.max(0, left / 20));
		MechanicUtil.bossBar(curse, level, curse.parameter("bar_range", 48), BossEvent.BossBarColor.BLUE, title);
		curse.setMechB(open ? 0 : left / 20);

		// It never strays far from its shore.
		if (curse.position().distanceTo(shore) > curse.parameter("leash", 6) && curse.tickCount % 20 == 0) {
			curse.getNavigation().moveTo(shore.x, shore.y, shore.z, 1.2);
		}

		if (!open) {
			// Only count down while travellers (anyone living who is not a curse) are in the arena.
			boolean anyone = !level.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(shore, shore).inflate(arena),
					e -> e.isAlive() && !(e instanceof CurseEntity) && !(e instanceof Player p && (p.isSpectator() || p.isCreative()))).isEmpty();
			if (anyone && MechanicUtil.countDown(curse, "next_crossing") <= 0) {
				curse.setMechA(1);
				MechanicUtil.set(curse, "crossing", (int) curse.parameter(second(curse) ? "crossing_ticks_second" : "crossing_ticks", second(curse) ? 260 : 200));
				MechanicUtil.sound(level, curse, JjkSounds.DOMAIN_OPEN, 2.5f, 0.6f);
				curse.triggerAnim("action", "cast");
			}

			return;
		}

		double pull = curse.parameter(second(curse) ? "pull_second" : "pull", second(curse) ? 0.075 : 0.06);
		for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(shore, shore).inflate(arena, 6, arena),
				e -> e.isAlive() && !(e instanceof CurseEntity) && !(e instanceof Player p && (p.isCreative() || p.isSpectator())))) {
			Vec3 toShore = shore.subtract(victim.position()).multiply(1, 0, 1);
			double distance = toShore.length();
			if (distance > 2.5) {
				Vec3 v = toShore.scale(pull / distance);
				victim.setDeltaMovement(victim.getDeltaMovement().add(v.x, 0, v.z));
				victim.needsSync = true;
			} else if (curse.tickCount % 20 == 0) {
				ferry(curse, level, victim, shore);
			}
		}

		// The current, drawn as mist streaming toward the shore.
		if (curse.tickCount % 2 == 0) {
			for (int i = 0; i < 6; i++) {
				double a = curse.getRandom().nextDouble() * Math.PI * 2;
				double r = 3 + curse.getRandom().nextDouble() * (arena - 3);
				Vec3 at = shore.add(Math.cos(a) * r, 0.2, Math.sin(a) * r);
				Vec3 flow = shore.subtract(at).normalize().scale(0.4);
				level.sendParticles(ParticleTypes.CLOUD, at.x, at.y, at.z, 0, flow.x, 0, flow.z, 1.0);
			}
		}

		if (MechanicUtil.countDown(curse, "crossing") <= 0) {
			curse.setMechA(0);
			MechanicUtil.set(curse, "next_crossing", (int) curse.parameter(second(curse) ? "interval_second" : "interval", second(curse) ? 400 : 600));
			MechanicUtil.sound(level, curse, JjkSounds.DOMAIN_CLOSE, 2f, 0.7f);
		}
	}

	/** Reaching the shore: hurt, withered, thrown back out. */
	private void ferry(CurseEntity curse, ServerLevel level, LivingEntity victim, Vec3 shore) {
		victim.hurtServer(level, MechanicUtil.attack(level, curse), (float) curse.parameter("ferry_damage", 8));
		victim.addEffect(new MobEffectInstance(MobEffects.WITHER, (int) curse.parameter("wither_ticks", 100), 1, false, true), curse);
		MechanicUtil.push(curse, level, victim, shore, 1.6);
		level.sendParticles(JjkParticles.of(JjkParticles.DISSOLVE, MIST, 1.2f), victim.getX(), victim.getY() + 1, victim.getZ(), 20, 0.4, 0.6, 0.4, 0.05);
		MechanicUtil.sound(level, curse, JjkSounds.CURSE_TOLL, 1.5f, 0.6f);
	}

	@Override
	public void onAttack(CurseEntity curse, ServerLevel level, LivingEntity target) {
		// The oar sweeps: everyone close to the first target takes part of the blow.
		for (LivingEntity other : MechanicUtil.victims(level, curse, 3.5)) {
			if (other != target) {
				other.hurtServer(level, MechanicUtil.attack(level, curse), (float) curse.parameter("sweep_damage", 5));
			}
		}
	}

	@Override
	public void onRemoved(CurseEntity curse) {
		MechanicUtil.clearBossBar(curse);
	}
}
