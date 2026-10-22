package com.jjkcraft.network.payload;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import com.jjkcraft.JjkCraft;
public record InfinityPayload(boolean enabled,boolean preferred) implements CustomPacketPayload {
    public InfinityPayload(boolean enabled){this(enabled,enabled);}
    public static final Type<InfinityPayload> TYPE=new Type<>(JjkCraft.id("infinity_passive"));
    public static final StreamCodec<ByteBuf,InfinityPayload> CODEC=StreamCodec.composite(ByteBufCodecs.BOOL,InfinityPayload::enabled,ByteBufCodecs.BOOL,InfinityPayload::preferred,InfinityPayload::new);
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
