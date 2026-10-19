package com.jjkcraft.modulo.entity;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import com.jjkcraft.modulo.mission.Missions;

/**
 * A Modulo character: Maru, Cross, Yuka and Tsurugi Okkotsu, Usami, Mino, Kyoko Tomoe, Osada,
 * Masayoshi, Jabaloma, and Dabura when he speaks rather than fights (docs/MODULO.md 3.8). Right-click to talk. Characters are never saved and
 * cannot be hurt: missions bring them where they are needed.
 *
 * <ul>
 * <li>{@link Role#SITE}: stands at a mission site.</li>
 * <li>{@link Role#ALLY}: fights the boss of a mission battle.</li>
 * <li>{@link Role#COMPANION}: Maru, following one player through the expansion and fighting with Chaos.</li>
 * </ul>
 */
public class ModuloNpcEntity extends PathfinderMob {
	public enum Role {
		SITE, ALLY, COMPANION
	}

	/** Every character the mod knows; each has a skin and a name ({@code npc.jjkcraft.modulo.<id>}). */
	public static final Set<String> CHARACTERS = com.jjkcraft.modulo.data.ModuloCharacters.ALL;

	private static final EntityDataAccessor<String> CHARACTER = SynchedEntityData.defineId(ModuloNpcEntity.class, EntityDataSerializers.STRING);
	private static final EntityDataAccessor<Boolean> CHAOS = SynchedEntityData.defineId(ModuloNpcEntity.class, EntityDataSerializers.BOOLEAN);

	private Role role = Role.SITE;
	private @Nullable UUID owner;
	private @Nullable Identifier site;
	private BlockPos home = BlockPos.ZERO;
	private int abilityCooldown = 40;

	public ModuloNpcEntity(EntityType<? extends PathfinderMob> type, Level level) {
		super(type, level);
		setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 40)
				.add(Attributes.ATTACK_DAMAGE, 6)
				.add(Attributes.MOVEMENT_SPEED, 0.32)
				.add(Attributes.FOLLOW_RANGE, 32)
				.add(Attributes.ARMOR, 6);
	}

	/** Sets the character up right after it is created. */
	public void setup(String character, Role role, @Nullable UUID owner, @Nullable Identifier site, BlockPos home) {
		entityData.set(CHARACTER, character);
		this.role = role;
		this.owner = owner;
		this.site = site;
		this.home = home.immutable();
		// Masayoshi is a first-grader; Dabura stands a head taller than anyone.
		getAttribute(Attributes.SCALE).setBaseValue(switch (character) {
			case "masayoshi" -> 0.65;
			case "dabura" -> 1.15;
			default -> 1.0;
		});
		setCustomName(Component.translatable("npc.jjkcraft.modulo." + character));
		setCustomNameVisible(role != Role.ALLY);
	}

	public String character() {
		return entityData.get(CHARACTER);
	}

	public Role role() {
		return role;
	}

	public @Nullable UUID owner() {
		return owner;
	}

	public @Nullable Identifier site() {
		return site;
	}

	/** Whether Maru's third eye is open (Chaos in use); clients draw the eye. */
	public boolean chaos() {
		return entityData.get(CHAOS);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(CHARACTER, "maru");
		builder.define(CHAOS, false);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.2, true));
		goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8));
		goalSelector.addGoal(7, new RandomLookAroundGoal(this));
	}

	@Override
	public boolean shouldBeSaved() {
		return false;
	}

	@Override
	public boolean removeWhenFarAway(double distSqr) {
		return false;
	}

	@Override
	public boolean isInvulnerableTo(ServerLevel level, DamageSource source) {
		return !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY);
	}

	@Override
	public boolean isPushable() {
		return role != Role.SITE;
	}

	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (hand == InteractionHand.MAIN_HAND && player instanceof ServerPlayer server) {
			Missions.onTalk(server, this);
			return InteractionResult.SUCCESS_SERVER;
		}

		return InteractionResult.SUCCESS;
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		switch (role) {
			case SITE -> {
				getNavigation().stop();
				if (distanceToSqr(Vec3.atBottomCenterOf(home)) > 4) {
					snapTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, getYRot(), 0);
				}
			}
			case ALLY -> fight(level);
			case COMPANION -> companion(level);
		}
	}

	/** Allies attack the nearest boss or monster near them. */
	private void fight(ServerLevel level) {
		LivingEntity target = getTarget();
		if (target == null || !target.isAlive() || tickCount % 40 == 0) {
			List<LivingEntity> foes = level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(20),
					e -> e.isAlive() && (e instanceof ModuloBossEntity boss && !boss.isScene() || e instanceof Enemy) && !(e instanceof ModuloNpcEntity));
			foes.sort((a, b) -> Double.compare(a.distanceToSqr(this), b.distanceToSqr(this)));
			setTarget(foes.isEmpty() ? null : foes.getFirst());
		}
	}

	/** Maru follows his player, fights what they fight, and uses Chaos on it. */
	private void companion(ServerLevel level) {
		ServerPlayer player = owner == null ? null : level.getServer().getPlayerList().getPlayer(owner);
		if (player == null || player.level() != level) {
			discard();
			return;
		}

		double distance = distanceToSqr(player);
		if (distance > 24 * 24) {
			snapTo(player.getX(), player.getY(), player.getZ(), getYRot(), 0);
			getNavigation().stop();
		}

		LivingEntity threat = player.getLastHurtByMob() != null && player.getLastHurtByMob().isAlive() ? player.getLastHurtByMob() : player.getLastHurtMob();
		if (threat != null && (!threat.isAlive() || threat == this || threat instanceof ModuloNpcEntity || threat instanceof Player)) {
			threat = null;
		}

		setTarget(threat);
		if (threat == null && distance > 16) {
			getNavigation().moveTo(player, 1.1);
		}

		// Chaos: turn aside projectiles flying at Maru's player.
		for (Projectile projectile : level.getEntitiesOfClass(Projectile.class, player.getBoundingBox().inflate(4))) {
			if (projectile.getOwner() != player && projectile.getOwner() != this && projectile.getDeltaMovement().lengthSqr() > 0.04) {
				Vec3 away = projectile.position().subtract(player.position()).normalize().scale(0.8);
				projectile.setDeltaMovement(away.x, 0.3, away.z);
				projectile.needsSync = true;
				chaos(level, true);
			}
		}

		if (--abilityCooldown <= 0 && threat != null && threat.distanceToSqr(this) < 14 * 14 && hasLineOfSight(threat)) {
			// Chaos through the third eye: confusion, and gravity let go for a moment.
			threat.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 100, 0), this);
			threat.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 2), this);
			threat.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 20, 1), this);
			level.playSound(null, getX(), getY(), getZ(), SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.NEUTRAL, 0.8f, 1.4f);
			chaos(level, true);
			abilityCooldown = 120;
		} else if (abilityCooldown < 100) {
			chaos(level, false);
		}
	}

	private void chaos(ServerLevel level, boolean open) {
		if (entityData.get(CHAOS) != open) {
			entityData.set(CHAOS, open);
		}
	}

	/** Whether this companion belongs to {@code player}. */
	public boolean ownedBy(Player player) {
		return role == Role.COMPANION && player.getUUID().equals(owner);
	}

	public static AABB around(BlockPos pos, double radius) {
		return new AABB(pos).inflate(radius);
	}
}
