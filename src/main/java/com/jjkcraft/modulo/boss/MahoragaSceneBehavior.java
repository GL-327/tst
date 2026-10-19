package com.jjkcraft.modulo.boss;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.Vec3;

import com.jjkcraft.damage.JjkDamageTypes;
import com.jjkcraft.modulo.entity.MahoragaEntity;
import com.jjkcraft.modulo.entity.ModuloBossEntity;
import com.jjkcraft.modulo.entity.ModuloEntities;

/**
 * Mission 8's duel (chapters 15 to 19): Dabura against the Mahoraga Yuka summoned. This boss is
 * Dabura, untouchable here; he fires Light at Mahoraga, which adapts with every hit. When Mahoraga has
 * fully adapted to Light (as in chapter 18) the scene ends and the stage is won, provided the
 * fighters held off the fleeing cursed spirits meanwhile.
 * Params: {@code beam_interval} (50), {@code beam_damage} (24), {@code distance} (10).
 */
final class MahoragaSceneBehavior implements BossBehavior {
	private static final String LIGHT = JjkDamageTypes.LIGHT.identifier().toString();

	private @Nullable MahoragaEntity mahoraga;
	private int cooldown = 60;

	@Override
	public void onSpawn(ModuloBossEntity boss, ServerLevel level) {
		MahoragaEntity summoned = ModuloEntities.MAHORAGA.create(level, EntitySpawnReason.MOB_SUMMONED);
		if (summoned == null) {
			return;
		}

		BlockPos at = boss.blockPosition().offset((int) boss.param("distance", 10), 0, 0);
		summoned.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 90, 0);
		summoned.setup(level);
		summoned.setTarget(boss);
		level.addFreshEntity(summoned);
		level.sendParticles(ParticleTypes.SQUID_INK, at.getX(), at.getY() + 1, at.getZ(), 120, 1, 2, 1, 0.1);
		level.playSound(null, at, SoundEvents.BELL_BLOCK, SoundSource.HOSTILE, 2f, 0.5f);
		mahoraga = summoned;
	}

	@Override
	public void tick(ModuloBossEntity boss, ServerLevel level) {
		boss.getNavigation().stop();
		if (mahoraga == null || !mahoraga.isAlive()) {
			return;
		}

		mahoraga.setTarget(boss);
		boss.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, mahoraga.getEyePosition());
		if (mahoraga.fullyAdapted(LIGHT)) {
			mahoraga.discard();
			boss.yieldFight(Component.translatable("message.jjkcraft.modulo.mahoraga.adapted"));
			return;
		}

		if (--cooldown <= 0) {
			cooldown = (int) boss.param("beam_interval", 50);
			Vec3 from = boss.getEyePosition();
			Vec3 to = mahoraga.getBoundingBox().getCenter();
			BossBehavior.line(level, ParticleTypes.END_ROD, from, to, 0.4);
			level.playSound(null, boss.getX(), boss.getY(), boss.getZ(), SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 1.6f, 2f);
			mahoraga.hurtServer(level, level.damageSources().source(JjkDamageTypes.LIGHT, boss), (float) boss.param("beam_damage", 24));
		}
	}

	@Override
	public void onRemoved(ModuloBossEntity boss, ServerLevel level) {
		if (mahoraga != null && mahoraga.isAlive()) {
			mahoraga.discard();
		}
	}
}
