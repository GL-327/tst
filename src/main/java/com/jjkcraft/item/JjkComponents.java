package com.jjkcraft.item;

import com.mojang.serialization.Codec;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;

import com.jjkcraft.JjkCraft;
import com.jjkcraft.registry.LegacyIds;

/** Item data components. */
public final class JjkComponents {
	/** The technique an Arsenal Blade carries. */
	public static final DataComponentType<Identifier> LOADED_TECHNIQUE = register("loaded_technique",
			DataComponentType.<Identifier>builder().persistent(LegacyIds.TECHNIQUE_ID).networkSynchronized(Identifier.STREAM_CODEC).build());
	/** The domain instance an Arsenal Blade belongs to; it vanishes when that domain is gone. */
	public static final DataComponentType<Integer> ARSENAL_DOMAIN = register("arsenal_domain",
			DataComponentType.<Integer>builder().persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT).build());

	/** A Straw Doll's link to its target. */
	public static final DataComponentType<StrawDollLink> STRAW_DOLL_LINK = register("straw_doll_link",
			DataComponentType.<StrawDollLink>builder().persistent(StrawDollLink.CODEC).networkSynchronized(StrawDollLink.STREAM_CODEC).build());
	/** Culling Game points held in a Tally Ledger. */
	public static final DataComponentType<Integer> LEDGER_POINTS = register("ledger_points",
			DataComponentType.<Integer>builder().persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT).build());

	private JjkComponents() {
	}

	public static void init() {
		// Loads the class so the static fields register.
	}

	private static <T> DataComponentType<T> register(String name, DataComponentType<T> type) {
		return Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, JjkCraft.id(name), type);
	}
}
