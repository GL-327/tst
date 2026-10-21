package com.jjkcraft.client.screen;
import java.util.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import com.jjkcraft.JjkCraft;
import com.jjkcraft.client.ui.*;
import com.jjkcraft.network.payload.ProjectionPayloads;
import com.jjkcraft.technique.ProjectionPlan;

/** Click a relative destination, then choose the physical move to perform there. Server ACK owns saved state. */
public final class ProjectionPlannerScreen extends JjkScreen {
    private static ProjectionPayloads.Reply reply;
    public static void accept(ProjectionPayloads.Reply r){reply=r;}
    private final List<ProjectionPlan.Step> steps=new ArrayList<>();
    private boolean automatic=true,reading=true;private int request,ticks,action=1;private String feedback="Loading route…";
    public ProjectionPlannerScreen(){super(Component.literal("Projection Sorcery · 24-frame choreography"));send(true);}
    private void send(boolean read){request=(int)(System.nanoTime()&0x7fffffff);ticks=0;reading=read;ClientPlayNetworking.send(new ProjectionPayloads.Edit(new ProjectionPlan(automatic,List.copyOf(steps)),read,request));}
    private int size(){return Math.min(144,panelH()-95);}
    /** Maps a route coordinate to its visible grid location (also used by native input tests). */
    public int[] pointAtOffset(double right,double forward){return new int[]{panelX()+10+(int)((right/24+.5)*size()),contentY()+18+(int)((.5-forward/24)*size())};}
    @Override protected void initContent(){
        int x=panelX()+size()+24,y=contentY()+20,w=panelW()-size()-34;
        addRenderableWidget(new InkButton(x,contentY()-2,w,18,Component.literal(automatic?"Preset: terrain-aware":"Custom choreography"),b->{automatic=!automatic;rebuildWidgets();}));
        for(int n=0;n<8;n++){int row=n;String text=n<steps.size()?(n+1)+" · "+ProjectionPlan.ACTIONS[steps.get(n).action()]:"+ choose a point";
            addRenderableWidget(new InkButton(x,y+n*16,w,15,Component.literal(text),b->{if(row<steps.size()){var s=steps.get(row);steps.set(row,new ProjectionPlan.Step(s.right(),s.forward(),(s.action()+1)%7));automatic=false;rebuildWidgets();}}));}
        addRenderableWidget(new InkButton(panelX()+10,panelY()+panelH()-25,55,18,Component.literal("Back"),b->minecraft.gui.setScreen(new TechniqueWorkshopScreen(JjkCraft.id("projection_sorcery")))));
        addRenderableWidget(new InkButton(panelX()+70,panelY()+panelH()-25,65,18,Component.literal("Undo point"),b->{if(!steps.isEmpty())steps.removeLast();rebuildWidgets();}));
        var save=addRenderableWidget(new InkButton(panelX()+panelW()-105,panelY()+panelH()-25,95,18,Component.literal("Save route"),b->{send(false);b.active=false;feedback="Saving…";}));save.active=request==0&&(automatic||!steps.isEmpty());
    }
    @Override public void tick(){if(request!=0&&reply!=null&&reply.request()==request){request=0;if(reply.accepted()){automatic=reply.plan().automatic();steps.clear();steps.addAll(reply.plan().steps());feedback=reading?"Click points · click a row to change its move":"Saved · cast Projection · crouch to cancel";}else feedback="Rejected: learn Projection and finish any active route";rebuildWidgets();}else if(request!=0&&++ticks>100){request=0;feedback="No reply · retry saving";rebuildWidgets();}}
    @Override public boolean mouseClicked(MouseButtonEvent e,boolean twice){int x=panelX()+10,y=contentY()+18,s=size();if(e.x()>=x&&e.x()<x+s&&e.y()>=y&&e.y()<y+s){if(e.button()==com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT){if(!steps.isEmpty())steps.removeLast();}else if(e.button()==com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT&&steps.size()<8){double right=Math.round(((e.x()-x)/s-.5)*24*2)/2.0,forward=Math.round((.5-(e.y()-y)/s)*24*2)/2.0;steps.add(new ProjectionPlan.Step(right,forward,action));automatic=false;}rebuildWidgets();return true;}return super.mouseClicked(e,twice);}
    @Override protected void extractContent(GuiGraphicsExtractor g,int mx,int my,float a){
        int x=panelX()+10,y=contentY()+18,s=size();Ink.text(g,"PROJECTION ROUTE ↑",x,contentY()+2,Palette.energyHigh(),1);g.fill(x,y,x+s,y+s,0xFF101421);
        var p=minecraft.player;if(p!=null){double yaw=Math.toRadians(p.getYRot());for(int i=0;i<12;i++)for(int j=0;j<12;j++){double dx=(i-5.5)*2,dz=(5.5-j)*2;var pos=net.minecraft.core.BlockPos.containing(p.position().add(Math.cos(yaw)*dx-Math.sin(yaw)*dz,0,Math.sin(yaw)*dx+Math.cos(yaw)*dz));boolean blocked=!p.level().getBlockState(pos).isAir(),gap=p.level().getBlockState(pos.below()).isAir();int color=blocked?0xFF5B3044:gap?0xFF252036:0xFF263B3B;g.fill(x+i*s/12,y+j*s/12,x+(i+1)*s/12-1,y+(j+1)*s/12-1,color);}}
        for(int n=0;n<steps.size();n++){var step=steps.get(n);int xx=x+(int)((step.right()/24+.5)*s),yy=y+(int)((.5-step.forward()/24)*s);g.fill(xx-3,yy-3,xx+4,yy+4,Palette.energyHigh());Ink.text(g,Integer.toString(n+1),xx+4,yy-4,Palette.TEXT,1);}
        g.fill(x+s/2-2,y+s/2-2,x+s/2+3,y+s/2+3,Palette.gold());Ink.text(g,Ink.fit(Component.literal(feedback),panelW()-20),x,panelY()+panelH()-39,Palette.TEXT_DIM,1);
    }
}
