package com.jjkcraft.technique;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;

import com.jjkcraft.JjkCraft;
import com.jjkcraft.technique.behavior.ArcSlashBehavior;
import com.jjkcraft.technique.behavior.BarrierBehavior;
import com.jjkcraft.technique.behavior.BindBehavior;
import com.jjkcraft.technique.behavior.CursedSpeechBehavior;
import com.jjkcraft.technique.behavior.FieldBehavior;
import com.jjkcraft.technique.behavior.HealBehavior;
import com.jjkcraft.technique.behavior.LineCutBehavior;
import com.jjkcraft.technique.behavior.ProjectileBehavior;
import com.jjkcraft.technique.behavior.InfinityBehavior;
import com.jjkcraft.technique.behavior.SummonBehavior;
import com.jjkcraft.technique.behavior.StrawDollBehavior;
import com.jjkcraft.technique.behavior.IdleTransfigurationBehavior;
import com.jjkcraft.technique.behavior.ProjectionSorceryBehavior;
import com.jjkcraft.technique.behavior.BarrageBehavior;
import com.jjkcraft.technique.behavior.SingularityBehavior;
import com.jjkcraft.technique.behavior.VowStrikeBehavior;
import com.jjkcraft.technique.behavior.DashBehavior;
import com.jjkcraft.technique.behavior.ShockwaveBehavior;
import com.jjkcraft.technique.behavior.ThornsAuraBehavior;
import com.jjkcraft.technique.behavior.MendOtherBehavior;
import com.jjkcraft.technique.behavior.StrikeBehavior;

/**
 * The static registry of technique behaviors ({@code jjkcraft:technique_behavior}). A technique's
 * {@code behavior} object names one of these with {@code "type"}; the other fields are validated by
 * that behavior's codec. Datapacks can combine these freely but cannot add new ones.
 */
public final class TechniqueBehaviors {
	public static final ResourceKey<Registry<TechniqueBehaviorType<?>>> KEY = ResourceKey.createRegistryKey(JjkCraft.id("technique_behavior"));
	public static final Registry<TechniqueBehaviorType<?>> REGISTRY = FabricRegistryBuilder.create(KEY).buildAndRegister();

	public static final Codec<TechniqueBehavior> CODEC = REGISTRY.byNameCodec()
			.dispatch("type", TechniqueBehavior::type, TechniqueBehaviorType::codec);

	public static final TechniqueBehaviorType<StrikeBehavior> STRIKE = register("strike", StrikeBehavior.CODEC);
	public static final TechniqueBehaviorType<ProjectileBehavior> PROJECTILE = register("projectile", ProjectileBehavior.CODEC);
	public static final TechniqueBehaviorType<ArcSlashBehavior> ARC_SLASH = register("arc_slash", ArcSlashBehavior.CODEC);
	public static final TechniqueBehaviorType<LineCutBehavior> LINE_CUT = register("line_cut", LineCutBehavior.CODEC);
	public static final TechniqueBehaviorType<FieldBehavior> FIELD = register("field", FieldBehavior.CODEC);
	public static final TechniqueBehaviorType<CursedSpeechBehavior> CURSED_SPEECH = register("cursed_speech", CursedSpeechBehavior.CODEC);
	public static final TechniqueBehaviorType<HealBehavior> HEAL = register("heal", HealBehavior.CODEC);
	public static final TechniqueBehaviorType<BindBehavior> BIND = register("bind", BindBehavior.CODEC);
	public static final TechniqueBehaviorType<BarrierBehavior> BARRIER = register("barrier", BarrierBehavior.CODEC);
	public static final TechniqueBehaviorType<InfinityBehavior> INFINITY = register("infinity", InfinityBehavior.CODEC);
	public static final TechniqueBehaviorType<SummonBehavior> SUMMON = register("summon", SummonBehavior.CODEC);
	public static final TechniqueBehaviorType<StrawDollBehavior> STRAW_DOLL = register("straw_doll", StrawDollBehavior.CODEC);
	public static final TechniqueBehaviorType<IdleTransfigurationBehavior> IDLE_TRANSFIGURATION = register("idle_transfiguration", IdleTransfigurationBehavior.CODEC);
	public static final TechniqueBehaviorType<ProjectionSorceryBehavior> PROJECTION_SORCERY = register("projection_sorcery", ProjectionSorceryBehavior.CODEC);
	public static final TechniqueBehaviorType<BarrageBehavior> BARRAGE = register("barrage", BarrageBehavior.CODEC);
	public static final TechniqueBehaviorType<SingularityBehavior> SINGULARITY = register("singularity", SingularityBehavior.CODEC);
	public static final TechniqueBehaviorType<VowStrikeBehavior> VOW_STRIKE = register("vow_strike", VowStrikeBehavior.CODEC);
	public static final TechniqueBehaviorType<DashBehavior> DASH = register("dash", DashBehavior.CODEC);
	public static final TechniqueBehaviorType<ShockwaveBehavior> SHOCKWAVE = register("shockwave", ShockwaveBehavior.CODEC);
	public static final TechniqueBehaviorType<ThornsAuraBehavior> THORNS_AURA = register("thorns_aura", ThornsAuraBehavior.CODEC);
	public static final TechniqueBehaviorType<MendOtherBehavior> MEND_OTHER = register("mend_other", MendOtherBehavior.CODEC);

	private TechniqueBehaviors() {
	}

	public static void init() {
		// Loads the class so the static fields register.
	}

	public static <B extends TechniqueBehavior> TechniqueBehaviorType<B> register(String name, MapCodec<B> codec) {
		return Registry.register(REGISTRY, JjkCraft.id(name), new TechniqueBehaviorType<>(codec));
	}
}
