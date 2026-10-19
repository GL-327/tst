package com.jjkcraft.client.hud;

import java.util.Optional;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;

import com.jjkcraft.client.JjkKeys;
import com.jjkcraft.client.config.ClientConfig;
import com.jjkcraft.client.config.JjkClientConfig;
import com.jjkcraft.client.state.ClientEnergy;
import com.jjkcraft.client.state.ClientProgress;
import com.jjkcraft.client.state.ClientTechniques;
import com.jjkcraft.client.ui.Ink;
import com.jjkcraft.client.ui.Palette;
import com.jjkcraft.technique.CastFailure;
import com.jjkcraft.technique.TechniqueDefinition;
import com.jjkcraft.technique.TechniqueState;

/**
 * The technique hotbar: up to six slots stacked vertically. Each slot shows the icon, a cooldown
 * ring and dark sweep, a red "!" when CE is too low, the active glow for channels and toggles,
 * and a brief shake with a reason when the server refuses a cast.
 */
public final class TechniqueHotbarHud {
	private static final int SLOT = 22;
	private static final int GAP = 2;

	private TechniqueHotbarHud() {
	}

	public static void extract(GuiGraphicsExtractor g, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		JjkClientConfig.HudElement cfg = ClientConfig.get().hud.techniqueHotbar;
		if (!cfg.visible || mc.gui.hud.isHidden() || mc.player == null || !ClientProgress.hasData()
				|| ClientProgress.get().heavenlyRestricted() || ClientTechniques.known().isEmpty()) {
			return;
		}

		int slots = Math.min(TechniqueState.SLOTS, ClientTechniques.usableSlots());
		int height = slots * SLOT + (slots - 1) * GAP + 12;
		int width = SLOT + 8;
		float alpha = cfg.opacity();
		HudLayout.push(g, cfg, width, height);
		Ink.panel(g, 0, 0, width, height, Palette.accent(), alpha * 0.85f);

		TechniqueState state = ClientTechniques.state();
		for (int i = 0; i < slots; i++) {
			int x = 4;
			int y = 4 + i * (SLOT + GAP);
			CastFailure failure = ClientTechniques.recentFailure(i);
			if (failure != CastFailure.NONE && !ClientConfig.reducedMotion()) {
				x += (int) (Mth.sin(Util.getMillis() / 25f) * 2 * (1 - ClientTechniques.feedbackAge()));
			}

			slot(g, x, y, i, state.slot(i), i == state.selected(), failure, alpha);
		}

		String key = JjkKeys.CAST.getTranslatedKeyMessage().getString();
		g.pose().pushMatrix();
		g.pose().translate(4, height - 8);
		g.pose().scale(0.5f, 0.5f);
		Ink.text(g, Component.translatable("hud.jjkcraft.cast_hint", key), 0, 0, Palette.TEXT_DIM, alpha);
		g.pose().popMatrix();

		g.pose().popMatrix();
	}

	private static void slot(GuiGraphicsExtractor g, int x, int y, int index, Optional<Identifier> id, boolean selected, CastFailure failure, float alpha) {
		g.fill(x, y, x + SLOT, y + SLOT, ARGB.multiplyAlpha(0xFF15131C, alpha));
		if (id.isEmpty()) {
			g.outline(x, y, SLOT, SLOT, ARGB.multiplyAlpha(0xFF2E2A36, alpha));
			return;
		}

		Optional<TechniqueDefinition> def = ClientTechniques.definition(id.get());
		if (def.isEmpty()) {
			return;
		}

		g.blit(RenderPipelines.GUI_TEXTURED, def.get().icon(), x + 1, y + 1, 0, 0, SLOT - 2, SLOT - 2, SLOT - 2, SLOT - 2,
				ARGB.white(alpha));

		float cooldown = ClientTechniques.cooldownFraction(id.get());
		if (cooldown > 0) {
			int h = (int) Math.ceil((SLOT - 2) * cooldown);
			g.fill(x + 1, y + 1 + (SLOT - 2 - h), x + SLOT - 1, y + SLOT - 1, ARGB.multiplyAlpha(0xB0000000, alpha));
			Ink.ring(g, x + SLOT / 2, y + SLOT / 2, SLOT / 2 - 1, 1, 1 - cooldown, ARGB.multiplyAlpha(def.get().visuals().color(), alpha));
		}

		boolean affordable = ClientTechniques.affordable(id.get()) || ClientEnergy.max() <= 0;
		boolean active = ClientTechniques.activeIs(id.get());
		int border = failure != CastFailure.NONE ? Palette.danger()
				: active ? def.get().visuals().color()
				: selected ? Palette.gold() : Palette.INK_EDGE;
		g.outline(x, y, SLOT, SLOT, ARGB.multiplyAlpha(border, alpha));
		if (active && !ClientConfig.reduceFlashing()) {
			float pulse = 0.5f + 0.5f * Mth.sin(Util.getMillis() / 120f);
			g.outline(x - 1, y - 1, SLOT + 2, SLOT + 2, ARGB.multiplyAlpha(def.get().visuals().color(), alpha * pulse));
		}

		if (!affordable) {
			g.fill(x + 1, y + 1, x + SLOT - 1, y + SLOT - 1, ARGB.multiplyAlpha(0x50E0303F, alpha));
			Ink.text(g, "!", x + SLOT - 6, y + 2, Palette.danger(), alpha);
		}

		g.pose().pushMatrix();
		g.pose().translate(x + 2, y + SLOT - 6);
		g.pose().scale(0.5f, 0.5f);
		Ink.text(g, String.valueOf(index + 1), 0, 0, selected ? Palette.gold() : Palette.TEXT_DIM, alpha);
		g.pose().popMatrix();

		if (failure != CastFailure.NONE && selected) {
			g.pose().pushMatrix();
			g.pose().translate(-Ink.font().width(Component.translatable(failure.translationKey())) * 0.6f - 4, y + 7);
			g.pose().scale(0.6f, 0.6f);
			Ink.text(g, Component.translatable(failure.translationKey()), 0, 0, Palette.danger(), alpha * (1 - ClientTechniques.feedbackAge()));
			g.pose().popMatrix();
		}
	}
}
