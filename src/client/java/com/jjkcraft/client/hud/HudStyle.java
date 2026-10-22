package com.jjkcraft.client.hud;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

import com.jjkcraft.client.ui.Ink;
import com.jjkcraft.client.ui.Palette;

/**
 * Shared measurements and small drawing pieces for the in-game HUD, so the status column (CE,
 * grade, combat and event panels) lines up and every gauge looks the same.
 */
public final class HudStyle {
	/** Width shared by every panel in the top-left status column. */
	public static final int COLUMN_WIDTH = 124;
	/** Unfilled part of a gauge. */
	public static final int TRACK = 0xFF1B1A24;

	private HudStyle() {
	}

	/** A thin gauge with a highlight line along its filled top edge. */
	public static void gauge(GuiGraphicsExtractor g, int x, int y, int w, int h, double fraction, int color, float alpha) {
		g.fill(x, y, x + w, y + h, ARGB.multiplyAlpha(TRACK, alpha));
		int filled = (int) Math.round(w * Mth.clamp(fraction, 0, 1));
		if (filled > 0) {
			g.fill(x, y, x + filled, y + h, ARGB.multiplyAlpha(color, alpha));
			if (h > 2) {
				g.fill(x, y, x + filled, y + 1, ARGB.multiplyAlpha(0x50FFFFFF, alpha));
			}
		}
	}

	/** A small key cap showing a key binding's name, right-aligned at {@code right}. Returns its left edge. */
	public static int keyCap(GuiGraphicsExtractor g, Component key, int right, int y, float alpha) {
		Component label = Ink.fit(key, 30);
		int w = Ink.font().width(label) + 4;
		int x = right - w;
		g.fill(x, y, right, y + 10, ARGB.multiplyAlpha(0xE0101018, alpha));
		g.outline(x, y, w, 10, ARGB.multiplyAlpha(Palette.INK_EDGE, alpha));
		Ink.text(g, label, x + 2, y + 1, Palette.TEXT_DIM, alpha);
		return x;
	}
}
