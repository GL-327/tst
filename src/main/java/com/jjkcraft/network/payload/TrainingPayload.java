package com.jjkcraft.network.payload;
import com.jjkcraft.JjkCraft;
import com.jjkcraft.training.TrainingState;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
public record TrainingPayload(TrainingState state) implements CustomPacketPayload{
    public static final Type<TrainingPayload> TYPE=new Type<>(JjkCraft.id("physical_training"));
    public static final StreamCodec<ByteBuf,TrainingPayload> CODEC=ByteBufCodecs.fromCodec(TrainingState.CODEC).map(TrainingPayload::new,TrainingPayload::state);
    public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
