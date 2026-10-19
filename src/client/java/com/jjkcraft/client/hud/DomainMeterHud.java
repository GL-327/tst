package com.jjkcraft.client.hud;

import java.util.List;
import java.util.Optional;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;

import com.jjkcraft.client.config.ClientConfig;
import com.jjkcraft.client.config.JjkClientConfig;
import com.jjkcraft.client.state.ClientDomains;
import com.jjkcraft.client.state.ClientEnergy;
import com.jjkcraft.client.state.ClientProgress;
import com.jjkcraft.client.ui.Ink;
import com.jjkcraft.client.ui.Palette;
import com.jjkcraft.core.domain.DomainMath;
import com.jjkcraft.domain.ActiveDomain;
import com.jjkcraft.domain.DomainDefinition;
import com.jjkcraft.domain.behavior.GambleBehavior;
import com.jjkcraft.domain.behavior.RepetitionBehavior;
import com.jjkcraft.domain.Domains;
import com.jjkcraft.technique.CastFailure;

/**
 * The domain meter. Your own domain: ready (with its cost), how long your CE can hold it open, a
 * clash, or burnout. Inside someone else's: its name, whether sure-hit applies to you, and whether
 * a Simple Domain shields you. Gamble domains show their last three rolls; repetition domains count
 * their recitations. Hidden when there is nothing to show.
 */
public final class DomainMeterHud {
	public static final int WIDTH = 150;
	public static final int HEIGHT = 24;

	private DomainMeterHud() {
	}

	public static void extract(GuiGraphicsExtractor g, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		JjkClientConfig.HudElement cfg = ClientConfig.get().hud.domainMeter;
		if (!cfg.visible || mc.gui.hud.isHidden() || mc.player == null || mc.level == null) {
			return;
		}

		Optional<ClientDomains.Domain> inside = ClientDomains.at(mc.player.position()).filter(d -> !d.mine() && d.phase() != ActiveDomain.Phase.CLOSING);
		Optional<ClientDomains.Domain> own = ClientDomains.own();
		Optional<DomainDefinition> known = ClientDomains.self().known().flatMap(id -> Domains.get(mc.level.registryAccess(), id));
		boolean simple = ClientDomains.self().simpleActive();
		CastFailure feedback = ClientDomains.feedback();
		if (inside.isEmpty() && own.isEmpty() && known.isEmpty() && !simple && feedback == CastFailure.NONE) {
			return;
		}

		float alpha = cfg.opacity();
		HudLayout.push(g, cfg, WIDTH, HEIGHT);
		if (inside.isPresent()) {
			drawInside(g, inside.get(), alpha);
		} else if (own.isPresent()) {
			drawOwn(g, own.get(), alpha);
		} else {
			drawReady(g, known, alpha);
		}

		int tagX = WIDTH - 4;
		if (simple) {
			Component tag = Component.translatable("hud.jjkcraft.simple_domain_short");
			tagX -= Ink.font().width(tag);
			Ink.text(g, tag, tagX, 3, 0xFF9FD8E8, alpha);
		}

		if (feedback != CastFailure.NONE) {
			Component reason = Component.translatable(feedback.translationKey());
			Ink.text(g, reason, WIDTH - 4 - Ink.font().width(reason), HEIGHT - 9, Palette.danger(), alpha);
		}

		g.pose().popMatrix();
	}

	private static void drawReady(GuiGraphicsExtractor g, Optional<DomainDefinition> known, float alpha) {
		if (known.isEmpty()) {
			Ink.panel(g, 0, 0, WIDTH, HEIGHT, Palette.muted(), alpha * 0.7f);
			return;
		}

		DomainDefinition def = known.get();
		int color = 0xFF000000 | def.visuals().color();
		Ink.panel(g, 0, 0, WIDTH, HEIGHT, color, alpha * 0.8f);
		Ink.text(g, Component.translatable(def.name()), 6, 3, color, alpha);

		long now = Minecraft.getInstance().level.getGameTime();
		long burnout = ClientDomains.self().burnoutUntil() - now;
		if (burnout > 0) {
			float left = Mth.clamp(burnout / (float) Math.max(1, def.burnout()), 0, 1);
			bar(g, left, Palette.danger(), alpha);
			Ink.text(g, Component.translatable("hud.jjkcraft.domain_burnout", String.format("%.0f", burnout / 20f)), 6, 13, Palette.danger(), alpha);
			return;
		}

		int points = ClientProgress.hasData() ? ClientProgress.get().growth().domain() : 0;
		double cost = DomainMath.castCost(def.castCost(), DomainMath.Variant.FULL, points, 1);
		boolean affordable = ClientEnergy.hasData() && ClientEnergy.predicted() >= cost;
		Ink.text(g, Component.translatable(affordable ? "hud.jjkcraft.domain_ready" : "hud.jjkcraft.domain_need", String.format("%.0f", cost)), 6, 13,
				affordable ? Palette.good() : Palette.TEXT_DIM, alpha);
	}

