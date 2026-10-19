package com.jjkcraft.modulo.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

import com.jjkcraft.modulo.ModuloBlocks;

/**
 * The Naunax's halls, one chunk at a time (feature type {@code jjkcraft:naunax_deck}): a hull
 * ceiling over the flat deck, square pillars on a grid with a Mul conduit up one face, beams across
 * the ceiling, and conduit lights between them. Everything stays inside the chunk it is placed in.
 *
 * @param floorY   the deck's top block (players stand one above)
 * @param ceilingY the lowest block of the ceiling
 * @param spacing  the pillar grid's spacing in blocks
 */
public record NaunaxDeckFeature(int floorY, int ceilingY, int spacing) implements Feature {
	public static final MapCodec<NaunaxDeckFeature> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.intRange(1, 250).fieldOf("floor_y").forGetter(NaunaxDeckFeature::floorY),
			Codec.intRange(2, 254).fieldOf("ceiling_y").forGetter(NaunaxDeckFeature::ceilingY),
			Codec.intRange(4, 64).optionalFieldOf("spacing", 12).forGetter(NaunaxDeckFeature::spacing)
	).apply(i, NaunaxDeckFeature::new));

	@Override
	public MapCodec<? extends Feature> codec() {
		return CODEC;
	}

	@Override
	public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
		int baseX = origin.getX() & ~15;
		int baseZ = origin.getZ() & ~15;
		BlockState hull = ModuloBlocks.NAUNAX_HULL.defaultBlockState();
		BlockState conduit = ModuloBlocks.MUL_CONDUIT.defaultBlockState();
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int dx = 0; dx < 16; dx++) {
			for (int dz = 0; dz < 16; dz++) {
				int x = baseX + dx;
				int z = baseZ + dz;
				int gx = Math.floorMod(x, spacing);
				int gz = Math.floorMod(z, spacing);
				for (int y = ceilingY; y <= ceilingY + 2; y++) {
					level.setBlock(pos.set(x, y, z), hull, 2);
				}

				boolean pillar = gx < 2 && gz < 2;
				boolean beam = gz < 2 || gx < 2;
				if (pillar) {
					for (int y = floorY + 1; y < ceilingY; y++) {
						level.setBlock(pos.set(x, y, z), gx == 0 && gz == 1 && y % 3 != 0 ? conduit : hull, 2);
					}
				} else if (beam) {
					level.setBlock(pos.set(x, ceilingY - 1, z), hull, 2);
				} else if (gx == spacing / 2 && gz == spacing / 2) {
					level.setBlock(pos.set(x, ceilingY, z), conduit, 2);
				}
			}
		}

		return true;
	}
}
