package com.jjkcraft.gametest;

import java.util.*;
import com.jjkcraft.JjkCraft;
import com.jjkcraft.awakening.CharacterAwakening;
import com.jjkcraft.energy.*;
import com.jjkcraft.reverse.*;
import com.jjkcraft.technique.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

public final class WheelReverseGameTests {
    @GameTest(environment="jjkcraft:expansion",padding=40) public void bluePunchHitsAtContactAndClusterPlacesThreeSeparateFields(GameTestHelper h){
        var p=player(h);var target=h.spawn(EntityTypes.HUSK,new Vec3(2.5,1,5));target.setNoAi(true);target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(1000);target.setHealth(1000);
        h.assertTrue(TechniqueService.cast(p,JjkCraft.id("blue_punch"),-1)==CastFailure.NONE&&target.getHealth()<1000,"the aimed Blue punch actually damages at close range");h.assertTrue(target.getDeltaMovement().z<0,"Blue punch attracts toward the caster");
        h.assertTrue(TechniqueService.cast(p,JjkCraft.id("blue_cluster"),-1)==CastFailure.NONE,"cluster activates immediately");h.assertTrue(com.jjkcraft.technique.field.SpatialFields.active(h.getLevel()).stream().filter(f->f.caster()==p).count()==3,"cluster creates three independent attraction points");h.succeed();
    }
    @GameTest(environment="jjkcraft:expansion",padding=32) public void forsakingRctCannotKeepFreeInfinityOrPositiveDischarge(GameTestHelper h){
        var p=player(h);ReverseTechnique.set(p,ReverseTechnique.state(p).withUnlocked(true),false);
        var pact=TechniqueTuning.DEFAULT.withPact(new TechniquePact(1<<TechniquePact.HEALING));h.assertTrue(Personalization.save(p,JjkCraft.id("blue_punch"),pact),"non-healing power may forsake RCT");
        h.assertTrue(!ReverseTechnique.available(p)&&InfinityField.upkeep(p,Techniques.get(p.registryAccess(),JjkCraft.id("infinity")).orElseThrow())>0,"vow disables free Infinity upkeep");h.assertTrue(TechniqueService.cast(p,JjkCraft.id("positive_energy_discharge"),-1)==CastFailure.VOW,"vow blocks offensive positive energy too");h.assertTrue(ReverseTechnique.burnout(p,200)==200,"no free brain recovery while RCT is forbidden");h.succeed();
    }
    @GameTest(environment="jjkcraft:expansion",padding=32) public void creativeEditsPreserveValidatedFamilyBindings(GameTestHelper h){
        var p=player(h);p.setGameMode(GameType.CREATIVE);var binding=TechniqueBindings.binding(JjkCraft.id("limitless"));TechniqueService.setLoadout(p,0,Optional.of(binding));
        h.assertTrue(com.jjkcraft.awakening.CreativeCharacters.apply(p,JjkCraft.id("limitless"),com.jjkcraft.awakening.CreativeCharacters.snapshot(p)),"creative editor accepts a valid family selector");h.assertTrue(TechniqueService.get(p).slot(0).filter(binding::equals).isPresent(),"binding survives creative edits");h.succeed();
    }
    @GameTest(environment="jjkcraft:expansion",padding=32) public void usableDispelWeaponBreaksInfinityOnPhysicalContact(GameTestHelper h){
        var p=player(h);var attacker=player(h);attacker.setGameMode(GameType.CREATIVE);attacker.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(JjkCraft.id("sealbreaker_tanto"))));
        TechniqueService.cast(p,JjkCraft.id("infinity"),-1);float damage=com.jjkcraft.damage.DamageModifiers.apply(h.getLevel(),p,h.getLevel().damageSources().playerAttack(attacker),8);h.assertTrue(damage>0&&!InfinityField.on(p),"an eligible dispelling tool cancels the passive instead of being stopped forever");h.succeed();
    }
    @GameTest(environment="jjkcraft:expansion",padding=32) public void divineChalicePreservesCompletedUniversalReverseLessons(GameTestHelper h){
        var p=player(h);p.setAttached(ReverseStudy.LESSON,1);h.assertTrue(com.jjkcraft.item.DivineChaliceItem.reroll(p),"rare chalice replaces the innate family");h.assertTrue(ReverseStudy.completed(p)==1&&!ReverseTechnique.unlocked(p),"completed universal learning remains, without granting RCT");h.succeed();
    }
    private static ServerPlayer player(GameTestHelper h){var p=TestPlayers.fresh(h);p.setGameMode(GameType.CREATIVE);CharacterAwakening.creative(p,JjkCraft.id("limitless"),false);p.setGameMode(GameType.SURVIVAL);p.setPos(h.absoluteVec(new Vec3(2.5,1,2.5)));p.setYRot(0);p.setXRot(0);p.setOnGround(true);ReverseTechnique.set(p,ReverseTechnique.state(p).withUnlocked(false).withAutoHeal(false),false);CursedEnergy.fill(p);return p;}
    @GameTest(environment="jjkcraft:expansion",padding=32) public void familyBindingExposesOnlyLearnedChildrenAndAcceptsRebinding(GameTestHelper h){
        var p=player(h);var family=TechniqueBindings.binding(JjkCraft.id("limitless"));TechniqueService.setLoadout(p,0,Optional.empty());TechniqueService.setLoadout(p,0,Optional.of(family));
        h.assertTrue(TechniqueService.get(p).slot(0).orElseThrow().equals(family),"empty slot accepts family binding");
        TechniqueService.revoke(p,JjkCraft.id("hollow_purple"));h.assertFalse(TechniqueBindings.moves(p.registryAccess(),TechniqueService.get(p),family).contains(JjkCraft.id("hollow_purple")),"unlearned Purple omitted");
        double ce=CursedEnergy.current(p);TechniqueService.onVariant(p,0,JjkCraft.id("hollow_purple"));h.assertTrue(TechniqueService.chargePercent(p)==0&&CursedEnergy.current(p)==ce,"forged variant rejected before CE spend");
        TechniqueService.onVariant(p,0,JjkCraft.id("lapse_blue"));h.assertTrue(TechniqueService.chargePercent(p)>0,"learned child starts normal charge pipeline");TechniqueService.onCastKey(p,0,false);h.assertTrue(TechniqueService.chargePercent(p)==0,"release settles family charge");
        TechniqueService.setLoadout(p,0,Optional.of(JjkCraft.id("blue_punch")));h.assertTrue(TechniqueService.get(p).slot(0).filter(JjkCraft.id("blue_punch")::equals).isPresent(),"specific variant may replace selector");h.succeed();
    }
    @GameTest(environment="jjkcraft:expansion",padding=32,maxTicks=55) public void infinityDrainsWithoutRctAndCoexistsWithOtherChannels(GameTestHelper h){
        var p=player(h);TechniqueService.cast(p,JjkCraft.id("infinity"),-1);double before=CursedEnergy.current(p);
        TechniqueService.grant(p,JjkCraft.id("lacquer_ward"));TechniqueService.cast(p,JjkCraft.id("lacquer_ward"),-1);h.assertTrue(InfinityField.on(p)&&TechniqueService.active(p).isPresent(),"passive survives a separate channel");TechniqueService.cancelKey(p,-1);
        h.runAfterDelay(40,()->{h.assertTrue(InfinityField.on(p)&&CursedEnergy.current(p)<before,"passive consumes CE slowly without natural refill");ReverseTechnique.set(p,ReverseTechnique.state(p).withUnlocked(true),false);h.assertTrue(InfinityField.upkeep(p,Techniques.get(p.registryAccess(),JjkCraft.id("infinity")).orElseThrow())==0,"learned RCT removes upkeep");TechniqueService.cast(p,JjkCraft.id("infinity"),-1);h.assertFalse(InfinityField.on(p),"explicit recast toggles passive off");h.succeed();});
    }
    @GameTest(environment="jjkcraft:expansion",padding=32,maxTicks=8) public void infinityDropsWhenEnergyIsExhausted(GameTestHelper h){var p=player(h);TechniqueService.cast(p,JjkCraft.id("infinity"),-1);CursedEnergy.set(p,.001);h.runAfterDelay(3,()->{h.assertFalse(InfinityField.on(p),"no CE means no passive protection");h.succeed();});}
    @GameTest(environment="jjkcraft:expansion",padding=32) public void infinityStopsIncomingAttacksButCannotStopSureHitsOrHazards(GameTestHelper h){
        var p=player(h);TechniqueService.cast(p,JjkCraft.id("infinity"),-1);var enemy=h.spawn(EntityTypes.HUSK,new Vec3(2.5,1,4));enemy.setNoAi(true);
        h.assertTrue(com.jjkcraft.damage.DamageModifiers.apply(h.getLevel(),p,h.getLevel().damageSources().mobAttack(enemy),8)==0,"contact stopped by spatial field");
        var sure=new net.minecraft.world.damagesource.DamageSource(h.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE).getOrThrow(com.jjkcraft.damage.JjkDamageTypes.SURE_HIT),enemy);
        h.assertTrue(com.jjkcraft.damage.DamageModifiers.apply(h.getLevel(),p,sure,8)>0,"domain sure-hit bypasses infinity");h.assertTrue(com.jjkcraft.damage.DamageModifiers.apply(h.getLevel(),p,h.getLevel().damageSources().generic(),8)>0,"environmental harm remains");h.succeed();
    }
    @GameTest(environment="jjkcraft:expansion",padding=32) public void rctRequiresThreeCompletedDrillsAndDoesNotChangeFamilyBindings(GameTestHelper h){
        var p=player(h);p.experienceLevel=9;var before=TechniqueService.get(p);var family=before.innate();var slots=before.loadout();
        for(int lesson=0;lesson<3;lesson++){h.assertTrue(ReverseStudy.start(p),"lesson starts");for(int tick=0;tick<ReverseStudy.LESSON_TICKS;tick++)ReverseStudy.step(p);h.assertTrue(ReverseStudy.completed(p)==lesson+1,"completed lesson persists");}
        h.assertTrue(ReverseTechnique.unlocked(p)&&p.experienceLevel==0,"three completed lessons cost exactly nine levels");h.assertFalse(ReverseTechnique.state(p).autoHeal(),"healing starts opt-in");h.assertTrue(TechniqueService.get(p).innate().equals(family)&&TechniqueService.get(p).loadout().equals(slots),"learning preserves family and bindings");h.assertTrue(TechniqueService.knows(p,JjkCraft.id("positive_energy_discharge")),"positive output learned with RCT");h.succeed();
    }
    @GameTest(environment="jjkcraft:expansion",padding=32) public void movingInterruptsRctDrillWithoutGrantingProgressOrSpendingExperience(GameTestHelper h){var p=player(h);p.experienceLevel=3;h.assertTrue(ReverseStudy.start(p),"drill starts");p.setPos(p.position().add(1,0,0));ReverseStudy.step(p);h.assertTrue(!ReverseStudy.running(p)&&ReverseStudy.completed(p)==0&&p.experienceLevel==3,"movement cancels with no lesson or XP loss");h.succeed();}
    @GameTest(environment="jjkcraft:expansion",padding=32) public void positiveEnergyRequiresRctAndDamagesCursesOnly(GameTestHelper h){
        var p=player(h);var id=JjkCraft.id("positive_energy_discharge");double before=CursedEnergy.current(p);h.assertTrue(TechniqueService.cast(p,id,-1)==CastFailure.NOT_KNOWN&&CursedEnergy.current(p)==before,"cannot fake RCT output");ReverseTechnique.set(p,ReverseTechnique.state(p).withUnlocked(true),false);
        var curse=h.spawn(com.jjkcraft.curse.CurseEntities.HOLLOW_HYMNAL,new Vec3(2.5,1,5));curse.setNoAi(true);float hp=curse.getHealth();var human=h.spawn(EntityTypes.HUSK,new Vec3(2.5,1,4));human.setNoAi(true);float humanHp=human.getHealth();
        h.assertTrue(TechniqueService.cast(p,id,-1)==CastFailure.NONE,"positive output casts");h.assertTrue(curse.getHealth()<hp,"positive energy exorcises curse");h.assertTrue(human.getHealth()==humanHp,"living non-curse unaffected");h.succeed();
    }
}
