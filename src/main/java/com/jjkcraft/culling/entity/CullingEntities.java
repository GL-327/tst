package com.jjkcraft.culling.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;

import com.jjkcraft.entity.JjkEntities;

/** Culling Game entities: entrants (saved with the world) and the Kogane (raised on demand). */
public final class CullingEntities {
	public static final EntityType<EntrantEntity> ENTRANT = JjkEntities.register("entrant",
			EntityType.Builder.<EntrantEntity>of(EntrantEntity::new, MobCategory.MISC)
					.sized(0.6f, 1.8f).eyeHeight(1.62f).clientTrackingRange(10).noLootTable());

	public static final EntityType<KoganeEntity> KOGANE = JjkEntities.register("kogane",
			EntityType.Builder.<KoganeEntity>of(KoganeEntity::new, MobCategory.MISC)
					.sized(0.9f, 1.6f).clientTrackingRange(10).noSave().noSummon().noLootTable());

	private CullingEntities() {
	}

	public static void init() {
		FabricDefaultAttributeRegistry.register(ENTRANT, EntrantEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(KOGANE, KoganeEntity.createAttributes());
	}
}
