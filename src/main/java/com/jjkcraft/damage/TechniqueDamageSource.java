package com.jjkcraft.damage;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;

/**
 * Damage dealt by a cursed technique, carrying which technique dealt it, so that something can
 * react to the technique rather than to the damage type alone (Mahoraga's adaptation,
 * docs/MODULO.md 3.9). It behaves exactly like a plain source of the same type otherwise.
 */
public class TechniqueDamageSource extends DamageSource {
	private final Identifier technique;

	public TechniqueDamageSource(Holder<DamageType> type, Entity caster, Identifier technique) {
		super(type, caster);
		this.technique = technique;
	}

	public Identifier technique() {
		return technique;
	}
}
