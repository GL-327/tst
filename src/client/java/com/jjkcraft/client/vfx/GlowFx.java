package com.jjkcraft.client.vfx;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import com.jjkcraft.client.config.ClientConfig;

/**
 * Light-emitting looks for cursed energy, drawn in the additive glow pass: a hot core inside a
 * colored body and a wide halo, so energy brightens what is behind it instead of covering it like a
 * painted ball. Limitless has three signatures: Blue pulls streaks inward, Red throws them outward
 * and Purple folds both into a crackling violet sphere.
 */
public final class GlowFx {
	/** Glow atlas tiles (see tools/glow_atlas.py). */
	public static final int SOFT = 0, CORE = 1, RING = 2, STREAK = 3, STAR = 4, WISP = 5, SHOCK = 6, LENS = 7;

	/** The authored Limitless colours, used when an effect carries no colour of its own. */
	public static final int BLUE = 0x3D8BFF, RED = 0xFF3D4E, PURPLE = 0xA04BFF;

	private GlowFx() {
	}

	/** Which Limitless signature a technique uses: 0 Blue, 1 Red, 2 Purple, or -1 for none. */
	public static int limitless(String path) {
		if (path.contains("purple")) {
			return 2;
		}

		if (path.contains("red")) {
			return 1;
		}

		return path.contains("blue") || path.equals("infinity") ? 0 : -1;
	}

	/** The look for a Limitless technique ({@link #limitless(String)}) in {@code rgb}. */
	public static void limitless(WorldFx.Frame f, int kind, Vec3 at, double radius, float intensity, int rgb) {
		switch (kind) {
			case 2 -> purple(f, at, radius, intensity, 1, rgb);
			case 1 -> red(f, at, radius, intensity, rgb);
			default -> blue(f, at, radius, intensity, rgb);
		}
	}

	/** The authored colour of a Limitless look, for effects that carry no colour. */
	public static int limitlessColor(int kind) {
		return kind == 2 ? PURPLE : kind == 1 ? RED : BLUE;
	}

	/**
	 * A generic energy orb in {@code color}: halo, body, hot core and a soft rim.
	 *
	 * @param radius visual radius in blocks
	 */
	public static void orb(WorldFx.Frame f, Vec3 at, double radius, int color, float intensity) {
		float flicker = flicker(f, at, 0.08f);
		// The tint carries the hue against bright skies, where additive light alone drifts to white.
		f.tint(at, radius * 2.4, SOFT, darken(color, 0.35f), 0.7f * intensity, 0);
		// Large volumes cover a lot of screen; their wide light is damped so it doesn't bloom to white.
		float wide = (float) Math.clamp(1.2 / Math.max(0.01, radius), 0.4, 1);
		f.glow(at, radius * 3.4, SOFT, color, 0.22f * intensity * wide, 0);
		f.glow(at, radius * 1.9, SOFT, color, 0.48f * intensity * flicker * wide, 0);
		// The white-hot heart stays compact: on a large field it would read as a flat white disc.
		f.glow(at, Math.min(radius * 0.95, 0.5 + radius * 0.35), CORE, lighten(color, 0.65f), 0.95f * intensity * Math.max(0.6f, wide), 0);
		f.glow(at, radius * 1.25, RING, lighten(color, 0.3f), 0.28f * intensity, 0);
	}

	/**
	 * Blue: a tight white core inside a deep body; streaks fall inward as it gathers. {@code rgb} is
	 * the move's colour, so a personalized colour carries through.
	 */
	public static void blue(WorldFx.Frame f, Vec3 at, double radius, float intensity, int rgb) {
		f.tint(at, radius * 3.2, SOFT, darken(rgb, 0.55f), 0.5f * intensity, 0);
		orb(f, at, radius, rgb, intensity);
		f.glow(at, radius * 2.6, STAR, lighten(rgb, 0.6f), 0.22f * intensity, spin(f, 0.012));
		streaks(f, at, radius, lighten(rgb, 0.6f), intensity, true, 0);
		disk(f, at, radius * 1.6, rgb, intensity * 0.7f, 0.035);
	}

	/** Red: a hot white core in a deep body; streaks burst outward behind a pressure ring. */
	public static void red(WorldFx.Frame f, Vec3 at, double radius, float intensity, int rgb) {
		f.tint(at, radius * 3.0, SOFT, darken(rgb, 0.5f), 0.5f * intensity, 0);
		orb(f, at, radius, rgb, intensity);
		f.glow(at, radius * 2.4, STAR, lighten(rgb, 0.6f), 0.26f * intensity, spin(f, -0.02));
		streaks(f, at, radius, lighten(rgb, 0.6f), intensity, false, 3);
		float pulse = ClientConfig.reducedMotion() ? 0.5f : (f.time() * 0.07f) % 1f;
		f.glow(at, radius * (1.2 + 1.6 * pulse), SHOCK, rgb, 0.45f * intensity * (1 - pulse), 0);
	}

