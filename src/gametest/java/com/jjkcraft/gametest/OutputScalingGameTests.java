package com.jjkcraft.gametest;
import java.util.*;
import com.jjkcraft.JjkCraft;
import com.jjkcraft.awakening.*;
import com.jjkcraft.energy.*;
import com.jjkcraft.sorcerer.*;
import com.jjkcraft.technique.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
public final class OutputScalingGameTests {
    private static ServerPlayer veteran(GameTestHelper h){var p=TestPlayers.fresh(h);p.setGameMode(GameType.CREATIVE);CharacterAwakening.creative(p,JjkCraft.id("blood_manipulation"),false);p.setGameMode(GameType.SURVIVAL);return p;}
    @GameTest(environment="jjkcraft:expansion") public void earnedCapsAreEnforcedOnSavedAndEffectiveTuning(GameTestHelper h){
        var p=veteran(h);var id=JjkCraft.id("piercing_blood");var raw=Techniques.get(p.registryAccess(),id).orElseThrow();
        h.assertTrue(Personalization.save(p,id,new TechniqueTuning(3000,600,400,600,100,0)),"veteran may save overdrive");
        h.assertValueEqual(Personalization.get(p,id).output(),3000,"full grade, growth and mastery reach 3000%");
        Progression.set(p,Progression.get(p).withGrade(JjkCraft.id("grade_4"),0).withGrowth(SorcererProgress.Growth.NONE));TechniqueService.set(p,TechniqueService.get(p).withMastery(Map.of()));
        h.assertValueEqual(Personalization.get(p,id).output(),300,"old or creative saves cannot bypass a lower earned ceiling");h.assertTrue(Personalization.get(p,id).reach()<=300,"spatial ceiling is enforced too");
        Personalization.save(p,id,new TechniqueTuning(3000,600,400,600,100,0));h.assertValueEqual(p.getAttachedOrCreate(Personalization.TUNINGS).get(id).output(),300,"oversized packets are capped before persistence");
        for(var e:Techniques.all(p.registryAccess())){var tuned=new TechniqueTuning(3000,600,400,600,25,2).apply(e.definition());h.assertTrue(Double.isFinite(tuned.cast().cost())&&tuned.behavior().type()==e.definition().behavior().type(),"every technique keeps finite costs and its identity: "+e.id());}h.succeed();
    }
    @GameTest(environment="jjkcraft:expansion",padding=32) public void highOutputCastsDealRealDamageAndPayTheLargerCost(GameTestHelper h){
        var p=veteran(h);var id=JjkCraft.id("dismantle");TechniqueService.grant(p,id);var raw=Techniques.get(p.registryAccess(),id).orElseThrow();TechniqueService.set(p,TechniqueService.get(p).withMasteryAdded(id,raw.masteryUses()*4));
        Personalization.save(p,id,TechniqueTuning.DEFAULT.with(0,1000));CursedEnergy.fill(p);double before=CursedEnergy.current(p),cost=TechniqueService.effectiveCost(p,id,Personalization.definition(p,id,raw));
        p.setPos(h.absoluteVec(new Vec3(1.5,1,1.5)));p.setYRot(0);p.setXRot(0);var target=h.spawn(net.minecraft.world.entity.EntityTypes.ZOMBIE,new Vec3(1.5,1,4));target.setNoAi(true);target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(200);target.setHealth(200);
        h.assertValueEqual(TechniqueService.cast(p,id,0),CastFailure.NONE,"paid overdrive casts");h.assertTrue(target.getHealth()<50,"overdrive actually damages the target");h.assertTrue(cost>100&&Math.abs(CursedEnergy.current(p)-(before-cost))<.01,"server charges the larger CE price");
        CursedEnergy.set(p,0);TechniqueService.resetCooldowns(p);h.assertValueEqual(TechniqueService.cast(p,id,0),CastFailure.NOT_ENOUGH_ENERGY,"overdrive cannot cast without energy");h.succeed();
    }
    @GameTest(environment="jjkcraft:expansion") public void reinforcementAndReverseOutputHaveEarnedServerLimits(GameTestHelper h){
        var p=veteran(h);h.assertTrue(SorceryPractice.outputLimit(p)==2700,"universal ceiling uses real control drills, rather than a move's mastery");
        com.jjkcraft.training.Training.award(p,"control",100000);h.assertTrue(SorceryPractice.outputLimit(p)==3000,"control practice opens full universal output");
        SorceryPractice.act(p,new com.jjkcraft.network.payload.PersonalizationPayloads.Practice(4,3000,3000,60));h.assertTrue(SorceryPractice.get(p).output()==3000&&SorceryPractice.get(p).healing()==3000,"universal tuning accepts earned four-digit values");
        p.setGameMode(GameType.CREATIVE);var draft=CreativeCharacters.snapshot(p);var e=draft.energy();var energy=new CreativeSettings.Energy(e.birth(),e.trained(),e.regen(),e.stored(),e.reinforcement(),false,3000,3000,e.threshold(),e.rct(),false);
        var settings=new CreativeSettings(draft.grade(),draft.xp(),draft.growth(),energy,draft.traits(),draft.form(),draft.bodyHealth(),draft.heavenly(),draft.learned(),draft.mastery(),draft.slots(),draft.domain(),draft.special());
        var encoded=CreativeSettings.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,settings).getOrThrow();h.assertTrue(CreativeCharacters.apply(p,JjkCraft.id("blood_manipulation"),CreativeSettings.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,encoded).getOrThrow()),"creative four-digit settings survive codec and server validation");h.succeed();
    }
}
