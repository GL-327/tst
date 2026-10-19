package com.jjkcraft.damage;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * One place where incoming damage is adjusted before vanilla handles it (barrier shields, simple
 * domains, vows, Heavenly Restriction, curse resistances). Called from {@code LivingEntityMixin}
 * at the start of {@code hurtServer}. Modifiers run in registration order.
 */
public final class DamageModifiers {
	@FunctionalInterface
	public interface Modifier {
		/** Returns the new amount. Return the input unchanged if this modifier does not apply. */
		float modify(ServerLevel level, LivingEntity target, DamageSource source, float amount);
	}

	private static final List<Modifier> MODIFIERS = new CopyOnWriteArrayList<>();

	private DamageModifiers() {
	}

	public static void add(Modifier modifier) {
		MODIFIERS.add(modifier);
	}

	public static float apply(ServerLevel level, LivingEntity target, DamageSource source, float amount) {
		float result = amount;
		for (Modifier modifier : MODIFIERS) {
			result = modifier.modify(level, target, source, result);
			if (result <= 0) {
				return 0;
			}
		}

		return result;
	}
}
