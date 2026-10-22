package com.jjkcraft.client.vfx;

import java.util.ArrayList;
import java.util.List;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.Vec3;
import com.jjkcraft.JjkCraft;
import com.jjkcraft.client.config.ClientConfig;
import com.jjkcraft.network.payload.TechniqueFxPayload;

/** Animated, depth-tested material layers over the geometric technique silhouettes.
 * One atlas, one render submission, bounded sprites. The server never renders effects.
 */
public final class TexturedFx {
    private static final List<Layer> LAYERS = new ArrayList<>();
    private static net.minecraft.client.multiplayer.ClientLevel world;
    private static final net.minecraft.resources.Identifier ATLAS=JjkCraft.id("textures/vfx/technique_atlas.png");
    private record Layer(Vec3 at, Vec3 direction, int color, int row, float start,
                         float duration, double radius, boolean ground, int seed) { }
    private TexturedFx() { }

    public static void init() {
        LevelRenderEvents.COLLECT_SUBMITS.register(context -> {
            var mc=Minecraft.getInstance();
            if(mc.level==null||ClientConfig.effectsOff()){clear();return;}
            if(world!=mc.level){LAYERS.clear();world=mc.level;}
            float time=mc.level.getGameTime()+mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
            Vec3 camera=context.levelState().cameraRenderState.pos;
            LAYERS.removeIf(l->time-l.start>l.duration||time<l.start-2||camera.distanceToSqr(l.at)>128*128);
            if(LAYERS.isEmpty())return;
            List<Layer> snapshot=List.copyOf(LAYERS);
            int limit=Math.min(ClientConfig.lowVfx()?24:96,Math.max(8,ClientConfig.get().vfx.maxWorldQuads/8));
            context.submitNodeCollector().submitCustomGeometry(context.poseStack(),RenderTypes.entityTranslucentEmissive(ATLAS),(pose,buffer)->{
                int count=0;
                // New casts take priority over old residual motes when the scene is crowded.
                for(int i=snapshot.size()-1;i>=0&&count<limit;i--){
                    Layer l=snapshot.get(i);float t=Math.clamp((time-l.start)/l.duration,0,1);
                    float envelope=(float)(Math.min(1,t*12)*Math.pow(1-t,1.5));
                    if(envelope<=.005f)continue;
                    double r=l.radius*(ClientConfig.reducedMotion()?1:.55+1.15*(1-Math.pow(1-t,3)));
                    int frame=ClientConfig.reducedMotion()?0:Math.floorMod((int)((time-l.start)*.6)+l.seed,4);
                    Vec3 normal=l.ground?new Vec3(0,1,0):camera.subtract(l.at).normalize();
                    Vec3 right=l.ground?new Vec3(1,0,0):normal.cross(new Vec3(0,1,0)).normalize();
                    if(right.lengthSqr()<.01)right=new Vec3(1,0,0);
                    Vec3 up=l.ground?new Vec3(0,0,1):right.cross(normal).normalize();
                    double spin=ClientConfig.reducedMotion()?0:(l.row==1?-.15:.20)*t+(l.seed%7)*.3;
                    Vec3 u=right.scale(Math.cos(spin)).add(up.scale(Math.sin(spin))).scale(r);
                    Vec3 v=up.scale(Math.cos(spin)).subtract(right.scale(Math.sin(spin))).scale(l.row==2?r*.45:r);
                    Vec3 at=l.at;
                    int color=ARGB.color((int)(Math.min(ClientConfig.reduceFlashing()?.42:.72,envelope*.7)*255),l.color&0xffffff);
                    quad(pose,buffer,at,camera,u,v,color,frame,l.row);count++;
                    if(l.row==0&&!ClientConfig.lowVfx()&&count<limit){
                        quad(pose,buffer,at,camera,u.scale(.37),v.scale(.37),ARGB.color((int)(envelope*150),0xe9efff),frame,0);count++;
                    }
                }
            });
        });
    }

    public static void clear(){LAYERS.clear();world=null;}

