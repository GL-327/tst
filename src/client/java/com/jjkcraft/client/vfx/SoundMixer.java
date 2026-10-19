package com.jjkcraft.client.vfx;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import com.jjkcraft.client.config.ClientConfig;

/**
 * Plays layered technique audio (cue, whoosh, impact) with pitch variance and a voice limit.
 * When the mix is busy, the lowest-priority, oldest mod sound is stopped to make room.
 */
public final class SoundMixer {
	/** Higher wins. */
	public enum Priority {
		AMBIENT(0), WHOOSH(1), CUE(2), IMPACT(3), DRAMATIC(4);

		final int level;

		Priority(int level) {
			this.level = level;
		}
	}

	private record Voice(SoundInstance instance, Priority priority, long startedAt) {
	}

	private static final List<Voice> VOICES = new ArrayList<>();
	private static final RandomSource RANDOM = RandomSource.create();

	private SoundMixer() {
	}

	public static void play(SoundEvent sound, Vec3 at, Priority priority, float volume, float pitchVariance) {
		Minecraft mc = Minecraft.getInstance();
		VOICES.removeIf(v -> !mc.getSoundManager().isActive(v.instance()));
		int max = ClientConfig.get().audio.maxConcurrentSounds;
		if (VOICES.size() >= max) {
			Voice weakest = VOICES.stream()
					.min(Comparator.<Voice>comparingInt(v -> v.priority().level).thenComparingLong(Voice::startedAt))
					.orElse(null);
			if (weakest == null || weakest.priority().level > priority.level) {
				return;
			}

			mc.getSoundManager().stop(weakest.instance());
			VOICES.remove(weakest);
		}

		float pitch = 1f + (RANDOM.nextFloat() * 2 - 1) * pitchVariance;
		float scaled = volume * ClientConfig.get().audio.volumePercent / 100f;
		SimpleSoundInstance instance = new SimpleSoundInstance(sound, SoundSource.PLAYERS, scaled, pitch, RANDOM, at.x, at.y, at.z);
		mc.getSoundManager().play(instance);
		VOICES.add(new Voice(instance, priority, System.currentTimeMillis()));
	}
}
