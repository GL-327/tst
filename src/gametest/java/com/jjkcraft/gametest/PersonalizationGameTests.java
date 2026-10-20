package com.jjkcraft.gametest;
import com.jjkcraft.JjkCraft;
import com.jjkcraft.energy.*;
import com.jjkcraft.technique.*;
import com.jjkcraft.sorcerer.*;
import com.jjkcraft.reverse.ReverseTechnique;
import com.jjkcraft.network.payload.PersonalizationPayloads;
import com.jjkcraft.technique.behavior.*;
import com.mojang.serialization.JsonOps;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;

public final class PersonalizationGameTests {
    private static ServerPlayer sorcerer(GameTestHelper h){var p=TestPlayers.fresh(h);Awakening.awaken(p,false);return p;}
    @GameTest(environment="jjkcraft:expansion") public void everyTechniqueSupportsFiniteIndependentTuning(GameTestHelper h){
        for(var e:Techniques.all(h.getLevel().registryAccess())){
            var raw=e.definition();h.assertTrue(TechniqueTuning.DEFAULT.apply(raw).equals(raw),"defaults preserve "+e.id());
            var tuned=new TechniqueTuning(300,300,200,300,25,2).apply(raw);
            h.assertTrue(Double.isFinite(tuned.cast().cost())&&tuned.cast().cost()>=0,"finite costs "+e.id());
            h.assertTrue(tuned.behavior().type()==raw.behavior().type(),"identity preserved "+e.id());
            h.assertTrue(raw.grade().equals(tuned.grade())&&raw.icon().equals(tuned.icon()),"grade and identity immutable");
        }h.succeed();
    }
    @GameTest(environment="jjkcraft:expansion") public void tuningIsPrivateAndRejectsUnknownMoves(GameTestHelper h){
        var p=sorcerer(h);var other=sorcerer(h);var id=JjkCraft.id("ink_step");TechniqueService.grant(p,id);
        var raw=Techniques.get(p.registryAccess(),id).orElseThrow();var tuning=new TechniqueTuning(100,200,100,100,50,0);
        h.assertTrue(Personalization.save(p,id,tuning),"known move editable");
        var actual=Personalization.definition(p,id,raw);
        h.assertTrue(((DashBehavior)actual.behavior()).distance()==12&&actual.cast().cooldown()==30,"distance and cooldown affect behavior");
        h.assertTrue(actual.cast().cost()>raw.cast().cost(),"shorter cooldown and distance cost CE");
        h.assertTrue(Personalization.get(other,id).equals(TechniqueTuning.DEFAULT),"other players unaffected");
        h.assertFalse(Personalization.save(p,JjkCraft.id("not_a_move"),tuning),"unknown rejected");
        h.assertTrue(TechniqueTuning.CODEC.parse(JsonOps.INSTANCE,TechniqueTuning.CODEC.encodeStart(JsonOps.INSTANCE,tuning).getOrThrow()).getOrThrow().equals(tuning),"save codec round trip");h.succeed();
    }
    @GameTest(environment="jjkcraft:expansion") public void birthReserveSurvivesDeathAndRespec(GameTestHelper h){
        var p=TestPlayers.fresh(h);SorceryPractice.ensureBirth(p);int birth=SorceryPractice.get(p).birth();SorceryPractice.ensureBirth(p);
        h.assertTrue(birth>=100&&birth<=3000&&SorceryPractice.get(p).birth()==birth,"one bounded birth roll");
        Awakening.awaken(p,false);TechniqueService.grant(p,JjkCraft.id("ink_step"));Personalization.save(p,JjkCraft.id("ink_step"),new TechniqueTuning(100,200,100,100,50,0));
        var next=TestPlayers.fresh(h);SorcererLifecycle.copyOnDeath(p,next);
        h.assertTrue(Personalization.get(next,JjkCraft.id("ink_step")).reach()==200,"normal death retains personal moves");
        h.assertTrue(SorceryPractice.get(next).birth()==birth,"normal death retains potential");
        Awakening.reset(next);h.assertTrue(SorceryPractice.get(next).birth()==birth,"respec cannot reroll potential");h.succeed();
    }
    @GameTest(environment="jjkcraft:expansion") public void universalSkillsLearnWithoutChangingFamilyOrLoadout(GameTestHelper h){
        var p=sorcerer(h);var original=TechniqueService.get(p);p.experienceLevel=13;
        h.assertTrue(SorceryPractice.act(p,new PersonalizationPayloads.Practice(0,100,100,60)),"reinforcement teachable");
        h.assertTrue(SorceryPractice.act(p,new PersonalizationPayloads.Practice(1,100,100,60)),"RCT teachable at any sorcerer grade");
        h.assertTrue(p.experienceLevel==0&&SorceryPractice.get(p).reinforcement()&&ReverseTechnique.unlocked(p),"learning costs exactly 13 levels");
        h.assertTrue(TechniqueService.get(p).equals(original),"no innate or four-slot changes");
        h.assertFalse(SorceryPractice.act(p,new PersonalizationPayloads.Practice(2,100,100,60)),"training requires XP");
        p.experienceLevel=30;int gain=SorceryPractice.trainingGain(p);h.assertTrue(SorceryPractice.act(p,new PersonalizationPayloads.Practice(2,100,100,60)),"reserves trainable");
        h.assertTrue(SorceryPractice.get(p).trained()==gain,"training increases capacity according to current potential");h.succeed();
    }
    @GameTest(environment="jjkcraft:expansion") public void reverseTuningChangesHealingAndEnergy(GameTestHelper h){
        var p=sorcerer(h);ReverseTechnique.set(p,ReverseTechnique.state(p).withUnlocked(true),false);p.setHealth(2);CursedEnergy.fill(p);
        SorceryPractice.set(p,SorceryPractice.get(p).tune(100,200,100));double before=CursedEnergy.current(p);ReverseTechnique.healStep(p);
        h.assertTrue(p.getHealth()==4,"twice the healing");h.assertTrue(before-CursedEnergy.current(p)>6,"more output costs more per health");
        var restricted=sorcerer(h);Progression.set(restricted,Progression.get(restricted).withHeavenlyRestriction(true));restricted.experienceLevel=50;
        h.assertFalse(SorceryPractice.act(restricted,new PersonalizationPayloads.Practice(1,100,100,60)),"zero-CE restriction respected");h.succeed();
    }
    @GameTest(environment="jjkcraft:expansion") public void infinityCanSelectContactOrCombinedProtection(GameTestHelper h){
        var raw=Techniques.all(h.getLevel().registryAccess()).stream().map(Techniques.Entry::definition).filter(d->d.behavior() instanceof InfinityBehavior).findFirst().orElseThrow();
        var contact=new TechniqueTuning(200,100,150,100,100,1).apply(raw);var both=new TechniqueTuning(200,100,150,100,100,2).apply(raw);
        h.assertTrue(((InfinityBehavior)contact.behavior()).filter()==1&&((InfinityBehavior)both.behavior()).filter()==2,"chosen filters implemented");
        h.assertTrue(both.cast().upkeep()>contact.cast().upkeep(),"combined protection costs extra");h.succeed();
    }
    @GameTest(environment="jjkcraft:expansion",padding=24) public void tunedTeleportUsesPaidRangeAndCooldown(GameTestHelper h){
        var p=sorcerer(h);var id=JjkCraft.id("ink_step");TechniqueService.grant(p,id);
        for(int x=4;x<=8;x++)for(int z=3;z<=20;z++)for(int y=1;y<=4;y++)h.setBlock(new net.minecraft.core.BlockPos(x,y,z),net.minecraft.world.level.block.Blocks.AIR);
        var start=h.absoluteVec(new net.minecraft.world.phys.Vec3(6,1,4));p.setPos(start);p.setYRot(0);p.setYHeadRot(0);p.setXRot(0);
        SorceryPractice.set(p,new PracticeState(3000,0,false,false,100,100,60));CursedEnergy.fill(p);
        Personalization.save(p,id,new TechniqueTuning(100,200,100,100,50,0));
        var def=Personalization.definition(p,id,Techniques.get(p.registryAccess(),id).orElseThrow());double before=CursedEnergy.current(p);
        double cost=TechniqueService.effectiveCost(p,id,def);long now=p.level().getGameTime();
        h.assertTrue(TechniqueService.cast(p,id,0)==CastFailure.NONE,"custom teleport casts");
        h.assertTrue(p.position().distanceTo(start)>10,"custom reach actually moves the caster");
        h.assertTrue(Math.abs(before-CursedEnergy.current(p)-cost)<0.001,"server charges tuned cost once");
        h.assertTrue(TechniqueService.readyAt(p,id)-now==TechniqueService.effectiveCooldown(p,def),"tuned cooldown applied");h.succeed();
    }
}
