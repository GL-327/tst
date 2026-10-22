package com.jjkcraft.client.design;

import java.util.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.*;
import net.minecraft.network.chat.Component;
import com.jjkcraft.client.ui.*;

/** Drag real UI parts or snap blocks together. Presets, undo, JSON sharing and a playable prop pack. */
public final class DesignStudioScreen extends Screen{
    private final Screen parent;
    private String page,mode="UI",prop="training_dummy",selected="",feedback="Drag a part. Drag its lower-right corner to resize.";
    private final Deque<String> undo=new ArrayDeque<>();
    private int cube,dragX,dragY;private UiDesign.Part drag;private UiDesign.Cube dragCube;private boolean resize;
    private EditBox label;private final EditBox[] numbers=new EditBox[6];
    public DesignStudioScreen(Screen parent){super(Component.literal("Jujutsu Craft · Design studio"));this.parent=parent;page=UiDesign.mod(parent)?UiDesign.page(parent):"ProfileScreen";}
    private void button(int x,int y,int w,String text,Runnable r){addRenderableWidget(new InkButton(x,y,w,17,Component.literal(text),b->r.run()));}
    private int left(){return 83;}private int right(){return width-108;}private int top(){return 55;}private int bottom(){return height-35;}
    private void checkpoint(){undo.push(UiDesign.exportText());while(undo.size()>30)undo.removeLast();}
    private void edit(Runnable r){checkpoint();r.run();rebuildWidgets();}
    private String title(String s){return s.replace("Screen","").replaceAll("([a-z])([A-Z])","$1 $2");}
    @Override protected void init(){
        int w=Math.max(41,(width-16)/8),x=8;
        for(String name:new String[]{"UI","Props","HUD"}){String next=name;button(x,8,w-2,name,()->{mode=next;if(next.equals("HUD"))page="HUD";rebuildWidgets();});x+=w;}
        button(x,8,w-2,"Theme",()->edit(()->UiDesign.theme(UiDesign.document().theme+1)));x+=w;
        button(x,8,w-2,"Import",()->{try{checkpoint();UiDesign.importText(minecraft.keyboardHandler.getClipboard());feedback="Imported design. Save to keep it.";rebuildWidgets();}catch(Exception e){feedback="Import refused: "+e.getMessage();}});x+=w;
        button(x,8,w-2,"Copy",()->{minecraft.keyboardHandler.setClipboard(UiDesign.exportText());feedback="Design JSON copied. You can share it or paste it back with Import.";});x+=w;
        button(x,8,w-2,"Save",()->{try{UiDesign.save();feedback="Saved · config/jjkcraft-design.json";}catch(Exception e){feedback="Save failed: "+e.getMessage();}});x+=w;
        button(x,8,w-2,"Back",this::onClose);
        boolean props=mode.equals("Props");
        button(8,33,71,"Previous",()->{if(props){var keys=List.of("training_dummy","training_mat","focus_stone","balance_beam");prop=keys.get(Math.floorMod(keys.indexOf(prop)-1,keys.size()));cube=0;}else{page=UiDesign.PAGES[Math.floorMod(Arrays.asList(UiDesign.PAGES).indexOf(page)-1,UiDesign.PAGES.length)];}selected="";rebuildWidgets();});
        button(width-79,33,71,"Next",()->{if(props){var keys=List.of("training_dummy","training_mat","focus_stone","balance_beam");prop=keys.get((keys.indexOf(prop)+1)%keys.size());cube=0;}else page=UiDesign.PAGES[(Arrays.asList(UiDesign.PAGES).indexOf(page)+1)%UiDesign.PAGES.length];selected="";rebuildWidgets();});
        int y=58;
        if(props){
            button(8,y,68,"Add block",()->edit(()->{var list=UiDesign.prop(prop);if(list.size()<48){list.add(new UiDesign.Cube(0,0,0,4,4,4,0));cube=list.size()-1;}}));y+=20;
            button(8,y,68,"Next block",()->{cube=(cube+1)%UiDesign.prop(prop).size();rebuildWidgets();});y+=20;
            button(8,y,68,"Material",()->edit(()->{var list=UiDesign.prop(prop);var c=list.get(cube);list.set(cube,new UiDesign.Cube(c.x(),c.y(),c.z(),c.w(),c.h(),c.d(),(c.material()+1)%8));}));y+=20;
            button(8,y,68,"Duplicate",()->edit(()->{var list=UiDesign.prop(prop);if(list.size()<48){list.add(list.get(cube));cube=list.size()-1;}}));y+=20;
            button(8,y,68,"Apply prop",()->{try{UiDesign.save();PropPack.apply();feedback="Original prop pack applied. Server hitboxes stay the same.";}catch(Exception e){feedback="Prop pack failed: "+e.getMessage();}});y+=20;
        }else{
            for(String kind:List.of("panel","label","bar","rule")){String type=kind;button(8,y,68,"+ "+kind,()->edit(()->{String id="custom/"+UUID.randomUUID().toString().substring(0,8);UiDesign.put(page,new UiDesign.Part(id,type,type.equals("label")?"Cursed energy":"",240,170,140,type.equals("rule")?3:28,Palette.accent(),false));selected=id;}));y+=20;}
            button(8,y,68,"Open page",()->{var screen=DesignPages.create(page);if(screen!=null)minecraft.gui.setScreen(screen);else feedback="Open this page during its gameplay event, then press F6.";});y+=20;
        }
        button(8,y,68,"Undo",()->{if(!undo.isEmpty()){UiDesign.importText(undo.pop());rebuildWidgets();}});y+=20;
        button(8,y,68,"Delete",()->edit(()->{if(props){var list=UiDesign.prop(prop);if(list.size()>1){list.remove(cube);cube=Math.min(cube,list.size()-1);}}else{var p=chosen();if(p!=null)UiDesign.put(page,new UiDesign.Part(p.id(),p.kind(),p.label(),p.x(),p.y(),p.w(),p.h(),p.color(),true));}}));
        int ix=width-101,iy=61;String[] names=props?new String[]{"X","Y","Z","Width","Height","Depth"}:new String[]{"X","Y","Width","Height"};
        int[] values;if(props){var c=UiDesign.prop(prop).get(Math.clamp(cube,0,UiDesign.prop(prop).size()-1));values=new int[]{c.x(),c.y(),c.z(),c.w(),c.h(),c.d()};}else{var p=chosen();values=p==null?new int[]{0,0,100,24}:new int[]{p.x(),p.y(),p.w(),p.h()};}
        Arrays.fill(numbers,null);for(int i=0;i<names.length;i++){var box=new EditBox(font,ix+(i%2)*48,iy+12+(i/2)*30,44,16,Component.literal(names[i]));box.setMaxLength(4);box.setValue(""+values[i]);numbers[i]=addRenderableWidget(box);}
        label=addRenderableWidget(new EditBox(font,ix,iy+(props?104:74),92,16,Component.literal("Label")));label.setMaxLength(80);label.setValue(props?"Block "+(cube+1):chosen()==null?"":chosen().label());label.active=!props&&chosen()!=null&&chosen().id().startsWith("custom/");
        if(!props){label.setWidth(60);button(ix+62,iy+74,30,"Tint",()->edit(()->{var p=chosen();if(p!=null){int index=0;for(int i=0;i<UiDesign.MATERIALS.length;i++)if(UiDesign.MATERIALS[i]==p.color())index=i+1;UiDesign.put(page,new UiDesign.Part(p.id(),p.kind(),p.label(),p.x(),p.y(),p.w(),p.h(),UiDesign.MATERIALS[index%8],p.hidden()));}}));}
        button(ix,iy+(props?124:94),92,"Apply values",this::applyValues);
        button(8,y+20,68,props?"Reset prop":"Reset page",()->edit(()->{if(props)UiDesign.document().props.remove(prop);else UiDesign.document().pages.remove(page);selected="";cube=0;}));
    }
    private UiDesign.Part chosen(){return UiDesign.parts(page).stream().filter(p->p.id().equals(selected)).findFirst().orElse(null);}
    private void applyValues(){try{int[] n=new int[6];for(int i=0;i<6;i++)if(numbers[i]!=null)n[i]=Integer.parseInt(numbers[i].getValue());checkpoint();if(mode.equals("Props")){var list=UiDesign.prop(prop);var old=list.get(cube);int x=Math.clamp(n[0],-16,31),y=Math.clamp(n[1],0,31),z=Math.clamp(n[2],-16,31);list.set(cube,new UiDesign.Cube(x,y,z,Math.clamp(n[3],1,Math.min(32,32-x)),Math.clamp(n[4],1,32-y),Math.clamp(n[5],1,Math.min(32,32-z)),old.material()));}else{var old=chosen();if(old!=null){var p=old.move(n[0],n[1],n[2],n[3]);UiDesign.put(page,new UiDesign.Part(p.id(),p.kind(),label.active?label.getValue():p.label(),p.x(),p.y(),p.w(),p.h(),p.color(),false));}}feedback="Updated. Save to keep the design.";rebuildWidgets();}catch(NumberFormatException e){feedback="Use whole numbers. Shapes snap to the block grid.";}}
    @Override public boolean mouseClicked(MouseButtonEvent e,boolean twice){
        if(e.x()>left()&&e.x()<right()&&e.y()>top()&&e.y()<bottom()){
            dragX=(int)e.x();dragY=(int)e.y();
            if(mode.equals("Props")){var list=UiDesign.prop(prop);var order=new ArrayList<>(list);order.sort(Comparator.comparingInt(c->c.x()+c.z()));for(var c:order){int x0=Integer.MAX_VALUE,y0=x0,x1=Integer.MIN_VALUE,y1=x1;for(int xx:new int[]{c.x(),c.x()+c.w()})for(int yy:new int[]{c.y(),c.y()+c.h()})for(int zz:new int[]{c.z(),c.z()+c.d()}){var p=point(xx,yy,zz);x0=Math.min(x0,p[0]);x1=Math.max(x1,p[0]);y0=Math.min(y0,p[1]);y1=Math.max(y1,p[1]);}if(e.x()>=x0&&e.x()<=x1&&e.y()>=y0&&e.y()<=y1){cube=list.indexOf(c);break;}}checkpoint();dragCube=list.get(cube);rebuildWidgets();setDragging(true);return true;}
            var list=UiDesign.parts(page);int px=(int)((e.x()-left())*640/(right()-left())),py=(int)((e.y()-top())*360/(bottom()-top()));
            for(int i=list.size()-1;i>=0;i--){var p=list.get(i);if(!p.hidden()&&px>=p.x()&&px<=p.x()+p.w()&&py>=p.y()&&py<=p.y()+p.h()){checkpoint();selected=p.id();drag=p;resize=px>p.x()+p.w()-16&&py>p.y()+p.h()-16;setDragging(true);return true;}}
        }return super.mouseClicked(e,twice);
    }
    @Override public boolean mouseDragged(MouseButtonEvent e,double dx,double dy){if(drag!=null){int x=(int)((e.x()-dragX)*640/(right()-left())),y=(int)((e.y()-dragY)*360/(bottom()-top()));UiDesign.put(page,resize?drag.move(drag.x(),drag.y(),drag.w()+x,drag.h()+y):drag.move((drag.x()+x)/4*4,(drag.y()+y)/4*4,drag.w(),drag.h()));return true;}if(dragCube!=null){int x=Math.clamp(dragCube.x()+(int)((e.x()-dragX)/propScale()),-16,32-dragCube.w()),y=Math.clamp(dragCube.y()-(int)((e.y()-dragY)/propScale()),0,32-dragCube.h());UiDesign.prop(prop).set(cube,new UiDesign.Cube(x,y,dragCube.z(),dragCube.w(),dragCube.h(),dragCube.d(),dragCube.material()));return true;}return super.mouseDragged(e,dx,dy);}
    @Override public boolean mouseReleased(MouseButtonEvent e){if(drag!=null||dragCube!=null){drag=null;dragCube=null;setDragging(false);rebuildWidgets();return true;}return super.mouseReleased(e);}
    private float propScale(){return Math.max(1,Math.min((right()-left())/50f,(bottom()-top())/40f));}
    private int[] point(int x,int y,int z){float s=propScale();return new int[]{(left()+right())/2+Math.round((x-z)*.7f*s),bottom()-8-Math.round((y+(x+z)*.3f)*s)};}
    private static void polygon(GuiGraphicsExtractor g,int[][] p,int color){int low=Arrays.stream(p).mapToInt(v->v[1]).min().orElse(0),high=Arrays.stream(p).mapToInt(v->v[1]).max().orElse(0);for(int y=low;y<=high;y++){var xs=new ArrayList<Integer>();for(int i=0;i<p.length;i++){int[] a=p[i],b=p[(i+1)%p.length];if((a[1]<=y&&b[1]>y)||(b[1]<=y&&a[1]>y))xs.add(a[0]+(y-a[1])*(b[0]-a[0])/(b[1]-a[1]));}if(xs.size()>=2){xs.sort(Integer::compare);g.fill(xs.getFirst(),y,xs.getLast()+1,y+1,color);}}}
    @Override public void extractBackground(GuiGraphicsExtractor g,int x,int y,float a){g.fill(0,0,width,height,0xFF0C0F15);}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float a){
        g.fill(left(),top(),right(),bottom(),Palette.INK_DEEP);for(int x=left();x<right();x+=12)g.fill(x,top(),x+1,bottom(),0xFF232934);for(int y=top();y<bottom();y+=12)g.fill(left(),y,right(),y+1,0xFF232934);
        Ink.centered(g,Component.literal(mode.equals("Props")?title(prop.replace('_',' '))+" · drag block X/Y, edit Z":title(page)+" · F6 edits any mod page"),width/2,36,Palette.TEXT,1);
        if(mode.equals("Props")){
            var list=UiDesign.prop(prop);var order=new ArrayList<>(list);order.sort(Comparator.comparingInt((UiDesign.Cube c)->c.x()+c.z()).reversed());for(var c:order){int x=c.x(),y=c.y(),z=c.z(),w=c.w(),h=c.h(),d=c.d();int color=UiDesign.MATERIALS[c.material()];polygon(g,new int[][]{point(x,y,z),point(x+w,y,z),point(x+w,y+h,z),point(x,y+h,z)},color);polygon(g,new int[][]{point(x,y,z),point(x,y,z+d),point(x,y+h,z+d),point(x,y+h,z)},net.minecraft.util.ARGB.scaleRGB(color,.72f));polygon(g,new int[][]{point(x,y+h,z),point(x+w,y+h,z),point(x+w,y+h,z+d),point(x,y+h,z+d)},net.minecraft.util.ARGB.scaleRGB(color,1.12f));if(c==list.get(cube)){int[] p=point(x,y+h,z);g.outline(p[0]-3,p[1]-3,7,7,Palette.gold());}}
        }else{
            g.pose().pushMatrix();g.pose().translate(left(),top());g.pose().scale((right()-left())/640f,(bottom()-top())/360f);
            for(var p:UiDesign.parts(page)){if(p.hidden())continue;int c=p.id().equals(selected)?Palette.gold():p.color();if(p.kind().equals("widget")||p.kind().equals("panel")){g.fill(p.x(),p.y(),p.x()+p.w(),p.y()+p.h(),0xFF242C39);g.outline(p.x(),p.y(),p.w(),p.h(),c);}else if(p.kind().equals("bar")||p.kind().equals("rule"))g.fill(p.x(),p.y(),p.x()+p.w(),p.y()+p.h(),c);g.text(font,p.label(),p.x()+2,p.y()+2,c,false);if(p.id().equals(selected))g.fill(p.x()+p.w()-8,p.y()+p.h()-8,p.x()+p.w(),p.y()+p.h(),c);}
            g.pose().popMatrix();
            if(UiDesign.parts(page).isEmpty())Ink.centered(g,Component.literal("Open page → F6 to capture its live controls"),(left()+right())/2,(top()+bottom())/2,Palette.TEXT_DIM,1);
        }
        String[] names=mode.equals("Props")?new String[]{"X","Y","Z","Width","Height","Depth"}:new String[]{"X","Y","Width","Height"};for(int i=0;i<names.length;i++)Ink.text(g,names[i],width-101+i%2*48,61+i/2*30,Palette.TEXT_DIM,1);
        Ink.text(g,Ink.fit(Component.literal(feedback),width-16),8,height-22,Palette.TEXT_DIM,1);Ink.text(g,"UI: 640 × 360 grid · props: 1 unit = 1/16 block",8,height-11,Palette.TEXT_DIM,1);
        super.extractRenderState(g,mx,my,a);
    }
    @Override public boolean isPauseScreen(){return false;}
    @Override public void onClose(){minecraft.gui.setScreen(parent);}
}
