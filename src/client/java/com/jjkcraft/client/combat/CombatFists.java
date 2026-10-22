package com.jjkcraft.client.combat;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;

/** Wrist-centered first-person fists with bent forearms and the player's own skin and sleeves. */
public final class CombatFists {
    private static final ModelPart RIGHT=build(true,false),LEFT=build(false,false),RIGHT_SLIM=build(true,true),LEFT_SLIM=build(false,true);
    private CombatFists(){}
    private static ModelPart build(boolean right,boolean slim){
        var mesh=new MeshDefinition();var root=mesh.getRoot();float w=slim?3:4;
        int u=right?40:32,v=right?16:48,su=right?40:48,sv=right?32:48;
        root.addOrReplaceChild("fist",CubeListBuilder.create().texOffs(u,v+6).mirror(!right).addBox(-w/2-.15f,-2,-2,w+.3f,4,4),PartPose.ZERO);
        root.addOrReplaceChild("thumb",CubeListBuilder.create().texOffs(u,v+6).mirror(!right).addBox(right?-w/2-1:w/2-.5f,-.4f,-1,1.5f,2.4f,2.5f),PartPose.ZERO);
        // The wrist bends away from the knuckles, down and back toward the viewer.
        var arm=root.addOrReplaceChild("forearm",CubeListBuilder.create().texOffs(u,v).mirror(!right).addBox(-w/2,0,-2,w,8,4),PartPose.offsetAndRotation(0,1,0,-.85f,0,0));
        arm.addOrReplaceChild("sleeve",CubeListBuilder.create().texOffs(su,sv).mirror(!right).addBox(-w/2,0,-2,w,8,4,new CubeDeformation(.20f)),PartPose.ZERO);
        var model=LayerDefinition.create(mesh,64,64).bakeRoot();
        // A shortened arm cube would reuse sleeve pixels on the knuckles. Sample the actual
        // bottom four skin rows on every fist face, also for slim and left-handed skins.
        for(String part:java.util.List.of("fist","thumb")){
            var cube=model.getChild(part).getRandomCube(net.minecraft.util.RandomSource.create(0));
            for(var face:cube.polygons){var vertices=face.vertices();for(int i=0;i<4;i++)vertices[i]=vertices[i].remap((u+4+(i==0||i==3?0:w))/64f,(v+12+(i<2?0:4))/64f);}
        }
        return model;
    }
    public static void submit(PoseStack stack,SubmitNodeCollector nodes,int light,PlayerRenderState player,boolean right){
        var avatar=player.avatarRenderState;if(avatar==null)return;
        boolean slim=avatar.skin.model()==net.minecraft.world.entity.player.PlayerModelType.SLIM;
        ModelPart model=right?(slim?RIGHT_SLIM:RIGHT):(slim?LEFT_SLIM:LEFT);
        model.getChild("forearm").getChild("sleeve").visible=right?avatar.showRightSleeve:avatar.showLeftSleeve;
        stack.scale(.70f,-.70f,-.70f);
        nodes.submitModelPart(model,stack,RenderTypes.entityCutout(avatar.skin.body().texturePath()),light,OverlayTexture.NO_OVERLAY,null);
    }
}
