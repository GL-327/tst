package com.jjkcraft.death;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import java.util.List;

/** Both the cost and approved Binding Vows are controlled by server data. */
public record PactDefinition(String name, int price, Identifier offering, int offeringCount, int fingers, List<Identifier> approvedVows, boolean offeringRequired) {
    public static final Codec<PactDefinition> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.STRING.fieldOf("name").forGetter(PactDefinition::name), Codec.intRange(0, 10000).fieldOf("price").forGetter(PactDefinition::price),
        Identifier.CODEC.fieldOf("offering").forGetter(PactDefinition::offering), Codec.intRange(1, 64).fieldOf("offering_count").forGetter(PactDefinition::offeringCount),
        Codec.intRange(1, 20).optionalFieldOf("fingers", 1).forGetter(PactDefinition::fingers),
        Identifier.CODEC.listOf().optionalFieldOf("approved_vows", List.of()).forGetter(PactDefinition::approvedVows),
        Codec.BOOL.optionalFieldOf("offering_required", false).forGetter(PactDefinition::offeringRequired)
    ).apply(i, PactDefinition::new));
}
