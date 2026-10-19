package com.jjkcraft.curse.mechanic;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import com.jjkcraft.JjkCraft;
import com.jjkcraft.block.JjkBlocks;
import com.jjkcraft.curse.CurseEntity;
import com.jjkcraft.curse.spawn.CurseSpawner;
import com.jjkcraft.particle.JjkParticles;
import com.jjkcraft.sound.JjkSounds;

/**
 * Gravebound Gate (Grade 1 boss): a standing gate of grave doors that never moves. When it wakes
 * it raises a wall around the clearing (for a limited time, and only where there is open air) and
 * shows a boss bar. It calls lesser curses through its doors; while two or more of them stand,
 * chains link them to the gate and it takes far less damage. Kill the summons, then the gate.
 * Its phases change the arena. At half health the doors open and the wall closes in tighter: it
 * drags everyone toward its maw (the ground spirals inward first) and bites anything that stays
 * close. At a quarter, graves open in the floor and hurt anyone standing on them. When it falls,
 * its wall and its summons go with it.
 *
 * MECH_A: 0 closed, 1 open (second phase), 2 graves open (third phase). MECH_B: wind-up ticks left of the current call or pull (-1 when none).
 */
public final class GateMechanic implements CurseMechanic {
	private static final int CHAIN_COLOR = 0x5E4B7A;
	private static final List<Identifier> MINIONS = List.of(JjkCraft.id("wick_moth"), JjkCraft.id("hollow_hymnal"), JjkCraft.id("ledger_crawler"));

	@Override
	public void onSpawn(CurseEntity curse, ServerLevel level) {
		// A boss stays where it was found.
		curse.setPersistenceRequired();
		curse.setMechB(-1);
	}

	@Override
	public void tick(CurseEntity curse, ServerLevel level) {
		// Rooted in place.
		Vec3 motion = curse.getDeltaMovement();
		curse.setDeltaMovement(0, motion.y, 0);
		curse.getNavigation().stop();

		MechanicUtil.bossBar(curse, level, curse.parameter("bar_range", 40), BossEvent.BossBarColor.PURPLE, null);
		List<CurseEntity> minions = minions(curse, level);
		if (minions.size() >= 2 && curse.tickCount % 10 == 0) {
			Vec3 gate = curse.position().add(0, curse.getBbHeight() * 0.5, 0);
			for (CurseEntity minion : minions) {
				MechanicUtil.line(level, JjkParticles.of(JjkParticles.CURSED_SPARK, CHAIN_COLOR, 0.5f), gate,
						minion.position().add(0, minion.getBbHeight() / 2, 0), 10);
			}
		}

		LivingEntity target = curse.getTarget();
		if (target != null && MechanicUtil.get(curse, "arena_cooldown") <= 0) {
			raiseArena(curse, level);
		}

		MechanicUtil.countDown(curse, "arena_cooldown");

		float health = curse.getHealth() / curse.getMaxHealth();
		if (curse.mechA() == 0 && health <= 0.5) {
			// Second stage: the doors open and the walls close in.
			curse.setMechA(1);
			curse.setMechB(-1);
			MechanicUtil.sound(level, curse, JjkSounds.CURSE_ROAR, 3f, 0.5f);
			MechanicUtil.particles(level, JjkParticles.of(JjkParticles.DOMAIN_SHARD, CHAIN_COLOR, 1.5f), curse.position().add(0, 1.3, 0), 40, 1.2, 0.15);
			reshapeArena(curse, level, curse.parameter("phase_two_arena_scale", 0.65));
		} else if (curse.mechA() == 1 && health <= 0.25) {
			// Third stage: the graves in the floor open.
			curse.setMechA(2);
			openGraves(curse, level);
			MechanicUtil.sound(level, curse, JjkSounds.CURSE_DEATH, 3f, 0.4f);
		}

		if (curse.mechA() == 2) {
			tickGraves(curse, level);
		}

		// The open maw bites anything that stays close.
		if (curse.mechA() >= 1 && curse.tickCount % 10 == 0) {
			for (LivingEntity victim : MechanicUtil.victims(level, curse, curse.parameter("maw_radius", 2.5))) {
				victim.hurtServer(level, MechanicUtil.attack(level, curse), (float) curse.parameter("maw_damage", 4));
			}
		}

		int windup = curse.mechB();
		if (windup > 0) {
			windupFx(curse, level, windup);
			curse.setMechB(windup - 1);
			if (windup == 1) {
				curse.setMechB(-1);
				if (MechanicUtil.get(curse, "action") == 1) {
					pull(curse, level);
				} else {
					summon(curse, level, target);
				}
			}

			return;
		}

		if (target == null) {
			return;
		}

		if (curse.mechA() >= 1 && MechanicUtil.countDown(curse, "pull_cooldown") <= 0) {
			MechanicUtil.set(curse, "action", 1);
			curse.setMechB(30);
			curse.triggerAnim("action", "cast");
			MechanicUtil.sound(level, curse, JjkSounds.ATTRACT, 2f, 0.4f);
		} else if (MechanicUtil.countDown(curse, "summon_cooldown") <= 0 && minions.size() < curse.parameter("max_minions", 3)) {
			MechanicUtil.set(curse, "action", 0);
			curse.setMechB(30);
			curse.triggerAnim("action", "cast");
			MechanicUtil.sound(level, curse, JjkSounds.SUMMON, 1.5f, 0.6f);
		}
	}

