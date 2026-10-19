package com.jjkcraft.modulo.world;

import java.util.Map;
import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import com.jjkcraft.JjkCraft;
import com.jjkcraft.modulo.Modulo;
import com.jjkcraft.modulo.ModuloBlocks;
import com.jjkcraft.modulo.ModuloState;
import com.jjkcraft.modulo.NaunaxGateBlock;
import com.jjkcraft.modulo.data.ModuloRegistries;
import com.jjkcraft.modulo.data.SiteDefinition;

/**
 * Raises mission sites (docs/MODULO.md 3.7): clears the ground inside the site's radius and builds
 * its layout, then places gate pads at its {@code gate_earth}, {@code gate_tokyo} and
 * {@code gate_ship} markers. Each site is built once per world. The art is programmer art.
 */
public final class SiteBuilder {
	private static final int FLAGS = Block.UPDATE_CLIENTS;

	private SiteBuilder() {
	}

	/** The site, built first if it hasn't been. Empty if the id is unknown or its dimension isn't loaded. */
	public static Optional<SiteDefinition> ensure(MinecraftServer server, Identifier id) {
		Optional<SiteDefinition> site = ModuloRegistries.site(server.registryAccess(), id);
		if (site.isEmpty()) {
			return Optional.empty();
		}

		ModuloState state = Modulo.state(server);
		if (!state.builtSites.contains(id)) {
			ServerLevel level = server.getLevel(site.get().dimension());
			if (level == null) {
				JjkCraft.LOGGER.warn("Modulo site {} is in {}, which is not loaded", id, site.get().dimension().identifier());
				return Optional.empty();
			}

			build(level, site.get(), RandomSource.create(id.hashCode()));
			state.builtSites.add(id);
			state.setDirty();
		}

		return site;
	}

	public static void build(ServerLevel level, SiteDefinition site, RandomSource random) {
		BlockPos c = site.center();
		int r = site.radius();
		boolean ship = level.dimension() == Modulo.NAUNAX;
		int clearTop = ship ? c.getY() + 11 : c.getY() + 48;
		BlockState floor = ship ? ModuloBlocks.NAUNAX_DECK_PLATE.defaultBlockState() : Blocks.SMOOTH_STONE.defaultBlockState();
		for (int dx = -r; dx <= r; dx++) {
			for (int dz = -r; dz <= r; dz++) {
				if (dx * dx + dz * dz > r * r) {
					continue;
				}

				for (int y = c.getY(); y <= clearTop; y++) {
					set(level, c.getX() + dx, y, c.getZ() + dz, Blocks.AIR.defaultBlockState());
				}

				set(level, c.getX() + dx, c.getY() - 1, c.getZ() + dz, floor);
			}
		}

		switch (site.layout()) {
			case "deck_hall" -> deckHall(level, c, r);
			case "mul_core" -> mulCore(level, c, r);
			case "meeting_room" -> meetingRoom(level, c, r);
			case "plaza" -> plaza(level, c, r);
			case "alley" -> alley(level, site, c, r);
			case "warehouse" -> warehouse(level, c, r, random);
			case "river" -> river(level, c, r, random);
			case "school" -> school(level, c, r);
			case "beach" -> beach(level, c, r);
			case "ruins" -> ruins(level, c, r, random);
			case "arena" -> arena(level, c, r);
			default -> {
			}
		}

		for (Map.Entry<String, NaunaxGateBlock.Destination> gate : Map.of("gate_earth", NaunaxGateBlock.Destination.EARTH,
				"gate_tokyo", NaunaxGateBlock.Destination.TOKYO, "gate_ship", NaunaxGateBlock.Destination.SHIP).entrySet()) {
			if (site.markers().containsKey(gate.getKey())) {
				gatePad(level, site.marker(gate.getKey()), gate.getValue());
			}
		}
	}

	private static void set(ServerLevel level, int x, int y, int z, BlockState state) {
		level.setBlock(new BlockPos(x, y, z), state, FLAGS);
	}

	private static void fill(ServerLevel level, BlockPos from, BlockPos to, BlockState state) {
		for (BlockPos pos : BlockPos.betweenClosed(from, to)) {
			level.setBlock(pos, state, FLAGS);
		}
	}

