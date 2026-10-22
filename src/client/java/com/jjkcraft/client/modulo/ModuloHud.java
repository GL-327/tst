package com.jjkcraft.client.modulo;

import java.util.List;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import com.jjkcraft.client.config.ClientConfig;
import com.jjkcraft.client.config.JjkClientConfig;
import com.jjkcraft.client.hud.HudLayout;
import com.jjkcraft.client.ui.Ink;
import com.jjkcraft.client.ui.Palette;
import com.jjkcraft.modulo.ModuloPayloads;

/**
 * The Modulo HUD (docs/UI_SPEC.md): the Mul energy meter, the mission tracker (title, objective,
 * distance and direction to where it happens, or the Descent's position), and the dialogue box.
 */
public final class ModuloHud {
	public static final int MUL_W = 112;
	public static final int MUL_H = 12;
	public static final int TRACKER_W = 160;
	public static final int DIALOGUE_W = 260;

	private ModuloHud() {
	}

	private static boolean hidden(Minecraft mc) {
		return mc.gui.hud.isHidden() || mc.player == null || !ClientModulo.view().enabled();
	}

	/** Mul energy: shown while the player has any, or is in a Modulo dimension. */
	public static void mulMeter(GuiGraphicsExtractor g, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		JjkClientConfig.HudElement cfg = ClientConfig.get().hud.mulMeter;
		ModuloPayloads.View view = ClientModulo.view();
		if (!cfg.visible || hidden(mc) || (view.mulEnergy() <= 0 && !view.inModulo())) {
			return;
		}

		float alpha = cfg.opacity();
		HudLayout.pushBelow(g, cfg, MUL_W, MUL_H, ClientConfig.get().hud.energyBar, ClientConfig.get().hud.gradeBadge, ClientConfig.get().hud.combatPanel, ClientConfig.get().hud.cullingPanel);
		Ink.flatPanel(g, 0, 0, MUL_W, MUL_H, alpha * 0.85f);
		Ink.text(g, Component.translatable("hud.jjkcraft.modulo.mul"), 3, 2, ClientModulo.ACCENT, alpha);
		int x0 = 24;
		int w = 70;
		double share = Mth.clamp(view.mulEnergy() / Math.max(1, view.mulMax()), 0, 1);
		g.fill(x0, 4, x0 + w, 8, ARGB.multiplyAlpha(0xFF14201A, alpha));
		g.fill(x0, 4, x0 + (int) Math.round(w * share), 8, ARGB.multiplyAlpha(ClientModulo.ACCENT, alpha));
		if (ClientConfig.get().hud.showNumbers) {
			String label = Math.round(view.mulEnergy()) + "";
			Ink.text(g, label, MUL_W - 3 - Ink.font().width(label), 2, Palette.TEXT_DIM, alpha);
		}

		g.pose().popMatrix();
	}

