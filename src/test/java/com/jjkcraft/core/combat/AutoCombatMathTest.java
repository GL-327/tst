package com.jjkcraft.core.combat;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class AutoCombatMathTest {
    @Test void telegraphedContactSuggestsGuard(){assertEquals(2,AutoCombatMath.action(3,-1,true));}
    @Test void guardSuggestsHeavyAndOpenTargetSuggestsLight(){assertEquals(5,AutoCombatMath.action(2,1,false));assertEquals(0,AutoCombatMath.action(2,-1,false));}
    @Test void distantThreatNeverRaisesGuardOrChoosesManualSpecial(){assertEquals(0,AutoCombatMath.action(8,-1,true));}
}
