package com.jjkcraft.technique;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.Vec3;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import com.jjkcraft.config.JjkConfig;

/** Bounded terrain damage with vanilla block displays: debris never places blocks or drops loot. */
public final class TechniqueTerrain {
    private TechniqueTerrain() { }
    private record Debris(Display.BlockDisplay entity,Vec3 start,Vec3 end,long born,int lifetime) { }
    private static final List<Debris> DEBRIS=new ArrayList<>();
    private static final Map<ServerLevel,Budget> BUDGETS=new IdentityHashMap<>();
    private static final class Budget {long tick;int changed,scanned;Budget(long tick){this.tick=tick;}}
    public static void init(){
        ServerTickEvents.END_LEVEL_TICK.register(TechniqueTerrain::tick);
        ServerLifecycleEvents.SERVER_STOPPING.register(s->{DEBRIS.forEach(d->d.entity.discard());DEBRIS.clear();BUDGETS.clear();});
    }
    private static Budget budget(ServerLevel level){var b=BUDGETS.get(level);if(b==null||b.tick!=level.getGameTime()){b=new Budget(level.getGameTime());BUDGETS.put(level,b);}return b;}
    public static int sphere(CastContext ctx,Vec3 center,double radius,int limit,boolean pull){
        if(!JjkConfig.server().casting.terrainDamage||radius<=0||limit<=0)return 0;
        var level=ctx.level();var caster=ctx.caster();
        if(caster instanceof ServerPlayer p){if(p.isSpectator()||p.gameMode.getGameModeForPlayer()==net.minecraft.world.level.GameType.ADVENTURE)return 0;}
        else if(!level.getGameRules().get(GameRules.MOB_GRIEFING))return 0;
        int used=(int)ctx.memory.getOrDefault("terrain_blocks",0);
        if(used>=384)return 0;
        radius=Math.min(6,radius);int extent=(int)Math.ceil(radius);var origin=BlockPos.containing(center);
        var budget=budget(level);int changed=0;double hardness=Math.min(12,Math.max(2.5,ctx.power()*5));
        List<BlockPos> positions=new ArrayList<>();
        for(int y=-extent;y<=extent;y++)for(int x=-extent;x<=extent;x++)for(int z=-extent;z<=extent;z++){
            if(++budget.scanned>12000)return 0;
            BlockPos pos=origin.offset(x,y,z);
            if(Vec3.atCenterOf(pos).distanceToSqr(center)<=radius*radius)positions.add(pos);
        }
        positions.sort(Comparator.comparingDouble(p->Vec3.atCenterOf(p).distanceToSqr(center)));
        for(var pos:positions){
            if(changed>=Math.min(limit,384-used)||budget.changed>=256)break;
            if(!level.hasChunkAt(pos)||!level.getWorldBorder().isWithinBounds(pos))continue;
            var state=level.getBlockState(pos);float resistance=state.getDestroySpeed(level,pos);
            if(state.isAir()||!state.getFluidState().isEmpty()||state.hasBlockEntity()||resistance<0||resistance>hardness)continue;
            if(BuiltInRegistries.BLOCK.getKey(state.getBlock()).getNamespace().equals("jjkcraft"))continue;
            if(caster instanceof ServerPlayer p&&(!level.mayInteract(p,pos)||!PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(level,p,pos,state,null)))continue;
            if(!level.setBlock(pos,Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL))continue;
            changed++;budget.changed++;
            Vec3 start=Vec3.atLowerCornerOf(pos),delta=Vec3.atCenterOf(pos).subtract(center);
            Vec3 end=pull?center.add(-.5,-.5,-.5):start.add(delta.lengthSqr()<.01?new Vec3(0,1,0):delta.normalize().scale(2.5)).add(0,1,0);
            if(DEBRIS.size()<192){
                var display=new Display.BlockDisplay(EntityTypes.BLOCK_DISPLAY,level){@Override public boolean shouldBeSaved(){return false;}};
                display.setBlockState(state);display.setPosRotInterpolationDuration(2);display.setPos(start.x,start.y,start.z);
                if(level.addFreshEntity(display))DEBRIS.add(new Debris(display,start,end,level.getGameTime(),pull?18:12));
            }
        }
        ctx.memory.put("terrain_blocks",used+changed);return changed;
    }
    public static void line(CastContext ctx,Vec3 from,Vec3 to,double width){
        int steps=Math.min(64,Math.max(1,(int)Math.ceil(from.distanceTo(to))));
        for(int i=1;i<=steps;i++){
            Vec3 point=from.lerp(to,i/(double)steps);sphere(ctx,point,Math.clamp(width,.7,3),12,false);
            var state=ctx.level().getBlockState(BlockPos.containing(point));
            if(!state.isAir()&&!state.getCollisionShape(ctx.level(),BlockPos.containing(point)).isEmpty())break;
        }
    }
    private static void tick(ServerLevel level){
        for(var it=DEBRIS.iterator();it.hasNext();){var d=it.next();if(d.entity.level()!=level)continue;
            double age=(level.getGameTime()-d.born)/(double)d.lifetime;
            if(age>=1||d.entity.isRemoved()){d.entity.discard();it.remove();continue;}
            double t=age*age*(3-2*age);Vec3 at=d.start.lerp(d.end,t);d.entity.setPos(at.x,at.y,at.z);
        }
    }
}
