package com.jjkcraft.client.vfx;

import com.jjkcraft.JjkCraft;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.resources.Identifier;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;

/** Shared depth-tested emissive materials for charges, impacts, fields and actual projectiles. */
public final class EnergySprites {
    public static final Identifier ATLAS=JjkCraft.id("textures/vfx/energy_materials.png");
    private EnergySprites() {}
    public static void quad(PoseStack.Pose pose,VertexConsumer b,Vec3 at,Vec3 right,Vec3 up,int tile,int color){
        float u=(tile%4)*.125f+.0005f,v=(tile/4)*.25f+.0005f,s=.124f,h=.249f;
        vertex(pose,b,at.subtract(right).subtract(up),color,u,v+h);
        vertex(pose,b,at.add(right).subtract(up),color,u+s,v+h);
        vertex(pose,b,at.add(right).add(up),color,u+s,v);
        vertex(pose,b,at.subtract(right).add(up),color,u,v);
    }
    private static void vertex(PoseStack.Pose p,VertexConsumer b,Vec3 v,int color,float u,float w){
        b.addVertex(p,(float)v.x,(float)v.y,(float)v.z).setColor(color).setUv(u,w).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xf000f0).setNormal(p,0,1,0);
    }
    public static int spatialTile(String name){return name.contains("purple")?2:name.contains("red")?1:0;}
    public static void surface(PoseStack.Pose pose,VertexConsumer b,Vec3 a,Vec3 c,Vec3 d,Vec3 e,int tile,int color,float u0,float v0,float u1,float v1){
        float u=(4+Math.floorMod(tile,4))*.125f,v=Math.floorDiv(Math.floorMod(tile,16),4)*.25f;
        u0=.004f+.992f*u0;u1=.004f+.992f*u1;v0=.004f+.992f*v0;v1=.004f+.992f*v1;
        vertex(pose,b,a,color,u+u0*.125f,v+v0*.25f);vertex(pose,b,c,color,u+u1*.125f,v+v0*.25f);vertex(pose,b,d,color,u+u1*.125f,v+v1*.25f);vertex(pose,b,e,color,u+u0*.125f,v+v1*.25f);
        // The entity translucent pipeline disables culling. A reversed duplicate would
        // occupy exactly the same depth and produce patterned z-fighting on Intel GPUs.
    }
    public static void orb(WorldFx.Frame f,Vec3 at,double radius,int tile,int color,float alpha){
        if(com.jjkcraft.client.config.ClientConfig.reduceFlashing())alpha*=.65f;
        double spin=com.jjkcraft.client.config.ClientConfig.reducedMotion()?0:f.time()*(tile==1?-.023:.018);
        VolumeMesh.orb(f,at,new Vec3(radius,radius,radius),tile,color,alpha,spin);
        VolumeMesh.filaments(f,at,new Vec3(radius,radius,radius),tile,color,alpha*.7f,spin);
        f.sprite(at,radius*1.45,radius*1.45,4,color,alpha*.19f,-spin*.8);
    }
}
