package com.jjkcraft.network.payload;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import com.jjkcraft.JjkCraft;

/** Server-owned progress; slot -1 clears the meter. */
public record ChargePayload(int slot,int percent,int maximum,float cePerSecond) implements CustomPacketPayload {
    public static final Type<ChargePayload> TYPE=new Type<>(JjkCraft.id("charge"));
    public static final StreamCodec<ByteBuf,ChargePayload> CODEC=StreamCodec.composite(
        ByteBufCodecs.VAR_INT,ChargePayload::slot,ByteBufCodecs.VAR_INT,ChargePayload::percent,
        ByteBufCodecs.VAR_INT,ChargePayload::maximum,ByteBufCodecs.FLOAT,ChargePayload::cePerSecond,ChargePayload::new);
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
