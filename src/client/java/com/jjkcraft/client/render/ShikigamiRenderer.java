package com.jjkcraft.client.render;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;

import com.jjkcraft.JjkCraft;
import com.jjkcraft.entity.shikigami.ShikigamiEntity;

/** Shikigami summons: GeckoLib models named after the entity (divine_dog, nue, toad). */
public class ShikigamiRenderer<T extends ShikigamiEntity> extends GeoEntityRenderer<T, LivingEntityRenderState> {
    private final net.minecraft.resources.Identifier glow;
	public ShikigamiRenderer(EntityRendererProvider.Context context, String model, float shadow) {
		super(context, new DefaultedEntityGeoModel<>(JjkCraft.id(model)));
		this.shadowRadius = shadow;
        this.glow=JjkCraft.id("textures/entity/"+model+"_glow.png");
	}

	@Override
	protected float getDeathMaxRotation(GeoRenderState renderState) {
		return 0f;
	}
    @Override public void submitRenderTasks(com.geckolib.renderer.base.RenderPassInfo<LivingEntityRenderState> info,net.minecraft.client.renderer.OrderedSubmitNodeCollector nodes,net.minecraft.client.renderer.rendertype.RenderType type){
        super.submitRenderTasks(info,nodes,type);
        if(com.jjkcraft.client.config.ClientConfig.effectsOff()||net.minecraft.client.Minecraft.getInstance().getResourceManager().getResource(glow).isEmpty())return;
        nodes.submitCustomGeometry(info.poseStack(),net.minecraft.client.renderer.rendertype.RenderTypes.entityTranslucentEmissive(glow),(pose,vertices)->{
            var stack=info.poseStack();stack.pushPose();stack.last().set(pose);info.renderPosed(()->info.model().render(info,vertices,15728880,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,0xDDFFFFFF));stack.popPose();
        });
    }
}
