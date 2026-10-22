package com.jjkcraft.client.screen;

import java.util.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import com.jjkcraft.JjkCraft;
import com.jjkcraft.awakening.CharacterAwakening;
import com.jjkcraft.awakening.InnateFamily;
import com.jjkcraft.client.state.*;
import com.jjkcraft.client.ui.*;
import com.jjkcraft.entity.shikigami.TenShadows;
import com.jjkcraft.network.payload.*;
import com.jjkcraft.technique.*;
import com.jjkcraft.technique.behavior.*;

/** One family power, explicit variants/options, and authoritative Ten Shadows subjugation. */
public final class TechniqueAbilityScreen extends JjkScreen {
    private final Identifier family;
    private Identifier selected;
    private int page;
    private boolean read,awaitingTrial;
    private TechniqueMenuPayloads.View snapshot;
    public TechniqueAbilityScreen(Identifier family){this(family,null);}
    public TechniqueAbilityScreen(Identifier family,Identifier selected){super(Component.literal("Technique abilities"));this.family=family;this.selected=selected;}
    private InnateFamily definition(){return minecraft.level==null?null:minecraft.level.registryAccess().lookupOrThrow(CharacterAwakening.FAMILIES).getValue(family);}
    private int left(){return panelX()+10;}private int column(){return (panelW()-30)/2;}private int right(){return left()+column()+10;}
    private int rows(){return Math.max(2,(panelH()-96)/29);}
    private boolean known(Identifier id){return ClientTechniques.known().contains(id);}
    private boolean tamed(TechniqueDefinition d){
        if(minecraft.player.isCreative())return true;
        if(d.behavior() instanceof FusionBehavior f)return f.components().stream().allMatch(id->!TenShadows.BEASTS.contains(id)||ClientTechniqueMenus.tamed().contains(id));
        return !(d.behavior() instanceof SummonBehavior b)||!TenShadows.BEASTS.contains(b.entity())||ClientTechniqueMenus.tamed().contains(b.entity());
    }
    private boolean familyEquipped(){return ClientTechniques.state().loadout().stream().anyMatch(s->s.filter(TechniqueFamilies.root(family)::equals).isPresent());}
    @Override protected void initContent(){
        var f=definition();if(f==null)return;
        if(selected==null||!f.techniques().contains(selected))selected=f.techniques().getFirst();
        if(!read){read=true;ClientTechniqueMenus.request(family,selected,0);}
        page=Math.clamp(page,0,Math.max(0,(f.techniques().size()-1)/rows()));
        for(int n=page*rows();n<Math.min(f.techniques().size(),(page+1)*rows());n++){
            var id=f.techniques().get(n);var d=ClientTechniques.definition(id).orElse(null);if(d==null)continue;
            int y=contentY()+31+(n-page*rows())*29;
            var button=addRenderableWidget(new InkButton(left()+29,y,column()-29,25,Ink.fit(Component.translatable(d.name()),column()-39),b->{selected=id;rebuildWidgets();}).selected(id.equals(selected)));
            button.setTooltip(Tooltip.create(Component.literal(!known(id)?"Grade locked":!tamed(d)?"Untamed · defeat in a subjugation ritual":d.cast().chargeable()?"Hold a direct key to charge; release to cast":"Ready · choose for details")));
        }
        int bottom=panelY()+panelH()-25,w=column(),x=right();
        addRenderableWidget(new InkButton(left(),bottom,28,18,Component.literal("<"),b->{page--;rebuildWidgets();}));
        addRenderableWidget(new InkButton(left()+32,bottom,28,18,Component.literal(">"),b->{page++;rebuildWidgets();}));
        addRenderableWidget(new InkButton(left()+66,bottom,column()-66,18,Component.literal("Families"),b->minecraft.gui.setScreen(new FamilyTechniqueScreen())));
        var d=ClientTechniques.definition(selected).orElse(null);if(d==null)return;
        boolean untamed=!tamed(d),learned=known(selected);
        boolean ritual=untamed&&d.behavior() instanceof SummonBehavior;
        boolean infinity=selected.equals(JjkCraft.id("infinity"));
        String action=infinity?(ClientTechniques.infinityEnabled()?"Disable Infinity":"Enable Infinity"):untamed?ritual?"Subjugate":"Tame components":familyEquipped()?d.behavior() instanceof SummonBehavior?"Summon":ClientTechniques.activeIs(selected)?"Turn off":"Cast":"Equip family";
        var cast=addRenderableWidget(new InkButton(x,bottom-23,w/2-2,20,Component.literal(action),b->{
            if(untamed){awaitingTrial=true;ClientTechniqueMenus.request(family,selected,1);}
            else if(!infinity&&!familyEquipped()){ClientPlayNetworking.send(new SetLoadoutPayload(ClientTechniques.state().selected(),Optional.of(TechniqueFamilies.root(family))));onClose();}
            else{
                int slot=-1;for(int n=0;n<ClientTechniques.state().loadout().size();n++)if(ClientTechniques.state().slot(n).filter(TechniqueFamilies.root(family)::equals).isPresent()){slot=n;break;}
                if(slot>=0){ClientPlayNetworking.send(new VariantCastPayload(slot,selected));ClientPlayNetworking.send(new CastPayload(slot,false));}
                else ClientPlayNetworking.send(new CastTechniquePayload(selected));
                onClose();
            }
        }));cast.active=learned&&(!untamed||ritual&&family.equals(JjkCraft.id("ten_shadows")));
        cast.setTooltip(Tooltip.create(Component.literal(untamed?"Defeat it yourself within three minutes. Stay within 48 blocks. Outside interference fails the ritual.":d.cast().chargeable()?"Quick cast at minimum charge. Equip direct to use hold and release charging.":"The server checks energy, grade and cooldown.")));
        var direct=addRenderableWidget(new InkButton(x+w/2+2,bottom-23,w/2-2,20,Component.literal("Equip direct"),b->{ClientPlayNetworking.send(new SetLoadoutPayload(ClientTechniques.state().selected(),Optional.of(selected)));onClose();}));direct.active=learned&&!untamed;
        var options=addRenderableWidget(new InkButton(x,bottom,w/2-2,18,Component.literal(d.behavior() instanceof FusionBehavior?"Fusion recipe":"Move options"),b->minecraft.gui.setScreen(d.behavior() instanceof FusionBehavior?new ShikigamiFusionScreen():new TechniqueWorkshopScreen(selected))));options.active=learned;
        addRenderableWidget(new InkButton(x+w/2+2,bottom,w/2-2,18,Component.literal("Dismiss summons"),b->ClientTechniqueMenus.request(family,selected,2)));
    }
    @Override public void tick(){
        var v=ClientTechniqueMenus.view();
        if(v!=snapshot&&v!=null&&v.family().equals(family)){
            snapshot=v;if(awaitingTrial&&!v.trial().isEmpty()){onClose();return;}awaitingTrial=false;rebuildWidgets();
        }
    }
    @Override public boolean mouseScrolled(double x,double y,double sx,double sy){page-=Math.signum(sy);rebuildWidgets();return true;}
    @Override protected void extractContent(GuiGraphicsExtractor g,int mx,int my,float a){
        var f=definition();if(f==null)return;
        Ink.text(g,Component.translatable(f.name()),left(),contentY()+2,Palette.gold(),1);
        Ink.text(g,"ONE FAMILY POWER · "+f.techniques().size()+" ABILITIES",left(),contentY()+16,Palette.TEXT_DIM,1);
        for(int n=page*rows();n<Math.min(f.techniques().size(),(page+1)*rows());n++){
            var id=f.techniques().get(n);var d=ClientTechniques.definition(id).orElse(null);if(d==null)continue;
            int y=contentY()+32+(n-page*rows())*29;
            g.blit(RenderPipelines.GUI_TEXTURED,d.icon(),left(),y,0,0,24,24,24,24);
            if(!known(id)||!tamed(d))g.fill(left(),y+22,left()+24,y+24,known(id)?0xffce8f54:0xff565d70);
        }
        var d=ClientTechniques.definition(selected).orElse(null);if(d==null)return;int x=right(),y=contentY()+32,w=column();
        Ink.text(g,Ink.fit(Component.translatable(d.name()),w),x,y,Palette.TEXT,1);
        String status=selected.equals(JjkCraft.id("infinity"))?ClientTechniques.infinityActive()?"INFINITY ACTIVE":ClientTechniques.infinityEnabled()?"INFINITY ENABLED · RECOVERING":"INFINITY DISABLED":!known(selected)?"GRADE LOCKED":!tamed(d)?"UNTAMED · RITUAL REQUIRED":minecraft.player.isCreative()?"CREATIVE PRACTICE":d.behavior() instanceof SummonBehavior?"SUBJUGATED / READY":"LEARNED";
        Ink.text(g,Ink.fit(Component.literal(status),w),x,y+15,tamed(d)?Palette.good():Palette.gold(),1);
        Ink.text(g,Math.round(d.cast().cost())+" CE · "+String.format(Locale.ROOT,"%.1fs cooldown",d.cast().cooldown()/20f),x,y+28,Palette.energyHigh(),1);
        int lineY=y+43,max=Math.max(1,(panelH()-155)/10),n=0;
        for(var line:Ink.font().split(Component.translatable(d.description()),w)){if(n++>=max)break;g.text(Ink.font(),line,x,lineY,Palette.TEXT_DIM,true);lineY+=10;}
        var v=ClientTechniqueMenus.view();if(v!=null&&v.family().equals(family)&&!v.feedback().isEmpty())Ink.text(g,Ink.fit(Component.literal(v.feedback()),panelW()-20),left(),panelY()+panelH()-60,Palette.gold(),1);
    }
}
