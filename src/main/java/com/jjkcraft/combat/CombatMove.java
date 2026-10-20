package com.jjkcraft.combat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Frame counts are authoritative 20 TPS ticks. Height: 0 low, 1 mid, 2 high. */
public record CombatMove(int startup, int active, int recovery, double damage, double ce,
        int height, double reach, double guardDamage, int stun, double knockback, double lift) {
    public static final Codec<CombatMove> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.intRange(1, 40).fieldOf("startup").forGetter(CombatMove::startup),
        Codec.intRange(1, 10).fieldOf("active").forGetter(CombatMove::active),
        Codec.intRange(1, 60).fieldOf("recovery").forGetter(CombatMove::recovery),
        Codec.doubleRange(0, 100).fieldOf("damage").forGetter(CombatMove::damage),
        Codec.doubleRange(0, 1000).optionalFieldOf("ce", 0.0).forGetter(CombatMove::ce),
        Codec.intRange(0, 2).optionalFieldOf("height", 1).forGetter(CombatMove::height),
        Codec.doubleRange(1, 6).optionalFieldOf("reach", 3.0).forGetter(CombatMove::reach),
        Codec.doubleRange(0, 100).optionalFieldOf("guard_damage", 15.0).forGetter(CombatMove::guardDamage),
        Codec.intRange(0, 30).optionalFieldOf("stun", 4).forGetter(CombatMove::stun),
        Codec.doubleRange(0, 3).optionalFieldOf("knockback", 0.3).forGetter(CombatMove::knockback),
        Codec.doubleRange(0, 2).optionalFieldOf("lift", 0.0).forGetter(CombatMove::lift)
    ).apply(i, CombatMove::new));
    public int duration() { return startup + active + recovery; }
}
