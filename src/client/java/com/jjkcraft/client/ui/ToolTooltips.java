package com.jjkcraft.client.ui;

import net.minecraft.ChatFormatting;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;

import com.jjkcraft.client.state.ClientProgress;
import com.jjkcraft.grade.Grades;
import com.jjkcraft.item.JjkComponents;
import com.jjkcraft.item.TechniqueScrollItem;
import com.jjkcraft.item.tool.CursedTools;
import com.jjkcraft.technique.Techniques;

/** Colours cursed tool and scroll tooltips by whether the player's grade can use them. */
public final class ToolTooltips {
	private ToolTooltips() {
	}

	public static void init() {
		ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
			RegistryAccess access = ClientProgress.registries().orElse(null);
			if (access == null || !ClientProgress.hasData()) {
				return;
			}

			int rank = Grades.rank(access, ClientProgress.get().grade());
			CursedTools.get(access, stack.getItem()).ifPresent(tool -> {
				boolean ok = rank >= Grades.rank(access, tool.grade());
				lines.add(Component.translatable(ok ? "tooltip.jjkcraft.tool.usable" : "tooltip.jjkcraft.tool.unusable")
						.withStyle(ok ? ChatFormatting.GREEN : ChatFormatting.RED));
			});

			Identifier technique = stack.getItem() instanceof TechniqueScrollItem ? stack.get(JjkComponents.LOADED_TECHNIQUE) : null;
			if (technique != null) {
				Techniques.get(access, technique).ifPresent(def -> {
					boolean ok = rank >= Grades.rank(access, def.grade());
					lines.add(Component.translatable(ok ? "tooltip.jjkcraft.scroll.learnable" : "tooltip.jjkcraft.scroll.too_low")
							.withStyle(ok ? ChatFormatting.GREEN : ChatFormatting.RED));
				});
			}
		});
	}
}
