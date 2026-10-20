package com.jjkcraft.network.payload;

import java.util.Map;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import com.mojang.serialization.Codec;
import com.jjkcraft.JjkCraft;
import com.jjkcraft.technique.TechniqueTuning;
import com.jjkcraft.energy.PracticeState;

public final class PersonalizationPayloads {
    private PersonalizationPayloads() { }
    public static final Codec<Map<Identifier,TechniqueTuning>> TUNINGS=Codec.unboundedMap(Identifier.CODEC,TechniqueTuning.CODEC);
    public record CreativeEdit(Identifier family,com.jjkcraft.awakening.CreativeSettings settings,int request,boolean apply) implements CustomPacketPayload {
        public static final Type<CreativeEdit> TYPE=new Type<>(JjkCraft.id("creative_edit"));
        public static final StreamCodec<ByteBuf,CreativeEdit> CODEC=StreamCodec.composite(Identifier.STREAM_CODEC,CreativeEdit::family,ByteBufCodecs.fromCodec(com.jjkcraft.awakening.CreativeSettings.CODEC),CreativeEdit::settings,ByteBufCodecs.VAR_INT,CreativeEdit::request,ByteBufCodecs.BOOL,CreativeEdit::apply,CreativeEdit::new);
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record CreativeReply(Identifier family,com.jjkcraft.awakening.CreativeSettings settings,int request,int result) implements CustomPacketPayload {
        public static final Type<CreativeReply> TYPE=new Type<>(JjkCraft.id("creative_reply"));
        public static final StreamCodec<ByteBuf,CreativeReply> CODEC=StreamCodec.composite(Identifier.STREAM_CODEC,CreativeReply::family,ByteBufCodecs.fromCodec(com.jjkcraft.awakening.CreativeSettings.CODEC),CreativeReply::settings,ByteBufCodecs.VAR_INT,CreativeReply::request,ByteBufCodecs.VAR_INT,CreativeReply::result,CreativeReply::new);
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record CreativeFamily(Identifier family,boolean mechamaru) implements CustomPacketPayload {
        public static final Type<CreativeFamily> TYPE=new Type<>(JjkCraft.id("creative_family"));
        public static final StreamCodec<ByteBuf,CreativeFamily> CODEC=StreamCodec.composite(Identifier.STREAM_CODEC,CreativeFamily::family,ByteBufCodecs.BOOL,CreativeFamily::mechamaru,CreativeFamily::new);
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record Tune(Identifier id,TechniqueTuning tuning,int request) implements CustomPacketPayload {
        public Tune(Identifier id,TechniqueTuning tuning){this(id,tuning,0);}
        public static final Type<Tune> TYPE=new Type<>(JjkCraft.id("tune"));
        public static final StreamCodec<ByteBuf,Tune> CODEC=StreamCodec.composite(Identifier.STREAM_CODEC,Tune::id,
            ByteBufCodecs.fromCodec(TechniqueTuning.CODEC),Tune::tuning,ByteBufCodecs.VAR_INT,Tune::request,Tune::new);
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record TuneReply(Identifier id,int request,int result) implements CustomPacketPayload {
        public static final Type<TuneReply> TYPE=new Type<>(JjkCraft.id("tune_reply"));
        public static final StreamCodec<ByteBuf,TuneReply> CODEC=StreamCodec.composite(Identifier.STREAM_CODEC,TuneReply::id,ByteBufCodecs.VAR_INT,TuneReply::request,ByteBufCodecs.VAR_INT,TuneReply::result,TuneReply::new);
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    /** 0 learn reinforcement, 1 learn RCT, 2 train reserves, 3 toggle reinforcement, 4 tune universal skills. */
    public record Practice(int action,int output,int healing,int threshold) implements CustomPacketPayload {
        public static final Type<Practice> TYPE=new Type<>(JjkCraft.id("practice"));
        public static final StreamCodec<ByteBuf,Practice> CODEC=StreamCodec.composite(ByteBufCodecs.VAR_INT,Practice::action,
            ByteBufCodecs.VAR_INT,Practice::output,ByteBufCodecs.VAR_INT,Practice::healing,ByteBufCodecs.VAR_INT,Practice::threshold,Practice::new);
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record View(Map<Identifier,TechniqueTuning> tunings,PracticeState practice) implements CustomPacketPayload {
        public static final Type<View> TYPE=new Type<>(JjkCraft.id("personalization"));
        public static final StreamCodec<ByteBuf,View> CODEC=StreamCodec.composite(ByteBufCodecs.fromCodec(TUNINGS),View::tunings,
            ByteBufCodecs.fromCodec(PracticeState.CODEC),View::practice,View::new);
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
}
