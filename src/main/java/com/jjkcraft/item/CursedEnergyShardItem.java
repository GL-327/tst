package com.jjkcraft.item;

import java.util.function.Consumer;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import com.jjkcraft.energy.CursedEnergy;
import com.jjkcraft.sound.JjkSounds;

/** A crystallized drop of cursed energy left by a curse. Using it restores CE. */
public class CursedEnergyShardItem extends Item {
	public static final double RESTORE = 25;

	public CursedEnergyShardItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (!CursedEnergy.isCapable(player) || CursedEnergy.current(player) >= CursedEnergy.max(player) - 0.5) {
			return InteractionResult.PASS;
		}

		if (player instanceof ServerPlayer server) {
			CursedEnergy.restore(server, RESTORE);
			level.playSound(null, player.getX(), player.getY(), player.getZ(), JjkSounds.HEAL, SoundSource.PLAYERS, 0.5f, 1.6f);
			player.getItemInHand(hand).consume(1, player);
			player.getCooldowns().addCooldown(player.getItemInHand(hand), 10);
		}

		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
		builder.accept(Component.translatable("item.jjkcraft.cursed_energy_shard.tooltip", (int) RESTORE).withColor(0xFF8F5CFF));
	}
}
