package com.jjkcraft.curse.mechanic;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.phys.Vec3;

import com.jjkcraft.block.JjkBlocks;
import com.jjkcraft.curse.CurseEntity;
import com.jjkcraft.damage.JjkDamageTypes;
import com.jjkcraft.particle.JjkParticles;
import com.jjkcraft.sound.JjkSounds;

/**
 * Culling Warden (Special Grade): the Culling Game's enforcer. It cannot die while its rules hold:
 * Rule Seals (stone tablets it raises around itself) keep it alive at a sliver of health until
 * every seal is broken; it raises two more at half health. Its weak point follows the current
 * Edict, which changes on a timer (the boss bar names it): under the Edict of Steel only melee
 * truly hurts it, under Distance only projectiles, under Energy only cursed techniques; everything
 * else barely scratches it. Break the seals, then match the Edict.
 *
 * <p>Other systems can add rules that keep it alive through {@link #addRule}, and fix its Edict through
 * {@link #addEdictSource}: the Culling Game does both from the rules in force.
 *
 * MECH_A: current edict (0 Steel, 1 Distance, 2 Energy). MECH_B: seals still standing.
 */
public final class WardenMechanic implements CurseMechanic {
	public enum Edict {
		STEEL(0xC9CED6), DISTANCE(0x53E0FF), ENERGY(0x8F5CFF);

		public final int color;

		Edict(int color) {
			this.color = color;
		}

		public String key() {
			return name().toLowerCase(Locale.ROOT);
		}

		boolean matches(DamageSource source) {
			return switch (this) {
				case STEEL -> source.is(DamageTypes.PLAYER_ATTACK) || source.is(DamageTypes.MOB_ATTACK);
				case DISTANCE -> source.is(DamageTypeTags.IS_PROJECTILE);
				case ENERGY -> source.is(JjkDamageTypes.TECHNIQUE) || source.is(JjkDamageTypes.TECHNIQUE_PIERCING)
						|| source.is(JjkDamageTypes.BLACK_FLASH) || source.is(JjkDamageTypes.SURE_HIT);
			};
		}
	}

	/** An outside rule: returns true while it keeps the Warden from dying. */
	private static final List<Function<CurseEntity, Boolean>> RULES = new CopyOnWriteArrayList<>();

	public static void addRule(Function<CurseEntity, Boolean> rule) {
		RULES.add(rule);
	}

	/** Outside sources that fix the current Edict (null: no say). The first answer wins. */
	private static final List<Function<CurseEntity, Edict>> EDICT_SOURCES = new CopyOnWriteArrayList<>();

	public static void addEdictSource(Function<CurseEntity, Edict> source) {
		EDICT_SOURCES.add(source);
	}

	private static Edict fixedEdict(CurseEntity curse) {
		for (Function<CurseEntity, Edict> source : EDICT_SOURCES) {
			Edict edict = source.apply(curse);
			if (edict != null) {
				return edict;
			}
		}

		return null;
	}

	@Override
	public void onSpawn(CurseEntity curse, ServerLevel level) {
		curse.setPersistenceRequired();
		curse.setMechA(0);
		curse.setMechB(0);
	}

	public static Edict edict(CurseEntity curse) {
		return Edict.values()[Math.floorMod(curse.mechA(), Edict.values().length)];
	}

	@SuppressWarnings("unchecked")
	private static List<BlockPos> seals(CurseEntity curse) {
		return (List<BlockPos>) curse.memory.computeIfAbsent("seals", k -> new ArrayList<BlockPos>());
	}

	/** Seals still standing (broken ones are forgotten). */
	public static int sealsStanding(CurseEntity curse, ServerLevel level) {
		List<BlockPos> seals = seals(curse);
		seals.removeIf(pos -> !level.getBlockState(pos).is(JjkBlocks.RULE_SEAL));
		return seals.size();
	}

