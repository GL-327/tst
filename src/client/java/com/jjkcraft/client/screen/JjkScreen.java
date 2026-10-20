package com.jjkcraft.client.screen;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;

import com.jjkcraft.client.config.ClientConfig;
import com.jjkcraft.client.ui.Ink;
import com.jjkcraft.client.ui.InkButton;
import com.jjkcraft.client.ui.Palette;

/**
 * Base for every mod screen: an ink-washed background, one centered ink panel, a title, and a tab
 * bar that switches between the sorcerer screens. Subclasses lay out inside {@link #panelX()} etc.
 */
public abstract class JjkScreen extends Screen {
	/** A tab in the shared bar. Tabs are added as their screens are built. */
	public record Tab(Component label, Class<? extends JjkScreen> type, Supplier<JjkScreen> factory) {
	}

	private static final List<Tab> TABS = new ArrayList<>();
	protected static final int PANEL_MAX_W = 560;
	protected static final int PANEL_MAX_H = 320;

	private final long openedAt = System.currentTimeMillis();
	private int panelX;
	private int panelY;
	private int panelW;
	private int panelH;
	private int tabsEnd;

	protected JjkScreen(Component title) {
		super(title);
	}

	public static void addTab(Tab tab) {
		TABS.add(tab);
	}

	public static List<Tab> tabs() {
		return TABS;
	}

	@Override
	protected void init() {
		panelW = Math.min(PANEL_MAX_W, width - 16);
		panelH = Math.min(PANEL_MAX_H, height - 16);
		panelX = (width - panelW) / 2;
		panelY = (height - panelH) / 2;

		int tabX = panelX + 4;
		int tabW = TABS.isEmpty() ? 0 : Math.min(70, (panelW - 8) / TABS.size());
		tabsEnd = tabX + tabW * TABS.size();
		for (Tab tab : TABS) {
			boolean current = tab.type() == getClass() || tab.type()==LoadoutScreen.class&&(this instanceof FamilyTechniqueScreen||this instanceof TechniqueWorkshopScreen||this instanceof TechniqueBindingsScreen||this instanceof CreativeTechniqueScreen);
			addRenderableWidget(new InkButton(tabX, panelY + 4, tabW - 2, 14, tab.label(), b -> {
				if (!current) {
					minecraft.gui.setScreen(tab.factory().get());
				}
			}).selected(current));
			tabX += tabW;
		}

		initContent();
	}

	/** Adds widgets. Called from {@link #init()} after the tab bar. */
	protected abstract void initContent();

	/** Draws the screen's own content under the widgets. */
	protected abstract void extractContent(GuiGraphicsExtractor g, int mouseX, int mouseY, float a);

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
		g.fill(0, 0, width, height, 0xD006080E);
		// A few wide, faint brush strokes behind the panel.
		for (int i = 0; i < 5; i++) {
			int y = height * (i + 1) / 6;
			int alpha = 18 + i * 4;
			g.fill(0, y, width, y + 1, ARGB.multiplyAlpha(Palette.accent(),0.06f));
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
		float appear = ClientConfig.reducedMotion() ? 1f : Math.min(1f, (System.currentTimeMillis() - openedAt) / 160f);
		g.fill(panelX,panelY,panelX+panelW,panelY+panelH,0xFF0D0C14);
		Ink.panel(g, panelX, panelY, panelW, panelH, Palette.accent(), 0.92f * appear + 0.08f);
		g.fill(panelX+1,panelY+1,panelX+panelW-1,panelY+20,0xFF171B29);
		g.outline(panelX-3,panelY-3,panelW+6,panelH+6,0x805D6786);
		g.fill(panelX+2,panelY+20,panelX+panelW-2,panelY+21,Palette.accent());
		if(panelY>=15){
			var family=com.jjkcraft.client.state.ClientTechniques.state().innate().flatMap(com.jjkcraft.client.state.ClientTechniques::definition);
			family.ifPresent(d->Ink.text(g,Ink.fit(Component.translatable(d.name()),panelW-120),panelX+2,panelY-12,Palette.energyHigh(),1));
			String reserve=com.jjkcraft.client.state.ClientEnergy.uncapped()?"UNBOUNDED CE BANK":String.format(java.util.Locale.ROOT,"%.0f CE RESERVE",com.jjkcraft.client.state.ClientEnergy.max());
			Ink.text(g,reserve,panelX+panelW-Ink.font().width(reserve)-2,panelY-12,Palette.TEXT_DIM,1);
		}
		int titleX = panelX + panelW - Ink.font().width(getTitle()) - 8;
		if (titleX > tabsEnd + 4) {
			// With many tabs the bar is full; the selected tab already names the screen.
			Ink.text(g, getTitle(), titleX, panelY + 7, Palette.TEXT_DIM, 1f);
		}
		extractContent(g, mouseX, mouseY, a);
		super.extractRenderState(g, mouseX, mouseY, a);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	protected int panelX() {
		return panelX;
	}

	protected int panelY() {
		return panelY;
	}

	protected int panelW() {
		return panelW;
	}

	protected int panelH() {
		return panelH;
	}

	/** Top of the content area below the tab bar. */
	protected int contentY() {
		return panelY + 24;
	}

	/** A labelled progress bar used by several screens. */
	protected static void bar(GuiGraphicsExtractor g, int x, int y, int w, double fraction, int color) {
		g.fill(x, y, x + w, y + 4, 0xFF1C1A24);
		int fw = (int) Math.round(w * Math.max(0, Math.min(1, fraction)));
		if (fw > 0) {
			g.fill(x, y, x + fw, y + 4, color);
			g.fill(x, y, x + fw, y + 1, 0x60FFFFFF);
		}
	}
}
