package com.jjkcraft.blackflash;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

import com.jjkcraft.config.JjkConfig;
import com.jjkcraft.core.blackflash.BlackFlashMath;
import com.jjkcraft.core.progress.ProgressionMath;
import com.jjkcraft.damage.JjkDamageTypes;
import com.jjkcraft.energy.CursedEnergy;
import com.jjkcraft.grade.GradeDefinition;
import com.jjkcraft.grade.Grades;
import com.jjkcraft.network.JjkNetworking;
import com.jjkcraft.network.payload.BlackFlashPayload;
import com.jjkcraft.particle.JjkParticles;
import com.jjkcraft.sorcerer.Progression;
import com.jjkcraft.sound.JjkSounds;
import com.jjkcraft.technique.CastContext;
import com.jjkcraft.technique.StrikeHooks;
import com.jjkcraft.technique.TechniqueService;

/**
 * Black Flash: a Black Flash-eligible strike (the reinforced palm) may land with cursed energy
 * inside a hair of the blow. It hits for {@code ce_ratio ^ exponent} times the damage, ignores
 * shields, refunds some CE and puts the striker "in the zone", where the next one comes easier.
 * The server rolls it; clients get a frame flash, lightning and a toast.
 */
public final class BlackFlash {
	/** Counter of Black Flashes landed (profile, trials). */
	public static final String COUNTER = "black_flashes";

	private record Zone(int stacks, long until) {
	}

	private static final Map<UUID, Zone> ZONES = new HashMap<>();
	/** Entities whose next eligible strike is a guaranteed Black Flash (commands, tests). */
	private static final Set<UUID> FORCED = new HashSet<>();

	private BlackFlash() {
	}

	public static void init() {
		StrikeHooks.add(BlackFlash::resolve);
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> ZONES.remove(handler.getPlayer().getUUID()));
	}

	public static void forceNext(LivingEntity entity) {
		FORCED.add(entity.getUUID());
	}

	public static int zoneStacks(LivingEntity entity) {
		Zone zone = ZONES.get(entity.getUUID());
		return zone == null || zone.until() < entity.level().getGameTime() ? 0 : zone.stacks();
	}

	/** Chance in percent for this entity's next eligible strike. */
	public static double chance(LivingEntity striker) {
		double base = JjkConfig.server().casting.blackFlashBaseChancePercent;
		double gradeBonus = Progression.gradeOf(striker).flatMap(id -> Grades.get(striker.registryAccess(), id))
				.map(GradeDefinition::perks).map(GradeDefinition.Perks::blackFlashBonus).orElse(0.0);
		double control = striker instanceof Player player ? ProgressionMath.blackFlashBonus(Progression.get(player).growth().control()) : 0;
		double swing = striker instanceof Player player ? player.getAttackStrengthScale(0.5f) : 1.0;
		return BlackFlashMath.chance(base, gradeBonus, control, zoneStacks(striker), swing);
	}

	private static StrikeHooks.Outcome resolve(CastContext ctx, LivingEntity target, double damage, boolean eligible) {
		if (!eligible) {
			return null;
		}

		LivingEntity striker = ctx.caster();
		boolean forced = FORCED.remove(striker.getUUID());
		if (!forced && striker.getRandom().nextDouble() * 100 >= chance(striker)) {
			return null;
		}

		ServerLevel level = ctx.level();
		double multiplier = BlackFlashMath.multiplier(BlackFlashMath.ceRatio(CursedEnergy.current(striker), CursedEnergy.max(striker)),
				JjkConfig.server().casting.blackFlashExponent);
		double amount = damage * multiplier;
		target.damageCooldownTime = 0;
		target.hurtServer(level, level.damageSources().source(JjkDamageTypes.BLACK_FLASH, striker), (float) amount);
		ctx.knockback(target, striker.position(), 1.4);

		CursedEnergy.restore(striker, CursedEnergy.max(striker) * BlackFlashMath.REFUND_FRACTION);
		long now = level.getGameTime();
		int stacks = Math.min(BlackFlashMath.MAX_ZONE_STACKS, zoneStacks(striker) + 1);
		ZONES.put(striker.getUUID(), new Zone(stacks, now + BlackFlashMath.ZONE_TICKS));

		if (striker instanceof ServerPlayer player) {
			Progression.addCounter(player, COUNTER, 1);
			TechniqueService.creditHit(player, ctx.techniqueId());
		}

		Vec3 at = target.position().add(0, target.getBbHeight() * 0.6, 0);
		level.sendParticles(JjkParticles.of(JjkParticles.BLACK_LIGHTNING, 0xE0303F, 1.5f), at.x, at.y, at.z, 24, 0.5, 0.6, 0.5, 0.2);
		level.sendParticles(JjkParticles.of(JjkParticles.BLACK_LIGHTNING, 0x0B0B10, 1.8f), at.x, at.y, at.z, 18, 0.7, 0.7, 0.7, 0.25);
		level.playSound(null, at.x, at.y, at.z, JjkSounds.BLACK_FLASH, SoundSource.PLAYERS, 2.0f, 1.0f);

		BlackFlashPayload payload = new BlackFlashPayload(striker.getId(), target.getId(), (float) multiplier, now + BlackFlashMath.ZONE_TICKS, stacks);
		for (ServerPlayer watcher : level.players()) {
			if (watcher.distanceToSqr(at) < 64 * 64) {
				JjkNetworking.send(watcher, payload);
			}
		}

		if (striker instanceof ServerPlayer player && stacks >= BlackFlashMath.MAX_ZONE_STACKS) {
			player.sendSystemMessage(Component.translatable("message.jjkcraft.in_the_zone"), true);
		}

		return new StrikeHooks.Outcome(true, amount);
	}
}
