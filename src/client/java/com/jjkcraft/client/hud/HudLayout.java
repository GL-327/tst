package com.jjkcraft.client.hud;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Util;

import com.jjkcraft.client.config.ClientConfig;
import com.jjkcraft.client.config.JjkClientConfig;
import com.jjkcraft.client.mixin.BossHealthOverlayAccessor;
import com.jjkcraft.core.ui.HudClearance;

/**
 * Places a HUD element from its config (anchor, offset, scale) and applies the scale to the pose
 * stack. Elements draw in their own unscaled coordinates starting at (0, 0).
 */
public final class HudLayout {
	/** How long a drawn element still counts as on screen for {@link #recentBoxes()}. */
	private static final long RECENT_MS = 250;

	private static final Map<JjkClientConfig.HudElement, Drawn> DRAWN = new IdentityHashMap<>();

	private record Drawn(HudClearance.Box box, long at) {
	}

	private HudLayout() {
	}

	/** Where the mod's HUD elements were drawn in the last frames, in GUI pixels (for other mods' overlays). */
	public static List<HudClearance.Box> recentBoxes() {
		long now = Util.getMillis();
		List<HudClearance.Box> boxes = new ArrayList<>();
		for (Drawn drawn : DRAWN.values()) {
			if (now - drawn.at() < RECENT_MS) {
				boxes.add(drawn.box());
			}
		}

		return boxes;
	}

	/**
	 * Pushes a pose with the element's translation and scale. Callers must call
	 * {@code g.pose().popMatrix()} afterwards.
	 *
	 * @param width  element width in its own unscaled units
	 * @param height element height in its own unscaled units
	 */
	/** The lowest pixel the vanilla boss bars reach (0 with none showing). */
	public static int bossBarsBottom(int screenHeight) {
		Minecraft mc = Minecraft.getInstance();
		int bars = ((BossHealthOverlayAccessor) mc.gui.hud.getBossOverlay()).jjkcraft$events().size();
		if (bars == 0) {
			return 0;
		}

		// Vanilla: bars 19 px apart from y 12, and it stops drawing past a third of the screen.
		int shown = Math.min(bars, Math.max(1, (screenHeight / 3 - 12) / 19 + 1));
		return 12 + 19 * (shown - 1) + 5;
	}

	public static void push(GuiGraphicsExtractor g, JjkClientConfig.HudElement element, int width, int height) {
		pushBelow(g, element, width, height);
	}

	/**
	 * Like {@link #push}, but if the element would overlap one of {@code above} as it was drawn in the
	 * last frames, it steps down below it. Only the later element moves, so the two never trade
	 * places from frame to frame. Used where elements share a column (the Mul meter and mission
	 * tracker under the grade badge and the Culling Game panel).
	 */
	public static void pushBelow(GuiGraphicsExtractor g, JjkClientConfig.HudElement element, int width, int height, JjkClientConfig.HudElement... above) {
		float scale = element.scale() * ClientConfig.uiScale();
		float w = width * scale;
		float h = height * scale;
		int sw = g.guiWidth();
		int sh = g.guiHeight();

		float x = switch (element.anchor) {
			case TOP_LEFT, CENTER_LEFT, BOTTOM_LEFT -> 0;
			case TOP_CENTER, CENTER, BOTTOM_CENTER -> (sw - w) / 2f;
			case TOP_RIGHT, CENTER_RIGHT, BOTTOM_RIGHT -> sw - w;
		};
		float y = switch (element.anchor) {
			case TOP_LEFT, TOP_CENTER, TOP_RIGHT -> 0;
			case CENTER_LEFT, CENTER, CENTER_RIGHT -> (sh - h) / 2f;
			case BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT -> sh - h;
		};

		int offsetY = element.offsetY;
		if (element.anchor == JjkClientConfig.Anchor.TOP_CENTER) {
			// Boss bars own the top centre: step below them.
			offsetY = Math.max(offsetY, bossBarsBottom(sh) + 4);
		}

		x = clamp(x + element.offsetX, 0, Math.max(0, sw - w));
		y = clamp(y + offsetY, 0, Math.max(0, sh - h));
		long now = Util.getMillis();
		for (JjkClientConfig.HudElement other : above) {
			Drawn drawn = other == null || other == element ? null : DRAWN.get(other);
			if (drawn == null || now - drawn.at() >= RECENT_MS) {
				continue;
			}

			HudClearance.Box box = drawn.box();
			boolean overlaps = x < box.x() + box.width() && box.x() < x + w && y < box.y() + box.height() && box.y() < y + h;
			if (overlaps) {
				y = clamp(box.y() + box.height() + 3, 0, Math.max(0, sh - h));
			}
		}

		var design=com.jjkcraft.client.design.UiDesign.hud(element,(int)x,(int)y,(int)w,(int)h,sw,sh);
        if(design!=null){x=design.x()*sw/640f;y=design.y()*sh/360f;scale=design.hidden()?0:Math.clamp(design.w()*sw/640f/width,.5f,3f);w=width*scale;h=height*scale;x=clamp(x,0,Math.max(0,sw-w));y=clamp(y,0,Math.max(0,sh-h));}
		DRAWN.put(element, new Drawn(new HudClearance.Box(x, y, w, h), now));

		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().scale(scale, scale);
	}

	private static float clamp(float v, float min, float max) {
		return Math.max(min, Math.min(max, v));
	}
}
