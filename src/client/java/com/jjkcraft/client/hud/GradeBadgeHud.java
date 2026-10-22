package com.jjkcraft.client.hud;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;

import com.jjkcraft.client.config.ClientConfig;
import com.jjkcraft.client.config.JjkClientConfig;
import com.jjkcraft.client.state.ClientProgress;
import com.jjkcraft.client.ui.Ink;
import com.jjkcraft.client.ui.Palette;
import com.jjkcraft.grade.GradeDefinition;
import com.jjkcraft.sorcerer.SorcererProgress;

/**
 * Grade badge under the CE gauge: a colored diamond, the grade name, a thin line of progress
 * toward the next grade, and status tags (Heavenly Restriction, and indicators other systems add
 * such as Black Flash).
 */
public final class GradeBadgeHud {
	public static final int WIDTH = HudStyle.COLUMN_WIDTH;
	public static final int HEIGHT = 16;

	/** A small tag shown on the badge while its condition holds. */
	public record Indicator(Component label, int color, BooleanSupplier active) {
	}

	private static final List<Indicator> INDICATORS = new ArrayList<>();

	private GradeBadgeHud() {
	}

	public static void addIndicator(Indicator indicator) {
		INDICATORS.add(indicator);
	}

	public static void extract(GuiGraphicsExtractor g, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		JjkClientConfig.HudElement cfg = ClientConfig.get().hud.gradeBadge;
		if (!cfg.visible || mc.gui.hud.isHidden() || mc.player == null || !ClientProgress.hasData()) {
			return;
		}

		SorcererProgress p = ClientProgress.get();
		GradeDefinition grade = ClientProgress.grade().orElse(null);
		if (grade == null) {
			return;
		}

		List<Indicator> tags = new ArrayList<>();
		if (p.heavenlyRestricted()) {
			tags.add(new Indicator(Component.translatable("hud.jjkcraft.heavenly_short"), Palette.gold(), () -> true));
		}

		for (Indicator indicator : INDICATORS) {
			if (indicator.active().getAsBoolean()) {
				tags.add(indicator);
			}
		}

		// Tags sit right-aligned beside the grade name; when they would run into it, they move to a
		// strip under the badge instead.
		int tagsWidth = 0;
		for (Indicator tag : tags) {
			tagsWidth += Ink.font().width(tag.label()) + 4;
		}

		Component name = Ink.fit(Component.translatable(grade.name()), WIDTH - 22);
		int nameEnd = 17 + Ink.font().width(name) + 4;
		boolean strip = !tags.isEmpty() && WIDTH - 4 - tagsWidth < nameEnd;
		float alpha = cfg.opacity();
		HudLayout.push(g, cfg, WIDTH, strip ? HEIGHT + 13 : HEIGHT);
		Ink.panel(g, 0, 0, WIDTH, HEIGHT, grade.color(), alpha);
		Ink.diamond(g, 9, 7, 4, ARGB.multiplyAlpha(grade.color(), alpha));
		Ink.diamond(g, 9, 7, 1, ARGB.multiplyAlpha(0xFF0B0B10, alpha));
		Ink.text(g, name, 17, 3, grade.color(), alpha);

		ClientProgress.nextGrade().ifPresent(next -> {
			int required = next.definition().xpRequired();
			if (required > 0) {
				double fraction = Math.min(1, (double) p.gradeXp() / required);
				HudStyle.gauge(g, 17, HEIGHT - 3, WIDTH - 21, 1, fraction, next.definition().color(), alpha);
			}
		});

		int tagX = WIDTH - 4;
		int tagY = 3;
		if (strip) {
			tagY = HEIGHT + 3;
			tagX = Math.min(WIDTH, tagsWidth + 4);
			Ink.panel(g, 0, HEIGHT + 1, tagX, 12, 0xFF2A2633, alpha);
			tagX -= 4;
		}

		for (Indicator tag : tags) {
			tagX -= Ink.font().width(tag.label());
			Ink.text(g, tag.label(), tagX, tagY, tag.color(), alpha);
			tagX -= 4;
		}

		g.pose().popMatrix();
	}
}
