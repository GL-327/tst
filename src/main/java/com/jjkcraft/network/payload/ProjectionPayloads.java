package com.jjkcraft.network.payload;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import com.jjkcraft.JjkCraft;
import com.jjkcraft.technique.ProjectionPlan;
public final class ProjectionPayloads {
    private ProjectionPayloads(){}
    public record Edit(ProjectionPlan plan,boolean read,int request) implements CustomPacketPayload {
        public static final Type<Edit> TYPE=new Type<>(JjkCraft.id("projection_edit"));
        public static final StreamCodec<ByteBuf,Edit> CODEC=StreamCodec.composite(ByteBufCodecs.fromCodec(ProjectionPlan.CODEC),Edit::plan,ByteBufCodecs.BOOL,Edit::read,ByteBufCodecs.VAR_INT,Edit::request,Edit::new);
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record Reply(ProjectionPlan plan,boolean accepted,int request,int result) implements CustomPacketPayload {
        public Reply(ProjectionPlan plan,boolean accepted,int request){this(plan,accepted,request,accepted?0:5);}
        public static final Type<Reply> TYPE=new Type<>(JjkCraft.id("projection_reply"));
        public static final StreamCodec<ByteBuf,Reply> CODEC=StreamCodec.composite(ByteBufCodecs.fromCodec(ProjectionPlan.CODEC),Reply::plan,ByteBufCodecs.BOOL,Reply::accepted,ByteBufCodecs.VAR_INT,Reply::request,ByteBufCodecs.VAR_INT,Reply::result,Reply::new);
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
}
