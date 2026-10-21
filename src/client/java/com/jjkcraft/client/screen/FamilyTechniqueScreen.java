package com.jjkcraft.client.screen;

import java.util.*;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import com.jjkcraft.awakening.CharacterAwakening;
import com.jjkcraft.client.state.*;
import com.jjkcraft.client.ui.*;

/** The family and its individual moves are visible together; a move opens its actual loadout entry. */
public final class FamilyTechniqueScreen extends JjkScreen {
    private Identifier selected;
    private int offset,moveOffset;
    private String search="";
    private InkButton bind;
    public FamilyTechniqueScreen(){super(Component.literal("Technique families"));}
    private int x(){return panelX()+10;}private int y(){return contentY()+27;}private int col(){return (panelW()-30)*2/5;}private int rx(){return x()+col()+10;}
    private int rows(){return Math.max(2,(panelH()-92)/29);}
    private List<Identifier> families(){return minecraft.level.registryAccess().lookupOrThrow(CharacterAwakening.FAMILIES).keySet().stream().sorted().filter(id->(id.getPath()+" "+Component.translatable(minecraft.level.registryAccess().lookupOrThrow(CharacterAwakening.FAMILIES).getValue(id).name()).getString()).toLowerCase(Locale.ROOT).contains(search.toLowerCase(Locale.ROOT))).toList();}
    @Override protected void initContent(){
        if(selected==null){selected=Optional.ofNullable(Identifier.tryParse(ClientAwakening.view().record().family())).orElseGet(()->families().getFirst());offset=Math.max(0,families().indexOf(selected));}
        var box=addRenderableWidget(new EditBox(Ink.font(),x(),contentY()+2,col(),18,Component.literal("Search families")));box.setValue(search);box.setHint(Component.literal("Find a technique family…"));box.setResponder(s->{search=s;offset=0;});
        addRenderableWidget(new InkButton(x(),panelY()+panelH()-24,70,18,Component.literal("Loadout"),b->minecraft.gui.setScreen(new LoadoutScreen())));
        addRenderableWidget(new InkButton(rx(),panelY()+panelH()-24,35,18,Component.literal("<"),b->moveOffset=Math.max(0,moveOffset-rows())));
        addRenderableWidget(new InkButton(panelX()+panelW()-45,panelY()+panelH()-24,35,18,Component.literal(">"),b->moveOffset+=rows()));
        bind=addRenderableWidget(new InkButton(rx()+39,panelY()+panelH()-24,Math.max(50,panelX()+panelW()-rx()-88),18,Component.literal("Bind family"),b->{
            var binding=com.jjkcraft.technique.TechniqueBindings.binding(selected);
            net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(new com.jjkcraft.network.payload.SetLoadoutPayload(ClientTechniques.state().selected(),Optional.of(binding)));
            minecraft.gui.setScreen(new LoadoutScreen(binding));
        }));
    }
    @Override protected void extractContent(GuiGraphicsExtractor g,int mx,int my,float a){
        bind.active=selected!=null&&ClientTechniques.bindable(com.jjkcraft.technique.TechniqueBindings.binding(selected));
        var ids=families();offset=Math.clamp(offset,0,Math.max(0,ids.size()-rows()));
        for(int n=offset;n<Math.min(ids.size(),offset+rows());n++){var id=ids.get(n);var f=minecraft.level.registryAccess().lookupOrThrow(CharacterAwakening.FAMILIES).getValue(id);int yy=y()+(n-offset)*29;
            Ink.flatPanel(g,x(),yy,col(),27,1);if(id.equals(selected))g.fill(x(),yy,x()+2,yy+27,Palette.accent());
            Ink.text(g,Ink.fit(Component.translatable(f.name()),col()-12),x()+6,yy+4,id.equals(selected)?Palette.TEXT:Palette.TEXT_DIM,1);
            Ink.text(g,f.techniques().size()+" moves"+(f.clan().isEmpty()?"":" · "+f.clan()),x()+6,yy+16,Palette.energyHigh(),1);
        }
        var f=minecraft.level.registryAccess().lookupOrThrow(CharacterAwakening.FAMILIES).getValue(selected);if(f==null)return;
        Ink.text(g,Ink.fit(Component.translatable(f.name()),panelX()+panelW()-rx()-10),rx(),contentY()+5,Palette.gold(),1);
        moveOffset=Math.clamp(moveOffset,0,Math.max(0,f.techniques().size()-rows()));
        for(int n=moveOffset;n<Math.min(f.techniques().size(),moveOffset+rows());n++){var id=f.techniques().get(n);var d=ClientTechniques.definition(id).orElse(null);if(d==null)continue;int yy=y()+(n-moveOffset)*29,ww=panelX()+panelW()-rx()-10;
            Ink.flatPanel(g,rx(),yy,ww,27,1);g.blit(RenderPipelines.GUI_TEXTURED,d.icon(),rx()+3,yy+3,0,0,21,21,21,21);
            Ink.text(g,Ink.fit(Component.translatable(d.name()),ww-32),rx()+29,yy+4,Palette.TEXT,1);
            Ink.text(g,Ink.fit(Component.literal(ClientTechniques.known().contains(id)?"LEARNED · equip / personalize":"Unlock at "+Component.translatable("grade."+d.grade().getNamespace()+"."+d.grade().getPath()).getString()),ww-32),rx()+29,yy+16,ClientTechniques.known().contains(id)?Palette.good():Palette.TEXT_DIM,1);
            if(mx>=rx()&&mx<rx()+ww&&my>=yy&&my<yy+27)g.setComponentTooltipForNextFrame(Ink.font(),TechniqueTooltip.lines(id,d),mx,my);
        }
    }
    @Override public boolean mouseClicked(MouseButtonEvent e,boolean twice){
        if(e.button()==InputConstants.MOUSE_BUTTON_LEFT&&e.y()>=y()&&e.y()<y()+rows()*29){int row=(int)(e.y()-y())/29;
            if(e.x()>=x()&&e.x()<x()+col()){var ids=families();if(offset+row<ids.size()){selected=ids.get(offset+row);moveOffset=0;}return true;}
            if(e.x()>=rx()&&e.x()<panelX()+panelW()-10){var f=minecraft.level.registryAccess().lookupOrThrow(CharacterAwakening.FAMILIES).getValue(selected);if(moveOffset+row<f.techniques().size())minecraft.gui.setScreen(new LoadoutScreen(f.techniques().get(moveOffset+row)));return true;}
        }return super.mouseClicked(e,twice);
    }
    @Override public boolean mouseScrolled(double mx,double my,double sx,double sy){if(mx<rx())offset=Math.max(0,offset-(int)Math.signum(sy));else moveOffset=Math.max(0,moveOffset-(int)Math.signum(sy));return true;}
}
