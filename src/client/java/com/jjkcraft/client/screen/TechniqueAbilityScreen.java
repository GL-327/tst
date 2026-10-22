package com.jjkcraft.client.screen;

import java.util.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import com.jjkcraft.JjkCraft;
import com.jjkcraft.awakening.CharacterAwakening;
import com.jjkcraft.awakening.InnateFamily;
import com.jjkcraft.client.state.*;
import com.jjkcraft.client.ui.*;
import com.jjkcraft.network.payload.*;
import com.jjkcraft.technique.*;
import com.jjkcraft.technique.behavior.*;

/** Searchable family management, using the same readiness and casting rules as the wheel. */
public final class TechniqueAbilityScreen extends JjkScreen {
    private final Identifier family;
    private Identifier selected;
    private int page,filter,refresh;
    private String search="";
    private boolean dirty,awaitingTrial,focusSelection=true;
    private EditBox query;
    private InkButton cast;
    private TechniqueMenuPayloads.View snapshot;
    private List<Identifier> rendered=List.of();
    private static final String[] FILTERS={"All","Learned","Ready","Untamed"};
    public TechniqueAbilityScreen(Identifier family){this(family,null);}
    public TechniqueAbilityScreen(Identifier family,Identifier selected){super(Component.literal("Technique abilities"));this.family=family;this.selected=selected;}
    private InnateFamily definition(){return minecraft.level==null?null:minecraft.level.registryAccess().lookupOrThrow(CharacterAwakening.FAMILIES).getValue(family);}
    private int left(){return panelX()+10;}private int column(){return (panelW()-30)/2;}private int right(){return left()+column()+10;}
    private int listY(){return contentY()+49;}private int rows(){return Math.max(2,(panelH()-133)/29);}
    private boolean known(Identifier id){return ClientTechniques.known().contains(id);}
    private boolean tamed(TechniqueDefinition d){return minecraft.player.isCreative()||ClientTechniqueMenus.allowed(d);}
    private int familySlot(){for(int n=0;n<ClientTechniques.state().loadout().size();n++)if(ClientTechniques.state().slot(n).filter(TechniqueFamilies.root(family)::equals).isPresent())return n;return -1;}
    public Identifier selection(){return selected;}
    public List<Identifier> visibleMoves(){var list=filtered();return list.subList(Math.min(list.size(),page*rows()),Math.min(list.size(),(page+1)*rows()));}
    private List<Identifier> filtered(){
        var f=definition();if(f==null)return List.of();String q=search.strip().toLowerCase(Locale.ROOT);
        return f.techniques().stream().filter(id->{var d=ClientTechniques.definition(id).orElse(null);if(d==null)return false;
            if(!q.isEmpty()&&!(id.getPath()+" "+Component.translatable(d.name()).getString()).toLowerCase(Locale.ROOT).contains(q))return false;
            return switch(filter){case 1->known(id);case 2->known(id)&&ClientTechniqueStatus.get(id).actionable();case 3->!tamed(d);default->true;};
        }).toList();
    }
    @Override protected void initContent(){
        var f=definition();if(f==null)return;
        var prior=query;boolean focused=prior!=null&&prior.isFocused();int caret=prior==null?search.length():prior.getCursorPosition();
        query=addRenderableWidget(new EditBox(Ink.font(),left(),contentY()+24,column(),18,Component.literal("Search abilities")));
        query.setMaxLength(64);query.setValue(search);query.setHint(Component.literal("Search abilities..."));
        query.setResponder(value->{search=value;page=0;dirty=true;});query.setCursorPosition(Math.min(caret,search.length()));if(focused)setFocused(query);
        addRenderableWidget(new InkButton(right(),contentY()+24,column(),18,Component.literal("Filter: "+FILTERS[filter]),b->{filter=(filter+1)%FILTERS.length;page=0;rebuildWidgets();}));
        var list=filtered();rendered=list;
        if(selected==null||!list.contains(selected))selected=list.isEmpty()?null:list.getFirst();
        if(focusSelection){if(selected!=null)page=list.indexOf(selected)/rows();focusSelection=false;}
        page=Math.clamp(page,0,Math.max(0,(list.size()-1)/rows()));
        for(int n=page*rows();n<Math.min(list.size(),(page+1)*rows());n++){
            var id=list.get(n);var d=ClientTechniques.definition(id).orElseThrow();int y=listY()+(n-page*rows())*29;
            var button=addRenderableWidget(new InkButton(left()+29,y,column()-29,25,Ink.fit(Component.translatable(d.name()),column()-39),b->{selected=id;rebuildWidgets();}).selected(id.equals(selected)));
            button.setTooltip(Tooltip.create(Component.literal(ClientTechniqueStatus.get(id).label()+" / "+Component.translatable(d.description()).getString())));
        }
        int bottom=panelY()+panelH()-25,w=column(),x=right();
        var back=addRenderableWidget(new InkButton(left(),bottom,28,18,Component.literal("<"),b->{page--;rebuildWidgets();}));back.active=page>0;
        var next=addRenderableWidget(new InkButton(left()+32,bottom,28,18,Component.literal(">"),b->{page++;rebuildWidgets();}));next.active=(page+1)*rows()<list.size();
        addRenderableWidget(new InkButton(left()+66,bottom,column()-66,18,Component.literal("Back to wheel"),b->{int slot=familySlot();minecraft.gui.setScreen(slot>=0?new TechniqueWheelScreen(TechniqueFamilies.root(family),slot):new FamilyTechniqueScreen());}));
        var d=selected==null?null:ClientTechniques.definition(selected).orElse(null);if(d==null)return;
        cast=addRenderableWidget(new InkButton(x,bottom-23,w/2-2,20,Component.empty(),b->activate()));
        cast.setTooltip(Tooltip.create(Component.literal(d.cast().chargeable()?"Quick cast at minimum charge. Use a direct binding or hold in the wheel to charge.":"The server validates energy, cooldown, grade and subjugation.")));
        var direct=addRenderableWidget(new InkButton(x+w/2+2,bottom-23,w/2-2,20,Component.literal("Equip direct"),b->{ClientPlayNetworking.send(new SetLoadoutPayload(ClientTechniques.state().selected(),Optional.of(selected)));onClose();}));direct.active=known(selected)&&tamed(d);
        var v=ClientTechniqueMenus.view(family);boolean ritual=family.equals(JjkCraft.id("ten_shadows"))&&v!=null&&!v.trial().isEmpty();
        var options=addRenderableWidget(new InkButton(x,bottom,w/2-2,18,Component.literal(ritual?"End ritual":d.behavior() instanceof FusionBehavior?"Fusion recipe":"Move options"),b->{
            if(ritual)ClientTechniqueMenus.request(family,selected,3);
            else minecraft.gui.setScreen(d.behavior() instanceof FusionBehavior?new ShikigamiFusionScreen():new TechniqueWorkshopScreen(selected));
        }));options.active=ritual||known(selected);if(ritual)options.setTooltip(Tooltip.create(Component.literal("Ends your own ritual without granting an unlock. Spent CE is not refunded.")));
        var dismiss=addRenderableWidget(new InkButton(x+w/2+2,bottom,w/2-2,18,Component.literal(v!=null&&v.owned()>0?"Dismiss ("+v.owned()+")":"No summons"),b->ClientTechniqueMenus.request(family,selected,2)));dismiss.active=v!=null&&v.owned()>0;
        updateCast();
    }
    private void updateCast(){
        if(cast==null||selected==null)return;var d=ClientTechniques.definition(selected).orElse(null);if(d==null)return;
        boolean untamed=!tamed(d),infinity=selected.equals(JjkCraft.id("infinity"));var v=ClientTechniqueMenus.view(family);
        boolean ritual=untamed&&d.behavior() instanceof SummonBehavior;boolean activeTrial=v!=null&&!v.trial().isEmpty();
        boolean fist=selected.getPath().equals("blue_punch")||selected.getPath().equals("red_punch");
        String label=infinity?(ClientTechniques.infinityEnabled()?"Disable Infinity":"Enable Infinity"):untamed?(ritual?(activeTrial?"Ritual active":"Subjugate"):"Tame components"):familySlot()<0?"Equip family":fist?(ClientTechniqueStatus.get(selected).label().startsWith("Fists imbued")?"Clear fists":"Imbue fists"):ClientTechniques.activeIs(selected)?"Turn off":d.behavior() instanceof SummonBehavior?"Summon":"Cast";
        cast.setMessage(Component.literal(label));
        cast.active=known(selected)&&(untamed?ritual&&!activeTrial:familySlot()<0&&!infinity||ClientTechniqueStatus.get(selected).actionable());
    }
    private void activate(){
        if(selected==null)return;var d=ClientTechniques.definition(selected).orElse(null);if(d==null)return;
        if(!tamed(d)){awaitingTrial=true;ClientTechniqueMenus.request(family,selected,1);return;}
        int slot=familySlot();
        if(slot<0&&!selected.equals(JjkCraft.id("infinity"))){ClientPlayNetworking.send(new SetLoadoutPayload(ClientTechniques.state().selected(),Optional.of(TechniqueFamilies.root(family))));onClose();return;}
        ClientPlayNetworking.send(new CastTechniquePayload(selected));onClose();
    }
    @Override public void tick(){
        var v=ClientTechniqueMenus.view(family);
        if(v!=snapshot&&v!=null){snapshot=v;if(awaitingTrial&&!v.trial().isEmpty()){onClose();return;}awaitingTrial=false;dirty=true;}
        if(++refresh%20==0&&selected!=null)ClientTechniqueMenus.request(family,selected,0);
        if(!rendered.equals(filtered()))dirty=true;
        if(dirty){boolean focused=query!=null&&(query.isFocused()||getFocused()==query);int caret=query==null?0:query.getCursorPosition();dirty=false;cast=null;rebuildWidgets();if(focused){setFocused(query);query.setFocused(true);query.setCursorPosition(Math.min(caret,search.length()));}}else updateCast();
    }
    @Override public boolean mouseScrolled(double x,double y,double sx,double sy){if(y<listY())return super.mouseScrolled(x,y,sx,sy);page-=(int)Math.signum(sy);rebuildWidgets();return true;}
    @Override protected void extractContent(GuiGraphicsExtractor g,int mx,int my,float a){
        var f=definition();if(f==null)return;var list=filtered();
        Ink.text(g,Component.translatable(f.name()),left(),contentY()+2,Palette.gold(),1);
        Ink.text(g,list.size()+(list.size()==1?" ability / page ":" abilities / page ")+(page+1)+" of "+Math.max(1,(list.size()+rows()-1)/rows()),left(),contentY()+14,Palette.TEXT_DIM,1);
        for(int n=page*rows();n<Math.min(list.size(),(page+1)*rows());n++){
            var id=list.get(n);var d=ClientTechniques.definition(id).orElseThrow();int y=listY()+(n-page*rows())*29;
            g.blit(RenderPipelines.GUI_TEXTURED,d.icon(),left(),y,0,0,24,24,24,24);g.fill(left(),y+23,left()+24,y+25,ClientTechniqueStatus.get(id).color());
        }
        if(list.isEmpty())Ink.text(g,"No matching abilities",left(),listY()+4,Palette.TEXT_DIM,1);
        var d=selected==null?null:ClientTechniques.definition(selected).orElse(null);if(d==null)return;int x=right(),y=listY(),w=column();
        Ink.text(g,Ink.fit(Component.translatable(d.name()),w),x,y,Palette.TEXT,1);var status=ClientTechniqueStatus.get(selected);
        Ink.text(g,Ink.fit(Component.literal(status.label()),w),x,y+15,status.color(),1);
        Ink.text(g,Ink.fit(Component.literal(String.format(Locale.ROOT,"%.0f CE / %.1fs cooldown",ClientTechniques.cost(selected),d.cast().cooldown()/20.0)),w),x,y+28,Palette.energyHigh(),1);
        int dy=y+43,max=Math.max(1,(panelY()+panelH()-66-dy)/10),n=0;
        for(var line:Ink.font().split(Component.translatable(d.description()),w)){if(n++>=max)break;g.text(Ink.font(),line,x,dy,Palette.TEXT_DIM,true);dy+=10;}
        String feedback=ClientTechniqueMenus.feedback(family);if(!feedback.isEmpty())Ink.text(g,Ink.fit(Component.literal(feedback),w),x,panelY()+panelH()-61,Palette.gold(),1);
    }
}
