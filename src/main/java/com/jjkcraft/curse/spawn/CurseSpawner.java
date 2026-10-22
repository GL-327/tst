package com.jjkcraft.curse.spawn;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import com.jjkcraft.JjkCraft;
import com.jjkcraft.block.JjkBlocks;
import com.jjkcraft.config.JjkConfig;
import com.jjkcraft.config.JjkServerConfig;
import com.jjkcraft.core.fear.FearMath;
import com.jjkcraft.curse.CurseDefinition;
import com.jjkcraft.curse.CurseEntity;
import com.jjkcraft.curse.Curses;
import com.jjkcraft.fear.Fear;

/**
 * Curses are born from fear. Every twelve seconds, for each player, the spawner may try one spot 28 to
 * 52 blocks away. If the spot is dark enough and its fear clears both the global threshold and a
 * curse's own minimum, a weighted pick spawns a group there. Very high fear can instead seed a
 * curse nest, which keeps spawning until a player breaks it.
 */
public final class CurseSpawner {
	private static final int INTERVAL = 240;
    private record Region(Identifier dimension,int x,int z){}
    private static final java.util.Map<Region,Long> REST=new java.util.HashMap<>();
    private static final java.util.Map<Region,Identifier> LAST=new java.util.HashMap<>();
    private static Region region(ServerLevel level,BlockPos p){return new Region(level.dimension().identifier(),Math.floorDiv(p.getX(),64),Math.floorDiv(p.getZ(),64));}
	private static final double NEST_FEAR = 75;
	private static final double NEST_CHANCE = 0.03;

	private CurseSpawner() {
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(CurseSpawner::tick);
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(s->{REST.clear();LAST.clear();});
	}

