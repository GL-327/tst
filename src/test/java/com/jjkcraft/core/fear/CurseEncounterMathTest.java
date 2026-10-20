package com.jjkcraft.core.fear;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CurseEncounterMathTest {
    @Test void weakCursesTravelInSmallPacks(){assertEquals(2,CurseEncounterMath.group(0,0,12));assertEquals(4,CurseEncounterMath.group(0,1,12));assertEquals(3,CurseEncounterMath.group(1,1,12));}
    @Test void StrongCursesAreSolitaryAndRarer(){for(int r=3;r<=7;r++){assertEquals(1,CurseEncounterMath.group(r,0.99,12));assertTrue(CurseEncounterMath.rarity(r)<1);}}
    @Test void budgetsAreNeverExceeded(){assertEquals(0,CurseEncounterMath.group(0,0.9,0));assertEquals(1,CurseEncounterMath.group(0,0.9,1));}
}
