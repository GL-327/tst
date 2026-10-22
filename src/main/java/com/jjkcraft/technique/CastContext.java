package com.jjkcraft.technique;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import com.jjkcraft.core.technique.TechniqueMath;
import com.jjkcraft.damage.JjkDamageTypes;
import com.jjkcraft.damage.TechniqueDamageSource;
import com.jjkcraft.network.payload.TechniqueFxPayload;

/**
 * Everything a behavior needs during a cast: the caster, the technique, scaled power, and
 * server-side targeting and damage helpers. Targeting always uses the caster's server-side eye
 * position and look direction; client aim is never trusted.
 */
public final class CastContext {
	private final LivingEntity caster;
	private final ServerLevel level;
	private final Identifier techniqueId;
	private final TechniqueDefinition technique;
	private final double power;
	private final double rangeMultiplier;
	private final TechniqueDetails details;
	/** Per-cast state for channel and toggle behaviors (a stored aim point, a counter). */
	public final java.util.Map<String, Object> memory = new java.util.HashMap<>();

	public CastContext(LivingEntity caster, ServerLevel level, Identifier techniqueId, TechniqueDefinition technique, double power, double rangeMultiplier) {
		this.caster = caster;
		this.level = level;
		this.techniqueId = techniqueId;
		this.technique = technique;
		this.power = power;
		this.rangeMultiplier = rangeMultiplier;
		this.details=Personalization.get(caster,techniqueId).details();
	}

	public LivingEntity caster() {
		return caster;
	}

	public ServerLevel level() {
		return level;
	}

	public Identifier techniqueId() {
		return techniqueId;
	}

	public TechniqueDefinition technique() {
		return technique;
	}

	/** Damage and effect multiplier (grade, Output, mastery, vows). */
	public double power() {
		return power;
	}
	public TechniqueDetails details(){return details;}

	/** The technique's range after vow modifiers. */
	public double range() {
		return technique.cast().range() * rangeMultiplier;
	}

	public Vec3 eye() {
		return caster.getEyePosition();
	}

	public Vec3 look() {
		return aim(0,0);
	}
	public Vec3 aim(double yawOffset,double pitchOffset){
		double yaw=Math.toRadians(caster.getYRot()+details.expression().yaw()+yawOffset);
		double pitch=Math.toRadians(Math.clamp(caster.getXRot()+details.expression().pitch()+pitchOffset,-89,89));
		return new Vec3(-Math.sin(yaw)*Math.cos(pitch),-Math.sin(pitch),Math.cos(yaw)*Math.cos(pitch));
	}

	public long now() {
		return level.getGameTime();
	}

	// ---------------------------------------------------------------- targeting

	/** True for anything a technique may harm: living, not the caster, not an ally or the caster's pet. */
	public boolean isEnemy(Entity entity) {
		if(details.targeting()==1 && !(entity instanceof LivingEntity target && com.jjkcraft.reverse.ReverseTechnique.curse(target)))return false;
		if(details.targeting()==2 && !(entity instanceof net.minecraft.world.entity.player.Player))return false;
		if(details.targeting()==3 && entity instanceof net.minecraft.world.entity.player.Player)return false;
		if(level.getAttachedOrCreate(com.jjkcraft.awakening.BodyTransfer.BODIES).containsKey(entity.getUUID().toString())) return false;
		if (!(entity instanceof LivingEntity living) || entity == caster || !living.isAlive() || living.isSpectator()) {
			return false;
		}

		if (caster.isAlliedTo(entity)) {
			return false;
		}

		if (entity instanceof OwnableEntity ownable && ownable.getOwner() == caster) {
			return false;
		}

		return !(caster instanceof OwnableEntity own && own.getOwner() == entity);
	}

