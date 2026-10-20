package com.jjkcraft.client.hud;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;

import com.jjkcraft.client.config.ClientConfig;
import com.jjkcraft.client.config.JjkClientConfig;
import com.jjkcraft.client.state.ClientEnergy;
import com.jjkcraft.client.ui.Ink;
import com.jjkcraft.client.ui.Palette;

/**
 * The segmented CE bar. Colors shift to the danger palette during overexertion, and a chevron
 * regen indicator shows when CE is refilling (two chevrons while idle-boosted). Meaning is also
 * carried by shape (stripes while overexerted), not color alone.
 */
public final class EnergyBarHud {
	public static final int WIDTH = 112;
	public static final int HEIGHT = 20;
	private static final int BAR_X = 18;
	private static final int BAR_Y = 6;
	private static final int BAR_W = 80;
	private static final int BAR_H = 6;

	private EnergyBarHud() {
	}

	public static void extract(GuiGraphicsExtractor g, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		JjkClientConfig.HudElement cfg = ClientConfig.get().hud.energyBar;
		if (!cfg.visible || mc.gui.hud.isHidden() || mc.player == null || !ClientEnergy.hasData() || ClientEnergy.max() <= 0) {
			return;
		}

		float alpha = cfg.opacity();
		double max = ClientEnergy.max();
		double shown = ClientEnergy.displayed();
		boolean strained = ClientEnergy.overexerted();
		long millis = Util.getMillis();
		boolean motion = !ClientConfig.reducedMotion();

		HudLayout.push(g, cfg, WIDTH, HEIGHT);
		Ink.panel(g, 0, 0, WIDTH, HEIGHT, strained ? Palette.danger() : Palette.accent(), alpha);
		Ink.text(g, "CE", 3, 5, strained ? Palette.danger() : Palette.energyHigh(), alpha);

		int segments = Mth.clamp((int) Math.round(max / 25.0), 4, 16);
		int gap = 1;
		int segW = Math.max(2, (BAR_W - gap * (segments - 1)) / segments);
		double perSegment = max / segments;

		int low = strained ? Palette.danger() : Palette.energy();
		int high = strained ? Palette.dangerHigh() : Palette.energyHigh();
		if (strained && !ClientConfig.reduceFlashing()) {
			float pulse = 0.65f + 0.35f * Mth.sin(millis / 90f);
			low = ARGB.scaleRGB(low, pulse);
			high = ARGB.scaleRGB(high, pulse);
		}

		for (int i = 0; i < segments; i++) {
			int sx = BAR_X + i * (segW + gap);
			g.fill(sx, BAR_Y, sx + segW, BAR_Y + BAR_H, ARGB.multiplyAlpha(0xFF1C1A24, alpha));
			double fill = Mth.clamp((shown - i * perSegment) / perSegment, 0, 1);
			if (fill > 0) {
				int fw = Math.max(1, (int) Math.round(segW * fill));
				float t = segments == 1 ? 1 : (float) i / (segments - 1);
				int color = ARGB.multiplyAlpha(ARGB.srgbLerp(t, low, high), alpha);
				g.fill(sx, BAR_Y, sx + fw, BAR_Y + BAR_H, color);
				g.fill(sx, BAR_Y, sx + fw, BAR_Y + 1, ARGB.multiplyAlpha(0x60FFFFFF, alpha));
				if (strained) {
					// Diagonal stripes so overexertion reads without color.
					for (int px = 0; px < fw; px++) {
						if (((px + (motion ? millis / 80 : 0)) % 4) == 0) {
							g.fill(sx + px, BAR_Y + 1, sx + px + 1, BAR_Y + BAR_H, ARGB.multiplyAlpha(0x70000000, alpha));
						}
					}
				}
			}
		}

		// Regen indicator.
		if (ClientEnergy.regenerating()) {
			int chevrons = ClientEnergy.idleBoosted() ? 2 : 1;
			int shift = motion ? (int) ((millis / 120) % 3) : 0;
			int cx = BAR_X + BAR_W + 3 + shift;
			for (int c = 0; c < chevrons; c++) {
				drawChevron(g, cx + c * 4, BAR_Y, ARGB.multiplyAlpha(Palette.good(), alpha));
			}
		}

		if (ClientConfig.get().hud.showNumbers) {
			String label = Math.round(shown) + "/" + (ClientEnergy.uncapped()?"∞":Math.round(max));
			g.pose().pushMatrix();
			g.pose().translate(BAR_X, BAR_Y + BAR_H + 2);
			g.pose().scale(0.5f, 0.5f);
			Ink.text(g, label, 0, 0, strained ? Palette.dangerHigh() : Palette.TEXT_DIM, alpha);
			g.pose().popMatrix();
		}

		g.pose().popMatrix();
	}

	private static void drawChevron(GuiGraphicsExtractor g, int x, int y, int color) {
		for (int i = 0; i < 3; i++) {
			g.fill(x + i, y + i, x + i + 1, y + i + 1, color);
			g.fill(x + i, y + 5 - i, x + i + 1, y + 6 - i, color);
		}
	}
}
