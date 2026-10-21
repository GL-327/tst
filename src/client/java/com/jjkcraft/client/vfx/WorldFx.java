package com.jjkcraft.client.vfx;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.BooleanSupplier;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;

import com.jjkcraft.client.config.ClientConfig;

/**
 * Short-lived world effects drawn as vertex geometry (not particles): slash ribbons, cut lines,
 * glyph rings, chains, burst shells, shout waves and auras. All are additive, self-lit and
 * submitted through the vanilla {@code lightning} render type, so they work with Sodium and need
 * no custom shader. Low VFX halves the segment counts; a per-frame quad budget caps the cost.
 */
public final class WorldFx {
	/** Geometry budget per frame. */
	private static final int MAX_QUADS = 6000;
	private static long profileNanos, profilePeak;
	private static int profileFrames, profileQuads;
	private static int lastFrameQuads;
	/** Most recent rendered geometry count, including the solid and additive passes. */
	public static int lastFrameQuads(){return lastFrameQuads;}

	/** One effect. Returns false when finished. */
	public interface Effect {
		boolean render(Frame frame);
	}

	/** Per-frame drawing context with camera-relative helpers. */
	public static final class Frame {
		final PoseStack.Pose pose;
		final VertexConsumer buffer;
		final Vec3 camera;
		final float time;
		final boolean low;
		int quads;
        int[] shared=new int[1];
		private float opacity=1;
		public void opacity(float value){opacity=Math.clamp(value,0,1);}
		final long began = System.nanoTime();

		Frame(PoseStack.Pose pose, VertexConsumer buffer, Vec3 camera, float time, boolean low) {
			this.pose = pose;
			this.buffer = buffer;
			this.camera = camera;
			this.time = time;
			this.low = low;
		}

		/** Game time in ticks with partial tick, for animation. */
		public float time() {
			return time;
		}

		public int segments(int high) {
			return low ? Math.max(4, high / 2) : high;
		}

		public boolean budgetLeft() {
			return shared[0] < Math.min(MAX_QUADS, Math.max(100, ClientConfig.get().vfx.maxWorldQuads))
				&& System.nanoTime() - began < Math.clamp(ClientConfig.get().vfx.maxEffectMillis, 1, 10) * 1_000_000L;
		}

		/** A double-sided quad from four world points. */
		public void quad(Vec3 a, Vec3 b, Vec3 c, Vec3 d, int colorA, int colorB) {
			if (!budgetLeft()) {
				return;
			}

			quads++;
            shared[0]++;
			vertex(a, colorA);
			vertex(b, colorA);
			vertex(c, colorB);
			vertex(d, colorB);
			vertex(d, colorB);
			vertex(c, colorB);
			vertex(b, colorA);
			vertex(a, colorA);
		}

		/** A camera-facing strip between two world points. */
		public void beam(Vec3 from, Vec3 to, double width, int colorFrom, int colorTo) {
			Vec3 axis = to.subtract(from);
			Vec3 toCam = camera.subtract(from.lerp(to, 0.5));
			Vec3 side = axis.cross(toCam);
			if (side.lengthSqr() < 1e-8) {
				side = axis.cross(new Vec3(0, 1, 0));
			}

			side = side.normalize().scale(width / 2);
			quad(from.add(side), from.subtract(side), to.subtract(side), to.add(side), colorFrom, colorTo);
		}

		private void vertex(Vec3 p, int color) {
			if(opacity<1)color=ARGB.multiplyAlpha(color,opacity);
			buffer.addVertex(pose, (float) (p.x - camera.x), (float) (p.y - camera.y), (float) (p.z - camera.z)).setColor(color);
		}
	}

	private static final List<Effect> EFFECTS = new ArrayList<>();
    private static final List<Effect> SOLID=new ArrayList<>();
    public static void addSolid(Effect effect){if(!ClientConfig.effectsOff()&&SOLID.size()<96)SOLID.add(effect);}

	private WorldFx() {
	}

	public static void init() {
		LevelRenderEvents.COLLECT_SUBMITS.register(WorldFx::render);
	}

	public static void add(Effect effect) {
		if (ClientConfig.effectsOff()) return;
		if (EFFECTS.size() < 256) {
			EFFECTS.add(effect);
		}
	}

	public static void clear() {
		lastFrameQuads=0;
		EFFECTS.clear();
        SOLID.clear();
	}

