package com.jjkcraft.client.screen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import com.jjkcraft.client.state.*;
import com.jjkcraft.client.ui.*;
import com.jjkcraft.network.payload.PersonalizationPayloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
/** Learning lives in the world; expression and automatic-healing preferences remain editable. */
public final class PracticeSettingsScreen extends JjkScreen{
    private final EditBox[] fields=new EditBox[3];private String feedback="Changes affect your own learned reinforcement and RCT.";
    private static final String[] LABELS={"Reinforcement output %","RCT healing output %","Automatic healing below %"};
    public PracticeSettingsScreen(){super(Component.literal("Universal skill expression"));}
    @Override protected void initContent(){var s=ClientPersonalization.practice();int[] values={s.output(),s.healing(),s.healBelow()};for(int i=0;i<3;i++){var f=new EditBox(font,panelX()+panelW()-100,contentY()+25+i*34,85,18,Component.literal(LABELS[i]));f.setMaxLength(3);f.setValue(""+values[i]);fields[i]=addRenderableWidget(f);}
        addRenderableWidget(new InkButton(panelX()+10,panelY()+panelH()-25,80,18,Component.literal("Back"),b->onClose()));
        addRenderableWidget(new InkButton(panelX()+panelW()-115,panelY()+panelH()-25,105,18,Component.literal("Apply settings"),b->{try{int a=Integer.parseInt(fields[0].getValue()),h=Integer.parseInt(fields[1].getValue()),t=Integer.parseInt(fields[2].getValue());if(a<25||a>300||h<25||h>300||t<25||t>100)throw new NumberFormatException();ClientPlayNetworking.send(new PersonalizationPayloads.Practice(4,a,h,t));feedback="Settings sent to server.";}catch(NumberFormatException e){feedback="Outputs: 25–300%. Healing threshold: 25–100%.";}}));
    }
    @Override protected void extractContent(GuiGraphicsExtractor g,int x,int y,float a){for(int i=0;i<3;i++)Ink.text(g,LABELS[i],panelX()+12,contentY()+29+i*34,Palette.TEXT,1);Ink.text(g,Ink.fit(Component.literal(feedback),panelW()-20),panelX()+10,panelY()+panelH()-42,Palette.TEXT_DIM,1);}
    @Override public void onClose(){minecraft.gui.setScreen(new PracticeScreen());}
}
