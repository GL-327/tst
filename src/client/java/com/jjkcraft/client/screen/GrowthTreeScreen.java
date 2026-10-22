package com.jjkcraft.client.screen;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import com.jjkcraft.client.state.ClientProgress;
import com.jjkcraft.client.ui.Ink;
import com.jjkcraft.client.ui.InkButton;
import com.jjkcraft.client.ui.Palette;
import com.jjkcraft.core.progress.ProgressionMath;
import com.jjkcraft.network.payload.SpendGrowthPayload;
import com.jjkcraft.sorcerer.GrowthPath;
import com.jjkcraft.sorcerer.SorcererProgress;

/**
 * Growth Tree: four upgrade paths of 20 nodes. Spending sends a request; the server checks it and
 * the screen updates when the new progress arrives.
 */
public final class GrowthTreeScreen extends JjkScreen {
	private final Map<GrowthPath, InkButton> buttons = new EnumMap<>(GrowthPath.class);

	public GrowthTreeScreen() {
		super(Component.translatable("screen.jjkcraft.growth"));
	}

	@Override
	protected void initContent() {
		int colW = (panelW() - 16) / 4;
		for (GrowthPath path : GrowthPath.values()) {
			int cx = panelX() + 8 + path.ordinal() * colW;
			InkButton button = new InkButton(cx + 4, panelY() + panelH() - 22, colW - 14, 14,
					Component.translatable("screen.jjkcraft.growth.spend"),
					b -> ClientPlayNetworking.send(new SpendGrowthPayload(path))).accent(color(path));
			buttons.put(path, addRenderableWidget(button));
		}
	}

	@Override
	public void tick() {
		SorcererProgress p = ClientProgress.get();
		int unspent = ClientProgress.unspentPoints();
		for (Map.Entry<GrowthPath, InkButton> entry : buttons.entrySet()) {
			GrowthPath path = entry.getKey();
			boolean capped = p.growth().get(path) >= ProgressionMath.MAX_POINTS_PER_PATH;
			boolean closed = p.heavenlyRestricted() && path == GrowthPath.DOMAIN;
			entry.getValue().active = minecraft.player!=null&&minecraft.player.isCreative()&&unspent > 0 && !capped && !closed;
            entry.getValue().setMessage(Component.literal(minecraft.player!=null&&minecraft.player.isCreative()?"+1 · creative":"Train in the world"));
            entry.getValue().setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(switch(path){case OUTPUT->"Land techniques on straw dolls and live opponents.";case CONTROL->"Practise accurate contact and cursed-energy control.";case EFFICIENCY->"Complete timed breathing drills on a mat.";case DOMAIN->"Sustain real domains for at least four seconds.";})));
		}
	}

	@Override
	protected void extractContent(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
		SorcererProgress p = ClientProgress.get();
		int y = contentY();
		Ink.text(g, Component.translatable("screen.jjkcraft.profile.unspent", ClientProgress.unspentPoints()), panelX() + 8, y, Palette.gold(), 1f);
		Ink.text(g, Component.literal("Drills assign available growth points through actual practice."), panelX() + 8, y + 10, Palette.TEXT_DIM, 1f);

		int colW = (panelW() - 16) / 4;
		int top = y + 26;
		for (GrowthPath path : GrowthPath.values()) {
			int cx = panelX() + 8 + path.ordinal() * colW;
			int points = p.growth().get(path);
			int color = color(path);
			boolean closed = p.heavenlyRestricted() && path == GrowthPath.DOMAIN;

			g.fill(cx, top - 2, cx + colW - 6, panelY() + panelH() - 26, 0x48000000);
			Ink.text(g, Component.translatable(path.translationKey()), cx + 4, top + 2, closed ? Palette.muted() : color, 1f);
			Ink.text(g, points + "/" + ProgressionMath.MAX_POINTS_PER_PATH, cx + colW - 12 - Ink.font().width(points + "/20"), top + 2, Palette.TEXT_DIM, 1f);

			// Nodes: two columns of ten diamonds, filled from the bottom.
			int nodeTop = top + 16;
			int nodeArea = panelY() + panelH() - 30 - nodeTop - 40;
			int step = Math.max(6, Math.min(9, nodeArea / 10));
			for (int n = 0; n < ProgressionMath.MAX_POINTS_PER_PATH; n++) {
				int row = 9 - n / 2;
				int col = n % 2;
				int nx = cx + colW / 2 - 8 + col * 12;
				int ny = nodeTop + row * step;
				boolean filled = n < points;
				Ink.diamond(g, nx, ny, filled ? 3 : 2, filled ? color : 0xFF3A3644);
			}

			// Effect summary.
			int textY = nodeTop + 10 * step + 2;
            for (Component lineText : effect(path, points)) {
                for(var line:Ink.font().split(lineText,colW-14)){
                    g.text(Ink.font(),line,cx+4,textY,Palette.TEXT_DIM,true);textY+=10;
                }
            }
		}
	}

	private static Component[] effect(GrowthPath path, int points) {
		return switch (path) {
			case OUTPUT -> new Component[] {
					Component.translatable("growth.jjkcraft.effect.capacity", pct(0.03 * points)),
					Component.translatable("growth.jjkcraft.effect.damage", pct(ProgressionMath.damageMultiplier(points) - 1))};
			case CONTROL -> new Component[] {
					Component.translatable("growth.jjkcraft.effect.cost", pct(1 - ProgressionMath.costMultiplier(points))),
					Component.translatable("growth.jjkcraft.effect.black_flash", fmt(ProgressionMath.blackFlashBonus(points)))};
			case EFFICIENCY -> new Component[] {
					Component.translatable("growth.jjkcraft.effect.regen", pct(0.02 * points)),
					Component.translatable("growth.jjkcraft.effect.cooldown", pct(1 - ProgressionMath.cooldownMultiplier(points)))};
			case DOMAIN -> new Component[] {
					Component.translatable("growth.jjkcraft.effect.clash", pct(ProgressionMath.domainPowerMultiplier(points) - 1)),
					Component.translatable("growth.jjkcraft.effect.domain_cost", pct(1 - ProgressionMath.domainCostMultiplier(points)))};
		};
	}

	private static int color(GrowthPath path) {
		return switch (path) {
			case OUTPUT -> Palette.danger();
			case CONTROL -> Palette.energyHigh();
			case EFFICIENCY -> Palette.good();
			case DOMAIN -> Palette.energy();
		};
	}

	private static String pct(double fraction) {
		return fmt(fraction * 100);
	}

	private static String fmt(double v) {
		return String.format(Locale.ROOT, "%.1f", v);
	}
}
