package com.jjkcraft.particle;

import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;

import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;

import com.jjkcraft.JjkCraft;

/**
 * Custom particle types from the VFX spec. All are tinted at spawn time; textures are original
 * and made by {@code tools/gen_textures.py}.
 */
public final class JjkParticles {
	/** Small bright spark: cast flashes and impacts. */
	public static final ParticleType<ColoredParticleOptions> CURSED_SPARK = colored("cursed_spark");
	/** Soft rising wisp: CE aura around a charging or sensed sorcerer. */
	public static final ParticleType<ColoredParticleOptions> CE_AURA = colored("ce_aura");
	/** Gentle pulsing orb: healing and Reverse Technique. */
	public static final ParticleType<ColoredParticleOptions> REVERSE_GLOW = colored("reverse_glow");
	/** Jagged black-and-red arc: Black Flash. */
	public static final ParticleType<ColoredParticleOptions> BLACK_LIGHTNING = colored("black_lightning");
	/** Glassy fragment: domain boundaries and clashes. */
	public static final ParticleType<ColoredParticleOptions> DOMAIN_SHARD = colored("domain_shard");
	/** Drifting ash flake: curse dissolution. */
	public static final ParticleType<ColoredParticleOptions> DISSOLVE = colored("dissolve");

	private JjkParticles() {
	}

	public static void init() {
		// Loads the class so the static fields register.
	}

	public static ColoredParticleOptions of(ParticleType<ColoredParticleOptions> type, int color, float scale) {
		return new ColoredParticleOptions(type, color & 0xFFFFFF, scale);
	}

	private static ParticleType<ColoredParticleOptions> colored(String name) {
		ParticleType<ColoredParticleOptions> type = FabricParticleTypes.complex(false, ColoredParticleOptions::codec, ColoredParticleOptions::streamCodec);
		return Registry.register(BuiltInRegistries.PARTICLE_TYPE, JjkCraft.id(name), type);
	}
}
