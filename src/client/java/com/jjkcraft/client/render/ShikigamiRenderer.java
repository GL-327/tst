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
	public ShikigamiRenderer(EntityRendererProvider.Context context, String model, float shadow) {
		super(context, new DefaultedEntityGeoModel<>(JjkCraft.id(model)));
		this.shadowRadius = shadow;
	}

	@Override
	protected float getDeathMaxRotation(GeoRenderState renderState) {
		return 0f;
	}
}
