package com.jjkcraft.technique.behavior;

import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import com.jjkcraft.item.StrawDollLink;
import com.jjkcraft.item.JjkComponents;
import com.jjkcraft.item.JjkItems;
import com.jjkcraft.network.payload.TechniqueFxPayload;
import com.jjkcraft.technique.CastContext;
import com.jjkcraft.technique.CastResult;
import com.jjkcraft.technique.TechniqueBehavior;
import com.jjkcraft.technique.TechniqueBehaviorType;
import com.jjkcraft.technique.TechniqueBehaviors;

/**
 * Straw Doll Technique: Resonance. Binds the target in sight to a Straw Doll in the caster's hands. Each
 * nail driven into the doll (using it) hurts the target wherever it stands nearby. A curse or
 * mob casting it nails at once instead.
 */
public record StrawDollBehavior(double damage, int nails, int duration) implements TechniqueBehavior {
	public static final MapCodec<StrawDollBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.doubleRange(0, 1000).fieldOf("damage").forGetter(StrawDollBehavior::damage),
			Codec.intRange(1, 10).optionalFieldOf("nails", 3).forGetter(StrawDollBehavior::nails),
			Codec.intRange(20, 12_000).optionalFieldOf("duration", 600).forGetter(StrawDollBehavior::duration)
	).apply(i, StrawDollBehavior::new));

	@Override
	public TechniqueBehaviorType<?> type() {
		return TechniqueBehaviors.STRAW_DOLL;
	}

	@Override
	public CastResult cast(CastContext ctx) {
		double reach = ctx.range() > 0 ? ctx.range() : 12;
		Optional<LivingEntity> target = ctx.rayTarget(reach, 0.5);
		if (target.isEmpty()) {
			ctx.fx(TechniqueFxPayload.Phase.CAST, ctx.blockLimit(reach));
			return CastResult.miss(ctx.blockLimit(reach));
		}

		LivingEntity bound = target.get();
		if (ctx.caster() instanceof ServerPlayer player) {
			ItemStack doll = new ItemStack(JjkItems.STRAW_DOLL);
			doll.set(JjkComponents.STRAW_DOLL_LINK, new StrawDollLink(bound.getUUID(), bound.getName().getString(), ctx.now() + duration, nails,
					(float) (damage * ctx.power())));
			if (!player.getInventory().add(doll)) {
				player.spawnAtLocation(ctx.level(), doll);
			}
		} else {
			ctx.hurt(bound, damage * ctx.power(), true);
		}

		// A red thread from the caster's hands to the bound one.
		ctx.fx(TechniqueFxPayload.Phase.CAST, ctx.eye().add(0, -0.3, 0), bound.position().add(0, bound.getBbHeight() * 0.6, 0).subtract(ctx.eye().add(0, -0.3, 0)),
				List.of());
		ctx.fx(TechniqueFxPayload.Phase.HIT, bound.position().add(0, bound.getBbHeight() * 0.6, 0), ctx.look(), List.of(bound));
		return CastResult.hit(List.of(bound), bound.position());
	}
}
