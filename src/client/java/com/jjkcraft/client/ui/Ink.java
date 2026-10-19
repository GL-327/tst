package com.jjkcraft.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

/**
 * Drawing primitives for the shared "ink-brushed panel" style. Everything is drawn from filled
 * rectangles so it works with any renderer, resource pack or shader pack. Brush edges are
 * deterministic (seeded by position) so panels do not shimmer frame to frame.
 */
public final class Ink {
	private Ink() {
	}

	public static Font font() {
		return Minecraft.getInstance().font;
	}

	/**
	 * A dark panel with ragged brush-stroke top and bottom edges and a thin accent stroke.
	 *
	 * @param alpha overall opacity multiplier (HUD opacity setting)
	 */
	public static void panel(GuiGraphicsExtractor g, int x, int y, int w, int h, int accent, float alpha) {
		int ink = ARGB.multiplyAlpha(Palette.INK, alpha);
		int edge = ARGB.multiplyAlpha(Palette.INK_EDGE, alpha);
		g.fill(x, y, x + w, y + h, ink);

		// Ragged edges: short strokes of varying length hanging off the top and bottom.
		int seed = x * 31 + y * 17 + w;
		for (int i = 0; i < w; i += 3) {
			int top = hash(seed + i) % 3;
			int bottom = hash(seed * 7 + i) % 3;
			int len = Math.min(3, w - i);
			g.fill(x + i, y - top, x + i + len, y, ARGB.multiplyAlpha(ink, 0.85f));
			g.fill(x + i, y + h, x + i + len, y + h + bottom, ARGB.multiplyAlpha(ink, 0.85f));
		}

		// Side edges are slightly darker.
		g.fill(x - 1, y + 1, x, y + h - 1, edge);
		g.fill(x + w, y + 1, x + w + 1, y + h - 1, edge);

		// Accent brush stroke along the bottom, tapering at both ends.
		int strokeY = y + h - 1;
		for (int i = 0; i < w; i++) {
			float t = (float) i / Math.max(1, w - 1);
			float taper = Mth.sin((float) Math.PI * t);
			if (taper > 0.15f) {
				g.fill(x + i, strokeY, x + i + 1, strokeY + 1, ARGB.multiplyAlpha(accent, alpha * taper));
			}
		}
	}

	/** A plain panel without the ragged edges, for dense layouts (lists, tooltips). */
	public static void flatPanel(GuiGraphicsExtractor g, int x, int y, int w, int h, float alpha) {
		g.fill(x, y, x + w, y + h, ARGB.multiplyAlpha(Palette.INK_DEEP, alpha));
		g.outline(x, y, w, h, ARGB.multiplyAlpha(Palette.INK_EDGE, alpha));
	}

	/** A horizontal gradient fill (vanilla only offers vertical gradients). */
	public static void hGradient(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int left, int right) {
		int w = x1 - x0;
		if (w <= 0) {
			return;
		}

		for (int i = 0; i < w; i++) {
			float t = w == 1 ? 0 : (float) i / (w - 1);
			g.fill(x0 + i, y0, x0 + i + 1, y1, ARGB.srgbLerp(t, left, right));
		}
	}

	public static void text(GuiGraphicsExtractor g, Component text, int x, int y, int color, float alpha) {
		g.text(font(), text, x, y, ARGB.multiplyAlpha(color, alpha), true);
	}

	public static void text(GuiGraphicsExtractor g, String text, int x, int y, int color, float alpha) {
		g.text(font(), text, x, y, ARGB.multiplyAlpha(color, alpha), true);
	}

	/** {@code text} cut to {@code width} pixels with an ellipsis, keeping its style. */
	public static Component fit(Component text, int width) {
		if (font().width(text) <= width) {
			return text;
		}

		String cut = font().plainSubstrByWidth(text.getString(), Math.max(0, width - font().width("…")));
		return Component.literal(cut.stripTrailing() + "…").withStyle(text.getStyle());
	}

	public static void centered(GuiGraphicsExtractor g, Component text, int cx, int y, int color, float alpha) {
		g.centeredText(font(), text, cx, y, ARGB.multiplyAlpha(color, alpha));
	}

	/** A filled diamond (badges, wheel nodes). */
	public static void diamond(GuiGraphicsExtractor g, int cx, int cy, int radius, int color) {
		for (int dy = -radius; dy <= radius; dy++) {
			int half = radius - Math.abs(dy);
			g.fill(cx - half, cy + dy, cx + half + 1, cy + dy + 1, color);
		}
	}

	/** A filled circle (radar backgrounds). */
	public static void disc(GuiGraphicsExtractor g, int cx, int cy, int radius, int color) {
		for (int dy = -radius; dy <= radius; dy++) {
			int half = (int) Math.round(Math.sqrt(radius * radius - dy * dy));
			g.fill(cx - half, cy + dy, cx + half + 1, cy + dy + 1, color);
		}
	}

	/** A ring approximated with short rectangles; {@code fraction} draws a partial ring clockwise from the top. */
	public static void ring(GuiGraphicsExtractor g, int cx, int cy, int radius, int thickness, float fraction, int color) {
		int steps = Math.max(12, radius * 6);
		int drawn = (int) (steps * Mth.clamp(fraction, 0, 1));
		for (int i = 0; i < drawn; i++) {
			double a = -Math.PI / 2 + 2 * Math.PI * i / steps;
			for (int t = 0; t < thickness; t++) {
				int px = cx + (int) Math.round(Math.cos(a) * (radius - t));
				int py = cy + (int) Math.round(Math.sin(a) * (radius - t));
				g.fill(px, py, px + 1, py + 1, color);
			}
		}
	}

	private static int hash(int v) {
		v ^= v >>> 16;
		v *= 0x7feb352d;
		v ^= v >>> 15;
		v *= 0x846ca68b;
		v ^= v >>> 16;
		return v & 0x7fffffff;
	}
}
