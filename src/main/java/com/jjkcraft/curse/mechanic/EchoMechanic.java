package com.jjkcraft.curse.mechanic;

import java.util.ArrayDeque;
import java.util.Deque;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import com.jjkcraft.curse.CurseEntity;
import com.jjkcraft.particle.JjkParticles;
import com.jjkcraft.sorcerer.PlayerActions;
import com.jjkcraft.sorcerer.Progression;
import com.jjkcraft.sound.JjkSounds;
import com.jjkcraft.technique.CastFailure;
import com.jjkcraft.technique.TechniqueCategory;
import com.jjkcraft.technique.TechniqueService;
import com.jjkcraft.technique.Techniques;

/**
 * Echo Mimic (Grade 3): it copies its target's last five actions (techniques cast, swings, jumps)
 * and plays each one back three seconds later, in order, with its own body and CE. Whatever you
 * did, it does to you. Too-big techniques (domains, ultimates, anything above its grade) come back
 * as a plain lunge. Slow down, vary, or bait it with something harmless.
 *
 * MECH_A: how many actions it holds (0 to 5).
 */
public final class EchoMechanic implements CurseMechanic {
	/** One recorded action and the game time it happened. */
	private record Action(PlayerActions.Kind kind, Identifier technique, long at) {
	}

	private static final String KEY = "echo_queue";

	@SuppressWarnings("unchecked")
	private static Deque<Action> queue(CurseEntity curse) {
		return (Deque<Action>) curse.memory.computeIfAbsent(KEY, k -> new ArrayDeque<Action>());
	}

	static void init() {
		PlayerActions.addListener((player, kind, technique, target) -> {
			ServerLevel level = player.level();
			for (CurseEntity curse : level.getEntitiesOfClass(CurseEntity.class, player.getBoundingBox().inflate(20),
					c -> c.mechanic() instanceof EchoMechanic && c.isAlive() && !c.isEmerging())) {
				// It listens to whoever it hunts, or the nearest player while it hunts no one.
				if (curse.getTarget() != null && curse.getTarget() != player) {
					continue;
				}

				record(curse, level, player, kind, technique);
			}
		});
	}

	private static void record(CurseEntity curse, ServerLevel level, ServerPlayer player, PlayerActions.Kind kind, Identifier technique) {
		Deque<Action> queue = queue(curse);
		int max = (int) curse.parameter("echo_length", 5);
		queue.addLast(new Action(kind, technique, level.getGameTime()));
		while (queue.size() > max) {
			queue.removeFirst();
		}

		curse.setMechA(queue.size());
		if (curse.getTarget() == null && !player.isCreative() && !player.isSpectator()) {
			curse.setTarget(player);
		}

		MechanicUtil.particles(level, JjkParticles.of(JjkParticles.CE_AURA, 0x9FD8E8, 0.6f), curse.getEyePosition(), 3, 0.25, 0.02);
	}

	@Override
	public void tick(CurseEntity curse, ServerLevel level) {
		Deque<Action> queue = queue(curse);
		if (queue.isEmpty()) {
			return;
		}

		int delay = (int) curse.parameter("echo_delay", 60);
		Action next = queue.peekFirst();
		if (level.getGameTime() - next.at() < delay) {
			return;
		}

		queue.removeFirst();
		curse.setMechA(queue.size());
		LivingEntity target = curse.getTarget();
		switch (next.kind()) {
			case JUMP -> {
				if (curse.onGround()) {
					curse.setDeltaMovement(curse.getDeltaMovement().add(0, 0.55, 0));
					curse.needsSync = true;
				}
			}
			case MELEE -> lunge(curse, level, target);
			case CAST -> {
				if (target == null || !castable(curse, level, next.technique())) {
					lunge(curse, level, target);
					break;
				}

				curse.brain().aimAt(target);
				if (TechniqueService.cast(curse, next.technique(), -1) == CastFailure.NONE) {
					curse.triggerAnim("action", "cast");
				}
			}
		}

		MechanicUtil.sound(level, curse, JjkSounds.SPEECH, 0.6f, 1.8f);
	}

	private static boolean castable(CurseEntity curse, ServerLevel level, Identifier id) {
		return Techniques.get(level.registryAccess(), id).filter(def -> def.category() != TechniqueCategory.DOMAIN
				&& def.category() != TechniqueCategory.ULTIMATE && def.category() != TechniqueCategory.PASSIVE
				&& Techniques.gradeRank(level.registryAccess(), def) <= Progression.gradeRank(level.registryAccess(), curse)).isPresent();
	}

	/** The copied swing: a short dash at the target, then a blow if it lands in reach. */
	private static void lunge(CurseEntity curse, ServerLevel level, LivingEntity target) {
		if (target == null) {
			curse.triggerAnim("action", "attack");
			return;
		}

		double distance = curse.distanceTo(target);
		if (distance > 2.5 && distance < 8) {
			Vec3 to = target.position().subtract(curse.position()).multiply(1, 0, 1).normalize().scale(0.9);
			curse.setDeltaMovement(to.x, 0.2, to.z);
			curse.needsSync = true;
		}

		if (distance <= 3.5) {
			curse.doHurtTarget(level, target);
		} else {
			curse.triggerAnim("action", "attack");
		}
	}

	/** The queued actions, for tests and the HUD. */
	public static int held(CurseEntity curse) {
		return queue(curse).size();
	}
}
