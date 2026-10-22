package com.jjkcraft.network.payload;
import io.netty.buffer.ByteBuf;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import com.jjkcraft.JjkCraft;
/** Observers receive authoritative frames instead of a vanilla arm swing. */
public record CombatPosePayload(int entity,String move,long started,int startup,int active,int recovery,int guard,int stun,int hand) implements CustomPacketPayload {
    public CombatPosePayload(int entity,String move,long started,int startup,int active,int recovery,int guard,int stun){this(entity,move,started,startup,active,recovery,guard,stun,0);}
    private static final Codec<CombatPosePayload> DATA=RecordCodecBuilder.create(i->i.group(
        Codec.INT.fieldOf("entity").forGetter(CombatPosePayload::entity),Codec.STRING.fieldOf("move").forGetter(CombatPosePayload::move),
        Codec.LONG.fieldOf("started").forGetter(CombatPosePayload::started),Codec.INT.fieldOf("startup").forGetter(CombatPosePayload::startup),
        Codec.INT.fieldOf("active").forGetter(CombatPosePayload::active),Codec.INT.fieldOf("recovery").forGetter(CombatPosePayload::recovery),
        Codec.INT.fieldOf("guard").forGetter(CombatPosePayload::guard),Codec.INT.fieldOf("stun").forGetter(CombatPosePayload::stun),Codec.intRange(-1,1).optionalFieldOf("hand",0).forGetter(CombatPosePayload::hand)
    ).apply(i,CombatPosePayload::new));
    public static final Type<CombatPosePayload> TYPE=new Type<>(JjkCraft.id("combat_pose"));
    public static final StreamCodec<ByteBuf,CombatPosePayload> CODEC=ByteBufCodecs.fromCodec(DATA);
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
