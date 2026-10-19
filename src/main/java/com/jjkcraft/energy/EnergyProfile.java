package com.jjkcraft.energy;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.codec.RegistryCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;

/**
 * Data entry ({@code data/<ns>/jjkcraft/energy_profile/*.json}) that makes vanilla or modded mobs
 * cursed-capable. Curse entities from this mod get their CE from curse data instead.
 *
 * @param entities        entity types (a list or a #tag) this profile applies to
 * @param grade           the tier the mob counts as, for sensing and grade-based effects
 * @param capacity        max CE
 * @param regenMultiplier multiplier on the configured regen rate
 */
public record EnergyProfile(HolderSet<EntityType<?>> entities, Identifier grade, double capacity, double regenMultiplier) {
	public static final Codec<EnergyProfile> CODEC = RecordCodecBuilder.create(i -> i.group(
			RegistryCodecs.holderSet(Registries.ENTITY_TYPE).fieldOf("entities").forGetter(EnergyProfile::entities),
			Identifier.CODEC.fieldOf("grade").forGetter(EnergyProfile::grade),
			Codec.doubleRange(0, 1_000_000).fieldOf("capacity").forGetter(EnergyProfile::capacity),
			Codec.doubleRange(0, 100).optionalFieldOf("regen_multiplier", 1.0).forGetter(EnergyProfile::regenMultiplier)
	).apply(i, EnergyProfile::new));
}
