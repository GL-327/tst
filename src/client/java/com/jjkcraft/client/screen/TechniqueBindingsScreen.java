package com.jjkcraft.client.screen;

import com.mojang.blaze3d.platform.InputConstants;

import com.jjkcraft.client.JjkKeys;

import com.jjkcraft.client.ui.*;

import net.minecraft.client.KeyMapping;

import net.minecraft.client.gui.GuiGraphicsExtractor;

import net.minecraft.client.input.KeyEvent;

import net.minecraft.network.chat.Component;

/** A compact, persistent binding editor beside the player's actual four-slot loadout. */
public final class TechniqueBindingsScreen extends JjkScreen {
    private final KeyMapping[] keys={JjkKeys.SKILLS[0],JjkKeys.SKILLS[1],JjkKeys.SKILLS[2],JjkKeys.SKILLS[3],JjkKeys.CAST,JjkKeys.BLOCK,JjkKeys.DASH,JjkKeys.DOMAIN,JjkKeys.WHEEL,JjkKeys.COMBAT,JjkKeys.LIGHT,JjkKeys.HEAVY,JjkKeys.BANK,JjkKeys.AUTO_COMBAT};
    private static final String[] NAMES={"Skill slot 1","Skill slot 2","Skill slot 3","Skill slot 4","Selected skill","Block / parry","Dash / evasive","Domain","Skill wheel","Toggle combat","Light / uppercut","Heavy / sweep","Next power bank","Toggle Auto Combat"};
    private int editing=-1;
    public TechniqueBindingsScreen(){super(Component.literal("Technique bindings"));}

    @Override protected void initContent(){
        int half=(panelW()-30)/2;
        addRenderableWidget(new InkButton(panelX()+panelW()-132,contentY()-3,122,17,Component.literal("Auto Combat: "+(com.jjkcraft.client.config.ClientConfig.get().autoCombat?"ON":"OFF")),b->{var cfg=com.jjkcraft.client.config.ClientConfig.get();cfg.autoCombat=!cfg.autoCombat;com.jjkcraft.client.config.ClientConfig.save();rebuildWidgets();}));
        for(int i=0;i<keys.length;i++){
            final int n=i;int x=panelX()+10+(i/7)*(half+10),y=contentY()+25+(i%7)*20;
            addRenderableWidget(new InkButton(x+half-75,y-3,75,19,Component.literal(editing==i?"Press a key…":keys[i].getTranslatedKeyMessage().getString()),b->{editing=n;rebuildWidgets();}));

        }
        addRenderableWidget(new InkButton(panelX()+10,panelY()+panelH()-25,65,18,Component.literal("Back"),b->minecraft.gui.setScreen(new LoadoutScreen())));
        addRenderableWidget(new InkButton(panelX()+panelW()-95,panelY()+panelH()-25,85,18,Component.literal("Reset bindings"),b->{for(var key:keys)key.setKey(key.getDefaultKey());commit();}));

    }
    private void commit(){KeyMapping.resetMapping();minecraft.options.save();editing=-1;rebuildWidgets();}

    @Override public boolean keyPressed(KeyEvent event){
        if(editing<0)return super.keyPressed(event);
        if(event.key()==InputConstants.KEY_ESCAPE){editing=-1;rebuildWidgets();return true;}
        keys[editing].setKey(event.key()==InputConstants.KEY_BACKSPACE?InputConstants.UNKNOWN:InputConstants.getKey(event));commit();return true;

    }

    @Override protected void extractContent(GuiGraphicsExtractor g,int mx,int my,float a){
        int x=panelX()+10,y=contentY(),half=(panelW()-30)/2;
        Ink.text(g,"TECHNIQUE BINDINGS",x,y,Palette.energyHigh(),1);
        Ink.text(g,"U/I/O manual - M1 auto assist - vanilla Q/E/F preserved",x,y+12,Palette.TEXT_DIM,1);
        for(int i=0;i<keys.length;i++){
            boolean conflict=false;for(int k=0;k<keys.length;k++)if(i!=k&&keys[i].same(keys[k])&&!keys[i].isUnbound())conflict=true;
            Ink.text(g,NAMES[i],x+(i/7)*(half+10),y+25+(i%7)*20,conflict?Palette.danger():Palette.TEXT,1);

        }
        Ink.text(g,editing<0?"Red = duplicate · Click a key to change it":"Press a key · Escape cancels · Backspace unbinds",x,y+163,Palette.TEXT_DIM,1);

    }

}
