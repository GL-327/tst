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
    private static final Map<Integer,Integer> SAMPLED_TICK=new HashMap<>();
    public static void clearHistory(){HISTORY.clear();SAMPLED_TICK.clear();}

	public static final class State extends EntityRenderState {
		int color;
        int tile;boolean spatial;String technique="";
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
        var technique=entity.technique();
        state.spatial=technique!=null&&com.jjkcraft.client.vfx.SpectralFx.spatial(technique.getPath());state.technique=technique==null?"":technique.getPath();
        state.tile=technique==null?3:state.spatial?com.jjkcraft.client.vfx.EnergySprites.spatialTile(technique.getPath()):com.jjkcraft.client.vfx.TechniqueMaterials.tile(technique);
        state.age=entity.tickCount+partialTicks;
		Vec3 now = new Vec3(state.x, state.y, state.z);
		if (HISTORY.size() > 256) {
			// Bolts removed without a final frame leave entries behind; drop them all now and then.
			clearHistory();
		}

		Deque<Vec3> history = HISTORY.computeIfAbsent(entity.getId(), id -> new ArrayDeque<>());
		if (history.isEmpty() || SAMPLED_TICK.getOrDefault(entity.getId(),-1)!=entity.tickCount) {
            SAMPLED_TICK.put(entity.getId(),entity.tickCount);
			history.addFirst(entity.getPosition(0));
			int max = ClientConfig.lowVfx() ? TRAIL / 2 : TRAIL;
			while (history.size() > max) {
				history.removeLast();
			}
		}

		if (entity.isRemoved()) {
			HISTORY.remove(entity.getId());
            SAMPLED_TICK.remove(entity.getId());
		}

		List<Vec3> relative = new ArrayList<>(history.size()+1);relative.add(Vec3.ZERO);
		for (Vec3 p : history) {
			relative.add(p.subtract(now));
		}

		state.trail = relative;
	}

	@Override
    public void submit(State state,PoseStack stack,SubmitNodeCollector collector,CameraRenderState camera){
        if(ClientConfig.effectsOff())return;
        if(state.spatial){
            // Limitless projectiles are light, not solid volumes: a mesh around the camera flooded the view.
            Vec3 at=new Vec3(state.x,state.y,state.z);var trail=state.trail;int kind=com.jjkcraft.client.vfx.GlowFx.limitless(state.technique);
            stack.pushPose();stack.translate(camera.pos.x-state.x,camera.pos.y-state.y,camera.pos.z-state.z);
            com.jjkcraft.client.vfx.WorldFx.submitGlow(collector,stack,camera,f->{
                double r=Math.max(.3,state.size*.55);
                int rgb=state.color&0xFFFFFF;
                com.jjkcraft.client.vfx.GlowFx.limitless(f,kind,at,r,1,rgb);
                if(trail.size()>1)com.jjkcraft.client.vfx.GlowFx.tail(f,at,at.add(trail.getLast()),r,rgb,.8f);
                return true;
            });
            stack.popPose();super.submit(state,stack,collector,camera);return;
        }
        Vec3 right=new Vec3(camera.orientation.transform(new Vector3f(1,0,0))),up=new Vec3(camera.orientation.transform(new Vector3f(0,1,0)));
        double size=state.size;
        float near=(float)Math.clamp(camera.pos.distanceTo(new Vec3(state.x,state.y,state.z))/Math.max(1,size*1.5),.18,1);
        collector.submitCustomGeometry(stack,RenderTypes.waterMask(),(pose,buffer)->com.jjkcraft.client.vfx.VolumeMesh.projectile(pose,buffer,size,state.tile,state.color&0xffffff,near*.94f,state.age,ClientConfig.lowVfx()||camera.pos.distanceToSqr(new Vec3(state.x,state.y,state.z))>24*24,true));
        collector.submitCustomGeometry(stack,RenderTypes.entityTranslucentEmissive(com.jjkcraft.client.vfx.EnergySprites.ATLAS),(pose,buffer)->{
            double spin=ClientConfig.reducedMotion()?0:state.age*.035;
            Vec3 u=right.scale(Math.cos(spin)).add(up.scale(Math.sin(spin))),v=up.scale(Math.cos(spin)).subtract(right.scale(Math.sin(spin)));
            com.jjkcraft.client.vfx.VolumeMesh.projectile(pose,buffer,size,state.tile,state.color&0xffffff,near*.94f,state.age,ClientConfig.lowVfx()||camera.pos.distanceToSqr(new Vec3(state.x,state.y,state.z))>24*24);
            com.jjkcraft.client.vfx.EnergySprites.quad(pose,buffer,Vec3.ZERO,u.scale(size*1.5),v.scale(size*1.5),4,ARGB.color((int)(near*45),state.color&0xffffff));
            for(int i=1;i<state.trail.size();i++){
                float t=1-i/(float)state.trail.size();double r=size*(.35+.45*t);
                com.jjkcraft.client.vfx.EnergySprites.quad(pose,buffer,state.trail.get(i),right.scale(r),up.scale(r),4,ARGB.color((int)(near*t*100),state.color&0xffffff));
            }
        });
        super.submit(state,stack,collector,camera);
    }
}