	/**
	 * Purple: a violet-white sphere with a crackling rim. While it charges ({@code merge} below 1)
	 * a blue and a red mote orbit and close in; at 1 they have merged.
	 */
	public static void purple(WorldFx.Frame f, Vec3 at, double radius, float intensity, double merge, int rgb) {
		// A deep body that darkens the background, with light only at its rim and heart: Purple
		// reads as a mass of imaginary matter rather than a lamp.
		int deep = darken(rgb, 0.62f), light = lighten(rgb, 0.7f);
		float wide = (float) Math.clamp(1.2 / Math.max(0.01, radius), 0.4, 1);
		f.tint(at, radius * 3.6, SOFT, deep, 0.55f * intensity, 0);
		f.tint(at, radius * 1.7, CORE, deep, 0.6f * intensity, 0);
		f.glow(at, radius * 4.0, SOFT, rgb, 0.3f * intensity * wide, 0);
		f.glow(at, radius * 2.0, SOFT, rgb, 0.55f * intensity * wide * flicker(f, at, 0.1f), 0);
		f.glow(at, Math.min(radius * 0.7, 0.4 + radius * 0.3), CORE, light, 0.75f * intensity * Math.max(0.6f, wide), 0);
		f.glow(at, radius * 1.6, RING, light, 0.55f * intensity, 0);
		f.glow(at, radius * 3.0, LENS, light, 0.25f * intensity, 0);
		arcs(f, at, radius * 1.05, light, intensity);
		double m = Mth.clamp(merge, 0, 1);
		if (m < 1) {
			double orbit = radius * (1.6 - 1.25 * m);
			double angle = ClientConfig.reducedMotion() ? 0 : f.time() * (0.12 + 0.2 * m);
			Vec3 offset = f.viewRight.scale(Math.cos(angle) * orbit).add(f.viewUp.scale(Math.sin(angle) * orbit * 0.45));
			float mote = (float) (0.9 - 0.4 * m) * intensity;
			orb(f, at.add(offset), radius * 0.38, BLUE, mote);
			orb(f, at.subtract(offset), radius * 0.38, RED, mote);
		}
	}

	/** A short bright flash with an expanding shock ring, for impacts and bursts. {@code t} runs 0 to 1. */
	public static void flash(WorldFx.Frame f, Vec3 at, double radius, int color, float t, float intensity) {
		float fade = (1 - t) * (1 - t);
		f.glow(at, radius * (0.6 + 0.8 * t), SOFT, color, 0.9f * fade * intensity, 0);
		f.glow(at, radius * 0.45 * (1 - t), CORE, lighten(color, 0.7f), fade * intensity, 0);
		f.glow(at, radius * (0.3 + 1.9 * Math.sqrt(t)), SHOCK, lighten(color, 0.25f), 0.7f * (1 - t) * intensity, 0);
		if (t < 0.35f) {
			f.glow(at, radius * 1.6, STAR, lighten(color, 0.5f), (1 - t / 0.35f) * 0.5f * intensity, at.x * 3);
		}
	}

	/** A glowing comet tail behind a moving orb. */
	public static void tail(WorldFx.Frame f, Vec3 head, Vec3 back, double radius, int color, float intensity) {
		double length = head.distanceTo(back);
		if (length < 1e-3) {
			return;
		}

		Vec3 axis = back.subtract(head).scale(1 / length);
		int pieces = f.segments(8);
		for (int i = 1; i <= pieces; i++) {
			double s = (double) i / pieces;
			Vec3 p = head.add(axis.scale(length * s));
			f.glow(p, radius * (1.7 - 1.2 * s), SOFT, color, (float) ((1 - s) * 0.55) * intensity, 0);
		}

		f.streak(head, head.add(axis.scale(Math.min(length, radius * 9))), radius * 1.1, lighten(color, 0.35f), 0.55f * intensity);
	}

