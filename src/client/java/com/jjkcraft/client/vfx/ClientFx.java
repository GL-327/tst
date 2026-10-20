package com.jjkcraft.client.vfx;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

import com.jjkcraft.client.config.ClientConfig;
import com.jjkcraft.client.config.JjkClientConfig;

/**
 * Client-only effect state and budgets: particle count per tick with distance LOD, camera shake,
 * and hit-stop. Everything here is visual; the server never slows or stops.
 */
public final class ClientFx {
	private static final RandomSource RANDOM = RandomSource.create();
	private static final double NEAR_CAMERA_SQR = 1.4 * 1.4;
	private static int particlesThisTick;

	private static float shakeStrength;
	private static long shakeStartMs;
	private static long shakeDurationMs;

	/** Entity id to the game time until which its client-side ticking is paused. */
	private static final Map<Integer, Long> FROZEN = new HashMap<>();

	private ClientFx() {
	}

	public static void init() {
		ClientTickEvents.START_CLIENT_TICK.register(mc -> {
			particlesThisTick = 0;
			if (mc.level != null && !FROZEN.isEmpty()) {
				long now = mc.level.getGameTime();
				FROZEN.values().removeIf(until -> until <= now);
			}
		});
	}

	// ---------------------------------------------------------------- particles

	/** Share of requested particles to actually spawn, from the VFX level. */
	public static float vfxScale() {
		return switch (ClientConfig.get().vfx.level) {
			case OFF -> 0;
			case LOW -> 0.3f;
			case MEDIUM -> 0.65f;
			case HIGH -> 1f;
		};
	}

	/** How many of {@code requested} particles to spawn at {@code at}, after the VFX level, distance LOD and the per-tick budget. */
	public static int budget(int requested, Vec3 at) {
		if (ClientConfig.effectsOff()) return 0;
		Minecraft mc = Minecraft.getInstance();
		double distance = mc.gameRenderer.mainCamera().position().distanceTo(at);
		double lod = distance < 16 ? 1 : 16 / distance;
		int wanted = Math.max(requested > 0 ? 1 : 0, (int) Math.round(requested * vfxScale() * lod));
		int cap = ClientConfig.get().vfx.maxParticlesPerFrame;
		int allowed = Math.max(0, Math.min(wanted, cap - particlesThisTick));
		particlesThisTick += allowed;
		return allowed;
	}

	/** Spawns up to {@code count} particles in a sphere around {@code at}, moving outward at {@code speed}. */
	public static void burst(ParticleOptions particle, Vec3 at, int count, double spread, double speed) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) {
			return;
		}

		int n = budget(count, at);
		Vec3 camera = mc.gameRenderer.mainCamera().position();
		for (int i = 0; i < n; i++) {
			Vec3 dir = new Vec3(RANDOM.nextGaussian(), RANDOM.nextGaussian(), RANDOM.nextGaussian()).normalize();
			Vec3 p = at.add(dir.scale(spread * RANDOM.nextDouble()));
			if (p.distanceToSqr(camera) < NEAR_CAMERA_SQR) {
				// Particles this close fill the screen; the caster sees the overlay and trail instead.
				continue;
			}

			Vec3 v = dir.scale(speed * (0.5 + RANDOM.nextDouble()));
			mc.level.addParticle(particle, p.x, p.y, p.z, v.x, v.y, v.z);
		}
	}

	/** Particles along a segment, for lines and chains. */
	public static void line(ParticleOptions particle, Vec3 from, Vec3 to, int count, double jitter) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) {
			return;
		}

		int n = budget(count, from.lerp(to, 0.5));
		for (int i = 0; i < n; i++) {
			Vec3 p = from.lerp(to, RANDOM.nextDouble()).add(RANDOM.nextGaussian() * jitter, RANDOM.nextGaussian() * jitter, RANDOM.nextGaussian() * jitter);
			mc.level.addParticle(particle, p.x, p.y, p.z, 0, 0, 0);
		}
	}

	// ---------------------------------------------------------------- camera shake

	public static void shake(float strength, int durationMs) {
		JjkClientConfig cfg = ClientConfig.get();
		if (!cfg.vfx.cameraShake || cfg.accessibility.reducedMotion || strength <= 0) {
			return;
		}

		long now = Util.getMillis();
		float remaining = currentShake(now);
		if (strength >= remaining) {
			shakeStrength = strength;
			shakeStartMs = now;
			shakeDurationMs = durationMs;
		}
	}

	private static float currentShake(long now) {
		if (shakeDurationMs <= 0) {
			return 0;
		}

		float t = (float) (now - shakeStartMs) / shakeDurationMs;
		return t >= 1 ? 0 : shakeStrength * (1 - t) * (1 - t);
	}

	/** Yaw and pitch offsets in degrees for this frame. Called by the camera mixin. */
	public static float[] shakeOffsets() {
		long now = Util.getMillis();
		float s = currentShake(now);
		if (s <= 0) {
			return null;
		}

		float t = now / 1000f;
		return new float[] {
				s * 2.2f * (Mth.sin(t * 61f) + 0.5f * Mth.sin(t * 97f)),
				s * 1.6f * (Mth.sin(t * 73f + 1.3f) + 0.5f * Mth.sin(t * 113f))
		};
	}

	// ---------------------------------------------------------------- hit-stop

	/** Pauses the listed entities' client-side ticking (animation, interpolation) for a few ticks. */
	public static void hitStop(int ticks, int... entityIds) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || ticks <= 0 || !ClientConfig.get().vfx.hitStop || ClientConfig.reducedMotion()) {
			return;
		}

		long until = mc.level.getGameTime() + Math.min(ticks, 8);
		for (int id : entityIds) {
			if (mc.player == null || id != mc.player.getId()) {
				FROZEN.merge(id, until, Math::max);
			}
		}
	}

	public static boolean isFrozen(int entityId) {
		return !FROZEN.isEmpty() && FROZEN.containsKey(entityId);
	}
}
