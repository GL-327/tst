package com.jjkcraft.modulo.entity;

import java.util.Optional;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import com.jjkcraft.config.JjkConfig;
import com.jjkcraft.core.modulo.ModuloMath;
import com.jjkcraft.modulo.boss.BossBehavior;
import com.jjkcraft.modulo.data.BossDefinition;
import com.jjkcraft.modulo.data.ModuloRegistries;
import com.jjkcraft.modulo.mission.Missions;

/**
 * A Modulo boss (docs/MODULO.md 3.9): numbers from {@code modulo_boss} data, actions from its
 * {@link BossBehavior}. It belongs to one mission stage at one site: the players on that stage who
 * are at the site are its fighters, its health grows with their number, and they share the win when
 * it falls or yields. Bosses are never saved; the mission brings one back if a fight was cut short.
 */
public class ModuloBossEntity extends Monster {
	/** Flags clients read for visuals. */
	public static final int FLAG_DISGUISED = 1;
	public static final int FLAG_HIDDEN = 2;
	public static final int FLAG_CHARGING = 4;
	public static final int FLAG_INVERTED = 8;

	private static final EntityDataAccessor<String> SKIN = SynchedEntityData.defineId(ModuloBossEntity.class, EntityDataSerializers.STRING);
	private static final EntityDataAccessor<Integer> FLAGS = SynchedEntityData.defineId(ModuloBossEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> CHARGE = SynchedEntityData.defineId(ModuloBossEntity.class, EntityDataSerializers.INT);

	private @Nullable Identifier bossId;
	private @Nullable BossDefinition definition;
	private @Nullable BossBehavior behavior;
	private @Nullable Identifier site;
	private @Nullable Identifier mission;
	private int stage;
	private int fighters = 1;
	private double damageScale = 1;
	private boolean finished;
	private @Nullable ServerBossEvent bar;

	public ModuloBossEntity(EntityType<? extends Monster> type, Level level) {
		super(type, level);
		setPersistenceRequired();
		xpReward = 50;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 100)
				.add(Attributes.ATTACK_DAMAGE, 6)
				.add(Attributes.MOVEMENT_SPEED, 0.3)
				.add(Attributes.FOLLOW_RANGE, 40)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
				.add(Attributes.ARMOR, 4);
	}

	/** Sets the boss up from its data right after it is created; {@code fighters} sets its health. */
	public void setup(Identifier bossId, BossDefinition definition, @Nullable Identifier site, @Nullable Identifier mission, int stage, int fighters) {
		this.bossId = bossId;
		this.definition = definition;
		this.site = site;
		this.mission = mission;
		this.stage = stage;
		this.behavior = BossBehavior.create(definition.behavior());
		entityData.set(SKIN, definition.skin());
		getAttribute(Attributes.ARMOR).setBaseValue(definition.armor());
		getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(definition.damage());
		getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(definition.speed());
		getAttribute(Attributes.SCALE).setBaseValue(definition.scale());
		setCustomName(Component.translatable(definition.name()));
		this.fighters = 0;
		rescale(Math.max(1, fighters));
		setHealth(getMaxHealth());
		if (!isScene() && !definition.behavior().equals("minion")) {
			bar = new ServerBossEvent(getUUID(), Component.translatable(definition.name()), color(definition.color()), BossEvent.BossBarOverlay.NOTCHED_10);
		}

		if (level() instanceof ServerLevel level) {
			behavior.onSpawn(this, level);
		}
	}

	private static BossEvent.BossBarColor color(String name) {
		return switch (name) {
			case "pink" -> BossEvent.BossBarColor.PINK;
			case "blue" -> BossEvent.BossBarColor.BLUE;
			case "green" -> BossEvent.BossBarColor.GREEN;
			case "yellow" -> BossEvent.BossBarColor.YELLOW;
			case "purple" -> BossEvent.BossBarColor.PURPLE;
			case "white" -> BossEvent.BossBarColor.WHITE;
			default -> BossEvent.BossBarColor.RED;
		};
	}

