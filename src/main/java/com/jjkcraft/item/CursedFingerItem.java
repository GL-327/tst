package com.jjkcraft.item;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import com.jjkcraft.effect.JjkEffects;
import com.jjkcraft.grade.Grades;
import com.jjkcraft.item.tool.CursedToolDefinition;
import com.jjkcraft.item.tool.CursedToolItem;
import com.jjkcraft.item.tool.CursedTools;
import com.jjkcraft.sorcerer.Progression;
import com.jjkcraft.technique.TechniqueService;
import com.jjkcraft.technique.Techniques;

/**
 * A Withered Digit: a cursed object. Swallowing it grants a technique (its data's, or one the
 * eater does not know yet) and unsettles them: their cursed energy runs thin and wild for a while.
 * Below the object's grade the backlash is worse.
 */
public class CursedFingerItem extends Item {
	public CursedFingerItem(Properties properties) {
		super(properties);
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
		ItemStack result = super.finishUsingItem(stack, level, entity);
		if (entity instanceof ServerPlayer player && level instanceof ServerLevel server) {
			swallow(player, server, CursedTools.get(player, new ItemStack(this)));
		}

		return result;
	}

	/** The cursed object takes hold. Public for tests. */
	public static Optional<Identifier> swallow(ServerPlayer player, ServerLevel level, Optional<CursedToolDefinition> data) {
		Optional<Identifier> granted = data.flatMap(CursedToolDefinition::technique).filter(id -> !TechniqueService.knows(player, id));
		if (granted.isEmpty()) {
			List<Identifier> unknown = new ArrayList<>();
			for (Techniques.Entry entry : Techniques.all(level.registryAccess())) {
				if (entry.definition().innateWeight() > 0 && !TechniqueService.knows(player, entry.id())) {
					unknown.add(entry.id());
				}
			}

			if (!unknown.isEmpty()) {
				granted = Optional.of(unknown.get(player.getRandom().nextInt(unknown.size())));
			}
		}

		granted.ifPresent(id -> {
			TechniqueService.grant(player, id);
			Component name = Techniques.get(level.registryAccess(), id).map(def -> (Component) Component.translatable(def.name()))
					.orElse(Component.literal(id.toString()));
			player.sendSystemMessage(Component.translatable("message.jjkcraft.cursed_object.learned", name));
		});

		int risk = (int) data.map(CursedToolDefinition::amount).orElse(1200.0).doubleValue();
		player.addEffect(new MobEffectInstance(JjkEffects.UNSETTLED, Math.max(20, risk), 0, false, true));
		boolean tooLow = data.map(tool -> Progression.gradeRank(level.registryAccess(), player) < Grades.rank(level.registryAccess(), tool.grade()))
				.orElse(false);
		if (tooLow) {
			player.hurtServer(level, level.damageSources().magic(), 4);
			player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 200, 0, false, true));
			player.sendSystemMessage(Component.translatable("message.jjkcraft.cursed_object.backlash"));
		}

		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 1.2f, 0.6f);
		return granted;
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
		CursedToolItem.describe(this, context.registries(), builder);
	}
}
