package com.jjkcraft.curse.mechanic;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

import com.jjkcraft.curse.CurseEntity;

/**
 * The code that makes a curse unique. A curse's data names its mechanic; numbers the mechanic
 * reads come from the curse's {@code parameters}. All hooks run on the server.
 */
public interface CurseMechanic {
	/** Every server tick while alive and not emerging. */
	default void tick(CurseEntity curse, ServerLevel level) {
	}

	/** Adjusts incoming damage. Runs after data resistances. */
	default float onHurt(CurseEntity curse, ServerLevel level, DamageSource source, float amount) {
		return amount;
	}

	/** Multiplier on this curse's melee damage against {@code target}. */
	default double attackMultiplier(CurseEntity curse, LivingEntity target) {
		return 1.0;
	}

	/** After a melee hit lands. */
	default void onAttack(CurseEntity curse, ServerLevel level, LivingEntity target) {
	}

	/** False pauses the brain (stagger, disguise approach, waiting for a vow). */
	default boolean canAct(CurseEntity curse) {
		return true;
	}

	/** False keeps the brain from melee and techniques while still moving (disguised approach). */
	default boolean canAttack(CurseEntity curse) {
		return true;
	}

	default void onSpawn(CurseEntity curse, ServerLevel level) {
	}

	default void onDeath(CurseEntity curse, ServerLevel level, DamageSource source) {
	}

	/** When the entity leaves the world for any reason (death, despawn, unload). Clean up boss bars here. */
	default void onRemoved(CurseEntity curse) {
	}
}
