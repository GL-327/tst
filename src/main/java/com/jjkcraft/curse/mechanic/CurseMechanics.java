package com.jjkcraft.curse.mechanic;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.minecraft.resources.Identifier;

import com.jjkcraft.JjkCraft;

/** The mechanic ids curse data can name. Unknown ids fall back to a plain mechanic with a warning. */
public final class CurseMechanics {
	private static final Map<Identifier, CurseMechanic> MECHANICS = new HashMap<>();
	private static final Set<Identifier> WARNED = new HashSet<>();
	public static final CurseMechanic PLAIN = new CurseMechanic() {
	};

	private CurseMechanics() {
	}

	public static void init() {
		register("plain", PLAIN);
		register("hymnal", new HymnalMechanic());
		register("ledger", new LedgerMechanic());
		register("wick", new WickMechanic());
		register("stilt", new StiltMechanic());
		register("echo", new EchoMechanic());
		register("salt_lantern", new SaltLanternMechanic());
		register("tidewash", new TidewashMechanic());
		register("thorn", new ThornMechanic());
		register("bell", new BellMechanic());
		register("saint", new SaintMechanic());
		register("oni", new OniMechanic());
		register("gate", new GateMechanic());
		register("vow_kin", new VowKinMechanic());
		register("paper_tide", new PaperTideMechanic());
		register("ferryman", new FerrymanMechanic());
		register("warden", new WardenMechanic());
		register("sovereign", new SovereignMechanic());
		register("doorframe", new DoorframeMechanic());
		register("mimic", new MimicMechanic());
		register("tithe", new TitheMechanic());
		LedgerMechanic.init();
		EchoMechanic.init();
		VowKinMechanic.init();
	}

	public static void register(String name, CurseMechanic mechanic) {
		MECHANICS.put(JjkCraft.id(name), mechanic);
	}

	public static CurseMechanic get(Identifier id) {
		CurseMechanic mechanic = MECHANICS.get(id);
		if (mechanic == null) {
			if (WARNED.add(id)) {
				JjkCraft.LOGGER.warn("Unknown curse mechanic {}; using the plain mechanic", id);
			}

			return PLAIN;
		}

		return mechanic;
	}

	public static boolean exists(Identifier id) {
		return MECHANICS.containsKey(id);
	}
}