	private static void render(LevelRenderContext context) {
		Minecraft mc = Minecraft.getInstance();
		if (ClientConfig.effectsOff() || EFFECTS.isEmpty()&&SOLID.isEmpty() || mc.level == null) {
			lastFrameQuads=0;
			return;
		}

		Vec3 camera = context.levelState().cameraRenderState.pos;
		float time = mc.level.getGameTime() + mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
		boolean low = ClientConfig.lowVfx();
		PoseStack poseStack = context.poseStack();
        int[] shared=new int[1];
        context.submitNodeCollector().submitCustomGeometry(poseStack,RenderTypes.debugQuads(),(pose,buffer)->{
            Frame frame=new Frame(pose,buffer,camera,time,low);frame.shared=shared;
            for(Iterator<Effect> it=SOLID.iterator();it.hasNext();)if(!it.next().render(frame))it.remove();
        });
		context.submitNodeCollector().submitCustomGeometry(poseStack, RenderTypes.lightning(), (pose, buffer) -> {
			Frame frame = new Frame(pose, buffer, camera, time, low);
            frame.shared=shared;
			for (Iterator<Effect> it = EFFECTS.iterator(); it.hasNext(); ) {
				if (!it.next().render(frame)) {
					it.remove();
				}
			}
			long elapsed = System.nanoTime() - frame.began;
			lastFrameQuads=shared[0];
			profileNanos += elapsed; profilePeak = Math.max(profilePeak, elapsed); profileQuads = Math.max(profileQuads, frame.quads);
			if (++profileFrames >= 300) {
				if (ClientConfig.get().vfx.logProfiling) com.jjkcraft.JjkCraft.LOGGER.info("VFX profile: frames={} mean_cpu_ms={} peak_cpu_ms={} peak_quads={} active={}", profileFrames, profileNanos / (profileFrames * 1_000_000.0), profilePeak / 1_000_000.0, profileQuads, EFFECTS.size());
				profileFrames = profileQuads = 0; profileNanos = profilePeak = 0;
			}
		});
	}

	// ---------------------------------------------------------------- effect shapes

	static float life(float start, float duration, float now) {
		return (now - start) / duration;
	}

	static int fade(int color, float alpha) {
		return ARGB.color(Mth.clamp((int) (alpha * 255), 0, 255), color & 0xFFFFFF);
	}

	/**
	 * A sweeping crescent ribbon in front of {@code origin}, facing {@code yaw} (radians). The arc
	 * lies in a plane rolled {@code tilt} radians around the facing direction, so it reads as a
	 * diagonal cut from eye level instead of a flat line.
	 */
	public static Effect slash(Vec3 origin, double yaw, double radius, double arcDegrees, double tilt, int color, float start) {
		float duration = 7f;
		Vec3 forward = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
		Vec3 right = new Vec3(Math.cos(yaw), 0, Math.sin(yaw));
		Vec3 side = right.scale(Math.cos(tilt)).add(0, Math.sin(tilt), 0);
		return frame -> {
			float t = life(start, duration, frame.time());
			if (t >= 1) {
				return false;
			}

			if (t < 0) {
				return true;
			}

			int seg = frame.segments(24);
			double half = Math.toRadians(arcDegrees) / 2;
			double sweep = Math.min(1, t * 2.5);
			double fadeOut = t < 0.4 ? 1 : 1 - (t - 0.4) / 0.6;
			for (int i = 0; i < seg; i++) {
				double f0 = (double) i / seg;
				double f1 = (double) (i + 1) / seg;
				if (f1 > sweep) {
					break;
				}

				double a0 = -half + 2 * half * f0;
				double a1 = -half + 2 * half * f1;
				double w0 = 0.2 + 0.7 * Math.sin(Math.PI * f0);
				double w1 = 0.2 + 0.7 * Math.sin(Math.PI * f1);
				Vec3 o0 = plane(origin, forward, side, a0, radius);
				Vec3 o1 = plane(origin, forward, side, a1, radius);
				Vec3 i0 = plane(origin, forward, side, a0, radius - w0);
				Vec3 i1 = plane(origin, forward, side, a1, radius - w1);
				float trail = (float) Math.max(0, 1 - (sweep - f1) * 3);
				int c = fade(color, (float) (fadeOut * (0.35 + 0.65 * trail)));
				frame.quad(i0, o0, o1, i1, c, c);
			}

			return true;
		};
	}

	private static Vec3 plane(Vec3 origin, Vec3 forward, Vec3 side, double angle, double r) {
		return origin.add(forward.scale(Math.cos(angle) * r)).add(side.scale(Math.sin(angle) * r));
	}

	private static Vec3 arc(Vec3 origin, double angle, double r, double lift) {
		return origin.add(-Math.sin(angle) * r, lift, Math.cos(angle) * r);
	}

	/** A bright straight cut that thins out. */
	public static Effect line(Vec3 from, Vec3 to, double width, int color, float start, float duration) {
		return frame -> {
			float t = life(start, duration, frame.time());
			if (t >= 1) {
				return false;
			}

			double w = width * (1 - t);
			frame.beam(from, to, w, fade(color, 1 - t), fade(color, 0.6f * (1 - t)));
			frame.beam(from, to, w * 0.35, fade(0xFFFFFF, 1 - t), fade(0xFFFFFF, 0.5f * (1 - t)));
			return true;
		};
	}

