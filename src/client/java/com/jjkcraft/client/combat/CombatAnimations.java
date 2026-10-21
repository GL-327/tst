package com.jjkcraft.client.combat;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.model.geom.ModelPart;
import net.fabricmc.fabric.api.client.networking.v1.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.jjkcraft.network.payload.CombatPosePayload;

/** Authored windup, contact and recovery poses follow the server's combat windows. */
public final class CombatAnimations {
    private record Received(CombatPosePayload pose,long received) { }
    private static final Map<Integer,Received> POSES=new HashMap<>();
    private static final Map<Integer,Received> CASTS=new HashMap<>();
    private static final Map<net.minecraft.client.renderer.entity.state.EntityRenderState,Integer> ENTITY_IDS=new java.util.WeakHashMap<>();
    public static void identify(net.minecraft.client.renderer.entity.state.EntityRenderState state,int id){ENTITY_IDS.put(state,id);}
    private CombatAnimations() { }
    public static void init(){
        ClientPlayNetworking.registerGlobalReceiver(CombatPosePayload.TYPE,(p,ctx)->{
            var level=ctx.client().level;if(level!=null){(p.move().startsWith("cast_")?CASTS:POSES).put(p.entity(),new Received(p,level.getGameTime()));com.jjkcraft.client.vfx.SpectralFx.melee(p);}
        });
        ClientPlayConnectionEvents.DISCONNECT.register((h,m)->{POSES.clear();CASTS.clear();});
    }
    public static CombatPosePayload pose(int id){
        var r=POSES.get(id);var level=Minecraft.getInstance().level;
        if(level==null)return null;
        if(r!=null&&Math.abs(level.getGameTime()-r.received())>40){POSES.remove(id);r=null;}
        if(r!=null&&(!r.pose().move().isEmpty()&&!r.pose().move().equals("stance")||r.pose().guard()>=0||r.pose().stun()>0))return r.pose();
        var cast=CASTS.get(id);if(cast!=null&&level.getGameTime()-cast.received()<cast.pose().startup()+11)return cast.pose();
        CASTS.remove(id);return r==null?null:r.pose();
    }
    private static float smooth(float n){n=Math.clamp(n,0,1);return n*n*(3-2*n);}
    public static float phase(CombatPosePayload p,float partial){
        var level=Minecraft.getInstance().level;float age=level==null?0:level.getGameTime()-p.started()+partial;
        if(age<0||age>p.startup()+p.active()+p.recovery())return 0;
        if(age<p.startup())return -smooth(age/Math.max(1,p.startup()));
        if(age<p.startup()+p.active())return 1;
        return 1-smooth((age-p.startup()-p.active())/Math.max(1,p.recovery()));
    }
    private static void rot(ModelPart p,float x,float y,float z){p.xRot=x;p.yRot=y;p.zRot=z;}
    public static void apply(HumanoidModel<?> m,net.minecraft.client.renderer.entity.state.HumanoidRenderState state){
        int id=state instanceof AvatarRenderState avatar?avatar.id:ENTITY_IDS.getOrDefault(state,-1);
        var p=pose(id);if(p==null||state instanceof AvatarRenderState avatar&&avatar.isSpectator||state.isFallFlying||state.isVisuallySwimming)return;
        float partial=state.ageInTicks-(float)Math.floor(state.ageInTicks);
        float k=phase(p,partial);boolean moving=!p.move().isEmpty()&&Math.abs(k)>0.001f;
        boolean stance=p.move().equals("stance");
        if(!moving&&!stance&&p.guard()<0&&p.stun()<=0)return;
        float breath=(float)Math.sin(state.ageInTicks*.11)*.025f;
        rot(m.rightArm,-1.12f,-0.26f,0.16f);rot(m.leftArm,-1.35f,0.32f,-0.2f);
        m.rightLeg.xRot+=0.12f;m.leftLeg.xRot-=0.12f;
        if(stance){m.body.yRot+=.18f;m.body.xRot+=.035f;m.head.yRot-=.12f;m.rightArm.xRot+=breath;m.leftArm.xRot-=breath;m.rightLeg.yRot+=.12f;m.leftLeg.yRot-=.1f;
            var level=Minecraft.getInstance().level;if(level!=null&&level.getEntity(id) instanceof net.minecraft.world.entity.LivingEntity e&&e.getMainHandItem().getItem() instanceof com.jjkcraft.item.tool.CursedToolItem)rot(m.rightArm,-.4f+breath,-.18f,.15f);
        }
        if(p.move().equals("knockdown")){m.root().zRot=1.35f;m.root().y+=5;rot(m.rightArm,-0.4f,0,0.5f);rot(m.leftArm,-0.3f,0,-0.4f);m.rightLeg.xRot=-0.4f;}
        else if(p.stun()>0){m.body.xRot+=0.25f;rot(m.rightArm,-0.2f,-0.1f,0.55f);rot(m.leftArm,-0.2f,0.1f,-0.55f);}
        else if(moving){
            float hit=Math.max(0,k),wind=Math.max(0,-k);
            m.root().y+=wind*.65f-hit*.35f;
            m.body.xRot+=wind*.08f-hit*.035f;
            m.head.xRot-=wind*.08f;m.head.yRot+=wind*.12f-hit*.1f;
            m.leftLeg.xRot-=wind*.23f;m.rightLeg.xRot+=wind*.18f;
            switch(p.move()) {
                case "feint" -> {float retract=Math.min(1,hit*.25f);rot(m.rightArm,-1.12f-.4f*retract+.25f*wind,-.18f,.2f);m.body.yRot=.18f*wind-.12f*retract;}
                case "jab" -> {rot(m.rightArm,-1.12f-0.55f*hit+0.8f*wind,-0.26f-0.18f*hit,0.16f);m.body.yRot=-0.32f*hit+0.25f*wind;m.rightArm.z-=hit*1.2f;m.leftLeg.xRot-=hit*.16f;}
                case "hook" -> {rot(m.rightArm,-1.35f+0.2f*wind,-0.9f*hit+1.1f*wind,0.65f);m.body.yRot=-0.7f*hit+0.45f*wind;m.rightLeg.yRot+=.3f*hit;m.head.yRot+=.25f*hit;}
                case "backfist" -> {rot(m.leftArm,-1.3f,1.1f*hit-1.2f*wind,-0.4f);m.body.yRot=0.75f*hit-0.65f*wind;}
                case "uppercut" -> {rot(m.rightArm,-0.4f-2.2f*hit+0.6f*wind,-0.15f,0.15f);m.body.xRot=-0.25f*hit+0.25f*wind;m.leftLeg.xRot=-0.6f*hit;}
                case "heavy" -> {rot(m.rightArm,-1.0f-1.9f*wind+0.4f*hit,-0.2f,0.2f);rot(m.leftArm,-1.0f-1.9f*wind+0.4f*hit,0.2f,-0.2f);m.body.xRot=0.42f*hit-0.18f*wind;}
                case "sweep" -> {m.body.yRot=0.75f*hit-0.45f*wind;m.body.xRot=0.32f*hit;rot(m.rightLeg,-1.35f*hit,0.4f*hit,0.75f*hit);m.leftLeg.xRot=0.35f*hit;rot(m.leftArm,-0.45f,0,-0.8f*hit);}
                case "cast_projectile","cast_strike" -> {rot(m.rightArm,-1.55f+0.5f*wind,-0.15f,0.1f);m.body.yRot=-0.2f*hit;}
                case "cast_summon","cast_domain","cast_restriction" -> {rot(m.rightArm,-1.3f,-0.7f,0.12f);rot(m.leftArm,-1.3f,0.7f,-0.12f);}
                case "cast_area","cast_ultimate" -> {rot(m.rightArm,-1.9f,-0.4f,0.3f);rot(m.leftArm,-1.9f,0.4f,-0.3f);m.body.xRot=-0.12f*wind;}
                case "cast_healing","cast_passive","cast_utility" -> {rot(m.rightArm,-1.0f,-0.6f,0.15f);rot(m.leftArm,-1.0f,0.6f,-0.15f);}
                default -> { }
            }
        }else if(p.guard()>=0){float height=p.guard()==2?-2.25f:p.guard()==0?-0.65f:-1.5f;
            rot(m.rightArm,height,-0.45f,0.28f);rot(m.leftArm,height,0.45f,-0.28f);m.body.xRot+=p.guard()==0?0.3f:0;}
        // Clothing is parented to these limbs in 26.3 and inherits their transforms.
        if(state.mainArm==net.minecraft.world.entity.HumanoidArm.LEFT){
            float x=m.rightArm.xRot,y=m.rightArm.yRot,z=m.rightArm.zRot;
            rot(m.rightArm,m.leftArm.xRot,-m.leftArm.yRot,-m.leftArm.zRot);rot(m.leftArm,x,-y,-z);m.body.yRot=-m.body.yRot;
        }
    }
    public static boolean hands(PoseStack stack,boolean right,float partial){
        var mc=Minecraft.getInstance();if(mc.player==null||mc.player.isUsingItem())return false;var p=pose(mc.player.getId());if(p==null)return false;
        float k=phase(p,partial);boolean move=!p.move().isEmpty()&&Math.abs(k)>0.001f;
        boolean stance=p.move().equals("stance");
        if(!move&&!stance&&p.guard()<0&&p.stun()<=0)return false;
        stack.pushPose();int side=right?1:-1;
        stack.translate(-0.10*side,0.04,-0.1);
        stack.rotateDegrees(Axis.YP,side*8);
        if(stance){float breath=(float)Math.sin(mc.level.getGameTime()*.11)*.008f;stack.translate(-side*.03,.1+breath,-.06);stack.rotateDegrees(Axis.ZP,side*9);
            if(right==(mc.player.getMainArm()==net.minecraft.world.entity.HumanoidArm.RIGHT)&&mc.player.getMainHandItem().getItem() instanceof com.jjkcraft.item.tool.CursedToolItem){stack.translate(side*.14,-.17,.04);stack.rotateDegrees(Axis.ZP,-side*13);}
        }
        if(p.guard()>=0&&!move){stack.translate(-0.16*side,0.2+(p.guard()-1)*0.1,-0.15);stack.rotateDegrees(Axis.ZP,18*side);}
        else if(move){float h=Math.max(0,k),w=Math.max(0,-k);switch(p.move()){
            case "feint" -> {stack.translate(0,.025*h,-.13*h+.09*w);stack.rotateDegrees(Axis.YP,side*10*w);}
            case "hook","backfist" -> {stack.translate(side*(0.3*w-0.35*h),0.12*h,-0.25*h);stack.rotateDegrees(Axis.YP,side*(55*w-60*h));}
            case "uppercut" -> {stack.translate(0,-0.2*w+0.45*h,-0.35*h);stack.rotateDegrees(Axis.XP,35*w-55*h);}
            case "heavy" -> {stack.translate(0,0.4*w-0.1*h,-0.35*h);stack.rotateDegrees(Axis.XP,-55*w+35*h);}
            case "sweep" -> {stack.translate(0,-0.25*h,0.12*h);stack.rotateDegrees(Axis.ZP,22*h);}
            default -> {if(right==(mc.player.getMainArm()==net.minecraft.world.entity.HumanoidArm.RIGHT)){stack.translate(-.035*h,0.08*h,-0.62*h+0.21*w);stack.rotateDegrees(Axis.XP,-22*h+15*w);}else{stack.translate(.04*h,.06*h,.1*h);stack.rotateDegrees(Axis.YP,-12*h);}}
        }}
        return true;
    }
}