	private static void drawOwn(GuiGraphicsExtractor g, ClientDomains.Domain domain, float alpha) {
		DomainDefinition def = domain.definition().orElse(null);
		int color = def == null ? Palette.accent() : 0xFF000000 | def.visuals().color();
		Ink.panel(g, 0, 0, WIDTH, HEIGHT, color, alpha);
		Ink.text(g, ClientDomains.name(domain), 6, 3, color, alpha);

		if (domain.phase() == ActiveDomain.Phase.CLASHING) {
			float pulse = ClientConfig.reduceFlashing() ? 1 : 0.6f + 0.4f * Mth.sin(Util.getMillis() / 90f);
			Ink.text(g, Component.translatable("hud.jjkcraft.domain_clashing"), 6, 13, ARGB.multiplyAlpha(Palette.danger(), pulse), alpha);
			return;
		}

		if (def != null && ClientEnergy.hasData()) {
			int points = ClientProgress.hasData() ? ClientProgress.get().growth().domain() : 0;
			double upkeep = DomainMath.upkeepPerSecond(def.upkeep(), domain.variant(), points, 1);
			double seconds = upkeep <= 0 ? 99 : ClientEnergy.predicted() / upkeep;
			bar(g, Mth.clamp((float) (seconds / 20.0), 0, 1), color, alpha);
			Ink.text(g, Component.translatable("hud.jjkcraft.domain_hold", String.format("%.0f", seconds)), 6, 13, Palette.TEXT_DIM, alpha);
			extras(g, domain, def, alpha);
		}
	}

	private static void drawInside(GuiGraphicsExtractor g, ClientDomains.Domain domain, float alpha) {
		DomainDefinition def = domain.definition().orElse(null);
		int color = def == null ? Palette.danger() : 0xFF000000 | def.visuals().color();
		Ink.panel(g, 0, 0, WIDTH, HEIGHT, Palette.danger(), alpha);
		Ink.text(g, ClientDomains.name(domain), 6, 3, color, alpha);
		boolean shielded = ClientDomains.shielded();
		Component status = Component.translatable(shielded ? "hud.jjkcraft.domain_shielded"
				: def != null && def.sureHit() ? "hud.jjkcraft.domain_sure_hit" : "hud.jjkcraft.domain_inside");
		Ink.text(g, status, 6, 13, shielded ? 0xFF9FD8E8 : Palette.danger(), alpha);
		if (def != null) {
			extras(g, domain, def, alpha);
		}
	}

	/** Gamble reels or the recitation count, right-aligned on the second line. */
	private static void extras(GuiGraphicsExtractor g, ClientDomains.Domain domain, DomainDefinition def, float alpha) {
		for (var behavior : def.behaviors()) {
			if (behavior instanceof GambleBehavior gamble) {
				List<Integer> rolls = ClientDomains.rolls(domain.instance());
				int x = WIDTH - 6 - 3 * 12;
				for (int i = 2; i >= 0; i--) {
					int color = Palette.TEXT_DIM;
					String glyph = "-";
					if (i < rolls.size()) {
						GambleBehavior.Outcome outcome = gamble.outcomes().get(Math.min(rolls.get(i), gamble.outcomes().size() - 1));
						glyph = switch (outcome.kind()) {
							case DAMAGE -> "⚔";
							case HEAL -> "♥";
							case ENERGY -> "◆";
							case STUN -> "✦";
							case WHIFF -> "·";
						};
						color = outcome.kind() == GambleBehavior.Kind.WHIFF ? Palette.TEXT_DIM : 0xFF000000 | def.visuals().color();
					}

					g.fill(x, 12, x + 10, 22, ARGB.multiplyAlpha(0xFF1C1A24, alpha));
					Ink.text(g, glyph, x + 2, 13, color, alpha);
					x += 12;
				}
			} else if (behavior instanceof RepetitionBehavior repetition) {
				Component count = Component.literal(ClientDomains.repeats(domain.instance()) + " / " + repetition.repeats());
				Ink.text(g, count, WIDTH - 6 - Ink.font().width(count), 13, 0xFF000000 | def.visuals().color(), alpha);
			}
		}
	}

	private static void bar(GuiGraphicsExtractor g, float fraction, int color, float alpha) {
		int x0 = 6;
		int x1 = WIDTH - 6;
		g.fill(x0, HEIGHT - 3, x1, HEIGHT - 2, ARGB.multiplyAlpha(0xFF2A2633, alpha));
		g.fill(x0, HEIGHT - 3, x0 + (int) ((x1 - x0) * fraction), HEIGHT - 2, ARGB.multiplyAlpha(color, alpha));
	}
}