	private void windupFx(CurseEntity curse, ServerLevel level, int windup) {
		if (windup % 2 != 0) {
			return;
		}

		if (MechanicUtil.get(curse, "action") == 1) {
			// The ground spirals inward toward the maw.
			double radius = curse.parameter("pull_radius", 16) * (windup / 30.0);
			for (int i = 0; i < 4; i++) {
				double a = windup * 0.35 + i * Math.PI / 2;
				level.sendParticles(JjkParticles.of(JjkParticles.CE_AURA, CHAIN_COLOR, 1f), curse.getX() + Math.cos(a) * radius, curse.getY() + 0.1,
						curse.getZ() + Math.sin(a) * radius, 2, 0.2, 0.02, 0.2, 0);
			}
		} else {
			level.sendParticles(JjkParticles.of(JjkParticles.DOMAIN_SHARD, CHAIN_COLOR, 0.8f), curse.getX(), curse.getY() + curse.getBbHeight() * 0.5,
					curse.getZ(), 4, curse.getBbWidth() * 0.3, curse.getBbHeight() * 0.3, 0.1, 0.02);
		}
	}

	private void summon(CurseEntity curse, ServerLevel level, LivingEntity target) {
		int count = 1 + (curse.mechA() == 1 ? 1 : curse.getRandom().nextInt(2));
		@SuppressWarnings("unchecked")
		List<UUID> ids = (List<UUID>) curse.memory.computeIfAbsent("minions", k -> new ArrayList<UUID>());
		Vec3 front = curse.position().add(curse.getLookAngle().multiply(1, 0, 1).normalize().scale(2.5));
		for (int i = 0; i < count; i++) {
			Identifier type = MINIONS.get(curse.getRandom().nextInt(MINIONS.size()));
			BlockPos at = BlockPos.containing(front.x + curse.getRandom().nextGaussian(), curse.getY(), front.z + curse.getRandom().nextGaussian());
			CurseEntity minion = CurseSpawner.spawnOne(level, type, at);
			if (minion != null) {
				ids.add(minion.getUUID());
				if (target != null) {
					minion.setTarget(target);
				}
			}
		}

		MechanicUtil.set(curse, "summon_cooldown", (int) curse.parameter("summon_interval", 220));
	}

	private void pull(CurseEntity curse, ServerLevel level) {
		double radius = curse.parameter("pull_radius", 16);
		for (LivingEntity victim : MechanicUtil.victims(level, curse, radius)) {
			MechanicUtil.pull(victim, curse.position(), curse.parameter("pull_strength", 1.1) * Math.min(1, victim.distanceTo(curse) / 6));
		}

		level.sendParticles(ParticleTypes.SCULK_SOUL, curse.getX(), curse.getY() + 1.2, curse.getZ(), 30, 1, 0.8, 1, 0.05);
		MechanicUtil.sound(level, curse, JjkSounds.DOMAIN_CLASH, 1.5f, 0.5f);
		MechanicUtil.set(curse, "pull_cooldown", (int) curse.parameter("pull_interval", 140));
	}

	/** Living summons of this gate. Forgets the dead ones. */
	private static List<CurseEntity> minions(CurseEntity curse, ServerLevel level) {
		if (!(curse.memory.get("minions") instanceof List<?> raw)) {
			return List.of();
		}

		List<CurseEntity> alive = new ArrayList<>();
		Iterator<?> it = raw.iterator();
		while (it.hasNext()) {
			if (it.next() instanceof UUID id && level.getEntity(id) instanceof CurseEntity minion && minion.isAlive() && minion.distanceTo(curse) < 48) {
				alive.add(minion);
			} else {
				it.remove();
			}
		}

		return alive;
	}

	/** Tears down the current wall and raises a new one at {@code scale} of the full radius. */
	private void reshapeArena(CurseEntity curse, ServerLevel level, double scale) {
		removeWall(curse, level);
		curse.memory.put("arena_scale", scale);
		MechanicUtil.set(curse, "arena_cooldown", 0);
		if (curse.getTarget() != null) {
			raiseArena(curse, level);
		}
	}

