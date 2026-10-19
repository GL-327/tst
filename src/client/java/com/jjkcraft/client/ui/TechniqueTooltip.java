package com.jjkcraft.client.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import com.jjkcraft.client.state.ClientProgress;
import com.jjkcraft.client.state.ClientTechniques;
import com.jjkcraft.grade.Grades;
import com.jjkcraft.technique.CastMode;
import com.jjkcraft.technique.TechniqueDefinition;

/**
 * Shared technique tooltip: name, category, CE cost, cooldown and grade requirement, colored by
 * whether the player can use it right now (green ok, red not), plus the description and mastery.
 */
public final class TechniqueTooltip {
	private TechniqueTooltip() {
	}

	public static List<Component> lines(Identifier id, TechniqueDefinition def) {
		List<Component> lines = new ArrayList<>();
		lines.add(Component.translatable(def.name()).withColor(def.visuals().color()));
		lines.add(Component.translatable(def.category().translationKey()).withColor(def.category().color()));

		double cost = ClientTechniques.cost(id);
		boolean affordable = ClientTechniques.affordable(id);
		lines.add(Component.translatable("tooltip.jjkcraft.cost", fmt(cost)).withColor(affordable ? Palette.good() : Palette.danger()));
		if (def.cast().mode() != CastMode.INSTANT && def.cast().upkeep() > 0) {
			lines.add(Component.translatable("tooltip.jjkcraft.upkeep", fmt(def.cast().upkeep())).withColor(Palette.TEXT_DIM));
		}

		lines.add(Component.translatable("tooltip.jjkcraft.cooldown", fmt(def.cast().cooldown() / 20.0)).withColor(Palette.TEXT_DIM));
		if (def.cast().castTime() > 0) {
			lines.add(Component.translatable("tooltip.jjkcraft.cast_time", fmt(def.cast().castTime() / 20.0)).withColor(Palette.TEXT_DIM));
		}

		boolean gradeOk = ClientTechniques.gradeAllows(def);
		Component grade = ClientProgress.registries().flatMap(r -> Grades.get(r, def.grade()))
				.map(g -> (Component) Component.translatable(g.name())).orElse(Component.literal(def.grade().toString()));
		lines.add(Component.translatable("tooltip.jjkcraft.grade", grade).withColor(gradeOk ? Palette.good() : Palette.danger()));
		lines.add(Component.translatable("tooltip.jjkcraft.mode." + def.cast().mode().getSerializedName()).withColor(Palette.TEXT_DIM));
		lines.add(Component.translatable(def.description()).withColor(Palette.TEXT));

		int mastery = ClientTechniques.state().masteryOf(id);
		lines.add(mastery >= def.masteryUses()
				? Component.translatable("tooltip.jjkcraft.mastered").withColor(Palette.gold())
				: Component.translatable("tooltip.jjkcraft.mastery", mastery, def.masteryUses()).withColor(Palette.TEXT_DIM));
		return lines;
	}

	private static String fmt(double v) {
		return v == Math.floor(v) ? String.valueOf((long) v) : String.format(Locale.ROOT, "%.1f", v);
	}
}
