package com.jjkcraft.gametest;

import java.util.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.phys.Vec3;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import com.jjkcraft.JjkCraft;
import com.jjkcraft.awakening.*;
import com.jjkcraft.culling.CullingGame;
import com.jjkcraft.culling.entity.CullingEntities;
import com.jjkcraft.death.*;
import com.jjkcraft.energy.CursedEnergy;
import com.jjkcraft.sorcerer.*;
import com.jjkcraft.technique.*;

public final class ExpansionGameTests {
    private static ServerPlayer character(GameTestHelper h,String family) {
        var p=TestPlayers.fresh(h);p.setAttached(CharacterAwakening.RECORD,new AwakeningRecord("inherit","jjkcraft:"+family,"",false,0));Awakening.awaken(p,false);return p;
    }
    @GameTest(environment="jjkcraft:expansion") public void expandedFamiliesHaveMatchingMovesAndIcons(GameTestHelper h) {
        var registry=h.getLevel().registryAccess().lookupOrThrow(CharacterAwakening.FAMILIES);
        h.assertTrue(registry.size()>=19,"nineteen families");h.assertTrue(Techniques.all(h.getLevel().registryAccess()).size()>=63,"expanded techniques");
        for(var family:registry)for(var id:family.techniques())h.assertTrue(Techniques.get(h.getLevel().registryAccess(),id).isPresent(),"family references a valid move "+id);
        h.assertTrue(h.getLevel().registryAccess().lookupOrThrow(CharacterTraits.DEFINITIONS).size()>=15,"fifteen physical/clan attributes");h.succeed();
    }
    @GameTest(environment="jjkcraft:expansion") public void bothModesOfferSpiritsAndRequirePriorReincarnationPacts(GameTestHelper h) {
        var soul=SoulState.EMPTY.phase("choosing");
        h.assertTrue(SoulSystem.allowedDeathChoice(soul,false,"respawn"),"Survival can use a bed");
        h.assertFalse(SoulSystem.allowedDeathChoice(soul,true,"respawn"),"Hardcore cannot use free normal respawn");
        for(boolean hardcore:List.of(false,true)) {
            h.assertTrue(SoulSystem.allowedDeathChoice(soul,hardcore,"spirit"),"spirit route offered in both modes");
            h.assertFalse(SoulSystem.allowedDeathChoice(soul,hardcore,"reincarnate"),"no unpaid colony reincarnation");
            h.assertTrue(SoulSystem.allowedDeathChoice(soul.pact("kenjaku"),hardcore,"reincarnate"),"prior pact enables reincarnation");
        }
        h.succeed();
    }
    @GameTest(environment="jjkcraft:expansion") public void exorcismStartsADifferentTechniqueAndResetsTheCharacter(GameTestHelper h) {
        var p=character(h,"blood_manipulation");var oldFamily=CharacterAwakening.get(p).family();
        p.setAttached(SoulSystem.STATE,new SoulState("becoming_spirit","",List.of(),0,p.level().dimension().identifier().toString(),p.getX(),p.getY(),p.getZ(),"survival",0,false,false));
        SoulSystem.becomeSpirit(p);h.assertValueEqual(SoulSystem.state(p).phase(),"spirit","spirit life starts");
        h.assertTrue(TechniqueService.knows(p,JjkCraft.id("crimson_binding")),"spirit keeps the technique");
        h.assertTrue(CharacterTraits.has(p,"vengeful_spirit"),"cursed body is an attribute");
        SoulSystem.restartAfterSpirit(p);h.assertValueEqual(SoulSystem.state(p).phase(),"alive","new human life");
        h.assertFalse(CharacterAwakening.get(p).family().equals(oldFamily),"new family differs");h.assertFalse(CharacterTraits.has(p,"vengeful_spirit"),"old cursed body removed");
        h.assertTrue(Progression.get(p).awakened(),"new technique is already awakened");h.succeed();
    }
    @GameTest(environment="jjkcraft:expansion") public void bodyTransferConsumesAFallenHostAndKeepsFourSlots(GameTestHelper h) {
        var p=character(h,"brain_transplant");
        var fallen=h.spawn(CullingEntities.ENTRANT,2,1,2,EntitySpawnReason.EVENT);fallen.setup(true,-1,h.getLevel().getRandom());
        var body=BodyTransfer.leaveBody(h.getLevel(),fallen);fallen.discard();h.assertTrue(body!=null,"fallen host is present");
        p.setPos(body.getX(),body.getY(),body.getZ()-2);
        h.assertTrue(BodyTransfer.occupy(p,body),"valid fallen host can be occupied");
        h.assertFalse(body.isAlive(),"host token consumed");h.assertFalse(BodyTransfer.occupy(p,body),"cannot take the same corpse twice");
        h.assertFalse(CharacterTraits.get(p).form().isEmpty(),"body form persists");h.assertTrue(TechniqueService.get(p).loadout().size()==12,"twelve slots");
        h.assertValueEqual(TechniqueService.get(p).slot(0),Optional.of(JjkCraft.id("brain_transplant")),"brain transfer remains available");
        h.assertFalse(BodyTransfer.occupy(p,h.spawn(EntityTypes.HUSK,3,1,3)),"living targets cannot be possessed");h.succeed();
    }
    @GameTest(environment="jjkcraft:expansion") public void sixEyesIsASeparateGojoAttribute(GameTestHelper h) {
        var p=TestPlayers.fresh(h);p.setAttached(CharacterAwakening.RECORD,new AwakeningRecord("inherit","jjkcraft:limitless","Gojo",true,0));Awakening.awaken(p,false);CharacterTraits.ensure(p);
        h.assertTrue(CharacterTraits.has(p,"six_eyes"),"clan gift recorded");
        h.assertTrue(TechniqueService.get(p).known().stream().noneMatch(id->id.getPath().equals("six_eyes")),"gift consumes no technique slot");
        var before=CharacterTraits.get(p);CharacterTraits.ensure(p);h.assertValueEqual(CharacterTraits.get(p),before,"relog does not reroll attributes");h.succeed();
    }
    @GameTest(environment="jjkcraft:expansion",padding=24) public void curseControlAbsorbsThenReleasesAnOwnedSpirit(GameTestHelper h) {
        // The empty template only clears its own volume; this longer ray needs open terrain.
        for(int x=5;x<=11;x++)for(int z=5;z<=12;z++) {
            h.setBlock(new BlockPos(x,0,z),net.minecraft.world.level.block.Blocks.STONE);
            for(int y=1;y<=4;y++)h.setBlock(new BlockPos(x,y,z),net.minecraft.world.level.block.Blocks.AIR);
        }
        var p=character(h,"curse_manipulation");var curse=h.spawn(com.jjkcraft.curse.CurseEntities.LEDGER_CRAWLER,8,1,10,EntitySpawnReason.EVENT);curse.applyStats();curse.setHealth(1);curse.setNoAi(true);
        p.setPos(curse.getX(),curse.getY(),curse.getZ()-3);p.setYRot(0);p.setYHeadRot(0);
        var aim=curse.getBoundingBox().getCenter().subtract(p.getEyePosition());p.setXRot((float)-Math.toDegrees(Math.atan2(aim.y,Math.sqrt(aim.horizontalDistanceSqr()))));
        var definition=Techniques.get(p.registryAccess(),JjkCraft.id("cursed_spirit_manipulation")).orElseThrow();
        var ctx=new CastContext(p,h.getLevel(),JjkCraft.id("cursed_spirit_manipulation"),definition,1,1);
        h.runAfterDelay(2,()->{
        h.assertTrue(ctx.rayTarget(ctx.range(),0.8).filter(curse::equals).isPresent(),"aim reaches the capture target; look="+ctx.look()+" eye="+ctx.eye()+" target="+curse.position()+" limit="+ctx.blockLimit(ctx.range()));
        ControlledCurses.cast(ctx);h.assertTrue(p.getAttachedOrCreate(ControlledCurses.STOCK).size()==1,"weakened curse absorbed");
        ControlledCurses.cast(ctx);h.assertTrue(p.getAttachedOrCreate(ControlledCurses.STOCK).isEmpty(),"stock consumed on release");
        h.assertTrue(h.getLevel().getEntitiesOfClass(com.jjkcraft.curse.CurseEntity.class,p.getBoundingBox().inflate(5),c->c.getAttachedOrCreate(ControlledCurses.OWNER).equals(p.getUUID().toString())).size()==1,"released spirit is controlled");h.succeed();});
    }
    @GameTest(environment="jjkcraft:expansion") public void explicitColonyEntryBindsAndPersonalKoganeWorks(GameTestHelper h) {
        var server=h.getLevel().getServer();CullingGame.reset(server);var p=character(h,"boogie_woogie");
        try {
            if(!CullingGame.state(server).running)CullingGame.start(h.getLevel(),h.absolutePos(BlockPos.ZERO),1,24,0);
            var colony=CullingGame.raise(h.getLevel(),h.absolutePos(new BlockPos(4,1,4)),24,"Test Colony");
            h.assertTrue(CullingGame.enterColony(p,colony.id),"map entry accepts and travels");
            h.assertTrue(CullingGame.state(server).active(p.getUUID())!=null,"registered participant");
            h.assertTrue(CullingGame.nearMaster(p),"personal Kogane is usable away from NPC");
            h.assertFalse(CullingGame.enterColony(p,colony.id),"cannot teleport out through a repeated entry");
        }finally{CullingGame.reset(server);}h.succeed();
    }
}
