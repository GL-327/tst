package com.jjkcraft.core.combat;

public final class CombatMath {
    private CombatMath() { }
    public static int chainStep(int previous,long last,long now){return now-last>24?0:(previous+1)%4;}
    public static String chainMove(int step,boolean airborne,boolean crouching){return step==3?(airborne?"uppercut":crouching?"sweep":"heavy"):step==0?"jab":step==1?"hook":"backfist";}
    public static boolean active(int age, int startup, int active) { return age >= startup && age < startup + active; }
    public static boolean parry(long raised, long now, int guardHeight, int attackHeight) { return guardHeight == attackHeight && now >= raised && now - raised <= 3; }
    public static boolean ratio(double heightFraction) { return Math.abs(heightFraction - 0.7) <= 0.09 || Math.abs(heightFraction - 0.3) <= 0.09; }
    public static double combo(int hits) { return 1 + Math.min(0.35, Math.max(0, hits - 1) * 0.05); }
    public static long decayAt(long start, int minutes) { return start + Math.clamp(minutes, 1, 120) * 1200L; }
    public static int possession(int revivals, int threshold) { return Math.max(0, revivals) % Math.max(1, threshold); }
}