	/** The current mission and objective, with how far it is. */
	public static void tracker(GuiGraphicsExtractor g, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		JjkClientConfig.HudElement cfg = ClientConfig.get().hud.missionTracker;
		ModuloPayloads.View view = ClientModulo.view();
		if (!cfg.visible || hidden(mc) || view.objective().isEmpty() || ClientModulo.sequence() != null) {
			return;
		}

		float alpha = cfg.opacity();
		Component objective = Component.translatable(view.objective(), view.args().toArray());
		List<FormattedCharSequence> lines = Ink.font().split(objective, TRACKER_W - 10);
		String where = where(mc, view);
		int height = 16 + lines.size() * 10 + (where.isEmpty() ? 0 : 10);
		var hud = ClientConfig.get().hud;
		HudLayout.pushBelow(g, cfg, TRACKER_W, height, hud.energyBar, hud.gradeBadge, hud.combatPanel, hud.cullingPanel, hud.mulMeter);
		Ink.panel(g, 0, 0, TRACKER_W, height, ClientModulo.ACCENT, alpha);
		Component title = view.missionTitle().isEmpty() ? Component.translatable("itemGroup.jjkcraft.modulo")
				: Component.translatable(view.missionTitle());
		int titleRoom = TRACKER_W - 10;
		if (view.missionOrder().getFirst() > 0) {
			Component order = Component.translatable("hud.jjkcraft.modulo.mission", view.missionOrder().get(0), view.missionOrder().get(1));
			int orderWidth = Ink.font().width(order);
			Ink.text(g, order, TRACKER_W - 5 - orderWidth, 4, Palette.TEXT_DIM, alpha);
			titleRoom -= orderWidth + 6;
		}

		Ink.text(g, Ink.fit(title, titleRoom), 5, 4, ClientModulo.ACCENT, alpha);

		int y = 15;
		for (FormattedCharSequence line : lines) {
			g.text(Ink.font(), line, 5, y, ARGB.multiplyAlpha(Palette.TEXT, alpha), true);
			y += 10;
		}

		if (!where.isEmpty()) {
			Ink.text(g, Ink.fit(Component.literal(where), TRACKER_W - 10), 5, y, Palette.TEXT_DIM, alpha);
		}

		g.pose().popMatrix();
	}

	/** "Kyoto Jujutsu High: 120 m, NE", the Descent's coordinates, or nothing. */
	private static String where(Minecraft mc, ModuloPayloads.View view) {
		if (view.descent().isPresent() && mc.level.dimension() == Level.OVERWORLD) {
			return Component.translatable("hud.jjkcraft.modulo.descent", view.descent().get().getX(), view.descent().get().getZ()).getString();
		}

		if (view.target().isEmpty()) {
			return "";
		}

		String site = view.siteName().isEmpty() ? "" : Component.translatable(view.siteName()).getString() + ": ";
		if (!view.target().get().dimension().equals(mc.level.dimension())) {
			return site + Component.translatable("hud.jjkcraft.modulo.elsewhere").getString();
		}

		Vec3 to = Vec3.atCenterOf(view.target().get().pos()).subtract(mc.player.position());
		int metres = (int) Math.round(to.horizontalDistance());
		String[] compass = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};
		double angle = Math.toDegrees(Math.atan2(-to.x, to.z));
		String direction = compass[Math.floorMod((int) Math.round(angle / 45.0), 8)];
		return site + Component.translatable("hud.jjkcraft.modulo.distance", metres).getString() + " " + direction;
	}

	/** The dialogue box: the speaker's name and their line. */
	public static void dialogue(GuiGraphicsExtractor g, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		JjkClientConfig.HudElement cfg = ClientConfig.get().hud.dialogue;
		ClientModulo.Line line = ClientModulo.showing();
		if (!cfg.visible || line == null || mc.gui.hud.isHidden() || mc.player == null) {
			return;
		}

		long shown = ClientModulo.showingFor();
		float fade = ClientConfig.reducedMotion() ? 1f : Mth.clamp(shown / 150f, 0, 1);
		float alpha = cfg.opacity() * fade;
		List<FormattedCharSequence> lines = Ink.font().split(Component.translatable(line.text()), DIALOGUE_W - 12);
		boolean narrator = line.speaker().equals("narrator");
		int height = (narrator ? 6 : 16) + lines.size() * 10 + 4;
		HudLayout.push(g, cfg, DIALOGUE_W, height);
		Ink.panel(g, 0, 0, DIALOGUE_W, height, narrator ? Palette.TEXT_DIM : ClientModulo.ACCENT, alpha);
		int y = 5;
		if (!narrator) {
			Ink.text(g, Component.translatable("npc.jjkcraft.modulo." + line.speaker()), 6, y, ClientModulo.ACCENT, alpha);
			y += 11;
		}

		for (FormattedCharSequence text : lines) {
			g.text(Ink.font(), text, 6, y, ARGB.multiplyAlpha(narrator ? Palette.PAPER : Palette.TEXT, alpha), true);
			y += 10;
		}

		g.pose().popMatrix();
	}
}