	/** Health for {@code count} fighters, keeping the share of health it has left. */
	public void rescale(int count) {
		if (definition == null || count == fighters) {
			return;
		}

		double share = getMaxHealth() > 0 && fighters > 0 ? getHealth() / getMaxHealth() : 1;
		fighters = count;
		double max = ModuloMath.bossHealth(definition.health(), count, JjkConfig.server().modulo.bossHealthPerExtraPlayer);
		damageScale = ModuloMath.damageScale(max);
		getAttribute(Attributes.MAX_HEALTH).setBaseValue(max * damageScale);
		setHealth((float) Math.max(1, max * damageScale * share));
	}

	/** Its health for the current fighters, including any part above vanilla's ceiling (see {@link ModuloMath#damageScale}). */
	public double effectiveMaxHealth() {
		return getMaxHealth() / damageScale;
	}

	public int fighters() {
		return fighters;
	}

	public Optional<BossDefinition> definition() {
		return Optional.ofNullable(definition);
	}

	public @Nullable BossBehavior behavior() {
		return behavior;
	}

	public double param(String key, double fallback) {
		return definition == null ? fallback : definition.param(key, fallback);
	}

	public @Nullable Identifier bossId() {
		return bossId;
	}

	public @Nullable Identifier site() {
		return site;
	}

	public @Nullable Identifier mission() {
		return mission;
	}

	public int stage() {
		return stage;
	}

	/** A scene boss (Dabura facing Mahoraga) takes no part in the players' fight. */
	public boolean isScene() {
		return definition != null && definition.behavior().equals("mahoraga_scene");
	}

	public String skin() {
		return entityData.get(SKIN);
	}

	public boolean hasFlag(int flag) {
		return (entityData.get(FLAGS) & flag) != 0;
	}

	public void setFlag(int flag, boolean on) {
		int flags = entityData.get(FLAGS);
		int next = on ? flags | flag : flags & ~flag;
		if (next != flags) {
			entityData.set(FLAGS, next);
		}
	}

	/** A charge in progress, 0 to 100 (Dabura's darkness). */
	public int charge() {
		return entityData.get(CHARGE);
	}

	public void setCharge(int value) {
		entityData.set(CHARGE, Math.max(0, Math.min(100, value)));
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(SKIN, "");
		builder.define(FLAGS, 0);
		builder.define(CHARGE, 0);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.1, true));
		goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 16));
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, (target, level) -> !isScene()));
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
		if (isScene()) {
			return !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY);
		}

		return super.isInvulnerableTo(level, source) || hasFlag(FLAG_HIDDEN) && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY);
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		float adjusted = (behavior == null ? amount : behavior.onHurt(this, level, source, amount)) * (float) damageScale;
		return adjusted > 0 && super.hurtServer(level, source, adjusted);
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (behavior == null || definition == null) {
			// A boss without its setup (summoned by command): nothing to fight for.
			if (tickCount > 1) {
				discard();
			}

			return;
		}

		behavior.tick(this, level);
	}

	@Override
	public void tick() {
		super.tick();
		// The bar follows the boss even while something has switched its AI off (a restraint).
		if (bar != null && level() instanceof ServerLevel level) {
			bar.setProgress(getHealth() / getMaxHealth());
			if (tickCount % 10 == 0) {
				for (ServerPlayer player : level.players()) {
					boolean near = player.distanceToSqr(this) < 48 * 48 && player.isAlive();
					if (near && !bar.getPlayers().contains(player)) {
						bar.addPlayer(player);
					} else if (!near && bar.getPlayers().contains(player)) {
						bar.removePlayer(player);
					}
				}
			}
		}
	}

	/** The fight ends without a death: the boss yields or is taken away. Its fighters win. */
	public void yieldFight(Component message) {
		if (finished || !(level() instanceof ServerLevel level)) {
			return;
		}

		finished = true;
		for (ServerPlayer player : level.players()) {
			if (player.distanceToSqr(this) < 48 * 48) {
				player.sendSystemMessage(message);
			}
		}

		level.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1, getZ(), 60, 0.6, 1, 0.6, 0.1);
		Missions.onBossDefeated(this);
		discard();
	}

	@Override
	public void die(DamageSource source) {
		super.die(source);
		if (!finished && level() instanceof ServerLevel) {
			finished = true;
			Missions.onBossDefeated(this);
		}
	}

	@Override
	public void remove(RemovalReason reason) {
		if (level() instanceof ServerLevel level && behavior != null) {
			behavior.onRemoved(this, level);
		}

		if (bar != null) {
			bar.removeAllPlayers();
		}

		super.remove(reason);
	}
}
