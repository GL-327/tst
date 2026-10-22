package com.jjkcraft.network.payload;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import com.jjkcraft.JjkCraft;
public record SpatialShotPayload(int caster,Identifier technique,Vec3 from,Vec3 to,float radius,int ticks,int color) implements CustomPacketPayload {
    public static final Type<SpatialShotPayload> TYPE=new Type<>(JjkCraft.id("spatial_shot"));
    public static final StreamCodec<ByteBuf,SpatialShotPayload> CODEC=StreamCodec.composite(ByteBufCodecs.VAR_INT,SpatialShotPayload::caster,Identifier.STREAM_CODEC,SpatialShotPayload::technique,Vec3.STREAM_CODEC,SpatialShotPayload::from,Vec3.STREAM_CODEC,SpatialShotPayload::to,ByteBufCodecs.FLOAT,SpatialShotPayload::radius,ByteBufCodecs.VAR_INT,SpatialShotPayload::ticks,ByteBufCodecs.VAR_INT,SpatialShotPayload::color,SpatialShotPayload::new);
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
