package com.jjkcraft.gametest;
import java.util.*;
import com.jjkcraft.JjkCraft;
import com.jjkcraft.training.*;
import com.jjkcraft.combat.*;
import com.jjkcraft.energy.*;
import com.jjkcraft.awakening.CharacterAwakening;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
public final class DojoGameTests{
    private static ServerPlayer player(GameTestHelper h){var p=TestPlayers.fresh(h);p.setGameMode(GameType.CREATIVE);CharacterAwakening.creative(p,JjkCraft.id("blood_manipulation"),false);p.setGameMode(GameType.SURVIVAL);p.setOnGround(true);CursedEnergy.fill(p);return p;}
    @GameTest(environment="jjkcraft:expansion") public void survivalCannotBuySkillsAndPhysicalBonusesAreBounded(GameTestHelper h){
        var p=player(h);p.experienceLevel=100;var req=new com.jjkcraft.network.payload.PersonalizationPayloads.Practice(0,100,100,60);h.assertFalse(SorceryPractice.act(p,req),"GUI buying is closed in survival");double health=p.getMaxHealth(),damage=p.getAttributeValue(Attributes.ATTACK_DAMAGE);Training.award(p,"strength",100000);Training.award(p,"endurance",100000);
        h.assertTrue(Training.get(p).level("strength")==20&&Math.abs(p.getAttributeValue(Attributes.ATTACK_DAMAGE)-damage-2.4)<.001,"strength has a real bounded physical benefit");h.assertTrue(Math.abs(p.getMaxHealth()-health-8)<.001,"endurance increases health by at most eight");h.assertTrue(CursedCombat.maximumStamina(p)>=140,"endurance increases stamina");var before=Training.get(p);p.setGameMode(GameType.CREATIVE);Training.award(p,"agility",500);h.assertValueEqual(Training.get(p),before,"creative cannot farm drills");h.succeed();
    }
    @GameTest(environment="jjkcraft:expansion",maxTicks=150,padding=32) public void realDummyContactTeachesReinforcementAndCannotBeSpammedInOneTick(GameTestHelper h){
        var p=player(h);SorceryPractice.set(p,SorceryPractice.get(p).withReinforcement(false,false));Training.award(p,"control",30);var doll=h.spawn(com.jjkcraft.entity.JjkEntities.TRAINING_DUMMY,new Vec3(1,1,1));
        for(int i=0;i<10;i++)h.runAfterDelay(i*12+1,()->{doll.damageCooldownTime=0;doll.hurtServer(h.getLevel(),h.getLevel().damageSources().playerAttack(p),4);});
        h.runAfterDelay(125,()->{h.assertTrue(SorceryPractice.get(p).reinforcement(),"real repeated contact unlocks reinforcement");h.assertValueEqual(Training.get(p).xp("strength"),30,"only measured physical contacts grant work");int before=Training.get(p).xp("strength");for(int i=0;i<100;i++)Training.contact(p,false,false);h.assertTrue(Training.get(p).xp("strength")<=before+3,"same-tick contact spam is bounded");h.assertTrue(doll.isAlive(),"training prop survives strikes");h.succeed();});
    }
    @GameTest(environment="jjkcraft:expansion",padding=32) public void breathingRequiresAMatAndACompletedRelease(GameTestHelper h){
        var p=player(h);p.setPos(h.absoluteVec(new Vec3(.5,1.1875,.5)));h.setBlock(new BlockPos(0,1,0),com.jjkcraft.block.JjkBlocks.TRAINING_MAT);p.setOnGround(true);Training.step(p);p.setShiftKeyDown(true);for(int i=0;i<45;i++)Training.step(p);h.assertTrue(Training.get(p).xp("reserves")==0,"holding is not enough");p.setShiftKeyDown(false);Training.step(p);h.assertTrue(Training.get(p).xp("reserves")==6,"valid timed release trains reserves");
        p.setShiftKeyDown(true);for(int i=0;i<150;i++)Training.step(p);p.setShiftKeyDown(false);Training.step(p);h.assertTrue(Training.get(p).xp("reserves")==6,"long AFK hold gives nothing");h.succeed();
    }
    @GameTest(environment="jjkcraft:expansion",maxTicks=45,padding=32) public void animatedBlackFlashPunchStillTrainsPhysicalContact(GameTestHelper h){
        var p=player(h);p.setPos(h.absoluteVec(new Vec3(1,1,1)));p.setNoGravity(true);p.setYRot(0);p.setXRot(0);
        var doll=h.spawn(com.jjkcraft.entity.JjkEntities.TRAINING_DUMMY,new Vec3(1,1,3));doll.setNoGravity(true);
        com.jjkcraft.blackflash.BlackFlash.forceNext(p);
        h.assertTrue(CursedCombat.start(p,JjkCraft.id("jab"),new Vec3(0,0,1)),"animated strike starts");
        h.runAfterDelay(12,()->{h.assertTrue(Training.get(p).xp("strength")==3,"Black Flash remains physical training after replacing the damage source");h.assertTrue(doll.isAlive(),"doll survives Black Flash");h.succeed();});
    }
    @GameTest(environment="jjkcraft:expansion",padding=32) public void reverseFlowAtAStoneUsesPracticeInsteadOfExperienceLevels(GameTestHelper h){
        var p=player(h);SorceryPractice.set(p,SorceryPractice.get(p).withReinforcement(true,false));com.jjkcraft.reverse.ReverseTechnique.set(p,com.jjkcraft.reverse.ReverseTechnique.state(p).withUnlocked(false),false);p.setAttached(com.jjkcraft.reverse.ReverseStudy.LESSON,0);p.experienceLevel=0;
        for(int lesson=0;lesson<3;lesson++){h.assertTrue(com.jjkcraft.reverse.ReverseStudy.startInWorld(p),"physical drill starts without experience levels");for(int tick=0;tick<200;tick++)com.jjkcraft.reverse.ReverseStudy.step(p);}
        h.assertTrue(com.jjkcraft.reverse.ReverseTechnique.unlocked(p),"deliberate drills learn RCT");h.assertTrue(p.experienceLevel==0,"no hidden XP purchase");h.succeed();
    }
    @GameTest(environment="jjkcraft:expansion",padding=32) public void weaponProfilesUseRealSwingsAndDoNotGrantOffhandSwordDamage(GameTestHelper h){
        var p=player(h);p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Items.DIAMOND_SWORD));h.assertTrue(CursedCombat.start(p,JjkCraft.id("jab"),new Vec3(0,0,1)),"held sword begins attack");h.assertTrue(CursedCombat.currentMove(p).startsWith("blade_"),"sword selects slash animation");CursedCombat.clearActions(p);h.assertFalse(CursedCombat.startPunch(p,JjkCraft.id("jab"),new Vec3(0,0,1),-1),"empty hand cannot borrow sword damage");h.assertValueEqual(WeaponCombat.kind(new ItemStack(Items.TRIDENT)),"pole","trident selects polearm moves");h.assertValueEqual(WeaponCombat.kind(new ItemStack(Items.APPLE)),"item","ordinary held objects get an improvised swing");h.succeed();
    }
    @GameTest(environment="jjkcraft:expansion") public void trainingPersistsAndInvalidKeysCannotGrowAnAttachment(GameTestHelper h){var p=player(h);Training.award(p,"agility",120);var bytes=io.netty.buffer.Unpooled.buffer();try{var payload=new com.jjkcraft.network.payload.TrainingPayload(Training.get(p));com.jjkcraft.network.payload.TrainingPayload.CODEC.encode(bytes,payload);h.assertValueEqual(com.jjkcraft.network.payload.TrainingPayload.CODEC.decode(bytes),payload,"training network roundtrip");}finally{bytes.release();}var q=TestPlayers.fresh(h);com.jjkcraft.sorcerer.SorcererLifecycle.copyOnDeath(p,q);h.assertValueEqual(Training.get(q),Training.get(p),"physical training survives ordinary death");Training.award(p,"forged",10000);h.assertFalse(Training.get(p).work().containsKey("forged"),"unknown skills are rejected");h.succeed();}
    @GameTest(environment="jjkcraft:expansion") public void habitatsPreferRelatedCursesAndProtectLowFearAreasFromBosses(GameTestHelper h){h.assertTrue(com.jjkcraft.curse.spawn.CurseHabitat.weight("sewer_effigy","swamp",false,true,80,2)>com.jjkcraft.curse.spawn.CurseHabitat.weight("sewer_effigy","desert",false,false,80,2),"wet curses prefer wet encounters");h.assertTrue(com.jjkcraft.curse.spawn.CurseHabitat.weight("hunger_eclipse","forest",false,false,60,6)==0,"bosses require exceptionally high fear");h.succeed();}
}
