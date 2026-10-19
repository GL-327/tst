package com.jjkcraft;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import net.minecraft.resources.Identifier;

class JjkCraftIdTest {
	@Test
	void idUsesModNamespace() {
		Identifier id = JjkCraft.id("cursed_energy");
		Assertions.assertEquals("jjkcraft", id.getNamespace());
		Assertions.assertEquals("cursed_energy", id.getPath());
		Assertions.assertEquals("jjkcraft:cursed_energy", id.toString());
	}
}
