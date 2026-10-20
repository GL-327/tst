package com.jjkcraft.core.combat;

/** Coarse assistance deliberately lacks manual cancels, parry timing and double-tap specials. */
public final class AutoCombatMath {
    private AutoCombatMath(){}
    public static int action(double distance,int guard,boolean telegraphedThreat){
        if(telegraphedThreat&&distance<=4)return 2;
        return guard>=0&&distance<=3.4?5:0;
    }
}
