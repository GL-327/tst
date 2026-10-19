package com.jjkcraft.client.modulo;

import java.util.List;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import com.jjkcraft.client.ui.Ink;
import com.jjkcraft.client.ui.InkButton;
import com.jjkcraft.client.ui.Palette;
import com.jjkcraft.modulo.ModuloPayloads;

/** A mission choice (mission 9: duel Maru yourself, or stand aside): the question and one button per answer. */
public class ModuloChoiceScreen extends Screen {
	private static final int W = 260;

	private final ModuloPayloads.Dialogue question;

	public ModuloChoiceScreen(ModuloPayloads.Dialogue question) {
		super(Component.translatable("npc.jjkcraft.modulo." + question.speaker()));
		this.question = question;
	}

	@Override
	protected void init() {
		int x = (width - W) / 2 + 10;
		int y = height / 2;
		for (int i = 0; i < question.options().size(); i++) {
			int option = i;
			addRenderableWidget(new InkButton(x, y + i * 22, W - 20, 18, Component.translatable(question.options().get(i)), b -> {
				ClientPlayNetworking.send(new ModuloPayloads.Choose(option));
				onClose();
			}).accent(ClientModulo.ACCENT));
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
		int x = (width - W) / 2;
		List<FormattedCharSequence> lines = Ink.font().split(Component.translatable(question.text()), W - 20);
		int top = height / 2 - 26 - lines.size() * 10;
		Ink.panel(g, x, top, W, height / 2 - top + question.options().size() * 22 + 8, ClientModulo.ACCENT, 0.95f);
		Ink.text(g, getTitle(), x + 10, top + 6, ClientModulo.ACCENT, 1f);
		int y = top + 18;
		for (FormattedCharSequence line : lines) {
			g.text(Ink.font(), line, x + 10, y, Palette.TEXT, true);
			y += 10;
		}

		super.extractRenderState(g, mouseX, mouseY, a);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
