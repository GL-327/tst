package com.jjkcraft.core.technique;

/** Shared, finite trade-offs. Percentages are clamped again on the server. */
public final class PersonalizationMath {
    private PersonalizationMath() { }
    public static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
    public static double cost(int output, int reach, int area, int duration, int cooldown) {
        double value = Math.pow(clamp(output,25,OutputScaling.MAX_OUTPUT)/100.0,1.65)
            * Math.pow(clamp(reach,25,OutputScaling.MAX_REACH)/100.0,0.55)
            * Math.pow(clamp(area,50,OutputScaling.MAX_AREA)/100.0,0.75)
            * Math.pow(clamp(duration,25,OutputScaling.MAX_DURATION)/100.0,0.45)
            * Math.pow(clamp(cooldown,25,400)/100.0,-0.8);
        return Math.max(0.15, Math.min(4096, value));
    }
    /** A technique roll never influences birth reserves. Birth luck gives a modest advantage without thirtyfold starting pools. */
    public static int birthReserve(double roll) {
        double r = Math.max(0, Math.min(Math.nextDown(1.0), roll));
        return 350 + 10 * (int)Math.floor(21 * Math.pow(r, 2));
    }
    public static int regenLevel(double roll){return 4+(int)Math.floor(Math.max(0,Math.min(Math.nextDown(1.0),roll))*3);}
    public static double regenMultiplier(int level){return level<=0?1:0.4+0.2*clamp(level,1,10);}
    public static int trainingGain(double capacity){return Math.max(25,(int)Math.round(125/(1+Math.max(0,capacity)/500)));}
    public static int trainingPrice(double capacity){return clamp(1+(int)Math.floor(Math.max(0,capacity)/500),1,30);}
}
