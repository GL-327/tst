package com.jjkcraft.core.technique;

/** Shared deterministic charge curve. Holding at the cap keeps consuming CE. */
public final class ChargeMath {
    private ChargeMath() { }
    public static int pace(String id){return id.contains("maximum")||id.equals("hollow_purple")||id.equals("shrine_furnace")||id.equals("perfect_sphere")?160:id.startsWith("mini_")?32:80;}
    public static double fraction(int ticks,String id){
        double first=Math.min(1,.25+Math.max(0,ticks)/(double)pace(id));
        return id.equals("hollow_purple")?Math.min(2,first+Math.max(0,ticks-pace(id)*.75)/(pace(id)*1.25)):first;
    }
    public static double upkeep(double cost,int ticks,String id,boolean sixEyes){
        double q=fraction(ticks,id),cap=pace(id)*.75+(id.equals("hollow_purple")?pace(id)*1.25:0),over=Math.max(0,ticks-cap)/20;
        return Math.max(.05,cost)*(.009+.045*q*q+.015*over*over)*(sixEyes?.15:1);
    }
    public static double fraction(int ticks,boolean purple){return Math.min(purple?2:1,.25+Math.max(0,ticks)/80.0);}
    public static double upkeep(double cost,int ticks,boolean purple,boolean sixEyes){
        double q=fraction(ticks,purple),over=Math.max(0,ticks-(purple?140:60))/20.0;
        return Math.max(.05,cost)*(.0125+.075*q*q+.025*over*over)*(sixEyes?.15:1);
    }
}
