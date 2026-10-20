package com.jjkcraft.client.vfx;

import java.util.Optional;

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
import com.jjkcraft.client.render.JjkRenderTypes;
import com.jjkcraft.client.state.ClientDomains;
import com.jjkcraft.domain.ActiveDomain;
import com.jjkcraft.domain.DomainDefinition;
import com.jjkcraft.network.payload.DomainWorldPayload;

/**
 * Draws domains in the world as plain vertex geometry (no shaders, so it works with Sodium and
 * Iris): a dome that hides the sky from inside (near-opaque for a barrier, a faint tint without
 * one), a turning glyph ring on the ground at the boundary, each domain's ambience, clash shards,
 * and the small rings of Simple Domains. The dome grows out of the caster while the domain opens
 * and fades when it closes.
 */
public final class DomainFx {
	private DomainFx() {
	}

	public static void init() {
		LevelRenderEvents.COLLECT_SUBMITS.register(DomainFx::render);
	}

	/** One frame's drawing state: single-sided quads in world space, relative to the camera. */
	private record Quads(PoseStack.Pose pose, VertexConsumer buffer, Vec3 camera) {
		void vertex(Vec3 p, int color) {
			buffer.addVertex(pose, (float) (p.x - camera.x), (float) (p.y - camera.y), (float) (p.z - camera.z)).setColor(color);
		}

		void quad(Vec3 a, Vec3 b, Vec3 c, Vec3 d, int ca, int cb, int cc, int cd) {
			vertex(a, ca);
			vertex(b, cb);
			vertex(c, cc);
			vertex(d, cd);
		}
	}

	private static void render(LevelRenderContext context) {
		if (ClientConfig.effect(ClientConfig.get().vfx.domains) == 0) return;
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || (ClientDomains.domains().isEmpty() && ClientDomains.simpleDomains().isEmpty())) {
			return;
		}

		Vec3 camera = context.levelState().cameraRenderState.pos;
		float partial = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
		float time = mc.level.getGameTime() + partial;
		boolean low = ClientConfig.lowVfx();
		PoseStack poseStack = context.poseStack();

		context.submitNodeCollector().submitCustomGeometry(poseStack, JjkRenderTypes.dome(), (pose, buffer) -> {
			Quads quads = new Quads(pose, buffer, camera);
			WorldFx.Frame interior=new WorldFx.Frame(pose,buffer,camera,time,low);
			for (ClientDomains.Domain domain : ClientDomains.domains().values()) {
				domain.definition().ifPresent(def -> dome(quads, domain, def, time, low));
				if(domain.contains(camera))domain.definition().ifPresent(def->DomainScenery.draw(interior,domain,def,alpha(domain,time)));
			}

			for (DomainWorldPayload.SimpleView simple : ClientDomains.simpleDomains()) {
				Entity holder = mc.level.getEntity(simple.casterEntity());
				if (holder != null) {
					simpleDome(quads, holder.getPosition(partial), simple.radius(), time, low);
				}
			}
		});

