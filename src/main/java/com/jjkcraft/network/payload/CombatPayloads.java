package com.jjkcraft.network.payload;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import com.jjkcraft.JjkCraft;

public final class CombatPayloads {
    private CombatPayloads() { }
    public record Impact(int attacker,int target,int kind,double x,double y,double z) implements CustomPacketPayload {
        public static final Type<Impact> TYPE=new Type<>(JjkCraft.id("combat_impact"));
        public static final StreamCodec<ByteBuf,Impact> CODEC=StreamCodec.composite(ByteBufCodecs.VAR_INT,Impact::attacker,ByteBufCodecs.VAR_INT,Impact::target,ByteBufCodecs.VAR_INT,Impact::kind,ByteBufCodecs.DOUBLE,Impact::x,ByteBufCodecs.DOUBLE,Impact::y,ByteBufCodecs.DOUBLE,Impact::z,Impact::new);
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record Resources(int escapeCooldown,int knockdown,int chain) implements CustomPacketPayload {
        public static final Type<Resources> TYPE=new Type<>(JjkCraft.id("combat_resources"));
        public static final StreamCodec<ByteBuf,Resources> CODEC=StreamCodec.composite(ByteBufCodecs.VAR_INT,Resources::escapeCooldown,ByteBufCodecs.VAR_INT,Resources::knockdown,ByteBufCodecs.VAR_INT,Resources::chain,Resources::new);
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    /** 0 press, 1 release, 2 guard, 3 dash, 4 toggle; data holds movement/guard height only. */
    public record Input(int action, int data) implements CustomPacketPayload {
        public static final Type<Input> TYPE = new Type<>(JjkCraft.id("combat_input"));
        public static final StreamCodec<ByteBuf, Input> CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, Input::action, ByteBufCodecs.VAR_INT, Input::data, Input::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    public record View(boolean enabled, float guard, int combo, int stun, String move, int age) implements CustomPacketPayload {
        public static final Type<View> TYPE = new Type<>(JjkCraft.id("combat_view"));
        public static final StreamCodec<ByteBuf, View> CODEC = StreamCodec.composite(ByteBufCodecs.BOOL, View::enabled,
            ByteBufCodecs.FLOAT, View::guard, ByteBufCodecs.VAR_INT, View::combo, ByteBufCodecs.VAR_INT, View::stun,
            ByteBufCodecs.stringUtf8(64), View::move, ByteBufCodecs.VAR_INT, View::age, View::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
}
