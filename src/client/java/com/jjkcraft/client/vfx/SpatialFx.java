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
            var state=new Field(e,now);FIELDS.put(key,state);int kind=GlowFx.limitless(e.technique().getPath());
            WorldFx.addGlow(f->{
                if(FIELDS.get(key)!=state)return false;
                if(f.time()>state.expires){FIELDS.remove(key);return false;}
                double radius=state.event.direction().x,r=Math.clamp(radius*.3,.25,5);
                float fade=Math.min(1,(state.expires-f.time())/8)*Math.min(1,(f.time()-state.born+1)/4);
                GlowFx.limitless(f,kind,e.origin(),r,fade,color&0xFFFFFF);
                double age=f.time()-state.born;
                if(age<18)GlowFx.flash(f,e.origin(),radius*1.4,color&0xFFFFFF,(float)Math.clamp(age/18,0,1),.8f);
                return true;
            });return;
        }
        if(e.phase()!=TechniqueFxPayload.Phase.HIT&&e.phase()!=TechniqueFxPayload.Phase.BURST)return;
        boolean burst=e.phase()==TechniqueFxPayload.Phase.BURST;float life=burst?18:9;double radius=burst?Math.max(.8,e.direction().x):.6;
        WorldFx.addGlow(f->{float t=(f.time()-now)/life;if(t>1)return false;if(t<0)return true;
            GlowFx.flash(f,e.origin(),radius*(burst?1.3:1.6),color&0xFFFFFF,t,1);
            return true;
        });
    }
}
