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
    private boolean scrolling=true,finishing;
    private int branch;
    private static final String[] BRANCHES={"All","Blue","Red","Infinity","Purple"};
    public TechniqueWheelScreen(){this(null,ClientTechniques.state().selected());}
    public TechniqueWheelScreen(Identifier family,int slot){super(Component.literal("Technique wheel"));this.family=family;this.slot=slot;hovered=family==null?slot:0;}
    @Override protected void init(){
        entries=family==null?ClientTechniques.state().loadout():ClientTechniques.variants(family).stream().filter(id->inBranch(id,branch)).map(Optional::of).toList();
        hovered=Math.clamp(hovered,0,Math.max(0,entries.size()-1));page=hovered/12;
        if(limitless()&&width>=320)for(int i=0;i<BRANCHES.length;i++){final int b=i;var button=addRenderableWidget(new InkButton(7,height/2-49+i*20,57,18,Component.literal(BRANCHES[i]),v->{branch=b;page=hovered=0;scrolling=true;rebuildWidgets();}).selected(i==branch));button.active=held<0&&ClientTechniques.variants(family).stream().anyMatch(id->inBranch(id,b));}
        if(family!=null&&family.getPath().equals("family/ten_shadows"))addRenderableWidget(new InkButton(7,35,90,20,Component.literal("Subjugation"),b->minecraft.gui.setScreen(new TechniqueAbilityScreen(com.jjkcraft.JjkCraft.id("ten_shadows")))));
    }
    private boolean limitless(){return family!=null&&family.getPath().equals("family/limitless");}
    private boolean untamed(Identifier id){return minecraft.player!=null&&!minecraft.player.isCreative()&&ClientTechniques.definition(id).filter(d->!com.jjkcraft.client.state.ClientTechniqueMenus.allowed(d)).isPresent();}
    private static boolean inBranch(Identifier id,int branch){String p=id.getPath();return switch(branch){case 1->p.contains("blue");case 2->p.contains("red");case 3->p.equals("infinity");case 4->p.contains("purple");default->true;};}
    public int entryCount(){return entries.size();}
    public Optional<Identifier> entry(int index){return index>=0&&index<entries.size()?entries.get(index):Optional.empty();}
    private static boolean mouse(net.minecraft.client.KeyMapping key,int button){var bound=KeyMappingHelper.getBoundKeyOf(key);return bound.getType()==InputConstants.Type.MOUSE&&bound.getValue()==button;}
    private int pinButton(){return mouse(JjkKeys.CAST,InputConstants.MOUSE_BUTTON_RIGHT)?InputConstants.MOUSE_BUTTON_MIDDLE:InputConstants.MOUSE_BUTTON_RIGHT;}
    @Override public boolean isPauseScreen(){return false;}
    public int selection(){return hovered;}
    public boolean isFamilyWheel(){return family!=null;}
    public int[] nodePoint(int index){int count=Math.min(12,entries.size()-page*12);double angle=-Math.PI/2+2*Math.PI*(index-page*12)/Math.max(1,count);return new int[]{width/2+(int)(Math.cos(angle)*radius()),height/2+(int)(Math.sin(angle)*radius())};}
    private int radius(){return Math.max(42,Math.min(94,height/2-48));}
    @Override public void extractBackground(GuiGraphicsExtractor g,int x,int y,float a){g.fill(0,0,width,height,0xB007080E);}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float a){
        int cx=width/2,cy=height/2,count=Math.min(12,entries.size()-page*12);
        if(Double.isNaN(lastX)){lastX=mx;lastY=my;}
        if(Math.hypot(mx-lastX,my-lastY)>3){scrolling=false;lastX=mx;lastY=my;}
        double dx=mx-cx,dy=my-cy;
        if(!scrolling&&dx*dx+dy*dy>28*28&&count>0){double angle=Math.atan2(dy,dx)+Math.PI/2;if(angle<0)angle+=Math.PI*2;hovered=page*12+(int)Math.floor((angle+Math.PI/count)/(Math.PI*2/count))%count;}
        Ink.ring(g,cx,cy,radius(),2,1,Palette.INK_EDGE);
        for(int i=page*12;i<Math.min(entries.size(),page*12+12);i++){
            var id=entries.get(i);var d=id.flatMap(ClientTechniques::definition);int[] p=nodePoint(i);boolean hot=i==hovered;int r=hot?16:13;
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
            Ink.centered(g,Ink.fit(name,104),cx,cy-14,Palette.TEXT,1);
            String detail=family==null?"Slot "+(hovered+1)+(id.filter(TechniqueBindings::family).isPresent()?" · family selector":""):d.map(v->String.format(Locale.ROOT,"%.0f CE · %s",ClientTechniques.cost(id.orElseThrow()),v.cast().chargeable()?"hold to charge":v.cast().mode()==CastMode.TOGGLE?"toggle":"instant")).orElse("");
            Ink.centered(g,Ink.fit(Component.literal(detail),112),cx,cy+1,Palette.energyHigh(),1);
            var failure=ClientTechniques.recentFailure(slot);if(family!=null&&failure!=CastFailure.NONE)Ink.centered(g,Ink.fit(Component.translatable(failure.translationKey()),120),cx,cy+15,Palette.danger(),1);
            else if(family!=null&&id.isPresent()&&d.isPresent()&&held<0){long ticks=ClientTechniques.state().readyAt(id.get())-com.jjkcraft.client.state.ClientEnergy.gameTime();String ready=ClientTechniques.activeIs(id.get())?"ACTIVE · click to stop":ticks>0?String.format(Locale.ROOT,"Ready in %.1f s",ticks/20.0):!ClientTechniques.gradeAllows(d.get())?"Higher grade required":untamed(id.get())?"UNTAMED � subjugate":!ClientTechniques.affordable(id.get())?"Low CE (estimate)":"READY";Ink.centered(g,Ink.fit(Component.literal(ready),112),cx,cy+15,ticks>0?Palette.TEXT_DIM:Palette.gold(),1);}
        }
        var charge=ClientTechniques.charge();if(held>=0&&charge.slot()>=0)Ink.centered(g,Component.literal("CHARGE "+charge.percent()+"% · release to cast"),cx,cy+18,Palette.accent(),1);
        String hint=family==null?"Scroll / point · release "+JjkKeys.WHEEL.getTranslatedKeyMessage().getString()+" to select":"Hold click / "+JjkKeys.CAST.getTranslatedKeyMessage().getString()+" to cast · "+(pinButton()==InputConstants.MOUSE_BUTTON_RIGHT?"right":"middle")+" click pins move";
        Ink.centered(g,Ink.fit(Component.literal(hint),width-12),cx,height-21,Palette.TEXT_DIM,1);
        Ink.centered(g,Component.literal(family==null?"Click a family to browse its powers":"Scroll through learned moves · Esc cancels"),cx,height-10,Palette.TEXT_DIM,1);
        if(entries.size()>12)Ink.text(g,(page+1)+" / "+((entries.size()+11)/12),width-40,7,Palette.TEXT_DIM,1);
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
        held=hovered;heldMouse=mouse;ClientPlayNetworking.send(new VariantCastPayload(slot,id));}
    private void finish(){if(held<0)return;finishing=true;ClientPlayNetworking.send(new CastPayload(slot,false));held=-1;onClose();}
    @Override public boolean mouseClicked(MouseButtonEvent e,boolean twice){
        if(family!=null&&family.getPath().equals("family/ten_shadows")&&e.x()>=7&&e.x()<97&&e.y()>=35&&e.y()<55)return super.mouseClicked(e,twice);
        if(limitless()&&width>=320&&e.x()>=7&&e.x()<64&&e.y()>=height/2-49&&e.y()<height/2+49)return super.mouseClicked(e,twice);
        if(hovered<0||hovered>=entries.size())return true;
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
    @Override public void removed(){if(held>=0&&!finishing)ClientPlayNetworking.send(new CastPayload(slot,false,true));super.removed();}
}
