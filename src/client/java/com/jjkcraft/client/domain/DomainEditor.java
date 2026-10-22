package com.jjkcraft.client.domain;
import com.jjkcraft.domain.DomainProfile;
import com.jjkcraft.network.payload.DomainProfilePayload;
import net.fabricmc.fabric.api.client.networking.v1.*;
public final class DomainEditor {
    public static DomainProfile profile=DomainProfile.DEFAULT;
    public static boolean accepted;
    public static int revision;
    public static void init(){ClientPlayNetworking.registerGlobalReceiver(DomainProfilePayload.TYPE,(p,c)->{profile=p.profile();accepted=p.save();revision++;});ClientPlayConnectionEvents.DISCONNECT.register((h,m)->{profile=DomainProfile.DEFAULT;accepted=false;revision++;});}
}
