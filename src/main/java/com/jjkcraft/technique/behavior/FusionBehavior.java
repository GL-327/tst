package com.jjkcraft.technique.behavior;

import java.util.List;
import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import com.jjkcraft.technique.*;

/** Fixed recipes or the player's chosen two-to-four component chimera. */
public record FusionBehavior(List<Identifier> components,Identifier result,int lifetime) implements TechniqueBehavior {
    public static final MapCodec<FusionBehavior> CODEC=RecordCodecBuilder.mapCodec(i->i.group(
        Identifier.CODEC.listOf(0,4).optionalFieldOf("components",List.of()).forGetter(FusionBehavior::components),
        Identifier.CODEC.fieldOf("result").forGetter(FusionBehavior::result),Codec.intRange(20,12000).optionalFieldOf("lifetime",600).forGetter(FusionBehavior::lifetime)
    ).apply(i,FusionBehavior::new));
    @Override public TechniqueBehaviorType<?> type(){return TechniqueBehaviors.FUSION;}
    @Override public CastResult cast(CastContext ctx){return com.jjkcraft.entity.shikigami.ShikigamiFusion.cast(ctx,components,result,lifetime);}
}
