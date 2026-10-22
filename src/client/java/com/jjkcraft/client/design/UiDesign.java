package com.jjkcraft.client.design;

import java.util.*;
import java.nio.file.*;
import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import com.jjkcraft.client.ui.*;

/** Client-only visual blueprints. Real controls keep their original callbacks and server checks. */
public final class UiDesign{
    public record Part(String id,String kind,String label,int x,int y,int w,int h,int color,boolean hidden){
        public Part move(int x,int y,int w,int h){return new Part(id,kind,label,Math.clamp(x,0,630),Math.clamp(y,0,350),Math.clamp(w,4,640-Math.clamp(x,0,630)),Math.clamp(h,2,360-Math.clamp(y,0,350)),color,hidden);}
    }
    public record Cube(int x,int y,int z,int w,int h,int d,int material){}
    public static final class Document{
        public int version=1,theme=0;
        public Map<String,Map<String,Part>> pages=new LinkedHashMap<>();
        public Map<String,List<Cube>> props=new LinkedHashMap<>();
    }
    public static final String[] PAGES={"ProfileScreen","TraitsScreen","GrowthTreeScreen","PracticeScreen","PracticeSettingsScreen","LoadoutScreen","FamilyTechniqueScreen","TechniqueAbilityScreen","TechniqueBindingsScreen","TechniqueWorkshopScreen","CreativeTechniqueScreen","TechniqueWheelScreen","DomainWorkshopScreen","ProjectionPlannerScreen","ShikigamiFusionScreen","VowBuilderScreen","CodexScreen","CullingScreen","AwakeningScreen","SoulScreen","ModuloChoiceScreen","HUD"};
    public static final int[] MATERIALS={0xFFBDA06A,0xFF715638,0xFF343F51,0xFFA44C4B,0xFFC7C3AD,0xFF65867B,0xFF343139,0xFFB4AF9A};
    private static final Gson GSON=new GsonBuilder().setPrettyPrinting().create();
    private static Document document=new Document();
    public static final Map<String,LinkedHashMap<String,Part>> LIVE=new LinkedHashMap<>();
    private UiDesign(){}
    public static Document document(){return document;}
    public static boolean mod(Screen s){return s!=null&&s.getClass().getName().startsWith("com.jjkcraft.")&&!(s instanceof DesignStudioScreen);}
    public static String page(Screen s){return s.getClass().getSimpleName();}
    public static Path path(){return FabricLoader.getInstance().getConfigDir().resolve("jjkcraft-design.json");}
    public static void init(){try{if(Files.exists(path())&&Files.size(path())<262144)importText(Files.readString(path()));}catch(Exception e){com.jjkcraft.JjkCraft.LOGGER.warn("Design could not load; keeping defaults",e);}theme(document.theme);}
    public static String exportText(){return GSON.toJson(document);}
    public static void importText(String text){
        if(text.length()>262144)throw new IllegalArgumentException("Design is too large");Document d=GSON.fromJson(text,Document.class);if(d==null||d.version!=1||d.pages==null||d.props==null||d.pages.size()>32||d.props.size()>4)throw new IllegalArgumentException("Invalid design document");
        d.pages.forEach((page,parts)->{if(!Arrays.asList(PAGES).contains(page)||parts==null||parts.size()>160)throw new IllegalArgumentException("Invalid page");parts.forEach((key,p)->{if(p==null||key.length()>100||p.id()==null||p.label()==null||p.label().length()>80||!List.of("widget","text","panel","label","bar","rule").contains(p.kind())||p.x()<0||p.y()<0||p.x()>640||p.y()>360||p.w()<2||p.h()<1||p.w()>640||p.h()>360)throw new IllegalArgumentException("Invalid part");});});
        d.props.forEach((name,cubes)->{if(!List.of("training_dummy","training_mat","focus_stone","balance_beam").contains(name)||cubes==null||cubes.size()>48||cubes.isEmpty())throw new IllegalArgumentException("Invalid prop");for(var c:cubes)if(c==null||c.x() < -16||c.x()>31||c.y()<0||c.y()>31||c.z() < -16||c.z()>31||c.w()<1||c.h()<1||c.d()<1||c.w()>32||c.h()>32||c.d()>32||c.x()+c.w()>32||c.y()+c.h()>32||c.z()+c.d()>32||c.material()<0||c.material()>7)throw new IllegalArgumentException("Prop bounds exceed 32 units");});
        document=d;theme(d.theme);
    }
    public static void save()throws java.io.IOException{Files.createDirectories(path().getParent());Path tmp=path().resolveSibling("jjkcraft-design.tmp");Files.writeString(tmp,exportText());Files.move(tmp,path(),StandardCopyOption.REPLACE_EXISTING);}
    public static void theme(int n){
        document.theme=Math.floorMod(n,3);int[][] p={{0xF012141C,0xFF10121A,0xFF414550,0xFFF0EADD,0xFFAAA9AA},{0xF01D1418,0xFF190F14,0xFF68404B,0xFFF5E5DA,0xFFBEA6A7},{0xF0111B1B,0xFF0D1515,0xFF3F615B,0xFFE5EDE1,0xFFA4BAB0}};var c=p[document.theme];Palette.INK=c[0];Palette.INK_DEEP=c[1];Palette.INK_EDGE=c[2];Palette.TEXT=c[3];Palette.TEXT_DIM=c[4];
    }
    public static void capture(Screen screen){
        if(!mod(screen)||screen.width<=0||screen.height<=0)return;var parts=LIVE.computeIfAbsent(page(screen),k->new LinkedHashMap<>());int index=0;
        for(var child:screen.children())if(child instanceof AbstractWidget w){String id="widget/"+index++;Part original=new Part(id,"widget",w.getMessage().getString(),w.getX()*640/screen.width,w.getY()*360/screen.height,Math.max(4,w.getWidth()*640/screen.width),Math.max(4,w.getHeight()*360/screen.height),Palette.TEXT,false);parts.put(id,original);var p=override(page(screen),id);if(p!=null){w.setX(p.x()*screen.width/640);w.setY(p.y()*screen.height/360);w.setWidth(Math.max(4,p.w()*screen.width/640));w.setHeight(Math.max(4,p.h()*screen.height/360));w.visible=!p.hidden();}}
    }
    public static Part override(String page,String id){return document.pages.getOrDefault(page,Map.of()).get(id);}
    public static void put(String page,Part part){document.pages.computeIfAbsent(page,k->new LinkedHashMap<>()).put(part.id(),part);}
    public static List<Part> parts(String page){var list=new LinkedHashMap<String,Part>(LIVE.getOrDefault(page,new LinkedHashMap<>()));list.putAll(document.pages.getOrDefault(page,Map.of()));return List.copyOf(list.values());}
    public static Part text(String label,int x,int y,int w,int h,int color){var m=Minecraft.getInstance();var s=m.gui.screen();if(!mod(s))return null;String key="text/"+Integer.toHexString(label.hashCode());var parts=LIVE.computeIfAbsent(page(s),k->new LinkedHashMap<>());if(parts.size()>160)return null;parts.put(key,new Part(key,"text",label.substring(0,Math.min(80,label.length())),x*640/s.width,y*360/s.height,Math.max(4,w*640/s.width),Math.max(4,h*360/s.height),color,false));return override(page(s),key);}
    private static final Map<Object,String> HUD_KEYS=new IdentityHashMap<>();
    public static Part hud(Object element,int x,int y,int w,int h,int sw,int sh){
        String key=HUD_KEYS.get(element);if(key==null){var hud=com.jjkcraft.client.config.ClientConfig.get().hud;for(var field:hud.getClass().getFields())try{if(field.get(hud)==element){key="hud/"+field.getName();HUD_KEYS.put(element,key);break;}}catch(IllegalAccessException ignored){}}
        if(key==null)return null;LIVE.computeIfAbsent("HUD",k->new LinkedHashMap<>()).put(key,new Part(key,"widget",key.substring(4),x*640/sw,y*360/sh,Math.max(4,w*640/sw),Math.max(4,h*360/sh),Palette.accent(),false));return override("HUD",key);
    }
    public static void decorations(Screen s,GuiGraphicsExtractor g){if(!mod(s))return;for(var p:document.pages.getOrDefault(page(s),Map.of()).values()){if(p.hidden()||p.kind().equals("widget")||p.kind().equals("text"))continue;int x=p.x()*s.width/640,y=p.y()*s.height/360,w=p.w()*s.width/640,h=p.h()*s.height/360;switch(p.kind()){case "panel"->{g.fill(x,y,x+w,y+h,Palette.INK);g.outline(x,y,w,h,p.color());}case "bar"->{g.fill(x,y,x+w,y+h,Palette.INK_DEEP);g.fill(x,y,x+w*3/4,y+h,p.color());}case "rule"->g.fill(x,y,x+w,y+1,p.color());default->g.text(Ink.font(),p.label(),x,y,p.color(),true);}}}
    public static List<Cube> prop(String key){return document.props.computeIfAbsent(key,k->switch(k){case "training_mat"->new ArrayList<>(List.of(new Cube(0,0,0,16,2,16,2),new Cube(1,2,1,14,1,14,3)));case "focus_stone"->new ArrayList<>(List.of(new Cube(2,0,2,12,6,12,6),new Cube(4,6,4,8,4,8,4)));case "balance_beam"->new ArrayList<>(List.of(new Cube(6,0,2,4,12,4,1),new Cube(6,0,10,4,12,4,1),new Cube(5,12,0,6,4,16,1)));default->new ArrayList<>(List.of(new Cube(-5,0,-5,10,2,10,1),new Cube(-1,2,-1,2,23,2,1),new Cube(-5,12,-3,10,11,6,0),new Cube(-4,24,-4,8,7,8,0),new Cube(-11,17,-2,6,3,4,0),new Cube(5,17,-2,6,3,4,0),new Cube(-5,14,-3,10,2,7,3)));});}
}
