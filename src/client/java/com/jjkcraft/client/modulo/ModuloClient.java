package com.jjkcraft.client.modulo;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;

import com.jjkcraft.JjkCraft;
import com.jjkcraft.modulo.ModuloPayloads;

/** The client side of the Modulo expansion: renderers, HUD, sky geometry and packets. */
public final class ModuloClient {
	private ModuloClient() {
	}

	public static void init() {
		ModuloRenderers.init();
		ModuloSkyFx.init();
		ClientPlayNetworking.registerGlobalReceiver(ModuloPayloads.Sync.TYPE, (payload, context) -> ClientModulo.accept(payload));
		ClientPlayNetworking.registerGlobalReceiver(ModuloPayloads.Dialogue.TYPE, (payload, context) -> ClientModulo.accept(payload));
		ClientPlayNetworking.registerGlobalReceiver(ModuloPayloads.Event.TYPE, (payload, context) -> ClientModulo.accept(payload));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientModulo.reset());
		ClientTickEvents.END_CLIENT_TICK.register(ClientModulo::tick);
		HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, JjkCraft.id("modulo_mul_meter"), ModuloHud::mulMeter);
		HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, JjkCraft.id("modulo_tracker"), ModuloHud::tracker);
		// The dialogue box draws over recent chat lines, so a line is never hidden behind chat.
		HudElementRegistry.attachElementAfter(VanillaHudElements.CHAT, JjkCraft.id("modulo_dialogue"), ModuloHud::dialogue);
		// The epilogue's veil covers the whole HUD.
		HudElementRegistry.addLast(JjkCraft.id("modulo_epilogue"), EpilogueOverlay::extract);
	}
}
