package com.jjkcraft.client.hud;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import com.jjkcraft.client.config.ClientConfig;
import com.jjkcraft.client.config.JjkClientConfig;
import com.jjkcraft.client.state.ClientSensing;
import com.jjkcraft.client.ui.Ink;
import com.jjkcraft.client.ui.Palette;
import com.jjkcraft.network.payload.SensePayload;

/**
 * Sense radar: a ring around the player, forward at the top. Each presence the server reports is
 * a dot in its grade color: diamonds for curses, squares for everything else. Presences above or
 * below get a small tick. Only shown while sensing is on.
 */
public final class SenseRadarHud {
	public static final int SIZE = 72;
	private static final int RADIUS = 30;

	private SenseRadarHud() {
	}

	public static void extract(GuiGraphicsExtractor g, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		JjkClientConfig.HudElement cfg = ClientConfig.get().hud.senseRadar;
		if (!cfg.visible || mc.gui.hud.isHidden() || mc.player == null || mc.level == null || !ClientSensing.isActive()) {
			return;
		}

		float alpha = cfg.opacity();
		float partial = delta.getGameTimeDeltaPartialTick(false);
		HudLayout.push(g, cfg, SIZE, SIZE + 10);
		int cx = SIZE / 2;
		int cy = SIZE / 2;

		Ink.disc(g, cx, cy, RADIUS + 3, ARGB.multiplyAlpha(Palette.INK, alpha * 0.85f));
		Ink.ring(g, cx, cy, RADIUS, 1, 1f, ARGB.multiplyAlpha(Palette.INK_EDGE, alpha));
		Ink.ring(g, cx, cy, RADIUS / 2, 1, 1f, ARGB.multiplyAlpha(0x602A2633, alpha));
		g.fill(cx, cy - RADIUS, cx + 1, cy - RADIUS + 4, ARGB.multiplyAlpha(Palette.TEXT_DIM, alpha));

		Vec3 me = mc.player.getPosition(partial);
		float yaw = mc.player.getViewYRot(partial) * Mth.DEG_TO_RAD;
		double fx = -Mth.sin(yaw);
		double fz = Mth.cos(yaw);
		double rx = -Mth.cos(yaw);
		double rz = -Mth.sin(yaw);
		float range = Math.max(1, ClientSensing.range());

		int curses = 0;
		for (SensePayload.Sensed s : ClientSensing.sensed()) {
			Entity entity = mc.level.getEntity(s.entityId());
			if (entity == null) {
				continue;
			}

			Vec3 d = entity.getPosition(partial).subtract(me);
			double side = d.x * rx + d.z * rz;
			double ahead = d.x * fx + d.z * fz;
			double scale = RADIUS / range;
			double len = Math.sqrt(side * side + ahead * ahead) * scale;
			if (len > RADIUS - 2) {
				double k = (RADIUS - 2) / len;
				side *= k;
				ahead *= k;
			}

			int x = cx + (int) Math.round(side * scale);
			int y = cy - (int) Math.round(ahead * scale);
			int color = ARGB.multiplyAlpha(ARGB.opaque(s.color()), alpha);
			if (s.curse()) {
				curses++;
				Ink.diamond(g, x, y, 2 + Math.min(2, s.rank() / 2), color);
			} else {
				g.fill(x - 1, y - 1, x + 2, y + 2, color);
			}

			if (Math.abs(d.y) > 4) {
				int tick = d.y > 0 ? y - 4 : y + 4;
				g.fill(x, Math.min(y, tick), x + 1, Math.max(y, tick), color);
			}
		}

		Ink.diamond(g, cx, cy, 1, ARGB.multiplyAlpha(Palette.PAPER, alpha));
		Component label = Component.translatable("hud.jjkcraft.sense.count", curses);
		Ink.centered(g, label, cx, SIZE + 1, Palette.TEXT_DIM, alpha);
		g.pose().popMatrix();
	}
}
