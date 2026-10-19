package com.jjkcraft.domain;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.Identifier;

import com.jjkcraft.registry.LegacyIds;

/**
 * A player's domain progress (persistent).
 *
 * @param known        the domain they can expand, if any
 * @param burnoutUntil game time until which their techniques are burnt out
 * @param opened       how many times they have opened it
 */
public record DomainState(Optional<Identifier> known, long burnoutUntil, int opened) {
	public static final DomainState EMPTY = new DomainState(Optional.empty(), 0, 0);

	public static final Codec<DomainState> CODEC = RecordCodecBuilder.create(i -> i.group(
			LegacyIds.DOMAIN_ID.optionalFieldOf("known").forGetter(DomainState::known),
			Codec.LONG.optionalFieldOf("burnout_until", 0L).forGetter(DomainState::burnoutUntil),
			Codec.INT.optionalFieldOf("opened", 0).forGetter(DomainState::opened)
	).apply(i, DomainState::new));

	public DomainState withKnown(Optional<Identifier> domain) {
		return new DomainState(domain, burnoutUntil, opened);
	}

	public DomainState withBurnoutUntil(long tick) {
		return new DomainState(known, tick, opened);
	}

	public DomainState withOpened() {
		return new DomainState(known, burnoutUntil, opened + 1);
	}

	public boolean burntOut(long now) {
		return now < burnoutUntil;
	}
}
