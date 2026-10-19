package com.jjkcraft.client.vfx;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;

import com.jjkcraft.client.config.ClientConfig;

/**
 * The Black Flash frame flash: for a moment the screen splits into red and black stripes with a
 * jag of black lightning across them, then fades. Reduced flashing shows a single steady dark-red
 * wash instead; reduced motion keeps the stripes still.
 */
public final class BlackFlashFx {
	private static final long DURATION = 380;
	private static long startedAt = -1;
	private static long seed;

	private BlackFlashFx() {
	}

	public static void trigger() {
		startedAt = Util.getMillis();
		seed = startedAt * 31 + 7;
	}

	public static boolean active() {
		return startedAt >= 0 && Util.getMillis() - startedAt < DURATION;
	}

	public static void extract(GuiGraphicsExtractor g, DeltaTracker delta) {
		if (!active() || Minecraft.getInstance().gui.hud.isHidden()) {
			return;
		}

		long t = Util.getMillis() - startedAt;
		float fade = Mth.clamp(1 - t / (float) DURATION, 0, 1);
		int w = g.guiWidth();
		int h = g.guiHeight();
		if (ClientConfig.reduceFlashing()) {
			g.fill(0, 0, w, h, ARGB.color((int) (110 * fade), 0x50, 0x06, 0x0C));
			return;
		}

		// Stripes, deep red and black, slanted, sliding unless motion is reduced. They frame the
		// blow: strongest at the edges of the screen, clear around the crosshair.
		int stripe = Math.max(6, w / 14);
		int piece = Math.max(2, stripe / 4);
		int shift = ClientConfig.reducedMotion() ? 0 : (int) (t / 8);
		for (int x = -h; x < w + h; x += stripe * 2) {
			int x0 = x + shift % (stripe * 2);
			for (int y = 0; y < h; y += 3) {
				for (int p = 0; p < stripe * 2; p += piece) {
					int sx = x0 + y / 2 + p;
					if (sx + piece < 0 || sx > w) {
						continue;
					}

					float edge = edge(sx + piece / 2f, y, w, h) * fade;
					if (edge <= 0.01f) {
						continue;
					}

					// A thin band of deep red against wide black, so it reads as black lightning.
					boolean red = p < stripe * 2 / 3;
					int color = red ? ARGB.color((int) (235 * edge), 0x9A, 0x06, 0x14) : ARGB.color((int) (205 * edge), 0x05, 0x02, 0x04);
					g.fill(sx, y, sx + piece, y + 3, color);
				}
			}
		}

		// A jag of black lightning across the middle.
		long r = seed;
		int px = w / 6;
		int py = h / 2;
		while (px < w * 5 / 6) {
			r = r * 6364136223846793005L + 1442695040888963407L;
			int nx = px + 10 + (int) ((r >>> 33) % 22);
			int ny = Mth.clamp(py + (int) ((r >>> 20) % 41) - 20, h / 4, h * 3 / 4);
			line(g, px, py, nx, ny, ARGB.color((int) (255 * fade), 0x02, 0x01, 0x02), 3);
			line(g, px, py - 1, nx, ny - 1, ARGB.color((int) (200 * fade), 0xFF, 0x50, 0x60), 1);
			px = nx;
			py = ny;
		}
	}

	/** 0 in the middle of the screen, rising to 1 toward the edges. */
	private static float edge(float x, float y, int w, int h) {
		float dx = (x - w / 2f) / (w / 2f);
		float dy = (y - h / 2f) / (h / 2f);
		float d = (float) Math.sqrt(dx * dx + dy * dy) / 1.4142f;
		return Mth.clamp((d - 0.3f) / 0.35f, 0, 1);
	}

	private static void line(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int color, int thickness) {
		int steps = Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0));
		for (int i = 0; i <= steps; i++) {
			int x = x0 + (x1 - x0) * i / Math.max(1, steps);
			int y = y0 + (y1 - y0) * i / Math.max(1, steps);
			g.fill(x, y, x + thickness, y + thickness, color);
		}
	}
}
