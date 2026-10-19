package com.jjkcraft.modulo;

import java.util.function.Function;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import com.jjkcraft.JjkCraft;

/** The Naunax's blocks (docs/MODULO.md 3.5). Original names and art: canon describes none of them except Mul. */
public final class ModuloBlocks {
	/** Mul: the Simurian ore that converts rolloluca (cursed energy) into energy. Veins in the Naunax's hull. */
	public static final Block MUL_ORE = register("mul_ore", properties -> new DropExperienceBlock(UniformInt.of(2, 5), properties),
			BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GREEN).requiresCorrectToolForDrops().strength(4f, 6f)
					.sound(SoundType.AMETHYST).lightLevel(s -> 5));
	/** The ship's hull: what the Naunax's decks are built from. */
	public static final Block NAUNAX_HULL = register("naunax_hull", Block::new,
			BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_WHITE).requiresCorrectToolForDrops().strength(5f, 9f).sound(SoundType.METAL));
	/** Deck plating, the floor of the Naunax's halls. */
	public static final Block NAUNAX_DECK_PLATE = register("naunax_deck_plate", Block::new,
			BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY).requiresCorrectToolForDrops().strength(5f, 9f).sound(SoundType.METAL));
	/** A conduit of glowing Mul running along the hull: the ship's light. */
	public static final Block MUL_CONDUIT = register("mul_conduit", Block::new,
			BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GREEN).strength(3f, 6f).sound(SoundType.GLASS).lightLevel(s -> 14));
	/** Dabura's solid light (canon: Light "solidified" into constructs): pins, shelters, and fades. Breakable; never drops. */
	public static final com.jjkcraft.block.ExpiringBlock HARD_LIGHT = register("hard_light", com.jjkcraft.block.ExpiringBlock::new,
			BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(1.5f, 6f).noLootTable().noOcclusion()
					.sound(SoundType.GLASS).lightLevel(s -> 12).pushReaction(net.minecraft.world.level.material.PushReaction.IMMOVEABLE));
	/** Travel pads at mission sites (to Earth, to Tokyo 2086, to the ship). Unbreakable: sites need them. */
	public static final NaunaxGateBlock NAUNAX_GATE = register("naunax_gate", NaunaxGateBlock::new,
			BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GREEN).strength(-1f, 3_600_000f).noLootTable().noOcclusion()
					.sound(SoundType.AMETHYST).lightLevel(s -> 15));

	private ModuloBlocks() {
	}

	public static void init() {
		// Loads the class so the static fields register.
	}

	private static <B extends Block> B register(String name, Function<BlockBehaviour.Properties, B> factory, BlockBehaviour.Properties properties) {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, JjkCraft.id(name));
		return Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(properties.setId(key)));
	}
}