	private static void removeWall(CurseEntity curse, ServerLevel level) {
		if (curse.memory.remove("arena") instanceof List<?> wall) {
			for (Object o : wall) {
				BlockPos pos = (BlockPos) o;
				if (level.isLoaded(pos) && level.getBlockState(pos).is(JjkBlocks.GATE_BARRIER)) {
					level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
				}
			}
		}
	}

	/** Grave pits: spots on the arena floor that hurt anyone standing on them. */
	private void openGraves(CurseEntity curse, ServerLevel level) {
		List<Vec3> graves = new ArrayList<>();
		double radius = curse.parameter("arena_radius", 14) * (curse.memory.get("arena_scale") instanceof Double d ? d : 1.0) * 0.8;
		int count = (int) curse.parameter("graves", 6);
		for (int i = 0; i < count; i++) {
			double a = 2 * Math.PI * i / count + curse.getRandom().nextDouble() * 0.5;
			double r = radius * (0.35 + curse.getRandom().nextDouble() * 0.55);
			graves.add(curse.position().add(Math.cos(a) * r, 0, Math.sin(a) * r));
		}

		curse.memory.put("graves", graves);
	}

	@SuppressWarnings("unchecked")
	private void tickGraves(CurseEntity curse, ServerLevel level) {
		if (!(curse.memory.get("graves") instanceof List<?> raw)) {
			return;
		}

		List<Vec3> graves = (List<Vec3>) raw;
		boolean pulse = curse.tickCount % 20 == 0;
		for (Vec3 grave : graves) {
			if (curse.tickCount % 4 == 0) {
				MechanicUtil.ring(level, JjkParticles.of(JjkParticles.CE_AURA, 0x2A2233, 1.2f), grave, 1.5, 10);
			}

			if (pulse) {
				for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(grave, grave).inflate(1.5, 1.5, 1.5),
						e -> e.isAlive() && !(e instanceof CurseEntity) && !(e instanceof net.minecraft.world.entity.player.Player p && (p.isCreative() || p.isSpectator())))) {
					victim.hurtServer(level, MechanicUtil.attack(level, curse), (float) curse.parameter("grave_damage", 3));
				}
			}
		}
	}

	private void raiseArena(CurseEntity curse, ServerLevel level) {
		int radius = (int) Math.round(curse.parameter("arena_radius", 14) * (curse.memory.get("arena_scale") instanceof Double d ? d : 1.0));
		int ticks = (int) curse.parameter("arena_ticks", 900);
		MechanicUtil.set(curse, "arena_cooldown", ticks + 200);
		if (!MechanicUtil.griefing(level)) {
			return;
		}

		@SuppressWarnings("unchecked")
		List<BlockPos> wall = (List<BlockPos>) curse.memory.computeIfAbsent("arena", k -> new ArrayList<BlockPos>());
		BlockPos center = curse.blockPosition();
		int points = (int) (2 * Math.PI * radius * 1.5);
		for (int i = 0; i < points; i++) {
			double a = 2 * Math.PI * i / points;
			BlockPos column = center.offset((int) Math.round(Math.cos(a) * radius), 0, (int) Math.round(Math.sin(a) * radius));
			// Find the ground near the gate's height, then stack the wall three high.
			for (int dy = 4; dy >= -4; dy--) {
				BlockPos base = column.above(dy);
				if (level.getBlockState(base).isAir() && !level.getBlockState(base.below()).isAir()) {
					for (int h = 0; h < 3; h++) {
						BlockPos pos = base.above(h);
						if (!wall.contains(pos) && JjkBlocks.GATE_BARRIER.placeFor(level, pos, ticks)) {
							wall.add(pos);
						}
					}

					break;
				}
			}
		}

		MechanicUtil.sound(level, curse, JjkSounds.BARRIER, 2f, 0.5f);
	}

	@Override
	public float onHurt(CurseEntity curse, ServerLevel level, DamageSource source, float amount) {
		if (minions(curse, level).size() >= 2) {
			level.sendParticles(JjkParticles.of(JjkParticles.CURSED_SPARK, CHAIN_COLOR, 0.8f), curse.getX(), curse.getY() + curse.getBbHeight() / 2,
					curse.getZ(), 8, 0.6, 0.6, 0.6, 0.05);
			return amount * (float) curse.parameter("guarded_damage_taken", 0.35);
		}

		return amount;
	}

	@Override
	public void onDeath(CurseEntity curse, ServerLevel level, DamageSource source) {
		for (CurseEntity minion : minions(curse, level)) {
			minion.kill(level);
		}
	}

	@Override
	public void onRemoved(CurseEntity curse) {
		MechanicUtil.clearBossBar(curse);
		if (curse.level() instanceof ServerLevel level) {
			removeWall(curse, level);
		}
	}

	/** True for entities a gate summoned (used by tests). */
	public static boolean isMinionOf(CurseEntity gate, Entity entity) {
		return gate.memory.get("minions") instanceof List<?> ids && ids.contains(entity.getUUID());
	}
}
