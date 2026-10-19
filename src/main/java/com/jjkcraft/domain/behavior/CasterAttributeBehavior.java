package com.jjkcraft.domain.behavior;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import com.jjkcraft.JjkCraft;
import com.jjkcraft.domain.ActiveDomain;

/**
 * Boosts one of the caster's attributes while they stand in their own domain (for example the
 * cutting field's melee speed).
 *
 * @param attribute any attribute id
 * @param amount    modifier amount at power 1
 * @param operation {@code add_value}, {@code add_multiplied_base} or {@code add_multiplied_total}
 */
public record CasterAttributeBehavior(Holder<Attribute> attribute, double amount, AttributeModifier.Operation operation) implements DomainBehavior {
	public static final MapCodec<CasterAttributeBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			BuiltInRegistries.ATTRIBUTE.holderByNameCodec().fieldOf("attribute").forGetter(CasterAttributeBehavior::attribute),
			Codec.doubleRange(-10, 10).fieldOf("amount").forGetter(CasterAttributeBehavior::amount),
			AttributeModifier.Operation.CODEC.optionalFieldOf("operation", AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
					.forGetter(CasterAttributeBehavior::operation)
	).apply(i, CasterAttributeBehavior::new));

	@Override
	public DomainBehaviorType<?> type() {
		return DomainBehaviors.CASTER_ATTRIBUTE;
	}

	private Identifier modifierId() {
		return JjkCraft.id("domain/" + attribute.unwrapKey().map(k -> k.identifier().getPath()).orElse("attribute"));
	}

	@Override
	public void open(ActiveDomain domain) {
		tick(domain);
	}

	@Override
	public void tick(ActiveDomain domain) {
		AttributeInstance instance = domain.caster().getAttribute(attribute);
		if (instance == null) {
			return;
		}

		if (domain.contains(domain.caster())) {
			instance.addOrUpdateTransientModifier(new AttributeModifier(modifierId(), amount * domain.power(), operation));
		} else {
			instance.removeModifier(modifierId());
		}
	}

	@Override
	public void close(ActiveDomain domain) {
		AttributeInstance instance = domain.caster().getAttribute(attribute);
		if (instance != null) {
			instance.removeModifier(modifierId());
		}
	}
}