	/**
	 * A ring of glyph ticks on the ground, slowly rotating. Used for fields now and for domain
	 * boundaries later.
	 */
	public static Effect glyphRing(Vec3 center, double radius, int color, float start, float duration, boolean inward) {
		return frame -> {
			float t = life(start, duration, frame.time());
			if (t >= 1) {
				return false;
			}

			float appear = Math.min(1, t * 8);
			float alpha = Math.min(appear, t > 0.85f ? (1 - t) / 0.15f : 1);
			int seg = frame.segments(48);
			double spin = frame.time() * (inward ? -0.04 : 0.04);
			double r = radius * (0.85 + 0.15 * appear);
			for (int i = 0; i < seg; i++) {
				double a0 = spin + 2 * Math.PI * i / seg;
				double a1 = spin + 2 * Math.PI * (i + 0.7) / seg;
				Vec3 p0 = center.add(Math.cos(a0) * r, 0.05, Math.sin(a0) * r);
				Vec3 p1 = center.add(Math.cos(a1) * r, 0.05, Math.sin(a1) * r);
				Vec3 q0 = center.add(Math.cos(a0) * (r - 0.25), 0.05, Math.sin(a0) * (r - 0.25));
				Vec3 q1 = center.add(Math.cos(a1) * (r - 0.25), 0.05, Math.sin(a1) * (r - 0.25));
				frame.quad(q0, p0, p1, q1, fade(color, alpha), fade(color, alpha));
				// Glyph ticks every third segment.
				if (i % 3 == 0) {
					Vec3 g0 = center.add(Math.cos(a0) * (r - 0.6), 0.05, Math.sin(a0) * (r - 0.6));
					frame.beam(q0, g0, 0.1, fade(color, alpha * 0.8f), fade(color, 0));
				}
			}

			// A vertical shimmer column at the center.
			frame.beam(center, center.add(0, 1.8 * appear, 0), 0.25, fade(color, alpha * 0.4f), fade(color, 0));
			return true;
		};
	}

	/** A zigzag chain between two entities that follows them while they move. */
	public static Effect chain(int fromId, int toId, int color, float start, float duration) {
		return frame -> {
			float t = life(start, duration, frame.time());
			Minecraft mc = Minecraft.getInstance();
			if (t >= 1 || mc.level == null) {
				return false;
			}

			Entity a = mc.level.getEntity(fromId);
			Entity b = mc.level.getEntity(toId);
			if (a == null || b == null) {
				return false;
			}

			float partial = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
			Vec3 from = a.getPosition(partial).add(0, a.getBbHeight() * 0.6, 0);
			Vec3 to = b.getPosition(partial).add(0, b.getBbHeight() * 0.5, 0);
			int links = frame.segments(16);
			float alpha = t > 0.85f ? (1 - t) / 0.15f : 1;
			Vec3 prev = from;
			for (int i = 1; i <= links; i++) {
				double f = (double) i / links;
				Vec3 p = from.lerp(to, f).add(0, Math.sin(f * Math.PI) * -0.4, 0);
				Vec3 jitter = new Vec3(Math.sin(i * 1.7 + frame.time() * 0.3), Math.cos(i * 2.3), Math.sin(i * 0.9)).scale(0.06);
				p = p.add(jitter);
				frame.beam(prev, p, i % 2 == 0 ? 0.12 : 0.06, fade(color, alpha), fade(color, alpha));
				prev = p;
			}

			// Loops around the bound target.
			for (int k = 0; k < 2; k++) {
				double y = b.getBbHeight() * (0.3 + 0.35 * k);
				double rr = b.getBbWidth() * 0.75;
				Vec3 c = b.getPosition(partial).add(0, y, 0);
				int seg = frame.segments(16);
				for (int i = 0; i < seg; i++) {
					double a0 = 2 * Math.PI * i / seg + k;
					double a1 = 2 * Math.PI * (i + 1) / seg + k;
					frame.beam(c.add(Math.cos(a0) * rr, 0, Math.sin(a0) * rr), c.add(Math.cos(a1) * rr, 0, Math.sin(a1) * rr), 0.08,
							fade(color, alpha), fade(color, alpha));
				}
			}

			return true;
		};
	}

