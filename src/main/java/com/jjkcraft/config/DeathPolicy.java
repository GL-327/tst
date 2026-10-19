package com.jjkcraft.config;

/** What happens to sorcerer progress when a player dies. Default is {@link #KEEP}. */
public enum DeathPolicy {
	/** Everything is kept. CE refills to the configured respawn share. */
	KEEP,
	/** Grade is kept, but some grade experience and technique mastery are lost. */
	PARTIAL,
	/** Grade is kept. Learned techniques and all mastery are lost; the innate technique stays. */
	RESET_TECHNIQUES
}
