package com.jjkcraft.client.render;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.Vec3;

import com.jjkcraft.client.config.ClientConfig;
import com.jjkcraft.entity.CursedBoltEntity;

/**
 * Draws a cursed bolt as a camera-facing glowing core with a tapering ribbon trail built from its
 * recent positions. Geometry only; no particles are spawned for the trail.
 */
public final class CursedBoltRenderer extends EntityRenderer<CursedBoltEntity, CursedBoltRenderer.State> {
	private static final int TRAIL = 10;
	private static final Map<Integer, Deque<Vec3>> HISTORY = new HashMap<>();

	public static final class State extends EntityRenderState {
		int color;
		float size;
        float age;
		List<Vec3> trail = List.of();
	}

	public CursedBoltRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(CursedBoltEntity entity, State state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.color = entity.color();
		state.size = entity.size();
        state.age=entity.tickCount+partialTicks;
		Vec3 now = new Vec3(state.x, state.y, state.z);
		if (HISTORY.size() > 256) {
			// Bolts removed without a final frame leave entries behind; drop them all now and then.
			HISTORY.clear();
		}

		Deque<Vec3> history = HISTORY.computeIfAbsent(entity.getId(), id -> new ArrayDeque<>());
		if (history.isEmpty() || history.peekFirst().distanceToSqr(now) > 0.01) {
			history.addFirst(now);
			int max = ClientConfig.lowVfx() ? TRAIL / 2 : TRAIL;
			while (history.size() > max) {
				history.removeLast();
			}
		}

		if (entity.isRemoved()) {
			HISTORY.remove(entity.getId());
		}

		List<Vec3> relative = new ArrayList<>(history.size());
		for (Vec3 p : history) {
			relative.add(p.subtract(now));
		}

		state.trail = relative;
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		Quaternionf facing = camera.orientation;
		float size = state.size;
		int core = ARGB.color(230, state.color & 0xFFFFFF);
		int halo = ARGB.color(90, state.color & 0xFFFFFF);
		List<Vec3> trail = state.trail;
		Vec3 cameraFromEntity = camera.pos.subtract(state.x, state.y, state.z);

		collector.submitCustomGeometry(poseStack, RenderTypes.lightning(), (pose, buffer) -> {
			glow(pose, buffer, facing, size * 2.2f, halo);
			glow(pose, buffer, facing, size * 1.1f, core);
            glow(pose, buffer, facing, size * 0.22f, 0xD0FFFFFF);
            int segments=ClientConfig.lowVfx()||cameraFromEntity.lengthSqr()>32*32?8:16;
            for(int plane=0;plane<2;plane++)for(int j=0;j<segments;j++){
                double a=j*Math.PI*2/segments+state.age*.16,b=(j+.7)*Math.PI*2/segments+state.age*.16;
                Vec3 p=plane==0?new Vec3(Math.cos(a)*size*1.35,Math.sin(a)*size*.5,Math.sin(a)*size):new Vec3(Math.cos(a)*size,Math.sin(a)*size*1.35,Math.cos(a)*size*.45);
                Vec3 q=plane==0?new Vec3(Math.cos(b)*size*1.35,Math.sin(b)*size*.5,Math.sin(b)*size):new Vec3(Math.cos(b)*size,Math.sin(b)*size*1.35,Math.cos(b)*size*.45);
                Vec3 side=q.subtract(p).cross(cameraFromEntity.subtract(p)).normalize().scale(size*.035);
                vertex(pose,buffer,p.add(side),core);vertex(pose,buffer,p.subtract(side),core);vertex(pose,buffer,q.subtract(side),halo);vertex(pose,buffer,q.add(side),halo);
            }

			for (int i = 0; i + 1 < trail.size(); i++) {
				Vec3 a = trail.get(i);
				Vec3 b = trail.get(i + 1);
				float t0 = 1 - (float) i / trail.size();
				float t1 = 1 - (float) (i + 1) / trail.size();
				Vec3 axis = b.subtract(a);
				Vec3 side = axis.cross(cameraFromEntity.subtract(a));
				if (side.lengthSqr() < 1e-8) {
					continue;
				}

				side = side.normalize();
				Vec3 s0 = side.scale(size * 0.8 * t0);
				Vec3 s1 = side.scale(size * 0.8 * t1);
				int c0 = ARGB.color((int) (170 * t0), state.color & 0xFFFFFF);
				int c1 = ARGB.color((int) (170 * t1), state.color & 0xFFFFFF);
				vertex(pose, buffer, a.add(s0), c0);
				vertex(pose, buffer, a.subtract(s0), c0);
				vertex(pose, buffer, b.subtract(s1), c1);
				vertex(pose, buffer, b.add(s1), c1);
				vertex(pose, buffer, b.add(s1), c1);
				vertex(pose, buffer, b.subtract(s1), c1);
				vertex(pose, buffer, a.subtract(s0), c0);
				vertex(pose, buffer, a.add(s0), c0);
			}
		});
		super.submit(state, poseStack, collector, camera);
	}

	/** A round, camera-facing glow: full colour at the centre, fading to nothing at the rim. */
	private static void glow(PoseStack.Pose pose, VertexConsumer buffer, Quaternionf facing, float radius, int color) {
		int segments = 12;
		int rim = color & 0xFFFFFF;
		for (int i = 0; i < segments; i++) {
			double a0 = 2 * Math.PI * i / segments;
			double a1 = 2 * Math.PI * (i + 1) / segments;
			Vector3f p0 = facing.transform(new Vector3f((float) Math.cos(a0) * radius, (float) Math.sin(a0) * radius, 0));
			Vector3f p1 = facing.transform(new Vector3f((float) Math.cos(a1) * radius, (float) Math.sin(a1) * radius, 0));
			// One wedge as a quad with a doubled centre vertex, both windings.
			buffer.addVertex(pose, 0, 0, 0).setColor(color);
			buffer.addVertex(pose, p0.x, p0.y, p0.z).setColor(rim);
			buffer.addVertex(pose, p1.x, p1.y, p1.z).setColor(rim);
			buffer.addVertex(pose, 0, 0, 0).setColor(color);
			buffer.addVertex(pose, 0, 0, 0).setColor(color);
			buffer.addVertex(pose, p1.x, p1.y, p1.z).setColor(rim);
			buffer.addVertex(pose, p0.x, p0.y, p0.z).setColor(rim);
			buffer.addVertex(pose, 0, 0, 0).setColor(color);
		}
	}

	private static void vertex(PoseStack.Pose pose, VertexConsumer buffer, Vec3 p, int color) {
		buffer.addVertex(pose, (float) p.x, (float) p.y, (float) p.z).setColor(color);
	}
}
