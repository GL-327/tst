package com.jjkcraft.client.vfx;
import java.util.*;
import net.minecraft.world.phys.Vec3;
import com.jjkcraft.network.payload.TechniqueFxPayload;
import com.jjkcraft.client.config.ClientConfig;

/** Stable spatial volumes: inward accretion for Blue, outward pressure for Red. */
final class SpatialFx {
    private static final class Field {TechniqueFxPayload event;float expires,born;Field(TechniqueFxPayload e,float now){event=e;born=now;expires=now+(float)e.direction().y;}}
    private static final Map<String,Field> FIELDS=new HashMap<>();
    static void clear(){FIELDS.clear();}
    static void event(TechniqueFxPayload e,int color,float now){
        if(e.phase()==TechniqueFxPayload.Phase.FIELD){
            String key=e.caster()+":"+e.technique()+":"+e.origin();var old=FIELDS.get(key);
            if(old!=null){old.expires=now+(float)e.direction().y;old.event=e;return;}
            if(FIELDS.size()>=128)return;
            var state=new Field(e,now);FIELDS.put(key,state);int tile=EnergySprites.spatialTile(e.technique().getPath());
            WorldFx.addMaterial(f->{
                if(FIELDS.get(key)!=state)return false;
                if(f.time()>state.expires){FIELDS.remove(key);return false;}
                double radius=state.event.direction().x,r=Math.clamp(radius*.38,.28,6);
                float fade=Math.min(1,(state.expires-f.time())/8)*Math.min(1,(f.time()-state.born+1)/4);
                EnergySprites.orb(f,e.origin(),r,tile,color,.98f*fade);
                double age=f.time()-state.born;
                f.sprite(e.origin(),radius*1.05,radius*1.05,4,color,.18f*fade,ClientConfig.reducedMotion()?0:f.time()*(tile==1?-.009:.009));
                if(age<18){float t=(float)Math.clamp(age/18,0,1);double shock=radius*1.5*(.12+.88*Math.sqrt(t));f.sprite(e.origin(),shock,shock,6,color,(1-t)*.55f,0);}
                if(!ClientConfig.reducedMotion())for(int i=0;i<f.segments(12);i++){
                    double cycle=(f.time()*.022+i*.618)%1,u=tile==1?cycle:1-cycle;
                    double a=i*2.399+cycle*(tile==1?-.8:1.8),distance=radius*(.12+.78*u);
                    Vec3 p=e.origin().add(Math.cos(a)*distance,Math.sin(a*2+cycle)*radius*.23,Math.sin(a)*distance);
                    double size=.12+radius*.035;f.sprite(p,size,size*.45,4,color,(float)(Math.sin(cycle*Math.PI)*.62*fade),a);
                }
                return true;
            });return;
        }
        if(e.phase()!=TechniqueFxPayload.Phase.HIT&&e.phase()!=TechniqueFxPayload.Phase.BURST)return;
        boolean burst=e.phase()==TechniqueFxPayload.Phase.BURST;float life=burst?18:9;double radius=burst?Math.max(.8,e.direction().x):.6;
        WorldFx.addMaterial(f->{float t=(f.time()-now)/life;if(t>1)return false;
            double r=radius*(.25+.85*Math.sqrt(Math.max(0,t)));
            f.sprite(e.origin(),r,r,6,color,(1-t)*.68f,0);
            if(t<.45)EnergySprites.orb(f,e.origin(),Math.min(radius*.25,2.6),EnergySprites.spatialTile(e.technique().getPath()),color,(1-t/.45f)*.8f);
            return true;
        });
    }
}
