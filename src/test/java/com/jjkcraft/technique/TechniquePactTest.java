package com.jjkcraft.technique;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class TechniquePactTest {
    @Test void combatOfferingHasExactFivefoldOutput(){var p=TechniquePact.NONE.toggle(TechniquePact.COMBAT);assertEquals(5,p.output());assertEquals(1,p.cost());assertTrue(p.has(TechniquePact.COMBAT));}
    @Test void combinationsHaveFiniteCapAndTogglesAreReversible(){var p=new TechniquePact(1023);assertEquals(12,p.output());assertEquals(2,p.cost());assertEquals(3,p.recovery());assertEquals(20,p.windup());assertEquals(TechniquePact.NONE,TechniquePact.NONE.toggle(3).toggle(3));}
    @Test void unknownFlagsCannotEscapeTheKnownRestrictions(){assertEquals(0,new TechniquePact(1<<20).flags());assertEquals(1023,new TechniquePact(-1).flags());}
}
