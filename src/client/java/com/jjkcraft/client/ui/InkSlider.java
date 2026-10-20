package com.jjkcraft.client.ui;

import java.util.function.IntConsumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

/** Native drag/keyboard slider with the same contrast and focus treatment as our buttons. */
public final class InkSlider extends AbstractSliderButton {
    private final int min, max;
    private final IntConsumer changed;
    public InkSlider(int x,int y,int w,int h,Component label,int min,int max,int current,IntConsumer changed){
        super(x,y,w,h,label,(current-min)/(double)(max-min));
        this.min=min;this.max=max;this.changed=changed;
    }
    @Override protected void updateMessage() { }
    public void setInt(int n){value=Math.clamp((n-min)/(double)(max-min),0,1);}
    @Override protected void applyValue(){changed.accept((int)Math.round(min+value*(max-min)));}
    @Override public void extractWidgetRenderState(GuiGraphicsExtractor g,int mx,int my,float partial){
        int x=getX(),y=getY(),w=getWidth(),h=getHeight();
        g.fill(x,y+h/2-2,x+w,y+h/2+2,Palette.INK_EDGE);
        int at=x+3+(int)Math.round((w-6)*value);
        g.fill(x,y+h/2-2,at,y+h/2+2,active?Palette.energy():Palette.muted());
        g.fill(at-3,y+1,at+3,y+h-1,active?Palette.TEXT:Palette.muted());
        if(isHoveredOrFocused()&&active)g.outline(x,y,w,h,Palette.energyHigh());
    }
}
