package com.jjkcraft.modulo;

import java.util.Collection;
import java.util.Optional;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import com.jjkcraft.command.JjkCommands;
import com.jjkcraft.modulo.data.ModuloRegistries;
import com.jjkcraft.modulo.mission.Missions;
import com.jjkcraft.modulo.world.SiteBuilder;

/**
 * {@code /jjk modulo ...}: run the Modulo expansion by hand. descent start|stop|status, board, ship,
 * tokyo, leave, mission status|jump|complete|reset, mul, companion, site build.
 */
public final class ModuloCommands {
	private static final SuggestionProvider<CommandSourceStack> MISSIONS = (ctx, builder) -> SharedSuggestionProvider.suggestResource(
			ModuloRegistries.missions(ctx.getSource().registryAccess()).stream().map(ModuloRegistries.Mission::id), builder);
	private static final SuggestionProvider<CommandSourceStack> SITES = (ctx, builder) -> SharedSuggestionProvider.suggestResource(
			ctx.getSource().registryAccess().lookupOrThrow(ModuloRegistries.SITE).keySet(), builder);

	private ModuloCommands() {
	}

	public static void init() {
		JjkCommands.addSubcommand(ModuloCommands::build);
	}

	private interface PlayerAction {
		void run(ServerPlayer player) throws CommandSyntaxException;
	}

	private static LiteralArgumentBuilder<CommandSourceStack> build(LiteralArgumentBuilder<CommandSourceStack> root) {
		return root.then(Commands.literal("modulo")
				.then(Commands.literal("descent")
						.then(Commands.literal("start")
								.executes(ctx -> descent(ctx, BlockPos.containing(ctx.getSource().getPosition())))
								.then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(ctx -> descent(ctx, BlockPosArgument.getBlockPos(ctx, "pos")))))
						.then(Commands.literal("stop").executes(ctx -> {
							NaunaxDescent.end(ctx.getSource().getServer());
							ctx.getSource().sendSuccess(() -> Component.translatable("commands.jjkcraft.modulo.descent_stopped"), true);
							return 1;
						}))
						.then(Commands.literal("status").executes(ModuloCommands::status)))
				.then(players("board", ModuloTravel::board))
				.then(players("ship", player -> ModuloTravel.toSite(player, ModuloTravel.SHIP_SITE, "arrival")))
				.then(players("tokyo", ModuloTravel::toTokyo))
				.then(players("leave", ModuloTravel::leave))
				.then(Commands.literal("mission")
						.then(players("status", player -> player.sendSystemMessage(statusLine(player))))
						.then(players("complete", player -> Missions.current(player).ifPresent(c -> Missions.complete(player, c, true))))
						.then(players("reset", player -> Modulo.setProgress(player, ModuloProgress.EMPTY)))
						.then(Commands.literal("jump")
								.then(Commands.argument("mission", IdentifierArgument.id()).suggests(MISSIONS)
										.executes(ctx -> jump(ctx, 0, null))
										.then(Commands.argument("stage", IntegerArgumentType.integer(0, 63))
												.executes(ctx -> jump(ctx, IntegerArgumentType.getInteger(ctx, "stage"), null))
												.then(Commands.argument("targets", EntityArgument.players())
														.executes(ctx -> jump(ctx, IntegerArgumentType.getInteger(ctx, "stage"), EntityArgument.getPlayers(ctx, "targets"))))))))
				.then(Commands.literal("mul")
						.then(Commands.argument("amount", DoubleArgumentType.doubleArg(0, 100_000))
								.executes(ctx -> each(ctx, null, p -> MulEnergy.set(p, DoubleArgumentType.getDouble(ctx, "amount"))))
								.then(Commands.argument("targets", EntityArgument.players())
										.executes(ctx -> each(ctx, EntityArgument.getPlayers(ctx, "targets"), p -> MulEnergy.set(p, DoubleArgumentType.getDouble(ctx, "amount")))))))
				.then(Commands.literal("companion")
						.then(Commands.argument("value", BoolArgumentType.bool())
								.executes(ctx -> each(ctx, null, p -> Modulo.setProgress(p, Modulo.progress(p).withCompanion(BoolArgumentType.getBool(ctx, "value")))))))
				.then(Commands.literal("site")
						.then(Commands.literal("build")
								.then(Commands.argument("site", IdentifierArgument.id()).suggests(SITES).executes(ctx -> {
									Identifier id = IdentifierArgument.getId(ctx, "site");
									Modulo.state(ctx.getSource().getServer()).builtSites.remove(id);
									boolean built = SiteBuilder.ensure(ctx.getSource().getServer(), id).isPresent();
									ctx.getSource().sendSuccess(() -> Component.translatable(built ? "commands.jjkcraft.modulo.site_built" : "commands.jjkcraft.modulo.no_site",
											id.toString()), true);
									return built ? 1 : 0;
								})))));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> players(String name, PlayerAction action) {
		return Commands.literal(name)
				.executes(ctx -> each(ctx, null, action))
				.then(Commands.argument("targets", EntityArgument.players()).executes(ctx -> each(ctx, EntityArgument.getPlayers(ctx, "targets"), action)));
	}

	private static int each(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets, PlayerAction action) throws CommandSyntaxException {
		Collection<ServerPlayer> players = targets != null ? targets : java.util.List.of(ctx.getSource().getPlayerOrException());
		for (ServerPlayer player : players) {
			action.run(player);
		}

		ctx.getSource().sendSuccess(() -> Component.translatable("commands.jjkcraft.modulo.done", players.size()), true);
		return players.size();
	}

	private static int descent(CommandContext<CommandSourceStack> ctx, BlockPos pos) {
		BlockPos foot = NaunaxDescent.start(ctx.getSource().getServer(), pos, com.jjkcraft.config.JjkConfig.server().modulo.descentMinutes * 1200L);
		ctx.getSource().sendSuccess(() -> Component.translatable("commands.jjkcraft.modulo.descent_started", foot.getX(), foot.getY(), foot.getZ()), true);
		return 1;
	}

	private static int status(CommandContext<CommandSourceStack> ctx) {
		ModuloState state = Modulo.state(ctx.getSource().getServer());
		boolean active = NaunaxDescent.active(ctx.getSource().getServer());
		ctx.getSource().sendSuccess(() -> Component.translatable("commands.jjkcraft.modulo.descent_status", Modulo.enabled() ? "on" : "off",
				active ? state.descentPos.map(BlockPos::toShortString).orElse("-") : "-", state.nextDescent), false);
		return 1;
	}

	private static int jump(CommandContext<CommandSourceStack> ctx, int stage, Collection<ServerPlayer> targets) throws CommandSyntaxException {
		Identifier mission = IdentifierArgument.getId(ctx, "mission");
		if (ModuloRegistries.mission(ctx.getSource().registryAccess(), mission).isEmpty()) {
			ctx.getSource().sendFailure(Component.translatable("commands.jjkcraft.modulo.no_mission", mission.toString()));
			return 0;
		}

		return each(ctx, targets, p -> Missions.jumpTo(p, mission, stage));
	}

	private static Component statusLine(ServerPlayer player) {
		ModuloProgress progress = Modulo.progress(player);
		Optional<Missions.Current> current = Missions.current(player);
		return Component.translatable("commands.jjkcraft.modulo.mission_status", player.getName(),
				current.map(c -> c.id().toString()).orElse("-"), current.map(c -> Integer.toString(c.stageIndex())).orElse("-"),
				progress.counter(), progress.completed().size(), String.format("%.0f", MulEnergy.get(player)), progress.companion());
	}
}
