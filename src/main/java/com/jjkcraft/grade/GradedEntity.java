package com.jjkcraft.grade;

import net.minecraft.resources.Identifier;

/** Entities that have a grade on the shared ladder (curses, shikigami, NPC sorcerers). */
public interface GradedEntity {
	Identifier grade();

	/** True for curses, which count toward "defeat a curse" trials. */
	default boolean isCurse() {
		return true;
	}
}
