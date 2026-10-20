package com.jjkcraft.core.energy;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class UnboundedEnergyTest {
    @Test void emptyBankGeneratesEnergyPastReferenceWithoutCompounding(){
        var p=EnergyParams.DEFAULT;var zero=new EnergyState(0,0,0,0);
        var a=EnergyMath.advanceUnbounded(zero,100,20000,p,1);
        assertTrue(a.current()>1000&&Double.isFinite(a.current()));
        var b=EnergyMath.advanceUnbounded(new EnergyState(50000,0,0,0),100,20000,p,1);
        assertEquals(a.current()+50000,b.current(),1e-6);
    }
    @Test void pausedBankDoesNotRefillOrClampAtReference(){
        var state=new EnergyState(50000,0,0,0);
        assertEquals(50000,EnergyMath.advanceUnbounded(state,100,20000,EnergyParams.DEFAULT,0).current());
    }
}
