package com.jjkcraft.client.screen;

import java.util.List;
import java.util.Optional;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import com.jjkcraft.client.JjkKeys;
import com.jjkcraft.client.config.ClientConfig;
import com.jjkcraft.client.state.ClientTechniques;
import com.jjkcraft.client.ui.Ink;
import com.jjkcraft.client.ui.Palette;
import com.jjkcraft.client.ui.TechniqueTooltip;
import com.jjkcraft.network.payload.CastTechniquePayload;
import com.jjkcraft.network.payload.SetLoadoutPayload;
import com.jjkcraft.technique.TechniqueDefinition;

/**
 * The technique wheel, opened by holding the wheel key. Point at a technique: releasing the key
 * casts it; left click equips it in the selected hotbar slot; right click casts without closing.
 * Nodes bloom outward when it opens (instant with reduced motion).
 */
public final class TechniqueWheelScreen extends Screen {
	private static final int NODE = 13;
	private final long openedAt = System.currentTimeMillis();
	private List<Identifier> techniques = List.of();
	private int hovered = -1;

	public TechniqueWheelScreen() {
		super(Component.translatable("screen.jjkcraft.wheel"));
	}

	@Override
	protected void init() {
		techniques = ClientTechniques.known();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
		g.fill(0, 0, width, height, 0x70050508);
	}

	private float openProgress() {
		if (ClientConfig.reducedMotion()) {
			return 1;
		}

		float t = Math.min(1, (System.currentTimeMillis() - openedAt) / 180f);
		return 1 - (1 - t) * (1 - t);
	}

	private int radius() {
		return Math.max(40, Math.min(90, 26 + techniques.size() * 6));
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
		int cx = width / 2;
		int cy = height / 2;
		float open = openProgress();
		int radius = (int) (radius() * open);
		hovered = pick(mouseX, mouseY);

		Ink.ring(g, cx, cy, radius + NODE + 4, 2, 1, ARGB.multiplyAlpha(Palette.accent(), 0.6f * open));
		Ink.ring(g, cx, cy, 16, 2, 1, ARGB.multiplyAlpha(Palette.INK_EDGE, open));

		if (techniques.isEmpty()) {
			Ink.centered(g, Component.translatable("screen.jjkcraft.wheel.empty"), cx, cy - 4, Palette.TEXT_DIM, 1f);
			return;
		}

		for (int i = 0; i < techniques.size(); i++) {
			Identifier id = techniques.get(i);
			Optional<TechniqueDefinition> def = ClientTechniques.definition(id);
			if (def.isEmpty()) {
				continue;
			}

			// Stagger the bloom so nodes appear one after another.
			float stagger = ClientConfig.reducedMotion() ? 1 : Mth.clamp(open * 1.4f - i * 0.04f, 0, 1);
			double angle = -Math.PI / 2 + 2 * Math.PI * i / techniques.size();
			int nx = cx + (int) (Math.cos(angle) * radius * stagger);
			int ny = cy + (int) (Math.sin(angle) * radius * stagger);
			boolean hot = i == hovered;
			int size = hot ? NODE + 3 : NODE;
			int color = def.get().visuals().color();
			boolean usable = ClientTechniques.gradeAllows(def.get()) && ClientTechniques.cooldownFraction(id) <= 0;

			Ink.diamond(g, nx, ny, size + 2, ARGB.multiplyAlpha(hot ? color : Palette.INK_EDGE, stagger));
			g.fill(nx - size + 3, ny - size + 3, nx + size - 3, ny + size - 3, ARGB.multiplyAlpha(0xFF0E0C14, stagger));
			int icon = (size - 3) * 2;
			g.blit(RenderPipelines.GUI_TEXTURED, def.get().icon(), nx - icon / 2, ny - icon / 2, 0, 0, icon, icon, icon, icon,
					ARGB.white(stagger * (usable ? 1f : 0.45f)));
			float cd = ClientTechniques.cooldownFraction(id);
			if (cd > 0) {
				Ink.ring(g, nx, ny, size, 1, 1 - cd, ARGB.multiplyAlpha(color, stagger));
			}

			if (ClientTechniques.state().loadout().contains(Optional.of(id))) {
				g.fill(nx + size - 4, ny - size + 1, nx + size - 1, ny - size + 4, Palette.gold());
			}
		}

		if (hovered >= 0) {
			Identifier id = techniques.get(hovered);
			ClientTechniques.definition(id).ifPresent(def -> {
				Ink.centered(g, Component.translatable(def.name()), cx, cy - 4, def.visuals().color(), 1f);
				g.setComponentTooltipForNextFrame(Ink.font(), TechniqueTooltip.lines(id, def), mouseX, mouseY);
			});
		} else {
			Ink.centered(g, Component.translatable("screen.jjkcraft.wheel.hint"), cx, cy - 4, Palette.TEXT_DIM, 1f);
		}
	}

	private int pick(int mouseX, int mouseY) {
		if (techniques.isEmpty()) {
			return -1;
		}

		int dx = mouseX - width / 2;
		int dy = mouseY - height / 2;
		if (dx * dx + dy * dy < 18 * 18) {
			return -1;
		}

		double angle = Math.atan2(dy, dx) + Math.PI / 2;
		if (angle < 0) {
			angle += 2 * Math.PI;
		}

		double step = 2 * Math.PI / techniques.size();
		return (int) Math.floor((angle + step / 2) / step) % techniques.size();
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (hovered < 0) {
			return super.mouseClicked(event, doubleClick);
		}

		Identifier id = techniques.get(hovered);
		if (event.button() == 0) {
			int slot = ClientTechniques.state().selected();
			ClientPlayNetworking.send(new SetLoadoutPayload(slot, Optional.of(id)));
		} else if (event.button() == 1) {
			ClientPlayNetworking.send(new CastTechniquePayload(id));
		}

		return true;
	}

	@Override
	public boolean keyReleased(KeyEvent event) {
		if (JjkKeys.WHEEL.matches(event)) {
			if (hovered >= 0) {
				ClientPlayNetworking.send(new CastTechniquePayload(techniques.get(hovered)));
			}

			onClose();
			return true;
		}

		return super.keyReleased(event);
	}
}
