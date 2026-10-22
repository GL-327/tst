package com.jjkcraft.network.payload;
import java.util.List;
import com.jjkcraft.JjkCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
public record VesselPayload(int fingers,int pressure,int phase,int remaining,List<Integer> falseTargets) implements CustomPacketPayload {
    public static final Type<VesselPayload> TYPE=new Type<>(JjkCraft.id("vessel_presence"));
    public static final StreamCodec<ByteBuf,VesselPayload> CODEC=StreamCodec.composite(ByteBufCodecs.VAR_INT,VesselPayload::fingers,ByteBufCodecs.VAR_INT,VesselPayload::pressure,ByteBufCodecs.VAR_INT,VesselPayload::phase,ByteBufCodecs.VAR_INT,VesselPayload::remaining,ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(8)),VesselPayload::falseTargets,VesselPayload::new);
    public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
