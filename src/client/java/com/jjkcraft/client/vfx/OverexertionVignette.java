package com.jjkcraft.client.vfx;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;

import com.jjkcraft.client.config.ClientConfig;
import com.jjkcraft.client.state.ClientEnergy;
import com.jjkcraft.client.ui.Palette;

/**
 * A dark red edge vignette while the player is overexerted. Drawn in the HUD layer from plain
 * gradients, so it works with shader packs. It pulses unless reduced flashing is on, and fades out
 * over the last second of the window.
 */
public final class OverexertionVignette {
	private static final int STRIPS = 14;

	private OverexertionVignette() {
	}

	public static void extract(GuiGraphicsExtractor g, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.gui.hud.isHidden() || !ClientEnergy.hasData() || !ClientEnergy.overexerted()) {
			return;
		}

		float fadeOut = Mth.clamp(ClientEnergy.overexertTicksLeft() / 20f, 0, 1);
		float pulse = ClientConfig.reduceFlashing() ? 0.75f : 0.6f + 0.4f * Mth.sin(Util.getMillis() / 140f);
		float strength = (ClientConfig.lowVfx() ? 0.35f : 0.55f) * fadeOut * pulse;
		int edge = ARGB.color(Mth.floor(255 * strength), ARGB.scaleRGB(Palette.danger(), 0.45f) & 0xFFFFFF);
		int clear = edge & 0x00FFFFFF;

		int w = g.guiWidth();
		int h = g.guiHeight();
		int depthY = h / 6;
		int depthX = w / 8;

		g.fillGradient(0, 0, w, depthY, edge, clear);
		g.fillGradient(0, h - depthY, w, h, clear, edge);

		int stripW = Math.max(1, depthX / STRIPS);
		for (int i = 0; i < STRIPS; i++) {
			float t = 1 - (float) i / STRIPS;
			int color = ARGB.multiplyAlpha(edge, t * t);
			g.fill(i * stripW, 0, (i + 1) * stripW, h, color);
			g.fill(w - (i + 1) * stripW, 0, w - i * stripW, h, color);
		}
	}
}
