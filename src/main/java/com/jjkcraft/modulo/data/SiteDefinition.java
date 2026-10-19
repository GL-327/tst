package com.jjkcraft.modulo.data;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * A place where Modulo missions happen ({@code data/<ns>/jjkcraft/modulo_site/*.json}). The site is
 * built the first time a player on one of its stages comes near, and its characters stand there
 * while someone is near (docs/MODULO.md 3.7, docs/SCHEMAS.md).
 *
 * @param dimension the level it is in
 * @param center    its centre, at standing height
 * @param radius    how far it reaches: the build size and the area that counts as "at the site"
 * @param layout    what the builder raises there (see {@link #LAYOUTS})
 * @param name      translation key of its name on the HUD
 * @param npcs      characters who stand there, by offset from the centre
 * @param markers   named points by offset from the centre (travel stages, boss and gate positions)
 */
public record SiteDefinition(ResourceKey<Level> dimension, BlockPos center, int radius, String layout, String name, List<Npc> npcs,
		Map<String, BlockPos> markers) {
	/** Layouts the site builder knows. */
	public static final Set<String> LAYOUTS = Set.of("deck_hall", "mul_core", "meeting_room", "plaza", "alley", "warehouse", "river", "school",
			"beach", "ruins", "arena", "open");

	public static final Codec<SiteDefinition> CODEC = RecordCodecBuilder.create(i -> i.group(
			ResourceKey.codec(Registries.DIMENSION).fieldOf("dimension").forGetter(SiteDefinition::dimension),
			BlockPos.CODEC.fieldOf("center").forGetter(SiteDefinition::center),
			Codec.intRange(4, 96).fieldOf("radius").forGetter(SiteDefinition::radius),
			Codec.STRING.validate(s -> LAYOUTS.contains(s) ? DataResult.success(s) : DataResult.error(() -> "Unknown layout: " + s + ", expected one of " + LAYOUTS))
					.fieldOf("layout").forGetter(SiteDefinition::layout),
			Codec.STRING.fieldOf("name").forGetter(SiteDefinition::name),
			Npc.CODEC.listOf().optionalFieldOf("npcs", List.of()).forGetter(SiteDefinition::npcs),
			Codec.unboundedMap(Codec.STRING, BlockPos.CODEC).optionalFieldOf("markers", Map.of()).forGetter(SiteDefinition::markers)
	).apply(i, SiteDefinition::new));

	/** A character standing at the site. */
	public record Npc(String character, BlockPos offset) {
		public static final Codec<Npc> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.STRING.fieldOf("character").forGetter(Npc::character),
				BlockPos.CODEC.fieldOf("offset").forGetter(Npc::offset)
		).apply(i, Npc::new));
	}

	/** A marker's world position, or the centre if the site has no such marker. */
	public BlockPos marker(String name) {
		BlockPos offset = markers.get(name);
		return offset == null ? center : center.offset(offset);
	}

	public boolean contains(BlockPos pos, double extra) {
		double r = radius + extra;
		return pos.distSqr(center) <= r * r;
	}
}
