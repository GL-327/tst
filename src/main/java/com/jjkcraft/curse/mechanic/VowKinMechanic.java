package com.jjkcraft.curse.mechanic;

import java.util.Comparator;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import com.jjkcraft.curse.CurseEntity;
import com.jjkcraft.particle.JjkParticles;
import com.jjkcraft.sound.JjkSounds;
import com.jjkcraft.vow.Vows;

/**
 * Vow Kin (Grade 1): a curse born from broken promises, drawn to the people who keep them. It
 * fights only players who hold an active Binding Vow, and hits harder the heavier the vow. Everyone
 * else it ignores, even when struck. When its quarry's vow ends by breaking or renouncing, it flees
 * and fades. The way out of a losing fight is to give up the vow, and pay for it.
 *
 * MECH_A: 1 while it hunts a vow-holder, 2 while fleeing.
 */
public final class VowKinMechanic implements CurseMechanic {
	static void init() {
		Vows.addEndListener((player, vow, ending) -> {
			if (ending == Vows.Ending.FULFILLED) {
				return;
			}

			for (CurseEntity curse : player.level().getEntitiesOfClass(CurseEntity.class, player.getBoundingBox().inflate(48),
					c -> c.mechanic() instanceof VowKinMechanic && c.isAlive() && c.getTarget() == player)) {
				flee(curse, player);
			}
		});
	}

	private static void flee(CurseEntity curse, ServerPlayer from) {
		curse.setMechA(2);
		curse.setTarget(null);
		curse.memory.put("flee_from", from.position());
		MechanicUtil.set(curse, "flee_ticks", (int) curse.parameter("flee_ticks", 100));
		from.sendSystemMessage(Component.translatable("message.jjkcraft.vow_kin_flees"), true);
		MechanicUtil.sound((ServerLevel) curse.level(), curse, JjkSounds.CURSE_HURT, 1.2f, 1.6f);
	}

	@Override
	public void tick(CurseEntity curse, ServerLevel level) {
		if (curse.mechA() == 2) {
			Vec3 from = (Vec3) curse.memory.get("flee_from");
			if (from != null && (curse.getNavigation().isDone() || curse.tickCount % 10 == 0)) {
				Vec3 away = curse.position().subtract(from).multiply(1, 0, 1).normalize().scale(12).add(curse.position());
				curse.getNavigation().moveTo(away.x, away.y, away.z, 1.4);
			}

			if (MechanicUtil.countDown(curse, "flee_ticks") <= 0) {
				level.sendParticles(JjkParticles.of(JjkParticles.DISSOLVE, 0xB08D57, 1.2f), curse.getX(), curse.getY() + 1, curse.getZ(), 30, 0.4, 0.8, 0.4, 0.05);
				curse.discard();
			}

			return;
		}

		// Only vow-holders are prey.
		LivingEntity target = curse.getTarget();
		if (!(target instanceof ServerPlayer player && Vows.active(player))) {
			ServerPlayer next = level.players().stream()
					.filter(p -> !p.isSpectator() && !p.isCreative() && Vows.active(p) && p.distanceTo(curse) <= curse.parameter("hunt_range", 24))
					.min(Comparator.comparingDouble(p -> p.distanceToSqr(curse)))
					.orElse(null);
			curse.setTarget(next);
		}

		curse.setMechA(curse.getTarget() != null ? 1 : 0);
		if (curse.mechA() == 1 && curse.tickCount % 6 == 0) {
			MechanicUtil.particles(level, JjkParticles.of(JjkParticles.CURSED_SPARK, 0xB08D57, 0.6f), curse.getEyePosition(), 2, 0.2, 0.02);
		}
	}

	@Override
	public boolean canAttack(CurseEntity curse) {
		return curse.mechA() == 1 && curse.getTarget() instanceof ServerPlayer player && Vows.active(player);
	}

	@Override
	public boolean canAct(CurseEntity curse) {
		return curse.mechA() != 2;
	}

	@Override
	public double attackMultiplier(CurseEntity curse, LivingEntity target) {
		if (target instanceof ServerPlayer player && Vows.active(player)) {
			return 1 + curse.parameter("damage_per_point", 0.08) * Vows.state(player).points();
		}

		return 0;
	}
}
