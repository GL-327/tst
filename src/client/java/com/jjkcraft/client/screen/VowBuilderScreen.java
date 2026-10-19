package com.jjkcraft.client.screen;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import com.jjkcraft.client.state.ClientProgress;
import com.jjkcraft.client.state.ClientSorcery;
import com.jjkcraft.client.ui.Ink;
import com.jjkcraft.client.ui.InkButton;
import com.jjkcraft.client.ui.Palette;
import com.jjkcraft.core.vow.VowMath;
import com.jjkcraft.network.payload.VowPayloads;
import com.jjkcraft.vow.VowDefinition;
import com.jjkcraft.vow.VowState;
import com.jjkcraft.vow.Vows;

/**
 * Vow Builder: choose up to three restrictions, where the power goes and for how long, and see the
 * trade (the power gained, and the price of breaking it) before swearing. With a vow already held
 * it shows that vow, its time left and a Renounce button.
 */
public final class VowBuilderScreen extends JjkScreen {
	private static final int ROW_H = 16;

	private final List<Identifier> chosen = new ArrayList<>();
	private VowMath.Focus focus = VowMath.Focus.POWER;
	private VowMath.Term term = VowMath.Term.DAY;
	private InkButton swear;

	public VowBuilderScreen() {
		super(Component.translatable("screen.jjkcraft.vows"));
	}

	private List<Vows.Entry> entries() {
		return ClientProgress.registries().map(Vows::all).orElse(List.of());
	}

	private int listWidth() {
		return Math.min(170, panelW() / 2 - 8);
	}

	@Override
	protected void initContent() {
		swear = null;
		VowState held = ClientSorcery.vow();
		int x = panelX() + 8;
		int y = contentY();
		if (held.active()) {
			addRenderableWidget(new InkButton(panelX() + panelW() - 98, panelY() + panelH() - 22, 90, 14,
					Component.translatable("screen.jjkcraft.vows.renounce"), b -> ClientPlayNetworking.send(VowPayloads.Renounce.INSTANCE))
					.accent(Palette.danger()));
			return;
		}

		int listW = listWidth();
		for (Vows.Entry entry : entries()) {
			boolean on = chosen.contains(entry.id());
			Component label = Component.translatable(entry.definition().name()).append(" ").append(Component.literal("+" + fmt(entry.definition().weight())));
			addRenderableWidget(new InkButton(x, y, listW, ROW_H - 2, label, b -> toggle(entry.id()))
					.accent(entry.definition().restriction().breakable ? Palette.danger() : Palette.gold()).selected(on));
			y += ROW_H;
		}

		int rx = x + listW + 10;
		int rw = panelX() + panelW() - 8 - rx;
		int bw = (rw - 8) / 3;
		int fy = contentY() + 10;
		for (VowMath.Focus f : VowMath.Focus.values()) {
			addRenderableWidget(new InkButton(rx + f.ordinal() * (bw + 4), fy, bw, 14, Component.translatable("vow_focus.jjkcraft." + f.key()), b -> {
				focus = f;
				rebuildWidgets();
			}).selected(focus == f));
		}

		int ty = fy + 30;
		for (VowMath.Term t : VowMath.Term.values()) {
			addRenderableWidget(new InkButton(rx + t.ordinal() * (bw + 4), ty, bw, 14, Component.translatable("vow_term.jjkcraft." + t.key()), b -> {
				term = t;
				rebuildWidgets();
			}).selected(term == t));
		}

		swear = addRenderableWidget(new InkButton(panelX() + panelW() - 98, panelY() + panelH() - 22, 90, 14,
				Component.translatable("screen.jjkcraft.vows.swear"),
				b -> ClientPlayNetworking.send(new VowPayloads.Commit(List.copyOf(chosen), focus.ordinal(), term.ordinal()))).accent(Palette.gold()));
		swear.active = !chosen.isEmpty() && !ClientSorcery.inDebt();
	}

	private void toggle(Identifier id) {
		if (!chosen.remove(id) && chosen.size() < VowMath.MAX_RESTRICTIONS) {
			chosen.add(id);
		}

		rebuildWidgets();
	}

	@Override
	public void tick() {
		// The vow can change underneath the screen (sworn, broken, fulfilled): rebuild when it does.
		boolean active = ClientSorcery.vow().active();
		if (active != (swear == null)) {
			rebuildWidgets();
		}
	}

