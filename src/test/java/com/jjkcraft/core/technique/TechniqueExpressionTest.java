package com.jjkcraft.core.technique;
import com.jjkcraft.technique.TechniqueExpression;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class TechniqueExpressionTest {
    @Test void defaultsKeepAuthoredBehavior(){var e=TechniqueExpression.DEFAULT;assertEquals(0,e.sacrifice());assertEquals(0,e.pulse());assertEquals(-1,e.gravity());assertEquals(100,e.size());assertEquals(1,e.falloff(99,10));}
    @Test void eachEditIsImmutableAndClamped(){var original=TechniqueExpression.DEFAULT;var e=original.with(16,900).with(18,-900).with(22,1).with(28,900).with(31,900);assertEquals(90,e.yaw());assertEquals(-1,e.gravity());assertEquals(4,e.pulse());assertEquals(8,e.sacrifice());assertEquals(5,e.dash());assertEquals(0,original.yaw());}
    @Test void falloffTradesEdgeDamageWhileRetainingTheCenter(){var e=TechniqueExpression.DEFAULT.with(27,80);assertEquals(1,e.falloff(0,5));assertTrue(e.falloff(2,5)>e.falloff(4,5));assertTrue(e.falloff(5,5)>0&&e.falloff(5,5)<1);assertTrue(Double.isFinite(e.falloff(20,0)));}
}
