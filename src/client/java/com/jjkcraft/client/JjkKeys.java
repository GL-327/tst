package com.jjkcraft.client;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import com.jjkcraft.JjkCraft;
import com.jjkcraft.client.screen.ProfileScreen;
import com.jjkcraft.client.screen.TechniqueWheelScreen;
import com.jjkcraft.client.state.ClientTechniques;
import com.jjkcraft.network.payload.CastPayload;
import com.jjkcraft.network.payload.SelectSlotPayload;

/** Every key the mod uses. All are rebindable in Controls; defaults avoid vanilla bindings. */
public final class JjkKeys {
	public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(JjkCraft.id("main"));

	public static final KeyMapping PROFILE = key("profile", InputConstants.KEY_J);
	public static final KeyMapping CAST = key("cast", InputConstants.KEY_R);
	public static final KeyMapping NEXT_SLOT = key("next_slot", InputConstants.KEY_V);
	public static final KeyMapping WHEEL = key("wheel", InputConstants.KEY_G);
	public static final KeyMapping SENSE = key("sense", InputConstants.KEY_B);
	public static final KeyMapping DOMAIN = key("domain", InputConstants.KEY_K);
	public static final KeyMapping SIMPLE_DOMAIN = key("simple_domain", InputConstants.KEY_N);
	public static final KeyMapping REVERSE = key("reverse", InputConstants.KEY_H);
	public static final KeyMapping CULLING = key("culling", InputConstants.KEY_M);

	/** Extra per-tick key handlers added by later systems (sense, domains). */
	private static final List<Consumer<Minecraft>> HANDLERS = new ArrayList<>();
	private static boolean castWasDown;

	private JjkKeys() {
	}

	public static void addHandler(Consumer<Minecraft> handler) {
		HANDLERS.add(handler);
	}

	public static void init() {
		ClientTickEvents.END_CLIENT_TICK.register(JjkKeys::tick);
	}

	private static void tick(Minecraft mc) {
		boolean inGame = mc.player != null && mc.gui.screen() == null;

		while (PROFILE.consumeClick()) {
			if (inGame) {
				mc.gui.setScreen(new ProfileScreen());
			}
		}

		// Cast: send press and release so channelled techniques end when the key comes up.
		boolean castDown = CAST.isDown() && inGame;
		if (castDown != castWasDown && mc.player != null) {
			ClientPlayNetworking.send(new CastPayload(-1, castDown));
		}

		castWasDown = castDown;
		while (CAST.consumeClick()) {
			// Presses are handled through isDown above; drain the queue.
		}

		while (NEXT_SLOT.consumeClick()) {
			if (inGame) {
				int slots = Math.max(1, ClientTechniques.usableSlots());
				ClientPlayNetworking.send(new SelectSlotPayload((ClientTechniques.state().selected() + 1) % slots));
			}
		}

		while (WHEEL.consumeClick()) {
			if (inGame) {
				mc.gui.setScreen(new TechniqueWheelScreen());
			}
		}

		for (Consumer<Minecraft> handler : HANDLERS) {
			handler.accept(mc);
		}
	}

	private static KeyMapping key(String name, int defaultKey) {
		return KeyMappingHelper.registerKeyMapping(new KeyMapping("key.jjkcraft." + name, InputConstants.Type.KEYBOARD, defaultKey, CATEGORY));
	}
}
