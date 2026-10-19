package com.jjkcraft.client.render;

import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

import com.jjkcraft.JjkCraft;
import com.jjkcraft.entity.ShadowDivineDogEntity;

/** Shadow-garden hounds: GeckoLib assets at the default paths for {@code jjkcraft:shadow_divine_dog}. */
public class ShadowDivineDogRenderer extends GeoEntityRenderer<ShadowDivineDogEntity, LivingEntityRenderState> {
	public ShadowDivineDogRenderer(EntityRendererProvider.Context context) {
		super(context, new DefaultedEntityGeoModel<>(JjkCraft.id("shadow_divine_dog")));
		this.shadowRadius = 0.4f;
	}

	@Override
	protected float getDeathMaxRotation(GeoRenderState renderState) {
		return 0f;
	}
}
