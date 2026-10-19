package com.jjkcraft.curse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootTable;

import com.jjkcraft.curse.brain.CurseBrain;
import com.jjkcraft.curse.mechanic.CurseMechanic;
import com.jjkcraft.curse.mechanic.CurseMechanics;
import com.jjkcraft.energy.CursedEnergy;
import com.jjkcraft.energy.CursedEnergyUser;
import com.jjkcraft.fear.Fear;
import com.jjkcraft.grade.GradedEntity;
import com.jjkcraft.particle.JjkParticles;
import com.jjkcraft.sorcerer.HeavenlyRestriction;
import com.jjkcraft.sorcerer.SorcererProgress;

/**
 * Base class for every curse. Its numbers come from the curse registry entry whose {@code entity}
 * is this entity's type; its unique behavior comes from the named {@link CurseMechanic}. The
 * shared {@link CurseBrain} runs targeting, movement, melee and technique casting.
 */
public class CurseEntity extends Monster implements GeoEntity, CursedEnergyUser, GradedEntity {
	/** Brain state, for animation and the codex preview. */
	public static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(CurseEntity.class, EntityDataSerializers.INT);
	/** Two integers a mechanic may use to show its state on the client (charging, phase, disguise target). */
	public static final EntityDataAccessor<Integer> MECH_A = SynchedEntityData.defineId(CurseEntity.class, EntityDataSerializers.INT);
	public static final EntityDataAccessor<Integer> MECH_B = SynchedEntityData.defineId(CurseEntity.class, EntityDataSerializers.INT);
	/** True while the curse wears a disguise model (the Redacted Saint). */
	public static final EntityDataAccessor<Boolean> DISGUISED = SynchedEntityData.defineId(CurseEntity.class, EntityDataSerializers.BOOLEAN);
	/** Ticks of the emergence animation left. */
	public static final EntityDataAccessor<Integer> EMERGING = SynchedEntityData.defineId(CurseEntity.class, EntityDataSerializers.INT);

	public static final int EMERGE_TICKS = 28;
	public static final int DEATH_TICKS = 30;

	private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
	private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
	private static final RawAnimation SPAWN = RawAnimation.begin().thenPlayAndHold("spawn");
	private static final RawAnimation DEATH = RawAnimation.begin().thenPlayAndHold("death");

	private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
	private final CurseBrain brain = new CurseBrain(this);
	private Curses.Entry entry;
	private boolean statsApplied;
	/** Per-curse scratch state for mechanics (timers, counters) that does not need to reach the client. */
	public final Map<String, Object> memory = new HashMap<>();

	public CurseEntity(EntityType<? extends CurseEntity> type, Level level) {
		super(type, level);
		this.xpReward = 5;
	}

	// ---------------------------------------------------------------- data

	public Optional<Curses.Entry> entry() {
		if (entry == null) {
			entry = Curses.forEntityType(registryAccess(), getType()).orElse(null);
		}

		return Optional.ofNullable(entry);
	}

	public Optional<CurseDefinition> definition() {
		return entry().map(Curses.Entry::definition);
	}

	public Identifier curseId() {
		return entry().map(Curses.Entry::id).orElse(BuiltInRegistries.ENTITY_TYPE.getKey(getType()));
	}

	public CurseMechanic mechanic() {
		return definition().map(d -> CurseMechanics.get(d.mechanic())).orElse(CurseMechanics.PLAIN);
	}

	public double parameter(String key, double fallback) {
		return definition().map(d -> d.parameter(key, fallback)).orElse(fallback);
	}

	public CurseBrain brain() {
		return brain;
	}

	@Override
	public Identifier grade() {
		return definition().map(CurseDefinition::grade).orElse(SorcererProgress.STARTING_GRADE);
	}

	@Override
	public double maxCursedEnergy() {
		return definition().map(d -> d.stats().ceCapacity()).orElse(50.0);
	}

	@Override
	public double cursedEnergyRegenMultiplier() {
		double base = definition().map(d -> d.stats().regenMultiplier()).orElse(1.0);
		// Curses feed on fear: regen rises with the fear around them.
		return level() instanceof ServerLevel server ? base * (1 + Fear.at(server, blockPosition()) / 50.0) : base;
	}

	public int mechA() {
		return entityData.get(MECH_A);
	}

	public void setMechA(int value) {
		entityData.set(MECH_A, value);
	}

