package com.jjkcraft.client.training;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import com.jjkcraft.training.TrainingDummyEntity;
public final class TrainingDummyRenderer extends GeoEntityRenderer<TrainingDummyEntity,LivingEntityRenderState>{
    public TrainingDummyRenderer(EntityRendererProvider.Context context){super(context,new DefaultedEntityGeoModel<>(com.jjkcraft.JjkCraft.id("training_dummy")));shadowRadius=.35f;}
}
