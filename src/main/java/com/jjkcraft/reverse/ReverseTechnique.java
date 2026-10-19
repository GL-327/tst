package com.jjkcraft.reverse;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import com.jjkcraft.JjkCraft;
import com.jjkcraft.config.JjkConfig;
import com.jjkcraft.curse.CurseEntity;
import com.jjkcraft.damage.DamageModifiers;
import com.jjkcraft.damage.JjkDamageTypes;
import com.jjkcraft.energy.CursedEnergy;
import com.jjkcraft.grade.Grades;
import com.jjkcraft.network.JjkNetworking;
import com.jjkcraft.network.payload.ReversePayload;
import com.jjkcraft.network.payload.ReverseTogglePayload;
import com.jjkcraft.particle.JjkParticles;
import com.jjkcraft.sorcerer.Awakening;
import com.jjkcraft.sorcerer.Progression;
import com.jjkcraft.sorcerer.SorcererLifecycle;
import com.jjkcraft.sorcerer.SorcererProgress;
import com.jjkcraft.sound.JjkSounds;

/**
 * Reverse Cursed Technique: cursed energy multiplied against itself into positive energy. It is
 * not learned like a technique; it awakens at the edge of death. A Semi-Grade 1 or higher sorcerer
 * holding enough CE who takes a lethal blow may grasp it and survive. Once awakened:
 * <ul>
 * <li>below 60% health it heals automatically, 1 health every half second for 3 CE each;</li>
 * <li>once every five minutes it turns a lethal blow aside at the cost of 40% of max CE;</li>
 * <li>its positive energy makes techniques hit curses 25% harder;</li>
 * <li>it mends the brain after a domain: burnout is halved.</li>
 * </ul>
 */
public final class ReverseTechnique {
	public static final double HEAL_BELOW = 0.6;
	public static final double CE_PER_HEALTH = 3.0;
	public static final int SAVE_COOLDOWN = 6000;
	public static final double SAVE_COST_FRACTION = 0.4;
	public static final double CURSE_DAMAGE_BONUS = 1.25;
	public static final double BURNOUT_FACTOR = 0.5;
	/** Minimum share of max CE a sorcerer must hold for the awakening to happen. */
	public static final double AWAKEN_MIN_ENERGY = 0.35;

	public static final AttachmentType<ReverseState> STATE = AttachmentRegistry.create(JjkCraft.id("reverse"),
			builder -> builder.persistent(ReverseState.CODEC).initializer(() -> ReverseState.EMPTY));

	private static final Set<UUID> HEALING = new HashSet<>();
	/** The next eligible lethal blow awakens it for sure (commands, tests). */
	private static final Set<UUID> FORCED = new HashSet<>();
	private static final com.jjkcraft.core.RateLimiter TOGGLE_LIMIT = new com.jjkcraft.core.RateLimiter(4, 4);

	private ReverseTechnique() {
	}

