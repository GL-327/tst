package com.jjkcraft.client.render;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;

import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;

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

	private JjkRenderTypes() {
	}

	/** Translucent, unlit, double-sided position-colour quads. */
	public static RenderType dome() {
		return DOME;
	}
}
