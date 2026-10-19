package com.jjkcraft.core.ui;

import java.util.List;

/**
 * Keeps another mod's overlay box (Jade's look-at tooltip) off the mod's HUD elements. A box that
 * overlaps an element in the top half of the screen moves below it; one in the bottom half moves
 * above it. Only the box's top edge changes. Pure math, so it is unit tested.
 */
public final class HudClearance {
	/** Pixels left between the moved box and the element it cleared. */
	public static final float GAP = 2;

	private HudClearance() {
	}

	/** A screen rectangle in GUI pixels. */
	public record Box(float x, float y, float width, float height) {
		public float bottom() {
			return y + height;
		}

		public boolean overlaps(Box other) {
			return x < other.x + other.width && other.x < x + width && y < other.y + other.height && other.y < y + height;
		}
	}

	/** The top edge for {@code box} once it is clear of {@code elements}, kept on screen. */
	public static float clearTop(Box box, List<Box> elements, float screenHeight) {
		float y = box.y();
		// Each pass clears one element; a box pushed into a second element is moved again.
		for (int pass = 0; pass <= elements.size(); pass++) {
			Box moved = new Box(box.x(), y, box.width(), box.height());
			Box hit = null;
			for (Box element : elements) {
				if (moved.overlaps(element)) {
					hit = element;
					break;
				}
			}

			if (hit == null) {
				return y;
			}

			boolean upperHalf = hit.y() + hit.height() / 2 < screenHeight / 2;
			y = upperHalf ? hit.bottom() + GAP : hit.y() - box.height() - GAP;
			y = Math.max(0, Math.min(y, screenHeight - box.height()));
		}

		return y;
	}
}
