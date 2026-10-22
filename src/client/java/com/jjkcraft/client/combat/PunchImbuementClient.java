package com.jjkcraft.client.combat;
import java.util.*;
import net.minecraft.client.Minecraft;
import com.jjkcraft.network.payload.CombatPayloads;
import net.fabricmc.fabric.api.client.networking.v1.*;

public final class PunchImbuementClient {
    private static final Map<Integer,CombatPayloads.Imbuement> STATES=new HashMap<>();
    private PunchImbuementClient() {}
    public static void init(){
        ClientPlayNetworking.registerGlobalReceiver(CombatPayloads.Imbuement.TYPE,(p,c)->STATES.put(p.entity(),p));
        ClientPlayConnectionEvents.DISCONNECT.register((h,m)->STATES.clear());
    }
    public static int mode(){var p=Minecraft.getInstance().player;var s=p==null?null:STATES.get(p.getId());return s==null?0:s.mode();}
    public static boolean ce(){var p=Minecraft.getInstance().player;var s=p==null?null:STATES.get(p.getId());return s!=null&&s.ce();}
    public static String label(){return (ce()?"CE fists":"")+(ce()&&mode()!=0?" + ":"")+(mode()==1?"Blue fists":mode()==2?"Red fists":"");}
}
