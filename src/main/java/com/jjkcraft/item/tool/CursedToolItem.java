package com.jjkcraft.item.tool;

import java.util.Optional;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import com.jjkcraft.energy.CursedEnergy;
import com.jjkcraft.particle.JjkParticles;
import com.jjkcraft.registry.JjkRegistries;
import com.jjkcraft.technique.TechniqueDefinition;
import com.jjkcraft.technique.TechniqueService;

/**
 * A grade-tagged cursed weapon. Its data ({@link CursedToolDefinition}) names the grade, the
 * technique it lends while held, and what its blows do. Below that grade it is just a weapon.
 */
public class CursedToolItem extends Item {
	public CursedToolItem(Properties properties) {
		super(properties);
	}
    @Override public net.minecraft.world.InteractionResult use(net.minecraft.world.level.Level level,net.minecraft.world.entity.player.Player player,net.minecraft.world.InteractionHand hand){
        var tool=CursedTools.get(player,player.getItemInHand(hand));
        if(tool.isEmpty()||tool.get().technique().isEmpty())return net.minecraft.world.InteractionResult.PASS;
        if(player instanceof net.minecraft.server.level.ServerPlayer p){
            var failure=TechniqueService.cast(p,tool.get().technique().orElseThrow(),-1);
            if(failure!=com.jjkcraft.technique.CastFailure.NONE)p.sendSystemMessage(Component.translatable(failure.translationKey()),true);
        }
        return net.minecraft.world.InteractionResult.SUCCESS;
    }

	@Override
	public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		super.postHurtEnemy(stack, target, attacker);
		if (!(attacker.level() instanceof ServerLevel level)) {
			return;
		}

		CursedTools.get(attacker, stack).filter(tool -> CursedTools.usable(attacker, tool)).ifPresent(tool -> {
			switch (tool.onHit()) {
				case DISPEL -> {
					TechniqueService.cancel(target);
					CursedEnergy.drain(target, tool.amount());
					level.sendParticles(JjkParticles.of(JjkParticles.DOMAIN_SHARD, 0xE0E0F0, 0.8f), target.getX(), target.getY() + target.getBbHeight() * 0.6,
							target.getZ(), 10, 0.3, 0.4, 0.3, 0.08);
				}
				case SIPHON -> {
					double taken = CursedEnergy.drain(target, tool.amount());
					CursedEnergy.restore(attacker, taken);
					level.sendParticles(JjkParticles.of(JjkParticles.CE_AURA, 0x8F5CFF, 0.6f), target.getX(), target.getY() + 1, target.getZ(), 8, 0.3, 0.4, 0.3, 0.02);
				}
				case IGNITE -> target.igniteForSeconds((float) tool.amount());
                case SHOCK -> {if(CursedEnergy.trySpend(attacker,4))target.addEffect(new net.minecraft.world.effect.MobEffectInstance(com.jjkcraft.effect.JjkEffects.STUNNED,Math.clamp((int)tool.amount(),1,6),0));}
                case RECOIL -> {if(CursedEnergy.trySpend(attacker,3))target.knockback(Math.min(.8,tool.amount()),attacker.getX()-target.getX(),attacker.getZ()-target.getZ(),level.damageSources().mobAttack(attacker),0f);}
				default -> {
				}
			}
		});
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
		describe(stack.getItem(), context.registries(), builder);
	}

	/** The tool lines every cursed tool shows: grade, the technique it lends (with cost), its effect. */
	public static void describe(Item item, HolderLookup.@Nullable Provider registries, Consumer<Component> builder) {
		Optional<CursedToolDefinition> found = find(registries, item);
		if (found.isEmpty()) {
			return;
		}

		CursedToolDefinition tool = found.get();
		Component grade = registries == null ? Component.literal(tool.grade().toString())
				: registries.lookup(JjkRegistries.GRADE).flatMap(r -> r.get(net.minecraft.resources.ResourceKey.create(JjkRegistries.GRADE, tool.grade())))
						.map(h -> (Component) Component.translatable(h.value().name())).orElse(Component.literal(tool.grade().toString()));
		builder.accept(Component.translatable("tooltip.jjkcraft.tool.grade", grade).withStyle(ChatFormatting.GOLD));
		tool.technique().ifPresent(id -> {
			TechniqueDefinition technique = technique(registries, id);
			Component name = technique == null ? Component.literal(id.toString()) : Component.translatable(technique.name());
			String key = tool.kind() == CursedToolDefinition.Kind.CURSED_OBJECT ? "tooltip.jjkcraft.tool.cursed_object" : "tooltip.jjkcraft.tool.grants";
			builder.accept(technique == null ? Component.translatable(key, name).withStyle(ChatFormatting.AQUA)
					: Component.translatable(key + "_cost", name, Math.round(technique.cast().cost() * tool.costMultiplier())).withStyle(ChatFormatting.AQUA));
		});
		builder.accept(Component.translatable(tool.description()).withStyle(ChatFormatting.GRAY));
	}

	public static Optional<CursedToolDefinition> find(HolderLookup.@Nullable Provider registries, Item item) {
		if (registries == null) {
			return Optional.empty();
		}

		Identifier id = BuiltInRegistries.ITEM.getKey(item);
		return registries.lookup(JjkRegistries.CURSED_TOOL).flatMap(r -> r.listElements().map(h -> h.value()).filter(def -> def.item().equals(id)).findFirst());
	}

	private static @Nullable TechniqueDefinition technique(HolderLookup.@Nullable Provider registries, Identifier id) {
		if (registries == null) {
			return null;
		}

		return registries.lookup(JjkRegistries.TECHNIQUE).flatMap(r -> r.get(net.minecraft.resources.ResourceKey.create(JjkRegistries.TECHNIQUE, id)))
				.map(h -> h.value()).orElse(null);
	}
}
