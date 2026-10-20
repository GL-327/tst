package com.jjkcraft.client.hud;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;

import com.jjkcraft.client.config.ClientConfig;
import com.jjkcraft.client.state.ClientDomains;
import com.jjkcraft.client.ui.Ink;
import com.jjkcraft.client.ui.Palette;

/**
 * Step one of the domain sequence: the domain's name appears letter by letter, each glyph dropping
 * into place from a larger size, over a brief ink flash; "Domain Expansion" sits above it. Reduced
 * motion shows it whole and fades; reduced flashing replaces the flash with a steady wash.
 */
public final class DomainTitle {
	private static final long DURATION = 3200;
	private static final long LETTER_MS = 70;

	private static long startedAt = -1;
	private static String name = "";
	private static int color;
	private static boolean barrierless;

	private DomainTitle() {
	}

	public static void start(ClientDomains.Domain domain) {
		domain.definition().ifPresent(def -> {
			name = Component.translatable(def.name()).getString();
			color = 0xFF000000 | def.visuals().color();
			barrierless = !domain.barrier();
			startedAt = Util.getMillis();
		});
	}

	public static boolean active() {
		return startedAt >= 0;
	}

	public static void extract(GuiGraphicsExtractor g, DeltaTracker delta) {
		if (startedAt < 0) {
			return;
		}

		long t = Util.getMillis() - startedAt;
		if (t > DURATION) {
			startedAt = -1;
			return;
		}

		Minecraft mc = Minecraft.getInstance();
		if (mc.gui.hud.isHidden()||mc.gui.screen()!=null) {
			return;
		}

		boolean reduced = ClientConfig.reducedMotion();
		int w = g.guiWidth();
		int h = g.guiHeight();
		float fadeOut = Mth.clamp((DURATION - t) / 700f, 0, 1);

		// The opening flash (or a steady wash with reduced flashing).
		float flash = ClientConfig.reduceFlashing() ? 0.35f * Mth.clamp(1 - t / 900f, 0, 1) : Mth.clamp(1 - t / 260f, 0, 1) * 0.8f;
		if (flash > 0) {
			g.fill(0, 0, w, h, ARGB.color((int) (255 * flash), 0x05, 0x04, 0x08));
		}

		// Below the domain meter (top center) and above the crosshair.
		int cy = (int) (h * 0.4f);
		Component header = Component.translatable(barrierless ? "hud.jjkcraft.domain_expansion_barrierless" : "hud.jjkcraft.domain_expansion");
		Ink.centered(g, header, w / 2, cy - 22, Palette.TEXT_DIM, fadeOut * Mth.clamp(t / 300f, 0, 1));

		float scale = Math.min(2.5f,(w-48f)/Math.max(1,Ink.font().width(name)));
		long letterMs=Math.min(LETTER_MS,1400/Math.max(1,name.length()));
		int total = (int) (Ink.font().width(name) * scale);
		int x = (w - total) / 2;
		for (int i = 0; i < name.length(); i++) {
			String letter = name.substring(i, i + 1);
			int letterW = Ink.font().width(letter);
			long appear = reduced ? 0 : 200 + i * letterMs;
			float p = Mth.clamp((t - appear) / 180f, 0, 1);
			if (p > 0) {
				float drop = reduced ? 1 : 1 + 1.4f * (1 - p) * (1 - p);
				float a = Math.min(p, fadeOut);
				g.pose().pushMatrix();
				g.pose().translate(x + letterW * scale / 2, cy + 4);
				g.pose().scale(scale * drop, scale * drop);
				g.text(Ink.font(), letter, -letterW / 2, -4, ARGB.multiplyAlpha(color, a), true);
				g.pose().popMatrix();
			}

			x += (int) (letterW * scale);
		}

		// A brush underline that sweeps once the name is written.
		float sweep = reduced ? 1 : Mth.clamp((t - 200 - name.length() * letterMs) / 500f, 0, 1);
		int lineW = (int) (total * sweep);
		g.fill((w - total) / 2, cy + 18, (w - total) / 2 + lineW, cy + 20, ARGB.multiplyAlpha(color, 0.8f * fadeOut));
	}
}
