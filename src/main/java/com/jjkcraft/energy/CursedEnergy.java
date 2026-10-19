package com.jjkcraft.energy;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import com.jjkcraft.attachment.JjkAttachments;
import com.jjkcraft.config.JjkConfig;
import com.jjkcraft.core.energy.EnergyMath;
import com.jjkcraft.core.energy.EnergyParams;
import com.jjkcraft.core.energy.EnergyState;
import com.jjkcraft.grade.GradeDefinition;
import com.jjkcraft.grade.Grades;
import com.jjkcraft.sorcerer.SorcererProgress;
import com.jjkcraft.sound.JjkSounds;

/**
 * Server-side cursed energy API. Every change goes through here so that storage, overexertion
 * effects and client sync stay consistent. Values are stored lazily (see {@link EnergyMath#advance}).
 */
public final class CursedEnergy {
	/** Multiplies an entity's regen. Domains, vows and effects register one of these. */
	@FunctionalInterface
	public interface RegenModifier {
		double modify(LivingEntity entity);
	}

	/** Adds to an entity's max CE. Vows and cursed tools register one of these. */
	@FunctionalInterface
	public interface CapacityModifier {
		double modify(LivingEntity entity, double capacity);
	}

	private static final List<RegenModifier> REGEN_MODIFIERS = new CopyOnWriteArrayList<>();
	private static final List<CapacityModifier> CAPACITY_MODIFIERS = new CopyOnWriteArrayList<>();

	private CursedEnergy() {
	}

	public static void addRegenModifier(RegenModifier modifier) {
		REGEN_MODIFIERS.add(modifier);
	}

	public static void addCapacityModifier(CapacityModifier modifier) {
		CAPACITY_MODIFIERS.add(modifier);
	}

	public static EnergyParams params() {
		return JjkConfig.energyParams();
	}

	/** True if the entity has a CE pool at all (players always do, unless Heavenly Restricted). */
	public static boolean isCapable(LivingEntity entity) {
		return max(entity) > 0;
	}

	public static double max(LivingEntity entity) {
		double capacity;
		if (entity instanceof Player player) {
			SorcererProgress progress = progress(player);
			double gradeMultiplier = Grades.get(entity.registryAccess(), progress.grade())
					.map(GradeDefinition::ceMultiplier).orElse(1.0);
			capacity = EnergyMath.sorcererCapacity(JjkConfig.server().energy.baseCapacity, gradeMultiplier,
					progress.growth().output(), progress.heavenlyRestricted());
		} else if (entity instanceof CursedEnergyUser user) {
			capacity = user.maxCursedEnergy();
		} else {
			capacity = profile(entity).map(EnergyProfile::capacity).orElse(0.0);
		}

		if (capacity <= 0) {
			return 0;
		}

		for (CapacityModifier modifier : CAPACITY_MODIFIERS) {
			capacity = modifier.modify(entity, capacity);
		}

		return Math.max(0, capacity);
	}

	public static double regenModifier(LivingEntity entity) {
		double modifier;
		if (entity instanceof Player player) {
			SorcererProgress progress = progress(player);
			double perk = Grades.get(entity.registryAccess(), progress.grade())
					.map(g -> g.perks().regenBonus()).orElse(0.0);
			modifier = 1.0 + perk + 0.02 * progress.growth().efficiency();
		} else if (entity instanceof CursedEnergyUser user) {
			modifier = user.cursedEnergyRegenMultiplier();
		} else {
			modifier = profile(entity).map(EnergyProfile::regenMultiplier).orElse(1.0);
		}

		for (RegenModifier hook : REGEN_MODIFIERS) {
			modifier *= hook.modify(entity);
		}

		return Math.max(0, modifier);
	}

	/** The stored state advanced to the current game time. Does not store it. */
	public static EnergyState state(LivingEntity entity) {
		double max = max(entity);
		EnergyState stored = stored(entity, max);
		return EnergyMath.advance(stored, max, now(entity), params(), regenModifier(entity));
	}

