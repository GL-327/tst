package com.jjkcraft.entity;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;

import com.jjkcraft.JjkCraft;

/** Entity types. Curses are registered by {@code CurseEntities}. */
public final class JjkEntities {
	public static final EntityType<com.jjkcraft.entity.shikigami.TransfiguredSoulEntity> TRANSFIGURED_SOUL = register("transfigured_soul",
			EntityType.Builder.<com.jjkcraft.entity.shikigami.TransfiguredSoulEntity>of(com.jjkcraft.entity.shikigami.TransfiguredSoulEntity::new,MobCategory.MISC)
					.sized(0.65f,1.55f).clientTrackingRange(10).noSave().noSummon().noLootTable());
	public static final EntityType<CursedBoltEntity> CURSED_BOLT = register("cursed_bolt",
			EntityType.Builder.<CursedBoltEntity>of(CursedBoltEntity::new, MobCategory.MISC)
					.sized(0.4f, 0.4f).clientTrackingRange(8).updateInterval(1).noSave().noSummon().noLootTable());

	/** Chimera Shadow Garden's shadow Divine Dogs: transient, never saved, never summoned by command. */
	public static final EntityType<ShadowDivineDogEntity> SHADOW_DIVINE_DOG = register("shadow_divine_dog",
			EntityType.Builder.<ShadowDivineDogEntity>of(ShadowDivineDogEntity::new, MobCategory.MISC)
					.sized(0.8f, 0.9f).clientTrackingRange(10).noSave().noSummon().noLootTable());

	/** Ten Shadows shikigami (Divine Dogs, Nue, Toad): summons, never saved. */
	public static final EntityType<com.jjkcraft.entity.shikigami.DivineDogEntity> DIVINE_DOG = register("divine_dog",
			EntityType.Builder.<com.jjkcraft.entity.shikigami.DivineDogEntity>of(com.jjkcraft.entity.shikigami.DivineDogEntity::new, MobCategory.MISC)
					.sized(0.7f, 0.85f).clientTrackingRange(10).noSave().noSummon().noLootTable());
	public static final EntityType<com.jjkcraft.entity.shikigami.NueEntity> NUE = register("nue",
			EntityType.Builder.<com.jjkcraft.entity.shikigami.NueEntity>of(com.jjkcraft.entity.shikigami.NueEntity::new, MobCategory.MISC)
					.sized(0.6f, 0.5f).clientTrackingRange(10).noSave().noSummon().noLootTable());
	public static final EntityType<com.jjkcraft.entity.shikigami.ToadEntity> TOAD = register("toad",
			EntityType.Builder.<com.jjkcraft.entity.shikigami.ToadEntity>of(com.jjkcraft.entity.shikigami.ToadEntity::new, MobCategory.MISC)
					.sized(1.2f, 0.9f).clientTrackingRange(10).noSave().noSummon().noLootTable());

	private JjkEntities() {
	}

	public static void init() {
		com.jjkcraft.entity.shikigami.ShadowBeasts.init();
		FabricDefaultAttributeRegistry.register(TRANSFIGURED_SOUL,com.jjkcraft.entity.shikigami.TransfiguredSoulEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(SHADOW_DIVINE_DOG, ShadowDivineDogEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(DIVINE_DOG, com.jjkcraft.entity.shikigami.DivineDogEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(NUE, com.jjkcraft.entity.shikigami.NueEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(TOAD, com.jjkcraft.entity.shikigami.ToadEntity.createAttributes());
	}

	public static <T extends Entity> EntityType<T> register(String name, EntityType.Builder<T> builder) {
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, JjkCraft.id(name));
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
	}
}
