package com.jjkcraft.client.vfx;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.phys.Vec3;

import com.jjkcraft.client.config.ClientConfig;
import com.jjkcraft.client.state.ClientDomains;
import com.jjkcraft.client.ui.Ink;
import com.jjkcraft.domain.ActiveDomain;
import com.jjkcraft.effect.JjkEffects;

/**
 * Screen-space domain effects, drawn over the HUD as plain overlays (no post shader, so they work
 * everywhere): the distortion at the screen's edges inside someone else's domain, and the
 * information flood that buries view and HUD while the player is overloaded. "Custom shaders off"
 * and Low VFX tone the distortion down to a still tint; reduced flashing stops the flood flicker.
 */
public final class DomainOverlay {
	private static final String GLYPHS = "∴∵∷∻≡≣⊢⊣⊤⊥⌂⌐◇○△▽✦✧";

	private DomainOverlay() {
	}

	public static void extract(GuiGraphicsExtractor g, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.gui.hud.isHidden()) {
			return;
		}

		Vec3 eye = mc.gameRenderer.mainCamera().position();
		ClientDomains.at(eye).ifPresent(domain -> {
			if (domain.phase() != ActiveDomain.Phase.CLOSING && domain.phase() != ActiveDomain.Phase.OPENING) {
				domain.definition().ifPresent(def -> distortion(g, def.visuals().color(), !domain.mine()));
			}
		});

		MobEffectInstance overload = mc.player.getEffect(JjkEffects.OVERLOADED);
		if (overload != null) {
			flood(g, overload.getDuration());
		}
	}

	/** Edge distortion: waves of the domain's color rolling in from the screen edges. */
	private static void distortion(GuiGraphicsExtractor g, int color, boolean hostile) {
		int w = g.guiWidth();
		int h = g.guiHeight();
		boolean still = !ClientConfig.get().vfx.customShaders || ClientConfig.lowVfx() || ClientConfig.reducedMotion();
		float strength = hostile ? 0.32f : 0.16f;
		int edge = ARGB.color((int) (255 * strength), color & 0xFFFFFF);
		int clear = edge & 0x00FFFFFF;
		int depth = h / 5;
		g.fillGradient(0, 0, w, depth, edge, clear);
		g.fillGradient(0, h - depth, w, h, clear, edge);
		if (still) {
			return;
		}

		// Rolling bands at the sides, a few pixels wide, drifting with time.
		float t = Util.getMillis() / 1000f;
		int bands = 18;
		for (int i = 0; i < bands; i++) {
			float phase = t * 1.6f + i * 0.7f;
			int bandX = (int) ((Mth.sin(phase) * 0.5f + 0.5f) * (w / 7f));
			int y0 = h * i / bands;
			int y1 = h * (i + 1) / bands;
			int c = ARGB.multiplyAlpha(edge, 0.6f + 0.4f * Mth.sin(phase * 1.3f));
			g.fill(0, y0, bandX, y1, c);
			g.fill(w - bandX, y0, w, y1, c);
		}
	}

	/** The information flood: glyph noise over everything, strongest at the start of a surge. */
	private static void flood(GuiGraphicsExtractor g, int ticksLeft) {
		int w = g.guiWidth();
		int h = g.guiHeight();
		boolean steady = ClientConfig.reduceFlashing();
		float strength = Mth.clamp(ticksLeft / 20f, 0, 1);
		g.fill(0, 0, w, h, ARGB.color((int) (210 * strength), 0x04, 0x07, 0x0C));

		long frame = steady ? 0 : Util.getMillis() / 70;
		int cell = 9;
		int cols = w / cell + 1;
		int rows = h / cell + 1;
		int shown = (int) (cols * rows * (ClientConfig.lowVfx() ? 0.18f : 0.4f) * strength);
		for (int i = 0; i < shown; i++) {
			long hsh = hash(frame * 7919 + i * 104729L);
			int col = (int) (hsh % cols);
			int row = (int) ((hsh >> 16) % rows);
			char glyph = GLYPHS.charAt((int) ((hsh >> 32) % GLYPHS.length()));
			int bright = 120 + (int) ((hsh >> 40) % 135);
			int color = ARGB.color((int) (255 * strength), bright / 2, bright, Math.min(255, bright + 40));
			g.text(Ink.font(), String.valueOf(glyph), col * cell, row * cell, color, false);
		}
	}

	private static long hash(long v) {
		v ^= v >>> 33;
		v *= 0xff51afd7ed558ccdL;
		v ^= v >>> 33;
		v *= 0xc4ceb9fe1a85ec53L;
		v ^= v >>> 33;
		return v & Long.MAX_VALUE;
	}
}