	public static double current(LivingEntity entity) {
		return state(entity).current();
	}

	/** Spends CE. Returns false (and changes nothing) if the entity does not have enough. */
	public static boolean trySpend(LivingEntity entity, double cost) {
		double max = max(entity);
		if (max <= 0) {
			return cost <= 0;
		}

		EnergyMath.SpendResult result = EnergyMath.spend(stored(entity, max), max, cost, now(entity), params(), regenModifier(entity));
		if (!result.success()) {
			return false;
		}

		store(entity, result.state());
		if (result.triggeredOverexertion()) {
			onOverexerted(entity);
		}

		return true;
	}

	/** Removes up to {@code amount} CE and returns how much was actually removed. */
	public static double drain(LivingEntity entity, double amount) {
		double max = max(entity);
		if (max <= 0 || amount <= 0) {
			return 0;
		}

		EnergyState before = state(entity);
		EnergyState after = EnergyMath.drain(stored(entity, max), max, amount, now(entity), params(), regenModifier(entity));
		store(entity, after);
		return before.current() - after.current();
	}

	public static void restore(LivingEntity entity, double amount) {
		double max = max(entity);
		if (max <= 0) {
			return;
		}

		store(entity, EnergyMath.restore(stored(entity, max), max, amount, now(entity), params(), regenModifier(entity)));
	}

	/** Sets CE directly (commands, tests). Clears overexertion. */
	public static void set(LivingEntity entity, double value) {
		double max = max(entity);
		EnergyState current = state(entity);
		store(entity, new EnergyState(Math.max(0, Math.min(max, value)), now(entity), 0, current.lastActionTick()));
	}

	public static void fill(LivingEntity entity) {
		set(entity, max(entity));
	}

	/**
	 * Stores the current value before something that changes max CE or the regen modifier (a grade
	 * change, entering a domain), so regen up to now is counted at the old rate.
	 */
	public static void checkpoint(LivingEntity entity) {
		if (entity.hasAttached(JjkAttachments.ENERGY)) {
			store(entity, state(entity));
		}
	}

	/** Starts an overexertion window without spending (domain backlash). */
	public static void forceOverexert(LivingEntity entity, int ticks) {
		EnergyState s = state(entity);
		long now = now(entity);
		store(entity, new EnergyState(s.current(), now, Math.max(s.overexertedUntil(), now + ticks), now));
		onOverexerted(entity);
	}

	public static boolean isOverexerted(LivingEntity entity) {
		return stored(entity, max(entity)).isOverexerted(now(entity));
	}

	public static Optional<EnergyProfile> profile(LivingEntity entity) {
		if (entity.level().isClientSide()) {
			return Optional.empty();
		}

		return EnergyProfiles.forType(entity.registryAccess(), entity.getType());
	}

	private static SorcererProgress progress(Player player) {
		return player.getAttachedOrCreate(JjkAttachments.PROGRESS);
	}

	/** The stored state, or a full pool if the entity has never been touched. */
	private static EnergyState stored(LivingEntity entity, double max) {
		EnergyState state = entity.getAttached(JjkAttachments.ENERGY);
		return state != null ? state : EnergyState.full(max, now(entity));
	}

	private static void store(LivingEntity entity, EnergyState state) {
		entity.setAttached(JjkAttachments.ENERGY, state);
		if (entity instanceof ServerPlayer player) {
			EnergySync.markDirty(player);
		}
	}

	private static void onOverexerted(LivingEntity entity) {
		entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(),
				JjkSounds.OVEREXERT, entity instanceof Player ? SoundSource.PLAYERS : SoundSource.HOSTILE,
				0.8f, 0.9f + entity.getRandom().nextFloat() * 0.2f);
	}

	private static long now(LivingEntity entity) {
		return entity.level().getGameTime();
	}
}
