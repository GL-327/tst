package com.jjkcraft.client.screen;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import com.jjkcraft.client.state.ClientCulling;
import com.jjkcraft.client.ui.Ink;
import com.jjkcraft.client.ui.InkButton;
import com.jjkcraft.client.ui.Palette;
import com.jjkcraft.culling.CullingState;
import com.jjkcraft.culling.rule.CullingRule;
import com.jjkcraft.culling.rule.RuleEvent;
import com.jjkcraft.network.payload.CullingPayloads;

/**
 * The Culling Game: a map of the colonies, the scoreboard (with the actions the rules allow), the
 * rules in force, and negotiation with the Kogane. Every request goes to the server, which
 * checks it against the rules; this screen only offers what the rules seem to allow.
 */
public class CullingScreen extends JjkScreen {
	public enum Page {
		MAP, BOARD, RULES, NEGOTIATE
	}

	private static Page page = Page.MAP;
	private static final int ROW = 12;

	private UUID selected;
	private Identifier proposal;
	private int amount = 5;
	private int targetColony = -1;
	private int scroll;

	public CullingScreen() {
		super(Component.translatable("screen.jjkcraft.culling"));
	}

	public static CullingScreen at(Page start) {
		page = start;
		return new CullingScreen();
	}

	@Override
	protected void initContent() {
		int x = panelX() + 8;
		int y = contentY();
		for (Page p : Page.values()) {
			addRenderableWidget(new InkButton(x, y, 74, 13, Component.translatable("screen.jjkcraft.culling." + p.name().toLowerCase(Locale.ROOT)), b -> {
				page = p;
				scroll = 0;
				rebuildWidgets();
			}).selected(page == p).accent(ClientCulling.ACCENT));
			x += 77;
		}

		CullingPayloads.Me me = ClientCulling.me();
		int bottom = panelY() + panelH() - 20;
		int rx = panelX() + panelW() - 150;
		if (page == Page.BOARD && me.participant()) {
			boardActions(rx, contentY() + 70);
		} else if (page == Page.NEGOTIATE && me.participant()) {
			addRenderableWidget(new InkButton(panelX() + panelW() - 98, bottom, 90, 14, Component.translatable("screen.jjkcraft.culling.propose"),
					b -> {
						if (proposal != null) {
							send(RuleEvent.Action.NEGOTIATE, null, 0, proposal, -1);
						}
					}).accent(ClientCulling.ACCENT));
		}
	}

	private void boardActions(int x, int y) {
		if (ClientCulling.allowed(RuleEvent.Action.REVEAL)) {
			addRenderableWidget(new InkButton(x, y, 142, 14, Component.translatable("screen.jjkcraft.culling.reveal"),
					b -> send(RuleEvent.Action.REVEAL, selected, 0, null, -1)));
			y += 18;
		}

		if (ClientCulling.allowed(RuleEvent.Action.TRANSFER)) {
			addRenderableWidget(new InkButton(x, y, 18, 14, Component.literal("-"), b -> amount = Math.max(5, amount - 5)));
			addRenderableWidget(new InkButton(x + 20, y, 18, 14, Component.literal("+"), b -> amount = Math.min(1000, amount + 5)));
			addRenderableWidget(new InkButton(x + 42, y, 100, 14, Component.translatable("screen.jjkcraft.culling.give"),
					b -> send(RuleEvent.Action.TRANSFER, selected, amount, null, -1)));
			y += 18;
		}

		if (ClientCulling.allowed(RuleEvent.Action.SUBSTITUTE) && ClientCulling.view().colonies().size() > 1) {
			addRenderableWidget(new InkButton(x, y, 60, 14, Component.translatable("screen.jjkcraft.culling.next_colony"), b -> targetColony = nextColony()));
			addRenderableWidget(new InkButton(x + 62, y, 80, 14, Component.translatable("screen.jjkcraft.culling.move"),
					b -> send(RuleEvent.Action.SUBSTITUTE, null, 0, null, targetColony)));
			y += 18;
		}

		if (ClientCulling.allowed(RuleEvent.Action.WITHDRAW)) {
			addRenderableWidget(new InkButton(x, y, 142, 14, Component.translatable("screen.jjkcraft.culling.withdraw"),
					b -> send(RuleEvent.Action.WITHDRAW, null, 0, null, -1)).accent(Palette.danger()));
		}
	}