	@Override
	protected void extractContent(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
		VowState held = ClientSorcery.vow();
		if (held.active()) {
			drawHeld(g, held);
			return;
		}

		int rx = panelX() + 8 + listWidth() + 10;
		int rw = panelX() + panelW() - 8 - rx;
		int y = contentY();
		Ink.text(g, Component.translatable("screen.jjkcraft.vows.focus"), rx, y, Palette.TEXT_DIM, 1f);
		Ink.text(g, Component.translatable("screen.jjkcraft.vows.term"), rx, y + 30, Palette.TEXT_DIM, 1f);

		double weight = ClientProgress.registries().map(access -> Vows.totalWeight(access, chosen)).orElse(0.0);
		double points = VowMath.points(weight, term);
		int py = y + 62;
		Ink.text(g, Component.translatable("screen.jjkcraft.vows.points", fmt(points), chosen.size(), VowMath.MAX_RESTRICTIONS), rx, py, Palette.gold(), 1f);
		py += 12;
		for (Component line : gains(focus, points)) {
			Ink.text(g, line, rx, py, Palette.good(), 1f);
			py += 10;
		}

		py += 4;
		if (!chosen.isEmpty()) {
			Component price = Component.translatable("screen.jjkcraft.vows.price",
					(int) Math.round(VowMath.breakDrain(points, false) * 100), (int) Math.round(VowMath.debtTicks(points, false) / 1200.0));
			py = g.textWithWordWrap(Ink.font(), price, rx, py, rw, Palette.danger(), false) + 4;
		}

		for (Identifier id : chosen) {
			VowDefinition def = ClientProgress.registries().flatMap(access -> Vows.get(access, id)).orElse(null);
			if (def != null && py < panelY() + panelH() - 34) {
				py = g.textWithWordWrap(Ink.font(), Component.translatable(def.description()), rx, py, rw, Palette.TEXT_DIM, false) + 2;
			}
		}

		if (ClientSorcery.inDebt()) {
			Ink.text(g, Component.translatable("screen.jjkcraft.vows.debt"), panelX() + 8, panelY() + panelH() - 18, Palette.danger(), 1f);
		} else if (chosen.isEmpty()) {
			Ink.text(g, Component.translatable("screen.jjkcraft.vows.choose"), panelX() + 8, panelY() + panelH() - 18, Palette.TEXT_DIM, 1f);
		}
	}

	private void drawHeld(GuiGraphicsExtractor g, VowState held) {
		int x = panelX() + 8;
		int y = contentY();
		int w = panelW() - 16;
		Ink.text(g, Component.translatable("screen.jjkcraft.vows.held", fmt(held.points())), x, y, Palette.gold(), 1f);
		y += 12;
		for (Identifier id : held.vows()) {
			VowDefinition def = ClientProgress.registries().flatMap(access -> Vows.get(access, id)).orElse(null);
			if (def != null) {
				Ink.text(g, Component.translatable(def.name()), x, y, def.restriction().breakable ? Palette.danger() : Palette.gold(), 1f);
				y = g.textWithWordWrap(Ink.font(), Component.translatable(def.description()), x + 8, y + 10, w - 8, Palette.TEXT_DIM, false) + 4;
			}
		}

		for (Component line : gains(held.focus(), held.points())) {
			Ink.text(g, line, x, y, Palette.good(), 1f);
			y += 10;
		}

		long now = minecraft != null && minecraft.level != null ? minecraft.level.getGameTime() : 0;
		Component left = held.endsAt() < 0 ? Component.translatable("screen.jjkcraft.vows.binding")
				: Component.translatable("screen.jjkcraft.vows.left", (int) Math.max(0, (held.endsAt() - now) / 1200));
		Ink.text(g, left, x, y + 4, Palette.TEXT_DIM, 1f);
		Ink.text(g, Component.translatable("screen.jjkcraft.vows.renounce_hint",
				(int) Math.round(VowMath.breakDrain(held.points(), true) * 100)), x, panelY() + panelH() - 18, Palette.TEXT_DIM, 1f);
	}

	private static List<Component> gains(VowMath.Focus focus, double points) {
		List<Component> out = new ArrayList<>();
		if (points <= 0) {
			return out;
		}

		switch (focus) {
			case POWER -> {
				out.add(Component.translatable("screen.jjkcraft.vows.gain.damage", pct(VowMath.damageMultiplier(focus, points) - 1)));
				out.add(Component.translatable("screen.jjkcraft.vows.gain.melee", pct(VowMath.meleeMultiplier(focus, points) - 1)));
			}
			case EFFICIENCY -> out.add(Component.translatable("screen.jjkcraft.vows.gain.cost", pct(1 - VowMath.costMultiplier(focus, points))));
			case RESERVE -> {
				out.add(Component.translatable("screen.jjkcraft.vows.gain.capacity", pct(VowMath.capacityMultiplier(focus, points) - 1)));
				out.add(Component.translatable("screen.jjkcraft.vows.gain.regen", pct(VowMath.regenMultiplier(focus, points) - 1)));
			}
		}

		return out;
	}

	private static String pct(double fraction) {
		return String.valueOf(Math.round(fraction * 100));
	}

	private static String fmt(double value) {
		return String.format(java.util.Locale.ROOT, "%.1f", value);
	}
}
