package com.jjkcraft.modulo;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import com.jjkcraft.JjkCraft;

/**
 * The Modulo expansion's world state, saved to {@code data/jjkcraft/modulo.dat}: the Naunax
 * Descent's schedule and position, and which mission sites have been built.
 */
public final class ModuloState extends SavedData {
	public static final Codec<ModuloState> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.LONG.optionalFieldOf("descent_start", -1L).forGetter(s -> s.descentStart),
			Codec.LONG.optionalFieldOf("descent_length", 0L).forGetter(s -> s.descentLength),
			Codec.LONG.optionalFieldOf("next_descent", -1L).forGetter(s -> s.nextDescent),
			BlockPos.CODEC.optionalFieldOf("descent_pos").forGetter(s -> s.descentPos),
			Identifier.CODEC.listOf().optionalFieldOf("built_sites", List.of()).forGetter(s -> List.copyOf(s.builtSites))
	).apply(i, ModuloState::new));

	public static final SavedDataType<ModuloState> TYPE = new SavedDataType<>(JjkCraft.id("modulo"), ModuloState::new, CODEC, null);

	/** Game time the current or last Descent began (-1: never). */
	public long descentStart;
	/** How long the Descent that began at {@link #descentStart} lasts, in ticks. */
	public long descentLength;
	/** Game time the next Descent is due (-1: as soon as someone is eligible). */
	public long nextDescent;
	/** The ramp's foot in the overworld, at ground level. */
	public Optional<BlockPos> descentPos;
	public final Set<Identifier> builtSites;

	public ModuloState() {
		this(-1L, 0L, -1L, Optional.empty(), List.of());
	}

	private ModuloState(long descentStart, long descentLength, long nextDescent, Optional<BlockPos> descentPos, List<Identifier> builtSites) {
		this.descentStart = descentStart;
		this.descentLength = descentLength;
		this.nextDescent = nextDescent;
		this.descentPos = descentPos;
		this.builtSites = new LinkedHashSet<>(builtSites);
	}
}
