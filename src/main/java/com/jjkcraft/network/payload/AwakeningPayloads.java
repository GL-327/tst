package com.jjkcraft.network.payload;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import com.jjkcraft.JjkCraft;
import com.jjkcraft.awakening.AwakeningRecord;

public final class AwakeningPayloads {
    private AwakeningPayloads() { }
    public record Choose(String path, String family) implements CustomPacketPayload {
        public static final Type<Choose> TYPE = new Type<>(JjkCraft.id("awakening_choose"));
        public static final StreamCodec<ByteBuf, Choose> CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(16), Choose::path, ByteBufCodecs.stringUtf8(128), Choose::family, Choose::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    public record View(AwakeningRecord record, boolean pending, boolean allowChoice) implements CustomPacketPayload {
        public static final Type<View> TYPE = new Type<>(JjkCraft.id("awakening_view"));
        public static final StreamCodec<ByteBuf, View> CODEC = StreamCodec.composite(
            ByteBufCodecs.fromCodec(AwakeningRecord.CODEC), View::record,
            ByteBufCodecs.BOOL, View::pending, ByteBufCodecs.BOOL, View::allowChoice, View::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
}
