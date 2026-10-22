package com.jjkcraft.network.payload;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import com.jjkcraft.JjkCraft;

public final class CombatPayloads {
    private CombatPayloads() { }
    public record Imbuement(int entity,int mode,boolean ce) implements CustomPacketPayload {
        public static final Type<Imbuement> TYPE=new Type<>(JjkCraft.id("punch_imbuement"));
        public static final StreamCodec<ByteBuf,Imbuement> CODEC=StreamCodec.composite(ByteBufCodecs.VAR_INT,Imbuement::entity,ByteBufCodecs.VAR_INT,Imbuement::mode,ByteBufCodecs.BOOL,Imbuement::ce,Imbuement::new);
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record AutoStats(int attempts,int hits,int feints) implements CustomPacketPayload {
        public static final Type<AutoStats> TYPE=new Type<>(JjkCraft.id("auto_combat_stats"));
        public static final StreamCodec<ByteBuf,AutoStats> CODEC=StreamCodec.composite(ByteBufCodecs.VAR_INT,AutoStats::attempts,ByteBufCodecs.VAR_INT,AutoStats::hits,ByteBufCodecs.VAR_INT,AutoStats::feints,AutoStats::new);
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
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
    public record Endurance(float stamina,float maximum,int hand,int leftReady,int rightReady) implements CustomPacketPayload {
        public static final Type<Endurance> TYPE=new Type<>(JjkCraft.id("combat_endurance"));
        public static final StreamCodec<ByteBuf,Endurance> CODEC=StreamCodec.composite(ByteBufCodecs.FLOAT,Endurance::stamina,ByteBufCodecs.FLOAT,Endurance::maximum,ByteBufCodecs.VAR_INT,Endurance::hand,ByteBufCodecs.VAR_INT,Endurance::leftReady,ByteBufCodecs.VAR_INT,Endurance::rightReady,Endurance::new);
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    /** A combat key event. {@code data} carries the guard height, dash direction or assisted choice. */
    public record Input(int action, int data) implements CustomPacketPayload {
        public static final int RIGHT_PUNCH = 0, RELEASE = 1, GUARD = 2, DASH = 3, TOGGLE = 4, HEAVY = 5,
                ASSISTED_ATTACK = 6, ASSISTED_GUARD = 7, CE_FISTS = 8, LEFT_PUNCH = 9;
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
