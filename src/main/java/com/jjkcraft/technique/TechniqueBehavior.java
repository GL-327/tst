package com.jjkcraft.technique;

/**
 * What a technique does. Implementations are records whose fields are the data parameters, read
 * by the codec of their {@link TechniqueBehaviorType}. Behaviors run on the server only.
 */
public interface TechniqueBehavior {
	TechniqueBehaviorType<?> type();

	/** Runs the technique (instant), or starts it (channel and toggle). */
	CastResult cast(CastContext ctx);

	/** Called every tick while a channel or toggle technique is active. Return false to end it. */
	default boolean tick(CastContext ctx, int ticksActive) {
		return true;
	}

	/** Called once when a channel or toggle technique ends. */
	default void end(CastContext ctx) {
	}
}
