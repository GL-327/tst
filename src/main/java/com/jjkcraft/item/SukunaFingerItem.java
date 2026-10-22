package com.jjkcraft.item;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;
public final class SukunaFingerItem extends Item {
    public SukunaFingerItem(Properties p){super(p);}
    @Override public ItemStack finishUsingItem(ItemStack stack,Level level,LivingEntity user){
        if(user instanceof ServerPlayer p&&!com.jjkcraft.vessel.CursedVessels.consume(p))return stack;
        return super.finishUsingItem(stack,level,user);
    }
}
