package com.jjkcraft.fear;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.levelgen.structure.Structure;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;

import com.jjkcraft.JjkCraft;
import com.jjkcraft.core.fear.FearMath;

/**
 * The fear field. Curses are born from fear, so every chunk section has a fear value from 0 to
 * 100: an environmental part (darkness, night, depth, haunted structures), computed on demand, and
 * an event part (deaths nearby, especially of players and villagers) that is stored on the level
 * and decays over time. Curse spawning, curse regen and curse aggression all read it.
 */
public final class Fear {
	/** Structures that count as abandoned or haunted places. */
	public static final TagKey<Structure> HAUNTED = TagKey.create(Registries.STRUCTURE, JjkCraft.id("haunted"));

	/** One stored cell: event fear at a game tick (decays lazily from there). */
	public record Cell(float value, long tick) {
		static final Codec<Cell> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.FLOAT.fieldOf("v").forGetter(Cell::value),
				Codec.LONG.fieldOf("t").forGetter(Cell::tick)
		).apply(i, Cell::new));
	}

	/** Stored event fear by section, saved with the level. */
	public record Field(Map<Long, Cell> cells) {
		static final Codec<Field> CODEC = Codec.unboundedMap(Codec.STRING, Cell.CODEC)
				.xmap(m -> {
					Map<Long, Cell> out = new HashMap<>();
					m.forEach((k, v) -> out.put(Long.parseLong(k), v));
					return new Field(out);
				}, f -> {
					Map<String, Cell> out = new HashMap<>();
					f.cells.forEach((k, v) -> out.put(Long.toString(k), v));
					return out;
				});
	}

	public static final AttachmentType<Field> FIELD = AttachmentRegistry.create(JjkCraft.id("fear_field"),
			builder -> builder.persistent(Field.CODEC).initializer(() -> new Field(new HashMap<>())));

	private Fear() {
	}

	public static void init() {
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity.level() instanceof ServerLevel level) {
				float amount = entity instanceof Player ? 15 : entity instanceof Villager ? 10 : 2.5f;
				add(level, entity.blockPosition(), amount);
			}
		});
	}

	/** Total fear at a position, 0 to 100. */
	public static double at(ServerLevel level, BlockPos pos) {
		return FearMath.clamp(environment(level, pos) + stored(level, pos));
	}

	/** Event fear stored for the section containing {@code pos}, after decay. */
	public static double stored(ServerLevel level, BlockPos pos) {
		Field field = level.getAttached(FIELD);
		if (field == null) {
			return 0;
		}

		Cell cell = field.cells().get(SectionPos.asLong(pos));
		return cell == null ? 0 : FearMath.decayed(cell.value(), cell.tick(), level.getGameTime());
	}

	public static double environment(ServerLevel level, BlockPos pos) {
		int light = level.getMaxLocalRawBrightness(pos);
		boolean night = level.isDarkOutside();
		boolean haunted = level.structureManager().getStructureWithPieceAt(pos, HAUNTED).isValid();
		return FearMath.environmental(light, night, pos.getY() < 0, haunted);
	}

	/** Adds event fear to a section (deaths, rituals). Negative amounts calm it (clearing a nest). */
	public static void add(ServerLevel level, BlockPos pos, float amount) {
		Field field = level.getAttachedOrCreate(FIELD);
		long key = SectionPos.asLong(pos);
		long now = level.getGameTime();
		Cell old = field.cells().get(key);
		double current = old == null ? 0 : FearMath.decayed(old.value(), old.tick(), now);
		float next = (float) FearMath.clamp(current + amount);
		if (next <= 0.01f) {
			field.cells().remove(key);
		} else {
			field.cells().put(key, new Cell(next, now));
		}

		// Keep the stored map small: drop fully decayed cells once it grows.
		if (field.cells().size() > 4096) {
			field.cells().entrySet().removeIf(e -> FearMath.decayed(e.getValue().value(), e.getValue().tick(), now) <= 0.01);
		}

		level.setAttached(FIELD, field);
	}

	/** Stored cells (for debug commands). */
	public static List<Map.Entry<Long, Cell>> cells(ServerLevel level) {
		Field field = level.getAttached(FIELD);
		return field == null ? List.of() : List.copyOf(field.cells().entrySet());
	}
}
