package com.jjkcraft.modulo.boss;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;

import com.jjkcraft.modulo.entity.ModuloBossEntity;

/** A boss's helper with no tricks of its own: it chases and strikes. Masaki Takeda's skin-sewn guardian is one. */
final class MinionBehavior implements BossBehavior {
	@Override
	public void tick(ModuloBossEntity boss, ServerLevel level) {
	}

	@Override
	public float onHurt(ModuloBossEntity boss, ServerLevel level, DamageSource source, float amount) {
		MasakiTakedaBehavior.guardianHit(boss, level);
		return amount;
	}
}
