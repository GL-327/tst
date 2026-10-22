package com.jjkcraft.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

/**
 * Drawing primitives for the shared ink-panel style. Crisp borders and restrained accents
 * keep dense technique screens readable with any renderer, resource pack or shader pack.
 */
public final class Ink {
	private Ink() {
	}

	public static Font font() {
		return Minecraft.getInstance().font;
	}

	/**
     * A dark panel with a thin border and an accent corner.
	 *
	 * @param alpha overall opacity multiplier (HUD opacity setting)
	 */
	public static void panel(GuiGraphicsExtractor g, int x, int y, int w, int h, int accent, float alpha) {
        g.fill(x,y,x+w,y+h,ARGB.multiplyAlpha(Palette.INK,alpha));
        g.outline(x,y,w,h,ARGB.multiplyAlpha(Palette.INK_EDGE,alpha));
        g.fill(x,y,x+Math.min(w,24),y+2,ARGB.multiplyAlpha(accent,alpha));
        g.fill(x,y,x+2,y+Math.min(h,12),ARGB.multiplyAlpha(accent,alpha));

	}

	/** A plain panel without the ragged edges, for dense layouts (lists, tooltips). */
	public static void flatPanel(GuiGraphicsExtractor g, int x, int y, int w, int h, float alpha) {
		g.fill(x, y, x + w, y + h, ARGB.multiplyAlpha(Palette.INK_DEEP, alpha));
		g.outline(x, y, w, h, ARGB.multiplyAlpha(Palette.INK_EDGE, alpha));
	}

	/** A horizontal gradient fill (vanilla only offers vertical gradients). */
	public static void hGradient(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int left, int right) {
		int w = x1 - x0;
		if (w <= 0) {
			return;
		}

		for (int i = 0; i < w; i++) {
			float t = w == 1 ? 0 : (float) i / (w - 1);
			g.fill(x0 + i, y0, x0 + i + 1, y1, ARGB.srgbLerp(t, left, right));
		}
	}

	public static void text(GuiGraphicsExtractor g, Component text, int x, int y, int color, float alpha) {
        var m=Minecraft.getInstance();var at=g.pose().transformPosition(new org.joml.Vector2f(x,y));var end=g.pose().transformPosition(new org.joml.Vector2f(x+font().width(text),y+9));
        var p=com.jjkcraft.client.design.UiDesign.text(text.getString(),(int)at.x,(int)at.y,Math.max(1,(int)Math.abs(end.x-at.x)),Math.max(1,(int)Math.abs(end.y-at.y)),color);
        if(p!=null){if(p.hidden())return;var target=new org.joml.Matrix3x2f(g.pose()).invert().transformPosition(new org.joml.Vector2f(p.x()*m.getWindow().getGuiScaledWidth()/640f,p.y()*m.getWindow().getGuiScaledHeight()/360f));
            float desired=p.h()*m.getWindow().getGuiScaledHeight()/360f,current=Math.abs(end.y-at.y),scale=Math.abs(desired-current)<1?1:Math.clamp(desired/Math.max(.01f,current),.5f,3f);
            g.pose().pushMatrix();g.pose().translate(target.x,target.y);g.pose().scale(scale,scale);g.text(font(),text,0,0,ARGB.multiplyAlpha(p.color(),alpha),false);g.pose().popMatrix();return;}
        g.text(font(), text, x, y, ARGB.multiplyAlpha(color, alpha), false);
	}

	public static void text(GuiGraphicsExtractor g, String text, int x, int y, int color, float alpha) {text(g,Component.literal(text),x,y,color,alpha);
	}

	/** {@code text} cut to {@code width} pixels with an ellipsis, keeping its style. */
	public static Component fit(Component text, int width) {
		if (font().width(text) <= width) {
			return text;
		}

		String cut = font().plainSubstrByWidth(text.getString(), Math.max(0, width - font().width("…")));
		return Component.literal(cut.stripTrailing() + "…").withStyle(text.getStyle());
	}

	public static void centered(GuiGraphicsExtractor g, Component text, int cx, int y, int color, float alpha) {
		text(g,text,cx-font().width(text)/2,y,color,alpha);
	}

	/** A filled diamond (badges, wheel nodes). */
	public static void diamond(GuiGraphicsExtractor g, int cx, int cy, int radius, int color) {
		for (int dy = -radius; dy <= radius; dy++) {
			int half = radius - Math.abs(dy);
			g.fill(cx - half, cy + dy, cx + half + 1, cy + dy + 1, color);
		}
	}

	/** A filled circle (radar backgrounds). */
	public static void disc(GuiGraphicsExtractor g, int cx, int cy, int radius, int color) {
		for (int dy = -radius; dy <= radius; dy++) {
			int half = (int) Math.round(Math.sqrt(radius * radius - dy * dy));
			g.fill(cx - half, cy + dy, cx + half + 1, cy + dy + 1, color);
		}
	}

	/** A ring approximated with short rectangles; {@code fraction} draws a partial ring clockwise from the top. */
	public static void ring(GuiGraphicsExtractor g, int cx, int cy, int radius, int thickness, float fraction, int color) {
		int steps = Math.max(12, radius * 6);
		int drawn = (int) (steps * Mth.clamp(fraction, 0, 1));
		for (int i = 0; i < drawn; i++) {
			double a = -Math.PI / 2 + 2 * Math.PI * i / steps;
			for (int t = 0; t < thickness; t++) {
				int px = cx + (int) Math.round(Math.cos(a) * (radius - t));
				int py = cy + (int) Math.round(Math.sin(a) * (radius - t));
				g.fill(px, py, px + 1, py + 1, color);
			}
		}
	}

	private static int hash(int v) {
		v ^= v >>> 16;
		v *= 0x7feb352d;
		v ^= v >>> 15;
		v *= 0x846ca68b;
		v ^= v >>> 16;
		return v & 0x7fffffff;
	}
}
