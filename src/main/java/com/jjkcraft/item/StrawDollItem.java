package com.jjkcraft.item;

import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import com.jjkcraft.damage.JjkDamageTypes;
import com.jjkcraft.energy.CursedEnergy;
import com.jjkcraft.particle.JjkParticles;

/**
 * A Straw Doll (from Straw Doll Technique: Resonance): a straw doll tied to someone. Using it drives a nail: the bound
 * one is hurt wherever they stand within reach, for a little CE. It crumbles when its nails are
 * spent or its time runs out.
 */
public class StrawDollItem extends Item {
	public static final double NAIL_COST = 8;
	public static final double REACH = 64;

	public StrawDollItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		StrawDollLink link = stack.get(JjkComponents.STRAW_DOLL_LINK);
		if (link == null || !(level instanceof ServerLevel server) || !(player instanceof ServerPlayer sorcerer)) {
			return link == null ? InteractionResult.PASS : InteractionResult.SUCCESS;
		}

		if (server.getGameTime() > link.expires()) {
			crumble(sorcerer, stack);
			return InteractionResult.FAIL;
		}

		Entity found = server.getEntity(link.target());
		if (!(found instanceof LivingEntity target) || !target.isAlive() || target.distanceTo(player) > REACH) {
			sorcerer.sendSystemMessage(Component.translatable("message.jjkcraft.straw_doll.far", link.name()), true);
			return InteractionResult.FAIL;
		}

		if (!CursedEnergy.trySpend(player, NAIL_COST)) {
			sorcerer.sendSystemMessage(Component.translatable("message.jjkcraft.straw_doll.energy"), true);
			return InteractionResult.FAIL;
		}

		target.hurtServer(server, server.damageSources().source(JjkDamageTypes.TECHNIQUE_PIERCING, player), link.damage());
		target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 20, 1, false, false));
		server.sendParticles(JjkParticles.of(JjkParticles.CURSED_SPARK, 0xC0302F, 1f), target.getX(), target.getY() + target.getBbHeight() * 0.6,
				target.getZ(), 14, 0.2, 0.3, 0.2, 0.05);
		server.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ANVIL_PLACE, SoundSource.PLAYERS, 0.5f, 1.8f);
		player.getCooldowns().addCooldown(stack, 10);
		if (link.nails() <= 1) {
			stack.shrink(1);
		} else {
			stack.set(JjkComponents.STRAW_DOLL_LINK, link.withNails(link.nails() - 1));
		}

		return InteractionResult.SUCCESS;
	}

	private static void crumble(ServerPlayer player, ItemStack stack) {
		player.sendSystemMessage(Component.translatable("message.jjkcraft.straw_doll.crumbled"), true);
		stack.shrink(1);
	}

	@Override
	public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
		StrawDollLink link = stack.get(JjkComponents.STRAW_DOLL_LINK);
		if (link == null || level.getGameTime() > link.expires()) {
			stack.setCount(0);
		}
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
		StrawDollLink link = stack.get(JjkComponents.STRAW_DOLL_LINK);
		if (link != null) {
			builder.accept(Component.translatable("tooltip.jjkcraft.straw_doll.bound", link.name()).withStyle(ChatFormatting.RED));
			builder.accept(Component.translatable("tooltip.jjkcraft.straw_doll.nails", link.nails(), Math.round(link.damage()), (int) NAIL_COST)
					.withStyle(ChatFormatting.GRAY));
		}
	}
}
