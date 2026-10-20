package com.jjkcraft.awakening;

import java.util.*;
import net.minecraft.core.Registry;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.*;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import com.jjkcraft.JjkCraft;
import com.jjkcraft.core.progress.WeightedRoll;
import com.jjkcraft.energy.CursedEnergy;
import com.jjkcraft.network.JjkNetworking;
import com.jjkcraft.network.payload.TraitPayload;
import com.jjkcraft.sorcerer.*;

public final class CharacterTraits {
    public static final ResourceKey<Registry<TraitDefinition>> DEFINITIONS = ResourceKey.createRegistryKey(JjkCraft.id("trait"));
    public static final AttachmentType<TraitState> STATE = AttachmentRegistry.create(JjkCraft.id("traits"), b->b.persistent(TraitState.CODEC).copyOnDeath().initializer(()->TraitState.EMPTY));
    private CharacterTraits() { }
    public static void register() { DynamicRegistries.registerSynced(DEFINITIONS,TraitDefinition.CODEC); }
    public static TraitState get(Player p) { return p.getAttachedOrCreate(STATE); }
    public static boolean has(Player p,String trait) { return get(p).traits().contains(JjkCraft.id(trait)); }
    public static void set(ServerPlayer p,TraitState state) {
        boolean was=has(p,"mechamaru_restriction");CursedEnergy.checkpoint(p);p.setAttached(STATE,state);
        if(!was&&has(p,"mechamaru_restriction")){
            Progression.set(p,Progression.get(p).withHeavenlyRestriction(false));
            com.jjkcraft.energy.SorceryPractice.ensureBirth(p);CursedEnergy.set(p,0);
        }
        refresh(p);send(p);com.jjkcraft.energy.EnergySync.markDirty(p);
    }
    public static void init() {
        Awakening.addResetHook(p->set(p,TraitState.EMPTY));
        Awakening.addHook((p,heavenly)->ensure(p));
        SorcererLifecycle.addFullSync(p->{ensure(p);refresh(p);send(p);});
        ServerPlayConnectionEvents.JOIN.register((handler,sender,server)->{ for(var p:server.getPlayerList().getPlayers()) send(p); });
        CursedEnergy.addCapacityModifier((e,max)->e instanceof Player p?max*definitions(p).stream().mapToDouble(TraitDefinition::capacity).reduce(1,(a,b)->a*b):max);
        CursedEnergy.addRegenModifier(e->e instanceof Player p?definitions(p).stream().mapToDouble(TraitDefinition::regen).reduce(1,(a,b)->a*b):1);
        com.jjkcraft.damage.DamageModifiers.add((level,target,source,amount)->{
            if(source.is(net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK) && source.getEntity() instanceof ServerPlayer p && has(p,"electric_energy"))return amount+1.5f;
            return amount;
        });
        ServerTickEvents.END_SERVER_TICK.register(server->{ if(server.getTickCount()%40==0) for(var p:server.getPlayerList().getPlayers()){ refresh(p);send(p); } });
    }
    public static void ensure(ServerPlayer p) {
        if(!Progression.get(p).awakened()) return;
        var current=get(p); var traits=new ArrayList<>(current.traits());
        if(!current.rolled()) {
            boolean heavenly=Progression.get(p).heavenlyRestricted();
            var registry=p.registryAccess().lookupOrThrow(DEFINITIONS);
            var options=registry.keySet().stream().sorted().filter(id->{var d=registry.getValue(id); return d.weight()>0 && (heavenly?d.category().startsWith("restriction_"):d.category().equals("physical"));}).toList();
            String path=CharacterAwakening.get(p).path();
            boolean newCharacter=List.of("inherit","random","choose","rebirth").contains(path) || (heavenly && path.equals("pending"));
            if(newCharacter && (heavenly || p.getRandom().nextInt(100)<35)) {
                int pick=WeightedRoll.index(options.stream().map(id->registry.getValue(id).weight()).toList(),p.getRandom().nextDouble());
                if(pick>=0) traits.add(options.get(pick));
            }
        }
        if(CharacterAwakening.get(p).sixEyes() && CharacterAwakening.get(p).clan().equals("Gojo") && !traits.contains(JjkCraft.id("six_eyes"))) traits.add(JjkCraft.id("six_eyes"));
        var next=new TraitState(true,List.copyOf(traits),current.form(),current.hostTechniques(),current.hostHealth());
        if(!next.equals(current)) set(p,next);
    }
    public static List<TraitDefinition> definitions(Player p) {
        var registry=p.registryAccess().lookup(DEFINITIONS);
        return registry.map(r->get(p).traits().stream().map(r::getValue).filter(Objects::nonNull).toList()).orElse(List.of());
    }
    public static void refresh(ServerPlayer p) {
        var defs=definitions(p);
        apply(p,Attributes.MOVEMENT_SPEED,"speed",defs.stream().mapToDouble(TraitDefinition::speed).sum(),AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        double bodyBonus=get(p).form().isEmpty() || has(p,"vengeful_spirit")?0:get(p).hostHealth()-20;
        apply(p,Attributes.MAX_HEALTH,"health",defs.stream().mapToDouble(TraitDefinition::health).sum()+bodyBonus,AttributeModifier.Operation.ADD_VALUE);
        apply(p,Attributes.ATTACK_DAMAGE,"damage",defs.stream().mapToDouble(TraitDefinition::damage).sum(),AttributeModifier.Operation.ADD_VALUE);
        apply(p,Attributes.ARMOR,"armor",defs.stream().mapToDouble(TraitDefinition::armor).sum(),AttributeModifier.Operation.ADD_VALUE);
        if(p.getHealth()>p.getMaxHealth()) p.setHealth(p.getMaxHealth());
    }
    private static void apply(ServerPlayer p,net.minecraft.core.Holder<Attribute> type,String name,double value,AttributeModifier.Operation operation) {
        var attribute=p.getAttribute(type);if(attribute==null)return;var id=JjkCraft.id("trait/"+name);
        if(value==0) attribute.removeModifier(id); else attribute.addOrUpdateTransientModifier(new AttributeModifier(id,value,operation));
    }
    public static void send(ServerPlayer p) {
        var payload=new TraitPayload(p.getId(),get(p));
        for(var viewer:p.level().getServer().getPlayerList().getPlayers()) if(viewer==p || (viewer.level()==p.level() && viewer.distanceToSqr(p)<128*128)) JjkNetworking.send(viewer,payload);
    }
    public static void spirit(ServerPlayer p,String form) {
        set(p,new TraitState(true,List.of(JjkCraft.id("vengeful_spirit")),form,List.of()));
    }
}
