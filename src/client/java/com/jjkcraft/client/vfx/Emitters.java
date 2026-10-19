package com.jjkcraft.client.vfx;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.client.Minecraft;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

/** Particle emitters that run for several ticks (field swirls, windups). Budgeted through {@link ClientFx}. */
public final class Emitters {
	/** Called once per client tick with its age in ticks. Return false to stop. */
	@FunctionalInterface
	public interface Emitter {
		boolean tick(int age);
	}

	private static final class Running {
		final Emitter emitter;
		int age;

		Running(Emitter emitter) {
			this.emitter = emitter;
		}
	}

	private static final List<Running> RUNNING = new ArrayList<>();

	private Emitters() {
	}

	public static void init() {
		ClientTickEvents.END_CLIENT_TICK.register(Emitters::tick);
	}

	public static void add(Emitter emitter) {
		if (RUNNING.size() < 128) {
			RUNNING.add(new Running(emitter));
		}
	}

	public static void clear() {
		RUNNING.clear();
	}

	private static void tick(Minecraft mc) {
		if (mc.level == null || mc.isPaused()) {
			return;
		}

		for (Iterator<Running> it = RUNNING.iterator(); it.hasNext(); ) {
			Running running = it.next();
			if (!running.emitter.tick(running.age++)) {
				it.remove();
			}
		}
	}
}
