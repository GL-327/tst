package com.jjkcraft.curse.mechanic;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import com.jjkcraft.JjkCraft;
import com.jjkcraft.core.domain.DomainMath;
import com.jjkcraft.curse.CurseEntity;
import com.jjkcraft.domain.DomainService;
import com.jjkcraft.effect.JjkEffects;
import com.jjkcraft.energy.CursedEnergy;
import com.jjkcraft.particle.JjkParticles;
import com.jjkcraft.sound.JjkSounds;
import com.jjkcraft.technique.CastFailure;

/**
 * Sovereign of Ten Mouths (Special Grade, final boss): ten mouths, one closing for every tenth of
 * its health. Three phases follow the mouths it has left:
 * <ul>
 * <li>Speech (10 to 7): it speaks cursed commands (its data techniques) and, on a beat, all ten
 * voices at once: everyone near is stunned and hurt; the mouths glow first.</li>
 * <li>Hunger (6 to 4): mouths open in the ground under its prey; rings mark them, then they bite
 * and hold. Step out of the rings.</li>
 * <li>Throat (3 to 1): it expands its own domain, the Throat of Ten Thousand Words. Counter it
 * with a Simple Domain or a domain of your own (clash), and keep away from the bites.</li>
 * </ul>
 *
 * MECH_A: mouths left (10 to 1). MECH_B: chorus or bite wind-up ticks left (-1 when none).
 */
public final class SovereignMechanic implements CurseMechanic {
	private static final int MOUTH_COLOR = 0xC0392B;

	@Override
	public void onSpawn(CurseEntity curse, ServerLevel level) {
		curse.setPersistenceRequired();
		curse.setMechA(10);
		curse.setMechB(-1);
	}

	public static int mouths(CurseEntity curse) {
		return Math.max(1, (int) Math.ceil(curse.getHealth() / curse.getMaxHealth() * 10));
	}

	@Override
	public void tick(CurseEntity curse, ServerLevel level) {
		int mouths = mouths(curse);
		if (mouths < curse.mechA()) {
			curse.setMechA(mouths);
			MechanicUtil.sound(level, curse, JjkSounds.CURSE_ROAR, 3f, 0.5f + mouths * 0.05f);
			level.sendParticles(JjkParticles.of(JjkParticles.DISSOLVE, MOUTH_COLOR, 1.4f), curse.getX(), curse.getY() + curse.getBbHeight() * 0.7,
					curse.getZ(), 30, 1, 0.8, 1, 0.08);
		}

		MechanicUtil.bossBar(curse, level, curse.parameter("bar_range", 64), BossEvent.BossBarColor.RED,
				Component.translatable("boss.jjkcraft.sovereign", curse.getDisplayName(), mouths));

		LivingEntity target = curse.getTarget();
		if (mouths <= 3 && target != null && DomainService.active(curse).isEmpty() && !(curse.memory.get("throat") instanceof Boolean)) {
			// The throat opens: the Sovereign's own domain. It has the CE for it.
			CursedEnergy.fill(curse);
			if (DomainService.expand(curse, JjkCraft.id("ten_mouth_throat"), DomainMath.Variant.FULL, true) == CastFailure.NONE) {
				curse.memory.put("throat", true);
			}
		}

		int windup = curse.mechB();
		if (windup > 0) {
			curse.getNavigation().stop();
			if (windup % 3 == 0) {
				telegraph(curse, level);
			}

			curse.setMechB(windup - 1);
			if (windup == 1) {
				if (MechanicUtil.get(curse, "action") == 1) {
					bite(curse, level);
				} else {
					chorus(curse, level);
				}
			}

			return;
		}

		if (target == null) {
			return;
		}

		if (mouths >= 7) {
			if (MechanicUtil.countDown(curse, "chorus_cooldown") <= 0 && curse.distanceTo(target) <= curse.parameter("chorus_radius", 12)) {
				MechanicUtil.set(curse, "action", 0);
				curse.setMechB((int) curse.parameter("chorus_windup", 24));
				curse.triggerAnim("action", "cast");
				curse.brain().delayCasting(40);
			}
		} else if (MechanicUtil.countDown(curse, "bite_cooldown") <= 0) {
			MechanicUtil.set(curse, "action", 1);
			markBites(curse, level);
			curse.setMechB((int) curse.parameter("bite_windup", 28));
			curse.triggerAnim("action", "cast");
		}
	}

