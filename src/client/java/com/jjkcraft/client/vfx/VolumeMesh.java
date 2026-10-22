package com.jjkcraft.client.vfx;

import com.jjkcraft.vfx.EffectOrientation;
import net.minecraft.world.phys.Vec3;

/** Actual depth-tested surfaces. Camera movement reveals their side and back, not another flat disc. */
public final class VolumeMesh {
    private VolumeMesh(){}
    public static void projectile(com.mojang.blaze3d.vertex.PoseStack.Pose pose,com.mojang.blaze3d.vertex.VertexConsumer buffer,double radius,int tile,int color,float alpha,float time,boolean low){projectile(pose,buffer,radius,tile,color,alpha,time,low,false);}
    public static void projectile(com.mojang.blaze3d.vertex.PoseStack.Pose pose,com.mojang.blaze3d.vertex.VertexConsumer buffer,double radius,int tile,int color,float alpha,float time,boolean low,boolean depthOnly){var f=new WorldFx.Frame(pose,buffer,Vec3.ZERO,time,low);f.depthOnly=depthOnly;double spin=com.jjkcraft.client.config.ClientConfig.reducedMotion()?0:time*.025;var size=new Vec3(radius,radius,radius);orb(f,Vec3.ZERO,size,tile,color,alpha,spin);filaments(f,Vec3.ZERO,size,tile,color,alpha*.65f,spin);}
    public static void orb(WorldFx.Frame f,Vec3 at,Vec3 size,int tile,int color,float alpha,double spin){
        int sectors=f.segments(16),bands=f.low?5:8;
        for(int band=0;band<bands&&f.budgetLeft();band++)for(int n=0;n<sectors&&f.budgetLeft();n++){
            double p=band*Math.PI/bands-Math.PI/2,q=(band+1)*Math.PI/bands-Math.PI/2,a=n*Math.PI*2/sectors+spin,b=(n+1)*Math.PI*2/sectors+spin;
            Vec3 v0=ball(at,size,p,a),v1=ball(at,size,p,b),v2=ball(at,size,q,b),v3=ball(at,size,q,a);
            double shade=.86+.14*Math.max(0,Math.sin((p+q)/2)*.7+Math.cos((a+b)/2)*.5);int tint=tint(color,shade);
            f.surface(v0,v1,v2,v3,tile,tint,alpha,n/(float)sectors,band/(float)bands,(n+1f)/sectors,(band+1f)/bands);
        }
    }
    private static Vec3 ball(Vec3 c,Vec3 size,double latitude,double longitude){return c.add(Math.cos(latitude)*Math.cos(longitude)*size.x,Math.sin(latitude)*size.y,Math.cos(latitude)*Math.sin(longitude)*size.z);}
    /** Thin bright currents wrap the core itself; their back halves disappear behind it. */
    public static void filaments(WorldFx.Frame f,Vec3 at,Vec3 size,int tile,int color,float alpha,double spin){
        Vec3 shell=size.scale(1.025);int segments=f.segments(24),strands=f.low?2:3;
        int light=(((color>>16&255)*45/100+140)<<16)|(((color>>8&255)*45/100+140)<<8)|((color&255)*45/100+140);
        for(int n=0;n<strands;n++)for(int i=0;i<segments&&f.budgetLeft();i++){
            double p=(i/(double)segments-.5)*2.55,q=((i+1)/(double)segments-.5)*2.55;
            double a=p*2.8+spin*2+n*Math.PI*2/strands,b=q*2.8+spin*2+n*Math.PI*2/strands;
            double width=.012+.012*Math.sin((i+.5)*Math.PI/segments);
            f.surface(ball(at,shell,p,a-width),ball(at,shell,q,b-width),ball(at,shell,q,b+width),ball(at,shell,p,a+width),tile,light,alpha,i/(float)segments,0,(i+1f)/segments,1);
        }
    }
    public static int tint(int color,double shade){return ((int)((color>>16&255)*shade)<<16)|((int)((color>>8&255)*shade)<<8)|(int)((color&255)*shade);}
    /** A thick curved pressure front, open in the middle and with real axial depth. */
    public static void pressure(WorldFx.Frame f,Vec3 center,EffectOrientation basis,double radius,double width,int tile,int color,float alpha,double spin){
        int sectors=f.segments(28),sides=f.low?4:6;
        for(int n=0;n<sectors&&f.budgetLeft();n++)for(int j=0;j<sides&&f.budgetLeft();j++){
            double a=n*Math.PI*2/sectors+spin,b=(n+1)*Math.PI*2/sectors+spin,p=j*Math.PI*2/sides,q=(j+1)*Math.PI*2/sides;
            f.surface(torus(center,basis,radius,width,a,p),torus(center,basis,radius,width,b,p),torus(center,basis,radius,width,b,q),torus(center,basis,radius,width,a,q),tile,color,alpha,n/(float)sectors,j/(float)sides,(n+1f)/sectors,(j+1f)/sides);
        }
    }
    private static Vec3 torus(Vec3 c,EffectOrientation b,double r,double width,double a,double p){double radial=r+Math.cos(p)*width;return b.at(c,Math.cos(a)*radial,Math.sin(a)*radial,Math.sin(p)*width*1.8);}
    public static void shard(WorldFx.Frame f,Vec3 at,EffectOrientation basis,double width,double length,int tile,int color,float alpha){
        Vec3 top=basis.at(at,0,length,0),bottom=basis.at(at,0,-length*.55,0),left=basis.at(at,-width,0,0),right=basis.at(at,width,0,0),front=basis.at(at,0,0,width*.65),back=basis.at(at,0,0,-width*.65);
        Vec3[] rim={left,front,right,back};for(int n=0;n<4;n++){Vec3 a=rim[n],b=rim[(n+1)%4];f.surface(top,a,b,top,tile,tint(color,.6+n*.12),alpha,0,0,1,1);f.surface(bottom,b,a,bottom,tile,tint(color,.5+n*.12),alpha,0,0,1,1);}
    }
}
