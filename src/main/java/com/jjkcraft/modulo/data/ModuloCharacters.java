package com.jjkcraft.modulo.data;

import java.util.Set;

/** Every Modulo character the mod knows; each has a skin ({@code textures/entity/modulo/<id>.png}) and a name ({@code npc.jjkcraft.modulo.<id>}). */
public final class ModuloCharacters {
	public static final Set<String> ALL = Set.of("maru", "cross", "yuka", "tsurugi", "usami", "mino", "kyoko_tomoe", "osada", "masayoshi", "jabaloma", "dabura");

	private ModuloCharacters() {
	}
}
