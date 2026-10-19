package com.jjkcraft.domain.behavior;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import com.jjkcraft.domain.ActiveDomain;
import com.jjkcraft.domain.DomainService;
import com.jjkcraft.entity.JjkEntities;
import com.jjkcraft.entity.ShadowDivineDogEntity;
import com.jjkcraft.particle.JjkParticles;

/**
 * Chimera Shadow Garden: shadow Divine Dogs rise inside the domain and hunt its targets. Fallen dogs are
 * replaced on a timer; all of them melt when the domain ends.
 *
 * @param count           hounds kept alive at once
 * @param respawnInterval ticks between replacing a fallen hound
 * @param damage          hound bite damage at power 1
 * @param health          hound health
 */
public record SummonHuntersBehavior(int count, int respawnInterval, double damage, double health) implements DomainBehavior {
	public static final MapCodec<SummonHuntersBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.intRange(1, 16).fieldOf("count").forGetter(SummonHuntersBehavior::count),
			Codec.intRange(1, 1200).optionalFieldOf("respawn_interval", 60).forGetter(SummonHuntersBehavior::respawnInterval),
			Codec.doubleRange(0, 100).optionalFieldOf("damage", 4.0).forGetter(SummonHuntersBehavior::damage),
			Codec.doubleRange(1, 200).optionalFieldOf("health", 14.0).forGetter(SummonHuntersBehavior::health)
	).apply(i, SummonHuntersBehavior::new));

	private static final String KEY = "hunters";

	@Override
	public DomainBehaviorType<?> type() {
		return DomainBehaviors.SUMMON_HUNTERS;
	}

	@SuppressWarnings("unchecked")
	private static List<UUID> hounds(ActiveDomain domain) {
		return (List<UUID>) domain.memory().computeIfAbsent(KEY, k -> new ArrayList<UUID>());
	}

	@Override
	public void open(ActiveDomain domain) {
		for (int i = 0; i < count; i++) {
			spawn(domain);
		}
	}

	@Override
	public void tick(ActiveDomain domain) {
		List<UUID> hounds = hounds(domain);
		hounds.removeIf(id -> !(domain.level().getEntity(id) instanceof ShadowDivineDogEntity h) || !h.isAlive());
		if (hounds.size() < count && domain.activeTicks() % respawnInterval == 0) {
			spawn(domain);
		}
	}

	@Override
	public void close(ActiveDomain domain) {
		for (UUID id : hounds(domain)) {
			if (domain.level().getEntity(id) instanceof ShadowDivineDogEntity hound) {
				hound.dissolve(domain.level());
			}
		}

		hounds(domain).clear();
	}

	private void spawn(ActiveDomain domain) {
		ShadowDivineDogEntity hound = JjkEntities.SHADOW_DIVINE_DOG.create(domain.level(), EntitySpawnReason.MOB_SUMMONED);
		if (hound == null) {
			return;
		}

		var random = domain.level().getRandom();
		double angle = random.nextDouble() * Math.PI * 2;
		double distance = domain.radius() * (0.3 + random.nextDouble() * 0.4);
		Vec3 at = domain.center().add(Math.cos(angle) * distance, 0, Math.sin(angle) * distance);
		BlockPos column = BlockPos.containing(at);
		int ground = domain.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column.getX(), column.getZ());
		double y = Math.abs(ground - domain.center().y) < domain.radius() ? ground : domain.center().y;
		hound.snapTo(at.x, y, at.z, random.nextFloat() * 360f, 0);
		hound.bind(domain.caster(), domain.id());
		setBase(hound, Attributes.MAX_HEALTH, health);
		setBase(hound, Attributes.ATTACK_DAMAGE, damage * domain.power());
		hound.setHealth(hound.getMaxHealth());
		domain.level().addFreshEntity(hound);
		hounds(domain).add(hound.getUUID());
		domain.level().sendParticles(JjkParticles.of(JjkParticles.CE_AURA, 0x1A1424, 1.4f), at.x, y + 0.2, at.z, 14, 0.4, 0.1, 0.4, 0.02);
		domain.event(DomainService.Event.PULSE, hounds(domain).size());
	}

	private static void setBase(ShadowDivineDogEntity hound, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute, double value) {
		AttributeInstance instance = hound.getAttribute(attribute);
		if (instance != null) {
			instance.setBaseValue(value);
		}
	}
}