	/** Streaks on a shell around {@code at}: falling in (Blue) or flying out (Red). */
	private static void streaks(WorldFx.Frame f, Vec3 at, double radius, int color, float intensity, boolean inward, int seed) {
		if (ClientConfig.reducedMotion()) {
			return;
		}

		int count = f.segments(14);
		for (int i = 0; i < count; i++) {
			double cycle = (f.time() * 0.045 + i * 0.618 + seed * 0.13) % 1;
			double travel = inward ? 1 - cycle : cycle;
			// Golden-angle directions spread the streaks evenly over the sphere.
			double y = 1 - 2 * ((i + 0.5) / count);
			double ring = Math.sqrt(Math.max(0, 1 - y * y));
			double a = i * 2.399963 + seed;
			Vec3 dir = new Vec3(Math.cos(a) * ring, y, Math.sin(a) * ring);
			double near = radius * (0.9 + 2.2 * travel);
			Vec3 from = at.add(dir.scale(near));
			Vec3 to = at.add(dir.scale(near + radius * (inward ? 0.9 : 1.2)));
			float alpha = (float) Math.sin(cycle * Math.PI) * 0.7f * intensity;
			f.streak(from, to, radius * 0.22, color, alpha);
		}
	}

	/** A thin spinning disk of motes, flattened toward the camera's horizon: Blue's accretion. */
	private static void disk(WorldFx.Frame f, Vec3 at, double radius, int color, float intensity, double speed) {
		if (ClientConfig.reducedMotion()) {
			return;
		}

		int count = f.segments(18);
		double turn = f.time() * speed;
		for (int i = 0; i < count; i++) {
			double a = turn + i * Math.PI * 2 / count;
			double r = radius * (0.8 + 0.2 * Math.sin(i * 1.7));
			Vec3 p = at.add(Math.cos(a) * r, Math.sin(a * 2) * r * 0.06, Math.sin(a) * r);
			f.glow(p, radius * 0.22, SOFT, color, 0.5f * intensity, 0);
		}
	}

	/** Jagged arcs that jump around the rim every few ticks. */
	private static void arcs(WorldFx.Frame f, Vec3 at, double radius, int color, float intensity) {
		int count = f.segments(6);
		long step = ClientConfig.reducedMotion() ? 0 : (long) (f.time() / 2);
		for (int i = 0; i < count; i++) {
			long h = mix(step * 31 + i * 977);
			double a0 = (h & 1023) / 1023.0 * Math.PI * 2;
			double y0 = ((h >> 10) & 1023) / 1023.0 * 2 - 1;
			Vec3 prev = point(at, radius, a0, y0);
			int joints = 3;
			for (int j = 1; j <= joints; j++) {
				long k = mix(h + j * 7919);
				double a = a0 + ((k & 255) / 255.0 - 0.5) * 1.1;
				double y = Mth.clamp(y0 + (((k >> 8) & 255) / 255.0 - 0.5) * 0.7, -1, 1);
				double out = radius * (1.0 + ((k >> 16) & 255) / 255.0 * 0.35);
				Vec3 next = point(at, out, a, y);
				f.streak(prev, next, radius * 0.1, color, 0.75f * intensity);
				prev = next;
			}
		}
	}

	private static Vec3 point(Vec3 at, double radius, double angle, double y) {
		double ring = Math.sqrt(Math.max(0, 1 - y * y));
		return at.add(Math.cos(angle) * ring * radius, y * radius, Math.sin(angle) * ring * radius);
	}

	private static long mix(long v) {
		v ^= v >>> 33;
		v *= 0xff51afd7ed558ccdL;
		v ^= v >>> 33;
		v *= 0xc4ceb9fe1a85ec53L;
		return v ^ (v >>> 33);
	}

	private static float flicker(WorldFx.Frame f, Vec3 at, float amount) {
		if (ClientConfig.reduceFlashing() || ClientConfig.reducedMotion()) {
			return 1;
		}

		double t = f.time() + at.x * 7 + at.z * 3;
		return 1 - amount + amount * (float) (0.5 + 0.25 * Math.sin(t * 0.9) + 0.25 * Math.sin(t * 2.3));
	}

	private static double spin(WorldFx.Frame f, double speed) {
		return ClientConfig.reducedMotion() ? 0 : f.time() * speed;
	}

	/** Moves an RGB color toward black by {@code amount} (0 to 1). */
	public static int darken(int rgb, float amount) {
		int r = (rgb >> 16) & 255, g = (rgb >> 8) & 255, b = rgb & 255;
		float k = 1 - amount;
		return (int) (r * k) << 16 | (int) (g * k) << 8 | (int) (b * k);
	}

	/** Moves an RGB color toward white by {@code amount} (0 to 1). */
	public static int lighten(int rgb, float amount) {
		int r = (rgb >> 16) & 255, g = (rgb >> 8) & 255, b = rgb & 255;
		r += (int) ((255 - r) * amount);
		g += (int) ((255 - g) * amount);
		b += (int) ((255 - b) * amount);
		return r << 16 | g << 8 | b;
	}
}
