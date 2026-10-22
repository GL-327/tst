package com.jjkcraft.client.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
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
        if(com.jjkcraft.technique.TechniqueBindings.family(id)){
            lines.add(Component.literal("Family selector · "+ClientTechniques.variants(id).size()+" learned variants").withColor(Palette.gold()));
            lines.add(Component.literal("Activate to open the variant wheel.").withColor(Palette.TEXT));lines.add(Component.literal("Bind a specific move for direct casting.").withColor(Palette.TEXT));return lines;
        }
		lines.add(Component.translatable(def.category().translationKey()).withColor(def.category().color()));
        if(id.equals(com.jjkcraft.JjkCraft.id("infinity")))lines.add(Component.literal(com.jjkcraft.client.state.ClientTechniqueStatus.infinity()).withColor(Palette.gold()));

		double cost = ClientTechniques.cost(id);
		boolean affordable = ClientTechniques.affordable(id);
		lines.add(Component.translatable("tooltip.jjkcraft.cost", fmt(cost)).withColor(affordable ? Palette.good() : Palette.danger()));
		if (def.cast().mode() != CastMode.INSTANT && def.cast().upkeep() > 0) {
			lines.add(Component.translatable("tooltip.jjkcraft.upkeep", fmt(id.equals(com.jjkcraft.JjkCraft.id("infinity"))?ClientTechniques.infinityUpkeep():def.cast().upkeep())).withColor(Palette.TEXT_DIM));
		}

		lines.add(Component.translatable("tooltip.jjkcraft.cooldown", fmt(ClientTechniques.cooldown(id) / 20.0)).withColor(Palette.TEXT_DIM));
		if (def.cast().castTime() > 0) {
			lines.add(Component.translatable("tooltip.jjkcraft.cast_time", fmt(def.cast().castTime() / 20.0)).withColor(Palette.TEXT_DIM));
		}

		boolean gradeOk = ClientTechniques.gradeAllows(def);
		Component grade = ClientProgress.registries().flatMap(r -> Grades.get(r, def.grade()))
				.map(g -> (Component) Component.translatable(g.name())).orElse(Component.literal(def.grade().toString()));
		lines.add(Component.translatable("tooltip.jjkcraft.grade", grade).withColor(gradeOk ? Palette.good() : Palette.danger()));
		lines.add(Component.translatable("tooltip.jjkcraft.mode." + def.cast().mode().getSerializedName()).withColor(Palette.TEXT_DIM));
        lines.add(activation(def).withColor(def.cast().chargeable()?Palette.gold():Palette.energyHigh()));
		int wrap=Math.max(110,Math.min(250,net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScaledWidth()-28));
		for(var line:Ink.font().split(Component.translatable(def.description()),wrap)){
			var text=new StringBuilder();line.accept((index,style,point)->{text.appendCodePoint(point);return true;});
			lines.add(Component.literal(text.toString()).withColor(Palette.TEXT));
		}

		int mastery = ClientTechniques.state().masteryOf(id);
        double training=ClientTechniques.mastery(id);
        lines.add(Component.literal(String.format(Locale.ROOT,"Training %.0f%% · power ×%.2f",training*100,com.jjkcraft.core.technique.TechniqueMath.masteryPower(training))).withColor(Palette.energyHigh()));
        lines.add(Component.literal(String.format(Locale.ROOT,"Mastery: −%.0f%% CE and cooldown",(1-com.jjkcraft.core.technique.TechniqueMath.masteryCost(training))*100)).withColor(Palette.TEXT_DIM));
		lines.add(mastery >= def.masteryUses()
				? Component.translatable("tooltip.jjkcraft.mastered").withColor(Palette.gold())
				: Component.translatable("tooltip.jjkcraft.mastery", mastery, def.masteryUses()).withColor(Palette.TEXT_DIM));
		return lines;
	}

	private static String fmt(double v) {
		return v == Math.floor(v) ? String.valueOf((long) v) : String.format(Locale.ROOT, "%.1f", v);
	}
    public static MutableComponent activation(TechniqueDefinition def){
        if(def.name().startsWith("family."))return Component.literal("Activate to open learned variants");
        return Component.translatable(def.cast().chargeable()?"tooltip.jjkcraft.activation.charge":def.cast().mode()==CastMode.CHANNEL?"tooltip.jjkcraft.activation.channel":def.cast().mode()==CastMode.TOGGLE?"tooltip.jjkcraft.activation.toggle":"tooltip.jjkcraft.activation.press");
    }
}
