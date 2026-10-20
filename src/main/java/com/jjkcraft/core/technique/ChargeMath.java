package com.jjkcraft.core.technique;

/** Shared deterministic charge curve. Holding at the cap keeps consuming CE. */
public final class ChargeMath {
    private ChargeMath() { }
    public static double fraction(int ticks,boolean purple){return Math.min(purple?2:1,.25+Math.max(0,ticks)/80.0);}
    public static double upkeep(double cost,int ticks,boolean purple,boolean sixEyes){
        double q=fraction(ticks,purple),over=Math.max(0,ticks-(purple?140:60))/20.0;
        return Math.max(.05,cost)*(.0125+.075*q*q+.025*over*over)*(sixEyes?.15:1);
    }
}
