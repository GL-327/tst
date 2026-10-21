package com.jjkcraft.technique;

import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.tags.DamageTypeTags;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import com.jjkcraft.damage.*;
import com.jjkcraft.energy.CursedEnergy;
import com.jjkcraft.reverse.ReverseTechnique;
import com.jjkcraft.technique.behavior.InfinityBehavior;
import com.jjkcraft.network.JjkNetworking;
import com.jjkcraft.network.payload.InfinityPayload;

/** An independent passive field; ordinary casts and channels do not replace it. */
public final class InfinityField {
    private static final Map<UUID,CastContext> FIELDS=new HashMap<>();
    private InfinityField() { }
    public static boolean on(LivingEntity caster){return FIELDS.containsKey(caster.getUUID());}
    public static double upkeep(LivingEntity caster,TechniqueDefinition d){return ReverseTechnique.available(caster)?0:d.cast().upkeep()*TechniqueService.modifiers(caster,com.jjkcraft.JjkCraft.id("infinity"),d).cost();}
    public static void start(CastContext ctx){FIELDS.put(ctx.caster().getUUID(),ctx);sync(ctx.caster());}
    public static void stop(LivingEntity caster){if(FIELDS.containsKey(caster.getUUID()))CursedEnergy.checkpoint(caster);var ctx=FIELDS.remove(caster.getUUID());if(ctx!=null){ctx.technique().behavior().end(ctx);sync(caster);}}
    private static void sync(LivingEntity caster){if(caster instanceof ServerPlayer p)JjkNetworking.send(p,new InfinityPayload(on(caster)));}
    public static void init(){
        com.jjkcraft.sorcerer.SorcererLifecycle.addFullSync(InfinityField::sync);
        // Checkpoint before changing the map so lazy regeneration cannot be recovered on toggle-off.
        CursedEnergy.addRegenModifier(caster->on(caster)&&!ReverseTechnique.available(caster)?0:1);
        DamageModifiers.add((level,target,source,amount)->{
            var ctx=FIELDS.get(target.getUUID());if(ctx==null||!(ctx.technique().behavior() instanceof InfinityBehavior field))return amount;
            if(source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)||source.is(JjkDamageTypes.SURE_HIT)||source.is(JjkDamageTypes.BACKLASH))return amount;
            if(source.getEntity() instanceof LivingEntity attacker&&source.getDirectEntity()==attacker&&source.typeHolder().unwrapKey().map(k->k.identifier().getNamespace().equals("minecraft")).orElse(false)
                &&com.jjkcraft.item.tool.CursedTools.get(attacker,attacker.getMainHandItem()).filter(tool->com.jjkcraft.item.tool.CursedTools.usable(attacker,tool)&&tool.onHit()==com.jjkcraft.item.tool.CursedToolDefinition.OnHit.DISPEL).isPresent()){
                TechniqueService.cancel(target);return amount;
            }
            boolean projectile=source.is(DamageTypeTags.IS_PROJECTILE);
            if((projectile&&field.filter()==1)||(!projectile&&field.filter()==0)||source.getEntity()==null||source.getEntity()==target)return amount;
            // Environmental damage and sure-hits still apply; spatial separation stops incoming attacks.
            return 0;
        });
        ServerTickEvents.END_SERVER_TICK.register(server->{for(var ctx:List.copyOf(FIELDS.values())){
            var caster=ctx.caster();
            if(!caster.isAlive()||caster.isRemoved()||caster.level()!=ctx.level()||caster instanceof ServerPlayer p&&(p.isSpectator()||!TechniqueService.knows(p,ctx.techniqueId()))||PersonalPacts.check(caster,ctx.techniqueId(),ctx.technique())!=CastFailure.NONE){stop(caster);continue;}
            double cost=upkeep(caster,ctx.technique())/20;
            if(cost>0&&!CursedEnergy.trySpend(caster,cost)){stop(caster);continue;}
            ctx.technique().behavior().tick(ctx,0);
        }});
        ServerLifecycleEvents.SERVER_STOPPED.register(s->FIELDS.clear());
    }
}