	@Override
	public void tick(CurseEntity curse, ServerLevel level) {
		Edict edict = edict(curse);
		Component title = Component.translatable("boss.jjkcraft.warden", curse.getDisplayName(),
				Component.translatable("edict.jjkcraft." + edict.key()).withColor(edict.color));
		MechanicUtil.bossBar(curse, level, curse.parameter("bar_range", 48), BossEvent.BossBarColor.RED, title);

		if (curse.getTarget() != null && !(curse.memory.get("sealed") instanceof Boolean)) {
			curse.memory.put("sealed", true);
			raiseSeals(curse, level, (int) curse.parameter("seals", 3));
		}

		if (curse.getHealth() / curse.getMaxHealth() <= 0.5 && !(curse.memory.get("resealed") instanceof Boolean)) {
			curse.memory.put("resealed", true);
			raiseSeals(curse, level, (int) curse.parameter("seals_second", 2));
		}

		int standing = sealsStanding(curse, level);
		curse.setMechB(standing);
		if (standing > 0 && curse.tickCount % 10 == 0) {
			Vec3 chest = curse.position().add(0, curse.getBbHeight() * 0.6, 0);
			for (BlockPos seal : seals(curse)) {
				MechanicUtil.line(level, JjkParticles.of(JjkParticles.CURSED_SPARK, 0xE0303F, 0.5f), chest, Vec3.atCenterOf(seal), 10);
			}
		}

		Edict fixed = fixedEdict(curse);
		int interval = (int) curse.parameter(curse.getHealth() / curse.getMaxHealth() <= 0.5 ? "edict_interval_second" : "edict_interval",
				curse.getHealth() / curse.getMaxHealth() <= 0.5 ? 240 : 400);
		if (fixed != null) {
			// The rules in force name its weak point; it changes only when they do.
			if (curse.mechA() != fixed.ordinal()) {
				curse.setMechA(fixed.ordinal());
				announceEdict(curse, level);
			}
		} else if (curse.tickCount % interval == 0 && curse.tickCount > 0) {
			curse.setMechA((curse.mechA() + 1 + curse.getRandom().nextInt(Edict.values().length - 1)) % Edict.values().length);
			announceEdict(curse, level);
		}
	}

	private static void announceEdict(CurseEntity curse, ServerLevel level) {
		MechanicUtil.sound(level, curse, JjkSounds.CURSE_TOLL, 2f, 1.3f);
		level.sendParticles(JjkParticles.of(JjkParticles.DOMAIN_SHARD, edict(curse).color, 1.4f), curse.getX(), curse.getY() + curse.getBbHeight(),
				curse.getZ(), 30, 0.8, 0.4, 0.8, 0.1);
	}

	private void raiseSeals(CurseEntity curse, ServerLevel level, int count) {
		double radius = curse.parameter("seal_radius", 9);
		for (int i = 0; i < count; i++) {
			double a = 2 * Math.PI * i / count + curse.getRandom().nextDouble();
			// Prefer the full ring; in cramped places, fall back closer in.
			for (double r : new double[] {radius, radius * 0.6, radius * 0.35}) {
				if (placeSeal(curse, level, BlockPos.containing(curse.getX() + Math.cos(a) * r, curse.getY(), curse.getZ() + Math.sin(a) * r))) {
					break;
				}
			}
		}

		MechanicUtil.sound(level, curse, JjkSounds.BARRIER, 2f, 0.6f);
	}

	private boolean placeSeal(CurseEntity curse, ServerLevel level, BlockPos column) {
		for (int dy = 3; dy >= -3; dy--) {
			BlockPos pos = column.above(dy);
			if (level.isLoaded(pos) && level.getBlockState(pos).isAir()
					&& level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), net.minecraft.core.Direction.UP)
					&& JjkBlocks.RULE_SEAL.placeFor(level, pos, (int) curse.parameter("seal_lifetime", 12000))) {
				seals(curse).add(pos);
				return true;
			}
		}

		return false;
	}

	/** Whether something keeps it from dying right now: its seals or an outside rule. */
	public static boolean bound(CurseEntity curse, ServerLevel level) {
		if (sealsStanding(curse, level) > 0) {
			return true;
		}

		for (Function<CurseEntity, Boolean> rule : RULES) {
			if (Boolean.TRUE.equals(rule.apply(curse))) {
				return true;
			}
		}

		return false;
	}

	@Override
	public float onHurt(CurseEntity curse, ServerLevel level, DamageSource source, float amount) {
		float scaled = edict(curse).matches(source) ? amount * (float) curse.parameter("weak_point", 1.5) : amount * (float) curse.parameter("off_edict", 0.15);
		if (bound(curse, level)) {
			// The rules hold: it can't drop below a sliver of health.
			scaled = Math.min(scaled, Math.max(0, curse.getHealth() - 1));
			if (curse.tickCount % 5 == 0) {
				level.sendParticles(JjkParticles.of(JjkParticles.CURSED_SPARK, 0xE0303F, 0.8f), curse.getX(), curse.getY() + 1.5, curse.getZ(), 6, 0.5, 0.5, 0.5, 0.05);
			}
		}

		return scaled;
	}

	@Override
	public void onRemoved(CurseEntity curse) {
		MechanicUtil.clearBossBar(curse);
		if (curse.level() instanceof ServerLevel level) {
			for (BlockPos seal : seals(curse)) {
				if (level.isLoaded(seal) && level.getBlockState(seal).is(JjkBlocks.RULE_SEAL)) {
					level.setBlockAndUpdate(seal, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
				}
			}
		}
	}
}
