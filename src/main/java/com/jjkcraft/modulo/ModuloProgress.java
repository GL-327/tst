package com.jjkcraft.modulo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.GlobalPos;
import net.minecraft.resources.Identifier;

/**
 * A player's place in the Modulo expansion. Each finished stage is a checkpoint: {@code stage} is
 * saved, and only {@code counter} (kills, seconds held, items made) starts over after a death or a
 * relog (docs/MODULO.md 3.7).
 *
 * @param boarded      has boarded the Naunax at least once
 * @param returnTo     where leaving the expansion takes the player (where they boarded)
 * @param completed    missions finished, in order
 * @param mission      the mission {@code stage} belongs to (empty: none started yet)
 * @param stage        index of the current stage in that mission
 * @param counter      progress inside the current stage
 * @param companion    Maru has joined
 * @param epilogueSeen the epilogue sequence has played for this player
 * @param flags        choices made in missions (for example {@code chaos_and_harmony=duel})
 */
public record ModuloProgress(boolean boarded, Optional<GlobalPos> returnTo, List<Identifier> completed, Optional<Identifier> mission,
		int stage, int counter, boolean companion, boolean epilogueSeen, Map<String, String> flags) {
	public static final ModuloProgress EMPTY = new ModuloProgress(false, Optional.empty(), List.of(), Optional.empty(), 0, 0, false, false, Map.of());

	public static final Codec<ModuloProgress> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.BOOL.optionalFieldOf("boarded", false).forGetter(ModuloProgress::boarded),
			GlobalPos.CODEC.optionalFieldOf("return_to").forGetter(ModuloProgress::returnTo),
			Identifier.CODEC.listOf().optionalFieldOf("completed", List.of()).forGetter(ModuloProgress::completed),
			Identifier.CODEC.optionalFieldOf("mission").forGetter(ModuloProgress::mission),
			Codec.intRange(0, 1000).optionalFieldOf("stage", 0).forGetter(ModuloProgress::stage),
			Codec.INT.optionalFieldOf("counter", 0).forGetter(ModuloProgress::counter),
			Codec.BOOL.optionalFieldOf("companion", false).forGetter(ModuloProgress::companion),
			Codec.BOOL.optionalFieldOf("epilogue_seen", false).forGetter(ModuloProgress::epilogueSeen),
			Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("flags", Map.of()).forGetter(ModuloProgress::flags)
	).apply(i, ModuloProgress::new));

	public boolean hasCompleted(Identifier id) {
		return completed.contains(id);
	}

	public ModuloProgress withBoarded(GlobalPos from) {
		return new ModuloProgress(true, Optional.of(from), completed, mission, stage, counter, companion, epilogueSeen, flags);
	}

	public ModuloProgress withReturnTo(Optional<GlobalPos> to) {
		return new ModuloProgress(boarded, to, completed, mission, stage, counter, companion, epilogueSeen, flags);
	}

	/** Starts {@code id} at its first stage. */
	public ModuloProgress starting(Identifier id) {
		return new ModuloProgress(boarded, returnTo, completed, Optional.of(id), 0, 0, companion, epilogueSeen, flags);
	}

	/** The current stage is done: the next one is the new checkpoint. */
	public ModuloProgress nextStage() {
		return new ModuloProgress(boarded, returnTo, completed, mission, stage + 1, 0, companion, epilogueSeen, flags);
	}

	public ModuloProgress withCounter(int value) {
		return new ModuloProgress(boarded, returnTo, completed, mission, stage, value, companion, epilogueSeen, flags);
	}

	/** Back to the start of the current stage, as after a death or a relog. */
	public ModuloProgress atCheckpoint() {
		return withCounter(0);
	}

	public ModuloProgress finishing(Identifier id) {
		List<Identifier> done = new ArrayList<>(completed);
		if (!done.contains(id)) {
			done.add(id);
		}

		return new ModuloProgress(boarded, returnTo, List.copyOf(done), Optional.empty(), 0, 0, companion, epilogueSeen, flags);
	}

	public ModuloProgress withCompanion(boolean value) {
		return new ModuloProgress(boarded, returnTo, completed, mission, stage, counter, value, epilogueSeen, flags);
	}

	public ModuloProgress withEpilogueSeen() {
		return new ModuloProgress(boarded, returnTo, completed, mission, stage, counter, companion, true, flags);
	}

	public ModuloProgress withFlag(String key, String value) {
		Map<String, String> copy = new HashMap<>(flags);
		copy.put(key, value);
		return new ModuloProgress(boarded, returnTo, completed, mission, stage, counter, companion, epilogueSeen, Map.copyOf(copy));
	}

	public Optional<String> flag(String key) {
		return Optional.ofNullable(flags.get(key));
	}
}
