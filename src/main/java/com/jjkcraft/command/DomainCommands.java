package com.jjkcraft.command;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

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

import com.jjkcraft.core.domain.DomainMath;
import com.jjkcraft.domain.DomainService;
import com.jjkcraft.domain.Domains;
import com.jjkcraft.technique.CastFailure;

/** {@code /jjk domain ...}: grant, revoke, expand (forced), close, Simple Domain, burnout, list. */
public final class DomainCommands {
	private static final SuggestionProvider<CommandSourceStack> DOMAINS = (ctx, builder) ->
			SharedSuggestionProvider.suggestResource(Domains.all(ctx.getSource().registryAccess()).stream().map(Domains.Entry::id), builder);

	private DomainCommands() {
	}

	public static void init() {
		JjkCommands.addSubcommand(DomainCommands::build);
	}

	private static LiteralArgumentBuilder<CommandSourceStack> build(LiteralArgumentBuilder<CommandSourceStack> root) {
		root.then(Commands.literal("domain")
				.then(Commands.literal("grant")
						.then(Commands.argument("players", EntityArgument.players())
								.then(Commands.argument("domain", IdentifierArgument.id()).suggests(DOMAINS)
										.executes(ctx -> grant(ctx.getSource(), EntityArgument.getPlayers(ctx, "players"), IdentifierArgument.getId(ctx, "domain"))))))
				.then(Commands.literal("revoke")
						.then(Commands.argument("players", EntityArgument.players())
								.executes(ctx -> revoke(ctx.getSource(), EntityArgument.getPlayers(ctx, "players")))))
				.then(Commands.literal("expand")
						.then(Commands.argument("casters", EntityArgument.entities())
								.then(Commands.argument("domain", IdentifierArgument.id()).suggests(DOMAINS)
										.executes(ctx -> expand(ctx.getSource(), EntityArgument.getEntities(ctx, "casters"), IdentifierArgument.getId(ctx, "domain"),
												DomainMath.Variant.FULL))
										.then(Commands.literal("barrierless")
												.executes(ctx -> expand(ctx.getSource(), EntityArgument.getEntities(ctx, "casters"), IdentifierArgument.getId(ctx, "domain"),
														DomainMath.Variant.BARRIERLESS))))))
				.then(Commands.literal("close")
						.then(Commands.argument("casters", EntityArgument.entities())
								.executes(ctx -> close(ctx.getSource(), EntityArgument.getEntities(ctx, "casters")))))
				.then(Commands.literal("simple")
						.then(Commands.argument("players", EntityArgument.players())
								.executes(ctx -> simple(ctx.getSource(), EntityArgument.getPlayers(ctx, "players")))))
				.then(Commands.literal("burnout")
						.then(Commands.literal("clear")
								.then(Commands.argument("players", EntityArgument.players())
										.executes(ctx -> clearBurnout(ctx.getSource(), EntityArgument.getPlayers(ctx, "players"))))))
				.then(Commands.literal("list").executes(ctx -> list(ctx.getSource()))));
		return root;
	}

	private static int grant(CommandSourceStack source, Collection<ServerPlayer> players, Identifier id) {
		if (Domains.get(source.registryAccess(), id).isEmpty()) {
			source.sendFailure(Component.translatable("commands.jjkcraft.domain.unknown", id.toString()));
			return 0;
		}

		players.forEach(p -> DomainService.setState(p, DomainService.state(p).withKnown(Optional.of(id))));
		source.sendSuccess(() -> Component.translatable("commands.jjkcraft.domain.grant", id.toString(), players.size()), true);
		return players.size();
	}

	private static int revoke(CommandSourceStack source, Collection<ServerPlayer> players) {
		players.forEach(p -> {
			DomainService.close(p);
			DomainService.setState(p, DomainService.state(p).withKnown(Optional.empty()));
		});
		source.sendSuccess(() -> Component.translatable("commands.jjkcraft.domain.revoke", players.size()), true);
		return players.size();
	}

	private static int expand(CommandSourceStack source, Collection<? extends Entity> casters, Identifier id, DomainMath.Variant variant) {
		int opened = 0;
		for (Entity entity : casters) {
			if (entity instanceof LivingEntity caster) {
				CastFailure failure = DomainService.expand(caster, id, variant, true);
				if (failure == CastFailure.NONE) {
					opened++;
				} else {
					source.sendFailure(Component.translatable("commands.jjkcraft.domain.refused", entity.getDisplayName(),
							Component.translatable(failure.translationKey())));
				}
			}
		}

		int count = opened;
		source.sendSuccess(() -> Component.translatable("commands.jjkcraft.domain.expanded", id.toString(), count), true);
		return opened;
	}

	private static int close(CommandSourceStack source, Collection<? extends Entity> casters) {
		int closed = 0;
		for (Entity entity : casters) {
			if (entity instanceof LivingEntity caster && DomainService.active(caster).isPresent()) {
				DomainService.close(caster);
				closed++;
			}
		}

		int count = closed;
		source.sendSuccess(() -> Component.translatable("commands.jjkcraft.domain.closed", count), true);
		return closed;
	}

	private static int simple(CommandSourceStack source, Collection<ServerPlayer> players) {
		int toggled = 0;
		for (ServerPlayer player : players) {
			if (DomainService.toggleSimple(player) == CastFailure.NONE) {
				toggled++;
			}
		}

		int count = toggled;
		source.sendSuccess(() -> Component.translatable("commands.jjkcraft.domain.simple", count), true);
		return toggled;
	}

	private static int clearBurnout(CommandSourceStack source, Collection<ServerPlayer> players) {
		players.forEach(p -> DomainService.setState(p, DomainService.state(p).withBurnoutUntil(0)));
		source.sendSuccess(() -> Component.translatable("commands.jjkcraft.domain.burnout_cleared", players.size()), true);
		return players.size();
	}

	private static int list(CommandSourceStack source) {
		List<Domains.Entry> all = Domains.all(source.registryAccess());
		source.sendSuccess(() -> Component.translatable("commands.jjkcraft.domain.list", all.size(), DomainService.all().size()), false);
		for (Domains.Entry entry : all) {
			source.sendSuccess(() -> Component.literal(" - " + entry.id() + " (" + entry.definition().grade().getPath() + ", r"
					+ entry.definition().radius() + ")"), false);
		}

		return all.size();
	}
}
