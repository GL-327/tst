package com.jjkcraft.client.ui;

import java.util.function.Consumer;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;

/**
 * A button in the shared ink style. Selected buttons (active tabs) keep the accent underline.
 * It is a vanilla {@link Button} underneath, so anything that looks for buttons (narration,
 * client GameTests clicking by label) treats it like one.
 */
public class InkButton extends Button {
	private int accent;
	private boolean selected;

	public InkButton(int x, int y, int width, int height, Component message, Consumer<InkButton> onPress) {
		super(x, y, width, height, message, button -> onPress.accept((InkButton) button), DEFAULT_NARRATION);
		this.accent = Palette.accent();
	}

	public InkButton accent(int color) {
		this.accent = color;
		return this;
	}

	public InkButton selected(boolean value) {
		this.selected = value;
		return this;
	}

	@Override
	protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
		boolean hot = isHoveredOrFocused() && active;
		int x = getX();
		int y = getY();
		int w = getWidth();
		int h = getHeight();
		g.fill(x, y, x + w, y + h, active ? (hot || selected ? 0xFF26334A : 0xF0172030) : 0xD0101520);
		g.outline(x, y, w, h, hot || selected ? accent : Palette.INK_EDGE);
		if (selected || hot) {
			g.fill(x,y,x+2,y+h,accent);
			g.fill(x + 2, y + h - 2, x + w - 2, y + h - 1, ARGB.multiplyAlpha(accent, selected ? 1f : 0.6f));
		}

		int color = !active ? Palette.muted() : hot || selected ? Palette.TEXT : Palette.TEXT_DIM;
		g.centeredText(Ink.font(), Ink.fit(getMessage(),Math.max(1,w-6)), x + w / 2, y + (h - 8) / 2, color);
	}
}
