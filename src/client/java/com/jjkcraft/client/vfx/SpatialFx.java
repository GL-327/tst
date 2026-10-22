package com.jjkcraft.client.vfx;
import java.util.*;
import net.minecraft.world.phys.Vec3;
import com.jjkcraft.network.payload.TechniqueFxPayload;

/** Restrained spatial envelopes with stable identities and a visible influence radius. */
final class SpatialFx {
    private static final class Field {TechniqueFxPayload event;float expires;Field(TechniqueFxPayload e,float now){event=e;expires=now+(float)e.direction().y;}}
    private static final Map<String,Field> FIELDS=new HashMap<>();
    static void clear(){FIELDS.clear();}
    static void event(TechniqueFxPayload e,int color,float now){
        if(e.phase()==TechniqueFxPayload.Phase.FIELD){
            String key=e.caster()+":"+e.technique()+":"+e.origin();var f=FIELDS.get(key);
            if(f!=null){f.expires=now+(float)e.direction().y;f.event=e;return;}
            if(FIELDS.size()>=128)return;
            var state=new Field(e,now);FIELDS.put(key,state);
            boolean red=e.technique().getPath().contains("red");
            WorldFx.addSolid(frame->{if(FIELDS.get(key)!=state)return false;if(frame.time()>state.expires){FIELDS.remove(key);return false;}ChargeFx.core(frame,e.origin(),Math.min(5,Math.max(.22,e.direction().x*.28)),red?0xFFC44859:0xFF174B87);return true;});
            WorldFx.add(frame->{if(FIELDS.get(key)!=state||frame.time()>state.expires)return false;double r=e.direction().x;
                ChargeFx.halo(frame,e.origin(),Math.min(10,r*.85),color,frame.time()*.025,.65f);
                // A faint ground footprint describes the real radius without filling the view.
                int n=frame.segments(36);for(int i=0;i<n;i++){double a=i*Math.PI*2/n,b=(i+.65)*Math.PI*2/n;frame.beam(e.origin().add(Math.cos(a)*r,-.55,Math.sin(a)*r),e.origin().add(Math.cos(b)*r,-.55,Math.sin(b)*r),.025,WorldFx.fade(color,.16f),0);}
                for(int i=0;i<5;i++){double a=i*Math.PI*.4+frame.time()*.02;Vec3 from=e.origin().add(Math.cos(a)*r*.8,Math.sin(a*2)*r*.15,Math.sin(a)*r*.8),to=e.origin().add(Math.cos(a+.4)*r*.25,0,Math.sin(a+.4)*r*.25);frame.beam(e.direction().z>0?from:to,e.direction().z>0?to:from,.028,WorldFx.fade(color,.08f),WorldFx.fade(color,.5f));}
                return true;
            });return;
        }
        if(e.phase()!=TechniqueFxPayload.Phase.HIT&&e.phase()!=TechniqueFxPayload.Phase.BURST)return;
        boolean burst=e.phase()==TechniqueFxPayload.Phase.BURST;float life=burst?14:7;double radius=burst?Math.max(.8,e.direction().x):.5;
        WorldFx.add(f->{float t=(f.time()-now)/life;if(t>1)return false;ChargeFx.halo(f,e.origin(),radius*(.35+t),color,t*.4,(1-t)*.7f);return true;});
    }
}
