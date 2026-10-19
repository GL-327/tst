package com.jjkcraft.modulo.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * Tokyo 2086, one chunk at a time (feature type {@code jjkcraft:city_block}): streets on the chunk
 * grid, a pavement ring, and one building per block. West of {@code kyotoBoundaryX} is the Kyoto
 * region (low wooden and plaster buildings, some yards); east is Tokyo, where the Inhuman Makyo's
 * ruined towers stand broken among rubble. Everything stays inside its own chunk.
 *
 * @param groundY        the ground's top block (players stand one above)
 * @param kyotoBoundaryX west of this x is Kyoto
 * @param maxHeight      tallest Tokyo ruin, in blocks above the ground
 */
public record CityBlockFeature(int groundY, int kyotoBoundaryX, int maxHeight) implements Feature {
	public static final MapCodec<CityBlockFeature> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.intRange(1, 250).fieldOf("ground_y").forGetter(CityBlockFeature::groundY),
			Codec.INT.optionalFieldOf("kyoto_boundary_x", 0).forGetter(CityBlockFeature::kyotoBoundaryX),
			Codec.intRange(8, 120).optionalFieldOf("max_height", 40).forGetter(CityBlockFeature::maxHeight)
	).apply(i, CityBlockFeature::new));

	@Override
	public MapCodec<? extends Feature> codec() {
		return CODEC;
	}

	@Override
	public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
		int baseX = origin.getX() & ~15;
		int baseZ = origin.getZ() & ~15;
		boolean kyoto = baseX + 8 < kyotoBoundaryX;
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

		// Streets on the chunk edges, pavement inside them.
		BlockState asphalt = Blocks.CONCRETE.black().defaultBlockState();
		BlockState line = Blocks.CONCRETE.white().defaultBlockState();
		BlockState pavement = kyoto ? Blocks.STONE_BRICKS.defaultBlockState() : Blocks.CONCRETE.lightGray().defaultBlockState();
		for (int dx = 0; dx < 16; dx++) {
			for (int dz = 0; dz < 16; dz++) {
				boolean road = dx < 2 || dz < 2;
				BlockState top = road ? ((dx == 0 && (baseZ + dz) % 4 < 2) || (dz == 0 && (baseX + dx) % 4 < 2) ? line : asphalt) : pavement;
				if (!kyoto && road && random.nextFloat() < 0.08f) {
					top = Blocks.GRAVEL.defaultBlockState();
				}

				level.setBlock(pos.set(baseX + dx, groundY, baseZ + dz), top, 2);
			}
		}

		if (kyoto) {
			if (random.nextFloat() < 0.25f) {
				yard(level, random, baseX, baseZ, pos);
			} else {
				kyotoHouse(level, random, baseX, baseZ, pos);
			}
		} else {
			ruin(level, random, baseX, baseZ, pos);
		}

		return true;
	}

	/** A walled yard with grass, a stone lantern and a tree of leaves. */
	private void yard(WorldGenLevel level, RandomSource random, int baseX, int baseZ, BlockPos.MutableBlockPos pos) {
		for (int dx = 3; dx <= 12; dx++) {
			for (int dz = 3; dz <= 12; dz++) {
				level.setBlock(pos.set(baseX + dx, groundY, baseZ + dz), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
				if (dx == 3 || dx == 12 || dz == 3 || dz == 12) {
					level.setBlock(pos.set(baseX + dx, groundY + 1, baseZ + dz), Blocks.MUD_BRICKS.defaultBlockState(), 2);
				}
			}
		}

		level.setBlock(pos.set(baseX + 5, groundY + 1, baseZ + 5), Blocks.STONE_BRICK_WALL.defaultBlockState(), 2);
		level.setBlock(pos.set(baseX + 5, groundY + 2, baseZ + 5), Blocks.LANTERN.defaultBlockState(), 2);
		int tx = baseX + 9;
		int tz = baseZ + 9;
		for (int y = 1; y <= 4; y++) {
			level.setBlock(pos.set(tx, groundY + y, tz), Blocks.CHERRY_LOG.defaultBlockState(), 2);
		}

		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				for (int dy = 4; dy <= 6; dy++) {
					if (Math.abs(dx) + Math.abs(dz) + Math.abs(dy - 5) <= 3 && random.nextFloat() < 0.9f) {
						BlockPos leaf = pos.set(tx + dx, groundY + dy, tz + dz);
						if (level.getBlockState(leaf).isAir()) {
							level.setBlock(leaf, Blocks.CHERRY_LEAVES.defaultBlockState(), 2);
						}
					}
				}
			}
		}
	}

	/** A low plaster-and-timber building with a dark tiled roof. */
	private void kyotoHouse(WorldGenLevel level, RandomSource random, int baseX, int baseZ, BlockPos.MutableBlockPos pos) {
		int height = 4 + random.nextInt(5);
		BlockState plaster = Blocks.DYED_TERRACOTTA.white().defaultBlockState();
		BlockState timber = Blocks.DARK_OAK_LOG.defaultBlockState();
		BlockState window = Blocks.STAINED_GLASS.brown().defaultBlockState();
		BlockState roof = Blocks.DEEPSLATE_TILES.defaultBlockState();
		for (int dx = 3; dx <= 12; dx++) {
			for (int dz = 3; dz <= 12; dz++) {
				boolean wall = dx == 3 || dx == 12 || dz == 3 || dz == 12;
				boolean corner = (dx == 3 || dx == 12) && (dz == 3 || dz == 12);
				for (int y = 1; y <= height; y++) {
					if (wall) {
						BlockState block = corner ? timber : (y % 3 == 2 && (dx + dz) % 3 == 0 ? window : plaster);
						level.setBlock(pos.set(baseX + dx, groundY + y, baseZ + dz), block, 2);
					}
				}

				level.setBlock(pos.set(baseX + dx, groundY + height + 1, baseZ + dz), roof, 2);
			}
		}

		// A door on the street side.
		level.setBlock(pos.set(baseX + 7, groundY + 1, baseZ + 3), Blocks.AIR.defaultBlockState(), 2);
		level.setBlock(pos.set(baseX + 7, groundY + 2, baseZ + 3), Blocks.AIR.defaultBlockState(), 2);
	}

	/** A broken concrete tower of the Inhuman Makyo: walls with holes, a jagged top, rubble around. */
	private void ruin(WorldGenLevel level, RandomSource random, int baseX, int baseZ, BlockPos.MutableBlockPos pos) {
		if (random.nextFloat() < 0.2f) {
			// An empty lot of rubble.
			for (int i = 0; i < 18; i++) {
				int x = baseX + 3 + random.nextInt(10);
				int z = baseZ + 3 + random.nextInt(10);
				level.setBlock(pos.set(x, groundY + 1, z), random.nextBoolean() ? Blocks.COBBLESTONE.defaultBlockState() : Blocks.GRAVEL.defaultBlockState(), 2);
			}

			return;
		}

		int height = 10 + random.nextInt(Math.max(1, maxHeight - 9));
		BlockState concrete = random.nextBoolean() ? Blocks.CONCRETE.gray().defaultBlockState() : Blocks.CONCRETE.lightGray().defaultBlockState();
		BlockState glass = Blocks.STAINED_GLASS.gray().defaultBlockState();
		BlockState cracked = Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
		for (int dx = 3; dx <= 12; dx++) {
			for (int dz = 3; dz <= 12; dz++) {
				boolean wall = dx == 3 || dx == 12 || dz == 3 || dz == 12;
				int top = height - (int) (random.nextFloat() * 6) - ((dx + dz) % 5 == 0 ? random.nextInt(6) : 0);
				for (int y = 1; y <= top; y++) {
					if (wall) {
						if (random.nextFloat() < 0.07f) {
							continue;
						}

						BlockState block = y % 4 == 2 && (dx + dz) % 2 == 0 ? glass : (random.nextFloat() < 0.1f ? cracked : concrete);
						level.setBlock(pos.set(baseX + dx, groundY + y, baseZ + dz), block, 2);
					} else if (y % 4 == 0 && random.nextFloat() < 0.85f) {
						level.setBlock(pos.set(baseX + dx, groundY + y, baseZ + dz), concrete, 2);
					}
				}
			}
		}

		for (int i = 0; i < 6; i++) {
			int x = baseX + 2 + random.nextInt(12);
			int z = baseZ + 2 + random.nextInt(12);
			BlockPos at = pos.set(x, groundY + 1, z);
			if (level.getBlockState(at).isAir()) {
				level.setBlock(at, Blocks.COBBLESTONE.defaultBlockState(), 2);
			}
		}
	}
}
