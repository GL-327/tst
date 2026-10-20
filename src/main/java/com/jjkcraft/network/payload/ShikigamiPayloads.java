package com.jjkcraft.network.payload;
import java.util.List;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import com.jjkcraft.JjkCraft;
public final class ShikigamiPayloads {
    private ShikigamiPayloads(){}
    public record Recipe(List<Identifier> components,int request) implements CustomPacketPayload {
        public static final Type<Recipe> TYPE=new Type<>(JjkCraft.id("fusion_recipe"));
        public static final StreamCodec<ByteBuf,Recipe> CODEC=StreamCodec.composite(ByteBufCodecs.fromCodec(Identifier.CODEC.listOf(0,4)),Recipe::components,ByteBufCodecs.VAR_INT,Recipe::request,Recipe::new);
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record Reply(List<Identifier> components,int request,boolean accepted) implements CustomPacketPayload {
        public static final Type<Reply> TYPE=new Type<>(JjkCraft.id("fusion_reply"));
        public static final StreamCodec<ByteBuf,Reply> CODEC=StreamCodec.composite(ByteBufCodecs.fromCodec(Identifier.CODEC.listOf(0,4)),Reply::components,ByteBufCodecs.VAR_INT,Reply::request,ByteBufCodecs.BOOL,Reply::accepted,Reply::new);
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
}
