package com.jjkcraft.item;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import com.jjkcraft.energy.CursedEnergy;
import com.jjkcraft.item.tool.CursedToolDefinition;
import com.jjkcraft.item.tool.CursedToolItem;
import com.jjkcraft.item.tool.CursedTools;

/**
 * The Drifting Gourd: uncorked, it pours out a cloud that carries its holder. While it is held and
 * open the holder can fly, paying CE every second; a puff of cloud trails beneath them. Run dry,
 * let go of it, or cork it again, and the cloud lets them down gently.
 */
public class DriftingGourdItem extends Item {
	private static final Set<UUID> ALOFT = new HashSet<>();

	public DriftingGourdItem(Properties properties) {
		super(properties);
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(DriftingGourdItem::tick);
	}

	public static boolean aloft(Player player) {
		return ALOFT.contains(player.getUUID());
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (player instanceof ServerPlayer server && !player.isCreative() && !player.isSpectator()) {
			if (ALOFT.contains(player.getUUID())) {
				land(server, "message.jjkcraft.gourd.corked");
			} else if (CursedEnergy.current(player) >= upkeep(player) * 2) {
				ALOFT.add(player.getUUID());
				player.getAbilities().mayfly = true;
				player.getAbilities().flying = true;
				server.onUpdateAbilities();
				level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BREEZE_IDLE_GROUND, SoundSource.PLAYERS, 1f, 1.4f);
				server.sendSystemMessage(Component.translatable("message.jjkcraft.gourd.uncorked"), true);
			} else {
				server.sendSystemMessage(Component.translatable("message.jjkcraft.gourd.energy"), true);
			}
		}

		return InteractionResult.SUCCESS;
	}

	private static double upkeep(Player player) {
		return CursedTools.get(player, new ItemStack(JjkItems.DRIFTING_GOURD)).map(CursedToolDefinition::amount).filter(a -> a > 0).orElse(3.0);
	}

	private static boolean holding(Player player) {
		return player.getMainHandItem().getItem() instanceof DriftingGourdItem || player.getOffhandItem().getItem() instanceof DriftingGourdItem;
	}

	private static void tick(MinecraftServer server) {
		if (ALOFT.isEmpty()) {
			return;
		}

		for (UUID id : Set.copyOf(ALOFT)) {
			ServerPlayer player = server.getPlayerList().getPlayer(id);
			if (player == null) {
				ALOFT.remove(id);
				continue;
			}

			if (!holding(player) || !player.isAlive()) {
				land(player, "message.jjkcraft.gourd.dropped");
				continue;
			}

			if (server.getTickCount() % 20 == 0 && !CursedEnergy.trySpend(player, upkeep(player))) {
				land(player, "message.jjkcraft.gourd.dry");
				continue;
			}

			if (server.getTickCount() % 3 == 0) {
				player.level().sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() - 0.2, player.getZ(), 3, 0.35, 0.05, 0.35, 0.01);
			}
		}
	}

	/** The cloud sets its rider down. Public for tests. */
	public static void land(ServerPlayer player, String message) {
		ALOFT.remove(player.getUUID());
		if (!player.isCreative() && !player.isSpectator()) {
			player.getAbilities().mayfly = false;
			player.getAbilities().flying = false;
			player.onUpdateAbilities();
		}

		player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 100, 0, false, false));
		player.sendSystemMessage(Component.translatable(message), true);
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
		CursedToolItem.describe(this, context.registries(), builder);
	}
}
