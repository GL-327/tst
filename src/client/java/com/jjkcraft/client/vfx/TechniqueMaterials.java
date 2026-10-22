package com.jjkcraft.client.vfx;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import com.jjkcraft.awakening.CharacterAwakening;
import com.jjkcraft.client.config.ClientConfig;
import com.jjkcraft.client.state.ClientTechniques;
import com.jjkcraft.network.payload.TechniqueFxPayload;

/** Every synced technique receives a material, including datapacks and utility techniques. */
public final class TechniqueMaterials {
    private static final Map<Identifier,Integer> TILES=new HashMap<>();
    private static final Map<String,Float> RECENT=new HashMap<>();
    private static final Map<String,Field> FIELDS=new HashMap<>();
    private static net.minecraft.client.multiplayer.ClientLevel world;
    private static final class Field {float expires;Field(float e){expires=e;}}
    private TechniqueMaterials(){}
    public static void clear(){TILES.clear();RECENT.clear();FIELDS.clear();world=null;}
    public static int tile(Identifier id){
        var mc=Minecraft.getInstance();if(world!=mc.level){clear();world=mc.level;}
        if(mc.level==null)return 3;
        if(TILES.isEmpty()){
            var families=mc.level.registryAccess().lookupOrThrow(CharacterAwakening.FAMILIES);
            for(var family:families.keySet())for(var move:families.getValue(family).techniques())TILES.put(move,familyTile(family.getPath()));
        }
        String name=id.getPath();
        if(name.contains("heal")||name.contains("reverse_cursed")||name.contains("positive"))return 14;
        return TILES.getOrDefault(id,name.contains("slash")?7:name.contains("bolt")?12:3);
    }
    private static int familyTile(String family){return switch(family){
        case "blood_manipulation","explosive_flesh"->9;
        case "disaster_flames"->8;
        case "ice_formation","sky_manipulation","glass","porcelain","salt","kintsugi","mercury_mirror"->10;
        case "ten_shadows","moon_dregs","moonlit_menagerie","curse_manipulation","ink","brain_transplant"->11;
        case "shrine","ratio","straw_doll","mourning_thread","suture","thorn"->7;
        case "electrification","granite_discharge","lodestone","modulo_light_darkness"->12;
        case "projection","origami","stencil","carrion","echo_memory","echo_ledger"->13;
        case "cursed_speech","tollbell","pressure","momentum","star_rage","boogie_woogie","orbit"->6;
        case "idle_transfiguration","love_rendezvous","modulo_harmony_chaos","construction","tool_manipulation","disaster_plants","disaster_tides","resin","rust","wax","sand"->4;
        default->3;
    };}
    public static void event(TechniqueFxPayload e,int color,float now){
        if(ClientConfig.effectsOff()||SpectralFx.spatial(e.technique().getPath()))return;
        int tile=tile(e.technique());
        var phase=e.phase();boolean field=phase==TechniqueFxPayload.Phase.FIELD,wind=phase==TechniqueFxPayload.Phase.WINDUP;
        if(!field&&!wind&&phase!=TechniqueFxPayload.Phase.CAST&&phase!=TechniqueFxPayload.Phase.HIT&&phase!=TechniqueFxPayload.Phase.BURST)return;
        String key=e.caster()+":"+e.technique()+":"+phase+(field?e.origin().toString():"");
        if(field){var old=FIELDS.get(key);if(old!=null){old.expires=now+(float)Math.clamp(e.direction().y,8,600);return;}}
        else {if(now-RECENT.getOrDefault(key,-999f)<4)return;RECENT.put(key,now);RECENT.values().removeIf(t->now-t>80);}
        var mc=Minecraft.getInstance();if(mc.level==null)return;
        float duration=field?(float)Math.clamp(e.direction().y,8,600):wind?12:phase==TechniqueFxPayload.Phase.BURST?20:phase==TechniqueFxPayload.Phase.HIT?9:12;
        Field state=new Field(now+duration);if(field){if(FIELDS.size()>=128)return;FIELDS.put(key,state);}
        Vec3 origin=e.origin();
        var caster=mc.level.getEntity(e.caster());
        if(phase==TechniqueFxPayload.Phase.CAST&&caster!=null&&origin.distanceToSqr(caster.position())<1)origin=caster.getEyePosition().add(caster.getLookAngle().scale(1.8));
        final Vec3 base=origin;
        double size=field?Math.clamp(Math.abs(e.direction().x)*.3,.5,4):phase==TechniqueFxPayload.Phase.BURST?Math.clamp(Math.abs(e.direction().x)*.4,.7,5):wind?.35:phase==TechniqueFxPayload.Phase.HIT?.65:1.0;
        WorldFx.addMaterial(f->{
            if(f.time()>state.expires){if(field)FIELDS.remove(key,state);return false;}
            if(f.camera.distanceToSqr(base)>96*96)return true;
            float t=Math.clamp((f.time()-now)/duration,0,1),alpha=field?Math.min(1,(state.expires-f.time())/8):(float)Math.pow(1-t,1.3);
            Vec3 at=base;
            if(wind){var entity=mc.level.getEntity(e.caster());if(entity==null)return false;at=entity==mc.player&&mc.options.getCameraType().isFirstPerson()?f.aimPoint(1.25):ChargeFx.anchor(entity,mc.getDeltaTracker().getGameTimeDeltaPartialTick(false),f.camera);}
            double r=size*(wind?1:field?1:.55+.7*Math.sqrt(t));
            double spin=ClientConfig.reducedMotion()?0:(tile==7?-.4:tile==11?.04:f.time()*.017);
            f.sprite(at,r,r*(tile==7?.6:1),tile,color,alpha*(tile==11?.85f:.8f),spin);
            if(tile!=11&&tile!=7&&tile!=13)f.sprite(at,r*1.25,r*1.25,4,color,alpha*.22f,-spin);
            if(!wind&&!ClientConfig.lowVfx())f.sprite(at,r*1.35,r*1.35,15,color,alpha*.65f,spin*.5);
            return true;
        });
    }
}
