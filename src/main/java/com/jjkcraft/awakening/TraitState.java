package com.jjkcraft.awakening;

import java.util.List;
import net.minecraft.resources.Identifier;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record TraitState(boolean rolled, List<Identifier> traits, String form, List<Identifier> hostTechniques, double hostHealth) {
    public TraitState(boolean rolled,List<Identifier> traits,String form,List<Identifier> hostTechniques) { this(rolled,traits,form,hostTechniques,20); }
    public static final TraitState EMPTY = new TraitState(false,List.of(),"",List.of());
    public static final Codec<TraitState> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.BOOL.optionalFieldOf("rolled",false).forGetter(TraitState::rolled),
        Identifier.CODEC.listOf().optionalFieldOf("traits",List.of()).forGetter(TraitState::traits),
        Codec.STRING.optionalFieldOf("form","").forGetter(TraitState::form),
        Identifier.CODEC.listOf().optionalFieldOf("host_techniques",List.of()).forGetter(TraitState::hostTechniques),
        Codec.doubleRange(4,100).optionalFieldOf("host_health",20.0).forGetter(TraitState::hostHealth)
    ).apply(i,TraitState::new));
    public TraitState withForm(String next, List<Identifier> host) { return withForm(next,host,20); }
    public TraitState withForm(String next,List<Identifier> host,double health) { return new TraitState(rolled,traits,next,List.copyOf(host),Math.clamp(health,4,100)); }
}
