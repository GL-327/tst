package com.jjkcraft.domain.behavior;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import com.jjkcraft.domain.ActiveDomain;
import com.jjkcraft.domain.DomainService;
import com.jjkcraft.item.ArsenalBladeItem;
import com.jjkcraft.item.JjkComponents;
import com.jjkcraft.technique.TechniqueCategory;
import com.jjkcraft.technique.TechniqueService;
import com.jjkcraft.technique.TechniqueSources;
import com.jjkcraft.technique.Techniques;
import com.jjkcraft.registry.LegacyIds;

/**
 * The arsenal: the domain forges a blade for each technique its caster has recently copied (seen
 * cast near them by someone else), filling empty racks from its own list. A blade casts its
 * technique at reduced cost, only inside this domain; all blades vanish when it ends.
 *
 * @param slots          how many blades to forge
 * @param fallback       techniques to load when fewer were copied
 * @param costMultiplier CE cost multiplier for casts from a blade
 */
public record ArsenalBehavior(int slots, List<Identifier> fallback, double costMultiplier) implements DomainBehavior {
	public static final MapCodec<ArsenalBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.intRange(1, 9).fieldOf("slots").forGetter(ArsenalBehavior::slots),
			LegacyIds.TECHNIQUE_ID.listOf().optionalFieldOf("fallback", List.of()).forGetter(ArsenalBehavior::fallback),
			Codec.doubleRange(0, 1).optionalFieldOf("cost_multiplier", 0.5).forGetter(ArsenalBehavior::costMultiplier)
	).apply(i, ArsenalBehavior::new));

	/** How far away a cast can be seen and copied. */
	private static final double COPY_RANGE = 24;
	private static final int COPY_MEMORY = 8;
	/** Techniques each player has copied, newest first. Not saved: copies fade with the session. */
	private static final Map<UUID, Deque<Identifier>> COPIES = new HashMap<>();

	@Override
	public DomainBehaviorType<?> type() {
		return DomainBehaviors.ARSENAL;
	}

	/** Hooks: copying, blade-granted techniques and blade cost. Called once at startup. */
	public static void init() {
		TechniqueService.addCastListener((caster, id, definition) -> {
			if (!(caster.level() instanceof ServerLevel level) || definition.category() == TechniqueCategory.DOMAIN) {
				return;
			}

			for (ServerPlayer watcher : level.players()) {
				if (watcher != caster && watcher.distanceTo(caster) <= COPY_RANGE) {
					Deque<Identifier> copies = COPIES.computeIfAbsent(watcher.getUUID(), k -> new ArrayDeque<>());
					copies.remove(id);
					copies.addFirst(id);
					while (copies.size() > COPY_MEMORY) {
						copies.removeLast();
					}
				}
			}
		});

		TechniqueSources.add(ArsenalBehavior::bladeTechniques);
		TechniqueService.addModifierProvider((caster, id, definition) -> {
			if (caster instanceof Player player && !TechniqueService.knows(player, id) && bladeTechniques(player).contains(id)) {
				double multiplier = DomainService.active(player).flatMap(d -> d.definition().behaviors().stream()
						.filter(b -> b instanceof ArsenalBehavior).map(b -> ((ArsenalBehavior) b).costMultiplier()).findFirst()).orElse(1.0);
				return new TechniqueService.Modifiers(1, multiplier, 1);
			}

			return TechniqueService.Modifiers.NONE;
		});
	}

	public static List<Identifier> copiedBy(Player player) {
		return List.copyOf(COPIES.getOrDefault(player.getUUID(), new ArrayDeque<>()));
	}

	/** Techniques granted by usable Arsenal Blades in the player's hands. */
	private static Set<Identifier> bladeTechniques(Player player) {
		Set<Identifier> out = new LinkedHashSet<>();
		for (InteractionHand hand : InteractionHand.values()) {
			ItemStack stack = player.getItemInHand(hand);
			Identifier technique = stack.get(JjkComponents.LOADED_TECHNIQUE);
			if (technique != null && ArsenalBladeItem.usable(player, stack)) {
				out.add(technique);
			}
		}

		return out;
	}

	@Override
	public void open(ActiveDomain domain) {
		if (!(domain.caster() instanceof ServerPlayer player)) {
			return;
		}

		List<Identifier> loadout = new ArrayList<>();
		for (Identifier id : copiedBy(player)) {
			if (loadout.size() < slots && Techniques.get(domain.level().registryAccess(), id).isPresent() && !loadout.contains(id)) {
				loadout.add(id);
			}
		}

		for (Identifier id : fallback) {
			if (loadout.size() < slots && Techniques.get(domain.level().registryAccess(), id).isPresent() && !loadout.contains(id)) {
				loadout.add(id);
			}
		}

		for (Identifier id : loadout) {
			ItemStack blade = ArsenalBladeItem.forge(id, domain.id());
			if (!player.getInventory().add(blade)) {
				domain.level().addFreshEntity(new ItemEntity(domain.level(), player.getX(), player.getY() + 0.5, player.getZ(), blade));
			}
		}

		domain.memory().put("arsenal", loadout);
		domain.event(DomainService.Event.PULSE, loadout.size());
	}

	@Override
	public void close(ActiveDomain domain) {
		if (domain.caster() instanceof Player player) {
			for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
				if (isBladeOf(player.getInventory().getItem(slot), domain)) {
					player.getInventory().setItem(slot, ItemStack.EMPTY);
				}
			}
		}

		for (ItemEntity item : domain.level().getEntitiesOfClass(ItemEntity.class, domain.bounds().inflate(16), e -> isBladeOf(e.getItem(), domain))) {
			item.discard();
		}
	}

	private static boolean isBladeOf(ItemStack stack, ActiveDomain domain) {
		Integer instance = stack.get(JjkComponents.ARSENAL_DOMAIN);
		return instance != null && instance == domain.id();
	}
}
