package com.jjkcraft.client.vfx;

import com.jjkcraft.network.payload.TechniqueFxPayload;
import com.jjkcraft.vfx.EffectOrientation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** Live entity facing is only a fallback for older local effect callers, never radius metadata. */
public final class EffectAim {
    private EffectAim(){}
    public static Vec3 forward(TechniqueFxPayload fx,Entity caster){return EffectOrientation.of(EffectOrientation.valid(fx.aim())?fx.aim():caster!=null?caster.getLookAngle():new Vec3(0,0,1)).forward();}
}