	/** An expanding three-ring shell. */
	public static Effect burst(Vec3 center, double radius, int color, float start) {
		float duration = 10f;
		return frame -> {
			float t = life(start, duration, frame.time());
			if (t >= 1) {
				return false;
			}

			double r = radius * (0.2 + 0.8 * Math.sqrt(t));
			float alpha = 1 - t;
			int seg = frame.segments(32);
			for (int axis = 0; axis < 3; axis++) {
				for (int i = 0; i < seg; i++) {
					double a0 = 2 * Math.PI * i / seg;
					double a1 = 2 * Math.PI * (i + 1) / seg;
					frame.beam(ring(center, axis, a0, r), ring(center, axis, a1, r), 0.25 * (1 - t) + 0.05, fade(color, alpha), fade(color, alpha));
				}
			}

			return true;
		};
	}

	private static Vec3 ring(Vec3 c, int axis, double a, double r) {
		double x = Math.cos(a) * r;
		double y = Math.sin(a) * r;
		return switch (axis) {
			case 0 -> c.add(x, 0, y);
			case 1 -> c.add(x, y, 0);
			default -> c.add(0, x, y);
		};
	}

	/** Expanding arcs in front of the caster for cursed speech. */
	public static Effect shout(Vec3 origin, double yaw, double radius, int color, float start) {
		float duration = 12f;
		return frame -> {
			float t = life(start, duration, frame.time());
			if (t >= 1) {
				return false;
			}

			int seg = frame.segments(20);
			for (int wave = 0; wave < 3; wave++) {
				float wt = t * 1.4f - wave * 0.18f;
				if (wt <= 0 || wt >= 1) {
					continue;
				}

				double r = radius * wt;
				float alpha = (1 - wt) * 0.9f;
				for (int i = 0; i < seg; i++) {
					double a0 = yaw - 1.1 + 2.2 * i / seg;
					double a1 = yaw - 1.1 + 2.2 * (i + 1) / seg;
					frame.beam(arc(origin, a0, r, 0), arc(origin, a1, r, 0), 0.18, fade(color, alpha), fade(color, alpha));
				}
			}

			return true;
		};
	}

	/** Rising rings around an entity while a channel or toggle technique runs. */
	public static Effect aura(int entityId, int color, float start, BooleanSupplier alive) {
		return aura(entityId,color,start,alive,false);
	}
	public static Effect aura(int entityId, int color, float start, BooleanSupplier alive, boolean infinity) {
		return frame -> {
			Minecraft mc = Minecraft.getInstance();
			Entity e = mc.level == null ? null : mc.level.getEntity(entityId);
			if (e == null || (!alive.getAsBoolean() && frame.time()-start>3)) {
				return false;
			}

			float partial = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
			Vec3 base = e.getPosition(partial);
			boolean firstPerson=e==mc.player&&mc.options.getCameraType().isFirstPerson();
			if(infinity){
				double radius=e.getBbWidth()*.5+.48;int count=frame.segments(32);float spin=(frame.time()-start)*.018f;
				for(int i=0;i<count&&frame.budgetLeft();i++){double a=i*Math.PI*2/count+spin,b=(i+.65)*Math.PI*2/count+spin;frame.beam(base.add(Math.cos(a)*radius,.07,Math.sin(a)*radius),base.add(Math.cos(b)*radius,.07,Math.sin(b)*radius),.025,fade(color,.34f),fade(0xDBF7FF,.15f));}
				if(!firstPerson)for(int side=0;side<8;side++){
					double a=side*Math.PI/4+spin*.3;Vec3 edge=base.add(Math.cos(a)*radius,0,Math.sin(a)*radius),tangent=new Vec3(-Math.sin(a)*.15,0,Math.cos(a)*.15);
					for(int n=0;n<4;n++){double y=.15+n*e.getBbHeight()/4;Vec3 p=edge.add(0,y,0);frame.beam(p.subtract(tangent),p.add(tangent),.018,fade(color,.3f),fade(0xDBF7FF,.1f));frame.beam(p,p.add(0,.12,0),.02,fade(color,.24f),fade(color,.08f));}
				}
				return true;
			}
			double rr = e.getBbWidth() * 0.9;
			int seg = frame.segments(20);
			for (int k = 0; k < 3; k++) {
				float phase = ((frame.time() - start) * 0.04f + k / 3f) % 1f;
				double y = e.getBbHeight() * phase;
				if(firstPerson&&y>.55)continue;
				float alpha = (float) Math.sin(Math.PI * phase) * 0.6f;
				for (int i = 0; i < seg; i++) {
					double a0 = 2 * Math.PI * i / seg;
					double a1 = 2 * Math.PI * (i + 1) / seg;
					frame.beam(base.add(Math.cos(a0) * rr, y, Math.sin(a0) * rr), base.add(Math.cos(a1) * rr, y, Math.sin(a1) * rr), 0.07,
							fade(color, alpha), fade(color, alpha));
				}
			}

			return true;
		};
	}
}
