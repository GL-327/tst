package com.jjkcraft.technique;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.jjkcraft.core.technique.PersonalizationMath;

/** Each known move has its own persistent tuning, rather than modifying shared registry data. */
public record TechniqueTuning(int output, int reach, int area, int duration, int cooldown, int mode,TechniqueDetails details,TechniquePact pact) {
    public TechniqueTuning(int output,int reach,int area,int duration,int cooldown,int mode,TechniqueDetails details){this(output,reach,area,duration,cooldown,mode,details,TechniquePact.NONE);}
    public TechniqueTuning(int output,int reach,int area,int duration,int cooldown,int mode){this(output,reach,area,duration,cooldown,mode,TechniqueDetails.DEFAULT);}
    public static final TechniqueTuning DEFAULT = new TechniqueTuning(100,100,100,100,100,0);
    public static TechniqueTuning defaultFor(net.minecraft.resources.Identifier id){return id.getPath().equals("infinity")?new TechniqueTuning(100,100,100,100,100,2):DEFAULT;}
    public static final Codec<TechniqueTuning> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.INT.optionalFieldOf("output",100).forGetter(TechniqueTuning::output),
        Codec.INT.optionalFieldOf("reach",100).forGetter(TechniqueTuning::reach),
        Codec.INT.optionalFieldOf("area",100).forGetter(TechniqueTuning::area),
        Codec.INT.optionalFieldOf("duration",100).forGetter(TechniqueTuning::duration),
        Codec.INT.optionalFieldOf("cooldown",100).forGetter(TechniqueTuning::cooldown),
        Codec.INT.optionalFieldOf("mode",0).forGetter(TechniqueTuning::mode),TechniqueDetails.CODEC.optionalFieldOf("details",TechniqueDetails.DEFAULT).forGetter(TechniqueTuning::details),TechniquePact.CODEC.optionalFieldOf("pact",TechniquePact.NONE).forGetter(TechniqueTuning::pact)
    ).apply(i,TechniqueTuning::new));
    public TechniqueTuning {
        output=PersonalizationMath.clamp(output,25,300); reach=PersonalizationMath.clamp(reach,25,300);
        area=PersonalizationMath.clamp(area,50,200); duration=PersonalizationMath.clamp(duration,25,300);
        cooldown=PersonalizationMath.clamp(cooldown,25,400); mode=PersonalizationMath.clamp(mode,0,2);
        if(details==null)details=TechniqueDetails.DEFAULT; if(pact==null)pact=TechniquePact.NONE;
    }
    public double costMultiplier() { return PersonalizationMath.cost(output,reach,area,duration,cooldown); }
    public TechniqueTuning normalized(TechniqueDefinition d) {
        return new TechniqueTuning(TunedBehaviors.output(d.behavior())?output:100,TunedBehaviors.reach(d)?reach:100,TunedBehaviors.area(d.behavior())?area:100,
            TunedBehaviors.duration(d)?duration:100,cooldown,d.behavior() instanceof com.jjkcraft.technique.behavior.InfinityBehavior?mode:0,details.normalized(d),pact);
    }
    public TechniqueTuning with(int axis,int value) {
        return new TechniqueTuning(axis==0?value:output,axis==1?value:reach,axis==2?value:area,
            axis==3?value:duration,axis==4?value:cooldown,axis==5?value:mode,axis>=6?details.with(axis,value):details,pact);
    }
    public TechniqueTuning named(String name){return new TechniqueTuning(output,reach,area,duration,cooldown,mode,details.named(name),pact);}
    public TechniqueTuning withPact(TechniquePact next){return new TechniqueTuning(output,reach,area,duration,cooldown,mode,details,next);}
    public int get(int axis) { return switch(axis) { case 0->output;case 1->reach;case 2->area;case 3->duration;case 4->cooldown;case 5->mode;default->details.get(axis); }; }
    public TechniqueDefinition apply(TechniqueDefinition raw) {
        var valid=normalized(raw);if(!valid.equals(this))return valid.apply(raw);
        if(equals(DEFAULT))return raw;
        var c=raw.cast(); double cost=costMultiplier()*details.cost(raw)*pact.cost()*(raw.behavior() instanceof com.jjkcraft.technique.behavior.InfinityBehavior && mode==2?1.65:1);
        var cast=new TechniqueDefinition.Cast(Math.min(100000,c.cost()*cost),
            c.cooldown()==0?0:Math.max(1,(int)Math.round(c.cooldown()*cooldown/100.0*pact.recovery())),(details.windup()<0?c.castTime():details.windup())+pact.windup(),
            Math.min(256,c.range()*reach/100.0),c.mode(),Math.min(10000,c.upkeep()*cost),
            c.maxDuration()==0?0:Math.max(1,(int)Math.round(c.maxDuration()*duration/100.0)),Math.min(100000,c.mulCost()*cost),c.chargeable());
        var v=raw.visuals();var trail=details.expression().trail()==0?v.trail():TechniqueDefinition.Trail.values()[details.expression().trail()-1];
        var visual=new TechniqueDefinition.Visuals(details.color()<0?v.color():details.color(),v.particle(),v.sound(),v.hitSound(),v.shake(),v.hitStop(),trail);
        return new TechniqueDefinition(details.title().isEmpty()?raw.name():details.title(),raw.description(),raw.category(),raw.grade(),cast,
            TunedBehaviors.apply(raw.behavior(),this),visual,raw.icon(),raw.vowModifiers(),raw.innateWeight(),raw.masteryUses());
    }
}
