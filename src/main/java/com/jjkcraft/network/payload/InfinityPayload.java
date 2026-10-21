package com.jjkcraft.network.payload;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import com.jjkcraft.JjkCraft;
public record InfinityPayload(boolean enabled) implements CustomPacketPayload {
    public static final Type<InfinityPayload> TYPE=new Type<>(JjkCraft.id("infinity_passive"));
    public static final StreamCodec<ByteBuf,InfinityPayload> CODEC=ByteBufCodecs.BOOL.map(InfinityPayload::new,InfinityPayload::enabled);
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
