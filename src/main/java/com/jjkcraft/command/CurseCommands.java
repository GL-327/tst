package com.jjkcraft.command;

import java.util.Collection;
import java.util.List;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import com.jjkcraft.curse.CurseDefinition;
import com.jjkcraft.curse.CurseEntity;
import com.jjkcraft.curse.Curses;
import com.jjkcraft.curse.spawn.CurseSpawner;
import com.jjkcraft.fear.Fear;
import com.jjkcraft.sense.Sensing;

/** {@code /jjk curse ...}, {@code /jjk fear ...} and {@code /jjk sense ...}. */
public final class CurseCommands {
	private static final SuggestionProvider<CommandSourceStack> CURSES = (ctx, builder) ->
			SharedSuggestionProvider.suggestResource(Curses.all(ctx.getSource().registryAccess()).stream().map(Curses.Entry::id), builder);

	private CurseCommands() {
	}

	public static void init() {
		JjkCommands.addSubcommand(CurseCommands::build);
	}

	private static LiteralArgumentBuilder<CommandSourceStack> build(LiteralArgumentBuilder<CommandSourceStack> root) {
		root.then(Commands.literal("curse")
				.then(Commands.literal("spawn")
						.then(Commands.argument("curse", IdentifierArgument.id()).suggests(CURSES)
								.executes(ctx -> spawn(ctx.getSource(), IdentifierArgument.getId(ctx, "curse"), BlockPos.containing(ctx.getSource().getPosition())))
								.then(Commands.argument("pos", BlockPosArgument.blockPos())
										.executes(ctx -> spawn(ctx.getSource(), IdentifierArgument.getId(ctx, "curse"), BlockPosArgument.getSpawnablePos(ctx, "pos"))))))
				.then(Commands.literal("list").executes(ctx -> list(ctx.getSource())))
				.then(Commands.literal("clear")
						.then(Commands.argument("radius", IntegerArgumentType.integer(1, 256))
								.executes(ctx -> clear(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "radius"))))));

		root.then(Commands.literal("fear")
				.then(Commands.literal("get")
						.executes(ctx -> fearGet(ctx.getSource(), BlockPos.containing(ctx.getSource().getPosition())))
						.then(Commands.argument("pos", BlockPosArgument.blockPos())
								.executes(ctx -> fearGet(ctx.getSource(), BlockPosArgument.getLoadedBlockPos(ctx, "pos")))))
				.then(Commands.literal("add")
						.then(Commands.argument("amount", FloatArgumentType.floatArg(-1000, 1000))
								.executes(ctx -> fearAdd(ctx.getSource(), BlockPos.containing(ctx.getSource().getPosition()), FloatArgumentType.getFloat(ctx, "amount")))
								.then(Commands.argument("pos", BlockPosArgument.blockPos())
										.executes(ctx -> fearAdd(ctx.getSource(), BlockPosArgument.getLoadedBlockPos(ctx, "pos"), FloatArgumentType.getFloat(ctx, "amount")))))));

		root.then(Commands.literal("sense")
				.then(Commands.argument("players", EntityArgument.players())
						.then(Commands.literal("on").executes(ctx -> sense(ctx.getSource(), EntityArgument.getPlayers(ctx, "players"), true)))
						.then(Commands.literal("off").executes(ctx -> sense(ctx.getSource(), EntityArgument.getPlayers(ctx, "players"), false)))));
		return root;
	}

	private static int spawn(CommandSourceStack source, Identifier id, BlockPos pos) throws CommandSyntaxException {
		CurseDefinition definition = Curses.get(source.registryAccess(), id).orElse(null);
		if (definition == null) {
			source.sendFailure(Component.translatable("commands.jjkcraft.curse.unknown", id.toString()));
			return 0;
		}

		CurseEntity curse = CurseSpawner.spawnOne(source.getLevel(), definition.entity(), pos);
		if (curse == null) {
			source.sendFailure(Component.translatable("commands.jjkcraft.curse.failed", id.toString()));
			return 0;
		}

		source.sendSuccess(() -> Component.translatable("commands.jjkcraft.curse.spawned", Component.translatable(definition.name()),
				pos.getX(), pos.getY(), pos.getZ()), true);
		return 1;
	}

	private static int list(CommandSourceStack source) {
		List<Curses.Entry> all = Curses.all(source.registryAccess());
		source.sendSuccess(() -> Component.translatable("commands.jjkcraft.curse.list", all.size()), false);
		for (Curses.Entry entry : all) {
			source.sendSuccess(() -> Component.literal(" - " + entry.id() + " (" + entry.definition().grade().getPath() + ")"), false);
		}

		return all.size();
	}

	private static int clear(CommandSourceStack source, int radius) {
		ServerLevel level = source.getLevel();
		List<CurseEntity> curses = level.getEntitiesOfClass(CurseEntity.class,
				new net.minecraft.world.phys.AABB(source.getPosition(), source.getPosition()).inflate(radius), c -> true);
		curses.forEach(c -> c.discard());
		source.sendSuccess(() -> Component.translatable("commands.jjkcraft.curse.cleared", curses.size()), true);
		return curses.size();
	}

	private static int fearGet(CommandSourceStack source, BlockPos pos) {
		ServerLevel level = source.getLevel();
		double total = Fear.at(level, pos);
		double stored = Fear.stored(level, pos);
		double environment = Fear.environment(level, pos);
		source.sendSuccess(() -> Component.translatable("commands.jjkcraft.fear.get", pos.getX(), pos.getY(), pos.getZ(),
				String.format("%.1f", total), String.format("%.1f", stored), String.format("%.1f", environment)), false);
		return (int) Math.round(total);
	}

	private static int fearAdd(CommandSourceStack source, BlockPos pos, float amount) {
		Fear.add(source.getLevel(), pos, amount);
		return fearGet(source, pos);
	}

	private static int sense(CommandSourceStack source, Collection<ServerPlayer> players, boolean on) {
		int changed = 0;
		for (ServerPlayer player : players) {
			Sensing.toggle(player, on);
			if (Sensing.isSensing(player) == on) {
				changed++;
			}
		}

		int count = changed;
		source.sendSuccess(() -> Component.translatable(on ? "commands.jjkcraft.sense.on" : "commands.jjkcraft.sense.off", count), true);
		return count;
	}
}