	private int nextColony() {
		List<CullingPayloads.ColonyView> colonies = ClientCulling.view().colonies();
		int mine = ClientCulling.me().colony();
		int start = 0;
		for (int i = 0; i < colonies.size(); i++) {
			if (colonies.get(i).id() == targetColony) {
				start = i + 1;
			}
		}

		for (int i = 0; i < colonies.size(); i++) {
			CullingPayloads.ColonyView colony = colonies.get((start + i) % colonies.size());
			if (colony.id() != mine) {
				return colony.id();
			}
		}

		return -1;
	}

	private static void send(RuleEvent.Action action, UUID target, int amount, Identifier rule, int colony) {
		ClientPlayNetworking.send(new CullingPayloads.Action(action.getSerializedName(), Optional.ofNullable(target), amount, Optional.ofNullable(rule),
				colony));
	}

	// ---------------------------------------------------------------- drawing

	@Override
	protected void extractContent(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
		CullingPayloads.View view = ClientCulling.view();
		int x = panelX() + 8;
		int y = contentY() + 18;
		Ink.text(g, header(view), x, y, ClientCulling.ACCENT, 1f);
		int top = y + 13;
		int bottom = panelY() + panelH() - 6;
		if (!view.running() && view.colonies().isEmpty()) {
			if (view.ended()) {
				results(g, view, x, top);
			} else {
				g.textWithWordWrap(Ink.font(), Component.translatable("screen.jjkcraft.culling.not_started"), x, top, panelW() - 16, Palette.TEXT_DIM, false);
			}

			return;
		}

		switch (page) {
			case MAP -> map(g, view, x, top, bottom, mouseX, mouseY);
			case BOARD -> board(g, view, x, top, bottom);
			case RULES -> rules(g, view, x, top, bottom);
			case NEGOTIATE -> negotiate(g, view, x, top, bottom, mouseX, mouseY);
		}
	}

	private static Component header(CullingPayloads.View view) {
		if (!view.running()) {
			return Component.translatable(view.ended() ? "screen.jjkcraft.culling.over" : "screen.jjkcraft.culling.idle");
		}

		CullingPayloads.Me me = view.me();
		if (me.participant()) {
			return Component.translatable("screen.jjkcraft.culling.header", view.day() + 1, ClientCulling.colonyName(me.colony()), me.points());
		}

		int left = ClientCulling.daysLeft();
		return left >= 0 ? Component.translatable("screen.jjkcraft.culling.header_outside", view.day() + 1, left)
				: Component.translatable("screen.jjkcraft.culling.header_watching", view.day() + 1,
						Component.translatable("screen.jjkcraft.culling.status." + me.status()));
	}

	private void results(GuiGraphicsExtractor g, CullingPayloads.View view, int x, int y) {
		Ink.text(g, Component.translatable("screen.jjkcraft.culling.results"), x, y, Palette.gold(), 1f);
		y += 12;
		int place = 1;
		for (CullingState.Standing standing : view.results()) {
			Ink.text(g, Component.literal(place + ". " + standing.name() + "  " + standing.points()), x, y, place == 1 ? Palette.gold() : Palette.TEXT, 1f);
			y += 10;
			place++;
		}
	}

	// ---- map

