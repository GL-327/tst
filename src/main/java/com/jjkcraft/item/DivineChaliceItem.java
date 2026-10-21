package com.jjkcraft.item;

import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import com.jjkcraft.awakening.*;
import com.jjkcraft.energy.*;
import com.jjkcraft.sorcerer.*;
import com.jjkcraft.technique.*;

/** Endgame one-use technique rebirth. Body, grade, training and universal skills are preserved. */
public final class DivineChaliceItem extends Item {
    public DivineChaliceItem(Properties properties){super(properties);}
    @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
        if(!(player instanceof ServerPlayer p))return InteractionResult.PASS;
        if(!p.isShiftKeyDown()){p.sendSystemMessage(Component.literal("Sneak and use to consume the Divine Chalice. All techniques, mastery, expressions, summons and your domain are replaced."),true);return InteractionResult.FAIL;}
        if(!reroll(p))return InteractionResult.FAIL;
        p.getItemInHand(hand).shrink(1);return InteractionResult.SUCCESS;
    }
    public static boolean reroll(ServerPlayer p){
        String phase=com.jjkcraft.death.SoulSystem.state(p).phase();
        if(!p.isAlive()||p.isSpectator()||!Progression.get(p).awakened()||(!phase.equals("alive")&&!phase.equals("spirit")))return false;
        var registry=p.registryAccess().lookupOrThrow(CharacterAwakening.FAMILIES);
        var originalIdentity=CharacterAwakening.get(p);String previous=originalIdentity.family();
        if(registry.keySet().stream().noneMatch(id->!id.toString().equals(previous)))return false;
        var progress=Progression.get(p);var practice=SorceryPractice.get(p);var traits=CharacterTraits.get(p);var reverse=com.jjkcraft.reverse.ReverseTechnique.state(p);double energy=CursedEnergy.current(p);float health=p.getHealth();
        int reverseLessons=p.getAttachedOrCreate(com.jjkcraft.reverse.ReverseStudy.LESSON);
        TechniqueService.cancel(p);
        for(var pet:com.jjkcraft.entity.shikigami.ShikigamiEntity.ownedBy(p.level(),com.jjkcraft.entity.shikigami.ShikigamiEntity.class,p))pet.dissolve(p.level());
        for(var curse:p.level().getEntitiesOfClass(com.jjkcraft.curse.CurseEntity.class,p.getBoundingBox().inflate(64),e->e.getAttachedOrCreate(ControlledCurses.OWNER).equals(p.getUUID().toString())))curse.discard();
        Awakening.reset(p);
        if(!CharacterAwakening.rebirth(p,previous))throw new IllegalStateException("Validated family pool was empty");
        // Physical restriction remains a body choice, independent of the new innate family.
        Progression.set(p,progress);SorceryPractice.set(p,practice);CharacterTraits.set(p,new TraitState(traits.rolled(),traits.traits(),traits.form(),List.of(),traits.hostHealth()));
        var record=CharacterAwakening.get(p);p.setAttached(CharacterAwakening.RECORD,new AwakeningRecord("rebirth",record.family(),originalIdentity.clan(),traits.traits().contains(com.jjkcraft.JjkCraft.id("six_eyes")),record.tick()));
        com.jjkcraft.reverse.ReverseTechnique.set(p,reverse,false);
        p.setAttached(com.jjkcraft.reverse.ReverseStudy.LESSON,reverseLessons);
        CharacterAwakening.unlock(p);com.jjkcraft.domain.DomainService.grantIfEligible(p);CursedEnergy.set(p,energy);p.setHealth(Math.min(health,p.getMaxHealth()));
        SorcererLifecycle.fullSync(p);p.sendSystemMessage(Component.literal("The Divine Chalice replaced your technique. Your old mastery, bindings, expressions, captured spirits and domain are gone."));return true;
    }
}