		context.submitNodeCollector().submitCustomGeometry(poseStack, RenderTypes.lightning(), (pose, buffer) -> {
			WorldFx.Frame frame = new WorldFx.Frame(pose, buffer, camera, time, low);
			for (ClientDomains.Domain domain : ClientDomains.domains().values()) {
				domain.definition().ifPresent(def -> {
					float alpha = alpha(domain, time);
					if (alpha <= 0) {
						return;
					}

					ring(frame, domain.center(), domain.radius() * grow(domain, time), def.visuals().color(), alpha, time);
					if (camera.distanceTo(domain.center()) < domain.radius() + 32) {
						ambience(frame, domain, def, alpha, time);
					}

					if (domain.phase() == ActiveDomain.Phase.CLASHING) {
						clash(frame, domain, def, time);
					}
					if(domain.barrier()&&domain.integrity()<0.8f){
						for(int n=0;n<(1-domain.integrity())*18;n++){double a=n*2.399;Vec3 p=domain.center().add(Math.cos(a)*domain.radius()*0.97,n%4,Math.sin(a)*domain.radius()*0.97);frame.beam(p,p.add(0,1.4,0),0.045,0xCCF3DFFF,0x44FFFFFF);}
					}
				});
			}

			for (DomainWorldPayload.SimpleView simple : ClientDomains.simpleDomains()) {
				Entity holder = mc.level.getEntity(simple.casterEntity());
				if (holder != null) {
					ring(frame, holder.getPosition(partial), simple.radius(), 0x9FD8E8, 0.8f, time);
				}
			}
		});
	}

	// ---------------------------------------------------------------- timing

	/** 0 to 1 while opening (the dome grows from the caster), 1 while open. */
	static float grow(ClientDomains.Domain domain, float time) {
		float t = Mth.clamp((time - domain.openedAt()) / ActiveDomain.OPENING_TICKS, 0, 1);
		return ClientConfig.reducedMotion() ? 1 : 1 - (1 - t) * (1 - t) * (1 - t);
	}

	static float alpha(ClientDomains.Domain domain, float time) {
		if (domain.phase() == ActiveDomain.Phase.CLOSING) {
			return Mth.clamp(1 - (time - domain.phaseStart()) / ActiveDomain.CLOSING_TICKS, 0, 1);
		}

		return Mth.clamp((time - domain.openedAt()) / 12f, 0, 1);
	}

	// ---------------------------------------------------------------- shapes

	private static void dome(Quads quads, ClientDomains.Domain domain, DomainDefinition def, float time, boolean low) {
		float alpha = alpha(domain, time);
		if (alpha <= 0) {
			return;
		}

		double radius = domain.radius() * grow(domain, time);
		int rings = low ? 8 : 14;
		int segments = low ? 18 : 36;
		float base = domain.barrier() ? 0.86f : 0.22f;
		int color = def.visuals().dome();
		Vec3 c = domain.center();
		for (int ring = 0; ring < rings; ring++) {
			double lat0 = -Math.PI / 2 + Math.PI * ring / rings;
			double lat1 = -Math.PI / 2 + Math.PI * (ring + 1) / rings;
			for (int seg = 0; seg < segments; seg++) {
				double lon0 = 2 * Math.PI * seg / segments;
				double lon1 = 2 * Math.PI * (seg + 1) / segments;
				Vec3 a = point(c, radius, lat0, lon0);
				Vec3 b = point(c, radius, lat0, lon1);
				Vec3 d = point(c, radius, lat1, lon0);
				Vec3 e = point(c, radius, lat1, lon1);
				quads.quad(a, b, e, d, shade(color, base, alpha, lat0, lon0, time), shade(color, base, alpha, lat0, lon1, time),
						shade(color, base, alpha, lat1, lon1, time), shade(color, base, alpha, lat1, lon0, time));
			}
		}
	}

	private static Vec3 point(Vec3 center, double radius, double lat, double lon) {
		double cos = Math.cos(lat);
		return center.add(Math.cos(lon) * cos * radius, Math.sin(lat) * radius, Math.sin(lon) * cos * radius);
	}

	/** Dome color with slow bands drifting over it, lighter toward the top. */
	private static int shade(int color, float base, float alpha, double lat, double lon, float time) {
		double band = 0.5 + 0.5 * Math.sin(lat * 7 + lon * 2 + time * 0.03);
		float light = (float) (0.75 + 0.25 * band + 0.2 * Math.max(0, Math.sin(lat)));
		int rgb = ARGB.scaleRGB(color, Math.min(1.6f, light));
		return ARGB.color(Mth.clamp((int) (255 * base * alpha), 0, 255), rgb & 0xFFFFFF);
	}

	private static void simpleDome(Quads quads, Vec3 center, float radius, float time, boolean low) {
		int segments = low ? 12 : 24;
		int rings = low ? 4 : 6;
		for (int ring = 0; ring < rings; ring++) {
			double lat0 = Math.PI / 2 * ring / rings;
			double lat1 = Math.PI / 2 * (ring + 1) / rings;
			for (int seg = 0; seg < segments; seg++) {
				double lon0 = 2 * Math.PI * seg / segments + time * 0.01;
				double lon1 = 2 * Math.PI * (seg + 1) / segments + time * 0.01;
				int c0 = ARGB.color(seg % 2 == 0 ? 34 : 22, 0x9FD8E8);
				quads.quad(point(center, radius, lat0, lon0), point(center, radius, lat0, lon1), point(center, radius, lat1, lon1),
						point(center, radius, lat1, lon0), c0, c0, c0, c0);
			}
		}
	}

	/** The glyph ring: dashed arcs and glyph ticks on the ground at the boundary, turning slowly. */
	private static void ring(WorldFx.Frame frame, Vec3 center, double radius, int color, float alpha, float time) {
		int seg = frame.segments(Math.max(24, (int) (radius * 5)));
		double spin = time * 0.01;
		double y = center.y + 0.06;
		for (int i = 0; i < seg; i++) {
			double a0 = spin + 2 * Math.PI * i / seg;
			double a1 = spin + 2 * Math.PI * (i + 0.72) / seg;
			Vec3 p0 = new Vec3(center.x + Math.cos(a0) * radius, y, center.z + Math.sin(a0) * radius);
			Vec3 p1 = new Vec3(center.x + Math.cos(a1) * radius, y, center.z + Math.sin(a1) * radius);
			Vec3 q0 = new Vec3(center.x + Math.cos(a0) * (radius - 0.35), y, center.z + Math.sin(a0) * (radius - 0.35));
			Vec3 q1 = new Vec3(center.x + Math.cos(a1) * (radius - 0.35), y, center.z + Math.sin(a1) * (radius - 0.35));
			frame.quad(q0, p0, p1, q1, WorldFx.fade(color, alpha), WorldFx.fade(color, alpha));
			if (i % 4 == 0) {
				// A glyph: a short spoke and a crossbar.
				Vec3 inner = new Vec3(center.x + Math.cos(a0) * (radius - 1.1), y, center.z + Math.sin(a0) * (radius - 1.1));
				frame.beam(q0, inner, 0.08, WorldFx.fade(color, alpha * 0.9f), WorldFx.fade(color, alpha * 0.2f));
				Vec3 mid = q0.lerp(inner, 0.6);
				Vec3 across = new Vec3(-Math.sin(a0), 0, Math.cos(a0)).scale(0.3);
				frame.beam(mid.subtract(across), mid.add(across), 0.07, WorldFx.fade(color, alpha * 0.7f), WorldFx.fade(color, alpha * 0.7f));
			}
		}
	}

	private static void ambience(WorldFx.Frame frame, ClientDomains.Domain domain, DomainDefinition def, float alpha, float time) {
		int color = def.visuals().color();
		int secondary = def.visuals().secondary();
		Vec3 c = domain.center();
		double r = domain.radius() * grow(domain, time);
		switch (def.visuals().ambience()) {
			case "blades" -> {
				// Hairline cuts flicker through the air everywhere.
				int count = frame.segments(14);
				long bucket = (long) (time / 3);
				for (int i = 0; i < count; i++) {
					long h = hash(bucket * 31 + i);
					Vec3 p = c.add(((h & 0xFF) / 255.0 - 0.5) * r * 1.4, 0.3 + ((h >> 8) & 0xFF) / 255.0 * 3.5, ((h >> 16) & 0xFF) / 255.0 * r * 1.4 - r * 0.7);
					double yaw = ((h >> 24) & 0xFF) / 255.0 * Math.PI;
					Vec3 dir = new Vec3(Math.cos(yaw), ((h >> 4) & 0xF) / 15.0 - 0.5, Math.sin(yaw)).normalize().scale(1.2);
					float flicker = 1 - (time % 3) / 3f;
					frame.beam(p.subtract(dir), p.add(dir), 0.04, WorldFx.fade(i % 3 == 0 ? secondary : color, alpha * flicker),
							WorldFx.fade(color, alpha * flicker * 0.4f));
				}
			}
			case "glyphs" -> {
				// Columns of glyphs stream up the inside of the dome.
				int columns = frame.segments(28);
				for (int i = 0; i < columns; i++) {
					double a = 2 * Math.PI * i / columns + 0.13 * i;
					double cr = r * 0.85;
					for (int k = 0; k < 4; k++) {
						double y = ((time * 0.12 + i * 1.7 + k * 2.3) % 8.0);
						Vec3 p = new Vec3(c.x + Math.cos(a) * cr, c.y + y, c.z + Math.sin(a) * cr);
						float fadeY = (float) (1 - y / 8.0);
						frame.beam(p, p.add(0, 0.35, 0), 0.22, WorldFx.fade(k % 2 == 0 ? color : secondary, alpha * fadeY),
								WorldFx.fade(color, alpha * fadeY * 0.5f));
					}
				}
			}
			case "shadows" -> {
				// Low tendrils sway up from the ground and sink again.
				int count = frame.segments(24);
				for (int i = 0; i < count; i++) {
					long h = hash(i * 977L + 13);
					double a = (h & 0xFFFF) / 65535.0 * Math.PI * 2;
					double d = r * (0.2 + ((h >> 16) & 0xFF) / 255.0 * 0.75);
					Vec3 base = new Vec3(c.x + Math.cos(a) * d, c.y + 0.05, c.z + Math.sin(a) * d);
					double height = 0.8 + 0.7 * Math.sin(time * 0.05 + i);
					Vec3 sway = new Vec3(Math.sin(time * 0.07 + i) * 0.4, height, Math.cos(time * 0.06 + i) * 0.4);
					frame.beam(base, base.add(sway), 0.18, WorldFx.fade(color, alpha * 0.55f), WorldFx.fade(color, 0));
				}
			}
			case "lanterns" -> {
				// Paper lanterns bob in a ring overhead: a warm body, a dim cap, a faint string.
				int count = frame.segments(16);
				for (int i = 0; i < count; i++) {
					double a = 2 * Math.PI * i / count + time * 0.004;
					double y = 3.6 + 0.3 * Math.sin(time * 0.08 + i * 1.3);
					Vec3 p = new Vec3(c.x + Math.cos(a) * r * 0.6, c.y + y, c.z + Math.sin(a) * r * 0.6);
					float flicker = 0.75f + 0.25f * Mth.sin(time * 0.3f + i * 2.1f);
					frame.beam(p, p.add(0, 0.32, 0), 0.24, WorldFx.fade(secondary, alpha * 0.35f * flicker), WorldFx.fade(color, alpha * 0.45f * flicker));
					frame.beam(p.add(0, 0.32, 0), p.add(0, 0.38, 0), 0.14, WorldFx.fade(secondary, alpha * 0.3f), WorldFx.fade(secondary, alpha * 0.3f));
					frame.beam(p.add(0, 0.38, 0), p.add(0, 1.4, 0), 0.02, WorldFx.fade(color, alpha * 0.15f), WorldFx.fade(color, 0));
				}
			}
			case "arsenal" -> {
				// Blades orbit the center, points down.
				int count = frame.segments(10);
				for (int i = 0; i < count; i++) {
					double a = 2 * Math.PI * i / count + time * 0.015;
					Vec3 hilt = new Vec3(c.x + Math.cos(a) * r * 0.45, c.y + 3.0 + 0.2 * Math.sin(time * 0.1 + i), c.z + Math.sin(a) * r * 0.45);
					frame.beam(hilt, hilt.add(0, -1.6, 0), 0.12, WorldFx.fade(secondary, alpha), WorldFx.fade(color, alpha * 0.3f));
					frame.beam(hilt.add(-0.25, -0.15, 0), hilt.add(0.25, -0.15, 0), 0.06, WorldFx.fade(color, alpha), WorldFx.fade(color, alpha));
				}
			}
			case "rings" -> {
				// Rings ripple outward from the center on a steady beat.
				float beat = (time % 40) / 40f;
				ring(frame, c, Math.max(0.5, r * beat), secondary, alpha * (1 - beat), -time);
			}
			default -> {
			}
		}
	}

	/** Jagged shards along the plane where two domains push against each other. */
	private static void clash(WorldFx.Frame frame, ClientDomains.Domain domain, DomainDefinition def, float time) {
		Optional<ClientDomains.Domain> other = ClientDomains.domains().values().stream()
				.filter(d -> d.instance() != domain.instance() && d.phase() == ActiveDomain.Phase.CLASHING)
				.min((a, b) -> Double.compare(a.center().distanceTo(domain.center()), b.center().distanceTo(domain.center())));
		if (other.isEmpty()) {
			return;
		}

		Vec3 a = domain.center();
		Vec3 b = other.get().center();
		Vec3 axis = b.subtract(a).multiply(1, 0, 1);
		if (axis.lengthSqr() < 1e-6) {
			return;
		}

		axis = axis.normalize();
		double t = domain.radius() / (domain.radius() + other.get().radius());
		Vec3 mid = a.lerp(b, t);
		Vec3 side = new Vec3(-axis.z, 0, axis.x);
		int shards = frame.segments(16);
		long bucket = (long) (time / 2);
		for (int i = 0; i < shards; i++) {
			long h = hash(bucket * 17 + i + domain.instance() * 101L);
			double s = ((h & 0xFF) / 255.0 - 0.5) * 8;
			double y = ((h >> 8) & 0xFF) / 255.0 * 6;
			Vec3 p = mid.add(side.scale(s)).add(0, y, 0);
			Vec3 q = p.add(axis.scale(((h >> 16) & 0xF) / 15.0 - 0.5)).add(0, 0.6, 0);
			frame.beam(p, q, 0.1, WorldFx.fade(def.visuals().color(), 0.9f), WorldFx.fade(0xFFFFFF, 0.6f));
		}
	}

	// ---------------------------------------------------------------- events

	/** A sure-hit beat: the cutting field slashes its targets, other domains flash their ring. */
	public static void onPulse(ClientDomains.Domain domain) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) {
			return;
		}

		domain.definition().ifPresent(def -> {
			float now = mc.level.getGameTime();
			if ("blades".equals(def.visuals().ambience())) {
				int count = ClientConfig.lowVfx() ? 2 : 5;
				for (int i = 0; i < count; i++) {
					long h = hash((long) now * 7 + i);
					Vec3 at = domain.center().add(((h & 0xFF) / 255.0 - 0.5) * domain.radius(), 1 + ((h >> 8) & 0x3), ((h >> 16) & 0xFF) / 255.0 * domain.radius()
							- domain.radius() / 2);
					WorldFx.add(WorldFx.slash(at, ((h >> 24) & 0xFF) / 255.0 * Math.PI * 2, 1.6, 140, ((h >> 4) & 0xF) / 15.0 - 0.5,
							def.visuals().color(), now));
				}
			}
		});
	}

	/** A recitation lands: a ring sweeps inward over the whole domain. */
	public static void onRepeat(ClientDomains.Domain domain, int count) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) {
			return;
		}

		domain.definition().ifPresent(def -> WorldFx.add(WorldFx.glyphRing(domain.center(), domain.radius() * 0.9, def.visuals().color(),
				mc.level.getGameTime(), 30, true)));
		ClientFx.shake(0.25f, 200);
	}

	private static long hash(long v) {
		v ^= v >>> 33;
		v *= 0xff51afd7ed558ccdL;
		v ^= v >>> 33;
		v *= 0xc4ceb9fe1a85ec53L;
		v ^= v >>> 33;
		return v & Long.MAX_VALUE;
	}
}
