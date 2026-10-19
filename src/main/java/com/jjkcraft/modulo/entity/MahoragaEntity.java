package com.jjkcraft.modulo.entity;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import com.jjkcraft.core.modulo.ModuloMath;
import com.jjkcraft.damage.TechniqueDamageSource;
import com.jjkcraft.modulo.Modulo;
import com.jjkcraft.modulo.ModuloPayloads;
import com.jjkcraft.modulo.data.BossDefinition;
import com.jjkcraft.modulo.data.ModuloRegistries;

/**
 * Eight-Handled Sword Divergent Sila Divine General Mahoraga, Yuka Okkotsu's shikigami in mission 8
 * (chapters 15 to 21). Its adaptation is data ({@code modulo_boss/mahoraga.json}, field
 * {@code adaptation}): the first time a kind of damage hurts it, it gains resistance to that kind
 * for the rest of the fight, and more with each further hit; its wheel turns at each step. A kind
 * is the technique that dealt the damage, or the damage type for anything else. Original model:
 * a tall shadowed figure under an eight-spoked wheel.
 */
public class MahoragaEntity extends PathfinderMob {
	public static final Identifier DATA = Identifier.fromNamespaceAndPath(Modulo.NAMESPACE, "mahoraga");

	private static final EntityDataAccessor<Integer> WHEEL = SynchedEntityData.defineId(MahoragaEntity.class, EntityDataSerializers.INT);

	private final Map<String, Integer> hits = new HashMap<>();
	private BossDefinition.@Nullable Adaptation adaptation;

	public MahoragaEntity(EntityType<? extends PathfinderMob> type, Level level) {
		super(type, level);
		setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 400)
				.add(Attributes.ATTACK_DAMAGE, 14)
				.add(Attributes.MOVEMENT_SPEED, 0.32)
				.add(Attributes.FOLLOW_RANGE, 48)
				.add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
				.add(Attributes.ARMOR, 12)
				.add(Attributes.SCALE, 1.6);
	}

	/** Reads its numbers from data. */
	public void setup(ServerLevel level) {
		Optional<BossDefinition> data = ModuloRegistries.boss(level.registryAccess(), DATA);
		adaptation = data.flatMap(BossDefinition::adaptation).orElse(null);
		data.ifPresent(d -> {
			getAttribute(Attributes.MAX_HEALTH).setBaseValue(d.health());
			getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(d.damage());
			getAttribute(Attributes.ARMOR).setBaseValue(d.armor());
			getAttribute(Attributes.SCALE).setBaseValue(d.scale());
			setCustomName(Component.translatable(d.name()));
		});
		setHealth(getMaxHealth());
	}

	/** How many times the wheel has turned (one turn per adaptation step). */
	public int wheel() {
		return entityData.get(WHEEL);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(WHEEL, 0);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, true));
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
		// Yuka's shikigami: players are on its side in this fight.
		return source.getEntity() instanceof Player || super.isInvulnerableTo(level, source);
	}

	/** The kind of damage a source is, for adaptation. */
	public String kind(DamageSource source) {
		if (adaptation != null && adaptation.key().equals("technique") && source instanceof TechniqueDamageSource technique && technique.technique() != null) {
			return technique.technique().toString();
		}

		return source.typeHolder().unwrapKey().map(k -> k.identifier().toString()).orElse("unknown");
	}

	/** Resistance to a kind right now, 0 to 1. */
	public double resistance(String kind) {
		if (adaptation == null) {
			return 0;
		}

		return ModuloMath.adaptation(hits.getOrDefault(kind, 0), adaptation.first(), adaptation.perHit(), adaptation.max());
	}

	public boolean fullyAdapted(String kind) {
		return adaptation != null && resistance(kind) >= adaptation.fullAdaptation() - 1e-9;
	}

	/** Applies its current resistance to a blow, then adapts to it. Called before vanilla handles the damage. */
	public float adapt(ServerLevel level, DamageSource source, float amount) {
		if (adaptation == null || amount <= 0) {
			return amount;
		}

		String kind = kind(source);
		float reduced = (float) (amount * (1 - resistance(kind)));
		hits.merge(kind, 1, Integer::sum);
		entityData.set(WHEEL, wheel() + 1);
		level.playSound(null, getX(), getY() + 3, getZ(), SoundEvents.BELL_RESONATE, SoundSource.HOSTILE, 0.8f, 0.6f + 0.1f * Math.min(6, hits.get(kind)));
		level.sendParticles(ParticleTypes.END_ROD, getX(), getY() + getBbHeight() + 0.6, getZ(), 8, 0.6, 0.05, 0.6, 0.02);
		for (ServerPlayer player : level.players()) {
			if (player.distanceToSqr(this) < 64 * 64) {
				Modulo.send(player, new ModuloPayloads.Event("adapted", java.util.List.of(Integer.toString(getId()), Integer.toString(wheel()), kind)));
			}
		}

		return reduced;
	}
}
