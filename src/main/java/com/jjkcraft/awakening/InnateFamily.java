package com.jjkcraft.awakening;

import java.util.List;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

/** A complete innate family: the first move is usable at awakening, later moves unlock by grade. */
public record InnateFamily(String name, String clan, int inheritedWeight, int randomWeight,
        List<Identifier> techniques, String domain) {
    public static final Codec<InnateFamily> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.STRING.fieldOf("name").forGetter(InnateFamily::name),
        Codec.STRING.optionalFieldOf("clan", "").forGetter(InnateFamily::clan),
        Codec.intRange(0, 10000).optionalFieldOf("inherited_weight", 0).forGetter(InnateFamily::inheritedWeight),
        Codec.intRange(0, 10000).optionalFieldOf("random_weight", 0).forGetter(InnateFamily::randomWeight),
        Identifier.CODEC.listOf(1, 32).fieldOf("techniques").forGetter(InnateFamily::techniques),
        Codec.STRING.optionalFieldOf("domain", "").forGetter(InnateFamily::domain)
    ).apply(i, InnateFamily::new));
}
