package com.jjkcraft.client.death;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import com.jjkcraft.JjkCraft;
import com.jjkcraft.death.SoulState;
import com.jjkcraft.death.SoulSystem;
import com.jjkcraft.client.JjkKeys;
import com.jjkcraft.client.ui.Ink;
import com.jjkcraft.client.ui.Palette;
import com.jjkcraft.network.payload.SoulPayloads;

public final class SoulClient {
    private static SoulPayloads.View view = new SoulPayloads.View(SoulState.EMPTY, 0, false, false, SoulPayloads.Terms.DEFAULT);
    private static boolean menu, respawnSent;
    private static long receivedAt;
    private SoulClient() { }
    public static SoulPayloads.View view() { return view; }
    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(SoulPayloads.View.TYPE, (next, ctx) -> { view = next; receivedAt = System.currentTimeMillis(); menu |= next.vesselMenu(); });
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register((handler, mc) -> { view = new SoulPayloads.View(SoulState.EMPTY, 0, false, false, SoulPayloads.Terms.DEFAULT); menu = false; respawnSent = false; });
        EntityRendererRegistry.register(SoulSystem.VESSEL, com.jjkcraft.client.render.culling.KoganeRenderer::new);
        JjkKeys.addHandler(mc -> {
            if (mc.player == null) return;
            String phase = view.state().phase();
            if (phase.equals("choosing") && !(mc.gui.screen() instanceof SoulScreen)) mc.gui.setScreen(new SoulScreen(true));
            boolean awaitingRespawn = phase.equals("respawning") || phase.equals("reincarnating") || phase.equals("becoming_object") || phase.equals("becoming_spirit");
            if (awaitingRespawn && !respawnSent && !mc.player.isAlive()) { respawnSent = true; mc.player.respawn(); }
            if (!phase.equals("choosing") && !awaitingRespawn) {
                respawnSent = false;
                if (mc.gui.screen() instanceof SoulScreen screen && screen.deathChoice()) mc.gui.setScreen(null);
            }
            if (menu) { menu = false; mc.gui.setScreen(new SoulScreen(false)); }
        });
        HudElementRegistry.addLast(JjkCraft.id("soul"), (g, dt) -> {
            Minecraft mc = Minecraft.getInstance(); if (mc.player == null || mc.gui.hud.isHidden()) return;
            if (view.state().phase().equals("object")) {
                long seconds = Math.max(0, (view.state().expires() - view.now()) / 20 - (System.currentTimeMillis() - receivedAt) / 1000);
                Ink.centered(g, Component.literal("CURSED OBJECT  ·  " + seconds / 60 + ":" + String.format(java.util.Locale.ROOT, "%02d", seconds % 60)), g.guiWidth()/2, 18, Palette.gold(), 1);
                Ink.centered(g, Component.literal("Deliver an object to a Vessel. Destruction or decay loses this character."), g.guiWidth()/2, 31, Palette.TEXT_DIM, 1);
            } else if (view.state().phase().equals("spirit")) Ink.centered(g, Component.literal("CURSED SPIRIT"), g.guiWidth()/2, 18, Palette.accent(), 1);
            else if (view.state().phase().equals("permanent")) Ink.centered(g, Component.literal("This character's cursed objects are lost."), g.guiWidth()/2, 18, Palette.danger(), 1);
        });
    }
}