	private static void tick(MinecraftServer server) {
		JjkServerConfig config = JjkConfig.server();
		if (!config.systems.curseSpawns || server.getTickCount() % INTERVAL != 0 || config.curses.spawnRateMultiplier <= 0) {
			return;
		}

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (!player.isAlive() || player.isSpectator() || (player.isCreative() && !config.curses.spawnAroundCreative)) {
				continue;
			}

			ServerLevel level = player.level();
			if (level.dimension().identifier().getNamespace().equals("modulo")) continue;
			// Curses are monsters: the vanilla spawning rules and Peaceful apply to them too.
			if (level.getDifficulty() == Difficulty.PEACEFUL || !level.getGameRules().get(GameRules.SPAWN_MOBS)
					|| !level.getGameRules().get(GameRules.SPAWN_MONSTERS)) {
				continue;
			}

			RandomSource random = level.getRandom();
			if (random.nextDouble() > Math.min(1, 0.30 * config.curses.spawnRateMultiplier)) {
				continue;
			}

			AABB near = player.getBoundingBox().inflate(64);
			int budget = config.curses.maxCursesPerPlayer - level.getEntitiesOfClass(CurseEntity.class, near, Entity::isAlive).size();
			if (budget <= 0) {
				continue;
			}

			for (int attempt = 0; attempt < config.curses.spawnAttempts && budget > 0; attempt++) {
			double angle = random.nextDouble() * Math.PI * 2;
			double distance = 28 + random.nextDouble() * 24;
			int x = (int) Math.floor(player.getX() + Math.cos(angle) * distance);
			int z = (int) Math.floor(player.getZ() + Math.sin(angle) * distance);
			if (!level.hasChunkAt(new BlockPos(x, (int) player.getY(), z))) continue;
			Optional<BlockPos> spot = findSpot(level, x, (int) player.getY(), z, random);
			if (spot.isEmpty()) {
				continue;
			}

			var cell=region(level,spot.get());if(REST.getOrDefault(cell,0L)>level.getGameTime())continue;
            if(level.players().stream().anyMatch(other->other.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(spot.get()))<24*24))continue;
            var direction=net.minecraft.world.phys.Vec3.atCenterOf(spot.get()).subtract(player.getEyePosition());
            if(player.getLookAngle().dot(direction.normalize())>.5&&level.clip(new net.minecraft.world.level.ClipContext(player.getEyePosition(),net.minecraft.world.phys.Vec3.atCenterOf(spot.get()),net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,player)).getType()==net.minecraft.world.phys.HitResult.Type.MISS)continue;
            double fear = Fear.at(level, spot.get());
			if (fear < config.curses.fearSpawnThreshold) {
				if (random.nextDouble() * 100 >= config.curses.calmEncounterPercent) continue;
				fear = config.curses.fearSpawnThreshold;
			}

			if (fear >= NEST_FEAR && random.nextDouble() < NEST_CHANCE) {
				level.setBlockAndUpdate(spot.get(), JjkBlocks.CURSE_NEST.defaultBlockState());
				JjkCraft.LOGGER.debug("Curse nest formed at {} (fear {})", spot.get(), fear);
				continue;
			}

			int born=spawnAt(level, spot.get(), fear, random, false, budget);
            budget-=born;if(born>0){REST.put(cell,level.getGameTime()+1800);if(REST.size()>4096){REST.clear();LAST.clear();}break;}
			}
		}
	}

	/** A floor position near (x, z): surface if the player is outside, otherwise a cave floor near their height. */
	private static Optional<BlockPos> findSpot(ServerLevel level, int x, int playerY, int z, RandomSource random) {
		int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
		int startY = playerY < surface - 8 ? playerY + random.nextInt(9) - 4 : surface + 1;
		for (int dy = 0; dy < 12; dy++) {
			BlockPos pos = new BlockPos(x, startY - dy, z);
			if (standable(level, pos)) {
				return Optional.of(pos);
			}
		}

		return Optional.empty();
	}

	private static boolean standable(ServerLevel level, BlockPos pos) {
		return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)
				&& level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
				&& level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty();
	}

	/** Spawns a weighted group at {@code pos} for the given fear. Returns how many spawned. */
	public static int spawnAt(ServerLevel level, BlockPos pos, double fear, RandomSource random, boolean ignoreLight) {
		return spawnAt(level, pos, fear, random, ignoreLight, 16);
	}

	private static int spawnAt(ServerLevel level, BlockPos pos, double fear, RandomSource random, boolean ignoreLight, int budget) {
		Identifier dimension = level.dimension().identifier();
		boolean inWater = level.getFluidState(pos).isSource();
		int light = level.getMaxLocalRawBrightness(pos);
		List<Curses.Entry> candidates = new ArrayList<>();
		List<Double> weights = new ArrayList<>();
		double total = 0;
		for (Curses.Entry entry : Curses.all(level.registryAccess())) {
			Optional<CurseDefinition.Spawn> spawn = entry.definition().spawn();
			if (spawn.isEmpty() || spawn.get().water() != inWater
					|| (!spawn.get().dimensions().isEmpty() && !spawn.get().dimensions().contains(dimension))
					|| (!ignoreLight && light > spawn.get().maxLight())) {
				continue;
			}

			String biome=level.getBiome(pos).unwrapKey().map(k->k.identifier().getPath()).orElse("");
            int grade=com.jjkcraft.grade.Grades.rank(level.registryAccess(),entry.definition().grade());
            double habitat=CurseHabitat.weight(entry.id().getPath(),biome,pos.getY()<level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,pos.getX(),pos.getZ())-8,level.isRainingAt(pos),fear,grade);
            double variety=entry.id().equals(LAST.get(region(level,pos)))?.2:1;
            double weight = habitat*variety*FearMath.spawnWeight(spawn.get().weight(), fear, spawn.get().minFear())*com.jjkcraft.core.fear.CurseEncounterMath.rarity(com.jjkcraft.grade.Grades.rank(level.registryAccess(),entry.definition().grade()));
			if (weight > 0) {
				candidates.add(entry);
				weights.add(weight);
				total += weight;
			}
		}

		if (candidates.isEmpty()) {
			return 0;
		}

		double roll = random.nextDouble() * total;
		Curses.Entry chosen = candidates.getLast();
		for (int i = 0; i < candidates.size(); i++) {
			roll -= weights.get(i);
			if (roll <= 0) {
				chosen = candidates.get(i);
				break;
			}
		}

        if(LAST.size()>4096)LAST.clear();LAST.put(region(level,pos),chosen.id());
        CurseDefinition.Spawn rules = chosen.definition().spawn().orElseThrow();
		int rank=com.jjkcraft.grade.Grades.rank(level.registryAccess(),chosen.definition().grade());
        var nearby=level.getEntitiesOfClass(CurseEntity.class,new AABB(pos).inflate(24),Entity::isAlive);
        if(nearby.stream().anyMatch(c->com.jjkcraft.core.fear.CurseEncounterMath.solitary(com.jjkcraft.grade.Grades.rank(level.registryAccess(),c.grade()))) || com.jjkcraft.core.fear.CurseEncounterMath.solitary(rank)&&!nearby.isEmpty())return 0;
        int count = com.jjkcraft.core.fear.CurseEncounterMath.group(rank,random.nextDouble(),budget);
		int spawned = 0;
        var used=new java.util.HashSet<BlockPos>();
        for (int i=0;i<count;i++)for(int attempt=0;attempt<10;attempt++){
            BlockPos at=i==0&&attempt==0?pos:pos.offset(random.nextInt(9)-4,0,random.nextInt(9)-4);
            if(!level.hasChunkAt(at))continue;
            if(!standable(level,at)){if(standable(level,at.above()))at=at.above();else if(standable(level,at.below()))at=at.below();else continue;}
            if(!used.add(at))continue;
            Curses.Entry member=chosen;
            if(rank<3&&i>0&&random.nextFloat()<0.4f){
                var peers=candidates.stream().filter(c->com.jjkcraft.grade.Grades.rank(level.registryAccess(),c.definition().grade())==rank).toList();
                if(!peers.isEmpty())member=peers.get(random.nextInt(peers.size()));
            }
            if(spawnOne(level,member.definition().entity(),at,true)!=null){spawned++;break;}
        }

		return spawned;
	}

	/** Spawns any curse near {@code pos}, ignoring light (nests, rituals). */
	public static int spawnNear(ServerLevel level, BlockPos pos, int minRadius, int maxRadius) {
		RandomSource random = level.getRandom();
		for (int attempt = 0; attempt < 6; attempt++) {
			BlockPos at = pos.offset(random.nextInt(maxRadius * 2 + 1) - maxRadius, 0, random.nextInt(maxRadius * 2 + 1) - maxRadius);
			if (at.distManhattan(pos) < minRadius) {
				continue;
			}

			for (int dy = 3; dy >= -3; dy--) {
				BlockPos candidate = at.above(dy);
				if (standable(level, candidate)) {
					return spawnAt(level, candidate, Math.max(60, Fear.at(level, candidate)), random, true);
				}
			}
		}

		return 0;
	}

	/** Spawns one curse of the given entity type with the emergence animation. */
	public static CurseEntity spawnOne(ServerLevel level, Identifier entityType, BlockPos pos) {
		return spawnOne(level, entityType, pos, false);
	}

	private static CurseEntity spawnOne(ServerLevel level, Identifier entityType, BlockPos pos, boolean checkCollision) {
		EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(entityType);
		Entity entity = type.create(level, EntitySpawnReason.NATURAL);
		if (!(entity instanceof CurseEntity curse)) {
			return null;
		}

		curse.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.getRandom().nextFloat() * 360f, 0);
		if (checkCollision && !level.noCollision(curse)) return null;
		curse.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.NATURAL, null);
		level.addFreshEntity(curse);
		return curse;
	}
}
