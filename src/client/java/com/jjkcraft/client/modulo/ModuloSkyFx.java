package com.jjkcraft.client.modulo;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;

import com.jjkcraft.client.config.ClientConfig;
import com.jjkcraft.client.render.JjkRenderTypes;
import com.jjkcraft.modulo.entity.MahoragaEntity;

/**
 * World geometry for the Modulo expansion (docs/VFX_SPEC.md section 9): during a Naunax Descent, the
 * ship hanging over the badlands (a dark, flattened hull with a glowing rim) and its ramp of light
 * down to the ground; and the eight-spoked wheel over Mahoraga, which turns a step each time it adapts.
 * Plain geometry: the ship and its light with the mod's translucent type (no fog, so it reads from far
 * off), the wheel with the additive type.
 */
public final class ModuloSkyFx {
	/** Height of the ship's underside above the ramp's foot (canon: 30,000 m; the sky ends sooner here). */
	private static final double ALTITUDE = 150;
	private static final double HULL_RADIUS = 70;
	private static final double HULL_HEIGHT = 16;

	private ModuloSkyFx() {
	}

	public static void init() {
		LevelRenderEvents.COLLECT_SUBMITS.register(ModuloSkyFx::render);
	}

	private record Quads(PoseStack.Pose pose, VertexConsumer buffer, Vec3 camera) {
		void vertex(Vec3 p, int color) {
			buffer.addVertex(pose, (float) (p.x - camera.x), (float) (p.y - camera.y), (float) (p.z - camera.z)).setColor(color);
		}

		void quad(Vec3 a, Vec3 b, Vec3 c, Vec3 d, int color) {
			vertex(a, color);
			vertex(b, color);
			vertex(c, color);
			vertex(d, color);
		}

		/** Both windings, for the additive type, which culls back faces (the dome type does not). */
		void twoSided(Vec3 a, Vec3 b, Vec3 c, Vec3 d, int color) {
			quad(a, b, c, d, color);
			quad(d, c, b, a, color);
		}
	}