	private void map(GuiGraphicsExtractor g, CullingPayloads.View view, int x, int top, int bottom, int mouseX, int mouseY) {
		int size = Math.min(bottom - top, panelW() / 2 - 12);
		g.fill(x, top, x + size, top + size, 0xC0060609);
		g.outline(x, top, size, size, Palette.INK_EDGE);
		for (int i = 1; i < 4; i++) {
			g.fill(x + size * i / 4, top + 1, x + size * i / 4 + 1, top + size - 1, 0x18FFFFFF);
			g.fill(x + 1, top + size * i / 4, x + size - 1, top + size * i / 4 + 1, 0x18FFFFFF);
		}

		// Fit every colony and the viewer into the square.
		double minX = Double.MAX_VALUE;
		double maxX = -Double.MAX_VALUE;
		double minZ = Double.MAX_VALUE;
		double maxZ = -Double.MAX_VALUE;
		for (CullingPayloads.ColonyView colony : view.colonies()) {
			minX = Math.min(minX, colony.center().getX() - colony.radius());
			maxX = Math.max(maxX, colony.center().getX() + colony.radius());
			minZ = Math.min(minZ, colony.center().getZ() - colony.radius());
			maxZ = Math.max(maxZ, colony.center().getZ() + colony.radius());
		}

		if (minecraft.player != null) {
			minX = Math.min(minX, minecraft.player.getX() - 8);
			maxX = Math.max(maxX, minecraft.player.getX() + 8);
			minZ = Math.min(minZ, minecraft.player.getZ() - 8);
			maxZ = Math.max(maxZ, minecraft.player.getZ() + 8);
		}

		double span = Math.max(32, Math.max(maxX - minX, maxZ - minZ)) * 1.1;
		double midX = (minX + maxX) / 2;
		double midZ = (minZ + maxZ) / 2;
		double scale = size / span;
		int mine = view.me().colony();
		CullingPayloads.ColonyView hovered = null;
		for (CullingPayloads.ColonyView colony : view.colonies()) {
			int cx = x + size / 2 + (int) Math.round((colony.center().getX() - midX) * scale);
			int cy = top + size / 2 + (int) Math.round((colony.center().getZ() - midZ) * scale);
			int r = Math.max(4, (int) Math.round(colony.radius() * scale));
			boolean own = colony.id() == mine;
			int color = own ? ClientCulling.ACCENT : 0xFF8F6CC0;
			boolean hot = (mouseX - cx) * (mouseX - cx) + (mouseY - cy) * (mouseY - cy) <= r * r;
			if (hot) {
				hovered = colony;
			}

			Ink.disc(g, cx, cy, r, ARGB.color(hot ? 70 : 40, color & 0xFFFFFF));
			Ink.ring(g, cx, cy, r, 1, 1f, ARGB.color(hot ? 255 : 190, color & 0xFFFFFF));
			Component name = Component.translatableWithFallback(colony.name(), colony.name());
			g.pose().pushMatrix();
			g.pose().translate(cx - Ink.font().width(name) * 0.35f, cy - 3);
			g.pose().scale(0.7f, 0.7f);
			Ink.text(g, name, 0, 0, own ? ClientCulling.ACCENT : Palette.TEXT, 1f);
			g.pose().popMatrix();
		}

		for (CullingPayloads.Entry entry : view.board()) {
			entry.pos().ifPresent(pos -> {
				int px = x + size / 2 + (int) Math.round((pos.getX() - midX) * scale);
				int py = top + size / 2 + (int) Math.round((pos.getZ() - midZ) * scale);
				Ink.diamond(g, px, py, 2, entry.leader() ? Palette.gold() : Palette.danger());
			});
		}

		if (minecraft.player != null) {
			int px = x + size / 2 + (int) Math.round((minecraft.player.getX() - midX) * scale);
			int py = top + size / 2 + (int) Math.round((minecraft.player.getZ() - midZ) * scale);
			Ink.diamond(g, Mth.clamp(px, x + 3, x + size - 3), Mth.clamp(py, top + 3, top + size - 3), 3, Palette.TEXT);
		}

		// Right: colonies, then the log.
		int rx = x + size + 10;
		int rw = panelX() + panelW() - 8 - rx;
		int y = top;
		Ink.text(g, Component.translatable("screen.jjkcraft.culling.colonies"), rx, y, Palette.gold(), 1f);
		y += 11;
		for (CullingPayloads.ColonyView colony : view.colonies()) {
			Component line = Component.translatableWithFallback(colony.name(), colony.name()).copy()
					.append(Component.translatable("screen.jjkcraft.culling.colony_line", colony.participants(), colony.points()));
			Ink.text(g, Ink.fit(line, rw), rx, y, colony.id() == mine ? ClientCulling.ACCENT : Palette.TEXT, 1f);
			y += 10;
		}

		y += 6;
		Ink.text(g, Component.translatable("screen.jjkcraft.culling.log"), rx, y, Palette.gold(), 1f);
		y += 11;
		List<CullingState.LogLine> log = view.log();
		for (int i = log.size() - 1; i >= 0 && y < bottom - 8; i--) {
			CullingState.LogLine line = log.get(i);
			Object[] args = line.args().stream().map(s -> (Object) Component.translatableWithFallback(s, s)).toArray();
			g.pose().pushMatrix();
			g.pose().translate(rx, y);
			g.pose().scale(0.85f, 0.85f);
			Ink.text(g, Ink.fit(Component.translatable(line.key(), args), (int) (rw / 0.85f)), 0, 0, Palette.TEXT_DIM, 1f);
			g.pose().popMatrix();
			y += 9;
		}

		if (hovered != null) {
			List<Component> tip = new ArrayList<>();
			tip.add(Component.translatableWithFallback(hovered.name(), hovered.name()));
			tip.add(Component.translatable("screen.jjkcraft.culling.tip_people", hovered.participants()));
			tip.add(Component.translatable("screen.jjkcraft.culling.tip_points", hovered.points()));
			BlockPos c = hovered.center();
			tip.add(Component.translatable("screen.jjkcraft.culling.tip_where", c.getX(), c.getZ(), hovered.radius()));
			g.setComponentTooltipForNextFrame(Ink.font(), tip, mouseX, mouseY);
		}
	}

