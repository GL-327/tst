package com.jjkcraft.domain.behavior;

import com.mojang.serialization.MapCodec;

/** A registered kind of domain behavior and the codec for its data. */
public record DomainBehaviorType<B extends DomainBehavior>(MapCodec<B> codec) {
}
