package com.jjkcraft.command;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import com.jjkcraft.config.JjkConfig;
import com.jjkcraft.energy.CursedEnergy;
import com.jjkcraft.energy.EnergySync;

/**
 * {@code /jjk}: operator commands for testing and server administration. Later systems add their
 * own subcommands through {@link #addSubcommand}.
 */
public final class JjkCommands {
	@FunctionalInterface
	public interface Subcommand {
		LiteralArgumentBuilder<CommandSourceStack> build(LiteralArgumentBuilder<CommandSourceStack> root);
	}

	private static final List<Subcommand> SUBCOMMANDS = new ArrayList<>();

	private JjkCommands() {
	}

	public static void addSubcommand(Subcommand subcommand) {
		SUBCOMMANDS.add(subcommand);
	}

	public static void init() {
		CommandRegistrationCallback.EVENT.register((dispatcher, context, selection) -> register(dispatcher));
	}

	private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("jjk")
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));

		root.then(Commands.literal("energy")
				.then(Commands.literal("get")
						.then(Commands.argument("targets", EntityArgument.entities())
								.executes(ctx -> energyGet(ctx.getSource(), EntityArgument.getEntities(ctx, "targets")))))
				.then(Commands.literal("set")
						.then(Commands.argument("targets", EntityArgument.entities())
								.then(Commands.argument("amount", DoubleArgumentType.doubleArg(0))
										.executes(ctx -> energySet(ctx.getSource(), EntityArgument.getEntities(ctx, "targets"),
												DoubleArgumentType.getDouble(ctx, "amount"))))))
				.then(Commands.literal("fill")
						.then(Commands.argument("targets", EntityArgument.entities())
								.executes(ctx -> energyFill(ctx.getSource(), EntityArgument.getEntities(ctx, "targets")))))
				.then(Commands.literal("spend")
						.then(Commands.argument("targets", EntityArgument.entities())
								.then(Commands.argument("amount", DoubleArgumentType.doubleArg(0))
										.executes(ctx -> energySpend(ctx.getSource(), EntityArgument.getEntities(ctx, "targets"),
												DoubleArgumentType.getDouble(ctx, "amount")))))));

		root.then(Commands.literal("config")
				.then(Commands.literal("reload").executes(ctx -> configReload(ctx.getSource()))));

		for (Subcommand subcommand : SUBCOMMANDS) {
			root = subcommand.build(root);
		}

		dispatcher.register(root);
	}

	private static int energyGet(CommandSourceStack source, Collection<? extends Entity> targets) {
		int count = 0;
		for (Entity entity : targets) {
			if (entity instanceof LivingEntity living && CursedEnergy.isCapable(living)) {
				double current = CursedEnergy.current(living);
				double max = CursedEnergy.max(living);
				boolean strained = CursedEnergy.isOverexerted(living);
				source.sendSuccess(() -> Component.translatable("commands.jjkcraft.energy.get",
						entity.getDisplayName(), format(current), format(max),
						Component.translatable(strained ? "commands.jjkcraft.energy.overexerted" : "commands.jjkcraft.energy.normal")), false);
				count++;
			}
		}

		if (count == 0) {
			source.sendFailure(Component.translatable("commands.jjkcraft.energy.none"));
		}

		return count;
	}

	private static int energySet(CommandSourceStack source, Collection<? extends Entity> targets, double amount) {
		int count = 0;
		for (Entity entity : targets) {
			if (entity instanceof LivingEntity living && CursedEnergy.isCapable(living)) {
				CursedEnergy.set(living, amount);
				count++;
			}
		}

		int changed = count;
		source.sendSuccess(() -> Component.translatable("commands.jjkcraft.energy.set", changed, format(amount)), true);
		return count;
	}

	private static int energyFill(CommandSourceStack source, Collection<? extends Entity> targets) {
		int count = 0;
		for (Entity entity : targets) {
			if (entity instanceof LivingEntity living && CursedEnergy.isCapable(living)) {
				CursedEnergy.fill(living);
				count++;
			}
		}

		int changed = count;
		source.sendSuccess(() -> Component.translatable("commands.jjkcraft.energy.fill", changed), true);
		return count;
	}

	private static int energySpend(CommandSourceStack source, Collection<? extends Entity> targets, double amount) {
		int count = 0;
		for (Entity entity : targets) {
			if (entity instanceof LivingEntity living && CursedEnergy.trySpend(living, amount)) {
				count++;
			}
		}

		int changed = count;
		source.sendSuccess(() -> Component.translatable("commands.jjkcraft.energy.spend", changed, format(amount)), true);
		return count;
	}

	private static int configReload(CommandSourceStack source) throws CommandSyntaxException {
		if (!JjkConfig.reload()) {
			source.sendFailure(Component.translatable("commands.jjkcraft.config.reload_failed"));
			return 0;
		}

		for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
			EnergySync.sendSettings(player);
			EnergySync.sendNow(player);
		}

		source.sendSuccess(() -> Component.translatable("commands.jjkcraft.config.reloaded"), true);
		return 1;
	}

	public static String format(double value) {
		return String.format(Locale.ROOT, "%.1f", value);
	}
}
