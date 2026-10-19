package com.jjkcraft.client.vfx;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;

import com.jjkcraft.client.config.ClientConfig;
import com.jjkcraft.client.render.JjkRenderTypes;
import com.jjkcraft.client.state.ClientCulling;
import com.jjkcraft.core.culling.CullingMath;
import com.jjkcraft.network.payload.CullingPayloads;

/**
 * Colony barriers: a dark curtain wall that rises from the ground and closes overhead in a dome,
 * with slow streaks running down it like wet ink. Thinner from inside, so a colony is gloomy rather
 * than blind. Plain geometry with the vanilla translucent type, like the domain domes.
 */
public final class CullingFx {
	private static final int INK = 0x0B0912;
	private static final int STREAK = 0x2A1F38;
	/** Beyond this distance past the barrier it is not drawn. */
	private static final double VIEW = 320;

	private CullingFx() {
	}

	public static void init() {
		LevelRenderEvents.COLLECT_SUBMITS.register(CullingFx::render);
	}

	private record Quads(PoseStack.Pose pose, VertexConsumer buffer, Vec3 camera) {
		void vertex(double x, double y, double z, int color) {
			buffer.addVertex(pose, (float) (x - camera.x), (float) (y - camera.y), (float) (z - camera.z)).setColor(color);
		}

		/** One winding: the translucent type draws both faces, and a second copy would double the ink. */
		void quad(Vec3 a, Vec3 b, Vec3 c, Vec3 d, int ca, int cb, int cc, int cd) {
			vertex(a.x, a.y, a.z, ca);
			vertex(b.x, b.y, b.z, cb);
			vertex(c.x, c.y, c.z, cc);
			vertex(d.x, d.y, d.z, cd);
		}
	}

	private static void render(LevelRenderContext context) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || !ClientCulling.running()) {
			return;
		}

		Vec3 camera = context.levelState().cameraRenderState.pos;
		List<CullingPayloads.ColonyView> shown = new ArrayList<>();
		for (CullingPayloads.ColonyView colony : ClientCulling.view().colonies()) {
			double dx = camera.x - (colony.center().getX() + 0.5);
			double dz = camera.z - (colony.center().getZ() + 0.5);
			if (colony.dimension().equals(mc.level.dimension()) && Math.sqrt(dx * dx + dz * dz) < colony.radius() + VIEW) {
				shown.add(colony);
			}
		}

		if (shown.isEmpty()) {
			return;
		}

		float time = mc.level.getGameTime() + mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
		boolean low = ClientConfig.lowVfx();
		context.submitNodeCollector().submitCustomGeometry(context.poseStack(), JjkRenderTypes.dome(), (pose, buffer) -> {
			Quads quads = new Quads(pose, buffer, camera);
			for (CullingPayloads.ColonyView colony : shown) {
				barrier(quads, colony, camera, time, low);
			}
		});
	}

	private static void barrier(Quads quads, CullingPayloads.ColonyView colony, Vec3 camera, float time, boolean low) {
		double cx = colony.center().getX() + 0.5;
		double cy = colony.center().getY();
		double cz = colony.center().getZ() + 0.5;
		double r = colony.radius();
		boolean inside = CullingMath.inside(camera.x - cx, camera.y - cy, camera.z - cz, r);
		float base = inside ? 0.26f : 0.72f;
		int segments = low ? 40 : 80;
		double bottom = cy - 24;
		double top = cy + CullingMath.WALL;

		// The wall, in three bands so the streaks can run down it.
		double[] heights = {bottom, cy, cy + CullingMath.WALL / 2, top};
		for (int s = 0; s < segments; s++) {
			double a0 = 2 * Math.PI * s / segments;
			double a1 = 2 * Math.PI * (s + 1) / segments;
			for (int band = 0; band < heights.length - 1; band++) {
				Vec3 p0 = new Vec3(cx + Math.cos(a0) * r, heights[band], cz + Math.sin(a0) * r);
				Vec3 p1 = new Vec3(cx + Math.cos(a1) * r, heights[band], cz + Math.sin(a1) * r);
				Vec3 p2 = new Vec3(cx + Math.cos(a1) * r, heights[band + 1], cz + Math.sin(a1) * r);
				Vec3 p3 = new Vec3(cx + Math.cos(a0) * r, heights[band + 1], cz + Math.sin(a0) * r);
				quads.quad(p0, p1, p2, p3, shade(base, a0, heights[band], time), shade(base, a1, heights[band], time),
						shade(base, a1, heights[band + 1], time), shade(base, a0, heights[band + 1], time));
			}
		}

		// The dome over it.
		int rings = low ? 8 : 14;
		for (int ring = 0; ring < rings; ring++) {
			double lat0 = Math.PI / 2 * ring / rings;
			double lat1 = Math.PI / 2 * (ring + 1) / rings;
			for (int s = 0; s < segments; s++) {
				double a0 = 2 * Math.PI * s / segments;
				double a1 = 2 * Math.PI * (s + 1) / segments;
				Vec3 p0 = dome(cx, top, cz, r, lat0, a0);
				Vec3 p1 = dome(cx, top, cz, r, lat0, a1);
				Vec3 p2 = dome(cx, top, cz, r, lat1, a1);
				Vec3 p3 = dome(cx, top, cz, r, lat1, a0);
				quads.quad(p0, p1, p2, p3, shade(base, a0, p0.y, time), shade(base, a1, p1.y, time), shade(base, a1, p2.y, time),
						shade(base, a0, p3.y, time));
			}
		}
	}

	private static Vec3 dome(double cx, double base, double cz, double r, double lat, double lon) {
		double cos = Math.cos(lat);
		return new Vec3(cx + Math.cos(lon) * cos * r, base + Math.sin(lat) * r, cz + Math.sin(lon) * cos * r);
	}

	/** Ink with streaks: narrow lighter runs that slide slowly downward. */
	private static int shade(float base, double angle, double y, float time) {
		double streak = Math.pow(0.5 + 0.5 * Math.sin(angle * 37 + Math.sin(angle * 5) * 2), 8);
		double drip = 0.5 + 0.5 * Math.sin(y * 0.12 + time * 0.02 + angle * 11);
		float mix = (float) Mth.clamp(streak * drip, 0, 1);
		int rgb = ARGB.srgbLerp(mix, 0xFF000000 | INK, 0xFF000000 | STREAK);
		float alpha = base * (0.85f + 0.15f * mix);
		return ARGB.color(Mth.clamp((int) (alpha * 255), 0, 255), rgb & 0xFFFFFF);
	}
}
