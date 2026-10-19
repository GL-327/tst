package com.jjkcraft.energy;

/** Implemented by entities whose CE capacity comes from their own data (curses, shikigami). */
public interface CursedEnergyUser {
	double maxCursedEnergy();

	default double cursedEnergyRegenMultiplier() {
		return 1.0;
	}
}
