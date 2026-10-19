package com.jjkcraft.technique;

import java.util.List;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * The outcome of a cast.
 *
 * @param landed  hit something or completed its purpose (counts toward Control and mastery)
 * @param hits    entities affected, for hit effects
 * @param impact  where the effect landed, for client visuals (may be the caster's position)
 */
public record CastResult(boolean landed, List<Entity> hits, Vec3 impact) {
	public static CastResult miss(Vec3 at) {
		return new CastResult(false, List.of(), at);
	}

	public static CastResult hit(List<Entity> hits, Vec3 at) {
		return new CastResult(!hits.isEmpty(), hits, at);
	}

	public static CastResult done(Vec3 at) {
		return new CastResult(true, List.of(), at);
	}
}
