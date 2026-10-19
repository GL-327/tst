package com.jjkcraft.client.modulo;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.animal.squid.SquidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.SquidRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.entity.state.SquidRenderState;
import net.minecraft.resources.Identifier;

import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;

import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

import com.jjkcraft.JjkCraft;
import com.jjkcraft.modulo.entity.MahoragaEntity;
import com.jjkcraft.modulo.entity.ModuloBossEntity;
import com.jjkcraft.modulo.entity.ModuloEntities;
import com.jjkcraft.modulo.entity.ModuloNpcEntity;
import com.jjkcraft.modulo.entity.ToranosukeEntity;
import com.jjkcraft.modulo.entity.WanchaOctopusEntity;

/**
 * Renderers for the Modulo entities. Characters and humanoid bosses use the player model with an
 * original skin from {@code textures/entity/modulo/} (canon names, no canon art); Toranosuke is a
 * GeckoLib four-legged model; the wancha octopus is the squid model with its own texture.
 */
public final class ModuloRenderers {
	private ModuloRenderers() {
	}

	public static void init() {
		EntityRendererRegistry.register(ModuloEntities.NPC, NpcRenderer::new);
		EntityRendererRegistry.register(ModuloEntities.BOSS, BossRenderer::new);
		EntityRendererRegistry.register(ModuloEntities.MAHORAGA, MahoragaRenderer::new);
		EntityRendererRegistry.register(ModuloEntities.TORANOSUKE, ctx -> new TigerRenderer(ctx));
		EntityRendererRegistry.register(ModuloEntities.WANCHA_OCTOPUS, OctopusRenderer::new);
	}

	static Identifier skin(String name) {
		return JjkCraft.id("textures/entity/modulo/" + (name.isEmpty() ? "trafficker" : name) + ".png");
	}

	public static class SkinState extends HumanoidRenderState {
		public String skin = "";
	}

	public static class NpcRenderer extends HumanoidMobRenderer<ModuloNpcEntity, SkinState, HumanoidModel<SkinState>> {
		public NpcRenderer(EntityRendererProvider.Context context) {
			super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.5f);
		}

		@Override
		public SkinState createRenderState() {
			return new SkinState();
		}

		@Override
		public void extractRenderState(ModuloNpcEntity entity, SkinState state, float partialTicks) {
			super.extractRenderState(entity, state, partialTicks);
			state.skin = entity.character();
		}

		@Override
		public Identifier getTextureLocation(SkinState state) {
			return skin(state.skin);
		}
	}

	public static class BossRenderer extends HumanoidMobRenderer<ModuloBossEntity, SkinState, HumanoidModel<SkinState>> {
		public BossRenderer(EntityRendererProvider.Context context) {
			super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.5f);
		}

		@Override
		public SkinState createRenderState() {
			return new SkinState();
		}

		@Override
		public void extractRenderState(ModuloBossEntity entity, SkinState state, float partialTicks) {
			super.extractRenderState(entity, state, partialTicks);
			// Masaki Takeda passes as a boy until revealed.
			state.skin = entity.hasFlag(ModuloBossEntity.FLAG_DISGUISED) ? "masayoshi" : entity.skin();
		}

		@Override
		public Identifier getTextureLocation(SkinState state) {
			return skin(state.skin);
		}
	}

	public static class MahoragaRenderer extends HumanoidMobRenderer<MahoragaEntity, HumanoidRenderState, HumanoidModel<HumanoidRenderState>> {
		private static final Identifier TEXTURE = skin("mahoraga");

		public MahoragaRenderer(EntityRendererProvider.Context context) {
			super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.8f);
		}

		@Override
		public HumanoidRenderState createRenderState() {
			return new HumanoidRenderState();
		}

		@Override
		public Identifier getTextureLocation(HumanoidRenderState state) {
			return TEXTURE;
		}
	}

	public static class TigerRenderer extends GeoEntityRenderer<ToranosukeEntity, LivingEntityRenderState> {
		public TigerRenderer(EntityRendererProvider.Context context) {
			super(context, new DefaultedEntityGeoModel<>(JjkCraft.id("toranosuke")));
			this.shadowRadius = 0.7f;
		}
	}

	public static class OctopusRenderer extends SquidRenderer<WanchaOctopusEntity> {
		private static final Identifier TEXTURE = JjkCraft.id("textures/entity/modulo/wancha_octopus.png");

		public OctopusRenderer(EntityRendererProvider.Context context) {
			super(context, new SquidModel(context.bakeLayer(ModelLayers.SQUID)), new SquidModel(context.bakeLayer(ModelLayers.SQUID_BABY)));
		}

		@Override
		public Identifier getTextureLocation(SquidRenderState state) {
			return TEXTURE;
		}
	}
}
