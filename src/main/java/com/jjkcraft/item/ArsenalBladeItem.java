package com.jjkcraft.item;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import com.jjkcraft.domain.ActiveDomain;
import com.jjkcraft.domain.DomainService;
import com.jjkcraft.network.JjkNetworking;
import com.jjkcraft.network.payload.CastFeedbackPayload;
import com.jjkcraft.technique.CastFailure;
import com.jjkcraft.technique.TechniqueService;

/**
 * A blade an arsenal domain forges around a copied technique. Use it to cast that technique, for
 * less CE, but only inside the domain that made it; outside, or once that domain ends, the blade
 * is gone. It is also a decent sword while it lasts.
 */
public class ArsenalBladeItem extends Item {
	public ArsenalBladeItem(Properties properties) {
		super(properties);
	}

	public static ItemStack forge(Identifier technique, int domainInstance) {
		ItemStack stack = new ItemStack(JjkItems.ARSENAL_BLADE);
		stack.set(JjkComponents.LOADED_TECHNIQUE, technique);
		stack.set(JjkComponents.ARSENAL_DOMAIN, domainInstance);
		return stack;
	}

	/** Whether {@code player} may use this blade now: their own arsenal domain is up and they stand in it. */
	public static boolean usable(Player player, ItemStack stack) {
		Integer instance = stack.get(JjkComponents.ARSENAL_DOMAIN);
		if (instance == null) {
			return false;
		}

		return DomainService.active(player)
				.filter(d -> d.id() == instance && d.phase() != ActiveDomain.Phase.CLOSING && d.contains(player))
				.isPresent();
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		Identifier technique = stack.get(JjkComponents.LOADED_TECHNIQUE);
		if (technique == null) {
			return InteractionResult.PASS;
		}

		if (player instanceof ServerPlayer server) {
			CastFailure failure = usable(server, stack) ? TechniqueService.cast(server, technique, -1) : CastFailure.DISABLED;
			if (failure != CastFailure.NONE) {
				JjkNetworking.send(server, new CastFeedbackPayload(-1, failure));
				return InteractionResult.FAIL;
			}
		}

		return InteractionResult.SUCCESS;
	}

	@Override
	public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
		Integer instance = stack.get(JjkComponents.ARSENAL_DOMAIN);
		boolean alive = instance != null && owner instanceof Player player && DomainService.active(player)
				.filter(d -> d.id() == instance && d.phase() != ActiveDomain.Phase.CLOSING).isPresent();
		if (!alive) {
			stack.setCount(0);
		}
	}

	@Override
	public Component getName(ItemStack stack) {
		Identifier technique = stack.get(JjkComponents.LOADED_TECHNIQUE);
		if (technique == null) {
			return super.getName(stack);
		}

		return Component.translatable("item.jjkcraft.arsenal_blade.loaded", Component.translatable("technique." + technique.getNamespace() + "." + technique.getPath()));
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return true;
	}
}
