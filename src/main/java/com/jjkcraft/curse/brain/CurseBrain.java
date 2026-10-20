package com.jjkcraft.curse.brain;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import com.jjkcraft.curse.CurseDefinition;
import com.jjkcraft.curse.CurseEntity;
import com.jjkcraft.energy.CursedEnergy;
import com.jjkcraft.fear.Fear;
import com.jjkcraft.sorcerer.Progression;
import com.jjkcraft.technique.CastFailure;
import com.jjkcraft.technique.TechniqueDefinition;
import com.jjkcraft.technique.TechniqueService;
import com.jjkcraft.technique.Techniques;

/**
 * The shared curse AI: IDLE (wander), HUNT (close in, melee, look for a technique to cast), CAST
 * (hold still while the technique plays), RECOVER (a short pause after acting) and FLEE (weak
 * curses break off when badly hurt and come back once recovered). It remembers the last place
 * it saw its target for a while, alerts its own kind when hit, and grows bolder where fear runs
 * high. Mechanics can pause it or forbid attacks.
 */
public final class CurseBrain {
	public enum State {
		IDLE, HUNT, CAST, RECOVER, FLEE
	}

	private static final int MEMORY_TICKS = 200;

	private final CurseEntity curse;
	private State state = State.IDLE;
	private int stateTicks;
	private int attackCooldown;
	private int castCooldown = 40;
	private Vec3 lastSeen;
	private long lastSeenTick = Long.MIN_VALUE;
	private boolean fled;
    private long guardUntil,guardReady;

	public CurseBrain(CurseEntity curse) {
		this.curse = curse;
	}

	public State state() {
		return state;
	}

	public void setState(State next, int ticks) {
		state = next;
		stateTicks = ticks;
	}

	/** Lets mechanics delay technique use (for example after their own special attack). */
	public void delayCasting(int ticks) {
		castCooldown = Math.max(castCooldown, ticks);
	}

	public void tick(ServerLevel level) {
		com.jjkcraft.awakening.ControlledCurses.direct(curse);
		if (attackCooldown > 0) {
			attackCooldown--;
		}

		if (castCooldown > 0) {
			castCooldown--;
		}

		if (stateTicks > 0) {
			stateTicks--;
		}

		if (!curse.mechanic().canAct(curse)) {
			curse.getNavigation().stop();
			state = State.IDLE;
			return;
		}

		LivingEntity target = curse.getTarget();
        if(com.jjkcraft.config.JjkConfig.server().systems.cursedCombat){
            long tick=level.getGameTime();
            int rank=Progression.gradeRank(level.registryAccess(),curse);
            double chance=curse.parameter("guard_chance",rank<=1?0.06:rank<=3?0.14:0.24);
            if(target!=null&&target.isAlive()&&curse.distanceToSqr(target)<20&&guardReady<=tick&&com.jjkcraft.combat.CursedCombat.attacking(target)&&curse.getRandom().nextFloat()<chance){guardUntil=tick+10;guardReady=tick+60;}
            if(guardUntil>tick&&target!=null){aimAt(target);if(com.jjkcraft.combat.CursedCombat.guardMob(curse,target.isShiftKeyDown()?0:1)){curse.getNavigation().stop();return;}}
            else com.jjkcraft.combat.CursedCombat.guardMob(curse,-1);
            if(com.jjkcraft.combat.CursedCombat.busy(curse)){curse.getNavigation().stop();return;}
        }
		long now = level.getGameTime();
		if (target != null && target.isAlive() && curse.getSensing().hasLineOfSight(target)) {
			lastSeen = target.position();
			lastSeenTick = now;
		}

		switch (state) {
			case CAST -> {
				if (target != null) {
					curse.getLookControl().setLookAt(target);
				}

				if (stateTicks <= 0) {
					setState(State.RECOVER, 8);
				}
			}
			case RECOVER -> {
				if (stateTicks <= 0) {
					state = target != null ? State.HUNT : State.IDLE;
				}
			}
			case FLEE -> flee(target);
			case IDLE, HUNT -> {
				if (target == null || !target.isAlive()) {
					if (lastSeen != null && now - lastSeenTick < MEMORY_TICKS) {
						// Search the last known position.
						if (curse.getNavigation().isDone()) {
							curse.getNavigation().moveTo(lastSeen.x, lastSeen.y, lastSeen.z, 1.0);
						}

						state = State.HUNT;
					} else {
						state = State.IDLE;
					}

					return;
				}

				state = State.HUNT;
				hunt(level, target);
			}
		}
	}

