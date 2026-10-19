package com.jjkcraft.technique;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import net.minecraft.world.entity.LivingEntity;

/**
 * Lets other systems take over a strike. Black Flash (M6) registers a resolver that may turn a
 * strike into a Black Flash and deal the damage itself.
 */
public final class StrikeHooks {
	/**
	 * @param handled true if the resolver already dealt damage and effects
	 * @param damage  damage to deal when not handled
	 */
	public record Outcome(boolean handled, double damage) {
	}

	@FunctionalInterface
	public interface Resolver {
		/** Returns null to leave the strike alone. */
		Outcome resolve(CastContext ctx, LivingEntity target, double damage, boolean blackFlashEligible);
	}

	private static final List<Resolver> RESOLVERS = new CopyOnWriteArrayList<>();

	private StrikeHooks() {
	}

	public static void add(Resolver resolver) {
		RESOLVERS.add(resolver);
	}

	public static Outcome resolve(CastContext ctx, LivingEntity target, double damage, boolean blackFlashEligible) {
		for (Resolver resolver : RESOLVERS) {
			Outcome outcome = resolver.resolve(ctx, target, damage, blackFlashEligible);
			if (outcome != null) {
				return outcome;
			}
		}

		return new Outcome(false, damage);
	}
}
