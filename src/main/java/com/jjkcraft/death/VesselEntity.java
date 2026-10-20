package com.jjkcraft.death;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import com.jjkcraft.culling.entity.KoganeEntity;

/** A colony Vessel. Empty-hand interaction opens exact contract terms; held objects revive owners. */
public final class VesselEntity extends KoganeEntity {
    public VesselEntity(EntityType<? extends Mob> type, Level level) { super(type, level); }
    @Override protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer p) {
            var stack = p.getItemInHand(hand);
            SoulObject object = stack.get(SoulSystem.OBJECT_DATA);
            if (object != null) SoulSystem.revive(p, this, stack, object);
            else SoulSystem.open(p);
        }
        return InteractionResult.SUCCESS;
    }
}
