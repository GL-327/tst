package com.jjkcraft.network.payload;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import com.jjkcraft.JjkCraft;
import com.jjkcraft.awakening.TraitState;

public record TraitPayload(int entity, TraitState state) implements CustomPacketPayload {
    public static final Type<TraitPayload> TYPE = new Type<>(JjkCraft.id("traits"));
    public static final StreamCodec<ByteBuf,TraitPayload> CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT,TraitPayload::entity,
        ByteBufCodecs.fromCodec(TraitState.CODEC),TraitPayload::state,TraitPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
