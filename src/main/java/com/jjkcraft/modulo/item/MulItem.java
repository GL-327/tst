package com.jjkcraft.modulo.item;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import com.jjkcraft.config.JjkConfig;
import com.jjkcraft.modulo.Modulo;
import com.jjkcraft.modulo.ModuloItems;
import com.jjkcraft.modulo.MulEnergy;
import com.jjkcraft.modulo.mission.Missions;

/**
 * Mul, the Simurian ore that converts rolloluca (cursed energy) into energy (docs/MODULO.md 3.4).
 * Hold use to pour cursed energy into it: every quarter second a little drains away and turns into
 * Mul energy at {@code modulo.mulEnergyRatio}. Each piece holds {@code capacity} cursed energy; the
 * top piece of a stack shows what it still holds and crumbles when spent.
 */
public class MulItem extends Item {
	/** Cursed energy poured per second. */
	private static final double RATE = 20;
	private static final int STEP = 5;

	private final int capacity;

	public MulItem(Properties properties, int capacity) {
		super(properties);
		this.capacity = capacity;
	}

	public int capacity() {
		return capacity;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (!Modulo.enabled()) {
			if (player instanceof ServerPlayer server) {
				server.sendSystemMessage(Component.translatable("message.jjkcraft.modulo.disabled"), true);
			}

			return InteractionResult.FAIL;
		}

		player.startUsingItem(hand);
		return InteractionResult.CONSUME;
	}

	@Override
	public int getUseDuration(ItemStack stack, LivingEntity entity) {
		return 72_000;
	}

	@Override
	public ItemUseAnimation getUseAnimation(ItemStack stack) {
		return ItemUseAnimation.BOW;
	}

	@Override
	public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
		if (!(entity instanceof ServerPlayer player) || !(level instanceof ServerLevel server) || remaining % STEP != 0) {
			return;
		}

		int left = stack.getOrDefault(ModuloItems.MUL_LEFT, capacity);
		double poured = MulEnergy.pour(player, RATE * STEP / 20.0, left);
		if (poured <= 0) {
			player.stopUsingItem();
			player.sendSystemMessage(Component.translatable(MulEnergy.get(player) >= MulEnergy.max() - 1e-6
					? "message.jjkcraft.modulo.mul_full" : "message.jjkcraft.modulo.mul_no_energy"), true);
			return;
		}

		Missions.onMulEnergyMade(player, poured * JjkConfig.server().modulo.mulEnergyRatio);
		left -= (int) Math.ceil(poured);
		server.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 1.1, player.getZ(), 3, 0.3, 0.3, 0.3, 0);
		if (left <= 0) {
			stack.shrink(1);
			stack.remove(ModuloItems.MUL_LEFT);
			level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 0.8f, 1.2f);
		} else {
			stack.set(ModuloItems.MUL_LEFT, left);
			if (remaining % 20 == 0) {
				level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.6f, 1.6f);
			}
		}
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
		int left = stack.getOrDefault(ModuloItems.MUL_LEFT, capacity);
		builder.accept(Component.translatable("tooltip.jjkcraft.modulo.mul", left, capacity).withStyle(ChatFormatting.GREEN));
		builder.accept(Component.translatable("tooltip.jjkcraft.modulo.mul_use").withStyle(ChatFormatting.GRAY));
	}
}
