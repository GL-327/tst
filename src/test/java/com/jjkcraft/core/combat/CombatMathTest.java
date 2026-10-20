package com.jjkcraft.core.combat;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class CombatMathTest {
    @Test void fourHitChainResetsAfterPauseAndOffersThreeFinishers(){
        assertEquals(0,CombatMath.chainStep(-1,-100,0));assertEquals(1,CombatMath.chainStep(0,10,21));assertEquals(0,CombatMath.chainStep(2,10,35));assertEquals(0,CombatMath.chainStep(3,10,20));
        assertEquals("jab",CombatMath.chainMove(0,false,false));assertEquals("hook",CombatMath.chainMove(1,false,false));assertEquals("backfist",CombatMath.chainMove(2,false,false));
        assertEquals("heavy",CombatMath.chainMove(3,false,false));assertEquals("sweep",CombatMath.chainMove(3,false,true));assertEquals("uppercut",CombatMath.chainMove(3,true,false));
    }
    @Test void activeWindowExcludesStartupAndRecovery() { assertFalse(CombatMath.active(2, 3, 2)); assertTrue(CombatMath.active(3, 3, 2)); assertTrue(CombatMath.active(4, 3, 2)); assertFalse(CombatMath.active(5, 3, 2)); }
    @Test void parryRequiresHeightAndThreeTickWindow() { assertTrue(CombatMath.parry(10, 13, 1, 1)); assertFalse(CombatMath.parry(10, 14, 1, 1)); assertFalse(CombatMath.parry(10, 12, 0, 2)); assertFalse(CombatMath.parry(10, 9, 1, 1)); }
    @Test void ratioAndCombosAreBounded() { assertTrue(CombatMath.ratio(0.7)); assertTrue(CombatMath.ratio(0.3)); assertFalse(CombatMath.ratio(0.5)); assertEquals(1.35, CombatMath.combo(50), 0.0001); }
    @Test void timersAndPossessionWrapAtThreshold() { assertEquals(24000, CombatMath.decayAt(0, 20)); assertEquals(0, CombatMath.possession(5, 5)); assertEquals(2, CombatMath.possession(7, 5)); }
}
