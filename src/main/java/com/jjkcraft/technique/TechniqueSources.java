package com.jjkcraft.technique;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

/**
 * Techniques a player can use without knowing them, for example from a held cursed tool. Cursed
 * tools register a source; the technique works only while the source applies.
 */
public final class TechniqueSources {
	@FunctionalInterface
	public interface Source {
		Set<Identifier> techniques(Player player);
	}

	private static final List<Source> SOURCES = new CopyOnWriteArrayList<>();

	private TechniqueSources() {
	}

	public static void add(Source source) {
		SOURCES.add(source);
	}

	public static Set<Identifier> granted(Player player) {
		Set<Identifier> all = new LinkedHashSet<>();
		for (Source source : SOURCES) {
			all.addAll(source.techniques(player));
		}

		return all;
	}

	public static boolean grants(Player player, Identifier id) {
		for (Source source : SOURCES) {
			if (source.techniques(player).contains(id)) {
				return true;
			}
		}

		return false;
	}
}
