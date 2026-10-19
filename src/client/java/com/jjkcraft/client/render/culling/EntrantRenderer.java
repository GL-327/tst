package com.jjkcraft.client.render.culling;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

import com.jjkcraft.JjkCraft;
import com.jjkcraft.culling.entity.EntrantEntity;

/** Culling Game entrants: a plain humanoid on the player model with one of the original entrant skins. */
public class EntrantRenderer extends HumanoidMobRenderer<EntrantEntity, EntrantRenderer.State, HumanoidModel<EntrantRenderer.State>> {
	private static final Identifier[] SKINS = new Identifier[EntrantEntity.SKINS];

	static {
		for (int i = 0; i < SKINS.length; i++) {
			SKINS[i] = JjkCraft.id("textures/entity/entrant/entrant_" + i + ".png");
		}
	}

	public static class State extends HumanoidRenderState {
		public int skin;
	}

	public EntrantRenderer(EntityRendererProvider.Context context) {
		super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.5f);
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(EntrantEntity entity, State state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.skin = entity.skin();
	}

	@Override
	public Identifier getTextureLocation(State state) {
		return SKINS[Math.floorMod(state.skin, SKINS.length)];
	}
}
