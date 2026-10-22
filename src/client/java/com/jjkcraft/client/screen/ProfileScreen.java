package com.jjkcraft.client.screen;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import com.jjkcraft.client.state.ClientEnergy;
import com.jjkcraft.client.state.ClientProgress;
import com.jjkcraft.client.ui.Ink;
import com.jjkcraft.client.ui.Palette;
import com.jjkcraft.core.progress.ProgressionMath;
import com.jjkcraft.grade.GradeDefinition;
import com.jjkcraft.grade.Grades;
import com.jjkcraft.sorcerer.Progression;
import com.jjkcraft.sorcerer.SorcererProgress;

/**
 * Sorcerer Profile: model, grade, status, stats, next-grade requirements and short lists
 * (techniques, vows, completed trials). Systems add list sections through {@link #addSection}.
 */
public final class ProfileScreen extends JjkScreen {
	/** A short titled list in the bottom strip. */
	public record Section(Component title, Supplier<List<Component>> lines) {
	}

	private static final List<Section> SECTIONS = new ArrayList<>();

	public ProfileScreen() {
		super(Component.translatable("screen.jjkcraft.profile"));
	}

	public static void addSection(Section section) {
		SECTIONS.add(section);
	}

	@Override
	protected void initContent() {
	}

	@Override
	protected void extractContent(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
		SorcererProgress p = ClientProgress.get();
		int x = panelX();
		int y = contentY();

		// Left: model, name, grade, status.
		int modelX0 = x + 8;
		int modelY0 = y;
		int modelX1 = x + 88;
		int modelY1 = y + 96;
		g.fill(modelX0, modelY0, modelX1, modelY1, 0x60000000);
		if (minecraft.player != null) {
			InventoryScreen.extractEntityInInventoryFollowsMouse(g, modelX0, modelY0, modelX1, modelY1, 38, 0.0625f, mouseX, mouseY, minecraft.player);
			Ink.text(g, minecraft.player.getName(), modelX0, modelY1 + 4, Palette.TEXT, 1f);
		}

		GradeDefinition grade = ClientProgress.grade().orElse(null);
		int gradeColor = grade != null ? grade.color() : Palette.TEXT_DIM;
		Ink.diamond(g, modelX0 + 4, modelY1 + 19, 4, gradeColor);
		Ink.text(g, grade != null ? Component.translatable(grade.name()) : Component.literal(p.grade().toString()), modelX0 + 11, modelY1 + 15, gradeColor, 1f);
		Component status = p.heavenlyRestricted() ? Component.translatable("screen.jjkcraft.profile.heavenly")
				: Component.translatable(p.awakened() ? "screen.jjkcraft.profile.awakened" : "screen.jjkcraft.profile.dormant");
		Ink.text(g, status, modelX0, modelY1 + 27, p.heavenlyRestricted() ? Palette.gold() : Palette.TEXT_DIM, 1f);

		// Middle: stats.
		int sx = x + 98;
		int sw = Math.max(120, panelW() / 3 - 10);
		Ink.text(g, Component.translatable("screen.jjkcraft.profile.stats"), sx, y, Palette.gold(), 1f);
		double max = ClientEnergy.max();
		double regen = ClientEnergy.regenerationPerSecond();
		int line = y + 12;
		line = stat(g, sx, line, "screen.jjkcraft.profile.capacity", ClientEnergy.uncapped()?"∞":fmt(max), regen > 0 ? "+" + fmt(regen) + "/s" : "-");
		// Output is measured as CE spent and Control as the share of casts that landed.
		line = stat(g, sx, line, "growth.jjkcraft.output", fmt(p.ceSpentTotal()) + " CE spent", points(p.growth().output()));
		line = stat(g, sx, line, "growth.jjkcraft.control",
				p.castsAttempted() == 0 ? "no casts yet" : Math.round(p.controlRatio() * 100) + "% landed", points(p.growth().control()));
		line = stat(g, sx, line, "growth.jjkcraft.efficiency",
				p.growth().efficiency() == 0 ? "-" : "-" + fmt(100 - ProgressionMath.cooldownMultiplier(p.growth().efficiency()) * 100) + "% cd",
				points(p.growth().efficiency()));
		line = stat(g, sx, line, "growth.jjkcraft.domain",
				Component.translatable("screen.jjkcraft.profile.domain_tier", grade != null ? grade.domainTier() : 0).getString(), points(p.growth().domain()));
		Ink.text(g, Component.translatable("screen.jjkcraft.profile.unspent", ClientProgress.unspentPoints()), sx, line + 2, Palette.gold(), 1f);

		// Right: next grade.
		int nx = sx + sw + 8;
		int nw = x + panelW() - nx - 8;
		ClientProgress.nextGrade().ifPresentOrElse(next -> nextGrade(g, nx, y, nw, p, next),
				() -> Ink.text(g, Component.translatable("screen.jjkcraft.profile.top_grade"), nx, y, Palette.gold(), 1f));

		// Bottom strip: sections.
		int stripY = modelY1 + 42;
		int stripH = panelY() + panelH() - stripY - 6;
		List<Section> sections = new ArrayList<>(SECTIONS);
		sections.add(new Section(Component.translatable("screen.jjkcraft.profile.trials"),
				() -> p.trials().stream().sorted().map(t -> (Component) Component.literal(t.substring(t.indexOf(':') + 1).replace('_', ' '))).toList()));
		int colW = (panelW() - 16) / sections.size();
		for (int i = 0; i < sections.size(); i++) {
			Section section = sections.get(i);
			int cx = x + 8 + i * colW;
			g.fill(cx, stripY, cx + colW - 6, stripY + stripH, 0x50000000);
			// Titles shrink a little before they would be cut; entries wrap rather than truncate.
			float titleScale = Math.clamp((colW - 12) / (float) Math.max(1, Ink.font().width(section.title())), 0.8f, 1f);
			g.pose().pushMatrix();
			g.pose().translate(cx + 3, stripY + 3);
			g.pose().scale(titleScale, titleScale);
			Ink.text(g, Ink.fit(section.title(), (int) ((colW - 12) / titleScale)), 0, 0, Palette.gold(), 1f);
			g.pose().popMatrix();
			List<Component> lines = new ArrayList<>();
			for (Component entry : section.lines().get()) {
				lines.addAll(Ink.wrap(entry, (int) ((colW - 12) / 0.85f)));
			}

			int maxLines = Math.max(0, (stripH - 16) / 10);
			for (int l = 0; l < Math.min(lines.size(), maxLines); l++) {
				Component text = lines.get(l);
				if (l == maxLines - 1 && lines.size() > maxLines) {
					text = Component.translatable("screen.jjkcraft.more", lines.size() - l);
				}

				g.pose().pushMatrix();
				g.pose().translate(cx + 3, stripY + 15 + l * 10);
				g.pose().scale(0.85f, 0.85f);
				Ink.text(g, Ink.fit(text, (int) ((colW - 12) / 0.85f)), 0, 0, Palette.TEXT, 1f);
				g.pose().popMatrix();
			}

			if (lines.isEmpty()) {
				Ink.text(g, Component.translatable("screen.jjkcraft.none"), cx + 3, stripY + 15, Palette.muted(), 1f);
			}
		}
	}

