package com.jjkcraft.block;

import java.util.function.Function;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;

import com.jjkcraft.JjkCraft;

/** Blocks: temporary curse terrain and the curse nest. */
public final class JjkBlocks {
	/** Thorn Matriarch's roots: block paths, then wither away. */
	public static final ExpiringBlock CURSED_ROOT = register("cursed_root", ExpiringBlock::new,
			BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_RED).strength(1.5f, 2f).sound(SoundType.ROOTS)
					.noLootTable().pushReaction(PushReaction.POPPED));
	/** Gravebound Gate's arena wall: nearly unbreakable while it stands. */
	public static final ExpiringBlock GATE_BARRIER = register("gate_barrier", ExpiringBlock::new,
			BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(60f, 1200f).sound(SoundType.DEEPSLATE)
					.noLootTable().pushReaction(PushReaction.IMMOVEABLE).lightLevel(s -> 4));
	/** Culling Warden's Rule Seal: while one stands, the Warden can't die. Breakable; never drops. */
	public static final ExpiringBlock RULE_SEAL = register("rule_seal", ExpiringBlock::new,
			BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(3f, 6f).sound(SoundType.STONE)
					.noLootTable().pushReaction(PushReaction.IMMOVEABLE).lightLevel(s -> 7));
	/** Spawns curses until broken. */
	public static final CurseNestBlock CURSE_NEST = register("curse_nest", CurseNestBlock::new,
			BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(3f, 6f).sound(SoundType.SCULK).lightLevel(s -> 3));

	public static final BlockEntityType<CurseNestBlockEntity> CURSE_NEST_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
			JjkCraft.id("curse_nest"), FabricBlockEntityTypeBuilder.create(CurseNestBlockEntity::new, CURSE_NEST).build());

	public static final com.jjkcraft.training.TrainingBlock TRAINING_MAT=register("training_mat",com.jjkcraft.training.TrainingBlock::new,BlockBehaviour.Properties.of().strength(.8f).sound(SoundType.WOOL).noOcclusion());
    public static final com.jjkcraft.training.TrainingBlock FOCUS_STONE=register("focus_stone",com.jjkcraft.training.TrainingBlock::new,BlockBehaviour.Properties.of().strength(2f).sound(SoundType.STONE).noOcclusion());
    public static final com.jjkcraft.training.TrainingBlock BALANCE_BEAM=register("balance_beam",com.jjkcraft.training.TrainingBlock::new,BlockBehaviour.Properties.of().strength(1.5f).sound(SoundType.WOOD).noOcclusion());
	private JjkBlocks() {
	}

	public static void init() {
		// Loads the class so the static fields register.
	}

	private static <B extends Block> B register(String name, Function<BlockBehaviour.Properties, B> factory, BlockBehaviour.Properties properties) {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, JjkCraft.id(name));
		return Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(properties.setId(key)));
	}
}
