package com.jjkcraft.command;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import com.jjkcraft.blackflash.BlackFlash;
import com.jjkcraft.core.vow.VowMath;
import com.jjkcraft.reverse.ReverseTechnique;
import com.jjkcraft.technique.CastFailure;
import com.jjkcraft.vow.VowState;
import com.jjkcraft.vow.Vows;

/** {@code /jjk blackflash ...}, {@code /jjk reverse ...} and {@code /jjk vow ...}. */
public final class SorceryCommands {
	private static final SuggestionProvider<CommandSourceStack> VOWS = (ctx, builder) ->
			SharedSuggestionProvider.suggestResource(Vows.all(ctx.getSource().registryAccess()).stream().map(Vows.Entry::id), builder);
	private static final SuggestionProvider<CommandSourceStack> FOCI = (ctx, builder) ->
			SharedSuggestionProvider.suggest(List.of(VowMath.Focus.values()).stream().map(VowMath.Focus::key), builder);
	private static final SuggestionProvider<CommandSourceStack> TERMS = (ctx, builder) ->
			SharedSuggestionProvider.suggest(List.of(VowMath.Term.values()).stream().map(VowMath.Term::key), builder);

	private SorceryCommands() {
	}

	public static void init() {
		JjkCommands.addSubcommand(SorceryCommands::build);
	}

	private static LiteralArgumentBuilder<CommandSourceStack> build(LiteralArgumentBuilder<CommandSourceStack> root) {
		root.then(Commands.literal("blackflash")
				.then(Commands.literal("force")
						.then(Commands.argument("entities", EntityArgument.entities())
								.executes(ctx -> forceBlackFlash(ctx.getSource(), EntityArgument.getEntities(ctx, "entities"))))));

		root.then(Commands.literal("reverse")
				.then(Commands.literal("grant")
						.then(Commands.argument("players", EntityArgument.players())
								.executes(ctx -> setReverse(ctx.getSource(), EntityArgument.getPlayers(ctx, "players"), true))))
				.then(Commands.literal("revoke")
						.then(Commands.argument("players", EntityArgument.players())
								.executes(ctx -> setReverse(ctx.getSource(), EntityArgument.getPlayers(ctx, "players"), false))))
				.then(Commands.literal("force")
						.then(Commands.argument("players", EntityArgument.players())
								.executes(ctx -> forceReverse(ctx.getSource(), EntityArgument.getPlayers(ctx, "players"))))));

		root.then(Commands.literal("vow")
				.then(Commands.literal("swear")
						.then(Commands.argument("player", EntityArgument.player())
								.then(Commands.argument("vow", IdentifierArgument.id()).suggests(VOWS)
										.executes(ctx -> swear(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), IdentifierArgument.getId(ctx, "vow"),
												"power", "day"))
										.then(Commands.argument("focus", StringArgumentType.word()).suggests(FOCI)
												.then(Commands.argument("term", StringArgumentType.word()).suggests(TERMS)
														.executes(ctx -> swear(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), IdentifierArgument.getId(ctx, "vow"),
																StringArgumentType.getString(ctx, "focus"), StringArgumentType.getString(ctx, "term"))))))))
				.then(Commands.literal("break")
						.then(Commands.argument("players", EntityArgument.players())
								.executes(ctx -> breakVows(ctx.getSource(), EntityArgument.getPlayers(ctx, "players")))))
				.then(Commands.literal("clear")
						.then(Commands.argument("players", EntityArgument.players())
								.executes(ctx -> clearVows(ctx.getSource(), EntityArgument.getPlayers(ctx, "players")))))
				.then(Commands.literal("list").executes(ctx -> listVows(ctx.getSource()))));
		return root;
	}

	private static int forceBlackFlash(CommandSourceStack source, Collection<? extends Entity> entities) {
		int count = 0;
		for (Entity entity : entities) {
			if (entity instanceof LivingEntity living) {
				BlackFlash.forceNext(living);
				count++;
			}
		}

		int n = count;
		source.sendSuccess(() -> Component.translatable("commands.jjkcraft.blackflash.forced", n), true);
		return count;
	}

	private static int setReverse(CommandSourceStack source, Collection<ServerPlayer> players, boolean unlocked) {
		players.forEach(p -> ReverseTechnique.set(p, ReverseTechnique.state(p).withUnlocked(unlocked), false));
		source.sendSuccess(() -> Component.translatable(unlocked ? "commands.jjkcraft.reverse.granted" : "commands.jjkcraft.reverse.revoked",
				players.size()), true);
		return players.size();
	}

	private static int forceReverse(CommandSourceStack source, Collection<ServerPlayer> players) {
		players.forEach(ReverseTechnique::forceAwakening);
		source.sendSuccess(() -> Component.translatable("commands.jjkcraft.reverse.forced", players.size()), true);
		return players.size();
	}

	private static int swear(CommandSourceStack source, ServerPlayer player, Identifier vow, String focusName, String termName) {
		VowMath.Focus focus;
		VowMath.Term term;
		try {
			focus = VowMath.Focus.valueOf(focusName.toUpperCase(Locale.ROOT));
			term = VowMath.Term.valueOf(termName.toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException e) {
			source.sendFailure(Component.translatable("commands.jjkcraft.vow.bad_option", focusName, termName));
			return 0;
		}

		CastFailure failure = Vows.swear(player, List.of(vow), focus, term);
		if (failure != CastFailure.NONE) {
			source.sendFailure(Component.translatable("commands.jjkcraft.vow.refused", Component.translatable(failure.translationKey())));
			return 0;
		}

		source.sendSuccess(() -> Component.translatable("commands.jjkcraft.vow.sworn", player.getDisplayName(), vow.toString()), true);
		return 1;
	}

	private static int breakVows(CommandSourceStack source, Collection<ServerPlayer> players) {
		players.forEach(Vows::breakVow);
		source.sendSuccess(() -> Component.translatable("commands.jjkcraft.vow.broken", players.size()), true);
		return players.size();
	}

	private static int clearVows(CommandSourceStack source, Collection<ServerPlayer> players) {
		players.forEach(p -> {
			VowState state = Vows.state(p);
			p.setAttached(Vows.STATE, new VowState(List.of(), state.focus(), state.term(), 0, -1, 0, state.fulfilled(), state.broken(), 0));
			com.jjkcraft.energy.CursedEnergy.checkpoint(p);
			Vows.resync(p);
		});
		source.sendSuccess(() -> Component.translatable("commands.jjkcraft.vow.cleared", players.size()), true);
		return players.size();
	}

	private static int listVows(CommandSourceStack source) {
		List<Vows.Entry> all = Vows.all(source.registryAccess());
		source.sendSuccess(() -> Component.translatable("commands.jjkcraft.vow.list", all.size()), false);
		for (Vows.Entry entry : all) {
			source.sendSuccess(() -> Component.literal(" - " + entry.id() + " (" + entry.definition().restriction().getSerializedName() + ", "
					+ entry.definition().weight() + ")"), false);
		}

		return all.size();
	}
}
