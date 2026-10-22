package com.jjkcraft.core.technique;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MasteryScalingTest {
    @Test void trainingContinuesAfterFirstMilestone(){
        assertEquals(1,TechniqueMath.masteryPower(0));assertEquals(1.6,TechniqueMath.masteryPower(1));assertEquals(2.25,TechniqueMath.masteryPower(2));assertEquals(3,TechniqueMath.masteryPower(4));
        assertEquals(.65,TechniqueMath.masteryCost(4),1e-9);assertEquals(.65,TechniqueMath.masteryRecovery(4),1e-9);
    }
    @Test void everyLandedUseHelpsUntilTheCap(){
        double previous=0;
        for(int n=0;n<=200;n++){
            double mastery=TechniqueMath.mastery(n,50),power=TechniqueMath.power(4,12,mastery,1);
            assertTrue(power>previous);previous=power;
            assertTrue(TechniqueMath.cost(30,12,mastery,1)>0);
            assertTrue(TechniqueMath.cooldown(80,12,mastery)>=1);
        }
        assertEquals(previous,TechniqueMath.power(4,12,TechniqueMath.mastery(Integer.MAX_VALUE,50),1));
    }
    @Test void legacyAndBoundaryDataRemainBounded(){
        assertEquals(0,TechniqueMath.mastery(-1,50));assertEquals(4,TechniqueMath.mastery(Integer.MAX_VALUE,0));
        assertEquals(0,TechniqueMath.cooldown(0,40,4));assertEquals(1,TechniqueMath.cooldown(1,40,4));
        assertEquals(1.3,TechniqueMath.masteryReach(4),1e-9);assertEquals(1.5,TechniqueMath.masteryDuration(4),1e-9);
        assertEquals(TechniqueMath.power(3,10,true,1),TechniqueMath.power(3,10,1.0,1));
    }
}