	// ---- scoreboard

	private void board(GuiGraphicsExtractor g, CullingPayloads.View view, int x, int top, int bottom) {
		int w = panelW() - 170;
		Ink.text(g, Component.translatable("screen.jjkcraft.culling.col_name"), x + 18, top, Palette.gold(), 1f);
		Ink.text(g, Component.translatable("screen.jjkcraft.culling.col_colony"), x + w / 2, top, Palette.gold(), 1f);
		Component pointsHead = Component.translatable("screen.jjkcraft.culling.col_points");
		Ink.text(g, pointsHead, x + w - 26 - Ink.font().width(pointsHead), top, Palette.gold(), 1f);
		int y = top + 12;
		List<CullingPayloads.Entry> board = view.board();
		int visible = Math.max(1, (bottom - y) / ROW);
		scroll = Mth.clamp(scroll, 0, Math.max(0, board.size() - visible));
		for (int i = scroll; i < Math.min(board.size(), scroll + visible); i++) {
			CullingPayloads.Entry entry = board.get(i);
			boolean picked = entry.id().equals(selected);
			if (picked) {
				g.fill(x - 2, y - 2, x + w, y + ROW - 2, 0x40D4A35A);
			}

			Component name = entry.known() ? Component.literal(entry.name()) : Component.translatable("screen.jjkcraft.culling.unknown");
			int color = entry.you() ? ClientCulling.ACCENT : entry.known() ? Palette.TEXT : Palette.muted();
			Ink.text(g, Component.literal((i + 1) + "."), x, y, Palette.TEXT_DIM, 1f);
			Ink.text(g, Ink.fit(name, w / 2 - 20), x + 18, y, color, 1f);
			Component colony = entry.colony() >= 0 ? ClientCulling.colonyName(entry.colony()) : Component.literal("?");
			Ink.text(g, Ink.fit(colony, w / 3 - 8), x + w / 2, y, Palette.TEXT_DIM, 1f);
			Component points = Component.literal(String.valueOf(entry.points()));
			Ink.text(g, points, x + w - 26 - Ink.font().width(points), y, entry.leader() ? Palette.gold() : Palette.TEXT, 1f);
			Ink.text(g, Component.translatable(entry.sorcerer() ? "screen.jjkcraft.culling.sorcerer_tag" : "screen.jjkcraft.culling.ordinary_tag"),
					x + w - 20, y, entry.sorcerer() ? 0xFF8F6CC0 : Palette.TEXT_DIM, 1f);
			y += ROW;
		}

		// Right: the selected participant and what the rules allow.
		int rx = panelX() + panelW() - 150;
		int ry = top;
		CullingPayloads.Entry chosen = board.stream().filter(e -> e.id().equals(selected)).findFirst().orElse(null);
		Ink.text(g, chosen == null ? Component.translatable("screen.jjkcraft.culling.pick")
				: chosen.known() ? Component.literal(chosen.name()) : Component.translatable("screen.jjkcraft.culling.unknown"), rx, ry, Palette.gold(), 1f);
		if (ClientCulling.allowed(RuleEvent.Action.TRANSFER)) {
			Ink.text(g, Component.translatable("screen.jjkcraft.culling.amount", amount), rx, ry + 12, Palette.TEXT_DIM, 1f);
		}

		if (ClientCulling.allowed(RuleEvent.Action.SUBSTITUTE) && targetColony >= 0) {
			Ink.text(g, Ink.fit(Component.translatable("screen.jjkcraft.culling.to_colony", ClientCulling.colonyName(targetColony)), 146), rx, ry + 24,
					Palette.TEXT_DIM, 1f);
		}

		if (!view.me().participant()) {
			g.textWithWordWrap(Ink.font(), Component.translatable("screen.jjkcraft.culling.only_participants"), rx, ry + 40, 142, Palette.TEXT_DIM, false);
		}
	}

