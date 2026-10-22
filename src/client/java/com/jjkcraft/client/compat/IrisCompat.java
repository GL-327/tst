package com.jjkcraft.client.compat;

import net.irisshaders.iris.api.v0.IrisApi;
import net.irisshaders.iris.api.v0.IrisProgram;

import com.jjkcraft.client.render.JjkRenderTypes;

/**
 * Iris integration. Only loaded when Iris is installed. A shader pack draws a pipeline only through a
 * program Iris assigned to it; without one, domain domes and colony barriers are not drawn at all.
 */
public final class IrisCompat {
	private IrisCompat() {
	}

	public static void init() {
		// The pack's basic program: unlit coloured geometry, like the leash and debug lines.
		IrisApi.getInstance().assignPipeline(JjkRenderTypes.DOME_PIPELINE, IrisProgram.BASIC);
		// Energy glows are self-lit textured sprites, like spider eyes.
		IrisApi.getInstance().assignPipeline(JjkRenderTypes.GLOW_PIPELINE, IrisProgram.EMISSIVE_ENTITIES);
	}
}
