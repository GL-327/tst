package com.jjkcraft.client.screen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import com.jjkcraft.client.state.*;
import com.jjkcraft.client.ui.*;
import com.jjkcraft.network.payload.PersonalizationPayloads;
import com.jjkcraft.training.TrainingState;
/** A field journal: training happens in the world; controls manage already learned skills. */
public final class PracticeScreen extends JjkScreen{
    private InkButton reinforce,rct;
    public PracticeScreen(){super(Component.literal("Training field journal"));}
    @Override protected void initContent(){int x=panelX()+10,bottom=panelY()+panelH()-25,w=(panelW()-26)/2;
        reinforce=addRenderableWidget(new InkButton(x,bottom-23,w,18,Component.literal("Reinforcement"),b->{var s=ClientPersonalization.practice();ClientPlayNetworking.send(new PersonalizationPayloads.Practice(3,s.output(),s.healing(),s.healBelow()));}));
        rct=addRenderableWidget(new InkButton(x+w+6,bottom-23,w,18,Component.literal("RCT healing [H]"),b->ClientPlayNetworking.send(new com.jjkcraft.network.payload.ReverseTogglePayload())));
        addRenderableWidget(new InkButton(x,bottom,w,18,Component.literal("Energy / RCT settings"),b->minecraft.gui.setScreen(new PracticeSettingsScreen())));
        addRenderableWidget(new InkButton(x+w+6,bottom,w,18,Component.literal("Growth field notes"),b->minecraft.gui.setScreen(new GrowthTreeScreen())));
    }
    @Override public void tick(){var s=ClientPersonalization.practice();reinforce.active=s.reinforcement();reinforce.setMessage(Component.literal(s.reinforcement()?"Reinforcement: "+(s.active()?"ON":"OFF"):"Learn on a straw doll"));rct.active=ClientSorcery.reverse().unlocked();rct.setMessage(Component.literal(rct.active?"Toggle RCT healing [H]":"Learn at a focus stone"));}
    @Override protected void extractContent(GuiGraphicsExtractor g,int mx,int my,float a){
        int x=panelX()+10,y=contentY(),w=(panelW()-30)/2;var state=com.jjkcraft.client.training.TrainingClient.state;var s=ClientPersonalization.practice();
        Ink.text(g,"BODY & CURSED ENERGY",x,y,Palette.energyHigh(),1);
        Ink.text(g,Ink.fit(Component.literal("Dolls: strike · mats: timed breathing · beams: balance · stones: reverse flow"),panelW()-20),x,y+13,Palette.TEXT_DIM,1);
        String[] stats={"strength","endurance","agility","weaponry"};for(int i=0;i<stats.length;i++){String stat=stats[i];int xx=x+i%2*(w+10),yy=y+31+i/2*25,level=state.level(stat);Ink.text(g,stat.toUpperCase()+"  "+level+" / 20",xx,yy,Palette.TEXT,1);int low=TrainingState.threshold(level),next=TrainingState.threshold(Math.min(20,level+1));bar(g,xx,yy+12,w,level==20?1:(state.xp(stat)-low)/(double)Math.max(1,next-low),Palette.gold());}
        int line=y+86;Ink.text(g,Ink.fit(Component.literal("Reinforcement: "+Math.min(40,state.xp("control"))+" / 40 · reverse flow: "+ClientSorcery.study().lessons()+" / 3"),panelW()-20),x,line,Palette.TEXT_DIM,1);
        Ink.text(g,Ink.fit(Component.literal(String.format(java.util.Locale.ROOT,"CE birth %d · trained +%d · regen %d/10 · smaller reserves improve faster",s.birth(),s.trained(),s.regenLevel())),panelW()-20),x,line+13,Palette.TEXT_DIM,1);
        Ink.text(g,Ink.fit(Component.literal("Mat: hold sneak for 2–5s, release. Sprint to build endurance. Physical bonuses are bounded."),panelW()-20),x,line+26,Palette.TEXT_DIM,1);
    }
}
