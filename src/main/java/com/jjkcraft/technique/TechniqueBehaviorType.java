package com.jjkcraft.technique;

import com.mojang.serialization.MapCodec;

/** A kind of behavior, registered in code; data picks it by id and supplies its parameters. */
public record TechniqueBehaviorType<B extends TechniqueBehavior>(MapCodec<B> codec) {
}
