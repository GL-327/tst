package com.jjkcraft.modulo;

import net.minecraft.server.level.ServerPlayer;

import com.jjkcraft.damage.DamageModifiers;
import com.jjkcraft.modulo.entity.MahoragaEntity;
import com.jjkcraft.technique.CastFailure;
import com.jjkcraft.technique.TechniqueService;

/**
 * Where the Modulo expansion meets the technique and damage systems: techniques with a
 * {@code mul_cost} also pay Mul energy (docs/MODULO.md 3.4), and Mahoraga adapts to what hurts it.
 */
public final class ModuloTechniques {
	private ModuloTechniques() {
	}

	public static void init() {
		TechniqueService.addCastBlocker((caster, id, definition) -> {
			double cost = definition.cast().mulCost();
			if (cost <= 0 || !(caster instanceof ServerPlayer player)) {
				return null;
			}

			if (!Modulo.enabled()) {
				return CastFailure.DISABLED;
			}

			return MulEnergy.get(player) + 1e-9 >= cost || player.isCreative() ? null : CastFailure.NOT_ENOUGH_MUL;
		});
		TechniqueService.addCastListener((caster, id, definition) -> {
			if (definition.cast().mulCost() > 0 && caster instanceof ServerPlayer player) {
				MulEnergy.trySpend(player, definition.cast().mulCost());
			}
		});
		DamageModifiers.add((level, target, source, amount) -> target instanceof MahoragaEntity mahoraga ? mahoraga.adapt(level, source, amount) : amount);
	}
}