	private void nextGrade(GuiGraphicsExtractor g, int x, int y, int w, SorcererProgress p, Grades.Entry next) {
		GradeDefinition def = next.definition();
		Ink.text(g, Component.translatable("screen.jjkcraft.profile.next", Component.translatable(def.name())), x, y, def.color(), 1f);
		ProgressionMath.Requirements req = Progression.requirements(minecraft.level.registryAccess(), def);
		int line = y + 12;
		if (req.xpRequired() > 0) {
			bar(g, x, line + 1, w, (double) p.gradeXp() / req.xpRequired(), def.color());
			line = check(g, x, line + 6, w, p.gradeXp() >= req.xpRequired(),
					Component.translatable("screen.jjkcraft.req.xp", p.gradeXp(), req.xpRequired()));
		}

		if (!p.heavenlyRestricted() && req.minControl() > 0) {
			boolean ok = p.castsAttempted() >= 10 && p.controlRatio() >= req.minControl();
			line = check(g, x, line, w, ok, Component.translatable("screen.jjkcraft.req.control", Math.round(req.minControl() * 100)));
		}

		if (!p.heavenlyRestricted() && req.minOutput() > 0) {
			line = check(g, x, line, w, p.ceSpentTotal() >= req.minOutput(),
					Component.translatable("screen.jjkcraft.req.output", fmt(p.ceSpentTotal()), fmt(req.minOutput())));
		}

		for (ProgressionMath.TrialRequirement trial : req.trials()) {
			int progress = ProgressionMath.trialProgress(trial.kind(), trial.minRank(), p.counters());
			boolean done = p.trials().contains(trial.id()) || progress >= trial.count();
			Component label = Component.translatable("trial.jjkcraft." + trial.kind().name().toLowerCase(Locale.ROOT),
					Mth.clamp(progress, 0, trial.count()), trial.count(), rankName(trial.minRank()));
			line = check(g, x, line, w, done, label);
		}
	}

	private Component rankName(int rank) {
		return Grades.ladder(minecraft.level.registryAccess()).stream()
				.filter(e -> e.definition().rank() == rank).findFirst()
				.map(e -> (Component) Component.translatable(e.definition().name()))
				.orElse(Component.literal(String.valueOf(rank)));
	}

	private static int stat(GuiGraphicsExtractor g, int x, int y, String key, String value, String extra) {
		Ink.text(g, Component.translatable(key), x, y, Palette.TEXT_DIM, 1f);
		Ink.text(g, value, x + 58, y, Palette.TEXT, 1f);
		Ink.text(g, extra, x + 58 + Ink.font().width(value) + 4, y, Palette.energyHigh(), 1f);
		return y + 11;
	}

	/** A requirement row; the label shrinks to fit {@code width}. */
	private static int check(GuiGraphicsExtractor g, int x, int y, int width, boolean ok, Component label) {
		int color = ok ? Palette.good() : Palette.muted();
		g.fill(x, y + 2, x + 5, y + 7, color);
		if (ok) {
			g.fill(x + 1, y + 3, x + 4, y + 6, 0xFF0B0B10);
		}

		float scale = Math.min(0.85f, (width - 10) / (float) Math.max(1, Ink.font().width(label)));
		g.pose().pushMatrix();
		g.pose().translate(x + 8, y + 1);
		g.pose().scale(scale, scale);
		Ink.text(g, label, 0, 0, ok ? Palette.TEXT : Palette.TEXT_DIM, 1f);
		g.pose().popMatrix();
		return y + 10;
	}

	private static String points(int points) {
		return points > 0 ? "(" + points + ")" : "";
	}

	private static String fmt(double v) {
		return v >= 100 ? String.valueOf(Math.round(v)) : String.format(Locale.ROOT, "%.1f", v);
	}
}
