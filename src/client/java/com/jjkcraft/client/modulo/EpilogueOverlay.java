package com.jjkcraft.client.modulo;

import java.util.List;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import com.jjkcraft.client.config.ClientConfig;
import com.jjkcraft.client.ui.Ink;
import com.jjkcraft.client.ui.Palette;

/**
 * The epilogue sequence (mission 11, "A Bright Future"): motes of light rise around the player as
 * Tokyo's cursed spirits come to rest, and six lines of text fade in and out over a darkened screen.
 * No voice. Reduced motion shows each line whole with no fades; the screen never flashes.
 */
public final class EpilogueOverlay {
	public static final int LINES = 6;
	private static final long PER_LINE = 5000;
	public static final long LENGTH_MS = PER_LINE * LINES + 2000;

	private EpilogueOverlay() {
	}

	public static void extract(GuiGraphicsExtractor g, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		if (!"epilogue".equals(ClientModulo.sequence()) || mc.player == null) {
			return;
		}

		long t = ClientModulo.sequenceElapsed();
		boolean still = ClientConfig.reducedMotion();
		float veil = still ? 0.82f : Mth.clamp(t / 1500f, 0, 1) * 0.82f * Mth.clamp((LENGTH_MS - t) / 1500f, 0, 1);
		g.fill(0, 0, g.guiWidth(), g.guiHeight(), ARGB.color((int) (veil * 255), 6, 6, 10));
		int index = (int) Math.min(LINES - 1, t / PER_LINE);
		long into = t - index * PER_LINE;
		float alpha = still ? 1f : Mth.clamp(into / 700f, 0, 1) * Mth.clamp((PER_LINE - into) / 700f, 0, 1);
		Component text = Component.translatable("epilogue.jjkcraft.modulo." + (index + 1));
		List<FormattedCharSequence> lines = Ink.font().split(text, Math.min(320, g.guiWidth() - 40));
		int y = g.guiHeight() / 2 - lines.size() * 6;
		for (FormattedCharSequence line : lines) {
			int w = Ink.font().width(line);
			g.text(Ink.font(), line, (g.guiWidth() - w) / 2, y, ARGB.multiplyAlpha(Palette.PAPER, Math.max(0.02f, alpha)), true);
			y += 12;
		}

		Component title = Component.translatable("advancements.jjkcraft.modulo.epilogue.title");
		Ink.centered(g, title, g.guiWidth() / 2, g.guiHeight() / 2 - 40, ARGB.multiplyAlpha(ClientModulo.ACCENT, Math.max(0.02f, veil)), 1f);
	}
}
