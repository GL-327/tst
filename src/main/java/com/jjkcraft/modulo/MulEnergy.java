package com.jjkcraft.modulo;

import com.mojang.serialization.Codec;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

import com.jjkcraft.JjkCraft;
import com.jjkcraft.config.JjkConfig;
import com.jjkcraft.core.modulo.ModuloMath;
import com.jjkcraft.energy.CursedEnergy;
import com.jjkcraft.sorcerer.Progression;

/**
 * Mul energy: cursed energy run through Mul, the Simurian ore (docs/MODULO.md 3.4). A second pool
 * beside cursed energy, used by Simurian gear and by techniques with a {@code mul_cost}. Saved,
 * kept through death, and sent to its owner with the Modulo view.
 */
public final class MulEnergy {
	public static final AttachmentType<Double> POOL = AttachmentRegistry.create(JjkCraft.id("mul_energy"),
			builder -> builder.persistent(Codec.doubleRange(0, 1_000_000)).copyOnDeath().initializer(() -> 0.0));

	private MulEnergy() {
	}

	public static void init() {
		// Loads the class so the attachment registers.
	}

	public static double get(Player player) {
		return player.getAttachedOrElse(POOL, 0.0);
	}

	public static double max() {
		return Math.max(1, JjkConfig.server().modulo.maxMulEnergy);
	}

	public static void set(ServerPlayer player, double value) {
		player.setAttached(POOL, Math.max(0, Math.min(max(), value)));
		Modulo.markDirty(player);
	}

	public static void add(ServerPlayer player, double amount) {
		set(player, get(player) + amount);
	}

	/** Spends {@code amount} if the player has it. Creative players always can. */
	public static boolean trySpend(ServerPlayer player, double amount) {
		if (amount <= 0 || player.isCreative()) {
			return true;
		}

		double have = get(player);
		if (have + 1e-9 < amount) {
			return false;
		}

		set(player, have - amount);
		return true;
	}

	/**
	 * Pours up to {@code rate} cursed energy into a piece of Mul that still holds {@code pieceLeft}.
	 * Returns the cursed energy poured (0 if nothing happened). Heavenly Restriction has nothing to pour.
	 */
	public static double pour(ServerPlayer player, double rate, double pieceLeft) {
		if (Progression.get(player).heavenlyRestricted()) {
			return 0;
		}

		double ratio = JjkConfig.server().modulo.mulEnergyRatio;
		double poured = ModuloMath.pourAmount(rate, pieceLeft, CursedEnergy.current(player), get(player), max(), ratio);
		if (poured <= 0 || CursedEnergy.drain(player, poured) <= 0) {
			return 0;
		}

		add(player, ModuloMath.mulFromCursedEnergy(poured, ratio));
		return poured;
	}
}
