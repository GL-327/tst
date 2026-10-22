package com.jjkcraft.core.technique;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class PersonalizationMathTest {
    @Test void lowReservesGetLargerCheaperTrainingAndRegenHasIndependentLevels(){
        assertTrue(PersonalizationMath.trainingGain(100)>PersonalizationMath.trainingGain(3000));
        assertTrue(PersonalizationMath.trainingPrice(100)<PersonalizationMath.trainingPrice(3000));
        assertEquals(4,PersonalizationMath.regenLevel(0));assertEquals(6,PersonalizationMath.regenLevel(1));
        assertTrue(PersonalizationMath.regenMultiplier(1)<PersonalizationMath.regenMultiplier(10));
    }
    @Test void defaultsPreserveExistingBalance(){assertEquals(1,PersonalizationMath.cost(100,100,100,100,100),1e-9);}
    @Test void greaterBenefitsCostMoreAndLongerCooldownSavesEnergy(){
        assertTrue(PersonalizationMath.cost(200,100,100,100,100)>1);
        assertTrue(PersonalizationMath.cost(100,200,100,100,100)>1);
        assertTrue(PersonalizationMath.cost(100,100,200,100,100)>1);
        assertTrue(PersonalizationMath.cost(100,100,100,200,100)>1);
        assertTrue(PersonalizationMath.cost(100,100,100,100,25)>1);
        assertTrue(PersonalizationMath.cost(100,100,100,100,400)<1);
    }
    @Test void extremePacketsCannotMakeFreeOrInfiniteCasts(){
        for(int o:new int[]{Integer.MIN_VALUE,25,100,300,Integer.MAX_VALUE})for(int c:new int[]{Integer.MIN_VALUE,25,400,Integer.MAX_VALUE}){
            double cost=PersonalizationMath.cost(o,o,o,o,c);assertTrue(Double.isFinite(cost)&&cost>=0.15&&cost<=4096);
        }
    }
    @Test void birthRollHasLowAndHighPotentialAndNeverExceedsBounds(){
        assertEquals(350,PersonalizationMath.birthReserve(0));assertEquals(550,PersonalizationMath.birthReserve(1));
        assertTrue(PersonalizationMath.birthReserve(1)/(double)PersonalizationMath.birthReserve(0)<1.6);
        for(int n=0;n<=10000;n++){int reserve=PersonalizationMath.birthReserve(n/10000.0);assertTrue(reserve>=350&&reserve<=550);}
    }
}
