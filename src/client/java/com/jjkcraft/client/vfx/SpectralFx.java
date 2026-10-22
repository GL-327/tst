package com.jjkcraft.client.vfx;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import com.jjkcraft.client.config.ClientConfig;
import com.jjkcraft.network.payload.TechniqueFxPayload;
import com.jjkcraft.network.payload.CombatPosePayload;

/** Material-specific, layered volume effects. Geometry budgets and accessibility apply to every layer. */
public final class SpectralFx {
    private static final Map<Integer,Long> MELEE=new HashMap<>();
    private SpectralFx() { }
    public static void clear(){MELEE.clear();MaterialFx.clear();SpatialFx.clear();}
    public static boolean spatial(String name){return name.contains("blue")||name.contains("purple")||name.contains("reversal_red")||name.equals("mini_red")||name.equals("red_punch")||name.equals("maximum_output_red")||name.equals("infinity");}
    public static boolean event(TechniqueFxPayload fx,int color,float now){
        if(ClientConfig.effectsOff())return false;
        if(spatial(fx.technique().getPath())){SpatialFx.event(fx,color,now);return true;}
        if(MaterialFx.event(fx,color,now))return true;
        // Every remaining family has a material envelope; projectile entities draw their own trail.
        return switch(fx.phase()) {case WINDUP,CAST,FIELD,HIT,BURST->true;default->false;};
    }
    public static void melee(CombatPosePayload pose){
        if(ClientConfig.effectsOff()||!Set.of("jab","hook","backfist","heavy","uppercut","sweep","feint").contains(pose.move())||MELEE.getOrDefault(pose.entity(),-1L)==pose.started())return;
        MELEE.put(pose.entity(),pose.started());if(MELEE.size()>256)MELEE.clear();
        WorldFx.add(f->{var mc=Minecraft.getInstance();var entity=mc.level==null?null:mc.level.getEntity(pose.entity());if(entity==null)return false;
            // Seen from inside, the arc surrounds the camera as floating panels; the first-person fists show the swing.
            if(entity==mc.player&&mc.options.getCameraType().isFirstPerson())return true;
            float age=f.time()-pose.started();if(age>pose.startup()+pose.active()+4)return false;if(age<pose.startup()-1)return true;
            double yaw=Math.toRadians(entity.getYRot()),sweep=(age-pose.startup()+1)/Math.max(1,pose.active());
            Vec3 center=entity.position().add(0,pose.move().equals("sweep")?.4:entity.getBbHeight()*.7,0),right=new Vec3(Math.cos(yaw),0,Math.sin(yaw)),forward=new Vec3(-Math.sin(yaw),0,Math.cos(yaw));
            int color=pose.move().equals("feint")?0x526071:pose.move().equals("heavy")?0xDD8499:0x8ED3EC;
            for(int i=0;i<8;i++){double a=(i/8.0-.5)*2+sweep*.8;Vec3 p=center.add(forward.scale(Math.cos(a)*1.4)).add(right.scale(Math.sin(a)*1.4));Vec3 q=center.add(forward.scale(Math.cos(a+.12)*1.4)).add(right.scale(Math.sin(a+.12)*1.4));f.beam(p,q,.06,WorldFx.fade(color,.45f),WorldFx.fade(color,.1f));}return true;
        });
    }
}
