package com.jjkcraft.technique;
import java.util.*;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
/** Relative right/forward waypoints, with a real framed combat action at each stop. */
public record ProjectionPlan(boolean automatic,List<Step> steps) {
    public record Step(double right,double forward,int action){
        public static final Codec<Step> CODEC=RecordCodecBuilder.create(i->i.group(Codec.doubleRange(-12,12).fieldOf("right").forGetter(Step::right),Codec.doubleRange(-12,12).fieldOf("forward").forGetter(Step::forward),Codec.intRange(0,6).fieldOf("action").forGetter(Step::action)).apply(i,Step::new));
    }
    public static final ProjectionPlan DEFAULT=new ProjectionPlan(true,List.of());
    public static final Codec<ProjectionPlan> CODEC=RecordCodecBuilder.create(i->i.group(Codec.BOOL.optionalFieldOf("automatic",true).forGetter(ProjectionPlan::automatic),Step.CODEC.listOf(0,8).optionalFieldOf("steps",List.of()).forGetter(ProjectionPlan::steps)).apply(i,ProjectionPlan::new));
    public boolean valid(){return steps.size()<=8&&(automatic||!steps.isEmpty())&&steps.stream().allMatch(s->Double.isFinite(s.right)&&Double.isFinite(s.forward)&&Math.abs(s.right)<=12&&Math.abs(s.forward)<=12&&s.action>=0&&s.action<=6);}
    public static final String[] ACTIONS={"Move","Jab","Hook","Uppercut","Sweep","Heavy","Guard"};
}
