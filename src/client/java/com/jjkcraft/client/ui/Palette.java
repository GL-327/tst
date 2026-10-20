package com.jjkcraft.client.ui;

import com.jjkcraft.client.config.ClientConfig;

/**
 * The one color palette every screen and HUD element uses. The colorblind variant swaps the
 * accent hues for Okabe-Ito colors, which stay distinct under common color vision deficiencies.
 * Meaning never depends on color alone: bars also change pattern and icons.
 */
public final class Palette {
	/** Panel fill: near-black with a cold tint, like dried ink. */
	public static final int INK = 0xF00E121D;
	public static final int INK_DEEP = 0xF0090D16;
	public static final int INK_EDGE = 0xFF303B50;
	public static final int PAPER = 0xFFE9E3D5;
	public static final int TEXT = 0xFFEDE7F6;
	public static final int TEXT_DIM = 0xFFACB8CE;
	public static final int SHADOW = 0x90000000;

	private Palette() {
	}

	/** Cursed energy, normal state. */
	public static int energy() {
		return cb() ? 0xFF56B4E9 : theme();
	}

	/** Second stop of the CE gradient. */
	public static int energyHigh() {
		return cb() ? 0xFF0072B2 : lighten(theme());
	}

	/** Overexertion / danger. */
	public static int danger() {
		return cb() ? 0xFFD55E00 : 0xFFE0303F;
	}

	public static int dangerHigh() {
		return cb() ? 0xFFE69F00 : 0xFFFF8A3D;
	}

	/** Positive / ready / healing. */
	public static int good() {
		return cb() ? 0xFF009E73 : 0xFF5BE38C;
	}

	/** Highlight / gold accents (promotion, points). */
	public static int gold() {
		return cb() ? 0xFFF0E442 : 0xFFF2C14E;
	}

	/** Cursed accent used for brush strokes and borders. */
	public static int accent() {
		return cb() ? 0xFFCC79A7 : theme();
	}
	private static int theme(){
		var id=com.jjkcraft.client.state.ClientTechniques.state().innate();
		return id.flatMap(com.jjkcraft.client.state.ClientTechniques::definition)
			.map(d->0xFF000000|d.visuals().color()).orElse(0xFF9870E8);
	}
	private static int lighten(int c){return 0xFF000000|Math.min(255,((c>>16)&255)+55)<<16|Math.min(255,((c>>8)&255)+55)<<8|Math.min(255,(c&255)+55);}

	/** Cooldown / disabled. */
	public static int muted() {
		return 0xFF55505F;
	}

	private static boolean cb() {
		return ClientConfig.get().accessibility.colorblindPalette;
	}
}