	private static void render(LevelRenderContext context) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.player == null) {
			return;
		}

		Vec3 camera = context.levelState().cameraRenderState.pos;
		float time = mc.level.getGameTime() + mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
		boolean low = ClientConfig.lowVfx();
		BlockPos foot = ClientModulo.view().descent().filter(p -> mc.level.dimension() == Level.OVERWORLD).orElse(null);
		List<MahoragaEntity> wheels = mc.level.getEntitiesOfClass(MahoragaEntity.class, mc.player.getBoundingBox().inflate(64));
		if (foot == null && wheels.isEmpty()) {
			return;
		}

		PoseStack poseStack = context.poseStack();
		if (foot != null) {
			Vec3 center = new Vec3(foot.getX() + 0.5, Math.min(foot.getY() + ALTITUDE, 300), foot.getZ() + 0.5);
			// One translucent, fog-free, double-sided pass: the ship is a landmark seen from far off,
			// past where fog would wash the additive light type into the sky.
			context.submitNodeCollector().submitCustomGeometry(poseStack, JjkRenderTypes.dome(), (pose, buffer) -> {
				Quads quads = new Quads(pose, buffer, camera);
				hull(quads, center, low);
				rim(quads, center, time, low);
				ramp(quads, center, Vec3.atBottomCenterOf(foot), camera, time);
			});
		}

		if (!wheels.isEmpty()) {
			float partial = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
			context.submitNodeCollector().submitCustomGeometry(poseStack, RenderTypes.lightning(), (pose, buffer) -> {
				Quads quads = new Quads(pose, buffer, camera);
				for (MahoragaEntity mahoraga : wheels) {
					wheel(quads, mahoraga, partial);
				}
			});
		}
	}

	/** A flattened ellipsoid, darker underneath. */
	private static void hull(Quads quads, Vec3 c, boolean low) {
		int segments = low ? 24 : 48;
		int rings = low ? 6 : 12;
		for (int ring = 0; ring < rings; ring++) {
			double lat0 = -Math.PI / 2 + Math.PI * ring / rings;
			double lat1 = -Math.PI / 2 + Math.PI * (ring + 1) / rings;
			int color = ARGB.color(230, ARGB.srgbLerp((float) ring / rings, 0xFF0C1410, 0xFF3A4A44));
			for (int s = 0; s < segments; s++) {
				double a0 = 2 * Math.PI * s / segments;
				double a1 = 2 * Math.PI * (s + 1) / segments;
				quads.quad(ellipsoid(c, lat0, a0), ellipsoid(c, lat0, a1), ellipsoid(c, lat1, a1), ellipsoid(c, lat1, a0), color);
			}
		}
	}

	private static Vec3 ellipsoid(Vec3 c, double lat, double lon) {
		double cos = Math.cos(lat);
		return new Vec3(c.x + Math.cos(lon) * cos * HULL_RADIUS, c.y + Math.sin(lat) * HULL_HEIGHT, c.z + Math.sin(lon) * cos * HULL_RADIUS);
	}

	/** Mul light running round the hull's waist, pulsing slowly. */
	private static void rim(Quads quads, Vec3 c, float time, boolean low) {
		int segments = low ? 32 : 64;
		float pulse = 0.6f + 0.4f * Mth.sin(time * 0.05f);
		int color = ARGB.color((int) (200 * pulse), ClientModulo.ACCENT & 0xFFFFFF);
		for (int s = 0; s < segments; s++) {
			double a0 = 2 * Math.PI * s / segments;
			double a1 = 2 * Math.PI * (s + 1) / segments;
			Vec3 p0 = new Vec3(c.x + Math.cos(a0) * HULL_RADIUS * 1.01, c.y - 1, c.z + Math.sin(a0) * HULL_RADIUS * 1.01);
			Vec3 p1 = new Vec3(c.x + Math.cos(a1) * HULL_RADIUS * 1.01, c.y - 1, c.z + Math.sin(a1) * HULL_RADIUS * 1.01);
			quads.quad(p0, p1, p1.add(0, 2, 0), p0.add(0, 2, 0), color);
		}
	}

	/** The ramp: a beam of light from the ship's belly to the ground, facing the camera. */
	private static void ramp(Quads quads, Vec3 c, Vec3 ground, Vec3 camera, float time) {
		Vec3 top = new Vec3(c.x, c.y - HULL_HEIGHT * 0.9, c.z);
		Vec3 side = ground.subtract(camera).multiply(1, 0, 1).cross(new Vec3(0, 1, 0)).normalize();
		if (side.lengthSqr() < 1e-6) {
			side = new Vec3(1, 0, 0);
		}

		float flicker = 0.8f + 0.2f * Mth.sin(time * 0.3f);
		int core = ARGB.color((int) (220 * flicker), 0xE8FFF0);
		int glow = ARGB.color((int) (90 * flicker), ClientModulo.ACCENT & 0xFFFFFF);
		quads.quad(ground.add(side.scale(1.2)), ground.subtract(side.scale(1.2)), top.subtract(side.scale(4)), top.add(side.scale(4)), glow);
		quads.quad(ground.add(side.scale(0.4)), ground.subtract(side.scale(0.4)), top.subtract(side.scale(1.5)), top.add(side.scale(1.5)), core);
	}

	/** Mahoraga's wheel: a ring of eight spokes above its head, easing round by one spoke per adaptation. */
	private static void wheel(Quads quads, MahoragaEntity mahoraga, float partial) {
		Vec3 base = mahoraga.getPosition(partial).add(0, mahoraga.getBbHeight() + 0.5, 0);
		float ease = Mth.clamp(ClientModulo.sinceWheelTurned(mahoraga.getId()) / 600f, 0, 1);
		double angle = Math.PI / 4 * (mahoraga.wheel() - 1 + ease * ease * (3 - 2 * ease));
		double radius = 1.1 * mahoraga.getScale();
		int gold = 0xD8F2D07A;
		int segments = 24;
		for (int s = 0; s < segments; s++) {
			double a0 = angle + 2 * Math.PI * s / segments;
			double a1 = angle + 2 * Math.PI * (s + 1) / segments;
			Vec3 p0 = base.add(Math.cos(a0) * radius, 0, Math.sin(a0) * radius);
			Vec3 p1 = base.add(Math.cos(a1) * radius, 0, Math.sin(a1) * radius);
			quads.twoSided(p0, p1, p1.add(0, 0.14, 0), p0.add(0, 0.14, 0), gold);
		}

		for (int spoke = 0; spoke < 8; spoke++) {
			double a = angle + Math.PI / 4 * spoke;
			Vec3 tip = base.add(Math.cos(a) * radius * 1.25, 0.07, Math.sin(a) * radius * 1.25);
			Vec3 side = new Vec3(-Math.sin(a), 0, Math.cos(a)).scale(0.05);
			quads.twoSided(base.add(side).add(0, 0.07, 0), base.subtract(side).add(0, 0.07, 0), tip.subtract(side), tip.add(side), gold);
			quads.twoSided(tip.add(0, -0.12, 0), tip.add(side.scale(2)), tip.add(0, 0.12, 0), tip.subtract(side.scale(2)), gold);
		}
	}
}