	public int mechB() {
		return entityData.get(MECH_B);
	}

	public void setMechB(int value) {
		entityData.set(MECH_B, value);
	}

	public boolean isDisguised() {
		return entityData.get(DISGUISED);
	}

	public void setDisguised(boolean disguised) {
		entityData.set(DISGUISED, disguised);
	}

	public boolean isEmerging() {
		return entityData.get(EMERGING) > 0;
	}

	/** Items the curse took (the Tithe Burrower's thefts). Saved; dropped when it dies. */
	private final List<net.minecraft.world.item.ItemStack> stash = new ArrayList<>();

	public List<net.minecraft.world.item.ItemStack> stash() {
		return stash;
	}

	@Override
	protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
		super.dropCustomDeathLoot(level, source, killedByPlayer);
		for (net.minecraft.world.item.ItemStack stack : stash) {
			spawnAtLocation(level, stack);
		}

		stash.clear();
	}

	/** The curse's data names its loot table; without one, the entity type's default is used. */
	@Override
	protected void dropFromLootTable(ServerLevel level, DamageSource source, boolean playerKilled) {
		Optional<ResourceKey<LootTable>> table = definition().flatMap(CurseDefinition::lootTable)
				.map(id -> ResourceKey.create(Registries.LOOT_TABLE, id));
		if (table.isPresent()) {
			dropFromLootTable(level, source, playerKilled, table.get());
		} else {
			super.dropFromLootTable(level, source, playerKilled);
		}
	}

	// ---------------------------------------------------------------- setup

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(STATE, 0);
		builder.define(MECH_A, 0);
		builder.define(MECH_B, -1);
		builder.define(EMERGING, 0);
		builder.define(DISGUISED, false);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(1, new CurseBrain.Goal(this));
		goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
		goalSelector.addGoal(7, new RandomLookAroundGoal(this));
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true,
				(target, level) -> !(target instanceof Player p && p.isCreative()) && !(target instanceof ServerPlayer sp && HeavenlyRestriction.has(sp) && random.nextInt(4) != 0)));
	}

	@Override
	public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, SpawnGroupData groupData) {
		SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, groupData);
		applyStats();
		entityData.set(EMERGING, EMERGE_TICKS);
		if (level instanceof ServerLevel server) {
			mechanic().onSpawn(this, server);
		}

		return data;
	}

	/** Sets attribute base values from data. Safe to call again; only runs once per entity. */
	public void applyStats() {
		if (statsApplied) {
			return;
		}

		definition().ifPresent(def -> {
			CurseDefinition.Stats s = def.stats();
			setBase(Attributes.MAX_HEALTH, s.health());
			setBase(Attributes.ARMOR, s.armor());
			setBase(Attributes.MOVEMENT_SPEED, s.speed());
			setBase(Attributes.ATTACK_DAMAGE, s.attackDamage());
			setBase(Attributes.FOLLOW_RANGE, s.followRange());
			setBase(Attributes.KNOCKBACK_RESISTANCE, s.knockbackResist());
			setHealth(getMaxHealth());
			CursedEnergy.fill(this);
			statsApplied = true;
		});
	}

	private void setBase(Holder<Attribute> attribute, double value) {
		AttributeInstance instance = getAttribute(attribute);
		if (instance != null) {
			instance.setBaseValue(value);
		}
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putBoolean("jjk_stats_applied", statsApplied);
		output.putInt("jjk_mech_a", mechA());
		output.putInt("jjk_mech_b", mechB());
		output.putBoolean("jjk_disguised", isDisguised());
		if (!stash.isEmpty()) {
			output.store("jjk_stash", net.minecraft.world.item.ItemStack.OPTIONAL_CODEC.listOf(), List.copyOf(stash));
		}
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		statsApplied = input.getBooleanOr("jjk_stats_applied", false);
		setMechA(input.getIntOr("jjk_mech_a", 0));
		setMechB(input.getIntOr("jjk_mech_b", -1));
		setDisguised(input.getBooleanOr("jjk_disguised", false));
		stash.clear();
		input.read("jjk_stash", net.minecraft.world.item.ItemStack.OPTIONAL_CODEC.listOf()).ifPresent(list -> list.stream()
				.filter(stack -> !stack.isEmpty()).forEach(stash::add));
	}

	// ---------------------------------------------------------------- behavior

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (!statsApplied) {
			applyStats();
		}

		int emerging = entityData.get(EMERGING);
		if (emerging > 0) {
			entityData.set(EMERGING, emerging - 1);
			getNavigation().stop();
			if (emerging % 4 == 0) {
				level.sendParticles(JjkParticles.of(JjkParticles.CE_AURA, 0x2A1838, 1.6f), getX(), getY() + 0.1, getZ(), 6,
						getBbWidth() * 0.6, 0.1, getBbWidth() * 0.6, 0.02);
			}

			return;
		}

		mechanic().tick(this, level);
		brain.tick(level);
		entityData.set(STATE, brain.state().ordinal());
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		if (!(target instanceof LivingEntity living) || !mechanic().canAttack(this)) {
			return false;
		}

		AttributeInstance damage = getAttribute(Attributes.ATTACK_DAMAGE);
		double base = damage == null ? 0 : damage.getBaseValue();
		double multiplier = mechanic().attackMultiplier(this, living);
		if (damage != null) {
			damage.setBaseValue(base * multiplier);
		}

		boolean hit = super.doHurtTarget(level, target);
		if (damage != null) {
			damage.setBaseValue(base);
		}

		triggerAnim("action", "attack");
		if (hit) {
			double drain = definition().map(d -> d.stats().drainOnHit()).orElse(4.0);
			double taken = CursedEnergy.drain(living, drain);
			CursedEnergy.restore(this, taken);
			mechanic().onAttack(this, level, living);
		}

		return hit;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		// A blow the curse is immune to must not wake its mechanic either (a hidden curse would be revealed).
		if (isInvulnerableTo(level, source) || isDeadOrDying()) {
			return false;
		}

		float scaled = amount;
		Optional<CurseDefinition> def = definition();
		if (def.isPresent()) {
			Identifier type = source.typeHolder().unwrapKey().map(ResourceKey::identifier).orElse(null);
			if (type != null && def.get().resistances().containsKey(type)) {
				scaled *= def.get().resistances().get(type);
			}
		}

		scaled = mechanic().onHurt(this, level, source, scaled);
		boolean hurt = super.hurtServer(level, source, scaled);
		if (hurt && isAlive()) {
			triggerAnim("action", "hurt");
			if (source.getEntity() instanceof LivingEntity attacker) {
				brain.alertPack(attacker);
			}
		}

		return hurt;
	}

	@Override
	public void die(DamageSource source) {
		super.die(source);
		if (level() instanceof ServerLevel server) {
			mechanic().onDeath(this, server, source);
		}
	}

	@Override
	public void onRemoval(RemovalReason reason) {
		super.onRemoval(reason);
		if (!level().isClientSide()) {
			mechanic().onRemoved(this);
		}
	}

	@Override
	protected void tickDeath() {
		deathTime++;
		if (level().isClientSide()) {
			return;
		}

		if (deathTime >= DEATH_TICKS && !isRemoved()) {
			if (level() instanceof ServerLevel server) {
				int color = definition().map(d -> d.codex().color()).orElse(0x8F5CFF);
				server.sendParticles(JjkParticles.of(JjkParticles.DISSOLVE, color, 1.4f), getX(), getY() + getBbHeight() / 2, getZ(),
						30, getBbWidth() / 2, getBbHeight() / 2, getBbWidth() / 2, 0.05);
			}

			level().broadcastEntityEvent(this, (byte) 60);
			remove(RemovalReason.KILLED);
		}
	}

	@Override
	public boolean removeWhenFarAway(double distSqr) {
		return entry().map(e -> e.definition().spawn().isPresent()).orElse(true) && super.removeWhenFarAway(distSqr);
	}

	// ---------------------------------------------------------------- animation

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<>("base", 4, test -> {
			if (isDeadOrDying()) {
				return test.setAndContinue(DEATH);
			}

			if (isEmerging()) {
				return test.setAndContinue(SPAWN);
			}

			return test.setAndContinue(test.isMoving() ? WALK : IDLE);
		}));
		controllers.add(new AnimationController<>("action", 2, test -> PlayState.STOP)
				.triggerableAnim("attack", RawAnimation.begin().thenPlay("attack"))
				.triggerableAnim("cast", RawAnimation.begin().thenPlay("cast"))
				.triggerableAnim("hurt", RawAnimation.begin().thenPlay("hurt")));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return animationCache;
	}
}
