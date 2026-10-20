package com.jjkcraft.network.payload;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import com.jjkcraft.JjkCraft;
import com.jjkcraft.death.SoulState;

public final class SoulPayloads {
    private SoulPayloads() { }
    public record Action(String action) implements CustomPacketPayload {
        public static final Type<Action> TYPE = new Type<>(JjkCraft.id("soul_action"));
        public static final StreamCodec<ByteBuf, Action> CODEC = ByteBufCodecs.stringUtf8(32).map(Action::new, Action::action);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    public record Terms(int objectMinutes, int riteCost, int vesselCost, int possessionThreshold) {
        public static final Terms DEFAULT = new Terms(20, 100, 50, 3);
        public static final com.mojang.serialization.Codec<Terms> CODEC = com.mojang.serialization.codecs.RecordCodecBuilder.create(i -> i.group(
            com.mojang.serialization.Codec.INT.fieldOf("object_minutes").forGetter(Terms::objectMinutes),
            com.mojang.serialization.Codec.INT.fieldOf("rite_cost").forGetter(Terms::riteCost),
            com.mojang.serialization.Codec.INT.fieldOf("vessel_cost").forGetter(Terms::vesselCost),
            com.mojang.serialization.Codec.INT.fieldOf("possession_threshold").forGetter(Terms::possessionThreshold)
        ).apply(i, Terms::new));
    }
    public record View(SoulState state, long now, boolean hardcore, boolean vesselMenu, Terms terms) implements CustomPacketPayload {
        public static final Type<View> TYPE = new Type<>(JjkCraft.id("soul_view"));
        public static final StreamCodec<ByteBuf, View> CODEC = StreamCodec.composite(ByteBufCodecs.fromCodec(SoulState.CODEC), View::state,
            ByteBufCodecs.VAR_LONG, View::now, ByteBufCodecs.BOOL, View::hardcore, ByteBufCodecs.BOOL, View::vesselMenu,
            ByteBufCodecs.fromCodec(Terms.CODEC), View::terms, View::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
}
