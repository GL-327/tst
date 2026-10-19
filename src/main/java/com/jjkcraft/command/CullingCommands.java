package com.jjkcraft.command;

import java.util.Collection;
import java.util.Optional;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.levelgen.Heightmap;

import com.jjkcraft.config.JjkConfig;
import com.jjkcraft.core.culling.CullingMath;
import com.jjkcraft.culling.CullingGame;
import com.jjkcraft.culling.CullingState;

/**
 * {@code /jjk culling ...}: run the Culling Game by hand. start, stop, reset, status, colony add,
 * join, points, rule add|remove, day (advance the clock), entrant.
 */
public final class CullingCommands {
	private static final SuggestionProvider<CommandSourceStack> RULES = (ctx, builder) ->
			SharedSuggestionProvider.suggestResource(CullingGame.allRules(ctx.getSource().getServer()).stream().map(CullingGame.RuleEntry::id), builder);

	private CullingCommands() {
	}

	public static void init() {
		JjkCommands.addSubcommand(CullingCommands::build);
	}

	private static LiteralArgumentBuilder<CommandSourceStack> build(LiteralArgumentBuilder<CommandSourceStack> root) {
		var config = JjkConfig.server().cullingGame;
		return root.then(Commands.literal("culling")
				.then(Commands.literal("start")
						.executes(ctx -> start(ctx, config.colonies, config.colonyRadius, config.colonyDistance))
						.then(Commands.argument("colonies", IntegerArgumentType.integer(1, 16))
								.executes(ctx -> start(ctx, IntegerArgumentType.getInteger(ctx, "colonies"), config.colonyRadius, config.colonyDistance))
								.then(Commands.argument("radius", IntegerArgumentType.integer(16, 256))
										.executes(ctx -> start(ctx, IntegerArgumentType.getInteger(ctx, "colonies"), IntegerArgumentType.getInteger(ctx, "radius"),
												config.colonyDistance))
										.then(Commands.argument("distance", IntegerArgumentType.integer(0, 100_000))
												.executes(ctx -> start(ctx, IntegerArgumentType.getInteger(ctx, "colonies"),
														IntegerArgumentType.getInteger(ctx, "radius"), IntegerArgumentType.getInteger(ctx, "distance")))))))
				.then(Commands.literal("stop").executes(ctx -> {
					CullingGame.end(ctx.getSource().getServer(), "stopped");
					ctx.getSource().sendSuccess(() -> Component.translatable("commands.jjkcraft.culling.stopped"), true);
					return 1;
				}))
				.then(Commands.literal("reset").executes(ctx -> {
					CullingGame.reset(ctx.getSource().getServer());
					ctx.getSource().sendSuccess(() -> Component.translatable("commands.jjkcraft.culling.reset"), true);
					return 1;
				}))
				.then(Commands.literal("status").executes(CullingCommands::status))
				.then(Commands.literal("colony")
						.then(Commands.literal("add")
								.then(Commands.argument("radius", IntegerArgumentType.integer(4, 256))
										.executes(ctx -> colony(ctx, IntegerArgumentType.getInteger(ctx, "radius"), null))
										.then(Commands.argument("name", StringArgumentType.greedyString())
												.executes(ctx -> colony(ctx, IntegerArgumentType.getInteger(ctx, "radius"),
														StringArgumentType.getString(ctx, "name")))))))
				.then(Commands.literal("join")
						.then(Commands.argument("entities", EntityArgument.entities())
								.then(Commands.argument("colony", IntegerArgumentType.integer(0))
										.executes(ctx -> join(ctx, EntityArgument.getEntities(ctx, "entities"), IntegerArgumentType.getInteger(ctx, "colony"))))))
				.then(Commands.literal("points")
						.then(Commands.argument("entities", EntityArgument.entities())
								.then(Commands.argument("amount", IntegerArgumentType.integer(-100_000, 100_000))
										.executes(ctx -> points(ctx, EntityArgument.getEntities(ctx, "entities"), IntegerArgumentType.getInteger(ctx, "amount"))))))
				.then(Commands.literal("rule")
						.then(Commands.literal("add")
								.then(Commands.argument("rule", IdentifierArgument.id()).suggests(RULES)
										.executes(ctx -> rule(ctx, IdentifierArgument.getId(ctx, "rule"), true))))
						.then(Commands.literal("remove")
								.then(Commands.argument("rule", IdentifierArgument.id()).suggests(RULES)
										.executes(ctx -> rule(ctx, IdentifierArgument.getId(ctx, "rule"), false)))))
				.then(Commands.literal("day")
						.then(Commands.argument("days", IntegerArgumentType.integer(1, 1000))
								.executes(ctx -> advance(ctx, IntegerArgumentType.getInteger(ctx, "days")))))
				.then(Commands.literal("entrant")
						.then(Commands.argument("colony", IntegerArgumentType.integer(0))
								.executes(ctx -> entrant(ctx, IntegerArgumentType.getInteger(ctx, "colony"), false))
								.then(Commands.argument("sorcerer", BoolArgumentType.bool())
										.executes(ctx -> entrant(ctx, IntegerArgumentType.getInteger(ctx, "colony"), BoolArgumentType.getBool(ctx, "sorcerer")))))));
	}