	public static void init() {
		ServerLivingEntityEvents.ALLOW_DEATH.register(ReverseTechnique::allowDeath);
		ServerTickEvents.END_SERVER_TICK.register(ReverseTechnique::tick);
		ServerPlayNetworking.registerGlobalReceiver(ReverseTogglePayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			if (unlocked(player) && TOGGLE_LIMIT.tryAcquire(player.getUUID(), player.level().getGameTime())) {
				set(player, state(player).withAutoHeal(!state(player).autoHeal()), false);
			}
		});
		DamageModifiers.add(ReverseTechnique::positiveEnergy);
		SorcererLifecycle.addFullSync(player -> send(player, false));
		SorcererLifecycle.addDeathCopier((oldPlayer, newPlayer, policy, config) -> newPlayer.setAttached(STATE, state(oldPlayer)));
		Awakening.addResetHook(player -> set(player, ReverseState.EMPTY, false));
	}

	public static ReverseState state(Player player) {
		return player.getAttachedOrCreate(STATE);
	}

	public static boolean unlocked(LivingEntity entity) {
		return entity instanceof Player player && state(player).unlocked();
	}

	public static boolean healing(LivingEntity entity) {
		return HEALING.contains(entity.getUUID());
	}

	public static void forceAwakening(ServerPlayer player) {
		FORCED.add(player.getUUID());
	}

	public static void set(ServerPlayer player, ReverseState state, boolean awakenedNow) {
		player.setAttached(STATE, state);
		send(player, awakenedNow);
	}

	private static void send(ServerPlayer player, boolean awakenedNow) {
		ReverseState state = state(player);
		JjkNetworking.send(player, new ReversePayload(state.unlocked(), state.autoHeal(), HEALING.contains(player.getUUID()), awakenedNow));
	}

	/** Whether this player could awaken the technique on a lethal blow right now. */
	public static boolean canAwaken(ServerPlayer player) {
		SorcererProgress progress = Progression.get(player);
		return !state(player).unlocked() && progress.awakened() && !progress.heavenlyRestricted()
				&& Grades.rank(player.registryAccess(), progress.grade()) >= JjkConfig.server().casting.reverseUnlockMinRank
				&& CursedEnergy.current(player) >= CursedEnergy.max(player) * AWAKEN_MIN_ENERGY;
	}

	private static boolean allowDeath(LivingEntity entity, DamageSource source, float amount) {
		if (!(entity instanceof ServerPlayer player) || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			return true;
		}

		return !saveFromDeath(player);
	}

	/**
	 * A lethal blow is landing on {@code player}: returns true if Reverse Cursed Technique turns it
	 * aside (an emergency save, or the awakening itself). Public for tests.
	 */
	public static boolean saveFromDeath(ServerPlayer player) {
		long now = player.level().getGameTime();
		ReverseState state = state(player);
		if (state.unlocked()) {
			// The emergency save: positive energy forced through the wound.
			double cost = CursedEnergy.max(player) * SAVE_COST_FRACTION;
			if (now - state.lastSaveTick() >= SAVE_COOLDOWN && CursedEnergy.current(player) >= cost && !CursedEnergy.isOverexerted(player)) {
				CursedEnergy.drain(player, cost);
				player.setHealth(4f);
				set(player, state.withLastSave(now), false);
				glow(player, 40);
				player.level().playSound(null, player.getX(), player.getY(), player.getZ(), JjkSounds.HEAL, SoundSource.PLAYERS, 1.5f, 0.7f);
				player.sendSystemMessage(Component.translatable("message.jjkcraft.reverse_save"), true);
				return true;
			}

			return false;
		}

		boolean forced = FORCED.remove(player.getUUID());
		if (!canAwaken(player) || (!forced && player.getRandom().nextDouble() * 100 >= JjkConfig.server().casting.reverseUnlockChancePercent)) {
			return false;
		}

		// The awakening: survive at a sliver, the pool half spent on the first reversal.
		CursedEnergy.drain(player, CursedEnergy.max(player) * 0.3);
		player.setHealth(1f);
		set(player, state.withUnlocked(true).withLastSave(now), true);
		glow(player, 80);
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(), JjkSounds.PROMOTE, SoundSource.PLAYERS, 1.5f, 1.4f);
		player.sendSystemMessage(Component.translatable("message.jjkcraft.reverse_awakened"));
		return true;
	}

	private static void tick(MinecraftServer server) {
		if (server.getTickCount() % 10 != 0) {
			return;
		}

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			healStep(player);
		}
	}

	/** One healing beat (every half second): mends a point of health for CE when it should. Public for tests. */
	public static void healStep(ServerPlayer player) {
		{
			ReverseState state = state(player);
			boolean wasHealing = HEALING.contains(player.getUUID());
			boolean heal = state.unlocked() && state.autoHeal() && player.isAlive()
					&& player.getHealth() < player.getMaxHealth() * HEAL_BELOW
					&& !CursedEnergy.isOverexerted(player)
					&& CursedEnergy.current(player) >= CursedEnergy.max(player) * 0.08 + CE_PER_HEALTH
					&& CursedEnergy.trySpend(player, CE_PER_HEALTH);
			if (heal) {
				player.heal(1f);
				HEALING.add(player.getUUID());
				glow(player, 3);
			} else {
				HEALING.remove(player.getUUID());
			}

			if (heal != wasHealing) {
				send(player, false);
			}
		}
	}

	private static void glow(ServerPlayer player, int count) {
		player.level().sendParticles(JjkParticles.of(JjkParticles.REVERSE_GLOW, 0x5BE38C, 1f), player.getX(), player.getY() + 1, player.getZ(),
				count, 0.4, 0.6, 0.4, 0.02);
	}

	/** Positive energy against curses: techniques and Black Flashes from a Reverse user hit curses harder. */
	private static float positiveEnergy(net.minecraft.server.level.ServerLevel level, LivingEntity target, DamageSource source, float amount) {
		if (target instanceof CurseEntity && unlocked(source.getEntity() instanceof LivingEntity l ? l : null)
				&& (source.is(JjkDamageTypes.TECHNIQUE) || source.is(JjkDamageTypes.TECHNIQUE_PIERCING) || source.is(JjkDamageTypes.BLACK_FLASH))) {
			return amount * (float) CURSE_DAMAGE_BONUS;
		}

		return amount;
	}

	/** Burnout after a domain, shortened for Reverse users. */
	public static int burnout(LivingEntity caster, int ticks) {
		return unlocked(caster) ? (int) Math.round(ticks * BURNOUT_FACTOR) : ticks;
	}
}