	// ---- rules

	private void rules(GuiGraphicsExtractor g, CullingPayloads.View view, int x, int top, int bottom) {
		int w = panelW() - 16;
		int y = top - scroll * 10;
		int n = 1;
		long now = minecraft.level == null ? 0 : minecraft.level.getGameTime();
		for (CullingPayloads.RuleView active : view.rules()) {
			CullingRule rule = ClientCulling.rule(active.id()).orElse(null);
			Component text = rule == null ? Component.literal(active.id().toString()) : ClientCulling.ruleText(rule);
			int after = y;
			if (y >= top - 2 && y < bottom - 20) {
				Ink.text(g, Component.literal(n + "."), x, y, ClientCulling.ACCENT, 1f);
				after = g.textWithWordWrap(Ink.font(), text, x + 16, y, w - 16, Palette.TEXT, false);
			} else {
				after = y + Ink.font().split(text, w - 16).size() * 9;
			}

			boolean lapses = active.expiresAt() != CullingState.ActiveRule.NEVER;
			if (active.addedBy().isPresent() || lapses) {
				Component meta = lapses
						? Component.translatable("screen.jjkcraft.culling.rule_meta_lapse", active.addedBy().orElse("?"),
								Math.max(1, (int) Math.ceil((active.expiresAt() - now) / 24000.0)))
						: Component.translatable("screen.jjkcraft.culling.rule_meta", active.addedBy().orElse("?"));
				if (after >= top && after < bottom - 8) {
					Ink.text(g, meta, x + 16, after, Palette.TEXT_DIM, 1f);
				}

				after += 10;
			}

			y = after + 4;
			n++;
		}
	}

	// ---- negotiation

