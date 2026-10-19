package com.jjkcraft.client.screen;

import java.util.List;
import java.util.Optional;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import com.jjkcraft.client.state.ClientTechniques;
import com.jjkcraft.client.ui.Ink;
import com.jjkcraft.client.ui.Palette;
import com.jjkcraft.client.ui.TechniqueTooltip;
import com.jjkcraft.network.payload.SelectSlotPayload;
import com.jjkcraft.network.payload.SetLoadoutPayload;
import com.jjkcraft.technique.TechniqueDefinition;
import com.jjkcraft.technique.TechniqueState;

/**
 * Technique Loadout: drag a known technique onto one of the hotbar slots. Right-click a slot to
 * clear it, left-click to select it for the cast key. Locked slots (above the grade's slot count)
 * are shown but cannot be filled.
 */
public final class LoadoutScreen extends JjkScreen {
	private static final int CELL = 24;
	private Identifier dragging;
	private double dragX;
	private double dragY;

	public LoadoutScreen() {
		super(Component.translatable("screen.jjkcraft.loadout"));
	}

	@Override
	protected void initContent() {
	}

	private int listX() {
		return panelX() + 8;
	}

	private int listY() {
		return contentY() + 12;
	}

	private int columns() {
		return Math.max(1, (panelW() - 16 - 4 * CELL - 30) / (CELL + 4));
	}

	private int slotX() {
		return panelX() + panelW() - 8 - 2 * (CELL + 6);
	}

	private int slotY(int index) {
		return contentY() + 12 + (index / 2) * (CELL + 14);
	}

	private int slotCellX(int index) {
		return slotX() + (index % 2) * (CELL + 6);
	}

	@Override
	protected void extractContent(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
		List<Identifier> known = ClientTechniques.known();
		Ink.text(g, Component.translatable("screen.jjkcraft.loadout.known", known.size()), listX(), contentY(), Palette.gold(), 1f);
		Ink.text(g, Component.translatable("screen.jjkcraft.loadout.slots"), slotX(), contentY(), Palette.gold(), 1f);

		Identifier hoveredId = null;
		int cols = columns();
		for (int i = 0; i < known.size(); i++) {
			Identifier id = known.get(i);
			int x = listX() + (i % cols) * (CELL + 4);
			int y = listY() + (i / cols) * (CELL + 4);
			cell(g, x, y, id, id.equals(dragging) ? 0.35f : 1f);
			if (inside(mouseX, mouseY, x, y)) {
				hoveredId = id;
			}
		}

		TechniqueState state = ClientTechniques.state();
		int usable = ClientTechniques.usableSlots();
		for (int s = 0; s < TechniqueState.SLOTS; s++) {
			int x = slotCellX(s);
			int y = slotY(s);
			boolean locked = s >= usable;
			boolean selected = s == state.selected();
			g.fill(x - 1, y - 1, x + CELL + 1, y + CELL + 1, locked ? 0xFF0A0A0D : selected ? Palette.gold() : Palette.INK_EDGE);
			g.fill(x, y, x + CELL, y + CELL, 0xFF141219);
			if (locked) {
				Ink.centered(g, Component.literal("x"), x + CELL / 2, y + 8, Palette.muted(), 1f);
			} else {
				Optional<Identifier> slotted = state.slot(s);
				slotted.ifPresent(id -> cell(g, x, y, id, 1f));
				if (slotted.isPresent() && inside(mouseX, mouseY, x, y)) {
					hoveredId = slotted.get();
				}
			}

			g.pose().pushMatrix();
			g.pose().translate(x, y + CELL + 2);
			g.pose().scale(0.6f, 0.6f);
			Ink.text(g, Component.translatable(locked ? "screen.jjkcraft.loadout.locked" : "screen.jjkcraft.loadout.slot", s + 1), 0, 0,
					locked ? Palette.muted() : Palette.TEXT_DIM, 1f);
			g.pose().popMatrix();
		}

		Ink.text(g, Component.translatable("screen.jjkcraft.loadout.hint"), listX(), panelY() + panelH() - 14, Palette.TEXT_DIM, 1f);

		if (dragging != null) {
			cell(g, (int) dragX - CELL / 2, (int) dragY - CELL / 2, dragging, 0.9f);
		} else if (hoveredId != null) {
			Identifier id = hoveredId;
			ClientTechniques.definition(id).ifPresent(def -> g.setComponentTooltipForNextFrame(Ink.font(), TechniqueTooltip.lines(id, def), mouseX, mouseY));
		}
	}

	private void cell(GuiGraphicsExtractor g, int x, int y, Identifier id, float alpha) {
		Optional<TechniqueDefinition> def = ClientTechniques.definition(id);
		if (def.isEmpty()) {
			return;
		}

		boolean allowed = ClientTechniques.gradeAllows(def.get());
		g.blit(RenderPipelines.GUI_TEXTURED, def.get().icon(), x, y, 0, 0, CELL, CELL, CELL, CELL, ARGB.white(alpha * (allowed ? 1f : 0.4f)));
		if (ClientTechniques.mastered(id)) {
			g.fill(x + CELL - 4, y + 1, x + CELL - 1, y + 4, Palette.gold());
		}

		if (ClientTechniques.state().innate().map(id::equals).orElse(false)) {
			g.fill(x + 1, y + 1, x + 4, y + 4, Palette.energyHigh());
		}
	}

	private static boolean inside(double mx, double my, int x, int y) {
		return mx >= x && mx < x + CELL && my >= y && my < y + CELL;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		double mx = event.x();
		double my = event.y();
		List<Identifier> known = ClientTechniques.known();
		int cols = columns();
		for (int i = 0; i < known.size(); i++) {
			int x = listX() + (i % cols) * (CELL + 4);
			int y = listY() + (i / cols) * (CELL + 4);
			if (inside(mx, my, x, y) && event.button() == 0) {
				dragging = known.get(i);
				dragX = mx;
				dragY = my;
				return true;
			}
		}

		int usable = ClientTechniques.usableSlots();
		for (int s = 0; s < Math.min(usable, TechniqueState.SLOTS); s++) {
			if (inside(mx, my, slotCellX(s), slotY(s))) {
				if (event.button() == 1) {
					ClientPlayNetworking.send(new SetLoadoutPayload(s, Optional.empty()));
				} else {
					ClientPlayNetworking.send(new SelectSlotPayload(s));
					ClientTechniques.state().slot(s).ifPresent(id -> {
						dragging = id;
						dragX = mx;
						dragY = my;
					});
				}

				return true;
			}
		}

		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		if (dragging != null) {
			dragX = event.x();
			dragY = event.y();
			return true;
		}

		return super.mouseDragged(event, dx, dy);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (dragging != null) {
			int usable = ClientTechniques.usableSlots();
			for (int s = 0; s < Math.min(usable, TechniqueState.SLOTS); s++) {
				if (inside(event.x(), event.y(), slotCellX(s), slotY(s))) {
					ClientPlayNetworking.send(new SetLoadoutPayload(s, Optional.of(dragging)));
					break;
				}
			}

			dragging = null;
			return true;
		}

		return super.mouseReleased(event);
	}
}
