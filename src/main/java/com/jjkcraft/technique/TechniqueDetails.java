package com.jjkcraft.technique;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.jjkcraft.core.technique.PersonalizationMath;
import com.jjkcraft.technique.behavior.*;
/** Optional per-expression controls. Sentinel values preserve the original move's authored settings. */
public record TechniqueDetails(int speed,int shots,int spread,int homing,int penetration,int force,int windup,int center,int targeting,int color,String title,TechniqueExpression expression){
    public TechniqueDetails(int speed,int shots,int spread,int homing,int penetration,int force,int windup,int center,int targeting,int color,String title){this(speed,shots,spread,homing,penetration,force,windup,center,targeting,color,title,TechniqueExpression.DEFAULT);}
    public static final TechniqueDetails DEFAULT=new TechniqueDetails(100,0,-1,0,-1,100,-1,0,0,-1,"");
    public static final Codec<TechniqueDetails> CODEC=RecordCodecBuilder.create(i->i.group(
        Codec.INT.optionalFieldOf("speed",100).forGetter(TechniqueDetails::speed),Codec.INT.optionalFieldOf("shots",0).forGetter(TechniqueDetails::shots),
        Codec.INT.optionalFieldOf("spread",-1).forGetter(TechniqueDetails::spread),Codec.INT.optionalFieldOf("homing",0).forGetter(TechniqueDetails::homing),
        Codec.INT.optionalFieldOf("penetration",-1).forGetter(TechniqueDetails::penetration),Codec.INT.optionalFieldOf("force",100).forGetter(TechniqueDetails::force),
        Codec.INT.optionalFieldOf("windup",-1).forGetter(TechniqueDetails::windup),Codec.INT.optionalFieldOf("center",0).forGetter(TechniqueDetails::center),
        Codec.INT.optionalFieldOf("targeting",0).forGetter(TechniqueDetails::targeting),Codec.INT.optionalFieldOf("color",-1).forGetter(TechniqueDetails::color),
        Codec.string(0,32).optionalFieldOf("title","").forGetter(TechniqueDetails::title),TechniqueExpression.CODEC.optionalFieldOf("expression",TechniqueExpression.DEFAULT).forGetter(TechniqueDetails::expression)
    ).apply(i,TechniqueDetails::new));
    public TechniqueDetails{
        speed=clamp(speed,25,300);shots=clamp(shots,0,12);spread=clamp(spread,-1,90);homing=clamp(homing,0,2);penetration=clamp(penetration,-1,100);
        force=clamp(force,25,300);windup=clamp(windup,-1,100);center=clamp(center,0,1);targeting=clamp(targeting,0,3);color=clamp(color,-1,0xFFFFFF);
        title=title==null?"":title.replaceAll("[\\p{Cntrl}§]","").strip();if(title.length()>32)title=title.substring(0,32);
        if(expression==null)expression=TechniqueExpression.DEFAULT;
    }
    private static int clamp(int n,int lo,int hi){return PersonalizationMath.clamp(n,lo,hi);}
    public TechniqueDetails with(int axis,int value){return new TechniqueDetails(axis==6?value:speed,axis==7?value:shots,axis==8?value:spread,axis==9?value:homing,axis==10?value:penetration,axis==11?value:force,axis==12?value:windup,axis==13?value:center,axis==14?value:targeting,axis==15?value:color,title,axis>=16?expression.with(axis,value):expression);}
    public TechniqueDetails named(String name){return new TechniqueDetails(speed,shots,spread,homing,penetration,force,windup,center,targeting,color,name,expression);}
    public int get(int axis){return switch(axis){case 6->speed;case 7->shots;case 8->spread;case 9->homing;case 10->penetration;case 11->force;case 12->windup;case 13->center;case 14->targeting;case 15->color;default->expression.get(axis);};}
    public static boolean applicable(TechniqueDefinition d,int axis){return switch(axis){
        case 6,7,8,9->d.behavior() instanceof ProjectileBehavior;
        case 10->d.behavior() instanceof ProjectileBehavior||d.behavior() instanceof LineCutBehavior;
        case 11->d.behavior() instanceof FieldBehavior||d.behavior() instanceof SingularityBehavior||d.behavior() instanceof StrikeBehavior||d.behavior() instanceof ArcSlashBehavior||d.behavior() instanceof VowStrikeBehavior||d.behavior() instanceof ShockwaveBehavior||d.behavior() instanceof BarrierBehavior||d.behavior() instanceof MangaAbilityBehavior m&&java.util.Set.of("blue_punch","red_punch","blue_cluster").contains(m.ability());
        case 13->d.behavior() instanceof FieldBehavior;default->axis<16||TechniqueExpression.applicable(d,axis);};}
    public TechniqueDetails normalized(TechniqueDefinition d){var t=this;for(int axis=6;axis<=31;axis++)if(!applicable(d,axis))t=t.with(axis,DEFAULT.get(axis));return t;}
    public double cost(TechniqueDefinition def){
        double factor=Math.pow(speed/100.0,0.4)*Math.pow(force/100.0,0.4);
        if(def.behavior() instanceof ProjectileBehavior p){factor*=Math.pow((shots==0?p.count():shots)/(double)p.count(),0.85);if(homing==2&&!p.homing())factor*=1.5;}
        double old=def.behavior() instanceof LineCutBehavior b?b.pierce():0;
        if(penetration>=0)factor*=(1+penetration/100.0)/(1+old);
        if(windup>=0)factor*=Math.sqrt((def.cast().castTime()+10.0)/(windup+10.0));
        return Math.clamp(factor*expression.cost(def,homing==2||homing==0&&def.behavior() instanceof ProjectileBehavior p&&p.homing()),0.1,32);
    }
}
