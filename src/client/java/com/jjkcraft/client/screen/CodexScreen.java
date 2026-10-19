package com.jjkcraft.client.screen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import com.jjkcraft.client.state.ClientProgress;
import com.jjkcraft.client.state.ClientTechniques;
import com.jjkcraft.client.ui.Ink;
import com.jjkcraft.client.ui.InkButton;
import com.jjkcraft.client.ui.Palette;
import com.jjkcraft.curse.CurseDefinition;
import com.jjkcraft.curse.CurseEntity;
import com.jjkcraft.curse.Curses;
import com.jjkcraft.grade.GradeDefinition;
import com.jjkcraft.grade.Grades;
import com.jjkcraft.item.JjkItems;

/**
 * The curse codex. Every curse in the registry is listed by grade. A curse you have never defeated
 * shows only its grade; after the first kill you see its name and description; after its
 * {@code unlock_kills} you get the model, lore, techniques, weaknesses and trophy.
 */
public final class CodexScreen extends JjkScreen {
	private static final int ROW_H = 15;
	private static final Map<Identifier, CurseEntity> PREVIEWS = new HashMap<>();
	/** Ids for preview entities, far below anything the world hands out. */
	private static final int PREVIEW_ID_BASE = -1_000_000;

	private static Identifier lastSelected;
	private final List<Curses.Entry> entries = new ArrayList<>();
	private int scroll;
	private Identifier selected;

	public CodexScreen() {
		super(Component.translatable("screen.jjkcraft.codex"));
	}

	@Override
	protected void initContent() {
		entries.clear();
		ClientProgress.registries().ifPresent(access -> entries.addAll(Curses.all(access)));
		if (selected == null) {
			selected = lastSelected != null && entries.stream().anyMatch(e -> e.id().equals(lastSelected)) ? lastSelected
					: entries.isEmpty() ? null : entries.getFirst().id();
		}

		int listX = panelX() + 6;
		int listW = listColumnWidth();
		int rows = visibleRows();
		scroll = Math.max(0, Math.min(scroll, entries.size() - rows));
		for (int i = 0; i < rows && scroll + i < entries.size(); i++) {
			Curses.Entry entry = entries.get(scroll + i);
			Component label = kills(entry.id()) > 0 ? Component.translatable(entry.definition().name())
					: Component.translatable("screen.jjkcraft.codex.unknown");
			int accent = gradeColor(entry.definition());
			addRenderableWidget(new InkButton(listX, contentY() + i * ROW_H, listW, ROW_H - 2, label, b -> select(entry.id()))
					.accent(accent).selected(entry.id().equals(selected)));
		}

		if (entries.size() > rows) {
			int y = contentY() + rows * ROW_H;
			addRenderableWidget(new InkButton(listX, y, listW / 2 - 1, 12, Component.literal("▲"), b -> scrollBy(-rows)));
			addRenderableWidget(new InkButton(listX + listW / 2 + 1, y, listW / 2 - 1, 12, Component.literal("▼"), b -> scrollBy(rows)));
		}
	}

	private int listColumnWidth() {
		return Math.min(120, panelW() / 3);
	}

	private int visibleRows() {
		return Math.max(3, (panelH() - 24 - 22) / ROW_H);
	}

	private void select(Identifier id) {
		selected = id;
		lastSelected = id;
		rebuildWidgets();
	}

	private void scrollBy(int delta) {
		scroll = Math.max(0, Math.min(scroll + delta, Math.max(0, entries.size() - visibleRows())));
		rebuildWidgets();
	}

	@Override
	public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
		if (x < panelX() + 6 + listColumnWidth() && entries.size() > visibleRows()) {
			scrollBy(scrollY > 0 ? -1 : 1);
			return true;
		}

