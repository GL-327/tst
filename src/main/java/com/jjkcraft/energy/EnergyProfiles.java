package com.jjkcraft.energy;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;

import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.entity.EntityType;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

import com.jjkcraft.registry.JjkRegistries;

/** Finds the energy profile for an entity type. Cached per server, because world registries do not change while it runs. */
public final class EnergyProfiles {
	private static final Map<EntityType<?>, Optional<EnergyProfile>> CACHE = new IdentityHashMap<>();
	private static RegistryAccess cachedFor;

	private EnergyProfiles() {
	}

	public static void init() {
		ServerLifecycleEvents.SERVER_STARTING.register(server -> clear());
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> clear());
	}

	public static synchronized Optional<EnergyProfile> forType(RegistryAccess access, EntityType<?> type) {
		if (cachedFor != access) {
			CACHE.clear();
			cachedFor = access;
		}

		return CACHE.computeIfAbsent(type, t -> lookup(access, t));
	}

	private static Optional<EnergyProfile> lookup(RegistryAccess access, EntityType<?> type) {
		Holder<EntityType<?>> holder = type.builtInRegistryHolder();
		return access.lookup(JjkRegistries.ENERGY_PROFILE)
				.flatMap(registry -> registry.stream()
						.filter(profile -> profile.entities().contains(holder))
						.findFirst());
	}

	private static synchronized void clear() {
		CACHE.clear();
		cachedFor = null;
	}
}
