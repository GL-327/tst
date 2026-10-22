package com.jjkcraft.technique;

import java.util.*;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import com.jjkcraft.JjkCraft;
import com.jjkcraft.awakening.CharacterAwakening;
import com.jjkcraft.awakening.InnateFamily;
import com.jjkcraft.core.RateLimiter;
import com.jjkcraft.network.JjkNetworking;
import com.jjkcraft.network.payload.TechniqueMenuPayloads;
import com.jjkcraft.entity.shikigami.TenShadows;

/** Ritual management shares GitHub's family bindings and variant casting, rather than a second loadout format. */
public final class TechniqueFamilies {
    private static final RateLimiter LIMIT=new RateLimiter(6,8);
    private TechniqueFamilies() {}
    public static Identifier root(Identifier family){return TechniqueBindings.binding(family);}
    public static Optional<InnateFamily> family(Player p,Identifier id){return p.registryAccess().lookup(CharacterAwakening.FAMILIES).flatMap(r->r.getOptional(id));}
    public static boolean access(Player p,Identifier id){return family(p,id).map(f->f.techniques().stream().anyMatch(t->TechniqueService.knows(p,t))).orElse(false);}
    public static boolean equipped(Player p,Identifier move){return TechniqueService.get(p).loadout().stream().flatMap(Optional::stream).anyMatch(id->TechniqueBindings.contains(p.registryAccess(),TechniqueService.get(p),id,move));}
    public static void init(){
        ServerPlayNetworking.registerGlobalReceiver(TechniqueMenuPayloads.Action.TYPE,(message,ctx)->{
            var p=ctx.player();if(!p.isAlive()||p.isSpectator()||!LIMIT.tryAcquire(p.getUUID(),p.level().getGameTime())||!access(p,message.family()))return;
            String feedback="";
            if(message.action()==1){
                if(!message.family().equals(JjkCraft.id("ten_shadows"))||!family(p,message.family()).orElseThrow().techniques().contains(message.move()))return;
                feedback=TenShadows.challenge(p,message.move());
            }else if(message.action()==2){
                var allowed=family(p,message.family()).orElseThrow().techniques().stream().map(id->Techniques.get(p.registryAccess(),id)).flatMap(Optional::stream)
                    .filter(d->d.behavior() instanceof com.jjkcraft.technique.behavior.SummonBehavior).map(d->((com.jjkcraft.technique.behavior.SummonBehavior)d.behavior()).entity()).collect(java.util.stream.Collectors.toSet());
                for(var pet:com.jjkcraft.entity.shikigami.ShikigamiEntity.ownedBy(p.level(),com.jjkcraft.entity.shikigami.ShikigamiEntity.class,p))
                    if(allowed.contains(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(pet.getType())))pet.dissolve(p.level());
                feedback="Summons dismissed.";
            }else if(message.action()!=0)return;
            open(p,message.family(),false,feedback);
        });
        com.jjkcraft.sorcerer.SorcererLifecycle.addFullSync(p->{migrate(p);open(p,JjkCraft.id("ten_shadows"),false,"");});
    }
    public static void open(ServerPlayer p,Identifier family,boolean open,String feedback){
        if(access(p,family))JjkNetworking.send(p,new TechniqueMenuPayloads.View(family,TenShadows.tamed(p),TenShadows.trial(p),open,feedback));
    }
    /** Upgrade local-preview family_* slots to the newer family/ selector without losing direct bindings. */
    public static void migrate(ServerPlayer p){
        var old=TechniqueService.get(p);var slots=old.loadout().stream().map(s->s.map(id->legacy(p,id).orElse(id))).toList();
        var learned=old.learned().stream().filter(id->legacy(p,id).isEmpty()).toList();
        var mastery=new HashMap<>(old.mastery());mastery.keySet().removeIf(id->legacy(p,id).isPresent());
        TechniqueService.set(p,TechniqueState.normalized(old.innate(),learned,slots,old.selected(),mastery,old.cooldowns()));
    }
    private static Optional<Identifier> legacy(Player p,Identifier id){
        if(!id.getPath().startsWith("family_"))return Optional.empty();
        var family=Identifier.fromNamespaceAndPath(id.getNamespace(),id.getPath().substring(7));
        return family(p,family).map(f->root(family));
    }
}
