package com.jjkcraft.client.screen;

import java.util.*;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import com.jjkcraft.JjkCraft;
import com.jjkcraft.client.state.*;
import com.jjkcraft.client.ui.*;
import com.jjkcraft.network.payload.ProjectionPayloads;
import com.jjkcraft.technique.*;

/** Editable choreography with cached terrain clearance, segment warnings and authoritative saving. */
public final class ProjectionPlannerScreen extends JjkScreen {
    private static ProjectionPayloads.Reply reply;
    public static void accept(ProjectionPayloads.Reply r){reply=r;}
    private final List<ProjectionPlan.Step> steps=new ArrayList<>();
    private final List<InkButton> rows=new ArrayList<>();
    private final ProjectionPreview preview=new ProjectionPreview();
    private boolean automatic=true,reading=true;private int request,ticks,selected=-1,dragging=-1,cooldown;private String feedback="Loading route…";
    private InkButton save;
    public ProjectionPlannerScreen(){super(Component.literal("Projection Sorcery · choreography"));send(true);}
    private void send(boolean read){request=ClientPersonalization.nextRequest();ticks=0;reading=read;ClientPlayNetworking.send(new ProjectionPayloads.Edit(new ProjectionPlan(automatic,List.copyOf(steps)),read,request));}
    private int size(){return Math.min(144,panelH()-95);}
    public int[] pointAtOffset(double right,double forward){return new int[]{panelX()+10+(int)((right/24+.5)*size()),contentY()+18+(int)((.5-forward/24)*size())};}
    public int unsafeStep(){return preview.unsafe(steps);}
    public int pointCount(){return steps.size();}
    private double reach(){var d=ClientTechniques.definition(JjkCraft.id("projection_sorcery")).orElse(null);if(d==null||!(d.behavior() instanceof com.jjkcraft.technique.behavior.ProjectionSorceryBehavior b))return 12;var t=ClientPersonalization.get(JjkCraft.id("projection_sorcery"));return Math.clamp(b.blink()*com.jjkcraft.core.technique.TechniqueMath.masteryReach(ClientTechniques.mastery(JjkCraft.id("projection_sorcery")))*com.jjkcraft.core.technique.TechniqueMath.power(ClientProgress.rank(),ClientProgress.get().growth().output(),ClientTechniques.mastery(JjkCraft.id("projection_sorcery")),1)*t.output()/100.0*t.pact().output(),1,16);}
    private void changed(){automatic=false;preview.dirty=true;feedback="Unsaved route";}
    @Override protected void initContent(){
        preview.dirty=true;rows.clear();int x=panelX()+size()+24,y=contentY()+20,w=panelW()-size()-34;
        var mode=addRenderableWidget(new InkButton(x,contentY()-2,w,18,Component.literal(automatic?"Preset: terrain-aware":"Custom choreography"),b->{automatic=!automatic;preview.dirty=true;feedback="Unsaved route";rebuildWidgets();}));mode.active=request==0;
        for(int n=0;n<8;n++){
            int row=n;String text=n<steps.size()?(n+1)+" · "+ProjectionPlan.ACTIONS[steps.get(n).action()]:"+ choose a point";
            var button=addRenderableWidget(new InkButton(x,y+n*16,w,15,Component.literal(text),b->{if(row<steps.size()){selected=row;var s=steps.get(row);steps.set(row,new ProjectionPlan.Step(s.right(),s.forward(),(s.action()+1)%7));changed();rebuildWidgets();}}).selected(n==selected));button.active=request==0&&n<steps.size();rows.add(button);
        }
        addRenderableWidget(new InkButton(panelX()+10,panelY()+panelH()-25,55,18,Component.literal("Back"),b->minecraft.gui.setScreen(new TechniqueWorkshopScreen(JjkCraft.id("projection_sorcery")))));
        var undo=addRenderableWidget(new InkButton(panelX()+70,panelY()+panelH()-25,65,18,Component.literal("Undo point"),b->{if(!steps.isEmpty()){steps.removeLast();selected=Math.min(selected,steps.size()-1);changed();}rebuildWidgets();}));undo.active=request==0&&!steps.isEmpty();
        save=addRenderableWidget(new InkButton(panelX()+panelW()-105,panelY()+panelH()-25,95,18,Component.literal("Save route"),b->{send(false);b.active=false;feedback="Saving…";}));save.active=request==0&&cooldown==0&&(automatic||!steps.isEmpty());
    }
    @Override public void tick(){
        if(cooldown>0)cooldown--;
        if(request!=0&&reply!=null&&reply.request()==request){
            request=0;if(reply.accepted()||reading){automatic=reply.plan().automatic();steps.clear();steps.addAll(reply.plan().steps());selected=Math.min(selected,steps.size()-1);}
            feedback=reply.accepted()?(reading?"Click points · drag to move · click rows to change attacks":"Saved · crouch cancels the route"):switch(reply.result()){case 1->"Too fast · wait briefly and retry";case 2->"Learn Projection before saving a route";case 3->"Finish or cancel the active route first";case 4->"Invalid route · maximum 8 points within the grid";default->"Unavailable while dead or spectating";};
            if(reply.result()==1)cooldown=20;rebuildWidgets();
        }else if(request!=0&&++ticks>100){request=0;feedback="No reply · retry saving";rebuildWidgets();}
        preview.refresh(minecraft.player,steps,reach());
        for(int n=0;n<Math.min(rows.size(),steps.size());n++){var p=steps.get(n);rows.get(n).setTooltip(Tooltip.create(Component.literal(String.format(Locale.ROOT,"Right %.1f · forward %.1f\n%s\nClick row to cycle action; drag its point to move it.",p.right(),p.forward(),ProjectionPreview.explanation(preview.states[n])))));}
        save.active=request==0&&cooldown==0&&(automatic||!steps.isEmpty());int unsafe=preview.unsafe(steps);save.setTooltip(Tooltip.create(Component.literal(automatic?"Preset chooses safe approach positions; without an enemy it only moves.":unsafe<0?"Save this relative route. Terrain is checked again during the cast.":"Step "+(unsafe+1)+": "+ProjectionPreview.explanation(preview.states[unsafe])+". This route may fail when cast here.")));
    }
    private ProjectionPlan.Step coordinate(double mx,double my,int action){int s=size();double r=Math.clamp(Math.round(((mx-panelX()-10)/s-.5)*48)/2.0,-12,12),f=Math.clamp(Math.round((.5-(my-contentY()-18)/s)*48)/2.0,-12,12);return new ProjectionPlan.Step(r,f,action);}
    private int node(double mx,double my){for(int n=0;n<steps.size();n++){int[] at=pointAtOffset(steps.get(n).right(),steps.get(n).forward());if(Math.hypot(mx-at[0],my-at[1])<=6)return n;}return -1;}
    @Override public boolean mouseClicked(MouseButtonEvent e,boolean twice){
        int x=panelX()+10,y=contentY()+18,s=size();if(e.x()>=x&&e.x()<x+s&&e.y()>=y&&e.y()<y+s){
            if(request!=0)return true;int hit=node(e.x(),e.y());
            if(e.button()==InputConstants.MOUSE_BUTTON_RIGHT){if(!steps.isEmpty()){steps.remove(hit>=0?hit:steps.size()-1);selected=Math.min(selected,steps.size()-1);changed();}}
            else if(e.button()==InputConstants.MOUSE_BUTTON_LEFT){if(hit>=0){selected=hit;dragging=hit;}else if(steps.size()<8){steps.add(coordinate(e.x(),e.y(),1));selected=steps.size()-1;dragging=selected;changed();}else feedback="8 points maximum · select and drag an existing point";}
            rebuildWidgets();if(dragging>=0)setDragging(true);return true;
        }return super.mouseClicked(e,twice);
    }
    @Override public boolean mouseDragged(MouseButtonEvent e,double dx,double dy){if(dragging>=0&&dragging<steps.size()&&e.button()==InputConstants.MOUSE_BUTTON_LEFT){steps.set(dragging,coordinate(e.x(),e.y(),steps.get(dragging).action()));changed();return true;}return super.mouseDragged(e,dx,dy);}
    @Override public boolean mouseReleased(MouseButtonEvent e){if(dragging>=0){dragging=-1;setDragging(false);rebuildWidgets();return true;}return super.mouseReleased(e);}
    private static void line(GuiGraphicsExtractor g,int x1,int y1,int x2,int y2,int color){int n=Math.max(1,Math.max(Math.abs(x2-x1),Math.abs(y2-y1)));for(int i=0;i<=n;i++){int x=x1+(x2-x1)*i/n,y=y1+(y2-y1)*i/n;g.fill(x,y,x+1,y+1,color);}}
    @Override protected void extractContent(GuiGraphicsExtractor g,int mx,int my,float a){
        int x=panelX()+10,y=contentY()+18,s=size();Ink.text(g,"PROJECTION ROUTE ↑",x,contentY()+2,Palette.energyHigh(),1);g.fill(x,y,x+s,y+s,0xFF101421);
        for(int j=0;j<12;j++)for(int i=0;i<12;i++){int c=switch(preview.cells[j*12+i]){case 1->0xFF533246;case 2->0xFF42557D;default->0xFF263B3B;};g.fill(x+i*s/12,y+j*s/12,x+(i+1)*s/12-1,y+(j+1)*s/12-1,c);}
        int px=x+s/2,py=y+s/2;
        for(int n=0;n<steps.size();n++){var p=steps.get(n);int[] at=pointAtOffset(p.right(),p.forward());int c=ProjectionPreview.routeColor(preview.states[n]);line(g,px,py,at[0],at[1],c);g.fill(at[0]-3,at[1]-3,at[0]+4,at[1]+4,c);if(n==selected)g.outline(at[0]-5,at[1]-5,11,11,Palette.gold());Ink.text(g,Integer.toString(n+1),at[0]+4,at[1]-4,Palette.TEXT,1);px=at[0];py=at[1];}
        g.fill(x+s/2-2,y+s/2-2,x+s/2+3,y+s/2+3,Palette.gold());Ink.text(g,"Floor / step / blocked",x,y+s+3,Palette.TEXT_DIM,1);
        String message=feedback;int unsafe=preview.unsafe(steps);if(request==0&&!automatic&&unsafe>=0&&feedback.startsWith("Unsaved"))message="Step "+(unsafe+1)+": "+ProjectionPreview.explanation(preview.states[unsafe]);
        Ink.text(g,Ink.fit(Component.literal(message),panelW()-20),x,panelY()+panelH()-39,unsafe>=0&&!automatic?Palette.danger():Palette.TEXT_DIM,1);
        int rx=panelX()+size()+24;for(int n=0;n<steps.size();n++)g.fill(rx-5,contentY()+23+n*16,rx-2,contentY()+32+n*16,ProjectionPreview.routeColor(preview.states[n]));
    }
}
