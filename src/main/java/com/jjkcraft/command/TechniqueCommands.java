package com.jjkcraft.command;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

import com.mojang.brigadier.arguments.IntegerArgumentType;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import com.jjkcraft.technique.CastFailure;
import com.jjkcraft.technique.TechniqueService;
import com.jjkcraft.technique.TechniqueState;
import com.jjkcraft.technique.Techniques;

/** {@code /jjk technique ...}: grant, revoke, equip, list, force-cast and reset cooldowns. */
public final class TechniqueCommands {
	private static final SuggestionProvider<CommandSourceStack> TECHNIQUES = (ctx, builder) ->
			SharedSuggestionProvider.suggestResource(Techniques.all(ctx.getSource().registryAccess()).stream().map(Techniques.Entry::id), builder);

	private TechniqueCommands() {
	}

	public static void init() {
		JjkCommands.addSubcommand(TechniqueCommands::build);
	}

	private static LiteralArgumentBuilder<CommandSourceStack> build(LiteralArgumentBuilder<CommandSourceStack> root) {
		root.then(Commands.literal("technique")
				.then(Commands.literal("grant")
						.then(Commands.argument("players", EntityArgument.players())
								.then(Commands.argument("technique", IdentifierArgument.id()).suggests(TECHNIQUES)
										.executes(ctx -> grant(ctx.getSource(), EntityArgument.getPlayers(ctx, "players"), IdentifierArgument.getId(ctx, "technique"))))))
				.then(Commands.literal("revoke")
						.then(Commands.argument("players", EntityArgument.players())
								.then(Commands.argument("technique", IdentifierArgument.id()).suggests(TECHNIQUES)
										.executes(ctx -> revoke(ctx.getSource(), EntityArgument.getPlayers(ctx, "players"), IdentifierArgument.getId(ctx, "technique"))))))
				.then(Commands.literal("innate")
						.then(Commands.argument("players", EntityArgument.players())
								.then(Commands.argument("technique", IdentifierArgument.id()).suggests(TECHNIQUES)
										.executes(ctx -> innate(ctx.getSource(), EntityArgument.getPlayers(ctx, "players"), IdentifierArgument.getId(ctx, "technique"))))))
				.then(Commands.literal("equip")
						.then(Commands.argument("players", EntityArgument.players())
								.then(Commands.argument("slot", IntegerArgumentType.integer(1, TechniqueState.SLOTS))
										.then(Commands.argument("technique", IdentifierArgument.id()).suggests(TECHNIQUES)
												.executes(ctx -> equip(ctx.getSource(), EntityArgument.getPlayers(ctx, "players"),
														IntegerArgumentType.getInteger(ctx, "slot") - 1, IdentifierArgument.getId(ctx, "technique")))))))
				.then(Commands.literal("list")
						.then(Commands.argument("player", EntityArgument.player())
								.executes(ctx -> list(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
				.then(Commands.literal("cast")
						.then(Commands.argument("casters", EntityArgument.entities())
								.then(Commands.argument("technique", IdentifierArgument.id()).suggests(TECHNIQUES)
										.executes(ctx -> cast(ctx.getSource(), EntityArgument.getEntities(ctx, "casters"), IdentifierArgument.getId(ctx, "technique"))))))
				.then(Commands.literal("cooldowns")
						.then(Commands.argument("players", EntityArgument.players())
								.executes(ctx -> resetCooldowns(ctx.getSource(), EntityArgument.getPlayers(ctx, "players"))))));
		return root;
	}

	private static boolean exists(CommandSourceStack source, Identifier id) {
		if (Techniques.get(source.registryAccess(), id).isPresent()) {
			return true;
		}

		source.sendFailure(Component.translatable("commands.jjkcraft.technique.unknown", id.toString()));
		return false;
	}

	private static int grant(CommandSourceStack source, Collection<ServerPlayer> players, Identifier id) {
		if (!exists(source, id)) {
			return 0;
		}

		players.forEach(p -> TechniqueService.grant(p, id));
		source.sendSuccess(() -> Component.translatable("commands.jjkcraft.technique.grant", id.toString(), players.size()), true);
		return players.size();
	}

	private static int revoke(CommandSourceStack source, Collection<ServerPlayer> players, Identifier id) {
		players.forEach(p -> TechniqueService.revoke(p, id));
		source.sendSuccess(() -> Component.translatable("commands.jjkcraft.technique.revoke", id.toString(), players.size()), true);
		return players.size();
	}

	private static int innate(CommandSourceStack source, Collection<ServerPlayer> players, Identifier id) {
		if (!exists(source, id)) {
			return 0;
		}

		players.forEach(p -> TechniqueService.setInnate(p, id));
		source.sendSuccess(() -> Component.translatable("commands.jjkcraft.technique.innate", id.toString(), players.size()), true);
		return players.size();
	}

	private static int equip(CommandSourceStack source, Collection<ServerPlayer> players, int slot, Identifier id) {
		if (!exists(source, id)) {
			return 0;
		}

		for (ServerPlayer player : players) {
			if (!TechniqueService.knows(player, id)) {
				TechniqueService.grant(player, id);
			}

			TechniqueService.set(player, TechniqueService.get(player).withSlot(slot, Optional.of(id)));
		}

		source.sendSuccess(() -> Component.translatable("commands.jjkcraft.technique.equip", id.toString(), slot + 1, players.size()), true);
		return players.size();
	}

	private static int list(CommandSourceStack source, ServerPlayer player) {
		TechniqueState state = TechniqueService.get(player);
		String known = state.known().stream().map(Identifier::toString).collect(Collectors.joining(", "));
		String slots = state.loadout().stream().map(s -> s.map(Identifier::getPath).orElse("-")).collect(Collectors.joining(" | "));
		source.sendSuccess(() -> Component.translatable("commands.jjkcraft.technique.list", player.getDisplayName(),
				known.isEmpty() ? "-" : known, slots), false);
		return state.known().size();
	}

	private static int cast(CommandSourceStack source, Collection<? extends Entity> casters, Identifier id) throws CommandSyntaxException {
		if (!exists(source, id)) {
			return 0;
		}

		int ok = 0;
		for (Entity entity : casters) {
			if (entity instanceof LivingEntity living) {
				CastFailure failure = TechniqueService.cast(living, id, -1);
				if (failure == CastFailure.NONE) {
					ok++;
				} else {
					source.sendFailure(Component.translatable("commands.jjkcraft.technique.cast_failed", entity.getDisplayName(),
							Component.translatable(failure.translationKey())));
				}
			}
		}

		int count = ok;
		source.sendSuccess(() -> Component.translatable("commands.jjkcraft.technique.cast", id.toString(), count), true);
		return ok;
	}

	private static int resetCooldowns(CommandSourceStack source, Collection<ServerPlayer> players) {
		players.forEach(TechniqueService::resetCooldowns);
		source.sendSuccess(() -> Component.translatable("commands.jjkcraft.technique.cooldowns", players.size()), true);
		return players.size();
	}
}
