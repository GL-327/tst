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
        float u=(tile%4)*.25f+.0005f,v=(tile/4)*.25f+.0005f,s=.249f;
        vertex(pose,b,at.subtract(right).subtract(up),color,u,v+s);
        vertex(pose,b,at.add(right).subtract(up),color,u+s,v+s);
        vertex(pose,b,at.add(right).add(up),color,u+s,v);
        vertex(pose,b,at.subtract(right).add(up),color,u,v);
    }
    private static void vertex(PoseStack.Pose p,VertexConsumer b,Vec3 v,int color,float u,float w){
        b.addVertex(p,(float)v.x,(float)v.y,(float)v.z).setColor(color).setUv(u,w).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xf000f0).setNormal(p,0,1,0);
    }
    public static int spatialTile(String name){return name.contains("purple")?2:name.contains("red")?1:0;}
    public static void orb(WorldFx.Frame f,Vec3 at,double radius,int tile,int color,float alpha){
        if(com.jjkcraft.client.config.ClientConfig.reduceFlashing())alpha*=.65f;
        double spin=com.jjkcraft.client.config.ClientConfig.reducedMotion()?0:f.time()*(tile==1?-.023:.018);
        f.sprite(at,radius*1.75,radius*1.75,tile,0xffffff,alpha,spin);
        f.sprite(at,radius*2.1,radius*2.1,4,color,alpha*.32f,-spin*.8);
    }
}
