package com.jjkcraft.client.mixin;

import java.util.Map;
import java.util.UUID;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.client.gui.components.LerpingBossEvent;

/** Reads how many boss bars are showing, so top-centre HUD elements can sit below them. */
@Mixin(BossHealthOverlay.class)
public interface BossHealthOverlayAccessor {
	@Accessor("events")
	Map<UUID, LerpingBossEvent> jjkcraft$events();
}
