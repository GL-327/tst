package com.jjkcraft.core.technique;

/** Infinity removes inward motion, never adds an outward force beyond contact distance. */
public final class InfinityMath {
    private InfinityMath() {}
    public static double inwardSpeed(double speed,double distance,double contact,double radius,double slow){
        double gap=Math.max(0,distance-contact);
        if(gap<=.12)return 0;
        double approach=Math.clamp(gap/Math.max(.01,radius-contact),0,1);
        double retention=Math.clamp(slow,0,1)*(.2+.8*Math.sqrt(approach));
        return Math.min(Math.max(0,speed)*retention,Math.max(0,gap-.12)*.22);
    }
    public static double contactPush(double distance,double contact){return .06*Math.clamp((contact-distance)/Math.max(.01,contact),0,1);}
}
