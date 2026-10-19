package com.jjkcraft.item;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import com.jjkcraft.grade.Grades;
import com.jjkcraft.particle.JjkParticles;
import com.jjkcraft.registry.JjkRegistries;
import com.jjkcraft.sorcerer.Progression;
import com.jjkcraft.technique.TechniqueDefinition;
import com.jjkcraft.technique.TechniqueService;
import com.jjkcraft.technique.Techniques;

/** A technique scroll: reading it teaches the technique written in it, if the reader's grade allows. */
public class TechniqueScrollItem extends Item {
	public TechniqueScrollItem(Properties properties) {
		super(properties);
	}

	public static ItemStack of(Identifier technique) {
		ItemStack stack = new ItemStack(JjkItems.TECHNIQUE_SCROLL);
		stack.set(JjkComponents.LOADED_TECHNIQUE, technique);
		return stack;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		Identifier id = stack.get(JjkComponents.LOADED_TECHNIQUE);
		if (id == null || !(player instanceof ServerPlayer server)) {
			return InteractionResult.PASS;
		}

		TechniqueDefinition technique = Techniques.get(server.registryAccess(), id).orElse(null);
		if (technique == null) {
			return InteractionResult.FAIL;
		}

		if (TechniqueService.knows(server, id)) {
			server.sendSystemMessage(Component.translatable("message.jjkcraft.scroll.known"), true);
			return InteractionResult.FAIL;
		}

		if (Progression.gradeRank(server.registryAccess(), server) < Grades.rank(server.registryAccess(), technique.grade())) {
			server.sendSystemMessage(Component.translatable("message.jjkcraft.scroll.grade"), true);
			return InteractionResult.FAIL;
		}

		TechniqueService.grant(server, id);
		stack.shrink(1);
		server.level().sendParticles(JjkParticles.of(JjkParticles.CE_AURA, technique.visuals().color(), 1f), player.getX(), player.getY() + 1,
				player.getZ(), 20, 0.4, 0.6, 0.4, 0.03);
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1f, 1.1f);
		server.sendSystemMessage(Component.translatable("message.jjkcraft.scroll.learned", Component.translatable(technique.name())));
		return InteractionResult.SUCCESS;
	}

	@Override
	public Component getName(ItemStack stack) {
		Identifier id = stack.get(JjkComponents.LOADED_TECHNIQUE);
		return id == null ? super.getName(stack)
				: Component.translatable("item.jjkcraft.technique_scroll.named", Component.translatable("technique." + id.getNamespace() + "." + id.getPath()));
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
		Identifier id = stack.get(JjkComponents.LOADED_TECHNIQUE);
		if (id == null || context.registries() == null) {
			return;
		}

		context.registries().lookup(JjkRegistries.TECHNIQUE).flatMap(r -> r.get(ResourceKey.create(JjkRegistries.TECHNIQUE, id))).ifPresent(h -> {
			TechniqueDefinition def = h.value();
			builder.accept(Component.translatable("tooltip.jjkcraft.scroll.cost", def.cast().cost(), def.cast().cooldown() / 20.0).withStyle(ChatFormatting.AQUA));
			builder.accept(Component.translatable(def.description()).withStyle(ChatFormatting.GRAY));
		});
	}
}
