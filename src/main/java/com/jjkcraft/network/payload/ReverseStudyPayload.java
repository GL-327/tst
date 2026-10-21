package com.jjkcraft.network.payload;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import com.jjkcraft.JjkCraft;
public record ReverseStudyPayload(int lessons,int ticks,boolean running) implements CustomPacketPayload {
    public static final Type<ReverseStudyPayload> TYPE=new Type<>(JjkCraft.id("reverse_study"));
    public static final StreamCodec<ByteBuf,ReverseStudyPayload> CODEC=StreamCodec.composite(ByteBufCodecs.VAR_INT,ReverseStudyPayload::lessons,ByteBufCodecs.VAR_INT,ReverseStudyPayload::ticks,ByteBufCodecs.BOOL,ReverseStudyPayload::running,ReverseStudyPayload::new);
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
