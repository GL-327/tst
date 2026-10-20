package com.jjkcraft.technique;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.Identifier;

import com.jjkcraft.registry.LegacyIds;

/**
 * A player's techniques. Immutable.
 *
 * @param innate    the technique rolled at awakening
 * @param learned   techniques learned later (scrolls, teachers, absorbed curses)
 * @param loadout   the twelve power slots; empty string ids mean an empty slot
 * @param selected  the selected slot for the cast key
 * @param mastery   landed casts per technique
 * @param cooldowns game time at which each technique is ready again
 */
public record TechniqueState(
		Optional<Identifier> innate,
		List<Identifier> learned,
		List<Optional<Identifier>> loadout,
		int selected,
		Map<Identifier, Integer> mastery,
		Map<Identifier, Long> cooldowns
) {
	public static final int SLOTS = 12;

	public static final TechniqueState EMPTY = new TechniqueState(Optional.empty(), List.of(), emptyLoadout(), 0, Map.of(), Map.of());

	private static final Codec<Optional<Identifier>> SLOT_CODEC = Codec.STRING.xmap(
			s -> s.isEmpty() ? Optional.<Identifier>empty() : Optional.of(LegacyIds.technique(Identifier.parse(s))),
			o -> o.map(Identifier::toString).orElse(""));

	public static final Codec<TechniqueState> CODEC = RecordCodecBuilder.create(i -> i.group(
			LegacyIds.TECHNIQUE_ID.optionalFieldOf("innate").forGetter(TechniqueState::innate),
			LegacyIds.TECHNIQUE_ID.listOf().optionalFieldOf("learned", List.of()).forGetter(TechniqueState::learned),
			SLOT_CODEC.listOf().optionalFieldOf("loadout", List.of()).forGetter(TechniqueState::loadout),
			Codec.INT.optionalFieldOf("selected", 0).forGetter(TechniqueState::selected),
			Codec.unboundedMap(LegacyIds.TECHNIQUE_ID, Codec.INT).optionalFieldOf("mastery", Map.of()).forGetter(TechniqueState::mastery),
			Codec.unboundedMap(LegacyIds.TECHNIQUE_ID, Codec.LONG).optionalFieldOf("cooldowns", Map.of()).forGetter(TechniqueState::cooldowns)
	).apply(i, TechniqueState::normalized));

	/** Fixes list sizes and bounds after decoding. */
	public static TechniqueState normalized(Optional<Identifier> innate, List<Identifier> learned, List<Optional<Identifier>> loadout,
			int selected, Map<Identifier, Integer> mastery, Map<Identifier, Long> cooldowns) {
		List<Optional<Identifier>> slots = new ArrayList<>(loadout);
		while (slots.size() < SLOTS) {
			slots.add(Optional.empty());
		}

		return new TechniqueState(innate, List.copyOf(new LinkedHashSet<>(learned)), List.copyOf(slots.subList(0, SLOTS)),
				Math.floorMod(selected, SLOTS), Map.copyOf(mastery), Map.copyOf(cooldowns));
	}

	private static List<Optional<Identifier>> emptyLoadout() {
		List<Optional<Identifier>> slots = new ArrayList<>();
		for (int i = 0; i < SLOTS; i++) {
			slots.add(Optional.empty());
		}

		return List.copyOf(slots);
	}

	/** Innate first, then learned, without duplicates. */
	public Set<Identifier> known() {
		Set<Identifier> set = new LinkedHashSet<>();
		innate.ifPresent(set::add);
		set.addAll(learned);
		return set;
	}

	public Optional<Identifier> slot(int index) {
		return index >= 0 && index < SLOTS ? loadout.get(index) : Optional.empty();
	}

	public int masteryOf(Identifier id) {
		return mastery.getOrDefault(id, 0);
	}

	public long readyAt(Identifier id) {
		return cooldowns.getOrDefault(id, 0L);
	}

	public TechniqueState withInnate(Identifier id) {
		List<Optional<Identifier>> slots = new ArrayList<>(loadout);
		innate.filter(old -> !old.equals(id) && !learned.contains(old))
				.ifPresent(old -> slots.replaceAll(s -> s.filter(x -> !x.equals(old))));
		return normalized(Optional.of(id), learned, slots, selected, mastery, cooldowns);
	}

	public TechniqueState withLearned(Identifier id) {
		List<Identifier> next = new ArrayList<>(learned);
		if (!next.contains(id) && !innate.map(id::equals).orElse(false)) {
			next.add(id);
		}

		return normalized(innate, next, loadout, selected, mastery, cooldowns);
	}

	public TechniqueState withoutLearned(Identifier id) {
		List<Identifier> next = new ArrayList<>(learned);
		next.remove(id);
		List<Optional<Identifier>> slots = new ArrayList<>(loadout);
		if (!innate.map(id::equals).orElse(false)) {
			slots.replaceAll(s -> s.filter(x -> !x.equals(id)));
		}

		return normalized(innate, next, slots, selected, mastery, cooldowns);
	}

	public TechniqueState withSlot(int index, Optional<Identifier> id) {
		List<Optional<Identifier>> slots = new ArrayList<>(loadout);
		// A technique sits in at most one slot.
		id.ifPresent(target -> slots.replaceAll(s -> s.filter(x -> !x.equals(target))));
		slots.set(index, id);
		return normalized(innate, learned, slots, selected, mastery, cooldowns);
	}

	public TechniqueState withSelected(int index) {
		return normalized(innate, learned, loadout, index, mastery, cooldowns);
	}

	public TechniqueState withMasteryAdded(Identifier id, int amount) {
		Map<Identifier, Integer> next = new HashMap<>(mastery);
		next.merge(id, amount, Integer::sum);
		return normalized(innate, learned, loadout, selected, next, cooldowns);
	}

	public TechniqueState withMastery(Map<Identifier, Integer> next) {
		return normalized(innate, learned, loadout, selected, next, cooldowns);
	}

	public TechniqueState withCooldown(Identifier id, long readyAt, long now) {
		Map<Identifier, Long> next = new HashMap<>();
		// Drop expired entries so the map stays small.
		cooldowns.forEach((k, v) -> {
			if (v > now) {
				next.put(k, v);
			}
		});
		next.put(id, readyAt);
		return normalized(innate, learned, loadout, selected, mastery, next);
	}

	public TechniqueState withoutCooldowns() {
		return normalized(innate, learned, loadout, selected, mastery, Map.of());
	}

	/** Fills empty slots with known techniques that are not slotted yet (after awakening or learning). */
	public TechniqueState withAutoSlotted(int usableSlots) {
		List<Optional<Identifier>> slots = new ArrayList<>(loadout);
		for (Identifier id : known()) {
			if (slots.stream().anyMatch(s -> s.map(id::equals).orElse(false))) {
				continue;
			}

			for (int i = 0; i < Math.min(usableSlots, SLOTS); i++) {
				if (slots.get(i).isEmpty()) {
					slots.set(i, Optional.of(id));
					break;
				}
			}
		}

		return normalized(innate, learned, slots, selected, mastery, cooldowns);
	}
}
