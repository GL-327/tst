package com.jjkcraft.curse.mechanic;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import com.jjkcraft.curse.CurseEntity;
import com.jjkcraft.effect.JjkEffects;
import com.jjkcraft.particle.JjkParticles;
import com.jjkcraft.sound.JjkSounds;

/**
 * Paper Tide Gravemind (Special Grade): a drift of grave charms that learns. Every kind of damage
 * it takes is remembered: after a handful of hits of one damage type its paper folds around that
 * kind of harm (resists it), and after more it all but ignores it. A rustling wheel of charms
 * marks each adaptation. Forgotten types fade back after a while unused. Change how you hurt it:
 * blades, arrows, techniques, fire, falls, explosions, anything it has not learned yet.
 * Its own attack is the paper tide: a cone of charms that binds and cuts (it gathers first).
 *
 * MECH_A: adaptations made so far. MECH_B: paper tide wind-up ticks left (-1 when none).
 */
public final class PaperTideMechanic implements CurseMechanic {
	private static final int PAPER_COLOR = 0xE8E0C8;

	/** What it has learned about one damage type. */
	private record Lesson(int hits, long lastHit) {
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Lesson> lessons(CurseEntity curse) {
		return (Map<String, Lesson>) curse.memory.computeIfAbsent("lessons", k -> new HashMap<String, Lesson>());
	}

	@Override
	public void onSpawn(CurseEntity curse, ServerLevel level) {
		curse.setPersistenceRequired();
		curse.setMechB(-1);
	}

	/** Damage multiplier for a type after {@code hits} hits: full, then 60%, then 25% (data can change these). */
	static double resistance(CurseEntity curse, int hits) {
		if (hits >= curse.parameter("second_fold_hits", 8)) {
			return curse.parameter("second_fold", 0.25);
		}

		return hits >= curse.parameter("first_fold_hits", 4) ? curse.parameter("first_fold", 0.6) : 1.0;
	}

	/** How well it resists a damage type right now (tests and the codex). */
	public static double resistanceTo(CurseEntity curse, String damageType) {
		Lesson lesson = lessons(curse).get(damageType);
		return lesson == null ? 1.0 : resistance(curse, lesson.hits());
	}

	@Override
	public float onHurt(CurseEntity curse, ServerLevel level, DamageSource source, float amount) {
		String type = source.typeHolder().unwrapKey().map(ResourceKey::identifier).map(Identifier::toString).orElse("unknown");
		long now = level.getGameTime();
		Map<String, Lesson> lessons = lessons(curse);
		Lesson before = lessons.get(type);
		int hits = before == null || now - before.lastHit() > curse.parameter("forget_ticks", 600) ? 1 : before.hits() + 1;
		double previous = before == null ? 1.0 : resistance(curse, before.hits());
		double next = resistance(curse, hits);
		lessons.put(type, new Lesson(hits, now));
		if (next < previous) {
			// The wheel turns: it has learned this harm.
			curse.setMechA(curse.mechA() + 1);
			level.sendParticles(JjkParticles.of(JjkParticles.DOMAIN_SHARD, PAPER_COLOR, 1.3f), curse.getX(), curse.getY() + curse.getBbHeight() * 0.9,
					curse.getZ(), 30, 0.8, 0.3, 0.8, 0.12);
			MechanicUtil.ring(level, JjkParticles.of(JjkParticles.CURSED_SPARK, PAPER_COLOR, 0.8f), curse.position().add(0, curse.getBbHeight() + 0.4, 0), 1.2, 16);
			MechanicUtil.sound(level, curse, JjkSounds.CHAIN, 1.5f, 1.4f);
			curse.memory.put("last_lesson", type);
		}

		// Its first lesson is applied from the hit that taught it: that hit already lands softer.
		return amount * (float) next;
	}

