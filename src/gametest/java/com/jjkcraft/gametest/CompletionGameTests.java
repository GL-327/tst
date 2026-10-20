package com.jjkcraft.gametest;

import java.util.*;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import com.jjkcraft.JjkCraft;
import com.jjkcraft.awakening.*;
import com.jjkcraft.combat.CursedCombat;
import com.jjkcraft.curse.*;
import com.jjkcraft.damage.DamageModifiers;
import com.jjkcraft.death.*;
import com.jjkcraft.energy.CursedEnergy;
import com.jjkcraft.network.payload.CombatPayloads;
import com.jjkcraft.sorcerer.Awakening;
import com.jjkcraft.sorcerer.Progression;
import com.jjkcraft.technique.*;

/** Separate environment keeps the new character/death scenarios out of legacy Culling batches. */
public final class CompletionGameTests {
    @GameTest(environment="jjkcraft:completion") public void bloodFamilyRepairsTheWrongStarterLoadout(GameTestHelper h) {
        var p=TestPlayers.fresh(h); Awakening.awaken(p,false);
        p.setAttached(CharacterAwakening.RECORD,new AwakeningRecord("inherit","jjkcraft:blood_manipulation","Kamo",false,0));
        TechniqueService.set(p,TechniqueState.EMPTY.withInnate(JjkCraft.id("ink_iron_palm")).withLearned(JjkCraft.id("ink_step")).withAutoSlotted(2));
        CharacterAwakening.unlock(p);
        var state=TechniqueService.get(p);
        h.assertValueEqual(state.innate(),Optional.of(JjkCraft.id("crimson_binding")),"Blood Manipulation has its own starter");
        h.assertTrue(state.known().contains(JjkCraft.id("blood_needle")),"a blood attack starts unlocked");
        h.assertFalse(state.known().contains(JjkCraft.id("ink_step")),"generic teleport removed");
        h.assertFalse(state.known().contains(JjkCraft.id("ink_iron_palm")),"generic blast removed");
        for(var slot:state.loadout()) slot.ifPresent(id->h.assertTrue(CharacterAwakening.family(p).orElseThrow().techniques().contains(id),"slotted powers match the family"));
        Progression.setGrade(p,JjkCraft.id("special_grade"));
        h.assertTrue(TechniqueService.usableSlots(p)==12,"twelve equipped technique slots");
        TechniqueService.setLoadout(p,12,Optional.of(JjkCraft.id("blood_edge")));
        h.assertTrue(TechniqueService.get(p).slot(12).isEmpty(),"a forged thirteenth slot is rejected"); h.succeed();
    }
    @GameTest(environment="jjkcraft:completion") public void normalRespawnKeepsTheVanillaRespawnLocation(GameTestHelper h) {
        var p=TestPlayers.fresh(h); Awakening.awaken(p,false); var before=p.position(); var techniques=TechniqueService.get(p);
        p.setAttached(SoulSystem.STATE,new SoulState("choosing","",List.of(),0,p.level().dimension().identifier().toString(),0,100,0,"survival",0,false,false));
        p.setGameMode(GameType.SPECTATOR);
        SoulSystem.action(p,"respawn");
        h.assertValueEqual(SoulSystem.state(p).phase(),"alive","normal body restored");
        h.assertFalse(p.isSpectator(),"survival restored");
        h.assertValueEqual(p.position(),before,"vanilla's chosen respawn location is retained");
        h.assertValueEqual(TechniqueService.get(p),techniques,"normal respawn retains technique state");
        SoulSystem.action(p,"object"); h.assertValueEqual(SoulSystem.state(p).phase(),"alive","late death-choice request ignored"); h.succeed();
    }
    @GameTest(environment="jjkcraft:completion") public void inheritIsOneShotAndStarterWorks(GameTestHelper h) {
        ServerPlayer p=TestPlayers.fresh(h);
        h.assertTrue(CharacterAwakening.choose(p,"inherit",""),"inherit succeeds");
        var record=CharacterAwakening.get(p);
        h.assertFalse(record.clan().isEmpty(),"a clan is recorded");
        h.assertTrue(TechniqueService.get(p).innate().isPresent(),"a starter is learned");
        var starter=Techniques.get(p.registryAccess(),TechniqueService.get(p).innate().orElseThrow()).orElseThrow();
        h.assertTrue(com.jjkcraft.grade.Grades.rank(p.registryAccess(),starter.grade())==0,"starter can be used at Grade 4");
        h.assertFalse(CharacterAwakening.choose(p,"random",""),"second choice refused");
        h.assertValueEqual(CharacterAwakening.get(p),record,"result preserved"); h.succeed();
    }
    @GameTest(environment="jjkcraft:completion") public void randomExcludesClanOnlyFamilies(GameTestHelper h) {
        for(int n=0;n<32;n++) { var p=TestPlayers.fresh(h); h.assertTrue(CharacterAwakening.choose(p,"random",""),"random succeeds"); h.assertTrue(CharacterAwakening.get(p).clan().isEmpty(),"random does not inherit a clan"); }
        h.succeed();
    }
    @GameTest(environment="jjkcraft:completion") public void forgedPickerAndRerollsAreRejected(GameTestHelper h) {
        ServerPlayer p=TestPlayers.fresh(h);
        h.assertFalse(CharacterAwakening.choose(p,"choose","jjkcraft:limitless"),"picker disabled by default");
        CharacterAwakening.firstJoin(p); var record=CharacterAwakening.get(p); var progress=Progression.get(p);
        CharacterAwakening.firstJoin(p); h.assertValueEqual(CharacterAwakening.get(p),record,"reconnect does not reroll HR"); h.assertValueEqual(Progression.get(p),progress,"same awakening progress"); h.succeed();
    }
    @GameTest(environment="jjkcraft:completion") public void familyUnlocksWithGrade(GameTestHelper h) {
        ServerPlayer p=TestPlayers.fresh(h); CharacterAwakening.choose(p,"inherit","");
        var family=CharacterAwakening.family(p).orElseThrow(); Progression.setGrade(p,JjkCraft.id("special_grade"));
        for(var id:family.techniques()) h.assertTrue(TechniqueService.knows(p,id),"family move unlocks: "+id);
        h.succeed();
    }
    @GameTest(environment="jjkcraft:completion") public void everyCurseHasMovesAndAllFortyTypesExist(GameTestHelper h) {
        var all=Curses.all(h.getLevel().registryAccess()); h.assertTrue(all.size()>=40,"forty curse definitions");
        for(var entry:all) for(var move:entry.definition().combatMoves()) h.assertTrue(h.getLevel().registryAccess().lookupOrThrow(CursedCombat.MOVES).getValue(move)!=null,entry.id()+" valid move "+move);
        h.succeed();
    }
    @GameTest(environment="jjkcraft:completion") public void matchingHeightParriesAndMismatchDoesNot(GameTestHelper h) {
        var p=TestPlayers.fresh(h); Awakening.awaken(p,false); p.setYRot(0); p.setXRot(0); CursedEnergy.fill(p);
        var attacker=h.spawn(EntityTypes.HUSK,new Vec3(0.5,1,2.5));
        p.setPos(attacker.getX(),attacker.getY(),attacker.getZ()-2);
        CursedCombat.input(p,new CombatPayloads.Input(2,0));
        var source=h.getLevel().damageSources().mobAttack(attacker);
        h.assertTrue(DamageModifiers.apply(h.getLevel(),p,source,10)>0,"low guard does not block a mid attack");
        CursedCombat.input(p,new CombatPayloads.Input(2,-1)); CursedCombat.input(p,new CombatPayloads.Input(2,1));
        h.assertTrue(DamageModifiers.apply(h.getLevel(),p,source,10)==0,"fresh matching guard parries");
        h.assertTrue(Progression.get(p).counter("combat_parries")==1,"parry recorded"); h.succeed();
    }
    @GameTest(environment="jjkcraft:completion",maxTicks=40) public void attacksWaitForActiveFrames(GameTestHelper h) {
        var p=TestPlayers.fresh(h); Awakening.awaken(p,false); CursedEnergy.fill(p);
        var target=h.spawn(EntityTypes.HUSK,new Vec3(0.5,1,2.5)); target.setNoAi(true);
        p.setPos(target.getX(),target.getY(),target.getZ()-2); p.setYRot(0); p.setXRot(0);
        float health=target.getHealth(); h.assertTrue(CursedCombat.start(p,JjkCraft.id("jab"),target.getEyePosition().subtract(p.getEyePosition())),"move accepted");
        h.runAfterDelay(2,()->h.assertTrue(target.getHealth()==health,"startup cannot hit"));
        h.runAfterDelay(6,()->{h.assertTrue(target.getHealth()<health,"active window hits");h.succeed();});
    }
    @GameTest(environment="jjkcraft:completion") public void soulStateAndItemIdentityRoundTrip(GameTestHelper h) {
        var soul=new SoulState("object","sukuna",List.of("one","two"),24000,"minecraft:overworld",2,70,3,"survival",2,true,true);
        h.assertValueEqual(SoulState.CODEC.parse(JsonOps.INSTANCE,SoulState.CODEC.encodeStart(JsonOps.INSTANCE,soul).getOrThrow()).getOrThrow(),soul,"soul persistence");
        var object=new SoulObject(UUID.randomUUID().toString(),UUID.randomUUID().toString(),"Sorcerer","jjkcraft:grade_1",24000);
        h.assertValueEqual(SoulSystem.stack(object).get(SoulSystem.OBJECT_DATA),object,"item carries owner, grade, token, timer"); h.succeed();
    }
    @GameTest(environment="jjkcraft:completion") public void vesselRevivesOnceAndRejectsCopiedToken(GameTestHelper h) {
        ServerPlayer owner=h.makeMockServerPlayerInLevel();
        var vessel=h.spawn(SoulSystem.VESSEL,2,1,2,EntitySpawnReason.EVENT); vessel.setColony(0);
        try {
            var carrier=TestPlayers.fresh(h); carrier.setPos(vessel.getX()+1,vessel.getY(),vessel.getZ());
            String token=UUID.randomUUID().toString(); long expiry=h.getLevel().getServer().overworld().getGameTime()+1200;
            owner.setAttached(SoulSystem.STATE,new SoulState("object","",List.of(token),expiry,"minecraft:overworld",0,80,0,"survival",0,true,false)); owner.setGameMode(GameType.SPECTATOR);
            var identity=new SoulObject(owner.getUUID().toString(),token,"Test Soul","jjkcraft:grade_4",expiry);
            ItemStack first=SoulSystem.stack(identity), duplicate=SoulSystem.stack(identity);
            h.assertTrue(SoulSystem.revive(carrier,vessel,first,identity),"vessel revives a valid soul");
            h.assertTrue(first.isEmpty(),"object consumed"); h.assertTrue(owner.getHealth()==owner.getMaxHealth()*0.25f,"quarter health");
            h.assertFalse(owner.isSpectator(),"living body restored"); h.assertFalse(SoulSystem.revive(carrier,vessel,duplicate,identity),"duplicate cannot revive again");
        } finally { vessel.discard(); h.getLevel().getServer().getPlayerList().remove(owner); }
        h.succeed();
    }
    @GameTest(environment="jjkcraft:completion") public void placedObjectKeepsItsIdentity(GameTestHelper h) {
        var p=TestPlayers.fresh(h); Awakening.awaken(p,false);
        BlockPos pos=h.absolutePos(new BlockPos(1,1,1));
        p.setAttached(SoulSystem.STATE,new SoulState("becoming_object","",List.of(),0,h.getLevel().dimension().identifier().toString(),pos.getX(),pos.getY(),pos.getZ(),"survival",0,false,false));
        SoulSystem.becomeObject(p);
        h.assertTrue(p.isSpectator(),"object state has spectator camera");
        var object=SoulSystem.takePlaced(h.getLevel(),pos); h.assertTrue(object!=null,"a placed object exists");
        h.assertValueEqual(object.owner(),p.getUUID().toString(),"placed object belongs to its owner");
        h.getLevel().removeBlock(pos,false); h.succeed();
    }
}
