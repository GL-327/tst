package com.jjkcraft.item;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
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

import com.jjkcraft.culling.CullingGame;
import com.jjkcraft.culling.CullingState;
import com.jjkcraft.culling.rule.RuleEvent;
import com.jjkcraft.item.tool.CursedToolDefinition;
import com.jjkcraft.item.tool.CursedToolItem;
import com.jjkcraft.item.tool.CursedTools;

/**
 * The Tally Ledger: a rule-bound book that holds Culling Game points. A participant can enter
 * points into it (use) and take them all back out (sneak and use), but only while a rule that lets
 * points change hands is in force. Points in the ledger are safe from death, and from no one else:
 * whoever holds the book can take them.
 */
public class TallyLedgerItem extends Item {
	public TallyLedgerItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (!(player instanceof ServerPlayer server)) {
			return InteractionResult.SUCCESS;
		}

		ItemStack stack = player.getItemInHand(hand);
		CullingState state = CullingGame.state(server.level().getServer());
		CullingState.Participant participant = state.running ? state.active(player.getUUID()) : null;
		if (participant == null || !CullingGame.allows(server.level().getServer(), RuleEvent.Action.TRANSFER)) {
			server.sendSystemMessage(Component.translatable("message.jjkcraft.ledger.bound"), true);
			return InteractionResult.FAIL;
		}

		int held = stack.getOrDefault(JjkComponents.LEDGER_POINTS, 0);
		if (player.isShiftKeyDown()) {
			if (held <= 0) {
				return InteractionResult.FAIL;
			}

			CullingGame.movePoints(server.level().getServer(), player.getUUID(), held);
			stack.set(JjkComponents.LEDGER_POINTS, 0);
			server.sendSystemMessage(Component.translatable("message.jjkcraft.ledger.withdrew", held), true);
		} else {
			int step = (int) CursedTools.get(player, stack).map(CursedToolDefinition::amount).filter(a -> a > 0).orElse(5.0).doubleValue();
			if (participant.points < step) {
				server.sendSystemMessage(Component.translatable("message.jjkcraft.ledger.short", step), true);
				return InteractionResult.FAIL;
			}

			CullingGame.movePoints(server.level().getServer(), player.getUUID(), -step);
			stack.set(JjkComponents.LEDGER_POINTS, held + step);
			server.sendSystemMessage(Component.translatable("message.jjkcraft.ledger.deposited", step, held + step), true);
		}

		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1f, 1f);
		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
		builder.accept(Component.translatable("tooltip.jjkcraft.ledger.points", stack.getOrDefault(JjkComponents.LEDGER_POINTS, 0))
				.withStyle(ChatFormatting.GOLD));
		CursedToolItem.describe(this, context.registries(), builder);
	}
}
