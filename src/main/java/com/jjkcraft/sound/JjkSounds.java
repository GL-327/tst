package com.jjkcraft.sound;

import java.util.Optional;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

import com.jjkcraft.JjkCraft;

/**
 * Sound events. Every sound file is original and made by {@code tools/gen_sounds.py}; the events
 * are listed in {@code assets/jjkcraft/sounds.json}.
 */
public final class JjkSounds {
	public static final SoundEvent OVEREXERT = register("energy.overexert");
	public static final SoundEvent PROMOTE = register("grade.promote");
	public static final SoundEvent UI_CLICK = register("ui.click");
	public static final SoundEvent UI_OPEN = register("ui.open");
	public static final SoundEvent CAST = register("technique.cast");
	public static final SoundEvent STRIKE = register("technique.strike");
	public static final SoundEvent BLACK_FLASH = register("technique.black_flash");
	public static final SoundEvent BOLT_FIRE = register("technique.bolt_fire");
	public static final SoundEvent BOLT_HIT = register("technique.bolt_hit");
	public static final SoundEvent SLASH = register("technique.slash");
	public static final SoundEvent ATTRACT = register("technique.attract");
	public static final SoundEvent REPEL = register("technique.repel");
	public static final SoundEvent MERGE = register("technique.merge");
	public static final SoundEvent SPEECH = register("technique.speech");
	public static final SoundEvent HEAL = register("technique.heal");
	public static final SoundEvent CHAIN = register("technique.chain");
	public static final SoundEvent BARRIER = register("technique.barrier");
	public static final SoundEvent SUMMON = register("technique.summon");
	public static final SoundEvent DOMAIN_OPEN = register("domain.open");
	public static final SoundEvent DOMAIN_AMBIENT = register("domain.ambient");
	public static final SoundEvent DOMAIN_CLOSE = register("domain.close");
	public static final SoundEvent DOMAIN_CLASH = register("domain.clash");
	public static final SoundEvent CURSE_SPAWN = register("curse.spawn");
	public static final SoundEvent CURSE_HURT = register("curse.hurt");
	public static final SoundEvent CURSE_DEATH = register("curse.death");
	public static final SoundEvent CURSE_AMBIENT = register("curse.ambient");
	public static final SoundEvent CURSE_TOLL = register("curse.toll");
	public static final SoundEvent CURSE_ROAR = register("curse.roar");

	private JjkSounds() {
	}

	public static void init() {
		// Loads the class so the static fields register.
	}

	/** A registered sound event by id, for data-driven sound keys. */
	public static Optional<SoundEvent> byId(Identifier id) {
		return BuiltInRegistries.SOUND_EVENT.getOptional(id);
	}

	static SoundEvent register(String path) {
		SoundEvent event = SoundEvent.createVariableRangeEvent(JjkCraft.id(path));
		return Registry.register(BuiltInRegistries.SOUND_EVENT, JjkCraft.id(path), event);
	}
}
