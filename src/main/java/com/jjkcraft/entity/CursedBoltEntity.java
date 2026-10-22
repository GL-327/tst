package com.jjkcraft.entity;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import com.jjkcraft.network.payload.TechniqueFxPayload;
import com.jjkcraft.technique.CastContext;
import com.jjkcraft.technique.TechniqueService;

/**
 * A cursed energy projectile fired by the {@code projectile} behavior. Optionally homing,
 * piercing, or exploding on impact. It is not saved: bolts vanish when their chunk unloads.
 * The client draws it as a glowing core with a ribbon trail; size and color are synced.
 */
public class CursedBoltEntity extends ThrowableProjectile {
	private int burnSeconds;
	public void burnSeconds(int seconds){burnSeconds=Math.clamp(seconds,0,20);}
	private static final EntityDataAccessor<Integer> COLOR = SynchedEntityData.defineId(CursedBoltEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Float> SIZE = SynchedEntityData.defineId(CursedBoltEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<String> TECHNIQUE = SynchedEntityData.defineId(CursedBoltEntity.class, EntityDataSerializers.STRING);

	private CastContext ctx;
	private double damage;
	private boolean homing;
	private double homingStrength;
	private double blastRadius;
	private boolean pierce;
	private int lifetime = 60;
	private double gravity;
	private final Set<Integer> alreadyHit = new HashSet<>();

	public CursedBoltEntity(EntityType<? extends CursedBoltEntity> type, Level level) {
		super(type, level);
	}

	/** Configures a server-side bolt. */
	public CursedBoltEntity configure(CastContext ctx, double damage, boolean homing, double homingStrength, double blastRadius,
			boolean pierce, int lifetime, double gravity, float size) {
		this.ctx = ctx;
		this.damage = damage;
		this.homing = homing;
		this.homingStrength = homingStrength;
		this.blastRadius = blastRadius;
		this.pierce = pierce;
		this.lifetime = lifetime;
		this.gravity = gravity;
		this.entityData.set(COLOR, ctx.technique().visuals().color());
		this.entityData.set(SIZE, size);
        this.entityData.set(TECHNIQUE,ctx.techniqueId().toString());
		setOwner(ctx.caster());
		return this;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(COLOR, 0xFF8F5CFF);
		builder.define(SIZE, 0.35f);
        builder.define(TECHNIQUE,"");
	}

	public int color() {
		return entityData.get(COLOR);
	}

	public float size() {
		return entityData.get(SIZE);
	}

	@Override
	protected double getDefaultGravity() {
		return gravity;
	}

	@Override
	protected float getAirDrag() {
		return 1.0f;
	}

	@Override
	public void tick() {
		if (!level().isClientSide()) {
			if (ctx == null || tickCount > lifetime) {
				discard();
				return;
			}

			// Steering starts on the second tick: waiting longer let near targets slip past the turn.
			if (homing && tickCount > 0) {
				steer();
			}
            if(pierce)com.jjkcraft.technique.TechniqueTerrain.line(ctx,position(),position().add(getDeltaMovement()),Math.min(3,Math.max(.8,size())));
		}

		super.tick();
	}

	private void steer() {
		Vec3 velocity = getDeltaMovement();
		double speed = velocity.length();
		if (speed < 1e-4) {
			return;
		}

		LivingEntity best = null;
		double bestScore = Double.MAX_VALUE;
		for (LivingEntity candidate : ctx.enemiesInRadius(position(), 16)) {
			if (alreadyHit.contains(candidate.getId())) {
				continue;
			}

			Vec3 to = candidate.position().add(0, candidate.getBbHeight() / 2, 0).subtract(position());
			double facing = to.normalize().dot(velocity.normalize());
			if (facing < 0.2) {
				continue;
			}

			double score = to.length() * (2 - facing);
			if (score < bestScore) {
				bestScore = score;
				best = candidate;
			}
		}

		if (best != null) {
			Vec3 desired = best.position().add(0, best.getBbHeight() / 2, 0).subtract(position()).normalize().scale(speed);
			setDeltaMovement(velocity.lerp(desired, homingStrength).normalize().scale(speed));
		}
	}

	@Override
	protected boolean canHitEntity(Entity entity) {
		return super.canHitEntity(entity) && !alreadyHit.contains(entity.getId()) && (ctx == null || ctx.isEnemy(entity));
	}

	@Override
	protected void onHitEntity(EntityHitResult hit) {
		if (!(level() instanceof ServerLevel) || ctx == null || !(hit.getEntity() instanceof LivingEntity target)) {
			return;
		}

		if (alreadyHit.isEmpty()) {
			TechniqueService.creditHit(ctx.caster(), ctx.techniqueId());
		}

		alreadyHit.add(target.getId());
		if(burnSeconds>0)target.igniteForSeconds(burnSeconds);
		ctx.hurtSplit(target,damage*ctx.power(),Math.max(0,ctx.details().penetration())/100.0);
		if (blastRadius > 0) {
			explode(hit.getLocation());
		} else {
			ctx.fx(TechniqueFxPayload.Phase.HIT, hit.getLocation(), getDeltaMovement().normalize(), List.of(target));
		}

		if (!pierce) {
			discard();
		}
	}

	@Override
	protected void onHitBlock(BlockHitResult hit) {
		super.onHitBlock(hit);
		if (level() instanceof ServerLevel && ctx != null) {
            com.jjkcraft.technique.TechniqueTerrain.sphere(ctx,hit.getLocation(),Math.max(.9,Math.min(4,size()+Math.sqrt(ctx.power())*.5)),pierce?24:8,false);
			if (blastRadius > 0) {
				explode(hit.getLocation());
			} else {
				ctx.fx(TechniqueFxPayload.Phase.HIT, hit.getLocation(), getDeltaMovement().normalize(), List.of());
			}
            if(pierce&&level().getBlockState(hit.getBlockPos()).isAir())return;
		}

		discard();
	}

	private void explode(Vec3 at) {
        com.jjkcraft.technique.TechniqueTerrain.sphere(ctx,at,blastRadius,48,false);
		List<LivingEntity> targets = ctx.enemiesInRadius(at, blastRadius);
		for (LivingEntity target : targets) {
			if(burnSeconds>0)target.igniteForSeconds(burnSeconds);
			if (alreadyHit.add(target.getId())) {
				double falloff = 1 - 0.5 * Math.min(1, target.position().distanceTo(at) / blastRadius);
				falloff*=ctx.details().expression().falloff(target.position().distanceTo(at),blastRadius);
				ctx.hurtSplit(target,damage*ctx.power()*falloff,Math.max(0,ctx.details().penetration())/100.0);
			}

			ctx.knockback(target, at, 0.8);
		}

		ctx.fx(TechniqueFxPayload.Phase.BURST, at, new Vec3(blastRadius, 0, 0), targets);
	}

	public Identifier technique() {
		return ctx == null ? Identifier.tryParse(entityData.get(TECHNIQUE)) : ctx.techniqueId();
	}
}
