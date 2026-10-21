package com.jjkcraft.network.payload;

import com.jjkcraft.JjkCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Begin a real, learned variant from the family bound to this slot. Release uses CastPayload. */
public record VariantCastPayload(int slot,Identifier technique) implements CustomPacketPayload {
    public static final Type<VariantCastPayload> TYPE=new Type<>(JjkCraft.id("variant_cast"));
    public static final StreamCodec<ByteBuf,VariantCastPayload> CODEC=StreamCodec.composite(ByteBufCodecs.VAR_INT,VariantCastPayload::slot,Identifier.STREAM_CODEC,VariantCastPayload::technique,VariantCastPayload::new);
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
