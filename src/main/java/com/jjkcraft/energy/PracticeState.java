package com.jjkcraft.energy;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.jjkcraft.core.technique.PersonalizationMath;

public record PracticeState(int birth, int trained, boolean reinforcement, boolean active, int output, int healing, int healBelow, int regenLevel, boolean rolled, int potentialVersion) {
    public static final int CURRENT_POTENTIAL=2;
    public PracticeState(int birth,int trained,boolean reinforcement,boolean active,int output,int healing,int healBelow,int regenLevel,boolean rolled){this(birth,trained,reinforcement,active,output,healing,healBelow,regenLevel,rolled,CURRENT_POTENTIAL);}
    public PracticeState(int birth,int trained,boolean reinforcement,boolean active,int output,int healing,int healBelow){this(birth,trained,reinforcement,active,output,healing,healBelow,0,false,CURRENT_POTENTIAL);}
    public static final PracticeState EMPTY = new PracticeState(0,0,false,false,100,100,60,0,false,0);
    public static final Codec<PracticeState> CODEC=RecordCodecBuilder.create(i->i.group(
        Codec.INT.optionalFieldOf("birth",0).forGetter(PracticeState::birth),Codec.INT.optionalFieldOf("trained",0).forGetter(PracticeState::trained),
        Codec.BOOL.optionalFieldOf("reinforcement",false).forGetter(PracticeState::reinforcement),Codec.BOOL.optionalFieldOf("active",false).forGetter(PracticeState::active),
        Codec.INT.optionalFieldOf("output",100).forGetter(PracticeState::output),Codec.INT.optionalFieldOf("healing",100).forGetter(PracticeState::healing),
        Codec.INT.optionalFieldOf("heal_below",60).forGetter(PracticeState::healBelow),
        Codec.INT.optionalFieldOf("regen_level",0).forGetter(PracticeState::regenLevel),Codec.BOOL.optionalFieldOf("potential_rolled",false).forGetter(PracticeState::rolled),
        Codec.INT.optionalFieldOf("potential_version",0).forGetter(PracticeState::potentialVersion)
    ).apply(i,PracticeState::new));
    public PracticeState { birth=PersonalizationMath.clamp(birth,0,3000);trained=PersonalizationMath.clamp(trained,0,10000);
        output=PersonalizationMath.clamp(output,25,com.jjkcraft.core.technique.OutputScaling.MAX_OUTPUT);healing=PersonalizationMath.clamp(healing,25,com.jjkcraft.core.technique.OutputScaling.MAX_OUTPUT);healBelow=PersonalizationMath.clamp(healBelow,25,100);regenLevel=PersonalizationMath.clamp(regenLevel,0,10);active&=reinforcement; }
    public PracticeState withBirth(int b){return new PracticeState(b,trained,reinforcement,active,output,healing,healBelow,regenLevel,rolled,potentialVersion);}
    public PracticeState withPotential(int b,int r){return new PracticeState(b,trained,reinforcement,active,output,healing,healBelow,r,true,CURRENT_POTENTIAL);}
    public PracticeState withTraining(){return withTraining(25);}
    public PracticeState withTraining(int gain){return new PracticeState(birth,trained+gain,reinforcement,active,output,healing,healBelow,regenLevel,rolled,potentialVersion);}
    public PracticeState withReinforcement(boolean learned,boolean enabled){return new PracticeState(birth,trained,learned,enabled,output,healing,healBelow,regenLevel,rolled,potentialVersion);}
    public PracticeState tune(int attack,int heal,int threshold){return new PracticeState(birth,trained,reinforcement,active,attack,heal,threshold,regenLevel,rolled,potentialVersion);}
}
