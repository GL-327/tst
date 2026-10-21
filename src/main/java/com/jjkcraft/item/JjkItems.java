package com.jjkcraft.item;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.block.Block;

import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;

import com.jjkcraft.JjkCraft;
import com.jjkcraft.block.JjkBlocks;
import com.jjkcraft.curse.CurseEntities;
import com.jjkcraft.curse.CurseEntity;

/** Items: CE shards, curse trophies, spawn eggs and block items, plus the creative tab. */
public final class JjkItems {
	/** Every item in creative-tab order. */
	public static final List<Item> ALL = new ArrayList<>();
	/** Curse trophy items by curse id path, for loot and the codex. */
	public static final Map<String, Item> TROPHIES = new LinkedHashMap<>();
	public static final Map<EntityType<?>, Item> SPAWN_EGGS = new LinkedHashMap<>();

	public static final Item CURSED_ENERGY_SHARD = register("cursed_energy_shard", CursedEnergyShardItem::new, new Item.Properties().stacksTo(32));
	public static final Item DIVINE_CHALICE=register("divine_chalice",DivineChaliceItem::new,new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));

	// One trophy per curse: crafting material for cursed tools later.
	public static final Item HYMN_REED = trophy("hymn_reed", "hollow_hymnal", Rarity.COMMON);
	public static final Item TALLY_TOKEN = trophy("tally_token", "ledger_crawler", Rarity.COMMON);
	public static final Item EMBER_WAX = trophy("ember_wax", "wick_moth", Rarity.COMMON);
	public static final Item STILT_MARROW = trophy("stilt_marrow", "pale_stilt", Rarity.UNCOMMON);
	public static final Item ECHO_SHARD = trophy("echo_shard", "echo_mimic", Rarity.UNCOMMON);
	public static final Item SALT_CRYSTAL = trophy("salt_crystal", "salt_lantern", Rarity.UNCOMMON);
	public static final Item TIDE_PEARL = trophy("tide_pearl", "tidewash", Rarity.RARE);
	public static final Item THORN_HEART = trophy("thorn_heart", "thorn_matriarch", Rarity.RARE);
	public static final Item BELL_FRAGMENT = trophy("bell_fragment", "cracked_bell", Rarity.RARE);
	public static final Item SAINT_VEIL = trophy("saint_veil", "redacted_saint", Rarity.EPIC);
	public static final Item ONI_HORN = trophy("oni_horn", "ash_oni", Rarity.EPIC);
	public static final Item GATE_SEAL = trophy("gate_seal", "gravebound_gate", Rarity.EPIC);
	public static final Item BROKEN_KNOT = trophy("broken_knot", "vow_kin", Rarity.EPIC);
	public static final Item FOLDED_CHARM = trophy("folded_charm", "paper_tide_gravemind", Rarity.EPIC);
	public static final Item FERRY_COIN = trophy("ferry_coin", "unfinished_ferryman", Rarity.EPIC);
	public static final Item EDICT_TABLET = trophy("edict_tablet", "culling_warden", Rarity.EPIC);
	public static final Item SOVEREIGN_TOOTH = trophy("sovereign_tooth", "ten_mouth_sovereign", Rarity.EPIC);
	public static final Item HINGE_SPLINTER = trophy("hinge_splinter", "doorframe_hunter", Rarity.COMMON);
	public static final Item GILDED_TONGUE = trophy("gilded_tongue", "gilded_mimic", Rarity.UNCOMMON);
	public static final Item TITHE_PURSE = trophy("tithe_purse", "tithe_burrower", Rarity.RARE);

	public static final Item CURSE_NEST = registerBlock(JjkBlocks.CURSE_NEST);

	static {
		for (EntityType<? extends CurseEntity> type : CurseEntities.ALL) {
			String name = BuiltInRegistries.ENTITY_TYPE.getKey(type).getPath() + "_spawn_egg";
			SPAWN_EGGS.put(type, register(name, SpawnEggItem::new, new Item.Properties().spawnEgg(type)));
		}
	}

	/** Forged by an arsenal domain; never in the creative tab (useless without its domain). */
	public static final Item ARSENAL_BLADE = registerHidden("arsenal_blade", ArsenalBladeItem::new, new Item.Properties()
			.stacksTo(1).rarity(Rarity.EPIC).attributes(ItemAttributeModifiers.builder()
					.add(Attributes.ATTACK_DAMAGE, new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, 6, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
					.add(Attributes.ATTACK_SPEED, new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, -2.4, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
					.build()));

	// ---- cursed tools and objects (their numbers are data: jjkcraft/cursed_tool)
	public static final Item SPLIT_SOUL_KATANA = register("split_soul_katana", com.jjkcraft.item.tool.CursedToolItem::new,
			new Item.Properties().sword(ToolMaterial.NETHERITE, 5f, -2.7f).rarity(Rarity.EPIC));
	public static final Item CHAIN_OF_A_THOUSAND_MILES = register("chain_of_a_thousand_miles", com.jjkcraft.item.tool.CursedToolItem::new,
			new Item.Properties().sword(ToolMaterial.IRON, 3f, -2.2f).rarity(Rarity.RARE).attributes(ItemAttributeModifiers.builder()
					.add(Attributes.ATTACK_DAMAGE, new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, 5, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
					.add(Attributes.ATTACK_SPEED, new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, -2.2, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
					.add(Attributes.ENTITY_INTERACTION_RANGE, new AttributeModifier(JjkCraft.id("chain_of_a_thousand_miles_reach"), 2, AttributeModifier.Operation.ADD_VALUE),
							EquipmentSlotGroup.MAINHAND)
					.build()));
	public static final Item INVERTED_SPEAR_OF_HEAVEN = register("inverted_spear_of_heaven", com.jjkcraft.item.tool.CursedToolItem::new,
			new Item.Properties().sword(ToolMaterial.DIAMOND, 3f, -2.6f).rarity(Rarity.EPIC));
	public static final Item DEBT_CLEAVER = register("debt_cleaver", com.jjkcraft.item.tool.CursedToolItem::new,
			new Item.Properties().axe(ToolMaterial.IRON, 6f, -3.1f).rarity(Rarity.UNCOMMON));
	public static final Item BONEWOOD_STAFF = register("bonewood_staff", com.jjkcraft.item.tool.CursedToolItem::new,
			new Item.Properties().sword(ToolMaterial.WOOD, 1f, -2.4f).rarity(Rarity.UNCOMMON));
	public static final Item WITHERED_DIGIT = register("withered_digit", CursedFingerItem::new,
			new Item.Properties().stacksTo(16).rarity(Rarity.EPIC).food(new net.minecraft.world.food.FoodProperties(0, 0f, true)));
	public static final Item DRIFTING_GOURD = register("drifting_gourd", DriftingGourdItem::new, new Item.Properties().stacksTo(1).rarity(Rarity.RARE));
	public static final Item TALLY_LEDGER = register("tally_ledger", TallyLedgerItem::new, new Item.Properties().stacksTo(1).rarity(Rarity.RARE));
	/** Made by Straw Doll Technique: Resonance; useless without a link, so not in the creative tab. */
	public static final Item STRAW_DOLL = registerHidden("straw_doll", StrawDollItem::new, new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
	/** One per technique, filled into the creative tab from the technique registry. */
	public static final Item TECHNIQUE_SCROLL = registerHidden("technique_scroll", TechniqueScrollItem::new, new Item.Properties().stacksTo(16)
			.rarity(Rarity.RARE));

	public static final ResourceKey<CreativeModeTab> TAB = ResourceKey.create(Registries.CREATIVE_MODE_TAB, JjkCraft.id("main"));

	private JjkItems() {
	}

	public static void init() {
        CursedEquipment.init();
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, TAB, FabricCreativeModeTab.builder()
				.title(Component.translatable("itemGroup.jjkcraft.main"))
				.icon(() -> new ItemStack(CURSED_ENERGY_SHARD))
				.displayItems((context, output) -> {
					ALL.forEach(output::accept);
					context.holders().lookup(com.jjkcraft.registry.JjkRegistries.TECHNIQUE).ifPresent(techniques -> techniques.listElementIds()
							.forEach(key -> output.accept(TechniqueScrollItem.of(key.identifier()))));
				})
				.build());
	}

	private static Item trophy(String name, String curse, Rarity rarity) {
		Item item = register(name, Item::new, new Item.Properties().rarity(rarity));
		TROPHIES.put(curse, item);
		return item;
	}

	public static <T extends Item> T register(String name, Function<Item.Properties, T> factory, Item.Properties properties) {
		T item = registerHidden(name, factory, properties);
		ALL.add(item);
		return item;
	}

	/** Registers an item that stays out of the creative tab. */
	public static <T extends Item> T registerHidden(String name, Function<Item.Properties, T> factory, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, JjkCraft.id(name));
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
	}

	private static Item registerBlock(Block block) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, BuiltInRegistries.BLOCK.getKey(block));
		Item item = Registry.register(BuiltInRegistries.ITEM, key, new BlockItem(block, new Item.Properties().useBlockDescriptionPrefix().setId(key)));
		ALL.add(item);
		return item;
	}
}
