package com.jjkcraft.technique;

import java.util.*;
import net.minecraft.world.phys.Vec3;
import com.jjkcraft.network.payload.TechniqueFxPayload;
import com.jjkcraft.technique.field.SpatialFields;

/** Close-range spatial strikes and independently placed miniature attraction fields. */
public final class LimitlessArts {
    private LimitlessArts() { }
    public static CastResult cast(CastContext ctx,String ability,double damage,int duration,double radius){
        if(ctx.caster() instanceof net.minecraft.server.level.ServerPlayer&&(ability.equals("blue_punch")||ability.equals("red_punch")))return com.jjkcraft.combat.PunchImbuement.toggle(ctx);
        double force=ctx.details().force()/100.0*Math.sqrt(ctx.power());
        if(ability.equals("blue_cluster")){
            Vec3 center=ctx.blockLimit(ctx.range()),side=ctx.look().cross(new Vec3(0,1,0)).normalize();
            for(int i=-1;i<=1;i++){Vec3 at=center.add(side.scale(i*radius*1.4));var hit=ctx.level().clip(new net.minecraft.world.level.ClipContext(ctx.eye(),at,net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,ctx.caster()));at=hit.getLocation();SpatialFields.place(ctx,at,radius,.7*force,damage,10,radius,duration);ctx.fx(TechniqueFxPayload.Phase.FIELD,at,new Vec3(radius,duration,.7),List.of());}
            return CastResult.done(center);
        }
        var target=ctx.rayTarget(Math.min(6,ctx.range()),Math.min(.9,radius*.2)).orElse(null);if(target==null)return CastResult.miss(ctx.blockLimit(ctx.range()));
        ctx.hurt(target,damage*ctx.power(),false);
        if(ability.equals("blue_punch"))target.setDeltaMovement(ctx.caster().position().subtract(target.position()).normalize().scale(.55*force));
        else ctx.knockback(target,ctx.caster().position(),1.25*force);
        target.needsSync=true;ctx.caster().setLastHurtMob(target);ctx.fx(TechniqueFxPayload.Phase.HIT,target.position(),ctx.look(),List.of(target));return CastResult.hit(List.of(target),target.position());
    }
}
