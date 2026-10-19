package com.jjkcraft.modulo.item;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
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
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

import com.jjkcraft.damage.DamageModifiers;
import com.jjkcraft.modulo.Modulo;
import com.jjkcraft.modulo.ModuloItems;
import com.jjkcraft.modulo.ModuloTravel;
import com.jjkcraft.modulo.MulEnergy;

/**
 * Simurian gear paid in Mul energy (docs/MODULO.md 3.5). Original items: canon names none of them.
 * Costs are in BALANCE.md section 10.
 */
public final class ModuloGear {
	/** Mul energy per second while the Gravity Harness is on. */
	public static final double HARNESS_UPKEEP = 2;
	/** Share of a blow the Mul Ward takes, and Mul energy per point of damage it takes. */
	public static final double WARD_SHARE = 0.5;
	public static final double WARD_COST_PER_DAMAGE = 2;
	/** Mul energy to call the ship with a Naunax Beacon. */
	public static final double BEACON_COST = 30;

	private static final Set<UUID> HARNESSED = new HashSet<>();

	private ModuloGear() {
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(ModuloGear::tick);
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> HARNESSED.remove(handler.getPlayer().getUUID()));
		DamageModifiers.add((level, target, source, amount) -> {
			if (!(target instanceof ServerPlayer player) || !holding(player, ModuloItems.MUL_WARD) || !Modulo.enabled()) {
				return amount;
			}

			double share = amount * WARD_SHARE;
			double affordable = Math.min(share, MulEnergy.get(player) / WARD_COST_PER_DAMAGE);
			if (affordable <= 0.01) {
				return amount;
			}

			MulEnergy.trySpend(player, affordable * WARD_COST_PER_DAMAGE);
			level.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 1, player.getZ(), 6, 0.4, 0.5, 0.4, 0);
			return (float) (amount - affordable);
		});
	}

	static boolean holding(Player player, Item item) {
		return player.getMainHandItem().is(item) || player.getOffhandItem().is(item);
	}

	public static boolean harnessed(Player player) {
		return HARNESSED.contains(player.getUUID());
	}

	private static void tick(MinecraftServer server) {
		if (HARNESSED.isEmpty()) {
			return;
		}

		for (UUID id : Set.copyOf(HARNESSED)) {
			ServerPlayer player = server.getPlayerList().getPlayer(id);
			if (player == null || !player.isAlive() || !holding(player, ModuloItems.GRAVITY_HARNESS) || !Modulo.enabled()) {
				HARNESSED.remove(id);
				continue;
			}

			if (server.getTickCount() % 20 == 0 && !MulEnergy.trySpend(player, HARNESS_UPKEEP)) {
				HARNESSED.remove(id);
				player.sendSystemMessage(Component.translatable("message.jjkcraft.modulo.harness_dry"), true);
				continue;
			}

			player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 30, 0, false, false));
			player.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, 30, 2, false, false));
		}
	}

	/** The Gravity Harness: switched on, gravity loosens its hold (slow falling, high jumps) for Mul energy each second. */
	public static class GravityHarness extends Item {
		public GravityHarness(Properties properties) {
			super(properties);
		}

		@Override
		public InteractionResult use(Level level, Player player, InteractionHand hand) {
			if (player instanceof ServerPlayer server && Modulo.enabled()) {
				if (HARNESSED.remove(player.getUUID())) {
					server.sendSystemMessage(Component.translatable("message.jjkcraft.modulo.harness_off"), true);
				} else if (MulEnergy.get(player) >= HARNESS_UPKEEP * 2) {
					HARNESSED.add(player.getUUID());
					level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.6f, 1.8f);
					server.sendSystemMessage(Component.translatable("message.jjkcraft.modulo.harness_on"), true);
				} else {
					server.sendSystemMessage(Component.translatable("message.jjkcraft.modulo.mul_short"), true);
				}
			}

			return InteractionResult.SUCCESS;
		}

		@Override
		public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
			builder.accept(Component.translatable("item.jjkcraft.gravity_harness.desc").withStyle(ChatFormatting.GRAY));
			builder.accept(Component.translatable("tooltip.jjkcraft.modulo.mul_cost_second", (int) HARNESS_UPKEEP).withStyle(ChatFormatting.GREEN));
		}
	}

	/** The Mul Ward: held, it takes half of each blow and pays for it in Mul energy. */
	public static class MulWard extends Item {
		public MulWard(Properties properties) {
			super(properties);
		}

		@Override
		public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
			builder.accept(Component.translatable("item.jjkcraft.mul_ward.desc").withStyle(ChatFormatting.GRAY));
			builder.accept(Component.translatable("tooltip.jjkcraft.modulo.ward", (int) WARD_COST_PER_DAMAGE).withStyle(ChatFormatting.GREEN));
		}
	}

	/** The Naunax Beacon: calls you back to the ship's arrival hall, once you have boarded it. */
	public static class NaunaxBeacon extends Item {
		public NaunaxBeacon(Properties properties) {
			super(properties);
		}

		@Override
		public InteractionResult use(Level level, Player player, InteractionHand hand) {
			if (!(player instanceof ServerPlayer server)) {
				return InteractionResult.SUCCESS;
			}

			if (!Modulo.enabled() || !Modulo.progress(server).boarded()) {
				server.sendSystemMessage(Component.translatable("message.jjkcraft.modulo.beacon_unknown"), true);
				return InteractionResult.FAIL;
			}

			if (!MulEnergy.trySpend(server, BEACON_COST)) {
				server.sendSystemMessage(Component.translatable("message.jjkcraft.modulo.mul_short"), true);
				return InteractionResult.FAIL;
			}

			if (Modulo.isModuloLevel(level) && level.dimension() == Modulo.NAUNAX) {
				ModuloTravel.leave(server);
			} else {
				ModuloTravel.toShip(server);
			}

			server.getCooldowns().addCooldown(player.getItemInHand(hand), 200);
			return InteractionResult.SUCCESS_SERVER;
		}

		@Override
		public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
			builder.accept(Component.translatable("item.jjkcraft.naunax_beacon.desc").withStyle(ChatFormatting.GRAY));
			builder.accept(Component.translatable("tooltip.jjkcraft.modulo.mul_cost", (int) BEACON_COST).withStyle(ChatFormatting.GREEN));
		}
	}

	/** An item with a grey description line from its lang key ({@code <item key>.desc}). */
	public static class Described extends Item {
		private final List<String> extra;

		public Described(Properties properties, String... extra) {
			super(properties);
			this.extra = List.of(extra);
		}

		@Override
		public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
			builder.accept(Component.translatable(getDescriptionId() + ".desc").withStyle(ChatFormatting.GRAY));
			for (String key : extra) {
				builder.accept(Component.translatable(key).withStyle(ChatFormatting.DARK_PURPLE));
			}
		}
	}

}
