package com.jjkcraft.training;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;
public final class TrainingBlock extends Block{
    public TrainingBlock(Properties p){super(p);}
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return this==com.jjkcraft.block.JjkBlocks.TRAINING_MAT?Block.box(0,0,0,16,3,16):this==com.jjkcraft.block.JjkBlocks.BALANCE_BEAM?Block.box(5,0,0,11,16,16):Block.box(2,0,2,14,10,14);}
    @Override protected InteractionResult useWithoutItem(BlockState s,Level level,BlockPos pos,Player user,BlockHitResult hit){
        if(user instanceof net.minecraft.server.level.ServerPlayer p){
            if(this==com.jjkcraft.block.JjkBlocks.FOCUS_STONE){if(p.distanceToSqr(Vec3.atCenterOf(pos))>16)return InteractionResult.FAIL;if(!com.jjkcraft.reverse.ReverseStudy.startInWorld(p))p.sendSystemMessage(net.minecraft.network.chat.Component.literal("Learn reinforcement on a straw doll first. Stand grounded and still near this stone to practise reverse flow."),true);}
            else p.sendSystemMessage(net.minecraft.network.chat.Component.literal(this==com.jjkcraft.block.JjkBlocks.TRAINING_MAT?"Stand on the mat. Hold sneak for 2–5 seconds, then release to complete a breathing drill.":"Balance beam · walk its narrow top to train agility."),true);
        }return InteractionResult.SUCCESS;
    }
}
