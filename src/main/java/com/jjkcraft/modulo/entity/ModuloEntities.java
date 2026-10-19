package com.jjkcraft.modulo.entity;

import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;

import com.jjkcraft.JjkCraft;
import com.jjkcraft.entity.JjkEntities;
import com.jjkcraft.modulo.data.BossDefinition;
import com.jjkcraft.modulo.data.ModuloRegistries;

/** The Modulo expansion's entities. None is saved: missions bring them where they are needed. */
public final class ModuloEntities {
	public static final EntityType<ModuloNpcEntity> NPC = JjkEntities.register("modulo_npc",
			EntityType.Builder.<ModuloNpcEntity>of(ModuloNpcEntity::new, MobCategory.MISC).sized(0.6f, 1.8f).clientTrackingRange(10).noSave().noSummon().noLootTable());
	public static final EntityType<ModuloBossEntity> BOSS = JjkEntities.register("modulo_boss",
			EntityType.Builder.<ModuloBossEntity>of(ModuloBossEntity::new, MobCategory.MONSTER).sized(0.6f, 1.95f).clientTrackingRange(10).noSave().noSummon().noLootTable());
	public static final EntityType<ToranosukeEntity> TORANOSUKE = JjkEntities.register("toranosuke",
			EntityType.Builder.<ToranosukeEntity>of(ToranosukeEntity::new, MobCategory.MONSTER).sized(1.2f, 1.2f).clientTrackingRange(10).noSave().noSummon().noLootTable());
	public static final EntityType<MahoragaEntity> MAHORAGA = JjkEntities.register("mahoraga",
			EntityType.Builder.<MahoragaEntity>of(MahoragaEntity::new, MobCategory.MISC).sized(0.7f, 2.0f).clientTrackingRange(12).noSave().noSummon().noLootTable());
	public static final EntityType<WanchaOctopusEntity> WANCHA_OCTOPUS = JjkEntities.register("wancha_octopus",
			EntityType.Builder.<WanchaOctopusEntity>of(WanchaOctopusEntity::new, MobCategory.WATER_CREATURE).sized(0.8f, 0.8f).clientTrackingRange(8).noSave().noLootTable());

	private ModuloEntities() {
	}

	public static void init() {
		FabricDefaultAttributeRegistry.register(NPC, ModuloNpcEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(BOSS, ModuloBossEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(TORANOSUKE, ModuloBossEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(MAHORAGA, MahoragaEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(WANCHA_OCTOPUS, WanchaOctopusEntity.createAttributes());
	}

	/** Spawns a boss from its data at {@code pos} for a mission stage. Null if the id is unknown. */
	public static @Nullable ModuloBossEntity spawnBoss(ServerLevel level, Identifier bossId, BlockPos pos, @Nullable Identifier site, @Nullable Identifier mission,
			int stage, int fighters) {
		Optional<BossDefinition> definition = ModuloRegistries.boss(level.registryAccess(), bossId);
		if (definition.isEmpty()) {
			JjkCraft.LOGGER.warn("Unknown Modulo boss {}", bossId);
			return null;
		}

		EntityType<? extends ModuloBossEntity> type = definition.get().behavior().equals("toranosuke") ? TORANOSUKE : BOSS;
		ModuloBossEntity boss = type.create(level, EntitySpawnReason.EVENT);
		if (boss == null) {
			return null;
		}

		boss.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.getRandom().nextFloat() * 360f, 0);
		level.addFreshEntity(boss);
		boss.setup(bossId, definition.get(), site, mission, stage, fighters);
		return boss;
	}

	/** Spawns a character at {@code pos}. */
	public static @Nullable ModuloNpcEntity spawnNpc(ServerLevel level, String character, ModuloNpcEntity.Role role, @Nullable UUID owner, @Nullable Identifier site,
			BlockPos pos) {
		ModuloNpcEntity npc = NPC.create(level, EntitySpawnReason.EVENT);
		if (npc == null) {
			return null;
		}

		npc.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.getRandom().nextFloat() * 360f, 0);
		npc.setup(character, role, owner, site, pos);
		level.addFreshEntity(npc);
		return npc;
	}
}
