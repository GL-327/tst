package com.jjkcraft.domain.behavior;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;

import com.jjkcraft.JjkCraft;

/**
 * The static registry of domain behaviors ({@code jjkcraft:domain_behavior}). A domain's
 * {@code behaviors} list names these with {@code "type"}; datapacks combine them freely.
 */
public final class DomainBehaviors {
	public static final ResourceKey<Registry<DomainBehaviorType<?>>> KEY = ResourceKey.createRegistryKey(JjkCraft.id("domain_behavior"));
	public static final Registry<DomainBehaviorType<?>> REGISTRY = FabricRegistryBuilder.create(KEY).buildAndRegister();

	public static final Codec<DomainBehavior> CODEC = REGISTRY.byNameCodec()
			.dispatch("type", DomainBehavior::type, DomainBehaviorType::codec);

	public static final DomainBehaviorType<SureHitDamageBehavior> SURE_HIT_DAMAGE = register("sure_hit_damage", SureHitDamageBehavior.CODEC);
	public static final DomainBehaviorType<ApplyEffectBehavior> APPLY_EFFECT = register("apply_effect", ApplyEffectBehavior.CODEC);
	public static final DomainBehaviorType<SuppressRegenBehavior> SUPPRESS_REGEN = register("suppress_regen", SuppressRegenBehavior.CODEC);
	public static final DomainBehaviorType<CasterAttributeBehavior> CASTER_ATTRIBUTE = register("caster_attribute", CasterAttributeBehavior.CODEC);
	public static final DomainBehaviorType<OverloadBehavior> OVERLOAD = register("overload", OverloadBehavior.CODEC);
	public static final DomainBehaviorType<SummonHuntersBehavior> SUMMON_HUNTERS = register("summon_hunters", SummonHuntersBehavior.CODEC);
	public static final DomainBehaviorType<GambleBehavior> GAMBLE = register("gamble", GambleBehavior.CODEC);
	public static final DomainBehaviorType<ArsenalBehavior> ARSENAL = register("arsenal", ArsenalBehavior.CODEC);
	public static final DomainBehaviorType<RepetitionBehavior> REPETITION = register("repetition", RepetitionBehavior.CODEC);

	private DomainBehaviors() {
	}

	public static void init() {
		// Loads the class so the static fields register.
	}

	public static <B extends DomainBehavior> DomainBehaviorType<B> register(String name, MapCodec<B> codec) {
		return Registry.register(REGISTRY, JjkCraft.id(name), new DomainBehaviorType<>(codec));
	}
}
