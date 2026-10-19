package com.jjkcraft.curse.mechanic;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;

import com.jjkcraft.curse.CurseEntity;
import com.jjkcraft.particle.JjkParticles;

/**
 * Doorframe Hunter (Grade 4): it lives in door frames. With nothing to hunt it steps into the
 * nearest door's frame and vanishes; whoever comes close, or opens that door, is ambushed with a
 * heavy first blow. In a fight it slips from door to door: a door near its prey rattles for a
 * moment (the tell), then it steps out of it. Away from every door it is exposed and takes more
 * damage. Sensing shows it in the frame.
 *
 * MECH_A: 0 out, 1 hiding in a frame, 2 crossing to another door.
 */
public final class DoorframeMechanic implements CurseMechanic {
	public static final int OUT = 0;
	public static final int HIDING = 1;
	public static final int CROSSING = 2;

	@Override
	public void onSpawn(CurseEntity curse, ServerLevel level) {
		curse.setMechA(OUT);
	}

	@SuppressWarnings("unchecked")
	private static List<BlockPos> doors(CurseEntity curse, ServerLevel level) {
		List<BlockPos> cached = (List<BlockPos>) curse.memory.get("doors");
		if (cached == null || MechanicUtil.countDown(curse, "door_scan") <= 0) {
			cached = scan(level, curse.blockPosition(), (int) curse.parameter("door_range", 12));
			curse.memory.put("doors", cached);
			MechanicUtil.set(curse, "door_scan", 60);
		}

		cached.removeIf(pos -> !isDoor(level.getBlockState(pos)));
		return cached;
	}

	private static boolean isDoor(BlockState state) {
		return state.getBlock() instanceof DoorBlock && state.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER;
	}

	/** Lower halves of every door within {@code range}. Public for tests. */
	public static List<BlockPos> scan(ServerLevel level, BlockPos center, int range) {
		List<BlockPos> found = new ArrayList<>();
		for (BlockPos pos : BlockPos.betweenClosed(center.offset(-range, -4, -range), center.offset(range, 4, range))) {
			if (level.isLoaded(pos) && isDoor(level.getBlockState(pos))) {
				found.add(pos.immutable());
			}
		}

		return found;
	}

	private static @Nullable BlockPos nearest(List<BlockPos> doors, Vec3 to, double maxDistance, @Nullable BlockPos except) {
		BlockPos best = null;
		double bestDistance = maxDistance * maxDistance;
		for (BlockPos door : doors) {
			double d = Vec3.atBottomCenterOf(door).distanceToSqr(to);
			if (d < bestDistance && !door.equals(except)) {
				best = door;
				bestDistance = d;
			}
		}

		return best;
	}

