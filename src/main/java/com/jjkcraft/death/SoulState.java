package com.jjkcraft.death;

import java.util.List;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Phase: alive, choosing, respawning, reincarnating, becoming_object, object, permanent. Persisted through relogs. */
public record SoulState(String phase, String pact, List<String> tokens, long expires, String dimension,
        double x, double y, double z, String mode, int possession, boolean warned, boolean vessel) {
    public static final SoulState EMPTY = new SoulState("alive", "", List.of(), 0, "minecraft:overworld", 0, 80, 0, "survival", 0, false, false);
    public static final Codec<SoulState> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.STRING.optionalFieldOf("phase", "alive").forGetter(SoulState::phase),
        Codec.STRING.optionalFieldOf("pact", "").forGetter(SoulState::pact),
        Codec.STRING.listOf(0, 20).optionalFieldOf("tokens", List.of()).forGetter(SoulState::tokens),
        Codec.LONG.optionalFieldOf("expires", 0L).forGetter(SoulState::expires),
        Codec.STRING.optionalFieldOf("dimension", "minecraft:overworld").forGetter(SoulState::dimension),
        Codec.DOUBLE.optionalFieldOf("x", 0.0).forGetter(SoulState::x), Codec.DOUBLE.optionalFieldOf("y", 80.0).forGetter(SoulState::y),
        Codec.DOUBLE.optionalFieldOf("z", 0.0).forGetter(SoulState::z), Codec.STRING.optionalFieldOf("mode", "survival").forGetter(SoulState::mode),
        Codec.INT.optionalFieldOf("possession", 0).forGetter(SoulState::possession), Codec.BOOL.optionalFieldOf("warned", false).forGetter(SoulState::warned),
        Codec.BOOL.optionalFieldOf("vessel", false).forGetter(SoulState::vessel)
    ).apply(i, SoulState::new));
    public SoulState phase(String next) { return new SoulState(next, pact, tokens, expires, dimension, x, y, z, mode, possession, warned, vessel); }
    public SoulState pact(String next) { return new SoulState(phase, next, tokens, expires, dimension, x, y, z, mode, possession, warned, vessel); }
    public SoulState alive(int nextPossession) { return new SoulState("alive", pact, List.of(), 0, dimension, x, y, z, mode, nextPossession, warned, vessel); }
    public SoulState withVessel() { return new SoulState(phase, pact, tokens, expires, dimension, x, y, z, mode, possession, warned, true); }
}
