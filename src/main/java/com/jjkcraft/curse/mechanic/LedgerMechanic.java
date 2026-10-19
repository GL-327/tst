package com.jjkcraft.curse.mechanic;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;

import com.jjkcraft.curse.CurseEntity;
import com.jjkcraft.item.JjkItems;
import com.jjkcraft.particle.JjkParticles;

/**
 * Ledger Crawler (Grade 4): keeps a tally of every death near it. Each mark adds to its damage, so
 * fights near it grow worse the longer they last. On death it drops one tally token per mark.
 *
 * MECH_A: current tally.
 */
public final class LedgerMechanic implements CurseMechanic {
	static void init() {
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity instanceof CurseEntity || !(entity.level() instanceof ServerLevel level)) {
				return;
			}

			for (CurseEntity curse : level.getEntitiesOfClass(CurseEntity.class, entity.getBoundingBox().inflate(16),
					c -> c.mechanic() instanceof LedgerMechanic && c.isAlive())) {
				int max = (int) curse.parameter("max_tally", 8);
				if (curse.mechA() < max) {
					curse.setMechA(curse.mechA() + 1);
					MechanicUtil.particles(level, JjkParticles.of(JjkParticles.CURSED_SPARK, 0xE6DCBE, 0.8f), curse.getEyePosition(), 6, 0.3, 0.05);
				}
			}
		});
	}

	@Override
	public double attackMultiplier(CurseEntity curse, LivingEntity target) {
		return 1 + curse.parameter("damage_per_mark", 0.15) * curse.mechA();
	}

	@Override
	public void onDeath(CurseEntity curse, ServerLevel level, DamageSource source) {
		int marks = curse.mechA();
		if (marks > 0) {
			level.addFreshEntity(new ItemEntity(level, curse.getX(), curse.getY() + 0.5, curse.getZ(), new ItemStack(JjkItems.TALLY_TOKEN, marks)));
		}
	}
}
