package com.jjkcraft.death;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;

/** Ordinary, tradable item; it is deliberately vulnerable to fire, lava and destruction. */
public final class SoulObjectItem extends Item {
    public SoulObjectItem(Properties p) { super(p); }
    @Override public void onDestroyed(ItemEntity entity) {
        super.onDestroyed(entity);
        if (entity.level() instanceof ServerLevel level) {
            SoulObject soul = entity.getItem().get(SoulSystem.OBJECT_DATA);
            if (soul != null) SoulSystem.destroy(level.getServer(), soul);
        }
    }
}
