package com.jjkcraft.client.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import com.jjkcraft.client.config.ClientConfig;
import com.jjkcraft.client.state.ClientDomains;
import com.jjkcraft.client.state.ClientProgress;
import com.jjkcraft.client.ui.Ink;
import com.jjkcraft.client.ui.Palette;
import com.jjkcraft.core.domain.DomainMath;
import com.jjkcraft.domain.Domains;

/** Domain readiness and active-state feedback, shown only within the technique wheel. */
public final class DomainMeterHud {
	public static final int WIDTH = 150;
	public static final int HEIGHT = 34;

	private DomainMeterHud() {
	}

	public static void extract(GuiGraphicsExtractor g, DeltaTracker delta) {
		// Domain readiness belongs to the selector, not the ordinary combat view.
	}

    public static void wheel(GuiGraphicsExtractor g,int width,int height){
        var mc=Minecraft.getInstance();if(mc.level==null||mc.player==null||!ClientConfig.get().hud.domainMeter.visible)return;
        var own=ClientDomains.own();var inside=ClientDomains.at(mc.player.position()).filter(d->!d.mine());
        var def=ClientDomains.self().known().flatMap(id->Domains.get(mc.level.registryAccess(),id));
        if(def.isEmpty()&&own.isEmpty()&&inside.isEmpty()&&!ClientDomains.self().simpleActive())return;
        long burnout=Math.max(0,ClientDomains.self().burnoutUntil()-mc.level.getGameTime());
        String status=inside.isPresent()?"Inside "+ClientDomains.name(inside.get()).getString():own.isPresent()?"Active: "+ClientDomains.name(own.get()).getString():burnout>0?"Technique burnout: "+(int)Math.ceil(burnout/20.0)+"s":def.map(d->Component.translatable(d.name()).getString()+" / "+Math.round(DomainMath.castCost(d.castCost(),DomainMath.Variant.FULL,ClientProgress.hasData()?ClientProgress.get().growth().domain():0,1))+" CE").orElse("Simple Domain active");
        int w=Math.min(280,width-20),x=(width-w)/2,y=height-43;
        g.pose().pushMatrix();g.pose().translate(x,y);
        Ink.panel(g,0,0,w,17,Palette.accent(),.9f);Ink.text(g,Ink.fit(Component.literal(status),w-12),6,4,burnout>0?Palette.danger():Palette.TEXT,1);g.pose().popMatrix();
    }

}