	private void hunt(ServerLevel level, LivingEntity target) {
		double health = curse.getHealth() / curse.getMaxHealth();
		int rank = Progression.gradeRank(level.registryAccess(), curse);
		if (!fled && health < 0.25 && rank <= 2 && curse.getRandom().nextFloat() < 0.4f) {
			fled = true;
			setState(State.FLEE, 70);
			return;
		}

		curse.getLookControl().setLookAt(target, 30, 30);
		double distance = curse.distanceTo(target);
		double reach = curse.getBbWidth() * 0.9 + target.getBbWidth() * 0.5 + 1.0;

		if (curse.mechanic().canAttack(curse) && castCooldown <= 0 && tryCast(level, target, distance)) {
			return;
		}

		if (curse.tickCount % 8 == 0 || curse.getNavigation().isDone()) {
			curse.getNavigation().moveTo(target, distance > 8 ? 1.15 : 1.0);
		}

		if (distance <= reach && attackCooldown <= 0 && curse.mechanic().canAttack(curse)) {
			attackCooldown = 13 + curse.getRandom().nextInt(8);
			if (com.jjkcraft.config.JjkConfig.server().systems.cursedCombat) {
				com.jjkcraft.combat.CursedCombat.startCurse(curse, target);
			} else {
				curse.doHurtTarget(level, target);
			}
			setState(State.RECOVER, 6);
		}
	}

	/** Picks an affordable, ready technique in range and casts it at the target. */
	private boolean tryCast(ServerLevel level, LivingEntity target, double distance) {
		CurseDefinition def = curse.definition().orElse(null);
		if (def == null || def.techniques().isEmpty()) {
			return false;
		}

		List<Identifier> options = new ArrayList<>();
		long now = level.getGameTime();
		double ce = CursedEnergy.current(curse);
		for (Identifier id : def.techniques()) {
			TechniqueDefinition technique = Techniques.get(level.registryAccess(), id).orElse(null);
			if (technique == null || TechniqueService.readyAt(curse, id) > now) {
				continue;
			}

			double range = technique.cast().range() > 0 ? technique.cast().range() : 4;
			if (distance <= range && ce >= TechniqueService.effectiveCost(curse, id, technique)) {
				options.add(id);
			}
		}

		if (options.isEmpty()) {
			castCooldown = 10;
			return false;
		}

		aimAt(target);
		Identifier choice = options.get(curse.getRandom().nextInt(options.size()));
		if (TechniqueService.cast(curse, choice, -1) == CastFailure.NONE) {
			curse.triggerAnim("action", "cast");
			curse.getNavigation().stop();
			setState(State.CAST, 14);
			// Bolder where fear runs high: fear 100 halves the wait between casts.
			double fear = Fear.at(level, curse.blockPosition());
			castCooldown = (int) Math.round((50 + curse.getRandom().nextInt(40)) * (1 - fear / 200.0));
			return true;
		}

		castCooldown = 20;
		return false;
	}

	/** Points the curse's head and body straight at the target so techniques aim correctly this tick. */
	public void aimAt(LivingEntity target) {
		Vec3 eye = curse.getEyePosition();
		Vec3 to = target.getEyePosition().subtract(eye);
		float yaw = (float) (Mth.atan2(to.z, to.x) * 180 / Math.PI) - 90f;
		float pitch = (float) -(Mth.atan2(to.y, Math.sqrt(to.x * to.x + to.z * to.z)) * 180 / Math.PI);
		curse.setYRot(yaw);
		curse.setXRot(pitch);
		curse.yHeadRot = yaw;
		curse.yBodyRot = yaw;
	}

	private void flee(LivingEntity target) {
		if (stateTicks <= 0 || target == null) {
			state = State.HUNT;
			return;
		}

		if (curse.getNavigation().isDone() || curse.tickCount % 10 == 0) {
			Vec3 away = curse.position().subtract(target.position()).multiply(1, 0, 1).normalize().scale(10);
			Vec3 to = curse.position().add(away);
			curse.getNavigation().moveTo(to.x, to.y, to.z, 1.3);
		}
	}

	/** Sends nearby curses of the same kind after whoever hurt this one. */
	public void alertPack(LivingEntity attacker) {
		if (attacker == curse) {
			return;
		}

		AABB box = curse.getBoundingBox().inflate(16);
		for (CurseEntity other : curse.level().getEntitiesOfClass(CurseEntity.class, box, c -> c != curse && c.getType() == curse.getType())) {
			if (other.getTarget() == null) {
				other.setTarget(attacker);
			}
		}
	}

	/**
	 * Holds the move and look controls while the brain is busy, so wandering goals stay out of the
	 * way. Mobs register goals from their constructor, before the curse's fields exist, so the
	 * brain is looked up when the goal runs.
	 */
	public static final class Goal extends net.minecraft.world.entity.ai.goal.Goal {
		private final CurseEntity curse;

		public Goal(CurseEntity curse) {
			this.curse = curse;
			setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			CurseBrain brain = curse.brain();
			return brain != null && brain.state != State.IDLE;
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}
	}
}