	private static int start(CommandContext<CommandSourceStack> ctx, int colonies, int radius, int distance) {
		CommandSourceStack source = ctx.getSource();
		ServerLevel level = source.getLevel();
		BlockPos around = BlockPos.containing(source.getPosition());
		if (!CullingGame.start(level, around, colonies, radius, distance)) {
			source.sendFailure(Component.translatable("commands.jjkcraft.culling.running"));
			return 0;
		}

		source.sendSuccess(() -> Component.translatable("commands.jjkcraft.culling.started", colonies), true);
		return colonies;
	}

	private static int status(CommandContext<CommandSourceStack> ctx) {
		MinecraftServer server = ctx.getSource().getServer();
		CullingState state = CullingGame.state(server);
		int day = CullingMath.day(CullingGame.now(server), state.startedAt) + 1;
		ctx.getSource().sendSuccess(() -> Component.translatable("commands.jjkcraft.culling.status", state.running ? day : 0, state.colonies.size(),
				state.standing(), state.rules.size()), false);
		for (CullingState.Colony colony : state.colonies) {
			ctx.getSource().sendSuccess(() -> Component.literal("  #" + colony.id + " ").append(Component.translatableWithFallback(colony.name, colony.name))
					.append(" " + colony.center.toShortString() + " r" + colony.radius), false);
		}

		return state.running ? 1 : 0;
	}

	private static int colony(CommandContext<CommandSourceStack> ctx, int radius, String name) {
		CommandSourceStack source = ctx.getSource();
		CullingState state = CullingGame.state(source.getServer());
		if (!state.running) {
			source.sendFailure(Component.translatable("culling.jjkcraft.refused.not_running"));
			return 0;
		}

		ServerLevel level = source.getLevel();
		BlockPos at = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BlockPos.containing(source.getPosition()));
		CullingState.Colony colony = CullingGame.raise(level, at, radius, name);
		source.sendSuccess(() -> Component.translatable("commands.jjkcraft.culling.colony", colony.id, at.toShortString()), true);
		return colony.id + 1;
	}

	private static int join(CommandContext<CommandSourceStack> ctx, Collection<? extends Entity> entities, int colony) {
		int count = 0;
		for (Entity entity : entities) {
			if (entity instanceof LivingEntity living) {
				CullingGame.join(living, colony);
				count++;
			}
		}

		int joined = count;
		ctx.getSource().sendSuccess(() -> Component.translatable("commands.jjkcraft.culling.joined", joined, colony), true);
		return count;
	}

	private static int points(CommandContext<CommandSourceStack> ctx, Collection<? extends Entity> entities, int amount) {
		for (Entity entity : entities) {
			CullingGame.addPoints(ctx.getSource().getServer(), entity.getUUID(), amount, null);
		}

		ctx.getSource().sendSuccess(() -> Component.translatable("commands.jjkcraft.culling.points", amount, entities.size()), true);
		return entities.size();
	}

	private static int rule(CommandContext<CommandSourceStack> ctx, Identifier id, boolean add) {
		MinecraftServer server = ctx.getSource().getServer();
		CullingState state = CullingGame.state(server);
		if (CullingGame.rule(server, id).isEmpty()) {
			ctx.getSource().sendFailure(Component.translatable("culling.jjkcraft.refused.unknown"));
			return 0;
		}

		long now = CullingGame.now(server);
		if (add) {
			if (state.hasRule(id)) {
				ctx.getSource().sendFailure(Component.translatable("culling.jjkcraft.refused.active"));
				return 0;
			}

			int days = CullingGame.rule(server, id).get().durationDays();
			state.rules.add(new CullingState.ActiveRule(id, Optional.empty(), now, days > 0 ? now + days * CullingMath.DAY : CullingState.ActiveRule.NEVER));
		} else {
			state.rules.removeIf(rule -> rule.id().equals(id));
		}

		state.changed();
		ctx.getSource().sendSuccess(() -> Component.translatable(add ? "commands.jjkcraft.culling.rule_added" : "commands.jjkcraft.culling.rule_removed",
				id.toString()), true);
		return 1;
	}

	/** Moves the game clock forward: the start, deadlines, scores and rule expiries shift back. */
	private static int advance(CommandContext<CommandSourceStack> ctx, int days) {
		MinecraftServer server = ctx.getSource().getServer();
		CullingState state = CullingGame.state(server);
		if (!state.running) {
			ctx.getSource().sendFailure(Component.translatable("culling.jjkcraft.refused.not_running"));
			return 0;
		}

		CullingGame.advance(server, days);
		ctx.getSource().sendSuccess(() -> Component.translatable("commands.jjkcraft.culling.day", days), true);
		return days;
	}

	private static int entrant(CommandContext<CommandSourceStack> ctx, int colonyId, boolean sorcerer) {
		MinecraftServer server = ctx.getSource().getServer();
		CullingState.Colony colony = CullingGame.state(server).colony(colonyId).orElse(null);
		ServerLevel level = colony == null ? null : server.getLevel(colony.dimension);
		if (level == null || CullingGame.spawnEntrant(level, colony, sorcerer) == null) {
			ctx.getSource().sendFailure(Component.translatable("commands.jjkcraft.culling.no_entrant"));
			return 0;
		}

		ctx.getSource().sendSuccess(() -> Component.translatable("commands.jjkcraft.culling.entrant", colonyId), true);
		return 1;
	}
}
