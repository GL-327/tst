package com.jjkcraft.death;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Immutable bearer item identity. The live owner record validates tokens, so copies cannot revive twice. */
public record SoulObject(String owner, String token, String name, String grade, long expires) {
    public static final Codec<SoulObject> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.STRING.fieldOf("owner").forGetter(SoulObject::owner), Codec.STRING.fieldOf("token").forGetter(SoulObject::token),
        Codec.STRING.fieldOf("name").forGetter(SoulObject::name), Codec.STRING.fieldOf("grade").forGetter(SoulObject::grade),
        Codec.LONG.fieldOf("expires").forGetter(SoulObject::expires)
    ).apply(i, SoulObject::new));
}
