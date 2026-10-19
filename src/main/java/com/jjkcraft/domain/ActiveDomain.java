package com.jjkcraft.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import com.jjkcraft.core.domain.DomainMath;
import com.jjkcraft.damage.JjkDamageTypes;

/**
 * One open domain: where it is, who cast it, its phase, who is inside, and who was caught when it
 * closed around them. Behaviors receive this as their context. Only {@link DomainService} changes
 * phases.
 */
public final class ActiveDomain {
	public enum Phase {
		/** The opening sequence: the barrier forms; no effects yet. */
		OPENING,
		ACTIVE,
		/** Pushing against another domain; effects pause until the clash resolves. */
		CLASHING,
		/** Fading out; no effects. */
		CLOSING
	}

	public static final int OPENING_TICKS = 30;
	public static final int CLOSING_TICKS = 20;

	private final int id;
	private final LivingEntity caster;
	private final ServerLevel level;
	private final Identifier domainId;
	private final DomainDefinition definition;
	private final DomainMath.Variant variant;
	private final Vec3 center;
	private final double radius;
	private final double power;
	private final long openedAt;
	private final Map<String, Object> memory = new HashMap<>();
	final Set<UUID> trapped = new HashSet<>();
	final Set<UUID> inside = new HashSet<>();
	private List<LivingEntity> insideNow = List.of();
	private Phase phase = Phase.OPENING;
	private long phaseStart;
	/** Game time the domain took hold (behaviors opened), or -1 while it hasn't. */
	private long heldAt = -1;
	ActiveDomain opponent;
	private boolean closeRequested;

	ActiveDomain(int id, LivingEntity caster, ServerLevel level, Identifier domainId, DomainDefinition definition, DomainMath.Variant variant,
			Vec3 center, double radius, double power) {
		this.id = id;
		this.caster = caster;
		this.level = level;
		this.domainId = domainId;
		this.definition = definition;
		this.variant = variant;
		this.center = center;
		this.radius = radius;
		this.power = power;
		this.openedAt = level.getGameTime();
		this.phaseStart = openedAt;
	}

	public int id() {
		return id;
	}

	public LivingEntity caster() {
		return caster;
	}

	public ServerLevel level() {
		return level;
	}

	public Identifier domainId() {
		return domainId;
	}

	public DomainDefinition definition() {
		return definition;
	}

	public DomainMath.Variant variant() {
		return variant;
	}

	public boolean hasBarrier() {
		return variant == DomainMath.Variant.FULL;
	}

	public Vec3 center() {
		return center;
	}

	public double radius() {
		return radius;
	}

	/** Multiplier on effects: variant and the caster's Domain growth. */
	public double power() {
		return power;
	}

	public long openedAt() {
		return openedAt;
	}

	public Phase phase() {
		return phase;
	}

	public long phaseStart() {
		return phaseStart;
	}

	void setPhase(Phase next) {
		phase = next;
		phaseStart = level.getGameTime();
	}

	/** Ticks since the domain took hold (0 before it has). */
	public int activeTicks() {
		return heldAt < 0 ? 0 : (int) (level.getGameTime() - heldAt);
	}

	public boolean tookHold() {
		return heldAt >= 0;
	}

	void markHeld() {
		heldAt = level.getGameTime();
	}

	/** Per-domain scratch state for behaviors. */
	public Map<String, Object> memory() {
		return memory;
	}

	public void requestClose() {
		closeRequested = true;
	}

	boolean closeRequested() {
		return closeRequested;
	}

	public boolean contains(Entity entity) {
		return contains(entity.position().add(0, entity.getBbHeight() / 2, 0));
	}

	public boolean contains(Vec3 point) {
		return point.distanceToSqr(center) <= radius * radius;
	}

	public AABB bounds() {
		return new AABB(center, center).inflate(radius);
	}

	/** Not the caster, not on the caster's team, not the caster's pets or summons, not creative or spectating. */
	public boolean isEnemy(LivingEntity entity) {
		if (entity == caster || !entity.isAlive() || entity.isAlliedTo(caster)) {
			return false;
		}

		if (entity instanceof Player player && (player.isCreative() || player.isSpectator())) {
			return false;
		}

		if (entity instanceof OwnableEntity owned && owned.getOwner() == caster) {
			return false;
		}

		return !(entity instanceof DomainSummon summon && caster.getUUID().equals(summon.summonerId()));
	}

	void refreshInside() {
		List<LivingEntity> now = new ArrayList<>(level.getEntitiesOfClass(LivingEntity.class, bounds(), this::contains));
		insideNow = now;
	}

	/** Every living entity inside right now, the caster included. */
	public List<LivingEntity> everyoneInside() {
		return insideNow;
	}

	/** Enemies inside, whether or not a Simple Domain shields them. */
	public List<LivingEntity> enemiesInside() {
		return insideNow.stream().filter(e -> e.isAlive() && isEnemy(e)).toList();
	}

	/**
	 * Enemies the domain's effects reach. A Simple Domain cancels sure-hit effects for whoever stands
	 * in it (costing its holder CE); a domain without sure-hit ignores Simple Domains.
	 */
	public List<LivingEntity> targets() {
		List<LivingEntity> out = new ArrayList<>();
		for (LivingEntity enemy : enemiesInside()) {
			if (!definition.sureHit() || !DomainService.shieldedFromSureHit(enemy)) {
				out.add(enemy);
			}
		}

		return out;
	}

	public DamageSource damageSource() {
		return level.damageSources().source(definition.sureHit() ? JjkDamageTypes.SURE_HIT : JjkDamageTypes.TECHNIQUE, caster);
	}

	/** Damages a target, already scaled by the caller. Pulses get past the invulnerability window. */
	public boolean hurt(LivingEntity target, double amount) {
		if (amount <= 0) {
			return false;
		}

		target.damageCooldownTime = 0;
		return target.hurtServer(level, damageSource(), (float) amount);
	}

	/** Tells nearby clients about something that happened (a pulse, a roll), for HUD and effects. */
	public void event(DomainService.Event kind, int value) {
		DomainService.sendEvent(this, kind, value);
	}
}
