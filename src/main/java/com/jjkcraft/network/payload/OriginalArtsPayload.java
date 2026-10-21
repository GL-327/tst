package com.jjkcraft.network.payload;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import com.jjkcraft.JjkCraft;
public record OriginalArtsPayload(String family,int resonance,int wards,int wardTicks) implements CustomPacketPayload {
    public static final Type<OriginalArtsPayload> TYPE=new Type<>(JjkCraft.id("original_resonance"));
    public static final StreamCodec<ByteBuf,OriginalArtsPayload> CODEC=StreamCodec.composite(ByteBufCodecs.STRING_UTF8,OriginalArtsPayload::family,ByteBufCodecs.VAR_INT,OriginalArtsPayload::resonance,ByteBufCodecs.VAR_INT,OriginalArtsPayload::wards,ByteBufCodecs.VAR_INT,OriginalArtsPayload::wardTicks,OriginalArtsPayload::new);
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
