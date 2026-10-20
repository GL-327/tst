package com.jjkcraft.technique;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.jjkcraft.technique.behavior.*;

/** Mechanical expression choices, saved independently for each player's individual move. */
public record TechniqueExpression(int yaw,int pitch,int gravity,int steering,int size,int pattern,int pulse,int summons,
        int vitality,int mobility,int lift,int falloff,int sacrifice,int recoil,int trail,int dash){
    public static final TechniqueExpression DEFAULT=new TechniqueExpression(0,0,-1,100,100,0,0,0,100,100,0,0,0,0,0,0);
    public static final Codec<TechniqueExpression> CODEC=RecordCodecBuilder.create(i->i.group(
        Codec.INT.optionalFieldOf("yaw",0).forGetter(TechniqueExpression::yaw),Codec.INT.optionalFieldOf("pitch",0).forGetter(TechniqueExpression::pitch),
        Codec.INT.optionalFieldOf("gravity",-1).forGetter(TechniqueExpression::gravity),Codec.INT.optionalFieldOf("steering",100).forGetter(TechniqueExpression::steering),
        Codec.INT.optionalFieldOf("size",100).forGetter(TechniqueExpression::size),Codec.INT.optionalFieldOf("pattern",0).forGetter(TechniqueExpression::pattern),
        Codec.INT.optionalFieldOf("pulse",0).forGetter(TechniqueExpression::pulse),Codec.INT.optionalFieldOf("summons",0).forGetter(TechniqueExpression::summons),
        Codec.INT.optionalFieldOf("vitality",100).forGetter(TechniqueExpression::vitality),Codec.INT.optionalFieldOf("mobility",100).forGetter(TechniqueExpression::mobility),
        Codec.INT.optionalFieldOf("lift",0).forGetter(TechniqueExpression::lift),Codec.INT.optionalFieldOf("falloff",0).forGetter(TechniqueExpression::falloff),
        Codec.INT.optionalFieldOf("sacrifice",0).forGetter(TechniqueExpression::sacrifice),Codec.INT.optionalFieldOf("recoil",0).forGetter(TechniqueExpression::recoil),
        Codec.INT.optionalFieldOf("trail",0).forGetter(TechniqueExpression::trail),Codec.INT.optionalFieldOf("dash",0).forGetter(TechniqueExpression::dash)
    ).apply(i,TechniqueExpression::new));
    public TechniqueExpression{
        yaw=clamp(yaw,-90,90);pitch=clamp(pitch,-60,60);gravity=clamp(gravity,-1,100);steering=clamp(steering,25,300);size=clamp(size,25,250);
        pattern=clamp(pattern,0,3);pulse=clamp(pulse,0,40);if(pulse>0&&pulse<4)pulse=4;summons=clamp(summons,0,6);vitality=clamp(vitality,25,300);mobility=clamp(mobility,25,200);
        lift=clamp(lift,-100,150);falloff=clamp(falloff,0,100);sacrifice=clamp(sacrifice,0,8);recoil=clamp(recoil,0,100);trail=clamp(trail,0,9);dash=clamp(dash,0,5);
    }
    private static int clamp(int v,int lo,int hi){return Math.clamp(v,lo,hi);}
    public int get(int axis){return switch(axis){case 16->yaw;case 17->pitch;case 18->gravity;case 19->steering;case 20->size;case 21->pattern;case 22->pulse;case 23->summons;case 24->vitality;case 25->mobility;case 26->lift;case 27->falloff;case 28->sacrifice;case 29->recoil;case 30->trail;default->dash;};}
    public TechniqueExpression with(int a,int v){return new TechniqueExpression(a==16?v:yaw,a==17?v:pitch,a==18?v:gravity,a==19?v:steering,a==20?v:size,a==21?v:pattern,a==22?v:pulse,a==23?v:summons,a==24?v:vitality,a==25?v:mobility,a==26?v:lift,a==27?v:falloff,a==28?v:sacrifice,a==29?v:recoil,a==30?v:trail,a==31?v:dash);}
    public static boolean applicable(TechniqueDefinition d,int axis){var b=d.behavior();return switch(axis){
        case 16,17->TunedBehaviors.reach(d);case 18,19,20,21->b instanceof ProjectileBehavior;
        case 22->b instanceof FieldBehavior||b instanceof BarrageBehavior;case 23->b instanceof SummonBehavior;case 24,25->b instanceof SummonBehavior||b instanceof FusionBehavior;
        case 26->d.category()!=TechniqueCategory.HEALING&&d.category()!=TechniqueCategory.UTILITY&&d.category()!=TechniqueCategory.PASSIVE&&!(b instanceof SummonBehavior);
        case 27->b instanceof FieldBehavior||b instanceof ProjectileBehavior p&&p.blastRadius()>0;case 31->b instanceof DashBehavior;default->true;};}
    public TechniqueExpression normalized(TechniqueDefinition d){var e=this;for(int a=16;a<=31;a++)if(!applicable(d,a))e=e.with(a,DEFAULT.get(a));return e;}
    public double cost(TechniqueDefinition d,boolean guided){
        double price=(1-sacrifice*0.04)*(1-recoil*0.0015)*(1-falloff*0.002)*(1+Math.abs(lift)*0.003);
        if(d.behavior() instanceof ProjectileBehavior)price*=Math.pow(size/100.0,0.45)*(guided?Math.pow(steering/100.0,0.25):1);
        if(d.behavior() instanceof SummonBehavior s)price*=Math.pow((summons==0?s.count():summons)/(double)s.count(),0.85)*Math.pow(vitality/100.0,0.6)*Math.pow(mobility/100.0,0.4);
        if(d.behavior() instanceof FusionBehavior)price*=Math.pow(vitality/100.0,0.6)*Math.pow(mobility/100.0,0.4);
        if(pulse>0){double base=d.behavior() instanceof BarrageBehavior b?b.interval():10;price*=Math.pow(base/pulse,0.2);}
        return Math.clamp(price,0.2,20);
    }
    public double falloff(double distance,double radius){return Math.pow(Math.max(0.05,1-Math.min(1,distance/Math.max(0.1,radius))),falloff/50.0);}
}
