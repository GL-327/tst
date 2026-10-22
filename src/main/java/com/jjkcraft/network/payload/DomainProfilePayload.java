package com.jjkcraft.network.payload;
import com.jjkcraft.JjkCraft;
import com.jjkcraft.domain.DomainProfile;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
public record DomainProfilePayload(DomainProfile profile,boolean save) implements CustomPacketPayload {
    public static final Type<DomainProfilePayload> TYPE=new Type<>(JjkCraft.id("domain_profile"));
    public static final StreamCodec<ByteBuf,DomainProfilePayload> CODEC=StreamCodec.composite(ByteBufCodecs.fromCodec(DomainProfile.CODEC),DomainProfilePayload::profile,ByteBufCodecs.BOOL,DomainProfilePayload::save,DomainProfilePayload::new);
    public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
