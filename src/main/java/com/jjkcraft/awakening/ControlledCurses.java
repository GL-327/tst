package com.jjkcraft.awakening;

import java.util.*;
import com.mojang.serialization.Codec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.fabricmc.fabric.api.attachment.v1.*;
import com.jjkcraft.JjkCraft;
import com.jjkcraft.curse.*;
import com.jjkcraft.curse.spawn.CurseSpawner;
import com.jjkcraft.damage.DamageModifiers;
import com.jjkcraft.network.payload.TechniqueFxPayload;
import com.jjkcraft.sorcerer.Awakening;
import com.jjkcraft.technique.*;

/** Captured spirits are separate from the four technique slots. Three may fight at once. */
public final class ControlledCurses {
    public static final AttachmentType<List<Identifier>> STOCK=AttachmentRegistry.create(JjkCraft.id("captured_curses"),b->b.persistent(Identifier.CODEC.listOf()).copyOnDeath().initializer(List::of));
    public static final AttachmentType<String> OWNER=AttachmentRegistry.create(JjkCraft.id("curse_controller"),b->b.persistent(Codec.STRING).initializer(()->""));
    private ControlledCurses() { }
    public static void init() {
        Awakening.addResetHook(p->p.setAttached(STOCK,List.of()));
        DamageModifiers.add((level,target,source,amount)->{
            if(source.getEntity() instanceof CurseEntity curse) {
                String owner=curse.getAttachedOrCreate(OWNER);
                if(owner.equals(target.getUUID().toString()) || (!owner.isEmpty() && owner.equals(target.getAttachedOrCreate(OWNER))))return 0;
            }
            return amount;
        });
    }
    public static boolean controlled(CurseEntity curse) { return !curse.getAttachedOrCreate(OWNER).isEmpty(); }
    public static void direct(CurseEntity curse) {
        if(!controlled(curse) || !(curse.level() instanceof net.minecraft.server.level.ServerLevel level))return;
        ServerPlayer owner;try{owner=level.getServer().getPlayerList().getPlayer(UUID.fromString(curse.getAttachedOrCreate(OWNER)));}catch(IllegalArgumentException e){curse.discard();return;}
        if(owner==null || !owner.isAlive() || owner.level()!=level || owner.distanceToSqr(curse)>64*64){curse.discard();return;}
        LivingEntity target=owner.getLastHurtMob();
        if(target==null || !target.isAlive() || target==curse || owner.tickCount-owner.getLastHurtMobTimestamp()>200)target=owner.getLastHurtByMob();
        if(target==null || !target.isAlive() || target==owner || target==curse || target.getAttachedOrCreate(OWNER).equals(owner.getUUID().toString()))
            target=level.getEntitiesOfClass(CurseEntity.class,owner.getBoundingBox().inflate(16),e->e!=curse && !controlled(e) && e.isAlive()).stream().findFirst().orElse(null);
        curse.setTarget(target);
        if(target==null && curse.distanceToSqr(owner)>16)curse.getNavigation().moveTo(owner,1.2);
    }
    public static CastResult cast(CastContext ctx) {
        if(!(ctx.caster() instanceof ServerPlayer p))return CastResult.miss(ctx.eye());
        var stock=new ArrayList<>(p.getAttachedOrCreate(STOCK));
        if(p.isShiftKeyDown() || stock.isEmpty()) {
            var target=ctx.rayTarget(ctx.range(),0.8).orElse(null);
            if(!(target instanceof CurseEntity curse) || stock.size()>=8 || (controlled(curse) && !curse.getAttachedOrCreate(OWNER).equals(p.getUUID().toString()))
                || (!controlled(curse) && curse.getHealth()>curse.getMaxHealth()*Math.min(0.5,0.3*Math.sqrt(ctx.power()))))return CastResult.miss(ctx.eye());
            stock.add(curse.curseId());p.setAttached(STOCK,List.copyOf(stock));curse.discard();ctx.fx(TechniqueFxPayload.Phase.HIT,curse.position());return CastResult.done(curse.position());
        }
        long active=ctx.level().getEntitiesOfClass(CurseEntity.class,p.getBoundingBox().inflate(64),e->e.getAttachedOrCreate(OWNER).equals(p.getUUID().toString())).size();
        if(active>=3)return CastResult.miss(ctx.eye());
        var curse=CurseSpawner.spawnOne(ctx.level(),stock.getFirst(),p.blockPosition().offset(2,0,0));if(curse==null)return CastResult.miss(ctx.eye());
        curse.setAttached(OWNER,p.getUUID().toString());curse.setPersistenceRequired();stock.removeFirst();p.setAttached(STOCK,List.copyOf(stock));ctx.fx(TechniqueFxPayload.Phase.BURST,curse.position());return CastResult.done(curse.position());
    }
    public static CastResult uzumaki(CastContext ctx) {
        if(!(ctx.caster() instanceof ServerPlayer p))return CastResult.miss(ctx.eye());
        int count=p.getAttachedOrCreate(STOCK).size();if(count==0)return CastResult.miss(ctx.eye());
        p.setAttached(STOCK,List.of());var targets=ctx.enemiesOnLine(ctx.range(),1.2);
        for(var target:targets)ctx.hurt(target,(8+count*7)*ctx.power(),true);
        ctx.fx(TechniqueFxPayload.Phase.CAST,ctx.eye(),ctx.look(),targets);return CastResult.hit(List.copyOf(targets),ctx.blockLimit(ctx.range()));
    }
}
