package com.jjkcraft.technique;

import com.mojang.serialization.Codec;

/** Explicit restrictions for one expression; restrictions on everyday actions apply character-wide. */
public record TechniquePact(int flags) {
    public static final TechniquePact NONE=new TechniquePact(0);
    public static final int COMBAT=0,GUARD=1,DASH=2,SINGLE=3,STILL=4,HEALING=5,WOUNDED=6,CHANT=7,RECOVERY=8,OVERCHARGE=9;
    public static final Codec<TechniquePact> CODEC=Codec.INT.xmap(TechniquePact::new,TechniquePact::flags);
    public static final String[] NAMES={"Forsake combat","Open guard","Grounded steps","One power only","Motionless casting","Forsake RCT","Half-health casting","Spoken incantation","Long recovery","Double CE offering"};
    public static final String[] TERMS={"No ordinary weapon attacks, even outside combat mode. This power gains x5 output.","No blocking or parrying. This power gains x1.6 output.","No combat dash or dash techniques. This power gains x1.6 output.","Only this technique may be cast. This power gains x2 output.","Stand still on the ground to cast and maintain this power. Gains x1.8 output.","No learned RCT or healing techniques. This power gains x1.8 output.","Use this power only at half health or less. Gains x1.7 output.","Add one second of preparation to this power. Gains x1.5 output.","Triple this power's cooldown. Gains x1.75 output.","Double this power's CE cost and upkeep. Gains x1.8 output."};
    public TechniquePact {flags&=1023;}
    public boolean has(int flag){return (flags&(1<<flag))!=0;}
    public TechniquePact toggle(int flag){return new TechniquePact(flags^(1<<flag));}
    public static boolean movement(TechniqueDefinition d){return d.behavior() instanceof com.jjkcraft.technique.behavior.DashBehavior||d.behavior() instanceof com.jjkcraft.technique.behavior.ProjectionSorceryBehavior||d.behavior() instanceof com.jjkcraft.technique.behavior.MangaAbilityBehavior m&&(m.ability().startsWith("original/")&&m.ability().endsWith("/step")||java.util.Set.of("shadow_escape","soul_dash","frame_rush","mirror_step","swap_feint").contains(m.ability()));}
    public static boolean compatible(TechniqueDefinition d,int flag){return switch(flag){
        case DASH->!movement(d);
        case COMBAT->!(d.behavior() instanceof com.jjkcraft.technique.behavior.ProjectionSorceryBehavior);
        case GUARD->!(d.behavior() instanceof com.jjkcraft.technique.behavior.MangaAbilityBehavior m&&m.ability().startsWith("original/")&&m.ability().endsWith("/guard"));
        case HEALING->d.category()!=TechniqueCategory.HEALING&&!(d.behavior() instanceof com.jjkcraft.technique.behavior.MangaAbilityBehavior m&&m.ability().equals("positive_output"));
        case RECOVERY->d.cast().cooldown()>0;
        case OVERCHARGE->d.cast().cost()>0||d.cast().upkeep()>0||d.cast().mulCost()>0;
        default->true;
    };}
    public double output(){double p=1;double[] gains={5,1.6,1.6,2,1.8,1.8,1.7,1.5,1.75,1.8};for(int n=0;n<gains.length;n++)if(has(n))p*=gains[n];return Math.min(12,p);}
    public double cost(){return has(OVERCHARGE)?2:1;}
    public int recovery(){return has(RECOVERY)?3:1;}
    public int windup(){return has(CHANT)?20:0;}
}
