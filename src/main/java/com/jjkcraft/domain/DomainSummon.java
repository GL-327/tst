package com.jjkcraft.domain;

import java.util.UUID;

/** Something a domain created (shadow Divine Dogs). It belongs to its summoner's side and goes when the domain does. */
public interface DomainSummon {
	UUID summonerId();

	/** The id of the domain instance that made it. */
	int domainInstance();
}
