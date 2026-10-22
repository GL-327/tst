package com.jjkcraft.client.render;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

import net.fabricmc.fabric.api.resource.v1.ResourceLoader;

import com.jjkcraft.JjkCraft;

/**
 * Remembers whether optional client resources exist (damage-variant textures, glow layers, LOD
 * models). Renderers ask once per resource instead of searching every resource pack each frame.
 * The answers are forgotten whenever resources reload, so resource packs and prop-pack edits apply.
 */
public final class ResourcePresence {
	private static final Map<Identifier, Boolean> KNOWN = new ConcurrentHashMap<>();

	private ResourcePresence() {
	}

	public static void init() {
		ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(JjkCraft.id("resource_presence"),
				(ResourceManagerReloadListener) manager -> KNOWN.clear());
	}

	public static boolean exists(Identifier id) {
		Boolean known = KNOWN.get(id);
		if (known != null) {
			return known;
		}

		boolean present = Minecraft.getInstance().getResourceManager().getResource(id).isPresent();
		KNOWN.put(id, present);
		return present;
	}
}
