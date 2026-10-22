package com.jjkcraft.vfx;

import net.minecraft.world.phys.Vec3;

/** Stable cast-local coordinates, including vertical casts. Independent of the observer camera. */
public record EffectOrientation(Vec3 forward,Vec3 right,Vec3 up){
    public static boolean valid(Vec3 v){return v!=null&&Double.isFinite(v.x)&&Double.isFinite(v.y)&&Double.isFinite(v.z)&&v.lengthSqr()>1e-10;}
    public static EffectOrientation of(Vec3 aim){
        Vec3 f=valid(aim)?aim.normalize():new Vec3(0,0,1);
        Vec3 reference=Math.abs(f.y)>.999?new Vec3(0,0,f.y>0?-1:1):new Vec3(0,1,0);
        Vec3 r=reference.cross(f).normalize();return new EffectOrientation(f,r,f.cross(r).normalize());
    }
    public Vec3 at(Vec3 origin,double x,double y,double z){return origin.add(right.scale(x)).add(up.scale(y)).add(forward.scale(z));}
}
