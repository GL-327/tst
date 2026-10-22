package com.jjkcraft.client.hud;

import java.util.Optional;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;

import com.jjkcraft.client.JjkKeys;
import com.jjkcraft.client.config.ClientConfig;
import com.jjkcraft.client.config.JjkClientConfig;
import com.jjkcraft.client.state.ClientEnergy;
import com.jjkcraft.client.state.ClientProgress;
import com.jjkcraft.client.state.ClientTechniques;
import com.jjkcraft.client.ui.Ink;
import com.jjkcraft.client.ui.Palette;
import com.jjkcraft.technique.CastFailure;
import com.jjkcraft.technique.TechniqueDefinition;
import com.jjkcraft.technique.TechniqueState;

/** Selected-power badge, cooldown/feedback and the held-charge meter. Slot selection uses the wheel. */
public final class TechniqueHotbarHud {
	private static final int SLOT = 22;
	private static final int GAP = 2;

	private TechniqueHotbarHud() {
	}

	public static void extract(GuiGraphicsExtractor g, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		JjkClientConfig.HudElement cfg = ClientConfig.get().hud.techniqueHotbar;
		if (mc.gui.hud.isHidden() || mc.gui.screen()!=null || mc.player == null || !ClientProgress.hasData()
				|| ClientProgress.get().heavenlyRestricted() || ClientTechniques.known().isEmpty()) {
			return;
		}

        var trial=com.jjkcraft.client.state.ClientTechniqueMenus.view(com.jjkcraft.JjkCraft.id("ten_shadows"));
        var tracker=ClientConfig.get().hud.ritualTracker;
        if(tracker.visible&&trial!=null&&!trial.trial().isEmpty()){
            int seconds=com.jjkcraft.client.state.ClientTechniqueMenus.trialSeconds();int color=trial.trialDistance()>40||seconds<20?Palette.danger():Palette.gold();float alpha=tracker.opacity();
            var hud=ClientConfig.get().hud;HudLayout.pushBelow(g,tracker,180,45,hud.energyBar,hud.gradeBadge,hud.cullingPanel,hud.missionTracker,hud.mulMeter);
            Ink.panel(g,0,0,180,45,color,alpha*.9f);
            Ink.text(g,String.format(java.util.Locale.ROOT,"SUBJUGATION / %d:%02d",seconds/60,seconds%60),5,4,color,alpha);
            var id=Identifier.tryParse(trial.trial());if(id!=null)Ink.text(g,Ink.fit(Component.translatable("entity."+id.getNamespace()+"."+id.getPath()),170),5,15,Palette.TEXT,alpha);
            Ink.text(g,String.format(java.util.Locale.ROOT,"%.0fm / 48m limit",trial.trialDistance()),5,26,color,alpha);
            g.fill(5,40,175,42,ARGB.multiplyAlpha(0xFF2A2533,alpha));g.fill(5,40,5+(int)(170*Math.clamp(seconds/180.0,0,1)),42,ARGB.multiplyAlpha(color,alpha));g.pose().popMatrix();
        }
        var charge=ClientTechniques.charge();
        var meter=ClientConfig.get().hud.chargeMeter;
        if(charge.slot()>=0&&meter.visible){
            float alpha=meter.opacity();HudLayout.pushBelow(g,meter,150,32,cfg);
            int color=charge.percent()>100?0xFFB56BFF:Palette.accent();
            Ink.panel(g,0,0,150,32,color,alpha*.92f);
            Ink.text(g,Ink.fit(Component.literal("CHARGE "+charge.percent()+"% / "+charge.maximum()+"%"),140),5,4,color,alpha);
            Ink.text(g,Ink.fit(Component.literal(String.format(java.util.Locale.ROOT,"%.1f CE/s · release to cast",charge.cePerSecond())),140),5,15,Palette.TEXT_DIM,alpha);
            g.fill(5,27,145,29,ARGB.multiplyAlpha(0xFF292333,alpha));
            g.fill(5,27,5+(int)Math.clamp(140*charge.percent()/(double)charge.maximum(),0,140),29,ARGB.multiplyAlpha(color,alpha));
            g.pose().popMatrix();
        }
        if(!cfg.visible)return;
        // A single selected-power badge replaces the four-row sidebar. Selection lives on the wheel.
        var state=ClientTechniques.state();int selected=state.selected();float alpha=cfg.opacity();
        boolean infinity=ClientTechniques.known().contains(com.jjkcraft.JjkCraft.id("infinity"));int badgeHeight=infinity?47:34;
        HudLayout.push(g,cfg,150,badgeHeight);Ink.panel(g,0,0,150,badgeHeight,Palette.accent(),alpha*.85f);
        slot(g,4,5,selected,state.slot(selected),true,ClientTechniques.recentFailure(selected),alpha);
        String title=state.slot(selected).flatMap(ClientTechniques::definition).map(d->Component.translatable(d.name()).getString()).orElse("Empty slot");
        Ink.text(g,Ink.fit(Component.literal(title),116),31,5,Palette.TEXT,alpha);
        Ink.text(g,Ink.fit(Component.literal(JjkKeys.WHEEL.getTranslatedKeyMessage().getString()+" wheel · "+JjkKeys.CAST.getTranslatedKeyMessage().getString()+" activate"),116),31,18,Palette.TEXT_DIM,alpha);
        if(infinity)Ink.text(g,Ink.fit(Component.literal(com.jjkcraft.client.state.ClientTechniqueStatus.infinity()),140),5,33,ClientTechniques.infinityActive()?Palette.good():ClientTechniques.infinityEnabled()?Palette.gold():Palette.TEXT_DIM,alpha);
        g.pose().popMatrix();
    }

