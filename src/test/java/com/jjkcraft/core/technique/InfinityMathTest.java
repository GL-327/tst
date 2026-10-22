package com.jjkcraft.core.technique;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InfinityMathTest {
    @Test void incomingMotionSlowsProgressivelyAndStops(){
        double outer=InfinityMath.inwardSpeed(.5,5,1,6,.55),near=InfinityMath.inwardSpeed(.5,1.5,1,6,.55);
        assertTrue(outer<.5&&near<outer);assertEquals(0,InfinityMath.inwardSpeed(.5,1.1,1,6,.55));
        assertEquals(0,InfinityMath.inwardSpeed(.005,.8,1,6,.55));
    }
    @Test void fastShotsCannotCrossTheInnerGapInOneStep(){
        for(double d=1.13;d<6;d+=.1)assertTrue(InfinityMath.inwardSpeed(100,d,1,6,.99)<d-1.12);
    }
    @Test void repulsionExistsOnlyAtContact(){
        assertEquals(0,InfinityMath.contactPush(3,1));assertEquals(0,InfinityMath.contactPush(1,1));
        assertTrue(InfinityMath.contactPush(.5,1)>0&&InfinityMath.contactPush(.5,1)<=.06);
    }
}
