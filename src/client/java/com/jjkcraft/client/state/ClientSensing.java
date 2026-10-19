package com.jjkcraft.client.state;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.Entity;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import com.jjkcraft.network.payload.SensePayload;
import com.jjkcraft.network.payload.SenseTogglePayload;

/** What the server says the player senses. Drives the outlines and the radar. */
public final class ClientSensing {
	/** Outlines and dots fade if the server stops sending (lag, or sensing ended without a packet). */
	private static final long STALE_MS = 2000;

	private static boolean active;
	private static float range;
	private static Map<Integer, SensePayload.Sensed> sensed = Map.of();
	private static long receivedAt;

	private ClientSensing() {
	}

	public static void accept(SensePayload payload) {
		active = payload.active();
		range = payload.range();
		Map<Integer, SensePayload.Sensed> next = new HashMap<>();
		for (SensePayload.Sensed s : payload.sensed()) {
			next.put(s.entityId(), s);
		}

		sensed = next;
		receivedAt = System.currentTimeMillis();
	}

	public static void toggle() {
		if (Minecraft.getInstance().player != null) {
			ClientPlayNetworking.send(new SenseTogglePayload(!isActive()));
		}
	}

	public static boolean isActive() {
		return active && System.currentTimeMillis() - receivedAt < STALE_MS;
	}

	public static float range() {
		return range;
	}

	public static List<SensePayload.Sensed> sensed() {
		return isActive() ? List.copyOf(sensed.values()) : List.of();
	}

	/** The outline color for an entity, or 0 when it isn't sensed. */
	public static int outlineColor(Entity entity) {
		if (!active || sensed.isEmpty()) {
			return 0;
		}

		SensePayload.Sensed s = sensed.get(entity.getId());
		return s == null || !isActive() ? 0 : outline(s.color());
	}

	/** The grade color lifted a third of the way toward white, so dull grades still read against stone. */
	static int outline(int color) {
		int r = ARGB.red(color);
		int g = ARGB.green(color);
		int b = ARGB.blue(color);
		return ARGB.color(255, r + (255 - r) / 3, g + (255 - g) / 3, b + (255 - b) / 3);
	}

	public static void reset() {
		active = false;
		range = 0;
		sensed = Map.of();
		receivedAt = 0;
	}
}