		return super.mouseScrolled(x, y, scrollX, scrollY);
	}

	private static int kills(Identifier id) {
		return ClientProgress.hasData() ? ClientProgress.get().counter(Curses.codexKey(id)) : 0;
	}

	private static int gradeColor(CurseDefinition definition) {
		return ClientProgress.registries().flatMap(access -> Grades.get(access, definition.grade())).map(GradeDefinition::color).orElse(0xFF9A93A8);
	}

	@Override
	protected void extractContent(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
		int x = panelX() + 12 + listColumnWidth();
		int y = contentY();
		int w = panelX() + panelW() - 8 - x;
		int h = panelY() + panelH() - 8 - y;

		Curses.Entry entry = entries.stream().filter(e -> e.id().equals(selected)).findFirst().orElse(null);
		if (entry == null) {
			Ink.centered(g, Component.translatable("screen.jjkcraft.codex.empty"), x + w / 2, y + h / 2, Palette.TEXT_DIM, 1f);
			return;
		}

		CurseDefinition def = entry.definition();
		int kills = kills(entry.id());
		boolean seen = kills > 0;
		boolean unlocked = kills >= def.codex().unlockKills();
		int accent = ARGB.opaque(def.codex().color());
		int gradeColor = gradeColor(def);

		// Preview frame on the left of the detail area.
		int frameW = Math.min(96, w / 2 - 4);
		g.fill(x, y, x + frameW, y + h, 0xC0080810);
		g.fill(x, y, x + frameW, y + 1, accent);
		if (unlocked) {
			CurseEntity preview = preview(entry);
			if (preview != null) {
				float height = Math.max(1f, preview.getBbHeight() * def.model().scale());
				int size = (int) Math.max(12, Math.min(48, (h - 24) / height * 0.8f));
				InventoryScreen.extractEntityInInventoryFollowsMouse(g, x + 2, y + 2, x + frameW - 2, y + h - 2, size, 0.0625f, mouseX, mouseY, preview);
			}
		} else {
			Ink.diamond(g, x + frameW / 2, y + h / 2 - 6, 14, ARGB.multiplyAlpha(gradeColor, 0.35f));
			Ink.centered(g, Component.literal("?"), x + frameW / 2, y + h / 2 - 10, Palette.TEXT, 1f);
			int left = def.codex().unlockKills() - kills;
			Ink.centered(g, Component.translatable("screen.jjkcraft.codex.locked", left), x + frameW / 2, y + h - 14, Palette.TEXT_DIM, 1f);
		}

		int tx = x + frameW + 8;
		int tw = x + w - tx;
		int ty = y;
		Component name = seen ? Component.translatable(def.name()) : Component.translatable("screen.jjkcraft.codex.unknown");
		Ink.text(g, name, tx, ty, accent, 1f);
		ty += 11;
		Component grade = ClientProgress.registries().flatMap(access -> Grades.get(access, def.grade()))
				.map(gd -> (Component) Component.translatable(gd.name())).orElse(Component.literal(def.grade().toString()));
		Ink.text(g, grade, tx, ty, gradeColor, 1f);
		Component killsLabel = Component.translatable("screen.jjkcraft.codex.kills", kills);
		Ink.text(g, killsLabel, tx + tw - Ink.font().width(killsLabel), ty, Palette.TEXT_DIM, 1f);
		ty += 14;

		if (!seen) {
			g.textWithWordWrap(Ink.font(), Component.translatable("screen.jjkcraft.codex.unseen"), tx, ty, tw, Palette.TEXT_DIM, false);
			return;
		}

		ty = g.textWithWordWrap(Ink.font(), Component.translatable(def.description()), tx, ty, tw, Palette.TEXT, false) + 4;
		if (!unlocked) {
			return;
		}

		ty = g.textWithWordWrap(Ink.font(), Component.translatable(def.lore()), tx, ty, tw, Palette.TEXT_DIM, false) + 4;

		List<Component> weak = new ArrayList<>();
		List<Component> resist = new ArrayList<>();
		List<Component> immune = new ArrayList<>();
		for (Entry<Identifier, Float> r : def.resistances().entrySet()) {
			Component label = Component.translatableWithFallback("codex.jjkcraft.damage." + r.getKey().getPath(), r.getKey().getPath().replace('_', ' '));
			if (r.getValue() > 1) {
				weak.add(label);
			} else if (r.getValue() <= 0) {
				immune.add(label);
			} else if (r.getValue() < 1) {
				resist.add(label);
			}
		}

		if (!weak.isEmpty()) {
			ty = line(g, Component.translatable("screen.jjkcraft.codex.weak"), join(weak), tx, ty, tw, Palette.good());
		}

		if (!resist.isEmpty()) {
			ty = line(g, Component.translatable("screen.jjkcraft.codex.resists"), join(resist), tx, ty, tw, Palette.danger());
		}

		if (!immune.isEmpty()) {
			ty = line(g, Component.translatable("screen.jjkcraft.codex.immune"), join(immune), tx, ty, tw, Palette.muted());
		}

		if (!def.techniques().isEmpty()) {
			List<Component> names = def.techniques().stream()
					.map(id -> ClientTechniques.definition(id).map(t -> (Component) Component.translatable(t.name())).orElse(Component.literal(id.getPath())))
					.toList();
			ty = line(g, Component.translatable("screen.jjkcraft.codex.techniques"), join(names), tx, ty, tw, Palette.energy());
		}

		Item trophy = JjkItems.TROPHIES.get(entry.id().getPath());
		if (trophy != null && ty + 18 < y + h) {
			g.item(new ItemStack(trophy), tx, ty);
			Ink.text(g, Component.translatable(trophy.getDescriptionId()), tx + 20, ty + 4, Palette.gold(), 1f);
		}
	}

	private static int line(GuiGraphicsExtractor g, Component label, Component value, int x, int y, int width, int color) {
		Ink.text(g, label, x, y, color, 1f);
		return g.textWithWordWrap(Ink.font(), value, x + Ink.font().width(label) + 4, y, Math.max(20, width - Ink.font().width(label) - 4),
				Palette.TEXT, false) + 2;
	}

	private static Component join(List<Component> parts) {
		var out = Component.empty();
		for (int i = 0; i < parts.size(); i++) {
			if (i > 0) {
				out.append(", ");
			}

			out.append(parts.get(i));
		}

		return out;
	}

	/** A client-only entity to draw in the preview frame, one per curse, reused between openings. */
	private CurseEntity preview(Curses.Entry entry) {
		if (minecraft == null || minecraft.level == null) {
			return null;
		}

		CurseEntity cached = PREVIEWS.get(entry.id());
		if (cached != null && cached.level() == minecraft.level) {
			// Not in the world, so never ticked: drive the idle animation from the clock.
			cached.tickCount = (int) (System.currentTimeMillis() / 50 % 1_000_000);
			return cached;
		}

		EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(entry.definition().entity());
		Entity entity = type == null ? null : type.create(minecraft.level, EntitySpawnReason.LOAD);
		if (entity instanceof CurseEntity curse) {
			// Never added to the world, so give it an id of its own (renderers read it).
			curse.setId(PREVIEW_ID_BASE - PREVIEWS.size());
			PREVIEWS.put(entry.id(), curse);
			return curse;
		}

		return null;
	}
}
