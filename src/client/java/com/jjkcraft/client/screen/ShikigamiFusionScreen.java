package com.jjkcraft.client.screen;

import java.util.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import com.jjkcraft.JjkCraft;
import com.jjkcraft.client.state.*;
import com.jjkcraft.client.ui.*;
import com.jjkcraft.network.payload.ShikigamiPayloads;
import com.jjkcraft.technique.*;
import com.jjkcraft.technique.behavior.SummonBehavior;

/** The saved recipe is personal; the cast consumes living, owned component summons atomically. */
public final class ShikigamiFusionScreen extends JjkScreen {
    private static ShikigamiPayloads.Reply reply;
    public static void accept(ShikigamiPayloads.Reply next){reply=next;}
    private final Set<Identifier> selected=new LinkedHashSet<>();
    private int page,request,waiting;private boolean readSent,reading;private String feedback="Reading recipe…";
    public ShikigamiFusionScreen(){super(Component.literal("Shikigami fusion"));}
    private List<Techniques.Entry> options(){return ClientProgress.registries().map(r->Techniques.all(r).stream().filter(e->ClientTechniques.known().contains(e.id())&&e.definition().behavior() instanceof SummonBehavior b&&!b.entity().equals(JjkCraft.id("transfigured_soul"))).toList()).orElse(List.of());}
    private int rows(){return Math.max(2,(panelH()-128)/24);}
    private void send(boolean read){reading=read;request=ClientPersonalization.nextRequest();waiting=0;ClientPlayNetworking.send(new ShikigamiPayloads.Recipe(read?List.of():List.copyOf(selected),request));}
    @Override protected void initContent(){
        if(!readSent){readSent=true;send(true);}var options=options();int x=panelX()+10,y=contentY()+30,w=panelW()-20,col=(w-8)/2,count=rows()*2;
        page=Math.clamp(page,0,Math.max(0,(options.size()-1)/count));
        for(int n=page*count;n<Math.min(options.size(),(page+1)*count);n++){var e=options.get(n);var entity=((SummonBehavior)e.definition().behavior()).entity();int at=n-page*count;
            var b=addRenderableWidget(new InkButton(x+(at%2)*(col+8),y+(at/2)*24,col,21,Component.literal((selected.contains(entity)?"✓ ":"+ ")+Component.translatable(e.definition().name()).getString()),button->{if(!selected.remove(entity)&&selected.size()<4)selected.add(entity);feedback="Unsaved recipe";rebuildWidgets();}).selected(selected.contains(entity)));
            b.setTooltip(Tooltip.create(Component.literal("Summon this creature before casting your chimera. Two to four different types.")));
        }
        int bottom=panelY()+panelH()-25;
        addRenderableWidget(new InkButton(x,bottom,58,18,Component.literal("Back"),b->minecraft.gui.setScreen(new LoadoutScreen())));
        addRenderableWidget(new InkButton(x+62,bottom,28,18,Component.literal("<"),b->{page--;rebuildWidgets();}));
        addRenderableWidget(new InkButton(x+94,bottom,28,18,Component.literal(">"),b->{page++;rebuildWidgets();}));
        addRenderableWidget(new InkButton(x+126,bottom,65,18,Component.literal("Clear draft"),b->{selected.clear();rebuildWidgets();}));
        var save=addRenderableWidget(new InkButton(panelX()+panelW()-125,bottom,115,18,Component.literal("Save fusion recipe"),b->{send(false);feedback="Saving…";}));save.active=selected.size()>=2&&selected.size()<=4&&request==0&&ClientTechniques.known().contains(JjkCraft.id("chimera_fusion"));
    }
    @Override public void tick(){if(request!=0){if(reply!=null&&reply.request()==request){request=0;if(reply.accepted()){selected.clear();selected.addAll(reply.components());feedback=reading?"Recipe loaded":"Saved · summon components, then cast Chimera Fusion";}else feedback="Rejected · learn the selected summons first";rebuildWidgets();}else if(++waiting>100){request=0;feedback="No reply · retry";rebuildWidgets();}}}
    @Override protected void extractContent(GuiGraphicsExtractor g,int mx,int my,float a){int x=panelX()+10;Ink.text(g,"CHIMERA WORKSHOP · "+selected.size()+" / 4 COMPONENTS",x,contentY()+2,Palette.gold(),1);Ink.text(g,"Owned summons merge their attack, control and support abilities.",x,contentY()+15,Palette.TEXT_DIM,1);Ink.text(g,Ink.fit(Component.literal(feedback),panelW()-24),x,panelY()+panelH()-42,Palette.energyHigh(),1);}
}
