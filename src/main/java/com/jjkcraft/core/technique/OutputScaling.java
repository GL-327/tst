package com.jjkcraft.core.technique;

/** Shared earned expression ceilings. Absolute bounds also protect saved data and packets. */
public final class OutputScaling {
    public static final int MAX_OUTPUT=3000, MAX_REACH=600, MAX_AREA=400, MAX_DURATION=600;
    private static final int[] GRADE_OUTPUT={300,450,600,800,1050,1400,1900,2500};
    private OutputScaling(){}
    public static int outputLimit(int rank,int outputPoints,double mastery){
        double learned=Double.isFinite(mastery)?Math.clamp(mastery,0,4):0;
        return Math.min(MAX_OUTPUT,GRADE_OUTPUT[Math.clamp(rank,0,7)]+10*Math.clamp(outputPoints,0,20)+(int)Math.floor(75*learned));
    }
    public static int reachLimit(int rank){return Math.min(MAX_REACH,300+45*Math.clamp(rank,0,7));}
    public static int areaLimit(int rank){return Math.min(MAX_AREA,200+30*Math.clamp(rank,0,7));}
    public static int durationLimit(int rank){return Math.min(MAX_DURATION,300+45*Math.clamp(rank,0,7));}
}
