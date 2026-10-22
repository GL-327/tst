package com.jjkcraft.client.hud;

import java.util.Locale;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import com.jjkcraft.client.combat.CombatClient;
import com.jjkcraft.client.combat.PunchImbuementClient;
import com.jjkcraft.client.config.ClientConfig;
import com.jjkcraft.client.config.JjkClientConfig;
import com.jjkcraft.client.ui.Ink;
import com.jjkcraft.client.ui.Palette;
import com.jjkcraft.network.payload.CombatPayloads;

/**
 * Cursed Combat state under the grade badge while combat mode is on: the current move or mode,
 * evade readiness, stamina and guard. Key hints are not repeated here; they are in Controls and
 * shown once when combat mode is switched on.
 */
public final class CombatPanelHud {
	public static final int WIDTH = HudStyle.COLUMN_WIDTH;
	private static final int ROW = 11;
	private static final int GAUGE_X = 46;

	private CombatPanelHud() {
	}

	public static int height() {
		return PunchImbuementClient.label().isEmpty() ? 3 + ROW * 3 : 3 + ROW * 4;
	}

	public static void extract(GuiGraphicsExtractor g, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		JjkClientConfig.HudElement cfg = ClientConfig.get().hud.combatPanel;
		if (!cfg.visible || !CombatClient.enabled() || mc.player == null || mc.gui.hud.isHidden() || mc.gui.screen() != null) {
			return;
		}

		CombatPayloads.View view = CombatClient.view();
		CombatPayloads.Endurance endurance = CombatClient.endurance();
		int escape = CombatClient.resources().escapeCooldown();
		boolean staggered = view.stun() > 0;
		boolean auto = ClientConfig.get().autoCombat;
		float alpha = cfg.opacity();
		int height = height();

		var hud = ClientConfig.get().hud;
		HudLayout.pushBelow(g, cfg, WIDTH, height, hud.energyBar, hud.gradeBadge);
		Ink.panel(g, 0, 0, WIDTH, height, staggered ? Palette.danger() : Palette.accent(), alpha);

		String status;
		int statusColor;
		if (staggered) {
			status = "STAGGERED";
			statusColor = Palette.danger();
		} else if (!view.move().isEmpty()) {
			status = view.move().replace('_', ' ').toUpperCase(Locale.ROOT) + (view.combo() > 1 ? " ×" + view.combo() : "");
			statusColor = view.combo() > 1 ? Palette.gold() : Palette.TEXT;
		} else {
			var stats = CombatClient.autoStats();
			status = !auto ? "MANUAL" : stats.attempts() < 5 ? "AUTO" : "AUTO · " + 100 * stats.hits() / Math.max(1, stats.attempts()) + "% contact";
			statusColor = Palette.TEXT;
		}

		String evade = escape == 0 ? "Evade" : String.format(Locale.ROOT, "%.1fs", escape / 20.0);
		int evadeX = WIDTH - 4 - Ink.font().width(evade);
		Ink.text(g, Ink.fit(Component.literal(status), evadeX - 9), 5, 3, statusColor, alpha);
		Ink.text(g, evade, evadeX, 3, escape == 0 ? Palette.good() : Palette.TEXT_DIM, alpha);

		double stamina = endurance.stamina() / Math.max(1, endurance.maximum());
		Ink.text(g, "Stamina", 5, 3 + ROW, Palette.TEXT_DIM, alpha);
		HudStyle.gauge(g, GAUGE_X, 5 + ROW, WIDTH - GAUGE_X - 4, 4, stamina, stamina < 0.24 ? Palette.danger() : Palette.gold(), alpha);
		Ink.text(g, "Guard", 5, 3 + ROW * 2, Palette.TEXT_DIM, alpha);
		HudStyle.gauge(g, GAUGE_X, 5 + ROW * 2, WIDTH - GAUGE_X - 4, 4, view.guard() / 100.0, view.guard() < 30 ? Palette.danger() : Palette.energyHigh(), alpha);

		String imbuement = PunchImbuementClient.label();
		if (!imbuement.isEmpty()) {
			Ink.text(g, Ink.fit(Component.literal(imbuement), WIDTH - 9), 5, 3 + ROW * 3, PunchImbuementClient.mode() == 2 ? 0xFFFF6A7A : Palette.energyHigh(), alpha);
		}

		g.pose().popMatrix();
	}
}
