package com.jjkcraft.damage;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;

import com.jjkcraft.JjkCraft;

/** Damage type keys. The types themselves are data ({@code data/jjkcraft/damage_type}). */
public final class JjkDamageTypes {
	/** Ordinary technique damage; armor applies. */
	public static final ResourceKey<DamageType> TECHNIQUE = key("cursed_technique");
	/** Technique damage that ignores armor (tagged {@code minecraft:bypasses_armor}). */
	public static final ResourceKey<DamageType> TECHNIQUE_PIERCING = key("cursed_technique_piercing");
	/** A Black Flash strike. */
	public static final ResourceKey<DamageType> BLACK_FLASH = key("black_flash");
	/** A domain's sure-hit effect; ignores armor. */
	public static final ResourceKey<DamageType> SURE_HIT = key("sure_hit");
	/** Backlash (domain clash loss, overused speech). */
	public static final ResourceKey<DamageType> BACKLASH = key("backlash");
	/** Dabura Karaba's Light (the Modulo expansion). */
	public static final ResourceKey<DamageType> LIGHT = key("light");
	/** Dabura Karaba's darkness, Light's Cursed Technique Reversal; ignores armor. */
	public static final ResourceKey<DamageType> DARKNESS = key("darkness");

	private JjkDamageTypes() {
	}

	private static ResourceKey<DamageType> key(String path) {
		return ResourceKey.create(Registries.DAMAGE_TYPE, JjkCraft.id(path));
	}
}
