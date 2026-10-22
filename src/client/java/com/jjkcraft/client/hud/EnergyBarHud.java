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
 * The CE gauge at the top of the status column: a label with regen chevrons and the exact value
 * above a full-width segmented bar. Colors shift to the danger palette during overexertion, which
 * also adds moving stripes so the state reads without color.
 */
public final class EnergyBarHud {
	public static final int WIDTH = HudStyle.COLUMN_WIDTH;
	public static final int HEIGHT = 22;
	private static final int BAR_X = 5;
	private static final int BAR_Y = 14;
	private static final int BAR_W = WIDTH - 9;
	private static final int BAR_H = 5;

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
		int accent = strained ? Palette.danger() : Palette.energyHigh();

		HudLayout.push(g, cfg, WIDTH, HEIGHT);
		Ink.panel(g, 0, 0, WIDTH, HEIGHT, strained ? Palette.danger() : Palette.accent(), alpha);
		Ink.text(g, "CE", BAR_X, 4, accent, alpha);

		if (ClientEnergy.regenerating()) {
			int chevrons = ClientEnergy.idleBoosted() ? 2 : 1;
			int shift = motion ? (int) ((millis / 140) % 3) : 0;
			for (int c = 0; c < chevrons; c++) {
				drawChevron(g, BAR_X + 16 + shift + c * 4, 5, ARGB.multiplyAlpha(Palette.good(), alpha));
			}
		}

		if (ClientConfig.get().hud.showNumbers) {
			String label = Math.round(shown) + " / " + (ClientEnergy.uncapped() ? "∞" : Math.round(max));
			Ink.text(g, label, WIDTH - 4 - Ink.font().width(label), 4, strained ? Palette.dangerHigh() : Palette.TEXT_DIM, alpha);
		}

		int segments = Mth.clamp((int) Math.round(max / 25.0), 6, 20);
		int gap = 1;
		int segW = Math.max(2, (BAR_W - gap * (segments - 1)) / segments);
		// Spread the rounding remainder so the bar always ends flush with the panel edge.
		int spare = BAR_W - (segW * segments + gap * (segments - 1));
		double perSegment = max / segments;

		int low = strained ? Palette.danger() : Palette.energy();
		int high = strained ? Palette.dangerHigh() : Palette.energyHigh();
		if (strained && !ClientConfig.reduceFlashing()) {
			float pulse = 0.65f + 0.35f * Mth.sin(millis / 90f);
			low = ARGB.scaleRGB(low, pulse);
			high = ARGB.scaleRGB(high, pulse);
		}

		int sx = BAR_X;
		for (int i = 0; i < segments; i++) {
			int w = segW + (i < spare ? 1 : 0);
			g.fill(sx, BAR_Y, sx + w, BAR_Y + BAR_H, ARGB.multiplyAlpha(HudStyle.TRACK, alpha));
			double fill = Mth.clamp((shown - i * perSegment) / perSegment, 0, 1);
			if (fill > 0) {
				int fw = Math.max(1, (int) Math.round(w * fill));
				float t = segments == 1 ? 1 : (float) i / (segments - 1);
				g.fill(sx, BAR_Y, sx + fw, BAR_Y + BAR_H, ARGB.multiplyAlpha(ARGB.srgbLerp(t, low, high), alpha));
				g.fill(sx, BAR_Y, sx + fw, BAR_Y + 1, ARGB.multiplyAlpha(0x50FFFFFF, alpha));
				if (strained) {
					for (int px = 0; px < fw; px++) {
						if (((px + (motion ? millis / 80 : 0)) % 4) == 0) {
							g.fill(sx + px, BAR_Y + 1, sx + px + 1, BAR_Y + BAR_H, ARGB.multiplyAlpha(0x70000000, alpha));
						}
					}
				}
			}

			sx += w + gap;
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
