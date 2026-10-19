package com.jjkcraft.effect;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import com.jjkcraft.JjkCraft;

/** Status effects used by techniques and domains. */
public final class JjkEffects {
	/** Immobilized by cursed chains or a command: cannot move or jump. */
	public static final Holder<MobEffect> BOUND = register("bound", new JjkEffect(MobEffectCategory.HARMFUL, 0x6B4E2E)
			.addAttributeModifier(Attributes.MOVEMENT_SPEED, JjkCraft.id("effect.bound.speed"), -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
			.addAttributeModifier(Attributes.JUMP_STRENGTH, JjkCraft.id("effect.bound.jump"), -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

	/** Stunned (domain backlash, lost clash): bound and unable to cast. */
	public static final Holder<MobEffect> STUNNED = register("stunned", new JjkEffect(MobEffectCategory.HARMFUL, 0x9A93A8)
			.addAttributeModifier(Attributes.MOVEMENT_SPEED, JjkCraft.id("effect.stunned.speed"), -0.85, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
			.addAttributeModifier(Attributes.JUMP_STRENGTH, JjkCraft.id("effect.stunned.jump"), -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
			.addAttributeModifier(Attributes.ATTACK_DAMAGE, JjkCraft.id("effect.stunned.attack"), -0.5, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

	/**
	 * Overloaded by an information-flood domain: the mind drowns in input. Slowed, can't cast, and
	 * the client floods the screen and HUD while it lasts.
	 */
	public static final Holder<MobEffect> OVERLOADED = register("overloaded", new JjkEffect(MobEffectCategory.HARMFUL, 0x9FD8E8)
			.addAttributeModifier(Attributes.MOVEMENT_SPEED, JjkCraft.id("effect.overloaded.speed"), -0.6, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
			.addAttributeModifier(Attributes.ATTACK_SPEED, JjkCraft.id("effect.overloaded.attack_speed"), -0.5, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

	/** Idle Transfiguration on oneself: bigger and harder to move or hurt, but slower. */
	public static final Holder<MobEffect> BULKED = register("bulked", new JjkEffect(MobEffectCategory.NEUTRAL, 0x8A5A4A)
			.addAttributeModifier(Attributes.SCALE, JjkCraft.id("effect.bulked.scale"), 0.3, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
			.addAttributeModifier(Attributes.ARMOR, JjkCraft.id("effect.bulked.armor"), 6, AttributeModifier.Operation.ADD_VALUE)
			.addAttributeModifier(Attributes.KNOCKBACK_RESISTANCE, JjkCraft.id("effect.bulked.knockback"), 0.5, AttributeModifier.Operation.ADD_VALUE)
			.addAttributeModifier(Attributes.MOVEMENT_SPEED, JjkCraft.id("effect.bulked.speed"), -0.25, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

	/** Idle Transfiguration on another: shrunk, quick and feeble. */
	public static final Holder<MobEffect> SHRUNK = register("shrunk", new JjkEffect(MobEffectCategory.HARMFUL, 0xC09A8A)
			.addAttributeModifier(Attributes.SCALE, JjkCraft.id("effect.shrunk.scale"), -0.45, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
			.addAttributeModifier(Attributes.ATTACK_DAMAGE, JjkCraft.id("effect.shrunk.attack"), -0.4, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
			.addAttributeModifier(Attributes.MOVEMENT_SPEED, JjkCraft.id("effect.shrunk.speed"), 0.2, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

	/** Projection Sorcery: caught in a frame. Frozen in place, unable to act. */
	public static final Holder<MobEffect> FROZEN_FRAME = register("frozen_frame", new JjkEffect(MobEffectCategory.HARMFUL, 0xD0D0E0)
			.addAttributeModifier(Attributes.MOVEMENT_SPEED, JjkCraft.id("effect.frozen_frame.speed"), -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
			.addAttributeModifier(Attributes.JUMP_STRENGTH, JjkCraft.id("effect.frozen_frame.jump"), -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
			.addAttributeModifier(Attributes.ATTACK_SPEED, JjkCraft.id("effect.frozen_frame.attack_speed"), -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

	/** A cursed object was swallowed: cursed energy runs thin and wild (max CE lower while it lasts). */
	public static final Holder<MobEffect> UNSETTLED = register("unsettled", new JjkEffect(MobEffectCategory.HARMFUL, 0x3A1F2E));

	private JjkEffects() {
	}

	public static void init() {
		// Loads the class so the static fields register.
	}

	private static Holder<MobEffect> register(String name, MobEffect effect) {
		return Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, JjkCraft.id(name), effect);
	}

	/** MobEffect's constructor is protected; this subclass exposes it. */
	static final class JjkEffect extends MobEffect {
		JjkEffect(MobEffectCategory category, int color) {
			super(category, color);
		}
	}
}
