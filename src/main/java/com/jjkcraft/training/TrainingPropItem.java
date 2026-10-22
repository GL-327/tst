package com.jjkcraft.training;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.InteractionResult;
public final class TrainingPropItem extends Item{
    public TrainingPropItem(Properties p){super(p);}
    @Override public InteractionResult useOn(UseOnContext c){
        if(c.getPlayer()==null)return InteractionResult.PASS;
        if(!c.getLevel().isClientSide()){
            var p=c.getClickedPos().relative(c.getClickedFace());var doll=com.jjkcraft.entity.JjkEntities.TRAINING_DUMMY.create(c.getLevel(),net.minecraft.world.entity.EntitySpawnReason.SPAWN_ITEM_USE);doll.snapTo(p.getX()+.5,p.getY(),p.getZ()+.5,c.getPlayer().getYRot()+180,0);
            if(!c.getLevel().noCollision(doll))return InteractionResult.FAIL;
            c.getLevel().addFreshEntity(doll);if(!c.getPlayer().isCreative())c.getItemInHand().shrink(1);
        }return InteractionResult.SUCCESS;
    }
}
