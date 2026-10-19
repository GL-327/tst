package com.jjkcraft.client.vfx;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;

import com.jjkcraft.client.config.ClientConfig;
import com.jjkcraft.client.ui.Ink;
import com.jjkcraft.client.ui.InkToast;
import com.jjkcraft.client.ui.Palette;
import com.jjkcraft.grade.GradeDefinition;
import com.jjkcraft.grade.Grades;
import com.jjkcraft.network.payload.PromotionPayload;

/**
 * The promotion ceremony: an ink wash closes in, a brush stroke in the new grade's color sweeps
 * across, the grade name is revealed stroke by stroke, then everything fades. About four seconds.
 * With reduced motion it is a short fade with no sweep. It is an overlay, so play continues.
 */
public final class PromotionCeremony {
	private static final long DURATION = 4000;
	private static final long REDUCED_DURATION = 2400;

	private static long startedAt = -1;
	private static Component gradeName = Component.empty();
	private static int color = 0xFFFFFFFF;

	private PromotionCeremony() {
	}

	public static void start(PromotionPayload payload) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) {
			return;
		}

		GradeDefinition def = Grades.get(mc.level.registryAccess(), payload.to()).orElse(null);
		gradeName = def != null ? Component.translatable(def.name()) : Component.literal(payload.to().toString());
		color = def != null ? def.color() : Palette.gold();
		startedAt = Util.getMillis();
		InkToast.show(Component.translatable("toast.jjkcraft.promoted"), gradeName, color);
	}

	public static boolean active() {
		return startedAt >= 0;
	}

	public static void extract(GuiGraphicsExtractor g, DeltaTracker delta) {
		if (startedAt < 0) {
			return;
		}

		boolean reduced = ClientConfig.reducedMotion();
		long duration = reduced ? REDUCED_DURATION : DURATION;
		long t = Util.getMillis() - startedAt;
		if (t > duration) {
			startedAt = -1;
			return;
		}

		int w = g.guiWidth();
		int h = g.guiHeight();
		float fadeIn = Mth.clamp(t / 400f, 0, 1);
		float fadeOut = Mth.clamp((duration - t) / 900f, 0, 1);
		float alpha = Math.min(fadeIn, fadeOut);

		// Ink wash.
		g.fill(0, 0, w, h, ARGB.color((int) (150 * alpha), 0x05, 0x04, 0x08));

		int cy = h / 2;
		if (!reduced) {
			// Brush stroke sweeping left to right, with a ragged edge.
			float sweep = Mth.clamp((t - 300) / 1100f, 0, 1);
			int strokeW = (int) (w * easeOut(sweep));
			for (int x = 0; x < strokeW; x += 2) {
				int jitter = (hash(x) % 5) - 2;
				int thickness = 16 + (hash(x * 3) % 4);
				int c = ARGB.multiplyAlpha(color, 0.55f * alpha);
				g.fill(x, cy - thickness / 2 + jitter, x + 2, cy + thickness / 2 + jitter, c);
			}

			// Ink splatter around the stroke head.
			for (int i = 0; i < 24; i++) {
				int sx = (int) (strokeW - 30 + (hash(i * 31) % 60));
				int sy = cy + (hash(i * 17) % 48) - 24;
				int size = 1 + hash(i * 7) % 3;
				g.fill(sx, sy, sx + size, sy + size, ARGB.multiplyAlpha(color, 0.5f * alpha));
			}
		}

		// Title, revealed left to right.
		Component label = Component.translatable("ceremony.jjkcraft.promoted");
		g.centeredText(Ink.font(), label, w / 2, cy - 30, ARGB.multiplyAlpha(Palette.TEXT_DIM, alpha));

		float scale = 3f;
		int textW = (int) (Ink.font().width(gradeName) * scale);
		float reveal = reduced ? 1f : Mth.clamp((t - 600) / 1200f, 0, 1);
		int left = (w - textW) / 2;
		g.enableScissor(left - 4, cy - 20, left + (int) ((textW + 8) * reveal), cy + 20);
		g.pose().pushMatrix();
		g.pose().translate(left, cy - 12);
		g.pose().scale(scale, scale);
		g.text(Ink.font(), gradeName, 0, 0, ARGB.multiplyAlpha(color, alpha), true);
		g.pose().popMatrix();
		g.disableScissor();
	}

	private static float easeOut(float x) {
		return 1 - (1 - x) * (1 - x) * (1 - x);
	}

	private static int hash(int v) {
		v ^= v >>> 16;
		v *= 0x45d9f3b;
		v ^= v >>> 16;
		return v & 0x7fffffff;
	}
}
