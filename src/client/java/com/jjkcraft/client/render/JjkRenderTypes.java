package com.jjkcraft.client.render;

import java.util.function.Function;

import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;

import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.oit.OitPipelineSet;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;

import com.jjkcraft.JjkCraft;

/**
 * The mod's own render types. Domain domes and colony barriers are translucent coloured quads:
 * the same pipeline as vanilla {@code debug_quads}, under the mod's name. Shader packs (Iris) only
 * draw pipelines they have a program for, and Iris has none for {@code debug_quads}, so the domes
 * vanished under shaders; a pipeline of our own can be handed a program
 * ({@link com.jjkcraft.client.compat.IrisCompat}) without touching vanilla's debug drawing.
 */
public final class JjkRenderTypes {
	public static final RenderPipeline DOME_PIPELINE = RenderPipelines.register(
			RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET).withLocation(JjkCraft.id("pipeline/dome")).withCull(false).build());

	// Order-independent transparency uses vanilla's debug-quad set: same shaders, same vertex format.
	private static final RenderType DOME = RenderType.create("jjkcraft_dome",
			RenderSetup.builder(DOME_PIPELINE).setOitPipelines(RenderPipelines.OIT_DEBUG_QUADS).sortOnUpload().createRenderSetup());

	/**
	 * Cursed-energy light: unlit textured quads added to the frame, depth-tested but never writing
	 * depth, so overlapping glows brighten each other and whatever is behind them. Vanilla has no
	 * such pipeline: its eyes layer is alpha-blended and its energy swirl writes depth.
	 */
	public static final RenderPipeline GLOW_PIPELINE = RenderPipelines.register(
			RenderPipeline.builder(RenderPipelines.ENTITY_NO_LIGHTMAP_SNIPPET, RenderPipelines.EYES_SNIPPET)
					.withLocation(JjkCraft.id("pipeline/glow"))
					.withFragmentShader(JjkCraft.id("core/glow"))
					.withColorTargetState(new ColorTargetState(BlendFunction.ADDITIVE))
					.withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
					.withCull(false)
					.build());
	private static final OitPipelineSet OIT_GLOW = RenderPipelines.register(OitPipelineSet.builder("jjkcraft_glow",
			RenderPipeline.builder(RenderPipelines.OIT_ENTITY_SNIPPET, RenderPipelines.EYES_SNIPPET).withFragmentShader(JjkCraft.id("core/glow")).withShaderDefine("OIT_ADDITIVE")).build());
	private static final Function<Identifier, RenderType> GLOW = Util.memoize(texture -> RenderType.create("jjkcraft_glow",
			RenderSetup.builder(GLOW_PIPELINE).setOitPipelines(OIT_GLOW).withTexture("Sampler0", texture).sortOnUpload().createRenderSetup()));

	private JjkRenderTypes() {
	}

	/** Additive glow sprites drawn from {@code texture}. */
	public static RenderType glow(Identifier texture) {
		return GLOW.apply(texture);
	}

	/** Translucent, unlit, double-sided position-colour quads. */
	public static RenderType dome() {
		return DOME;
	}
}
