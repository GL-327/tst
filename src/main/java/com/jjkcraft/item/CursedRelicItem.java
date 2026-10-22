package com.jjkcraft.item;

import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import com.jjkcraft.energy.CursedEnergy;
import com.jjkcraft.particle.JjkParticles;

/** Original ritual objects: a finite defensive charm and a risky energy reserve. */
public final class CursedRelicItem extends Item {
    private final boolean reliquary;
    public CursedRelicItem(Properties properties,boolean reliquary){super(properties);this.reliquary=reliquary;}
    @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
        if(!(player instanceof ServerPlayer p))return InteractionResult.SUCCESS;
        var stack=p.getItemInHand(hand);
        if(!p.isAlive()||p.isSpectator()||p.getCooldowns().isOnCooldown(stack))return InteractionResult.FAIL;
        if(reliquary){
            if(!p.isShiftKeyDown()){p.sendSystemMessage(Component.translatable("item.jjkcraft.sealed_reliquary.confirm"),true);return InteractionResult.FAIL;}
            if(!CursedEnergy.isCapable(p)||!CursedEnergy.uncapped(p)&&CursedEnergy.current(p)>=CursedEnergy.max(p)-1)return InteractionResult.FAIL;
            CursedEnergy.restore(p,250);p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,300,0,false,true));
        }else{
            if(!CursedEnergy.trySpend(p,25))return InteractionResult.FAIL;
            p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION,1200,0,false,true));
        }
        p.level().sendParticles(JjkParticles.of(JjkParticles.REVERSE_GLOW,reliquary?0xa781e8:0xe8c784,1),p.getX(),p.getY()+1,p.getZ(),14,.4,.6,.4,.04);
        p.getCooldowns().addCooldown(stack,200);stack.consume(1,p);return InteractionResult.SUCCESS_SERVER;
    }
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,TooltipDisplay display,Consumer<Component> lines,TooltipFlag flag){
        lines.accept(Component.translatable(getDescriptionId()+".desc").withColor(0xada1cf));
    }
}
