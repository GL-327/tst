package com.jjkcraft.curse.mechanic;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import com.jjkcraft.curse.CurseEntity;
import com.jjkcraft.particle.JjkParticles;
import com.jjkcraft.sound.JjkSounds;

/**
 * Redacted Saint (Grade 1): it wears your face. Disguised, it takes the skin and name of the nearest
 * player and walks among people looking exactly like them; it only follows and can't attack.
 * Sorcerers who sense cursed energy see the curse outline through the disguise. When it gets
 * close, or is struck, the face burns away and its first blow lands twice as hard. Its hits black
 * out the victim's sight for a moment. With no target for a while it puts a face back on.
 *
 * MECH_A: 1 while disguised (and {@link CurseEntity#DISGUISED}, which picks the disguise model).
 * MECH_B: entity id of the player whose face it wears (-1: none; the client then shows a hooded
 * pilgrim).
 */
public final class SaintMechanic implements CurseMechanic {
	private static final int REVEAL_COLOR = 0xF0E6D2;

	@Override
	public void onSpawn(CurseEntity curse, ServerLevel level) {
		disguise(curse, true);
	}

	private static void disguise(CurseEntity curse, boolean on) {
		curse.setMechA(on ? 1 : 0);
		curse.setDisguised(on);
		if (on) {
			copyFace(curse);
		} else {
			curse.setMechB(-1);
			curse.setCustomName(null);
		}
	}

	/** Takes the face and name of the nearest player (or keeps the pilgrim when no one is near). */
	private static void copyFace(CurseEntity curse) {
		Player model = curse.level().getNearestPlayer(curse, 32);
		if (model == null || model.isSpectator()) {
			curse.setMechB(-1);
			curse.setCustomName(null);
			return;
		}

		curse.setMechB(model.getId());
		curse.setCustomName(model.getName().copy());
		curse.setCustomNameVisible(false);
	}

	@Override
	public void tick(CurseEntity curse, ServerLevel level) {
		LivingEntity target = curse.getTarget();
		if (curse.mechA() == 1) {
			// The face it wears left: find another.
			if (curse.tickCount % 40 == 0 && !(level.getEntity(curse.mechB()) instanceof Player)) {
				copyFace(curse);
			}

			if (target == null) {
				return;
			}

			int patience = MechanicUtil.get(curse, "patience") + 1;
			MechanicUtil.set(curse, "patience", patience);
			if (curse.distanceTo(target) <= curse.parameter("reveal_distance", 2.6) || patience >= curse.parameter("max_patience", 400)) {
				reveal(curse, level);
			}

			return;
		}

		if (target == null) {
			int idle = MechanicUtil.get(curse, "idle") + 1;
			MechanicUtil.set(curse, "idle", idle);
			if (idle >= curse.parameter("redisguise_ticks", 600)) {
				disguise(curse, true);
				MechanicUtil.set(curse, "patience", 0);
				MechanicUtil.particles(level, JjkParticles.of(JjkParticles.CE_AURA, REVEAL_COLOR, 0.8f), curse.position().add(0, 1, 0), 8, 0.3, 0.02);
			}
		} else {
			MechanicUtil.set(curse, "idle", 0);
		}
	}

	private void reveal(CurseEntity curse, ServerLevel level) {
		disguise(curse, false);
		MechanicUtil.set(curse, "first_strike", 1);
		MechanicUtil.set(curse, "idle", 0);
		curse.triggerAnim("action", "cast");
		MechanicUtil.sound(level, curse, JjkSounds.CURSE_SPAWN, 1.4f, 1.3f);
		level.sendParticles(JjkParticles.of(JjkParticles.DISSOLVE, REVEAL_COLOR, 1.3f), curse.getX(), curse.getY() + 1, curse.getZ(), 40, 0.35, 0.8, 0.35, 0.06);
	}

	@Override
	public float onHurt(CurseEntity curse, ServerLevel level, DamageSource source, float amount) {
		if (curse.mechA() == 1) {
			reveal(curse, level);
		}

		return amount;
	}

	@Override
	public boolean canAttack(CurseEntity curse) {
		return curse.mechA() == 0;
	}

	@Override
	public double attackMultiplier(CurseEntity curse, LivingEntity target) {
		return MechanicUtil.get(curse, "first_strike") == 1 ? curse.parameter("first_strike_multiplier", 2) : 1;
	}

	@Override
	public void onAttack(CurseEntity curse, ServerLevel level, LivingEntity target) {
		MechanicUtil.set(curse, "first_strike", 0);
		target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, (int) curse.parameter("blind_ticks", 40), 0, false, true), curse);
	}
}