    public static void burst(Vec3 at,int color,double radius,boolean ground){
        var mc=Minecraft.getInstance();if(mc.level==null)return;
        add(new Layer(at,Vec3.ZERO,color,ground?3:1,mc.level.getGameTime(),18,Math.clamp(radius,.25,16),ground,0));
    }

    public static void event(TechniqueFxPayload fx,int color,float now){
        if(ClientConfig.effectsOff())return;
        var cfg=ClientConfig.get().vfx;
        int option=switch(fx.phase()){
            case WINDUP,CHANNEL_START -> ClientConfig.effect(cfg.aura);
            case CAST,FIELD -> ClientConfig.effect(cfg.trails);
            default -> ClientConfig.effect(cfg.impacts);
        };
        if(option==0)return;
        var mc=Minecraft.getInstance();if(mc.level==null)return;
        var caster=mc.level.getEntity(fx.caster());
        Vec3 direction=EffectAim.forward(fx,caster);
        Vec3 at=fx.origin();String id=fx.technique().getPath();
        boolean shadow=id.contains("shadow")||id.contains("summon")||id.contains("divine_dog")||id.contains("chimera");
        int row=shadow?3:id.contains("slash")||id.contains("cleave")||id.contains("dismantle")?2:0;
        int salt=fx.technique().hashCode();
        double power=Math.clamp(Math.sqrt(Math.max(.1,fx.power())),.65,2.5);
        switch(fx.phase()){
            case WINDUP -> {
                if(caster!=null)at=caster.position().add(0,.04,0);
                add(new Layer(at,direction,color,1,now,16,.9,true,salt));
            }
            case CAST -> {
                if(caster!=null)at=caster.getEyePosition().add(caster.getLookAngle().scale(1.4));
                add(new Layer(at,direction,color,row,now,14,1.1*power,shadow,salt));
                if(!ClientConfig.lowVfx())add(new Layer(at,direction,color,1,now,10,1.5*power,false,salt));
            }
            case HIT -> {
                add(new Layer(at,direction,color,2,now,9,1.25*power,false,salt));
                add(new Layer(at,direction,color,1,now,16,1.8*power,false,salt));
            }
            case BURST -> {
                double r=Math.clamp(Math.abs(fx.direction().x),1,8)*power;
                add(new Layer(at.add(0,.06,0),direction,color,shadow?3:1,now,22,r,shadow,salt));
                add(new Layer(at,direction,color,0,now,12,Math.min(r,3),false,salt));
            }
            case FIELD -> {
                add(new Layer(at.add(0,.06,0),direction,color,1,now,28,Math.clamp(fx.direction().x,1,12),true,salt));
            }
            case CHANNEL_START -> {if(caster!=null)add(new Layer(caster.position().add(0,1,0),direction,color,0,now,22,1.1,false,salt));}
            default -> { }
        }
    }

    private static void add(Layer layer){
        var level=Minecraft.getInstance().level;if(world!=level){LAYERS.clear();world=level;}
        if(level!=null&&!ClientConfig.effectsOff()&&LAYERS.size()<192)LAYERS.add(layer);
    }
    private static void quad(PoseStack.Pose pose,VertexConsumer b,Vec3 at,Vec3 camera,Vec3 u,Vec3 v,int color,int frame,int row){
        float x=frame*.25f,y=row*.25f,e=.0005f;
        Vec3 a=at.subtract(camera);
        vertex(pose,b,a.subtract(u).subtract(v),color,x+e,y+.25f-e);
        vertex(pose,b,a.add(u).subtract(v),color,x+.25f-e,y+.25f-e);
        vertex(pose,b,a.add(u).add(v),color,x+.25f-e,y+e);
        vertex(pose,b,a.subtract(u).add(v),color,x+e,y+e);
    }
    private static void vertex(PoseStack.Pose p,VertexConsumer b,Vec3 v,int color,float u,float w){
        b.addVertex(p,(float)v.x,(float)v.y,(float)v.z).setColor(color).setUv(u,w)
            .setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xf000f0).setNormal(p,0,1,0);
    }
}
