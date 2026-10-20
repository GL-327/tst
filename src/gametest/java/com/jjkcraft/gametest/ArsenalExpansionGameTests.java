package com.jjkcraft.gametest;

import java.util.*;
import com.jjkcraft.JjkCraft;
import com.jjkcraft.technique.*;
import com.jjkcraft.energy.CursedEnergy;
import com.jjkcraft.sorcerer.*;
import com.jjkcraft.combat.CursedCombat;
import com.jjkcraft.curse.*;
import com.jjkcraft.network.payload.CombatPayloads;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.phys.Vec3;

public final class ArsenalExpansionGameTests {
    private static net.minecraft.server.level.ServerPlayer player(GameTestHelper h){var p=TestPlayers.fresh(h);Awakening.awaken(p,false);Progression.setGrade(p,JjkCraft.id("calamity"));CursedEnergy.fill(p);return p;}
    @GameTest(environment="jjkcraft:expansion") public void twelveSlotsExistAtStartingGradeAndTheLastCanBeRebound(GameTestHelper h){
        var p=player(h);Progression.setGrade(p,JjkCraft.id("grade_4"));var id=JjkCraft.id("grudge_bolt");TechniqueService.grant(p,id);
        h.assertTrue(TechniqueService.usableSlots(p)==12,"twelve slots from birth");
        TechniqueService.setLoadout(p,11,Optional.of(id));TechniqueService.setLoadout(p,11,Optional.empty());TechniqueService.setLoadout(p,11,Optional.of(id));
        h.assertTrue(TechniqueService.get(p).slot(11).filter(id::equals).isPresent(),"last slot refills");h.succeed();
    }
    @GameTest(environment="jjkcraft:expansion") public void combatVowQuintuplesOnePowerAndCannotBeEvadedByUnbinding(GameTestHelper h){
        var p=player(h);var id=JjkCraft.id("grudge_bolt");TechniqueService.grant(p,id);var raw=Techniques.get(p.registryAccess(),id).orElseThrow();
        double ordinary=TechniqueService.power(p,id,raw,TechniqueService.modifiers(p,id,raw));h.assertTrue(Personalization.save(p,id,TechniqueTuning.DEFAULT.withPact(TechniquePact.NONE.toggle(0))),"vow saved");
        h.assertTrue(Math.abs(TechniqueService.power(p,id,Personalization.definition(p,id,raw),TechniqueService.modifiers(p,id,raw))/ordinary-5)<1e-6,"exact fivefold output");
        TechniqueService.setLoadout(p,0,Optional.empty());h.assertFalse(CursedCombat.start(p,JjkCraft.id("jab"),new Vec3(0,0,1)),"unbinding does not restore melee");
        var dummy=h.spawn(EntityTypes.HUSK,new Vec3(1,1,1));dummy.setNoAi(true);
        h.assertTrue(com.jjkcraft.damage.DamageModifiers.apply(h.getLevel(),dummy,h.getLevel().damageSources().playerAttack(p),8)==0,"ordinary damage is forbidden too");h.succeed();
    }
    @GameTest(environment="jjkcraft:expansion") public void castSealsVowsAndSurvivesTuningRoundTrip(GameTestHelper h){
        var p=player(h);var id=JjkCraft.id("grudge_bolt");TechniqueService.grant(p,id);var tuning=TechniqueTuning.DEFAULT.withPact(TechniquePact.NONE.toggle(0).toggle(8));Personalization.save(p,id,tuning);
        h.assertTrue(TechniqueService.cast(p,id,0)==CastFailure.NONE,"vowed cast succeeds");h.assertFalse(Personalization.save(p,id,TechniqueTuning.DEFAULT),"cannot shed vow after casting");
        var saved=TechniqueTuning.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,tuning).getOrThrow();h.assertTrue(TechniqueTuning.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,saved).getOrThrow().equals(tuning),"vows persist");
        h.assertTrue(p.getAttachedOrCreate(PersonalPacts.SEALED)>h.getLevel().getGameTime()+1190,"one minute seal");h.succeed();
    }
    @GameTest(environment="jjkcraft:expansion") public void solePowerVowRejectsOtherPowersAndConflictingOaths(GameTestHelper h){
        var p=player(h);var a=JjkCraft.id("grudge_bolt");var b=JjkCraft.id("ink_step");TechniqueService.grant(p,a);TechniqueService.grant(p,b);
        Personalization.save(p,a,TechniqueTuning.DEFAULT.withPact(TechniquePact.NONE.toggle(3)));
        h.assertTrue(TechniqueService.cast(p,b,0)==CastFailure.VOW,"other power forbidden");h.assertFalse(Personalization.save(p,b,TechniqueTuning.DEFAULT.withPact(TechniquePact.NONE.toggle(3))),"two sole-power promises are rejected");h.succeed();
    }
    @GameTest(environment="jjkcraft:expansion") public void openGuardAndGroundedPactsForbidTheirActions(GameTestHelper h){
        var p=player(h);var id=JjkCraft.id("grudge_bolt");TechniqueService.grant(p,id);Personalization.save(p,id,TechniqueTuning.DEFAULT.withPact(TechniquePact.NONE.toggle(1).toggle(2).toggle(5)));
        CursedCombat.input(p,new CombatPayloads.Input(2,1));var attacker=h.spawn(EntityTypes.HUSK,new Vec3(0,1,2));attacker.setNoAi(true);
        p.setPos(attacker.getX(),attacker.getY(),attacker.getZ()-2);p.setYRot(0);p.setXRot(0);
        h.assertTrue(com.jjkcraft.damage.DamageModifiers.apply(h.getLevel(),p,h.getLevel().damageSources().mobAttack(attacker),8)>0,"cannot guard");
        TechniqueService.grant(p,JjkCraft.id("ink_step"));h.assertTrue(TechniqueService.cast(p,JjkCraft.id("ink_step"),0)==CastFailure.VOW,"no dash powers");
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.SHIELD));
        h.assertTrue(net.fabricmc.fabric.api.event.player.UseItemCallback.EVENT.invoker().interact(p,h.getLevel(),net.minecraft.world.InteractionHand.MAIN_HAND)==net.minecraft.world.InteractionResult.FAIL,"shield cannot bypass open guard");
        h.assertFalse(com.jjkcraft.reverse.ReverseTechnique.saveFromDeath(p),"no emergency RCT save");h.succeed();
    }
    @GameTest(environment="jjkcraft:expansion",maxTicks=60) public void nativeHostileMeleeStartsWithWindupInsteadOfInstantDamage(GameTestHelper h){
        var zombie=h.spawn(EntityTypes.ZOMBIE,new Vec3(1,1,1));zombie.setNoAi(true);var target=h.spawn(EntityTypes.COW,new Vec3(1,1,3));target.setNoAi(true);float hp=target.getHealth();
        zombie.doHurtTarget(h.getLevel(),target);h.assertTrue(target.getHealth()==hp&&CursedCombat.attacking(zombie),"contact delayed until active frame");
        h.startSequence().thenWaitUntil(()->h.assertTrue(target.getHealth()<hp,"framed hit lands")).thenSucceed();
    }
    @GameTest(environment="jjkcraft:expansion",maxTicks=60) public void rapidManualInputsQueueAnUppercutAndHeavyDoubleTapChoosesSweep(GameTestHelper h){
        var p=player(h);CursedCombat.input(p,new CombatPayloads.Input(0,0));CursedCombat.input(p,new CombatPayloads.Input(0,0));
        h.startSequence().thenWaitUntil(()->h.assertTrue(CursedCombat.currentMove(p).equals("uppercut"),"double light queues uppercut"))
            .thenIdle(20).thenExecute(()->{CursedCombat.input(p,new CombatPayloads.Input(5,0));CursedCombat.input(p,new CombatPayloads.Input(5,0));})
            .thenWaitUntil(()->h.assertTrue(CursedCombat.currentMove(p).equals("sweep"),"double heavy selects sweep")).thenSucceed();
    }
    @GameTest(environment="jjkcraft:expansion") public void newFamiliesHaveUsableStartersAndEachScrollRecipeExists(GameTestHelper h){
        for(String f:List.of("electrification","granite_discharge","love_rendezvous","explosive_flesh","tool_manipulation","echo_ledger","mercury_mirror","mourning_thread")){
            var family=h.getLevel().registryAccess().lookupOrThrow(com.jjkcraft.awakening.CharacterAwakening.FAMILIES).getValue(JjkCraft.id(f));
            h.assertTrue(family!=null&&family.techniques().size()>=4,"at least four moves in "+f);
            var first=Techniques.get(h.getLevel().registryAccess(),family.techniques().getFirst()).orElseThrow();h.assertTrue(com.jjkcraft.grade.Grades.rank(h.getLevel().registryAccess(),first.grade())==0,"grade-four starter");
            for(var id:family.techniques())h.assertTrue(h.getLevel().getServer().getRecipeManager().byKey(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE,JjkCraft.id("scroll_"+id.getPath()))).isPresent(),"scroll recipe "+id);
        }h.succeed();
    }
    @GameTest(environment="jjkcraft:expansion") public void twelveAdditionalLowGradeSpeciesAreRegisteredWithDistinctResources(GameTestHelper h){
        var all=Curses.all(h.getLevel().registryAccess());h.assertTrue(all.size()>=52,"fifty-two curse species");
        for(String key:List.of("paper_tick","gutter_grub","chalk_biter","static_mite","whisper_knot","dustling","damp_effigy","porcelain_nipper","rustjaw_pup","drain_skitter","needle_cricket","worry_doll")){
            var d=Curses.get(h.getLevel().registryAccess(),JjkCraft.id(key)).orElseThrow();h.assertTrue(com.jjkcraft.grade.Grades.rank(h.getLevel().registryAccess(),d.grade())<=1,"low grade");
            h.assertTrue(d.model().geo().getPath().endsWith(key)&&d.combatMoves().size()>1,"dedicated art and combat kit");
            var type=net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getValue(d.entity());h.assertTrue(type!=EntityTypes.PIG,"real entity registered");
        }h.succeed();
    }
    @GameTest(environment="jjkcraft:expansion") public void electricalMarksArePrivateAndConsumedByDischarge(GameTestHelper h){
        var a=player(h);var b=player(h);var target=h.spawn(EntityTypes.HUSK,new Vec3(1,1,3));target.setNoAi(true);a.setPos(target.getX(),target.getY(),target.getZ()-2);a.setYRot(0);a.setXRot(0);
        var id=JjkCraft.id("charge_transfer");var d=Techniques.get(h.getLevel().registryAccess(),id).orElseThrow();var ctx=new CastContext(a,h.getLevel(),id,d,1,1);d.behavior().cast(ctx);
        h.assertTrue(ExpressionArts.charges(a,target,"charge")==1&&ExpressionArts.charges(b,target,"charge")==0,"marks owned by caster");
        var release=Techniques.get(h.getLevel().registryAccess(),JjkCraft.id("lightning_discharge")).orElseThrow();release.behavior().cast(new CastContext(a,h.getLevel(),JjkCraft.id("lightning_discharge"),release,1,1));
        h.assertTrue(ExpressionArts.charges(a,target,"charge")==0,"discharge spends mark");h.succeed();
    }
}
