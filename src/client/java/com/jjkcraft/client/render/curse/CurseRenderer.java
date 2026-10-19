package com.jjkcraft.client.render.curse;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.jspecify.annotations.Nullable;

import com.jjkcraft.curse.CurseEntity;

/**
 * Renders every curse through {@link CurseModel}. Curses fade in while emerging and dissolve
 * (fade out) while dying instead of tipping over; the data's model scale applies. Sensed curses
 * get a grade-colored outline, which GeckoLib's custom geometry doesn't draw on its own.
 */
public class CurseRenderer extends GeoEntityRenderer<CurseEntity, LivingEntityRenderState> {
	private static final DataTicket<Float> MODEL_SCALE = DataTicket.create("jjkcraft_curse_scale", Float.class);
	private static final DataTicket<Float> ALPHA = DataTicket.create("jjkcraft_curse_alpha", Float.class);
	private static final int FULL_BRIGHT = 15728880;

	public CurseRenderer(EntityRendererProvider.Context context) {
		super(context, new CurseModel());
		this.shadowRadius = 0.5f;
	}

	@Override
	public void addRenderData(CurseEntity curse, @Nullable Void relatedObject, LivingEntityRenderState renderState, float partialTick) {
		renderState.addGeckolibData(MODEL_SCALE, curse.definition().map(d -> d.model().scale()).orElse(1f));
		renderState.addGeckolibData(ALPHA, alpha(curse, partialTick));
	}

	/** 0 to 1: rises over the emergence, falls over the last two thirds of the death animation. */
	public static float alpha(CurseEntity curse, float partialTick) {
		if (curse.deathTime > 0) {
			float t = (curse.deathTime + partialTick) / CurseEntity.DEATH_TICKS;
			return Math.clamp(1.5f - 1.5f * t, 0f, 1f);
		}

		int emerging = curse.getEntityData().get(CurseEntity.EMERGING);
		if (emerging > 0) {
			float t = 1 - (emerging - partialTick) / CurseEntity.EMERGE_TICKS;
			return Math.clamp(0.15f + t, 0.15f, 1f);
		}

		return 1f;
	}

	@Override
	public int getRenderColor(CurseEntity curse, @Nullable Void relatedObject, float partialTick) {
		int color = super.getRenderColor(curse, relatedObject, partialTick);
		float alpha = alpha(curse, partialTick);
		return alpha >= 1f ? color : ARGB.color(Math.round(ARGB.alpha(color) * alpha), color);
	}

	@Override
	protected float getDeathMaxRotation(GeoRenderState renderState) {
		// Curses dissolve instead of toppling.
		return 0f;
	}

	@Override
	public void scaleModelForRender(RenderPassInfo<LivingEntityRenderState> renderPassInfo, float widthScale, float heightScale) {
		float scale = renderPassInfo.renderState().getOrDefaultGeckolibData(MODEL_SCALE, 1f);
		super.scaleModelForRender(renderPassInfo, widthScale * scale, heightScale * scale);
	}

	@Override
	public @Nullable RenderType getRenderType(LivingEntityRenderState renderState, Identifier texture) {
		if (renderState.getOrDefaultGeckolibData(ALPHA, 1f) < 1f) {
			return RenderTypes.entityTranslucent(texture);
		}

		return super.getRenderType(renderState, texture);
	}

	@Override
	public void submitRenderTasks(RenderPassInfo<LivingEntityRenderState> renderPassInfo, OrderedSubmitNodeCollector renderTasks,
			@Nullable RenderType renderType) {
		super.submitRenderTasks(renderPassInfo, renderTasks, renderType);

		int outline = renderPassInfo.renderState().outlineColor;
		if (outline == 0 || renderType == null || renderPassInfo.model().isMissingno()) {
			return;
		}

		RenderType outlineType = RenderTypes.outline(getTextureLocation(renderPassInfo.renderState()));
		renderTasks.submitCustomGeometry(renderPassInfo.poseStack(), outlineType, (pose, vertexConsumer) -> {
			PoseStack poseStack = renderPassInfo.poseStack();
			poseStack.pushPose();
			poseStack.last().set(pose);
			renderPassInfo.renderPosed(() -> renderPassInfo.model().render(renderPassInfo, vertexConsumer, FULL_BRIGHT, OverlayTexture.NO_OVERLAY, outline));
			poseStack.popPose();
		});
	}
}
