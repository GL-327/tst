package com.jjkcraft.client.screen;

import java.util.*;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.*;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import com.jjkcraft.client.JjkKeys;
import com.jjkcraft.client.state.ClientTechniques;
import com.jjkcraft.client.ui.*;
import com.jjkcraft.network.payload.*;
import com.jjkcraft.technique.*;

/** Hold/scroll to select one of twelve slots. A family opens its learned variants. */
public final class TechniqueWheelScreen extends Screen {
    private final Identifier family;
    private final int slot;
    private List<Optional<Identifier>> entries=List.of();
    private int hovered,page,held=-1,heldMouse=-1;
    private double lastX=Double.NaN,lastY;
    private boolean scrolling=true,finishing,quick;
    private int branch;
    private boolean restored;
    private InkButton ceButton;
    private static final String[] BRANCHES={"All","Blue","Red","Infinity","Purple"};
    public TechniqueWheelScreen(){this(null,ClientTechniques.state().selected());}
    public TechniqueWheelScreen(Identifier family,int slot){super(Component.literal("Technique wheel"));this.family=family;this.slot=slot;hovered=family==null?slot:0;if(family!=null)branch=com.jjkcraft.client.state.ClientTechniqueMenus.branch(family);}
    @Override protected void init(){
        entries=family==null?ClientTechniques.state().loadout():ClientTechniques.variants(family).stream().filter(id->inBranch(id,branch)).map(Optional::of).toList();
        if(family!=null&&!restored){
            if(entries.isEmpty()&&branch!=0){branch=0;entries=ClientTechniques.variants(family).stream().map(Optional::of).toList();}
            var remembered=com.jjkcraft.client.state.ClientTechniqueMenus.selected(family);for(int i=0;i<entries.size();i++)if(entries.get(i).filter(id->id.equals(remembered)).isPresent())hovered=i;
            restored=true;
        }
        hovered=Math.clamp(hovered,0,Math.max(0,entries.size()-1));page=hovered/12;
        if(limitless()&&width>=320)for(int i=0;i<BRANCHES.length;i++){final int b=i;var button=addRenderableWidget(new InkButton(7,height/2-49+i*20,57,18,Component.literal(BRANCHES[i]),v->{branch=b;page=hovered=0;scrolling=true;rebuildWidgets();}).selected(i==branch));button.active=held<0&&ClientTechniques.variants(family).stream().anyMatch(id->inBranch(id,b));}
        if(family!=null&&family.getPath().equals("family/ten_shadows"))addRenderableWidget(new InkButton(7,35,90,20,Component.literal("Subjugation"),b->minecraft.gui.setScreen(new TechniqueAbilityScreen(com.jjkcraft.JjkCraft.id("ten_shadows")))));
        if(family!=null){
            var id=Identifier.fromNamespaceAndPath(family.getNamespace(),family.getPath().substring(7));
            addRenderableWidget(new InkButton(width-87,35,80,20,Component.literal("Abilities"),b->minecraft.gui.setScreen(new TechniqueAbilityScreen(id,entry(hovered).orElse(null)))));
            if(ClientTechniques.variants(family).stream().map(ClientTechniques::definition).flatMap(Optional::stream).anyMatch(d->d.behavior() instanceof com.jjkcraft.technique.behavior.SummonBehavior||d.behavior() instanceof com.jjkcraft.technique.behavior.FusionBehavior))
                addRenderableWidget(new InkButton(width-107,58,100,20,Component.literal("Dismiss summons"),b->com.jjkcraft.client.state.ClientTechniqueMenus.request(id,entry(hovered).orElse(com.jjkcraft.JjkCraft.id("ten_shadows_divine_dogs")),2)));
            com.jjkcraft.client.state.ClientTechniqueMenus.request(id,entry(hovered).orElse(com.jjkcraft.JjkCraft.id("infinity")),0);
        }
        addRenderableWidget(new InkButton(width-107,105,100,20,Component.literal("Domain workshop"),b->minecraft.gui.setScreen(new DomainWorkshopScreen(this))));
        ceButton=addRenderableWidget(new InkButton(width-107,81,100,20,Component.literal("CE fists: "+(com.jjkcraft.client.combat.PunchImbuementClient.ce()?"ON":"OFF")),b->ClientPlayNetworking.send(new CombatPayloads.Input(8,0))));
    }
    private boolean limitless(){return family!=null&&family.getPath().equals("family/limitless");}
    @Override public void tick(){if(ceButton!=null)ceButton.setMessage(Component.literal("CE fists: "+(com.jjkcraft.client.combat.PunchImbuementClient.ce()?"ON":"OFF")));}
    private boolean untamed(Identifier id){return minecraft.player!=null&&!minecraft.player.isCreative()&&ClientTechniques.definition(id).filter(d->!com.jjkcraft.client.state.ClientTechniqueMenus.allowed(d)).isPresent();}
    private static boolean inBranch(Identifier id,int branch){String p=id.getPath();return switch(branch){case 1->p.contains("blue");case 2->p.contains("red");case 3->p.equals("infinity");case 4->p.contains("purple");default->true;};}
    public int entryCount(){return entries.size();}
    public Optional<Identifier> entry(int index){return index>=0&&index<entries.size()?entries.get(index):Optional.empty();}
    private static boolean mouse(net.minecraft.client.KeyMapping key,int button){var bound=KeyMappingHelper.getBoundKeyOf(key);return bound.getType()==InputConstants.Type.MOUSE&&bound.getValue()==button;}
    private int pinButton(){return mouse(JjkKeys.CAST,InputConstants.MOUSE_BUTTON_RIGHT)?InputConstants.MOUSE_BUTTON_MIDDLE:InputConstants.MOUSE_BUTTON_RIGHT;}
    @Override public boolean isPauseScreen(){return false;}
    public int selection(){return hovered;}
    public boolean isFamilyWheel(){return family!=null;}
    private int centerY(){return height/2-8;}
    public int[] nodePoint(int index){int count=Math.min(12,entries.size()-page*12);double angle=-Math.PI/2+2*Math.PI*(index-page*12)/Math.max(1,count);return new int[]{width/2+(int)(Math.cos(angle)*radius()),centerY()+(int)(Math.sin(angle)*radius())};}
    private int radius(){return Math.max(42,Math.min(94,height/2-60));}
    @Override public void extractBackground(GuiGraphicsExtractor g,int x,int y,float a){g.fill(0,0,width,height,0xB007080E);}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float a){
        int cx=width/2,cy=centerY(),count=Math.min(12,entries.size()-page*12);
        if(Double.isNaN(lastX)){lastX=mx;lastY=my;}
        if(Math.hypot(mx-lastX,my-lastY)>3){scrolling=false;lastX=mx;lastY=my;}
        double dx=mx-cx,dy=my-cy;
        if(!scrolling&&dx*dx+dy*dy>(radius()-28)*(radius()-28)&&dx*dx+dy*dy<(radius()+24)*(radius()+24)&&count>0){double angle=Math.atan2(dy,dx)+Math.PI/2;if(angle<0)angle+=Math.PI*2;hovered=page*12+(int)Math.floor((angle+Math.PI/count)/(Math.PI*2/count))%count;}
        Ink.ring(g,cx,cy,radius(),2,1,Palette.INK_EDGE);
        Ink.panel(g,cx-45,cy-28,90,60,Palette.INK_EDGE,1);
        for(int i=page*12;i<Math.min(entries.size(),page*12+12);i++){
            var id=entries.get(i);var d=id.flatMap(ClientTechniques::definition);int[] p=nodePoint(i);boolean hot=i==hovered;int r=hot?13:12;
            int color=d.map(v->v.visuals().color()).orElse(Palette.INK_EDGE);
            Ink.diamond(g,p[0],p[1],r+3,hot?Palette.gold():color);g.fill(p[0]-r+3,p[1]-r+3,p[0]+r-3,p[1]+r-3,0xFF100F1B);
            if(d.isPresent())g.blit(RenderPipelines.GUI_TEXTURED,d.get().icon(),p[0]-10,p[1]-10,0,0,20,20,20,20);
            if(id.isPresent()&&TechniqueBindings.family(id.get()))Ink.text(g,"+",p[0]+7,p[1]-15,Palette.gold(),1);
            if(family==null)Ink.centered(g,Component.literal(""+(i+1)),p[0],p[1]+r+2,hot?Palette.gold():Palette.TEXT_DIM,1);
            if(id.isPresent()&&ClientTechniques.cooldownFraction(id.get())>0)Ink.ring(g,p[0],p[1],r+1,2,1-ClientTechniques.cooldownFraction(id.get()),Palette.TEXT_DIM);
        }
        String title=family==null?"POWER SLOTS":ClientTechniques.definition(family).map(d->Component.translatable(d.name()).getString()).orElse("VARIANTS");
        Ink.centered(g,Component.literal(title),cx,7,Palette.gold(),1);
        if(hovered>=0&&hovered<entries.size()){
            var id=entries.get(hovered);var d=id.flatMap(ClientTechniques::definition);
            var name=d.map(v->Component.translatable(v.name())).orElse(Component.literal("Empty slot"));
            Ink.centered(g,Ink.fit(name,width-14),cx,20,Palette.TEXT,1);
            String shortName=name.getString();if(Ink.font().width(shortName)>104&&shortName.contains(":"))shortName=shortName.substring(shortName.lastIndexOf(':')+1).strip();
            Ink.centered(g,Ink.fit(Component.literal(shortName),84),cx,cy-14,Palette.TEXT,1);
            String detail=family==null?"Slot "+(hovered+1)+(id.filter(TechniqueBindings::family).isPresent()?" / family":""):d.map(v->String.format(Locale.ROOT,"%.0f CE",ClientTechniques.cost(id.orElseThrow()))).orElse("");
            Ink.centered(g,Ink.fit(Component.literal(detail),84),cx,cy+1,Palette.energyHigh(),1);
            var failure=ClientTechniques.recentFailure(slot);if(family!=null&&failure!=CastFailure.NONE)Ink.centered(g,Ink.fit(Component.translatable(failure.translationKey()),84),cx,cy+15,Palette.danger(),1);
            else if(family!=null&&id.isPresent()&&d.isPresent()&&held<0){var status=com.jjkcraft.client.state.ClientTechniqueStatus.get(id.get());Ink.centered(g,Ink.fit(Component.literal(status.label()),84),cx,cy+15,status.color(),1);}
        }
        var charge=ClientTechniques.charge();if(held>=0&&charge.slot()>=0)Ink.centered(g,Component.literal("CHARGE "+charge.percent()+"%"),cx,cy+18,Palette.accent(),1);
        String hint=held>=0?"Release to cast / Esc cancels":family==null?"Scroll / point · release "+JjkKeys.WHEEL.getTranslatedKeyMessage().getString()+" to select":"Hold click / "+JjkKeys.CAST.getTranslatedKeyMessage().getString()+" to cast · "+(pinButton()==InputConstants.MOUSE_BUTTON_RIGHT?"right":"middle")+" click pins move";
        Ink.centered(g,Ink.fit(Component.literal(hint),width-12),cx,height-21,Palette.TEXT_DIM,1);
        Ink.centered(g,Component.literal(family==null?"Click a family to browse its powers":"Scroll through learned moves · Esc cancels"),cx,height-10,Palette.TEXT_DIM,1);
        if(entries.size()>12)Ink.text(g,(page+1)+" / "+((entries.size()+11)/12),width-40,7,Palette.TEXT_DIM,1);
        if(family!=null){var id=Identifier.fromNamespaceAndPath(family.getNamespace(),family.getPath().substring(7));String feedback=com.jjkcraft.client.state.ClientTechniqueMenus.feedback(id);if(!feedback.isEmpty())Ink.centered(g,Ink.fit(Component.literal(feedback),Math.max(100,width-210)),cx,33,Palette.gold(),1);}
        com.jjkcraft.client.hud.DomainMeterHud.wheel(g,width,height);
        super.extractRenderState(g,mx,my,a);
    }
    @Override public boolean mouseScrolled(double x,double y,double sx,double sy){
        if(held>=0||entries.isEmpty())return true;
        hovered=Math.floorMod(hovered-(int)Math.signum(sy),entries.size());page=hovered/12;scrolling=true;return true;
    }
    private void select(){if(hovered>=0&&hovered<entries.size())ClientPlayNetworking.send(new SelectSlotPayload(hovered));onClose();}
    private void begin(int mouse){if(held>=0||hovered<0||hovered>=entries.size()||entries.get(hovered).isEmpty())return;
        var id=entries.get(hovered).orElseThrow();var d=ClientTechniques.definition(id).orElse(null);
        if(d!=null&&minecraft.player!=null&&!minecraft.player.isCreative()&&!com.jjkcraft.client.state.ClientTechniqueMenus.allowed(d)){
            minecraft.gui.setScreen(new TechniqueAbilityScreen(com.jjkcraft.JjkCraft.id("ten_shadows"),id));return;
        }
        held=hovered;heldMouse=mouse;quick=d!=null&&!d.cast().chargeable()&&d.cast().mode()!=CastMode.CHANNEL;
        if(quick)ClientPlayNetworking.send(new CastTechniquePayload(id));else ClientPlayNetworking.send(new VariantCastPayload(slot,id));}
    private void finish(){if(held<0)return;finishing=true;if(!quick)ClientPlayNetworking.send(new CastPayload(slot,false));held=-1;onClose();}
    @Override public boolean mouseClicked(MouseButtonEvent e,boolean twice){
        if(e.x()>=width-107&&e.y()>=35&&e.y()<126)return super.mouseClicked(e,twice);
        if(family!=null&&family.getPath().equals("family/ten_shadows")&&e.x()>=7&&e.x()<97&&e.y()>=35&&e.y()<55)return super.mouseClicked(e,twice);
        if(limitless()&&width>=320&&e.x()>=7&&e.x()<64&&e.y()>=height/2-49&&e.y()<height/2+49)return super.mouseClicked(e,twice);
        if(hovered<0||hovered>=entries.size())return true;
        int[] node=nodePoint(hovered);if(Math.pow(e.x()-node[0],2)+Math.pow(e.y()-node[1],2)>26*26&&Math.pow(e.x()-width/2.0,2)+Math.pow(e.y()-centerY(),2)>40*40)return super.mouseClicked(e,twice);
        if(family==null){var id=entries.get(hovered);if(id.filter(TechniqueBindings::family).isPresent()){ClientPlayNetworking.send(new SelectSlotPayload(hovered));minecraft.gui.setScreen(new TechniqueWheelScreen(id.get(),hovered));}else select();return true;}
        if(e.button()==InputConstants.MOUSE_BUTTON_LEFT||mouse(JjkKeys.CAST,e.button()))begin(e.button());
        else if(e.button()==pinButton()&&held<0){ClientPlayNetworking.send(new SetLoadoutPayload(slot,entries.get(hovered)));onClose();}
        return true;
    }
    @Override public boolean mouseReleased(MouseButtonEvent e){if(family==null&&mouse(JjkKeys.WHEEL,e.button())){select();return true;}if(e.button()==heldMouse&&held>=0){finish();return true;}return super.mouseReleased(e);}
    @Override public boolean keyPressed(KeyEvent e){if(family!=null&&JjkKeys.CAST.matches(e)){begin(-1);return true;}return super.keyPressed(e);}
    @Override public boolean keyReleased(KeyEvent e){
        if(family==null&&JjkKeys.WHEEL.matches(e)){select();return true;}
        if(family!=null&&held>=0&&heldMouse<0&&JjkKeys.CAST.matches(e)){finish();return true;}
        return super.keyReleased(e);
    }
    @Override public void removed(){if(family!=null)entry(hovered).ifPresent(id->com.jjkcraft.client.state.ClientTechniqueMenus.remember(family,id,branch));if(held>=0&&!finishing&&!quick)ClientPlayNetworking.send(new CastPayload(slot,false,true));super.removed();}
}
