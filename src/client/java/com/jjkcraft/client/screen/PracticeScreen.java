package com.jjkcraft.client.screen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import com.jjkcraft.client.state.*;
import com.jjkcraft.client.ui.*;
import com.jjkcraft.network.payload.PersonalizationPayloads;

/** Universal CE skills are learned independently of an innate family or four-move loadout. */
public final class PracticeScreen extends JjkScreen {
    private InkButton reinforce,rct,toggle,train;
    public PracticeScreen(){super(Component.literal("Cursed energy practice"));}
    private void send(int action,int output,int healing,int threshold){ClientPlayNetworking.send(new PersonalizationPayloads.Practice(action,output,healing,threshold));}
    private void action(int action){var s=ClientPersonalization.practice();send(action,s.output(),s.healing(),s.healBelow());}
    @Override protected void initContent(){
        int x=panelX()+10,y=contentY()+58,col=(panelW()-30)/2;
        reinforce=addRenderableWidget(new InkButton(x,y,col,18,Component.literal("Learn reinforcement · 3 levels"),b->action(0)));
        rct=addRenderableWidget(new InkButton(x+col+10,y,col,18,Component.literal("Reverse-flow drill · 3 levels"),b->action(1)));
        toggle=addRenderableWidget(new InkButton(x,y+23,col,18,Component.literal("Reinforcement: off"),b->action(3)));
        addRenderableWidget(new InkButton(x+col+10,y+23,col,18,Component.literal("Toggle RCT healing [H]"),b->ClientPlayNetworking.send(new com.jjkcraft.network.payload.ReverseTogglePayload())));
        for(int n=0;n<3;n++){
            final int axis=n;int xx=x+(n==0?0:col+10),yy=y+50+(n==2?30:0);
            for(int dir:new int[]{-1,1})addRenderableWidget(new InkButton(xx+col-48+(dir>0?25:0),yy-2,22,17,Component.literal(dir<0?"−":"+"),b->{
                var s=ClientPersonalization.practice();send(4,s.output()+(axis==0?dir*25:0),s.healing()+(axis==1?dir*25:0),s.healBelow()+(axis==2?dir*5:0));
            }));
        }
        train=addRenderableWidget(new InkButton(x,panelY()+panelH()-25,col,18,Component.literal("Train reserves +25"),b->action(2)));
        addRenderableWidget(new InkButton(x+col+10,panelY()+panelH()-25,col,18,Component.literal("Growth tree"),b->minecraft.gui.setScreen(new GrowthTreeScreen())));
    }
    @Override public void tick(){
        var s=ClientPersonalization.practice();boolean allowed=ClientProgress.get().awakened()&&!ClientProgress.get().heavenlyRestricted();
        reinforce.active=allowed&&!s.reinforcement();rct.active=allowed&&!ClientSorcery.reverse().unlocked();toggle.active=allowed&&s.reinforcement();train.active=allowed&&s.trained()<10000;
        toggle.setMessage(Component.literal("Reinforcement: "+(s.active()?"ON":"OFF")));
        reinforce.setMessage(Component.literal(s.reinforcement()?"Reinforcement learned":"Learn reinforcement · 3 levels"));
        rct.setMessage(Component.literal(ClientSorcery.reverse().unlocked()?"RCT learned":"Reverse flow · "+ClientSorcery.study().lessons()+"/3 · 3 levels"));
        rct.active=rct.active&&s.reinforcement()&&!ClientSorcery.study().running();
        if(ClientSorcery.study().running())onClose();
        train.setMessage(Component.literal("+"+com.jjkcraft.core.technique.PersonalizationMath.trainingGain(ClientEnergy.max())+" reserves · "+com.jjkcraft.core.technique.PersonalizationMath.trainingPrice(ClientEnergy.max())+" levels"));
    }
    @Override protected void extractContent(GuiGraphicsExtractor g,int mx,int my,float a){
        var s=ClientPersonalization.practice();int x=panelX()+10,y=contentY(),col=(panelW()-30)/2;
        Ink.text(g,"CURSED ENERGY · PERSONAL POTENTIAL",x,y,Palette.energyHigh(),1);
        Ink.text(g,ClientEnergy.uncapped()?String.format(java.util.Locale.ROOT,"Start: 0 CE   Bank: %.0f CE   Storage: UNBOUNDED",ClientEnergy.predicted()):String.format(java.util.Locale.ROOT,"Birth: %d CE   Training: +%d   Maximum: %.0f",s.birth(),s.trained(),ClientEnergy.max()),x,y+15,Palette.TEXT,1);
        bar(g,x,y+29,panelW()-20,ClientEnergy.predicted()/Math.max(1,ClientEnergy.max()),Palette.energy());
        Ink.text(g,ClientProgress.get().heavenlyRestricted()?"Your restriction prevents cursed-energy practice.":String.format(java.util.Locale.ROOT,"Regen level: %d/10 · %.1fx birth rate · smaller pools train faster",s.regenLevel(),com.jjkcraft.core.technique.PersonalizationMath.regenMultiplier(s.regenLevel())),x,y+39,Palette.TEXT_DIM,1);
        int yy=y+108;
        Ink.text(g,"Reinforce "+s.output()+"%",x,yy,Palette.TEXT,1);
        Ink.text(g,String.format(java.util.Locale.ROOT,"%.1f CE/s · damage +%.0f%%",3*Math.pow(s.output()/100.0,2),35*s.output()/100.0),x,yy+20,Palette.TEXT_DIM,1);
        Ink.text(g,"RCT "+s.healing()+"%",x+col+10,yy,Palette.TEXT,1);
        Ink.text(g,"Heal below "+s.healBelow()+"%",x+col+10,yy+30,Palette.TEXT,1);
        Ink.text(g,Ink.fit(Component.literal(ClientSorcery.reverse().unlocked()?"RCT: heal with H; bind Positive Discharge to exorcise curses.":"RCT: learn reinforcement, then finish 3 stillness drills (10s, 30 CE + 3 levels each)."),panelW()-20),x,panelY()+panelH()-40,Palette.TEXT_DIM,1);
    }
}
