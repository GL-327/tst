package com.jjkcraft.client.render.culling;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;

import com.jjkcraft.JjkCraft;
import com.jjkcraft.culling.entity.KoganeEntity;

/** The Kogane: a floating paper-mask lantern (GeckoLib). */
public class KoganeRenderer extends GeoEntityRenderer<KoganeEntity, LivingEntityRenderState> {
	public KoganeRenderer(EntityRendererProvider.Context context) {
		super(context, new DefaultedEntityGeoModel<>(JjkCraft.id("kogane")));
		this.shadowRadius = 0.3f;
	}
}
