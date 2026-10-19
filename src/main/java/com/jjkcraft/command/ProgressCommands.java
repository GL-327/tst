package com.jjkcraft.command;

import java.util.Collection;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import com.jjkcraft.grade.GradeDefinition;
import com.jjkcraft.grade.Grades;
import com.jjkcraft.sorcerer.Awakening;
import com.jjkcraft.sorcerer.Progression;
import com.jjkcraft.sorcerer.SorcererProgress;

/** {@code /jjk grade|xp|awaken|counter|progress}: progression commands for operators and tests. */
public final class ProgressCommands {
	private static final SuggestionProvider<CommandSourceStack> GRADES = (ctx, builder) ->
			SharedSuggestionProvider.suggestResource(Grades.ladder(ctx.getSource().registryAccess()).stream().map(Grades.Entry::id), builder);

	private ProgressCommands() {
	}

	public static void init() {
		JjkCommands.addSubcommand(ProgressCommands::build);
	}

	private static LiteralArgumentBuilder<CommandSourceStack> build(LiteralArgumentBuilder<CommandSourceStack> root) {
		root.then(Commands.literal("grade")
				.then(Commands.literal("get")
						.then(Commands.argument("player", EntityArgument.player())
								.executes(ctx -> gradeGet(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
				.then(Commands.literal("set")
						.then(Commands.argument("players", EntityArgument.players())
								.then(Commands.argument("grade", IdentifierArgument.id()).suggests(GRADES)
										.executes(ctx -> gradeSet(ctx.getSource(), EntityArgument.getPlayers(ctx, "players"),
												IdentifierArgument.getId(ctx, "grade"), false))
										.then(Commands.argument("ceremony", BoolArgumentType.bool())
												.executes(ctx -> gradeSet(ctx.getSource(), EntityArgument.getPlayers(ctx, "players"),
														IdentifierArgument.getId(ctx, "grade"), BoolArgumentType.getBool(ctx, "ceremony"))))))));

		root.then(Commands.literal("xp")
				.then(Commands.literal("add")
						.then(Commands.argument("players", EntityArgument.players())
								.then(Commands.argument("amount", IntegerArgumentType.integer(1))
										.executes(ctx -> xpAdd(ctx.getSource(), EntityArgument.getPlayers(ctx, "players"),
												IntegerArgumentType.getInteger(ctx, "amount")))))));

		root.then(Commands.literal("awaken")
				.then(Commands.argument("players", EntityArgument.players())
						.executes(ctx -> awaken(ctx.getSource(), EntityArgument.getPlayers(ctx, "players"), null))
						.then(Commands.argument("heavenly", BoolArgumentType.bool())
								.executes(ctx -> awaken(ctx.getSource(), EntityArgument.getPlayers(ctx, "players"),
										BoolArgumentType.getBool(ctx, "heavenly"))))));

		root.then(Commands.literal("counter")
				.then(Commands.literal("add")
						.then(Commands.argument("players", EntityArgument.players())
								.then(Commands.argument("key", StringArgumentType.string())
										.then(Commands.argument("amount", IntegerArgumentType.integer())
												.executes(ctx -> counterAdd(ctx.getSource(), EntityArgument.getPlayers(ctx, "players"),
														StringArgumentType.getString(ctx, "key"), IntegerArgumentType.getInteger(ctx, "amount"))))))));

		root.then(Commands.literal("progress")
				.then(Commands.literal("reset")
						.then(Commands.argument("players", EntityArgument.players())
								.executes(ctx -> reset(ctx.getSource(), EntityArgument.getPlayers(ctx, "players"))))));
		return root;
	}

	private static int gradeGet(CommandSourceStack source, ServerPlayer player) {
		SorcererProgress p = Progression.get(player);
		Component grade = Grades.get(source.registryAccess(), p.grade())
				.map(g -> (Component) Component.translatable(g.name()).withColor(g.color()))
				.orElse(Component.literal(p.grade().toString()));
		source.sendSuccess(() -> Component.translatable("commands.jjkcraft.grade.get", player.getDisplayName(), grade, p.gradeXp(),
				Progression.unspentPoints(source.registryAccess(), p)), false);
		return 1;
	}

	private static int gradeSet(CommandSourceStack source, Collection<ServerPlayer> players, Identifier grade, boolean ceremony)
			throws CommandSyntaxException {
		GradeDefinition definition = Grades.get(source.registryAccess(), grade).orElse(null);
		if (definition == null) {
			source.sendFailure(Component.translatable("commands.jjkcraft.grade.unknown", grade.toString()));
			return 0;
		}

		for (ServerPlayer player : players) {
			if (ceremony) {
				Progression.promote(player, new Grades.Entry(grade, definition));
			} else {
				Progression.setGrade(player, grade);
			}
		}

		source.sendSuccess(() -> Component.translatable("commands.jjkcraft.grade.set", players.size(), Component.translatable(definition.name())), true);
		return players.size();
	}

	private static int xpAdd(CommandSourceStack source, Collection<ServerPlayer> players, int amount) {
		players.forEach(player -> Progression.addXp(player, amount));
		source.sendSuccess(() -> Component.translatable("commands.jjkcraft.xp.add", amount, players.size()), true);
		return players.size();
	}

	private static int awaken(CommandSourceStack source, Collection<ServerPlayer> players, Boolean heavenly) {
		int count = 0;
		for (ServerPlayer player : players) {
			if (heavenly != null && Progression.get(player).awakened()) {
				// Re-roll on request: reset first so the forced outcome applies.
				Awakening.reset(player);
			}

			if (Awakening.awaken(player, heavenly)) {
				count++;
			}
		}

		int changed = count;
		source.sendSuccess(() -> Component.translatable("commands.jjkcraft.awaken", changed), true);
		return count;
	}

	private static int counterAdd(CommandSourceStack source, Collection<ServerPlayer> players, String key, int amount) {
		players.forEach(player -> Progression.addCounter(player, key, amount));
		source.sendSuccess(() -> Component.translatable("commands.jjkcraft.counter.add", key, amount, players.size()), true);
		return players.size();
	}

	private static int reset(CommandSourceStack source, Collection<ServerPlayer> players) {
		players.forEach(Awakening::reset);
		source.sendSuccess(() -> Component.translatable("commands.jjkcraft.progress.reset", players.size()), true);
		return players.size();
	}
}
