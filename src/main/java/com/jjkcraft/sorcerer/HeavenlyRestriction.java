package com.jjkcraft.sorcerer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import com.jjkcraft.JjkCraft;
import com.jjkcraft.grade.Grades;

/**
 * Heavenly Restriction: no cursed energy, but an enhanced body. Stats are transient attribute
 * modifiers, reapplied on join, respawn and every progress change, and they scale with grade rank
 * and Output points. "Hit recovery" is faster natural healing once the player has not been hurt
 * for three seconds.
 */
public final class HeavenlyRestriction {
	private static final Map<UUID, Long> LAST_HURT = new HashMap<>();
	private static final int RECOVERY_DELAY = 60;
	private static final int RECOVERY_INTERVAL = 20;

	private record Bonus(Holder<Attribute> attribute, String path, AttributeModifier.Operation operation, double base, double perRank, double perOutput) {
	}

	private static final Bonus[] BONUSES = {
			new Bonus(Attributes.MOVEMENT_SPEED, "speed", AttributeModifier.Operation.ADD_MULTIPLIED_BASE, 0.12, 0.012, 0.0),
			new Bonus(Attributes.ATTACK_DAMAGE, "strength", AttributeModifier.Operation.ADD_VALUE, 2.0, 0.75, 0.5),
			new Bonus(Attributes.ATTACK_SPEED, "reflexes", AttributeModifier.Operation.ADD_MULTIPLIED_BASE, 0.2, 0.02, 0.0),
			new Bonus(Attributes.ARMOR, "toughness", AttributeModifier.Operation.ADD_VALUE, 4.0, 0.5, 0.0),
			new Bonus(Attributes.ARMOR_TOUGHNESS, "density", AttributeModifier.Operation.ADD_VALUE, 2.0, 0.25, 0.0),
			new Bonus(Attributes.KNOCKBACK_RESISTANCE, "footing", AttributeModifier.Operation.ADD_VALUE, 0.25, 0.03, 0.0),
			new Bonus(Attributes.SAFE_FALL_DISTANCE, "landing", AttributeModifier.Operation.ADD_VALUE, 3.0, 0.5, 0.0),
			new Bonus(Attributes.JUMP_STRENGTH, "spring", AttributeModifier.Operation.ADD_MULTIPLIED_BASE, 0.1, 0.01, 0.0),
			new Bonus(Attributes.STEP_HEIGHT, "stride", AttributeModifier.Operation.ADD_VALUE, 0.4, 0.0, 0.0),
	};

	private HeavenlyRestriction() {
	}

	public static void init() {
		SorcererLifecycle.addFullSync(HeavenlyRestriction::refresh);
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, base, taken, blocked) -> {
			if (entity instanceof ServerPlayer player && taken > 0) {
				LAST_HURT.put(player.getUUID(), player.level().getGameTime());
			}
		});
		ServerTickEvents.END_SERVER_TICK.register(HeavenlyRestriction::tickRecovery);
	}

	public static boolean has(ServerPlayer player) {
		return Progression.get(player).heavenlyRestricted();
	}

	/** Adds, updates or removes the body bonuses to match the player's current progress. */
	public static void refresh(ServerPlayer player) {
		SorcererProgress progress = Progression.get(player);
		int rank = Grades.rank(player.registryAccess(), progress.grade());
		for (Bonus bonus : BONUSES) {
			AttributeInstance instance = player.getAttribute(bonus.attribute());
			if (instance == null) {
				continue;
			}

			Identifier id = JjkCraft.id("heavenly/" + bonus.path());
			if (progress.heavenlyRestricted()) {
				double amount = bonus.base() + bonus.perRank() * rank + bonus.perOutput() * progress.growth().output();
				instance.addOrUpdateTransientModifier(new AttributeModifier(id, amount, bonus.operation()));
			} else {
				instance.removeModifier(id);
			}
		}
	}

	private static void tickRecovery(MinecraftServer server) {
		if (server.getTickCount() % RECOVERY_INTERVAL != 0) {
			return;
		}

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (!has(player) || player.getHealth() >= player.getMaxHealth() || player.isDeadOrDying()) {
				continue;
			}

			long now = player.level().getGameTime();
			long lastHurt = LAST_HURT.getOrDefault(player.getUUID(), Long.MIN_VALUE / 2);
			if (now - lastHurt >= RECOVERY_DELAY) {
				player.heal(1.0f);
			}
		}
	}
}
