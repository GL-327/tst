package com.jjkcraft.client.screen;

import java.util.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import com.jjkcraft.client.domain.DomainEditor;
import com.jjkcraft.client.state.ClientDomains;
import com.jjkcraft.client.ui.*;
import com.jjkcraft.domain.*;
import com.jjkcraft.network.payload.*;

/** Mechanical costs and visual expression are saved by the server for the next expansion. */
public final class DomainWorkshopScreen extends JjkScreen {
    private static final String[] LABELS={"Radius %","Output %","Duration %","Barrier %","Primary RGB","Secondary RGB","Domain name"};
    private static final String[] AMBIENCE={"","none","blades","glyphs","shadows","lanterns","arsenal","rings"};
    private static final String[] HELP={"50–200%. Larger domains cost more CE.","50–200%. Raises sure-hit power and CE cost.","50–200%. Longer duration costs more CE.","50–200%. Stronger closed walls cost more CE. Ignored for open domains.","Six hexadecimal digits, or blank for the original color.","Six hexadecimal digits, or blank for the original color.","A personal name, up to 48 characters."};
    private final Screen parent;
    private final EditBox[] fields=new EditBox[7];
    private String[] text;
    private DomainProfile draft;
    private boolean open,pending,reading=true;
    private int ambience,ticks,revision=DomainEditor.revision;
    private String feedback="Loading saved expression…";
    private InkButton save;
    public DomainWorkshopScreen(Screen parent){super(Component.literal("Domain workshop"));this.parent=parent;draft=DomainEditor.profile;open=draft.open()&&canOpen();ambience=Arrays.asList(AMBIENCE).indexOf(draft.ambience());if(ambience<0)ambience=0;DomainEditor.accepted=false;ClientPlayNetworking.send(new DomainProfilePayload(draft,false));pending=true;}
    private boolean canOpen(){return ClientDomains.self().known().filter(DomainCustomization::allowsOpen).isPresent();}
    private int column(){return (panelW()-26)/2;}
    private int rowHeight(){return Math.min(34,(panelH()-100)/4);}
    @Override protected void initContent(){
        int x=panelX()+9,y=contentY()+13,cw=column(),rh=rowHeight();
        if(text==null)text=new String[]{""+draft.size(),""+draft.output(),""+draft.duration(),""+draft.barrier(),draft.color()<0?"":String.format("%06X",draft.color()),draft.secondary()<0?"":String.format("%06X",draft.secondary()),draft.title()};
        for(int i=0;i<7;i++){int index=i,col=i%2,row=i/2;var f=new EditBox(font,x+col*(cw+8),y+row*rh,cw,16,Component.literal(LABELS[i]));f.setMaxLength(i==6?48:i>=4?6:3);f.setValue(text[i]);f.setResponder(s->text[index]=s);f.setTooltip(Tooltip.create(Component.literal(HELP[i])));fields[i]=addRenderableWidget(f);}
        addRenderableWidget(new InkButton(x+cw+8,y+3*rh,cw,16,Component.literal("Scenery: "+(ambience==0?"Original":AMBIENCE[ambience])),b->{capture();ambience=(ambience+1)%AMBIENCE.length;rebuildWidgets();}));
        int bottom=panelY()+panelH()-25;
        addRenderableWidget(new InkButton(x,bottom,44,18,Component.literal("Back"),b->onClose()));
        var mode=addRenderableWidget(new InkButton(x+48,bottom,105,18,Component.literal(open?"Open domain":"Closed domain"),b->{capture();open=!open;rebuildWidgets();}));mode.active=!pending&&canOpen();mode.setTooltip(Tooltip.create(Component.literal(canOpen()?"Open domains have no wall: wider, stronger, 60% more CE; enemies can leave.":"Unlimited Void, courtroom and gambling domains require a closed barrier.")));
        save=addRenderableWidget(new InkButton(panelX()+panelW()-96,bottom,87,18,Component.literal("Save domain"),b->{try{draft=parse();pending=true;reading=false;ticks=0;DomainEditor.accepted=false;ClientPlayNetworking.send(new DomainProfilePayload(draft,true));feedback="Saving…";}catch(IllegalArgumentException e){feedback=e.getMessage();} }));
        save.active=!pending&&ClientDomains.own().isEmpty();
    }
    private DomainProfile parse(){
        try{int[] v=new int[4];for(int i=0;i<4;i++){v[i]=Integer.parseInt(fields[i].getValue());if(v[i]<50||v[i]>200)throw new IllegalArgumentException("Percentages must be 50–200.");}
            int primary=rgb(fields[4].getValue()),secondary=rgb(fields[5].getValue());
            return new DomainProfile(v[0],v[1],v[2],v[3],primary,secondary,open&&canOpen(),fields[6].getValue(),AMBIENCE[ambience]);
        }catch(NumberFormatException e){throw new IllegalArgumentException("Use whole percentages and six-digit RGB colors.");}
    }
    private static int rgb(String s){if(s.isBlank())return -1;if(!s.matches("[0-9A-Fa-f]{6}"))throw new IllegalArgumentException("RGB colors need six hexadecimal digits.");return Integer.parseInt(s,16);}
    private void capture(){try{draft=parse();}catch(IllegalArgumentException ignored){}}
    @Override public void tick(){
        if(pending){ticks++;if(DomainEditor.revision!=revision){revision=DomainEditor.revision;pending=false;if(reading||DomainEditor.accepted){draft=DomainEditor.profile;open=draft.open()&&canOpen();text=null;ambience=Math.max(0,Arrays.asList(AMBIENCE).indexOf(draft.ambience()));feedback=reading?"Changes apply to your next expansion.":"Saved on server · use your Domain key to expand.";}else feedback="Save refused. Close your domain and check your values.";rebuildWidgets();}else if(ticks>100){pending=false;feedback="No save confirmed. Finish your active domain, then retry.";rebuildWidgets();}}
        if(save!=null)save.active=!pending&&ClientDomains.own().isEmpty();
    }
    @Override protected void extractContent(GuiGraphicsExtractor g,int mx,int my,float a){
        int x=panelX()+9,y=contentY()+3,cw=column(),rh=rowHeight();for(int i=0;i<7;i++)Ink.text(g,LABELS[i],x+(i%2)*(cw+8),y+i/2*rh,Palette.TEXT_DIM,1);
        String estimate="Learn a domain to preview its CE cost.";
        try{var p=parse();var definition=ClientDomains.self().known().flatMap(id->Domains.get(minecraft.level.registryAccess(),id));if(definition.isPresent()){var d=DomainCustomization.apply(definition.get(),p);double factor=p.open()?1.6:1;int growth=com.jjkcraft.client.state.ClientProgress.get().growth().domain();double discount=com.jjkcraft.core.domain.DomainMath.growthCostMultiplier(growth);estimate=String.format(Locale.ROOT,"Radius %.1fm · %.0f CE cast · %.1f CE/s",d.radius()*(p.open()?1.4:1),d.castCost()*factor*discount,d.upkeep()*factor*discount);}}catch(IllegalArgumentException ignored){estimate="Enter valid percentages and colors to preview.";}
        Ink.text(g,Ink.fit(Component.literal(estimate),panelW()-18),x,panelY()+panelH()-51,Palette.energyHigh(),1);
        Ink.text(g,Ink.fit(Component.literal(ClientDomains.own().isPresent()?"Close your active domain before saving.":feedback),panelW()-18),x,panelY()+panelH()-39,Palette.TEXT_DIM,1);
    }
    @Override public void onClose(){minecraft.gui.setScreen(parent);}
}
