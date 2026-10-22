package com.jjkcraft.network.payload;

import java.util.List;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import com.jjkcraft.JjkCraft;

public final class TechniqueMenuPayloads {
    private TechniqueMenuPayloads() { }
    /** action: 0 refresh, 1 begin subjugation, 2 dismiss owned summons. */
    public record Action(Identifier family,Identifier move,int action) implements CustomPacketPayload {
        public static final Type<Action> TYPE=new Type<>(JjkCraft.id("technique_menu_action"));
        public static final StreamCodec<ByteBuf,Action> CODEC=StreamCodec.composite(Identifier.STREAM_CODEC,Action::family,Identifier.STREAM_CODEC,Action::move,ByteBufCodecs.VAR_INT,Action::action,Action::new);
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record View(Identifier family,List<Identifier> tamed,String trial,boolean open,String feedback) implements CustomPacketPayload {
        public static final Type<View> TYPE=new Type<>(JjkCraft.id("technique_menu_view"));
        public static final StreamCodec<ByteBuf,View> CODEC=StreamCodec.composite(Identifier.STREAM_CODEC,View::family,
            Identifier.STREAM_CODEC.apply(ByteBufCodecs.list(16)),View::tamed,ByteBufCodecs.STRING_UTF8,View::trial,
            ByteBufCodecs.BOOL,View::open,ByteBufCodecs.STRING_UTF8,View::feedback,View::new);
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
}
