package com.jjkcraft.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.network.chat.Component;

/** A short notification in the ink style (Black Flash, Domain Expansion, promotion, clash results). */
public final class InkToast implements Toast {
	private final Component title;
	private final Component body;
	private final int accent;
	private final long displayMs;
	private Visibility visibility = Visibility.SHOW;

	public InkToast(Component title, Component body, int accent, long displayMs) {
		this.title = title;
		this.body = body;
		this.accent = accent;
		this.displayMs = displayMs;
	}

	public static void show(Component title, Component body, int accent) {
		Minecraft.getInstance().gui.toastManager().addToast(new InkToast(title, body, accent, 3500));
	}

	@Override
	public Visibility getWantedVisibility() {
		return visibility;
	}

	@Override
	public void update(ToastManager manager, long fullyVisibleForMs) {
		visibility = fullyVisibleForMs >= displayMs ? Visibility.HIDE : Visibility.SHOW;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, Font font, long fullyVisibleForMs) {
		int w = width();
		int h = height();
		Ink.panel(g, 0, 0, w, h, accent, 1f);
		Ink.diamond(g, 12, h / 2, 5, accent);
		g.text(font, Ink.fit(title,w-30), 24, 7, Palette.TEXT, true);
		g.text(font, Ink.fit(body,w-30), 24, 18, Palette.TEXT_DIM, true);
	}
}