	private void telegraph(CurseEntity curse, ServerLevel level) {
		if (MechanicUtil.get(curse, "action") == 1) {
			for (Vec3 mark : marks(curse)) {
				MechanicUtil.ring(level, JjkParticles.of(JjkParticles.CE_AURA, MOUTH_COLOR, 1.1f), mark, curse.parameter("bite_radius", 1.8), 14);
			}
		} else {
			MechanicUtil.particles(level, JjkParticles.of(JjkParticles.CURSED_SPARK, MOUTH_COLOR, 1f), curse.position().add(0, curse.getBbHeight() * 0.7, 0),
					10, 1.0, 0.1);
		}
	}

	/** All ten voices at once. */
	private void chorus(CurseEntity curse, ServerLevel level) {
		MechanicUtil.set(curse, "chorus_cooldown", (int) curse.parameter("chorus_interval", 160));
		for (LivingEntity victim : MechanicUtil.victims(level, curse, curse.parameter("chorus_radius", 12))) {
			victim.addEffect(new MobEffectInstance(JjkEffects.STUNNED, (int) curse.parameter("chorus_stun", 24), 0, false, true), curse);
			victim.hurtServer(level, MechanicUtil.attack(level, curse), (float) curse.parameter("chorus_damage", 5));
		}

		MechanicUtil.ring(level, JjkParticles.of(JjkParticles.DOMAIN_SHARD, MOUTH_COLOR, 1.2f), curse.position(), curse.parameter("chorus_radius", 12), 48);
		MechanicUtil.sound(level, curse, JjkSounds.SPEECH, 3f, 0.5f);
	}

	@SuppressWarnings("unchecked")
	private static List<Vec3> marks(CurseEntity curse) {
		return curse.memory.get("bite_marks") instanceof List<?> l ? (List<Vec3>) l : List.of();
	}

	/** Marks a mouth under each player near it (and one more near the main target). */
	private void markBites(CurseEntity curse, ServerLevel level) {
		List<Vec3> marks = new ArrayList<>();
		for (ServerPlayer player : level.players()) {
			if (!player.isSpectator() && !player.isCreative() && player.distanceTo(curse) <= curse.parameter("bite_range", 18)) {
				marks.add(player.position());
			}
		}

		LivingEntity target = curse.getTarget();
		if (target != null) {
			marks.add(target.position().add(curse.getRandom().nextGaussian() * 2, 0, curse.getRandom().nextGaussian() * 2));
		}

		curse.memory.put("bite_marks", marks);
	}

	private void bite(CurseEntity curse, ServerLevel level) {
		MechanicUtil.set(curse, "bite_cooldown", (int) curse.parameter("bite_interval", 100));
		double radius = curse.parameter("bite_radius", 1.8);
		for (Vec3 mark : marks(curse)) {
			for (LivingEntity victim : MechanicUtil.victims(level, curse, curse.parameter("bite_range", 18) + 4)) {
				if (victim.position().distanceTo(mark) <= radius) {
					victim.hurtServer(level, MechanicUtil.attack(level, curse), (float) curse.parameter("bite_damage", 10));
					victim.addEffect(new MobEffectInstance(JjkEffects.BOUND, (int) curse.parameter("bite_hold", 40), 0, false, true), curse);
				}
			}

			level.sendParticles(JjkParticles.of(JjkParticles.BLACK_LIGHTNING, MOUTH_COLOR, 1.2f), mark.x, mark.y + 0.5, mark.z, 14, 0.6, 0.4, 0.6, 0.1);
		}

		curse.memory.remove("bite_marks");
		MechanicUtil.sound(level, curse, JjkSounds.STRIKE, 2f, 0.4f);
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
