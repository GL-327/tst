package com.jjkcraft.modulo.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.squid.Squid;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import com.jjkcraft.modulo.ModuloItems;

/**
 * The wancha octopus (mission 6, chapter 11): a mutated octopus with a single tentacle "said to contain
 * the flavor of all eight". Caught (killed by a player), it gives one wancha octopus for the takoyaki
 * party. Lives only at the beach site while someone is on that stage; never saved.
 */
public class WanchaOctopusEntity extends Squid {
	public WanchaOctopusEntity(EntityType<? extends Squid> type, Level level) {
		super(type, level);
	}

	@Override
	public boolean shouldBeSaved() {
		return false;
	}

	@Override
	public void die(DamageSource source) {
		super.die(source);
		if (level() instanceof ServerLevel level && source.getEntity() instanceof Player) {
			spawnAtLocation(level, new ItemStack(ModuloItems.WANCHA_OCTOPUS));
		}
	}
}
