package com.jjkcraft.core.fear;

/** Encounter policy is independent of a particular mob's datapack spawn grouping. */
public final class CurseEncounterMath {
    private CurseEncounterMath(){}
    public static int group(int rank,double roll,int budget){return Math.max(0,Math.min(budget,rank>=3?1:rank>=1?2+(int)(Math.clamp(roll,0,0.999999)*2):2+(int)(Math.clamp(roll,0,0.999999)*3)));}
    public static double rarity(int rank){return rank>=6?0.12:rank>=3?0.35:1;}
    public static boolean solitary(int rank){return rank>=3;}
}
