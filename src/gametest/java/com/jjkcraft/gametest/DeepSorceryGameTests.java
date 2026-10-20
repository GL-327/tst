package com.jjkcraft.gametest;
import java.util.List;
import com.jjkcraft.JjkCraft;
import com.jjkcraft.awakening.*;
import com.jjkcraft.energy.*;
import com.jjkcraft.sorcerer.*;
import com.jjkcraft.technique.*;
import com.jjkcraft.technique.behavior.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.GameType;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
public final class DeepSorceryGameTests {
    @GameTest(environment="jjkcraft:expansion") public void creativeSelectionIsAuthorizedAndFullyMaximized(GameTestHelper h){
        var p=TestPlayers.fresh(h);p.setGameMode(GameType.SURVIVAL);
        h.assertFalse(CharacterAwakening.creative(p,JjkCraft.id("limitless"),false),"survival cannot spoof creative grants");
        p.setGameMode(GameType.CREATIVE);h.assertTrue(CharacterAwakening.creative(p,JjkCraft.id("limitless"),false),"creative can select");
        h.assertTrue(Progression.get(p).grade().equals(JjkCraft.id("calamity"))&&Progression.get(p).growth().total()==80,"Calamity and maximum growth");
        var f=CharacterAwakening.family(p).orElseThrow();
        for(var id:f.techniques())h.assertTrue(TechniqueService.knows(p,id)&&TechniqueService.get(p).masteryOf(id)>=Techniques.get(p.registryAccess(),id).orElseThrow().masteryUses(),"every family move mastered");
        h.assertTrue(TechniqueService.get(p).loadout().size()==12&&com.jjkcraft.domain.DomainService.state(p).known().isPresent(),"twelve slots and matching domain");
        h.assertTrue(CharacterTraits.has(p,"six_eyes"),"Gojo attribute granted");h.succeed();
    }
    @GameTest(environment="jjkcraft:expansion") public void mechamaruStartsEmptyAndBankHasNoGameplayCap(GameTestHelper h){
        var p=TestPlayers.fresh(h);Awakening.awaken(p,true);
        CharacterTraits.set(p,new TraitState(true,List.of(JjkCraft.id("mechamaru_restriction")),"",List.of()));
        h.assertTrue(CursedEnergy.uncapped(p)&&CursedEnergy.current(p)==0&&!Progression.get(p).heavenlyRestricted(),"frail-body restriction starts empty and permits CE");
        CursedEnergy.restore(p,2_000_000);h.assertTrue(CursedEnergy.current(p)>CursedEnergy.max(p)*10,"storage exceeds ordinary capacity");
        CursedEnergy.fill(p);h.assertTrue(CursedEnergy.current(p)<2_000_001,"grade/fill cannot conjure unlimited CE");
        h.assertTrue(CursedEnergy.trySpend(p,1_000_000)&&CursedEnergy.current(p)>999999,"large stored bank remains spendable");h.succeed();
    }
    @GameTest(environment="jjkcraft:expansion") public void independentRegenerationRollIsPersistent(GameTestHelper h){
        var p=TestPlayers.fresh(h);SorceryPractice.ensureBirth(p);var before=SorceryPractice.get(p);SorceryPractice.ensureBirth(p);
        h.assertTrue(before.equals(SorceryPractice.get(p))&&before.regenLevel()>=1&&before.regenLevel()<=10,"one persistent regen roll");
        Awakening.awaken(p,false);var next=TestPlayers.fresh(h);SorcererLifecycle.copyOnDeath(p,next);Awakening.reset(next);
        h.assertTrue(SorceryPractice.get(next).regenLevel()==before.regenLevel()&&SorceryPractice.get(next).birth()==before.birth(),"death and respec cannot reroll potential");h.succeed();
    }
    @GameTest(environment="jjkcraft:expansion") public void deliveryControlsChangeMechanicsAndPrices(GameTestHelper h){
        var raw=Techniques.all(h.getLevel().registryAccess()).stream().map(Techniques.Entry::definition).filter(d->d.behavior() instanceof ProjectileBehavior).findFirst().orElseThrow();
        var tuning=TechniqueTuning.DEFAULT.with(6,175).with(7,6).with(8,30).with(9,2).with(10,75).with(12,25).with(14,1).with(15,0xD055BB).named("Arthur's expression");
        var def=tuning.apply(raw);var b=(ProjectileBehavior)def.behavior();
        h.assertTrue(b.count()==6&&b.homing()&&b.pierce()&&b.spread()==30,"delivery edits are real");
        h.assertTrue(def.cast().castTime()==25&&def.visuals().color()==0xD055BB&&def.name().equals("Arthur's expression"),"charge, color and identity");
        h.assertTrue(Double.isFinite(def.cast().cost())&&def.cast().cost()>0,"finite combined price");
        var decoded=TechniqueTuning.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,TechniqueTuning.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,tuning).getOrThrow()).getOrThrow();
        h.assertTrue(decoded.equals(tuning),"all advanced settings persist");h.succeed();
    }
    @GameTest(environment="jjkcraft:expansion") public void targetFiltersActuallyExcludeOtherClasses(GameTestHelper h){
        var p=TestPlayers.fresh(h);Awakening.awaken(p,false);var id=JjkCraft.id("ink_iron_palm");TechniqueService.grant(p,id);
        Personalization.save(p,id,TechniqueTuning.DEFAULT.with(14,1));
        var ctx=new CastContext(p,h.getLevel(),id,Techniques.get(p.registryAccess(),id).orElseThrow(),1,1);
        var other=TestPlayers.fresh(h);h.assertFalse(ctx.isEnemy(other),"curse-only targeting cannot hit a player");h.succeed();
    }
}
