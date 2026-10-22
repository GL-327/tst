package com.jjkcraft.client.hud;

import java.util.Locale;
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

import com.jjkcraft.JjkCraft;
import com.jjkcraft.client.JjkKeys;
import com.jjkcraft.client.config.ClientConfig;
import com.jjkcraft.client.config.JjkClientConfig;
import com.jjkcraft.client.state.ClientEnergy;
import com.jjkcraft.client.state.ClientProgress;
import com.jjkcraft.client.state.ClientTechniqueMenus;
import com.jjkcraft.client.state.ClientTechniqueStatus;
import com.jjkcraft.client.state.ClientTechniques;
import com.jjkcraft.client.ui.Ink;
import com.jjkcraft.client.ui.Palette;
import com.jjkcraft.technique.CastFailure;
import com.jjkcraft.technique.TechniqueDefinition;

/**
 * The selected power (bottom right, clear of the hotbar), a charge ring around the crosshair while
 * a move is held, and the Ten Shadows ritual tracker. Slot selection lives on the wheel.
 */
public final class TechniqueHotbarHud {
	/** Narrow enough to sit right of the hotbar at the smallest common GUI width (427). */
	public static final int WIDTH = 118;
	private static final int SLOT = 22;
	private static final int CHARGE_BOX = 64;

	private TechniqueHotbarHud() {
	}

	public static void extract(GuiGraphicsExtractor g, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.gui.hud.isHidden() || mc.gui.screen() != null || mc.player == null || !ClientProgress.hasData()
				|| ClientProgress.get().heavenlyRestricted() || ClientTechniques.known().isEmpty()) {
			return;
		}