	private static void slot(GuiGraphicsExtractor g, int x, int y, int index, Optional<Identifier> id, boolean selected, CastFailure failure, float alpha) {
		g.fill(x, y, x + SLOT, y + SLOT, ARGB.multiplyAlpha(0xFF15131C, alpha));
		if (id.isEmpty()) {
			g.outline(x, y, SLOT, SLOT, ARGB.multiplyAlpha(0xFF2E2A36, alpha));
			return;
		}

		Optional<TechniqueDefinition> def = ClientTechniques.definition(id.get());
		if (def.isEmpty()) {
			return;
		}

		g.blit(RenderPipelines.GUI_TEXTURED, def.get().icon(), x + 1, y + 1, 0, 0, SLOT - 2, SLOT - 2, SLOT - 2, SLOT - 2,
				ARGB.white(alpha));

		float cooldown = ClientTechniques.cooldownFraction(id.get());
		if (cooldown > 0) {
			int h = (int) Math.ceil((SLOT - 2) * cooldown);
			g.fill(x + 1, y + 1 + (SLOT - 2 - h), x + SLOT - 1, y + SLOT - 1, ARGB.multiplyAlpha(0xB0000000, alpha));
			Ink.ring(g, x + SLOT / 2, y + SLOT / 2, SLOT / 2 - 1, 1, 1 - cooldown, ARGB.multiplyAlpha(def.get().visuals().color(), alpha));
		}

		boolean affordable = ClientTechniques.affordable(id.get()) || ClientEnergy.max() <= 0;
		boolean active = ClientTechniques.activeIs(id.get());
		int border = failure != CastFailure.NONE ? Palette.danger()
				: active ? def.get().visuals().color()
				: selected ? Palette.gold() : Palette.INK_EDGE;
		g.outline(x, y, SLOT, SLOT, ARGB.multiplyAlpha(border, alpha));
		if (active && !ClientConfig.reduceFlashing()) {
			float pulse = 0.5f + 0.5f * Mth.sin(Util.getMillis() / 120f);
			g.outline(x - 1, y - 1, SLOT + 2, SLOT + 2, ARGB.multiplyAlpha(def.get().visuals().color(), alpha * pulse));
		}

		if (!affordable) {
			g.fill(x + 1, y + 1, x + SLOT - 1, y + SLOT - 1, ARGB.multiplyAlpha(0x50E0303F, alpha));
			Ink.text(g, "!", x + SLOT - 6, y + 2, Palette.danger(), alpha);
		}

		g.pose().pushMatrix();
		g.pose().translate(x + 2, y + SLOT - 6);
		g.pose().scale(0.5f, 0.5f);
		Component binding=Component.literal(String.valueOf(index+1));
		Ink.text(g, Ink.fit(binding,38), 0, 0, selected ? Palette.gold() : Palette.TEXT_DIM, alpha);
		g.pose().popMatrix();

		if (failure != CastFailure.NONE && selected) {
			g.pose().pushMatrix();
			g.pose().translate(-Ink.font().width(Component.translatable(failure.translationKey())) * 0.6f - 4, y + 7);
			g.pose().scale(0.6f, 0.6f);
			Ink.text(g, Component.translatable(failure.translationKey()), 0, 0, Palette.danger(), alpha * (1 - ClientTechniques.feedbackAge()));
			g.pose().popMatrix();
		}
	}
}