	@Override
	public void tick(CurseEntity curse, ServerLevel level) {
		String lesson = curse.memory.get("last_lesson") instanceof String s ? s.substring(s.indexOf(':') + 1) : null;
		MechanicUtil.bossBar(curse, level, curse.parameter("bar_range", 48), BossEvent.BossBarColor.WHITE, lesson == null ? null
				: Component.translatable("boss.jjkcraft.adapted", curse.getDisplayName(), Component.translatableWithFallback(
						"codex.jjkcraft.damage." + lesson, lesson.replace('_', ' '))));

		// Lessons fade when unused.
		if (curse.tickCount % 100 == 0) {
			long now = level.getGameTime();
			double forget = curse.parameter("forget_ticks", 600);
			lessons(curse).values().removeIf(l -> now - l.lastHit() > forget);
		}

		if (curse.tickCount % 5 == 0) {
			level.sendParticles(JjkParticles.of(JjkParticles.CURSED_SPARK, PAPER_COLOR, 0.5f), curse.getX(), curse.getY() + 1, curse.getZ(), 2,
					curse.getBbWidth() * 0.5, 1.0, curse.getBbWidth() * 0.5, 0.02);
		}

		int windup = curse.mechB();
		LivingEntity target = curse.getTarget();
		if (windup > 0) {
			curse.getNavigation().stop();
			if (target != null) {
				curse.brain().aimAt(target);
			}

			if (windup % 3 == 0) {
				MechanicUtil.ring(level, JjkParticles.of(JjkParticles.CURSED_SPARK, PAPER_COLOR, 0.7f), curse.position().add(0, 1.5, 0), 1.5 + windup * 0.05, 18);
			}

			curse.setMechB(windup - 1);
			if (windup == 1) {
				tide(curse, level);
			}

			return;
		}

		if (target != null && MechanicUtil.countDown(curse, "tide_cooldown") <= 0 && curse.distanceTo(target) <= curse.parameter("tide_range", 9)) {
			curse.setMechB((int) curse.parameter("tide_windup", 22));
			curse.triggerAnim("action", "cast");
			curse.brain().delayCasting(40);
			MechanicUtil.sound(level, curse, JjkSounds.ATTRACT, 1.4f, 1.3f);
		}
	}

	/** The paper tide: a cone of charms that cuts and binds. */
	private void tide(CurseEntity curse, ServerLevel level) {
		MechanicUtil.set(curse, "tide_cooldown", (int) curse.parameter("tide_interval", 120));
		double range = curse.parameter("tide_range", 9);
		Vec3 look = curse.getLookAngle().multiply(1, 0, 1).normalize();
		for (LivingEntity victim : MechanicUtil.victims(level, curse, range)) {
			Vec3 to = victim.position().subtract(curse.position()).multiply(1, 0, 1).normalize();
			if (to.dot(look) > 0.55) {
				victim.hurtServer(level, MechanicUtil.attack(level, curse), (float) curse.parameter("tide_damage", 7));
				victim.addEffect(new MobEffectInstance(JjkEffects.BOUND, (int) curse.parameter("tide_bind_ticks", 30), 0, false, true), curse);
				victim.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1, false, true), curse);
			}
		}

		Vec3 eye = curse.getEyePosition();
		for (int i = 0; i < 40; i++) {
			Vec3 dir = look.yRot((float) ((curse.getRandom().nextDouble() - 0.5) * 1.6));
			Vec3 at = eye.add(dir.scale(1 + curse.getRandom().nextDouble() * (range - 1)));
			level.sendParticles(JjkParticles.of(JjkParticles.CURSED_SPARK, PAPER_COLOR, 0.8f), at.x, at.y - 0.8, at.z, 1, 0.2, 0.3, 0.2, 0.05);
		}

		MechanicUtil.sound(level, curse, JjkSounds.SLASH, 1.4f, 1.2f);
	}

	@Override
	public boolean canAttack(CurseEntity curse) {
		return curse.mechB() <= 0;
	}

	@Override
	public void onRemoved(CurseEntity curse) {
		MechanicUtil.clearBossBar(curse);
	}
}