		ritualTracker(g);
		chargeRing(g);
		badge(g);
	}

	private static void ritualTracker(GuiGraphicsExtractor g) {
		var trial = ClientTechniqueMenus.view(JjkCraft.id("ten_shadows"));
		var tracker = ClientConfig.get().hud.ritualTracker;
		if (!tracker.visible || trial == null || trial.trial().isEmpty()) {
			return;
		}

		int seconds = ClientTechniqueMenus.trialSeconds();
		int color = trial.trialDistance() > 40 || seconds < 20 ? Palette.danger() : Palette.gold();
		float alpha = tracker.opacity();
		int width = HudStyle.COLUMN_WIDTH;
		var hud = ClientConfig.get().hud;
		HudLayout.pushBelow(g, tracker, width, 38, hud.energyBar, hud.gradeBadge, hud.combatPanel, hud.cullingPanel, hud.missionTracker, hud.mulMeter);
		Ink.panel(g, 0, 0, width, 38, color, alpha);
		Ink.text(g, String.format(Locale.ROOT, "SUBJUGATION  %d:%02d", seconds / 60, seconds % 60), 5, 3, color, alpha);
		Identifier id = Identifier.tryParse(trial.trial());
		if (id != null) {
			Ink.text(g, Ink.fit(Component.translatable("entity." + id.getNamespace() + "." + id.getPath()), width - 9), 5, 14, Palette.TEXT, alpha);
		}

		String distance = String.format(Locale.ROOT, "%.0fm of 48m", trial.trialDistance());
		Ink.text(g, distance, 5, 25, trial.trialDistance() > 40 ? Palette.danger() : Palette.TEXT_DIM, alpha);
		HudStyle.gauge(g, 5, 34, width - 9, 2, seconds / 180.0, color, alpha);
		g.pose().popMatrix();
	}

	/** A ring around the crosshair fills while a move charges; a second, outer ring covers 100–200%. */
	private static void chargeRing(GuiGraphicsExtractor g) {
		var charge = ClientTechniques.charge();
		var meter = ClientConfig.get().hud.chargeMeter;
		if (charge.slot() < 0 || !meter.visible) {
			return;
		}

		float alpha = meter.opacity();
		int c = CHARGE_BOX / 2;
		int color = Palette.accent();
		HudLayout.push(g, meter, CHARGE_BOX, CHARGE_BOX);
		double first = Math.min(100, charge.percent()) / 100.0;
		Ink.ring(g, c, c, 11, 2, 1f, ARGB.multiplyAlpha(0x60101018, alpha));
		Ink.ring(g, c, c, 11, 2, (float) first, ARGB.multiplyAlpha(color, alpha));
		if (charge.maximum() > 100) {
			double second = Mth.clamp((charge.percent() - 100) / (double) (charge.maximum() - 100), 0, 1);
			int over = 0xFFE6C8FF;
			Ink.ring(g, c, c, 15, 1, 1f, ARGB.multiplyAlpha(0x40101018, alpha));
			if (second > 0) {
				float pulse = ClientConfig.reduceFlashing() ? 1 : 0.75f + 0.25f * Mth.sin(Util.getMillis() / 80f);
				Ink.ring(g, c, c, 15, 1, (float) second, ARGB.multiplyAlpha(over, alpha * pulse));
			}
		}

		// Shadowed: the readout sits over the charge's own glow.
		String percent = charge.percent() + "%";
		int percentColor = charge.percent() >= charge.maximum() ? Palette.gold() : Palette.TEXT;
		g.text(Ink.font(), percent, c - Ink.font().width(percent) / 2, c + 18, ARGB.multiplyAlpha(percentColor, alpha), true);
		String rate = String.format(Locale.ROOT, "%.1f CE/s", charge.cePerSecond());
		g.text(Ink.font(), rate, c - Ink.font().width(rate) / 2, c + 28, ARGB.multiplyAlpha(Palette.TEXT_DIM, alpha * 0.85f), true);
		g.pose().popMatrix();
	}

	private static void badge(GuiGraphicsExtractor g) {
		JjkClientConfig.HudElement cfg = ClientConfig.get().hud.techniqueHotbar;
		if (!cfg.visible) {
			return;
		}

		var state = ClientTechniques.state();
		int selected = state.selected();
		Optional<Identifier> id = state.slot(selected);
		Optional<TechniqueDefinition> definition = id.flatMap(ClientTechniques::definition);
		boolean infinity = ClientTechniques.known().contains(JjkCraft.id("infinity"));
		int height = infinity ? 41 : 30;
		float alpha = cfg.opacity();
		int accent = definition.map(d -> 0xFF000000 | d.visuals().color()).orElse(Palette.accent());

		HudLayout.push(g, cfg, WIDTH, height);
		Ink.panel(g, 0, 0, WIDTH, height, accent, alpha * 0.92f);
		CastFailure failure = ClientTechniques.recentFailure(selected);
		icon(g, 4, 4, id, definition, failure, alpha);

		int textX = 4 + SLOT + 5;
		int textW = WIDTH - textX - 4;
		String title = definition.map(d -> ClientTechniques.shortName(Component.translatable(d.name()).getString())).orElse("Empty slot");
		Ink.text(g, Ink.fit(Component.literal(title), textW), textX, 5, definition.isPresent() ? Palette.TEXT : Palette.TEXT_DIM, alpha);

		int keyLeft = HudStyle.keyCap(g, JjkKeys.CAST.getTranslatedKeyMessage(), WIDTH - 4, 15, alpha);
		if (failure != CastFailure.NONE) {
			Component reason = Ink.fit(Component.translatable(failure.translationKey()), keyLeft - textX - 3);
			Ink.text(g, reason, textX, 16, Palette.danger(), alpha * Math.max(0.35f, 1 - ClientTechniques.feedbackAge()));
		} else if (id.isPresent() && definition.isPresent()) {
			ClientTechniqueStatus.Status status = ClientTechniqueStatus.get(id.get());
			boolean ready = status.label().equals("Ready");
			// Beside the hotbar only the state fits ("Active", "Untamed"); menus show the full hint.
			String line = ready ? Math.round(ClientTechniques.cost(id.get())) + " CE" : status.label().split(" / ")[0];
			Ink.text(g, Ink.fit(Component.literal(line), keyLeft - textX - 3), textX, 16, ready ? Palette.energyHigh() : status.color(), alpha);
		}

		if (infinity) {
			int color = ClientTechniques.infinityActive() ? Palette.good() : ClientTechniques.infinityEnabled() ? Palette.gold() : Palette.TEXT_DIM;
			Ink.text(g, Ink.fit(Component.literal(ClientTechniqueStatus.infinityShort()), WIDTH - 9), 5, 30, color, alpha);
		}

		g.pose().popMatrix();
	}

	private static void icon(GuiGraphicsExtractor g, int x, int y, Optional<Identifier> id, Optional<TechniqueDefinition> def, CastFailure failure, float alpha) {
		g.fill(x, y, x + SLOT, y + SLOT, ARGB.multiplyAlpha(0xFF15131C, alpha));
		if (id.isEmpty() || def.isEmpty()) {
			g.outline(x, y, SLOT, SLOT, ARGB.multiplyAlpha(0xFF2E2A36, alpha));
			return;
		}

		int color = 0xFF000000 | def.get().visuals().color();
		g.blit(RenderPipelines.GUI_TEXTURED, def.get().icon(), x + 1, y + 1, 0, 0, SLOT - 2, SLOT - 2, SLOT - 2, SLOT - 2, ARGB.white(alpha));

		float cooldown = ClientTechniques.cooldownFraction(id.get());
		if (cooldown > 0) {
			int h = (int) Math.ceil((SLOT - 2) * cooldown);
			g.fill(x + 1, y + 1 + (SLOT - 2 - h), x + SLOT - 1, y + SLOT - 1, ARGB.multiplyAlpha(0xB0000000, alpha));
			Ink.ring(g, x + SLOT / 2, y + SLOT / 2, SLOT / 2 - 1, 1, 1 - cooldown, ARGB.multiplyAlpha(color, alpha));
		}

		boolean affordable = ClientTechniques.affordable(id.get()) || ClientEnergy.max() <= 0;
		boolean active = ClientTechniques.activeIs(id.get());
		int border = failure != CastFailure.NONE ? Palette.danger() : active ? color : Palette.gold();
		g.outline(x, y, SLOT, SLOT, ARGB.multiplyAlpha(border, alpha));
		if (active && !ClientConfig.reduceFlashing()) {
			float pulse = 0.5f + 0.5f * Mth.sin(Util.getMillis() / 120f);
			g.outline(x - 1, y - 1, SLOT + 2, SLOT + 2, ARGB.multiplyAlpha(color, alpha * pulse));
		}

		if (!affordable) {
			g.fill(x + 1, y + 1, x + SLOT - 1, y + SLOT - 1, ARGB.multiplyAlpha(0x50E0303F, alpha));
		}
	}
}