	/** The first block the look ray hits within {@code distance}, or the end of the ray. */
	public Vec3 blockLimit(double distance) {
		Vec3 end = eye().add(look().scale(distance));
		HitResult hit = level.clip(new ClipContext(eye(), end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
		return hit.getType() == HitResult.Type.MISS ? end : hit.getLocation();
	}

	/** The nearest enemy along the look ray within {@code distance}, stopping at blocks. */
	public Optional<LivingEntity> rayTarget(double distance, double margin) {
		Vec3 from = eye();
		Vec3 to = blockLimit(distance);
		AABB box = caster.getBoundingBox().expandTowards(to.subtract(from)).inflate(1 + margin);
		EntityHitResult hit = ProjectileUtil.getEntityHitResult(caster, from, to, box.inflate(margin), this::isEnemy, from.distanceToSqr(to));
		return hit != null && hit.getEntity() instanceof LivingEntity living ? Optional.of(living) : Optional.empty();
	}

	public List<LivingEntity> enemiesInRadius(Vec3 center, double radius) {
		AABB box = AABB.ofSize(center, radius * 2, radius * 2, radius * 2);
		List<LivingEntity> list = new ArrayList<>();
		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, this::isEnemy)) {
			if (e.position().add(0, e.getBbHeight() / 2, 0).distanceTo(center) <= radius + e.getBbWidth() / 2) {
				list.add(e);
			}
		}

		list.sort(Comparator.comparingDouble(e -> e.distanceToSqr(center)));
		return list;
	}

	/** Enemies within {@code distance} whose center is inside a cone of {@code degrees} around the look direction. */
	public List<LivingEntity> enemiesInCone(double distance, double degrees) {
		double cos = Math.cos(Math.toRadians(degrees / 2));
		Vec3 look = look();
		List<LivingEntity> list = new ArrayList<>();
		for (LivingEntity e : enemiesInRadius(eye(), distance)) {
			Vec3 to = e.position().add(0, e.getBbHeight() / 2, 0).subtract(eye());
			if (to.lengthSqr() < 1e-6 || to.normalize().dot(look) >= cos) {
				list.add(e);
			}
		}

		return list;
	}

	/** Enemies whose bounding box crosses the segment from the eye along the look ray. */
	public List<LivingEntity> enemiesOnLine(double length, double halfWidth) {
		Vec3 from = eye();
		Vec3 to = blockLimit(length);
		AABB sweep = new AABB(from, to).inflate(halfWidth + 1);
		List<LivingEntity> list = new ArrayList<>();
		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, sweep, this::isEnemy)) {
			if (e.getBoundingBox().inflate(halfWidth).clip(from, to).isPresent()) {
				list.add(e);
			}
		}

		return list;
	}

	// ---------------------------------------------------------------- damage

	public DamageSource source(boolean piercing) {
		ResourceKey<DamageType> type = piercing ? JjkDamageTypes.TECHNIQUE_PIERCING : JjkDamageTypes.TECHNIQUE;
		return new TechniqueDamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(type), caster, techniqueId());
	}

	/** Deals technique damage, already scaled by the caller. Returns true if the target took damage. */
	public boolean hurt(LivingEntity target, double amount, boolean piercing) {
		if (amount <= 0) {
			return false;
		}

		boolean hit=target.hurtServer(level, source(piercing), (float) TechniqueMath.damage(amount,com.jjkcraft.config.JjkConfig.server().casting.techniqueDamageMultiplier));
		if(hit&&details.expression().lift()!=0&&!java.util.Objects.equals(memory.get("lift:"+target.getId()),now())){
			memory.put("lift:"+target.getId(),now());target.setDeltaMovement(target.getDeltaMovement().add(0,details.expression().lift()/200.0,0));target.needsSync=true;
		}return hit;
	}

	/** Deals damage split between armored and armor-ignoring parts. */
	public boolean hurtSplit(LivingEntity target, double amount, double pierceFraction) {
		double[] parts = TechniqueMath.armorSplit(amount, pierceFraction);
		boolean hit = false;
		if (parts[1] > 0) {
			hit = hurt(target, parts[1], true);
			// Let the second hit through the damage cooldown of the first.
			target.damageCooldownTime = 0;
		}

		if (parts[0] > 0) {
			hit |= hurt(target, parts[0], false);
		}

		return hit;
	}

	public void knockback(LivingEntity target, Vec3 from, double strength) {
		Vec3 push = target.position().subtract(from).multiply(1, 0, 1);
		if (push.lengthSqr() < 1e-6) {
			push = look();
		}

		push = push.normalize();
		target.knockback(strength, -push.x, -push.z, source(false), 0f);
		target.needsSync = true;
	}

	// ---------------------------------------------------------------- visuals

	public void fx(TechniqueFxPayload.Phase phase, Vec3 origin, Vec3 direction, List<? extends Entity> targets) {
		TechniqueFx.send(this, phase, origin, direction, targets);
	}

	public void fx(TechniqueFxPayload.Phase phase, Vec3 origin) {
		fx(phase, origin, look(), List.of());
	}

	public boolean isPlayer() {
		return caster instanceof ServerPlayer;
	}
}
