package com.jjkcraft.core.combat;

/** Coarse assistance deliberately lacks manual cancels, parry timing and double-tap specials. */
public final class AutoCombatMath {
    private AutoCombatMath(){}
    /** Three committed attacks per five opportunities; the other two visibly feint. */
    public static boolean commit(long sequence){return Math.floorDiv((sequence+1)*3+4,5)>Math.floorDiv(sequence*3+4,5);}
    public static int recovery(int sequence){return 6+Math.floorMod(sequence,3)*2;}
    public static int action(double distance,int guard,boolean telegraphedThreat){
        if(telegraphedThreat&&distance<=4)return 2;
        return guard>=0&&distance<=3.4?5:0;
    }
}
