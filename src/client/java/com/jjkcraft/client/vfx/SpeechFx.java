package com.jjkcraft.client.vfx;

import java.util.*;
import com.jjkcraft.client.config.ClientConfig;
import com.jjkcraft.network.payload.TechniqueFxPayload;
import com.jjkcraft.technique.TechniqueDefinition;
import com.jjkcraft.technique.behavior.CursedSpeechBehavior;
import com.jjkcraft.vfx.EffectOrientation;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

/** Mouth-origin pressure shells with depth, not a world-axis ring or camera-facing sticker. */
public final class SpeechFx {
    private static final Map<Integer,Vec3> AIM=new HashMap<>();
    private SpeechFx(){}
    public static void clear(){AIM.clear();}
    public static Optional<Vec3> lastAim(int caster){return Optional.ofNullable(AIM.get(caster));}
    public static boolean event(TechniqueFxPayload e,TechniqueDefinition def,int color,float now){
        if(!(def.behavior() instanceof CursedSpeechBehavior speech)||e.phase()!=TechniqueFxPayload.Phase.CAST)return false;
        if(ClientConfig.effectsOff())return true;var mc=Minecraft.getInstance();if(mc.level==null)return true;
        var basis=EffectOrientation.of(EffectAim.forward(e,mc.level.getEntity(e.caster())));if(AIM.size()>128)AIM.clear();AIM.put(e.caster(),basis.forward());
        double reach=Math.clamp(Double.isFinite(e.direction().x)&&e.direction().x>0?e.direction().x:speech.radius(),1,24);Vec3 mouth=e.origin().add(basis.forward().scale(.35));
        WorldFx.addMaterial(f->{float age=f.time()-now;if(age<0)return true;if(age>24)return false;if(f.camera.distanceToSqr(mouth)>96*96)return true;
            int fronts=ClientConfig.lowVfx()?2:3;
            for(int i=0;i<fronts;i++){
                float t=(age-i*3)/20;if(t<0||t>1)continue;double distance=.25+reach*.9*t,radius=.18+distance*.32;
                Vec3 center=mouth.add(basis.forward().scale(distance));if(center.distanceToSqr(f.camera)<.7*.7)continue;
                float alpha=(float)(Math.sin(Math.min(1,t*4)*Math.PI/2)*Math.pow(1-t,1.4))*(ClientConfig.reduceFlashing()?.32f:.58f);
                VolumeMesh.pressure(f,center,basis,radius,.045+radius*.045,6,color,alpha,0);
                if(!ClientConfig.lowVfx())for(int n=0;n<8;n++){
                    double a=n*Math.PI/4+t*.4;Vec3 p=basis.at(center,Math.cos(a)*radius,Math.sin(a)*radius,0),q=p.subtract(basis.forward().scale(.2+distance*.12));
                    f.surface(p.add(basis.up().scale(.025)),p.subtract(basis.up().scale(.025)),q.subtract(basis.up().scale(.015)),q.add(basis.up().scale(.015)),6,color,alpha*.28f,0,0,1,1);
                }
            }
            return true;
        });
        for(int id:e.targets().stream().limit(8).toList()){
            var target=mc.level.getEntity(id);if(target==null)continue;Vec3 at=target.getBoundingBox().getCenter();
            WorldFx.addMaterial(f->{float t=(f.time()-now)/14;if(t>1)return false;double r=target.getBbWidth()*.65;
                VolumeMesh.pressure(f,at,basis,r,.025,6,color,(1-t)*.35f,0);return true;});
        }
        return true;
    }
}
