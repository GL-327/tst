package com.jjkcraft.curse;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;

import com.jjkcraft.entity.JjkEntities;

/**
 * Curse entity types. Each curse needs its own type (for its model and hitbox); its numbers live in
 * curse data. Default attributes here are placeholders that data overrides on spawn.
 */
public final class CurseEntities {
	public static final List<EntityType<? extends CurseEntity>> ALL = new ArrayList<>();

	// Grade 4
	public static final EntityType<FlyingCurseEntity> HOLLOW_HYMNAL = flying("hollow_hymnal", 0.9f, 1.8f);
	public static final EntityType<CurseEntity> LEDGER_CRAWLER = ground("ledger_crawler", 1.0f, 0.8f);
	public static final EntityType<FlyingCurseEntity> WICK_MOTH = flying("wick_moth", 0.8f, 0.7f);
	// Grade 3
	public static final EntityType<CurseEntity> PALE_STILT = ground("pale_stilt", 0.7f, 3.4f);
	public static final EntityType<CurseEntity> ECHO_MIMIC = ground("echo_mimic", 0.6f, 2.0f);
	public static final EntityType<CurseEntity> SALT_LANTERN = ground("salt_lantern", 0.7f, 1.6f);
	// Grade 2
	public static final EntityType<CurseEntity> TIDEWASH = ground("tidewash", 1.2f, 1.4f);
	public static final EntityType<CurseEntity> THORN_MATRIARCH = ground("thorn_matriarch", 0.9f, 2.0f);
	public static final EntityType<FlyingCurseEntity> CRACKED_BELL = flying("cracked_bell", 1.2f, 1.9f);
	// Grade 1
	public static final EntityType<CurseEntity> REDACTED_SAINT = ground("redacted_saint", 0.6f, 1.95f);
	public static final EntityType<CurseEntity> ASH_ONI = ground("ash_oni", 1.0f, 2.8f);
	public static final EntityType<CurseEntity> GRAVEBOUND_GATE = ground("gravebound_gate", 2.4f, 2.6f);
	public static final EntityType<CurseEntity> VOW_KIN = ground("vow_kin", 0.7f, 2.2f);
	// Special Grade
	public static final EntityType<CurseEntity> PAPER_TIDE_GRAVEMIND = ground("paper_tide_gravemind", 1.4f, 3.0f);
	public static final EntityType<CurseEntity> UNFINISHED_FERRYMAN = ground("unfinished_ferryman", 1.0f, 3.2f);
	public static final EntityType<CurseEntity> CULLING_WARDEN = ground("culling_warden", 1.2f, 3.4f);
	public static final EntityType<CurseEntity> TEN_MOUTH_SOVEREIGN = ground("ten_mouth_sovereign", 2.0f, 4.0f);
	public static final EntityType<CurseEntity> DOORFRAME_HUNTER = ground("doorframe_hunter", 0.6f, 2.2f);
	public static final EntityType<CurseEntity> GILDED_MIMIC = ground("gilded_mimic", 0.9f, 0.95f);
	public static final EntityType<CurseEntity> TITHE_BURROWER = ground("tithe_burrower", 0.9f, 0.8f);

	private CurseEntities() {
	}

	public static void init() {
		AttributeSupplier.Builder defaults = Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 20)
				.add(Attributes.ATTACK_DAMAGE, 3)
				.add(Attributes.MOVEMENT_SPEED, 0.25)
				.add(Attributes.FOLLOW_RANGE, 24)
				.add(Attributes.ARMOR, 0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0)
				.add(Attributes.FLYING_SPEED, 0.5);
		for (EntityType<? extends CurseEntity> type : ALL) {
			FabricDefaultAttributeRegistry.register(type, defaults);
		}
	}

	private static EntityType<CurseEntity> ground(String name, float width, float height) {
		EntityType<CurseEntity> type = JjkEntities.register(name,
				EntityType.Builder.<CurseEntity>of(CurseEntity::new, MobCategory.MONSTER).sized(width, height).clientTrackingRange(10));
		ALL.add(type);
		return type;
	}

	private static EntityType<FlyingCurseEntity> flying(String name, float width, float height) {
		EntityType<FlyingCurseEntity> type = JjkEntities.register(name,
				EntityType.Builder.<FlyingCurseEntity>of(FlyingCurseEntity::new, MobCategory.MONSTER).sized(width, height).clientTrackingRange(10));
		ALL.add(type);
		return type;
	}
}
