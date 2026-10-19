package com.jjkcraft.item.tool;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import com.jjkcraft.core.modulo.ModuloMath;
import com.jjkcraft.grade.GradeDefinition;
import com.jjkcraft.grade.Grades;
import com.jjkcraft.registry.JjkRegistries;
import com.jjkcraft.sorcerer.Progression;
import com.jjkcraft.technique.TechniqueDefinition;
import com.jjkcraft.technique.TechniqueService;
import com.jjkcraft.technique.TechniqueSources;

/** Looks up cursed tool data by item, and grants weapons' techniques while they are held. */
public final class CursedTools {
	private CursedTools() {
	}

	public static void init() {
		TechniqueSources.add(CursedTools::heldTechniques);
		TechniqueService.addCostPayer(new TechniqueService.CostPayer() {
			@Override
			public boolean covers(LivingEntity caster, Identifier id, TechniqueDefinition definition) {
				return medium(caster, id).isPresent();
			}

			@Override
			public boolean pay(LivingEntity caster, Identifier id, TechniqueDefinition definition, double cost) {
				return payWithMedium(caster, id, cost);
			}
		});
		com.jjkcraft.energy.CursedEnergy.addCapacityModifier((entity, capacity) ->
				entity.hasEffect(com.jjkcraft.effect.JjkEffects.UNSETTLED) ? capacity * 0.7 : capacity);
		com.jjkcraft.item.DriftingGourdItem.init();
		TechniqueService.addModifierProvider((caster, id, definition) -> {
			double cost = 1;
			for (InteractionHand hand : InteractionHand.values()) {
				Optional<CursedToolDefinition> tool = get(caster, caster.getItemInHand(hand));
				if (tool.isPresent() && tool.get().technique().map(id::equals).orElse(false) && usable(caster, tool.get())) {
					cost *= tool.get().costMultiplier();
				}
			}

			return new TechniqueService.Modifiers(1, cost, 1);
		});
	}

	public static Optional<CursedToolDefinition> get(RegistryAccess access, Item item) {
		Identifier id = BuiltInRegistries.ITEM.getKey(item);
		return access.lookup(JjkRegistries.CURSED_TOOL).flatMap(registry -> registry.stream().filter(def -> def.item().equals(id)).findFirst());
	}

	public static Optional<CursedToolDefinition> get(LivingEntity holder, ItemStack stack) {
		return stack.isEmpty() ? Optional.empty() : get(holder.registryAccess(), stack.getItem());
	}

	/** Whether {@code holder}'s grade is high enough to use the tool's power. */
	public static boolean usable(LivingEntity holder, CursedToolDefinition tool) {
		if (!(holder instanceof Player)) {
			return true;
		}

		return Progression.gradeRank(holder.registryAccess(), holder) >= Grades.rank(holder.registryAccess(), tool.grade());
	}

	/** Techniques a player's held weapons grant right now. */
	private static Set<Identifier> heldTechniques(Player player) {
		Set<Identifier> granted = new LinkedHashSet<>();
		for (InteractionHand hand : InteractionHand.values()) {
			get(player, player.getItemInHand(hand)).filter(tool -> tool.kind() == CursedToolDefinition.Kind.WEAPON && usable(player, tool))
					.flatMap(CursedToolDefinition::technique).ifPresent(granted::add);
		}

		return granted;
	}

	/**
	 * The held tool that lends {@code technique} as a medium (docs/MODULO.md 3.6), and the hand that
	 * holds it. Mediums pay with their own durability, so Heavenly Restriction can use them.
	 */
	public static Optional<InteractionHand> medium(LivingEntity holder, Identifier technique) {
		if (!(holder instanceof Player)) {
			return Optional.empty();
		}

		for (InteractionHand hand : InteractionHand.values()) {
			Optional<CursedToolDefinition> tool = get(holder, holder.getItemInHand(hand));
			if (tool.isPresent() && tool.get().medium() && tool.get().kind() == CursedToolDefinition.Kind.WEAPON
					&& tool.get().technique().map(technique::equals).orElse(false) && usable(holder, tool.get())) {
				return Optional.of(hand);
			}
		}

		return Optional.empty();
	}

	/** The durability a tool of this grade has as a medium. */
	public static int mediumDurability(RegistryAccess access, Identifier grade) {
		Optional<GradeDefinition> definition = Grades.get(access, grade);
		return ModuloMath.toolDurability(definition.map(GradeDefinition::toolDurability).orElse(0), definition.map(GradeDefinition::rank).orElse(0));
	}

	private static boolean payWithMedium(LivingEntity holder, Identifier technique, double cost) {
		Optional<InteractionHand> hand = medium(holder, technique);
		if (hand.isEmpty()) {
			return false;
		}

		ItemStack stack = holder.getItemInHand(hand.get());
		CursedToolDefinition tool = get(holder, stack).orElseThrow();
		int max = mediumDurability(holder.registryAccess(), tool.grade());
		if (stack.getMaxDamage() != max) {
			// Durability scales with the tool's grade: rescale, keeping the share already worn.
			double worn = stack.getMaxDamage() > 0 ? (double) stack.getDamageValue() / stack.getMaxDamage() : 0;
			stack.set(DataComponents.MAX_DAMAGE, max);
			stack.setDamageValue((int) Math.round(worn * max));
		}

		int damage = ModuloMath.mediumDurability(cost, tool.durabilityPerCe());
		if (holder instanceof Player player && player.isCreative()) {
			return true;
		}

		stack.hurtAndBreak(damage, holder, hand.get());
		return true;
	}
}
