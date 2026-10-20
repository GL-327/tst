package com.jjkcraft.awakening;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Persistent character identity, separate from the legacy grade attachment. */
public record AwakeningRecord(String path, String family, String clan, boolean sixEyes, long tick) {
    public static final AwakeningRecord EMPTY = new AwakeningRecord("", "", "", false, -1);
    public static final Codec<AwakeningRecord> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.STRING.optionalFieldOf("path", "").forGetter(AwakeningRecord::path),
        Codec.STRING.optionalFieldOf("family", "").forGetter(AwakeningRecord::family),
        Codec.STRING.optionalFieldOf("clan", "").forGetter(AwakeningRecord::clan),
        Codec.BOOL.optionalFieldOf("six_eyes", false).forGetter(AwakeningRecord::sixEyes),
        Codec.LONG.optionalFieldOf("tick", -1L).forGetter(AwakeningRecord::tick)
    ).apply(i, AwakeningRecord::new));
}
