package com.jjkcraft.client.training;
import com.jjkcraft.training.TrainingState;
import com.jjkcraft.network.payload.TrainingPayload;
import net.fabricmc.fabric.api.client.networking.v1.*;
public final class TrainingClient{
    public static TrainingState state=TrainingState.EMPTY;
    public static void init(){ClientPlayNetworking.registerGlobalReceiver(TrainingPayload.TYPE,(p,c)->state=p.state());ClientPlayConnectionEvents.DISCONNECT.register((h,c)->state=TrainingState.EMPTY);}
}
