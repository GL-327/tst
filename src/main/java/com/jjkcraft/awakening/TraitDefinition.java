package com.jjkcraft.awakening;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Innate physical/clan attributes are independent of technique slots. */
public record TraitDefinition(String name, String description, String category, String clan, int weight,
        double capacity, double regen, double speed, double health, double damage, double armor) {
    public static final Codec<TraitDefinition> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.STRING.fieldOf("name").forGetter(TraitDefinition::name),
        Codec.STRING.fieldOf("description").forGetter(TraitDefinition::description),
        Codec.STRING.optionalFieldOf("category", "physical").forGetter(TraitDefinition::category),
        Codec.STRING.optionalFieldOf("clan", "").forGetter(TraitDefinition::clan),
        Codec.intRange(0,10000).optionalFieldOf("weight",0).forGetter(TraitDefinition::weight),
        Codec.doubleRange(0,10).optionalFieldOf("capacity",1.0).forGetter(TraitDefinition::capacity),
        Codec.doubleRange(0,10).optionalFieldOf("regen",1.0).forGetter(TraitDefinition::regen),
        Codec.doubleRange(-0.8,2).optionalFieldOf("speed",0.0).forGetter(TraitDefinition::speed),
        Codec.doubleRange(-16,100).optionalFieldOf("health",0.0).forGetter(TraitDefinition::health),
        Codec.doubleRange(-5,20).optionalFieldOf("damage",0.0).forGetter(TraitDefinition::damage),
        Codec.doubleRange(-10,30).optionalFieldOf("armor",0.0).forGetter(TraitDefinition::armor)
    ).apply(i,TraitDefinition::new));
}
