package com.jjkcraft.core.technique;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ChargeMathTest {
    @Test void strongerProfilesTakeLonger(){assertEquals(1,ChargeMath.fraction(24,"mini_blue"));assertEquals(1,ChargeMath.fraction(60,"lapse_blue"));assertTrue(ChargeMath.fraction(60,"maximum_output_blue")<.7);assertEquals(1,ChargeMath.fraction(120,"maximum_output_red"));}
    @Test void purpleOverchargeSlowsAndFitsTheHoldLimit(){assertEquals(1,ChargeMath.fraction(120,"hollow_purple"));assertEquals(1.5,ChargeMath.fraction(220,"hollow_purple"));assertEquals(2,ChargeMath.fraction(320,"hollow_purple"));assertEquals(2,ChargeMath.fraction(400,"hollow_purple"));}
    @Test void ordinaryChargeCapsAtOneHundred(){assertEquals(.25,ChargeMath.fraction(0,false));assertEquals(1,ChargeMath.fraction(60,false));assertEquals(1,ChargeMath.fraction(400,false));}
    @Test void purpleCanOverchargeToTwoHundred(){assertEquals(1,ChargeMath.fraction(60,true));assertEquals(2,ChargeMath.fraction(140,true));assertEquals(2,ChargeMath.fraction(400,true));}
    @Test void holdingAtCapIsIncreasinglyExpensive(){assertTrue(ChargeMath.upkeep(80,120,false,false)>ChargeMath.upkeep(80,60,false,false)*2);assertTrue(ChargeMath.upkeep(160,140,true,false)>ChargeMath.upkeep(160,60,true,false)*3);}
    @Test void sixEyesSubstantiallyReducesChargeDrain(){assertEquals(.15,ChargeMath.upkeep(100,100,true,true)/ChargeMath.upkeep(100,100,true,false),1e-9);}
}