	/** A hollow box: walls from {@code from} to {@code to} (inclusive), no roof. */
	private static void walls(ServerLevel level, BlockPos from, BlockPos to, BlockState state) {
		for (BlockPos pos : BlockPos.betweenClosed(from, to)) {
			if (pos.getX() == from.getX() || pos.getX() == to.getX() || pos.getZ() == from.getZ() || pos.getZ() == to.getZ()) {
				level.setBlock(pos, state, FLAGS);
			}
		}
	}

	/** A 3x3 gate pad with a ring of conduit around it. */
	private static void gatePad(ServerLevel level, BlockPos at, NaunaxGateBlock.Destination destination) {
		BlockState pad = ModuloBlocks.NAUNAX_GATE.defaultBlockState().setValue(NaunaxGateBlock.DESTINATION, destination);
		BlockState rim = ModuloBlocks.MUL_CONDUIT.defaultBlockState();
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				boolean inner = Math.abs(dx) <= 1 && Math.abs(dz) <= 1;
				set(level, at.getX() + dx, at.getY() - 1, at.getZ() + dz, inner ? ModuloBlocks.NAUNAX_HULL.defaultBlockState() : rim);
				if (inner) {
					set(level, at.getX() + dx, at.getY(), at.getZ() + dz, pad);
				}
			}
		}
	}

	private static void deckHall(ServerLevel level, BlockPos c, int r) {
		BlockState conduit = ModuloBlocks.MUL_CONDUIT.defaultBlockState();
		for (int dx = -r; dx <= r; dx++) {
			for (int dz = -r; dz <= r; dz++) {
				int d2 = dx * dx + dz * dz;
				if (d2 <= r * r && d2 > (r - 1) * (r - 1)) {
					set(level, c.getX() + dx, c.getY() - 1, c.getZ() + dz, conduit);
				}
			}
		}

		fill(level, c.offset(-2, 0, -2), c.offset(2, 0, 2), ModuloBlocks.NAUNAX_HULL.defaultBlockState());
		set(level, c.getX(), c.getY() + 1, c.getZ(), conduit);
	}

	private static void mulCore(ServerLevel level, BlockPos c, int r) {
		fill(level, c.offset(-1, 0, -1), c.offset(1, 10, 1), ModuloBlocks.MUL_CONDUIT.defaultBlockState());
		for (int dx = -5; dx <= 5; dx++) {
			for (int dz = -5; dz <= 5; dz++) {
				int d2 = dx * dx + dz * dz;
				if (d2 >= 12 && d2 <= 25 && (dx + dz) % 2 == 0) {
					fill(level, c.offset(dx, 0, dz), c.offset(dx, (dx * dz) % 3 == 0 ? 1 : 0, dz), ModuloBlocks.MUL_ORE.defaultBlockState());
				}
			}
		}
	}

	private static void meetingRoom(ServerLevel level, BlockPos c, int r) {
		int h = Math.min(r - 1, 8);
		walls(level, c.offset(-h, 0, -h), c.offset(h, 6, h), ModuloBlocks.NAUNAX_HULL.defaultBlockState());
		fill(level, c.offset(-h, 7, -h), c.offset(h, 7, h), ModuloBlocks.NAUNAX_HULL.defaultBlockState());
		fill(level, c.offset(-1, 7, -1), c.offset(1, 7, 1), ModuloBlocks.MUL_CONDUIT.defaultBlockState());
		fill(level, c.offset(-3, 0, -1), c.offset(3, 0, 1), ModuloBlocks.NAUNAX_HULL.defaultBlockState());
		fill(level, c.offset(0, 0, -h), c.offset(0, 2, -h), Blocks.AIR.defaultBlockState());
	}

	/** Kyoto Jujutsu High's yard: gravel and stone, a red gate, a timber hall. */
	private static void plaza(ServerLevel level, BlockPos c, int r) {
		for (int dx = -r; dx <= r; dx++) {
			for (int dz = -r; dz <= r; dz++) {
				if (dx * dx + dz * dz <= r * r) {
					set(level, c.getX() + dx, c.getY() - 1, c.getZ() + dz, Math.abs(dx) <= 1 ? Blocks.STONE_BRICKS.defaultBlockState() : Blocks.GRAVEL.defaultBlockState());
				}
			}
		}

		// The gate (torii-like), across the path south of the centre.
		int gz = c.getZ() + r - 3;
		for (int x : new int[] {c.getX() - 3, c.getX() + 3}) {
			fill(level, new BlockPos(x, c.getY(), gz), new BlockPos(x, c.getY() + 5, gz), Blocks.DYED_TERRACOTTA.red().defaultBlockState());
		}

		fill(level, new BlockPos(c.getX() - 4, c.getY() + 5, gz), new BlockPos(c.getX() + 4, c.getY() + 5, gz), Blocks.DARK_OAK_PLANKS.defaultBlockState());
		fill(level, new BlockPos(c.getX() - 5, c.getY() + 6, gz), new BlockPos(c.getX() + 5, c.getY() + 6, gz), Blocks.DYED_TERRACOTTA.red().defaultBlockState());

		// The hall, north of the centre.
		BlockPos h0 = c.offset(-6, 0, -r + 2);
		BlockPos h1 = c.offset(6, 5, -r + 9);
		walls(level, h0, h1, Blocks.DYED_TERRACOTTA.white().defaultBlockState());
		fill(level, h0.offset(-1, 6, -1), new BlockPos(h1.getX() + 1, h1.getY() + 1, h1.getZ() + 1), Blocks.DEEPSLATE_TILES.defaultBlockState());
		fill(level, new BlockPos(c.getX() - 1, c.getY(), h1.getZ()), new BlockPos(c.getX() + 1, c.getY() + 2, h1.getZ()), Blocks.AIR.defaultBlockState());
		for (BlockPos corner : new BlockPos[] {h0, new BlockPos(h1.getX(), h0.getY(), h0.getZ()), new BlockPos(h0.getX(), h0.getY(), h1.getZ()),
				new BlockPos(h1.getX(), h0.getY(), h1.getZ())}) {
			fill(level, corner, corner.above(5), Blocks.DARK_OAK_LOG.defaultBlockState());
		}
	}

	/** A narrow Kyoto street between two walls, with lanterns where the markers are. */
	private static void alley(ServerLevel level, SiteDefinition site, BlockPos c, int r) {
		for (int dx = -r; dx <= r; dx++) {
			for (int side : new int[] {-3, 3}) {
				fill(level, new BlockPos(c.getX() + dx, c.getY(), c.getZ() + side), new BlockPos(c.getX() + dx, c.getY() + 4, c.getZ() + side),
						(dx & 3) == 0 ? Blocks.DARK_OAK_LOG.defaultBlockState() : Blocks.DYED_TERRACOTTA.white().defaultBlockState());
			}

			for (int dz = -2; dz <= 2; dz++) {
				set(level, c.getX() + dx, c.getY() - 1, c.getZ() + dz, Blocks.STONE_BRICKS.defaultBlockState());
			}
		}

		for (Map.Entry<String, BlockPos> marker : site.markers().entrySet()) {
			if (marker.getKey().startsWith("path")) {
				BlockPos at = c.offset(marker.getValue());
				set(level, at.getX(), at.getY() - 1, at.getZ(), Blocks.SEA_LANTERN.defaultBlockState());
			}
		}
	}

	/** The trafficker's warehouse: a concrete shed papered with talismans, crates inside. */
	private static void warehouse(ServerLevel level, BlockPos c, int r, RandomSource random) {
		int hx = Math.min(r - 2, 9);
		int hz = Math.min(r - 2, 6);
		walls(level, c.offset(-hx, 0, -hz), c.offset(hx, 6, hz), Blocks.CONCRETE.gray().defaultBlockState());
		fill(level, c.offset(-hx, 7, -hz), c.offset(hx, 7, hz), Blocks.IRON_BLOCK.defaultBlockState());
		fill(level, c.offset(-1, 0, hz), c.offset(1, 3, hz), Blocks.AIR.defaultBlockState());
		for (int i = 0; i < 24; i++) {
			int x = c.getX() - hx + 1 + random.nextInt(hx * 2 - 1);
			int y = c.getY() + 1 + random.nextInt(5);
			boolean northSouth = random.nextBoolean();
			BlockPos talisman = northSouth ? new BlockPos(x, y, c.getZ() + (random.nextBoolean() ? -hz + 1 : hz - 1))
					: new BlockPos(c.getX() + (random.nextBoolean() ? -hx + 1 : hx - 1), y, c.getZ() - hz + 1 + random.nextInt(hz * 2 - 1));
			level.setBlock(talisman, Blocks.WOOL.white().defaultBlockState(), FLAGS);
		}

		for (int i = 0; i < 4; i++) {
			set(level, c.getX() - hx + 2 + i * 2, c.getY(), c.getZ() - hz + 2, Blocks.BARREL.defaultBlockState());
		}
	}

	/** The dried river below Gifu's restored solar plant, with forest on its banks. */
	private static void river(ServerLevel level, BlockPos c, int r, RandomSource random) {
		for (int dx = -r; dx <= r; dx++) {
			for (int dz = -r; dz <= r; dz++) {
				if (dx * dx + dz * dz > r * r) {
					continue;
				}

				boolean bed = Math.abs(dx) <= 2;
				set(level, c.getX() + dx, c.getY() - 1, c.getZ() + dz, bed ? (random.nextBoolean() ? Blocks.COARSE_DIRT.defaultBlockState() : Blocks.GRAVEL.defaultBlockState())
						: Blocks.PODZOL.defaultBlockState());
				if (bed) {
					set(level, c.getX() + dx, c.getY() - 2, c.getZ() + dz, Blocks.GRAVEL.defaultBlockState());
				} else if (Math.abs(dx) > 5 && random.nextFloat() < 0.03f) {
					tree(level, c.offset(dx, 0, dz));
				}
			}
		}

		// The solar plant on the slope to the east.
		int px = c.getX() + r - 5;
		for (int dz = -6; dz <= 6; dz += 2) {
			for (int dx = 0; dx < 4; dx++) {
				set(level, px + dx, c.getY(), c.getZ() + dz, Blocks.STONE_BRICKS.defaultBlockState());
				set(level, px + dx, c.getY() + 1, c.getZ() + dz, Blocks.DAYLIGHT_DETECTOR.defaultBlockState());
			}
		}
	}

	private static void tree(ServerLevel level, BlockPos base) {
		fill(level, base, base.above(4), Blocks.SPRUCE_LOG.defaultBlockState());
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				for (int dy = 3; dy <= 6; dy++) {
					if (Math.abs(dx) + Math.abs(dz) <= 6 - dy + 1 && !(dx == 0 && dz == 0 && dy <= 4)) {
						level.setBlock(base.offset(dx, dy, dz), Blocks.SPRUCE_LEAVES.defaultBlockState(), FLAGS);
					}
				}
			}
		}
	}

	/** Shuei Elementary: a two-storey school, a gym and a pool on a gravel yard. */
	private static void school(ServerLevel level, BlockPos c, int r) {
		BlockPos s0 = c.offset(-12, 0, -r + 2);
		BlockPos s1 = c.offset(4, 8, -r + 9);
		walls(level, s0, s1, Blocks.CONCRETE.white().defaultBlockState());
		fill(level, s0.offset(0, 4, 0), new BlockPos(s1.getX(), s0.getY() + 4, s1.getZ()), Blocks.CONCRETE.white().defaultBlockState());
		fill(level, s0.offset(0, 9, 0), new BlockPos(s1.getX(), s0.getY() + 9, s1.getZ()), Blocks.CONCRETE.lightGray().defaultBlockState());
		for (int x = s0.getX() + 2; x < s1.getX(); x += 3) {
			for (int y : new int[] {2, 6}) {
				set(level, x, s0.getY() + y, s1.getZ(), Blocks.STAINED_GLASS.lightBlue().defaultBlockState());
			}
		}

		fill(level, new BlockPos(c.getX() - 4, c.getY(), s1.getZ()), new BlockPos(c.getX() - 3, c.getY() + 2, s1.getZ()), Blocks.AIR.defaultBlockState());

		// The gym, west.
		BlockPos g0 = c.offset(-r + 2, 0, 2);
		BlockPos g1 = c.offset(-r + 12, 7, 12);
		walls(level, g0, g1, Blocks.CONCRETE.lightGray().defaultBlockState());
		fill(level, g0.offset(0, 8, 0), g1.above(1), Blocks.CONCRETE.lightGray().defaultBlockState());
		fill(level, new BlockPos(g1.getX(), c.getY(), c.getZ() + 6), new BlockPos(g1.getX(), c.getY() + 2, c.getZ() + 7), Blocks.AIR.defaultBlockState());

		// The pool, east.
		for (int dx = 6; dx <= 13; dx++) {
			for (int dz = 2; dz <= 8; dz++) {
				boolean rim = dx == 6 || dx == 13 || dz == 2 || dz == 8;
				for (int dy = 1; dy <= 3; dy++) {
					set(level, c.getX() + dx, c.getY() - dy, c.getZ() + dz, rim ? Blocks.QUARTZ_BLOCK.defaultBlockState() : Blocks.WATER.defaultBlockState());
				}

				set(level, c.getX() + dx, c.getY() - 4, c.getZ() + dz, Blocks.QUARTZ_BLOCK.defaultBlockState());
			}
		}
	}

	/** The beach where wancha octopi live: sand, a sheltered pool of sea water, a beach house. */
	private static void beach(ServerLevel level, BlockPos c, int r) {
		int water = Math.max(4, r / 2);
		for (int dx = -r; dx <= r; dx++) {
			for (int dz = -r; dz <= r; dz++) {
				int d2 = dx * dx + dz * dz;
				if (d2 > r * r) {
					continue;
				}

				int wx = dx - r / 3;
				boolean sea = wx * wx + dz * dz <= water * water;
				set(level, c.getX() + dx, c.getY() - 1, c.getZ() + dz, sea ? Blocks.WATER.defaultBlockState() : Blocks.SAND.defaultBlockState());
				if (sea) {
					for (int dy = 2; dy <= 5; dy++) {
						set(level, c.getX() + dx, c.getY() - dy, c.getZ() + dz, dy == 5 ? Blocks.SAND.defaultBlockState() : Blocks.WATER.defaultBlockState());
					}
				}
			}
		}

		BlockPos h0 = c.offset(-r + 2, 0, -4);
		BlockPos h1 = c.offset(-r + 8, 4, 2);
		walls(level, h0, h1, Blocks.OAK_PLANKS.defaultBlockState());
		fill(level, h0.above(5), h1.above(1), Blocks.SPRUCE_PLANKS.defaultBlockState());
		fill(level, new BlockPos(h1.getX(), c.getY(), c.getZ() - 1), new BlockPos(h1.getX(), c.getY() + 2, c.getZ()), Blocks.AIR.defaultBlockState());
	}

	/** The Inhuman Makyo: cracked ground, broken pillars, rubble. */
	private static void ruins(ServerLevel level, BlockPos c, int r, RandomSource random) {
		for (int dx = -r; dx <= r; dx++) {
			for (int dz = -r; dz <= r; dz++) {
				if (dx * dx + dz * dz <= r * r) {
					float roll = random.nextFloat();
					BlockState ground = roll < 0.15f ? Blocks.GRAVEL.defaultBlockState() : roll < 0.3f ? Blocks.COBBLESTONE.defaultBlockState()
							: roll < 0.4f ? Blocks.CRACKED_STONE_BRICKS.defaultBlockState() : Blocks.CONCRETE.gray().defaultBlockState();
					set(level, c.getX() + dx, c.getY() - 1, c.getZ() + dz, ground);
				}
			}
		}

		for (int i = 0; i < Math.max(4, r / 4); i++) {
			double angle = random.nextDouble() * Math.PI * 2;
			double dist = r * (0.5 + random.nextDouble() * 0.4);
			BlockPos base = c.offset((int) (Math.cos(angle) * dist), 0, (int) (Math.sin(angle) * dist));
			int height = 3 + random.nextInt(9);
			fill(level, base, base.offset(1, height, 1), random.nextBoolean() ? Blocks.CONCRETE.gray().defaultBlockState() : Blocks.CRACKED_STONE_BRICKS.defaultBlockState());
		}
	}

	/** A round arena with a ceiling, for fights that turn gravity over. */
	private static void arena(ServerLevel level, BlockPos c, int r) {
		int height = 14;
		for (int dx = -r; dx <= r; dx++) {
			for (int dz = -r; dz <= r; dz++) {
				int d2 = dx * dx + dz * dz;
				if (d2 > r * r) {
					continue;
				}

				set(level, c.getX() + dx, c.getY() - 1, c.getZ() + dz, Blocks.POLISHED_DEEPSLATE.defaultBlockState());
				set(level, c.getX() + dx, c.getY() + height, c.getZ() + dz, (dx + dz) % 7 == 0 ? Blocks.SEA_LANTERN.defaultBlockState() : Blocks.POLISHED_DEEPSLATE.defaultBlockState());
				if (d2 > (r - 1) * (r - 1)) {
					fill(level, new BlockPos(c.getX() + dx, c.getY(), c.getZ() + dz), new BlockPos(c.getX() + dx, c.getY() + height - 1, c.getZ() + dz),
							Blocks.DEEPSLATE_BRICKS.defaultBlockState());
				}
			}
		}

		// A doorway on the east side.
		fill(level, c.offset(r - 1, 0, -1), c.offset(r, 2, 1), Blocks.AIR.defaultBlockState());
	}
}
