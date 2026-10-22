package com.jjkcraft.vessel;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
public record VesselState(int fingers,int pressure,long safeUntil){
    public static final VesselState EMPTY=new VesselState(0,0,0);
    public static final Codec<VesselState> CODEC=RecordCodecBuilder.create(i->i.group(Codec.intRange(0,20).optionalFieldOf("fingers",0).forGetter(VesselState::fingers),Codec.intRange(0,100).optionalFieldOf("pressure",0).forGetter(VesselState::pressure),Codec.LONG.optionalFieldOf("safe_until",0L).forGetter(VesselState::safeUntil)).apply(i,VesselState::new));
}
