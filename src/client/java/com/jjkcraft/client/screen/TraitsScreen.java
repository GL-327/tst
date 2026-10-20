package com.jjkcraft.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import com.jjkcraft.awakening.CharacterTraits;
import com.jjkcraft.client.state.*;
import com.jjkcraft.client.ui.*;

/** Physical traits and clan gifts never consume a technique slot. */
public final class TraitsScreen extends JjkScreen {
    public TraitsScreen(){super(Component.literal("Body and clan attributes"));}
    @Override protected void initContent() { }
    @Override protected void extractContent(GuiGraphicsExtractor g,int mx,int my,float a) {
        int x=panelX()+10,y=contentY()+5,w=panelW()-20;
        Ink.text(g,"Physical attributes · separate from equipped techniques",x,y,Palette.gold(),1);y+=17;
        var state=ClientTraits.own();var registry=minecraft.level==null?null:minecraft.level.registryAccess().lookupOrThrow(CharacterTraits.DEFINITIONS);
        if(state.traits().isEmpty()){Ink.text(g,"Ordinary constitution. Your innate technique is a separate roll.",x,y,Palette.TEXT_DIM,1);y+=20;}
        for(var id:state.traits()) {
            var def=registry==null?null:registry.getValue(id);if(def==null)continue;
            Ink.text(g,Component.translatable(def.name()),x,y,Palette.energyHigh(),1);y+=12;
            for(var line:Ink.font().split(Component.translatable(def.description()),w)){g.text(Ink.font(),line,x,y,Palette.TEXT_DIM,true);y+=10;}y+=8;
        }
        var awakening=ClientAwakening.view().record();
        Ink.text(g,"Clan: "+(awakening.clan().isEmpty()?"Unaffiliated":awakening.clan()),x,panelY()+panelH()-47,Palette.gold(),1);
        Ink.text(g,Ink.fit(Component.literal(state.form().isEmpty()?"Body: original human body":"Body: "+state.form()+" · F5 to inspect"),w),x,panelY()+panelH()-32,Palette.TEXT_DIM,1);
        Ink.text(g,"Heavenly Restrictions trade physical limits for specific strengths.",x,panelY()+panelH()-17,Palette.TEXT_DIM,1);
    }
}
