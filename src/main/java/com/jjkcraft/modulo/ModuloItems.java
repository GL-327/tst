package com.jjkcraft.modulo;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.mojang.serialization.Codec;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.level.block.Block;

import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;

import com.jjkcraft.JjkCraft;
import com.jjkcraft.item.JjkItems;
import com.jjkcraft.item.tool.CursedToolItem;
import com.jjkcraft.modulo.item.ModuloGear;
import com.jjkcraft.modulo.item.MulItem;

/** The Modulo expansion's items and its creative tab (docs/MODULO.md 3.5). */
public final class ModuloItems {
	public static final List<Item> ALL = new ArrayList<>();

	/** Cursed energy still to pour into the top piece of a stack of Mul. */
	public static final DataComponentType<Integer> MUL_LEFT = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, JjkCraft.id("mul_left"),
			DataComponentType.<Integer>builder().persistent(Codec.intRange(0, 100_000)).networkSynchronized(ByteBufCodecs.VAR_INT).build());

	public static final Item MUL_ORE = block(ModuloBlocks.MUL_ORE);
	public static final Item NAUNAX_HULL = block(ModuloBlocks.NAUNAX_HULL);
	public static final Item NAUNAX_DECK_PLATE = block(ModuloBlocks.NAUNAX_DECK_PLATE);
	public static final Item MUL_CONDUIT = block(ModuloBlocks.MUL_CONDUIT);

	// Simurian
	public static final MulItem MUL = register("mul", p -> new MulItem(p, 40), new Item.Properties().rarity(Rarity.UNCOMMON));
	public static final MulItem HIGH_PURITY_MUL = register("high_purity_mul", p -> new MulItem(p, 160), new Item.Properties().rarity(Rarity.RARE));
	public static final CursedToolItem RUMELIAN_BLADE = register("rumelian_blade", CursedToolItem::new,
			new Item.Properties().sword(ToolMaterial.DIAMOND, 4f, -2.5f).rarity(Rarity.RARE));
	public static final Item GRAVITY_HARNESS = register("gravity_harness", ModuloGear.GravityHarness::new, new Item.Properties().stacksTo(1).rarity(Rarity.RARE));
	public static final Item MUL_WARD = register("mul_ward", ModuloGear.MulWard::new, new Item.Properties().stacksTo(1).rarity(Rarity.RARE));
	public static final Item NAUNAX_BEACON = register("naunax_beacon", ModuloGear.NaunaxBeacon::new, new Item.Properties().stacksTo(1).rarity(Rarity.RARE));

	// Canon
	public static final CursedToolItem HONOYAGI = register("honoyagi", CursedToolItem::new,
			new Item.Properties().sword(ToolMaterial.DIAMOND, 4f, -2.4f).rarity(Rarity.EPIC));
	public static final Item WANCHA_OCTOPUS = register("wancha_octopus", p -> new ModuloGear.Described(p), new Item.Properties());
	public static final Item TAKOYAKI = register("takoyaki", p -> new ModuloGear.Described(p),
			new Item.Properties().food(new FoodProperties(6, 0.8f, false)));
	public static final Item TIGER_CUB = register("tiger_cub", p -> new ModuloGear.Described(p), new Item.Properties().stacksTo(1));
	public static final Item YUJI_CURSED_OBJECT = register("yuji_cursed_object",
			p -> new ModuloGear.Described(p, "tooltip.jjkcraft.modulo.special_grade_object", "tooltip.jjkcraft.modulo.vessel_later"),
			new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant());

	public static final ResourceKey<CreativeModeTab> TAB = ResourceKey.create(Registries.CREATIVE_MODE_TAB, JjkCraft.id("modulo"));

	private ModuloItems() {
	}

	public static void init() {
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, TAB, FabricCreativeModeTab.builder()
				.title(Component.translatable("itemGroup.jjkcraft.modulo"))
				.icon(() -> new ItemStack(MUL))
				.displayItems((context, output) -> ALL.forEach(output::accept))
				.build());
		ModuloGear.init();
	}

	private static <T extends Item> T register(String name, Function<Item.Properties, T> factory, Item.Properties properties) {
		T item = JjkItems.registerHidden(name, factory, properties);
		ALL.add(item);
		return item;
	}

	private static Item block(Block block) {
		return register(BuiltInRegistries.BLOCK.getKey(block).getPath(), p -> new BlockItem(block, p), new Item.Properties().useBlockDescriptionPrefix());
	}
}
