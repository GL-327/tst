package com.jjkcraft.core.technique;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class OutputScalingTest {
    @Test void gradeTrainingAndMasteryOpenHigherOutput(){
        assertEquals(300,OutputScaling.outputLimit(0,0,0));assertEquals(1400,OutputScaling.outputLimit(5,0,0));
        assertEquals(3000,OutputScaling.outputLimit(7,20,4));
        for(int rank=0;rank<7;rank++)assertTrue(OutputScaling.outputLimit(rank+1,0,0)>OutputScaling.outputLimit(rank,0,0));
        assertTrue(OutputScaling.outputLimit(5,20,4)>OutputScaling.outputLimit(5,0,0));
        assertEquals(3000,OutputScaling.outputLimit(Integer.MAX_VALUE,Integer.MAX_VALUE,Double.MAX_VALUE));
        assertEquals(300,OutputScaling.outputLimit(-100,-100,Double.NaN));
    }
    @Test void overdriveRemainsPaidBeyondTheOldThreeHundredPercentCeiling(){
        double previous=0;for(int output=100;output<=3000;output+=25){double cost=PersonalizationMath.cost(output,100,100,100,100);assertTrue(Double.isFinite(cost)&&cost>previous);previous=cost;}
        assertTrue(PersonalizationMath.cost(3000,100,100,100,100)>250);
    }
    @Test void veteranPowerIsSubstantiallyStrongerWhileRecoveryAndUtilityImprove(){
        double formerMax=1.56*1.4*3;double current=TechniqueMath.power(7,20,4.0,1);
        assertTrue(current>formerMax*4);assertTrue(TechniqueMath.power(5,20,1.0,1)>6);
        assertEquals(.5,TechniqueMath.masteryCost(4),1e-9);assertEquals(.5,TechniqueMath.masteryRecovery(4),1e-9);
        assertEquals(600,OutputScaling.reachLimit(7));assertEquals(400,OutputScaling.areaLimit(7));assertEquals(600,OutputScaling.durationLimit(7));
    }
}
