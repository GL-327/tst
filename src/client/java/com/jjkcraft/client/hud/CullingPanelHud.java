package com.jjkcraft.client.hud;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import com.jjkcraft.client.config.ClientConfig;
import com.jjkcraft.client.config.JjkClientConfig;
import com.jjkcraft.client.state.ClientCulling;
import com.jjkcraft.client.ui.Ink;
import com.jjkcraft.client.ui.Palette;
import com.jjkcraft.network.payload.CullingPayloads;

/**
 * The Culling Game panel: the game day, your colony and points (or the days left to enter one),
 * and how many rules are in force. Shown only while the game runs and concerns you.
 */
public final class CullingPanelHud {
	public static final int WIDTH = 140;
	public static final int HEIGHT = 38;

	private CullingPanelHud() {
	}

	public static void extract(GuiGraphicsExtractor g, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		JjkClientConfig.HudElement cfg = ClientConfig.get().hud.cullingPanel;
		CullingPayloads.View view = ClientCulling.view();
		CullingPayloads.Me me = view.me();
		int daysLeft = ClientCulling.daysLeft();
		if (!cfg.visible || mc.gui.hud.isHidden() || mc.player == null || !view.running() || (!me.participant() && daysLeft < 0)) {
			return;
		}

		float alpha = cfg.opacity();
		HudLayout.push(g, cfg, WIDTH, HEIGHT);
		Ink.panel(g, 0, 0, WIDTH, HEIGHT, ClientCulling.ACCENT, alpha);
		Ink.text(g, Component.translatable("hud.jjkcraft.culling.title", view.day() + 1), 5, 4, ClientCulling.ACCENT, alpha);
		if (me.participant()) {
			Ink.text(g, Ink.fit(ClientCulling.colonyName(me.colony()), WIDTH - 46), 5, 15, Palette.TEXT, alpha);
			Component points = Component.translatable("hud.jjkcraft.culling.points", me.points());
			Ink.text(g, points, WIDTH - 5 - Ink.font().width(points), 15, Palette.gold(), alpha);
		} else {
			int color = daysLeft <= 3 ? Palette.danger() : Palette.TEXT;
			Ink.text(g, Component.translatable("hud.jjkcraft.culling.enter_within", daysLeft), 5, 15, color, alpha);
		}

		Ink.text(g, Component.translatable("hud.jjkcraft.culling.rules", view.rules().size()), 5, 26, Palette.TEXT_DIM, alpha);
		if (me.stagnant()) {
			Component tag = Component.translatable("hud.jjkcraft.culling.stagnant");
			Ink.text(g, tag, WIDTH - 5 - Ink.font().width(tag), 26, Palette.danger(), alpha);
		} else if (me.nearMaster()) {
			Component tag = Component.translatable("hud.jjkcraft.culling.master");
			Ink.text(g, tag, WIDTH - 5 - Ink.font().width(tag), 26, ClientCulling.ACCENT, alpha);
		}

		g.pose().popMatrix();
	}
}