	@Override
	public void tick(CurseEntity curse, ServerLevel level) {
		List<BlockPos> doors = doors(curse, level);
		if (curse.mechA() == HIDING) {
			hide(curse, level);
			return;
		}

		LivingEntity target = curse.getTarget();
		if (target == null || !target.isAlive()) {
			// Calm: walk to the nearest door and step into its frame.
			if (MechanicUtil.get(curse, "calm") < 60) {
				MechanicUtil.set(curse, "calm", MechanicUtil.get(curse, "calm") + 1);
				return;
			}

			BlockPos door = nearest(doors, curse.position(), curse.parameter("door_range", 12), null);
			if (door != null) {
				// A closed door can't be walked into: it walks up to it, then steps into the frame.
				if (curse.position().distanceTo(Vec3.atBottomCenterOf(door)) < 2.6) {
					curse.snapTo(door.getX() + 0.5, door.getY(), door.getZ() + 0.5, curse.getYRot(), 0);
					curse.memory.put("frame", door);
					curse.setInvisible(true);
					curse.setMechA(HIDING);
					curse.getNavigation().stop();
				} else if (curse.tickCount % 20 == 0) {
					Vec3 side = curse.position().subtract(Vec3.atBottomCenterOf(door)).multiply(1, 0, 1);
					Vec3 approach = Vec3.atBottomCenterOf(door).add(side.lengthSqr() > 1e-4 ? side.normalize().scale(1.5) : Vec3.ZERO);
					curse.getNavigation().moveTo(approach.x, approach.y, approach.z, 0.8);
				}
			}

			return;
		}

		MechanicUtil.set(curse, "calm", 0);
		int windup = MechanicUtil.get(curse, "cross_windup");
		if (windup > 0) {
			BlockPos to = (BlockPos) curse.memory.get("cross_to");
			if (to != null && windup % 4 == 0) {
				// The tell: the destination door rattles.
				level.sendParticles(ParticleTypes.CRIT, to.getX() + 0.5, to.getY() + 1.2, to.getZ() + 0.5, 6, 0.3, 0.6, 0.3, 0.05);
				level.playSound(null, to, SoundEvents.WOODEN_DOOR_CLOSE, SoundSource.HOSTILE, 0.7f, 1.6f);
			}

			MechanicUtil.set(curse, "cross_windup", windup - 1);
			if (windup == 1 && to != null && isDoor(level.getBlockState(to))) {
				MechanicUtil.particles(level, JjkParticles.of(JjkParticles.CURSED_SPARK, 0x6B5B4A, 0.8f), curse.position().add(0, 1, 0), 16, 0.3, 0.05);
				curse.teleportTo(to.getX() + 0.5, to.getY(), to.getZ() + 0.5);
				curse.memory.put("ambush", true);
				level.playSound(null, to, SoundEvents.WOODEN_DOOR_OPEN, SoundSource.HOSTILE, 1f, 0.6f);
				curse.setMechA(OUT);
			}

			return;
		}

		if (MechanicUtil.countDown(curse, "cross") <= 0) {
			MechanicUtil.set(curse, "cross", (int) curse.parameter("cross_interval", 100));
			BlockPos here = curse.blockPosition();
			BlockPos to = nearest(doors, target.position(), curse.parameter("cross_range", 6), null);
			if (to != null && to.distSqr(here) > 16) {
				curse.memory.put("cross_to", to);
				MechanicUtil.set(curse, "cross_windup", (int) curse.parameter("cross_windup", 15));
				curse.setMechA(CROSSING);
			}
		}
	}

	private void hide(CurseEntity curse, ServerLevel level) {
		curse.setDeltaMovement(Vec3.ZERO);
		curse.getNavigation().stop();
		BlockPos frame = (BlockPos) curse.memory.get("frame");
		boolean open = frame != null && level.getBlockState(frame).getBlock() instanceof DoorBlock
				&& level.getBlockState(frame).getValue(DoorBlock.OPEN);
		Player near = level.getNearestPlayer(curse.getX(), curse.getY(), curse.getZ(), curse.parameter("reveal_distance", 4),
				p -> p instanceof Player player && !player.isSpectator() && !player.isCreative());
		if (open || near != null || frame == null || !isDoor(level.getBlockState(frame))) {
			reveal(curse, level, near);
		}
	}

	private static void reveal(CurseEntity curse, ServerLevel level, @Nullable LivingEntity target) {
		curse.setInvisible(false);
		curse.setMechA(OUT);
		curse.memory.put("ambush", true);
		MechanicUtil.set(curse, "calm", 0);
		MechanicUtil.set(curse, "cross", 60);
		if (target != null) {
			curse.setTarget(target);
		}

		MechanicUtil.particles(level, JjkParticles.of(JjkParticles.CURSED_SPARK, 0x6B5B4A, 0.9f), curse.position().add(0, 1, 0), 20, 0.4, 0.08);
		level.playSound(null, curse.getX(), curse.getY(), curse.getZ(), SoundEvents.WOODEN_DOOR_OPEN, SoundSource.HOSTILE, 1.2f, 0.5f);
	}

	@Override
	public boolean canAct(CurseEntity curse) {
		return curse.mechA() == OUT;
	}

	@Override
	public double attackMultiplier(CurseEntity curse, LivingEntity target) {
		return curse.memory.get("ambush") instanceof Boolean ambush && ambush ? curse.parameter("ambush_multiplier", 1.8) : 1.0;
	}

	@Override
	public void onAttack(CurseEntity curse, ServerLevel level, LivingEntity target) {
		curse.memory.put("ambush", false);
	}

	@Override
	public float onHurt(CurseEntity curse, ServerLevel level, DamageSource source, float amount) {
		if (curse.mechA() == HIDING) {
			reveal(curse, level, source.getEntity() instanceof LivingEntity living ? living : null);
		}

		boolean exposed = nearest(doors(curse, level), curse.position(), curse.parameter("door_range", 12), null) == null;
		return exposed ? amount * (float) curse.parameter("exposed_damage_taken", 1.3) : amount;
	}

	@Override
	public void onRemoved(CurseEntity curse) {
		curse.setInvisible(false);
	}
}