	private void negotiate(GuiGraphicsExtractor g, CullingPayloads.View view, int x, int top, int bottom, int mouseX, int mouseY) {
		CullingPayloads.Me me = view.me();
		int listW = 150;
		int y = top;
		List<ClientCulling.RuleEntry> offers = new ArrayList<>();
		for (ClientCulling.RuleEntry entry : ClientCulling.allRules()) {
			if (!entry.rule().base()) {
				offers.add(entry);
			}
		}

		for (ClientCulling.RuleEntry entry : offers) {
			boolean active = ClientCulling.ruleActive(entry.id());
			boolean picked = entry.id().equals(proposal);
			if (picked) {
				g.fill(x - 2, y - 2, x + listW, y + ROW - 2, 0x40D4A35A);
			}

			Component name = Component.translatableWithFallback(entry.rule().text() + ".name", entry.id().getPath());
			Ink.text(g, Ink.fit(name, listW - 30), x, y, active ? Palette.muted() : Palette.TEXT, 1f);
			Component cost = Component.literal(String.valueOf(ClientCulling.cost(entry.rule())));
			Ink.text(g, cost, x + listW - 4 - Ink.font().width(cost), y, me.points() >= ClientCulling.cost(entry.rule()) ? Palette.gold() : Palette.muted(), 1f);
			y += ROW;
		}

		int rx = x + listW + 10;
		int rw = panelX() + panelW() - 8 - rx;
		int ry = top;
		if (!me.participant()) {
			g.textWithWordWrap(Ink.font(), Component.translatable("screen.jjkcraft.culling.only_participants"), rx, ry, rw, Palette.TEXT_DIM, false);
			return;
		}

		if (!me.nearMaster()) {
			ry = g.textWithWordWrap(Ink.font(), Component.translatable("screen.jjkcraft.culling.go_to_master"), rx, ry, rw, Palette.danger(), false) + 6;
		}

		CullingRule rule = proposal == null ? null : ClientCulling.rule(proposal).orElse(null);
		if (rule == null) {
			g.textWithWordWrap(Ink.font(), Component.translatable("screen.jjkcraft.culling.pick_rule"), rx, ry, rw, Palette.TEXT_DIM, false);
			return;
		}

		ry = g.textWithWordWrap(Ink.font(), ClientCulling.ruleText(rule), rx, ry, rw, Palette.TEXT, false) + 6;
		if (rule.durationDays() > 0) {
			Ink.text(g, Component.translatable("screen.jjkcraft.culling.lasts", rule.durationDays()), rx, ry, Palette.TEXT_DIM, 1f);
			ry += 11;
		}

		int cost = ClientCulling.cost(rule);
		Component verdict;
		int color;
		if (ClientCulling.ruleActive(proposal)) {
			verdict = Component.translatable("screen.jjkcraft.culling.verdict_active");
			color = Palette.muted();
		} else if (conflicts(rule, proposal)) {
			verdict = Component.translatable("screen.jjkcraft.culling.verdict_conflict");
			color = Palette.danger();
		} else if (me.points() < cost) {
			verdict = Component.translatable("screen.jjkcraft.culling.verdict_short", cost - me.points());
			color = Palette.danger();
		} else {
			verdict = Component.translatable("screen.jjkcraft.culling.verdict_ok", cost);
			color = ClientCulling.ACCENT;
		}

		g.textWithWordWrap(Ink.font(), verdict, rx, ry, rw, color, false);
	}

	private static boolean conflicts(CullingRule rule, Identifier id) {
		for (CullingPayloads.RuleView active : ClientCulling.view().rules()) {
			if (rule.conflicts().contains(active.id())) {
				return true;
			}

			if (ClientCulling.rule(active.id()).map(r -> r.conflicts().contains(id)).orElse(false)) {
				return true;
			}
		}

		return false;
	}

	// ---------------------------------------------------------------- input

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (super.mouseClicked(event, doubleClick)) {
			return true;
		}

		double mx = event.x();
		double my = event.y();
		int x = panelX() + 8;
		int top = contentY() + 31;
		if (page == Page.BOARD) {
			int w = panelW() - 170;
			int y = top + 12;
			List<CullingPayloads.Entry> board = ClientCulling.view().board();
			for (int i = scroll; i < board.size(); i++) {
				if (mx >= x - 2 && mx < x + w && my >= y - 2 && my < y + ROW - 2) {
					selected = board.get(i).id();
					return true;
				}

				y += ROW;
			}
		} else if (page == Page.NEGOTIATE) {
			int y = top;
			for (ClientCulling.RuleEntry entry : ClientCulling.allRules()) {
				if (entry.rule().base()) {
					continue;
				}

				if (mx >= x - 2 && mx < x + 150 && my >= y - 2 && my < y + ROW - 2) {
					proposal = entry.id();
					return true;
				}

				y += ROW;
			}
		}

		return false;
	}

	@Override
	public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
		if (page == Page.BOARD || page == Page.RULES) {
			scroll = Math.max(0, scroll - (int) Math.signum(scrollY) * (page == Page.RULES ? 3 : 1));
			return true;
		}

		return super.mouseScrolled(x, y, scrollX, scrollY);
	}

	/** For tests: pick a rule on the negotiation page, as a click would. */
	public void propose(Identifier id) {
		page = Page.NEGOTIATE;
		proposal = id;
	}
}
