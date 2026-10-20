package com.jjkcraft.gametest;

import java.util.*;
import com.jjkcraft.JjkCraft;
import com.jjkcraft.awakening.*;
import com.jjkcraft.energy.*;
import com.jjkcraft.technique.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public final class ChargeTerrainGameTests {
    @GameTest(environment="jjkcraft:expansion",padding=24,maxTicks=60) public void sixEyesReducesActualServerChargeSpending(GameTestHelper h){
        var ordinary=player(h);var eyes=player(h);var record=CharacterAwakening.get(ordinary);
        ordinary.setAttached(CharacterAwakening.RECORD,new AwakeningRecord(record.path(),record.family(),record.clan(),false,record.tick()));
        bind(ordinary,"hollow_purple");bind(eyes,"hollow_purple");TechniqueService.onCastKey(ordinary,0,true);TechniqueService.onCastKey(eyes,0,true);
        double a=CursedEnergy.current(ordinary),b=CursedEnergy.current(eyes);
        h.runAfterDelay(30,()->{double spentA=a-CursedEnergy.current(ordinary),spentB=b-CursedEnergy.current(eyes);h.assertTrue(spentA>0&&spentB<spentA*.25,"Six Eyes reduces paid ongoing CE, beyond the base discount");TechniqueService.cancel(ordinary);TechniqueService.cancel(eyes);h.succeed();});
    }
    @GameTest(environment="jjkcraft:expansion",padding=24) public void claimVetoAndTerrainSwitchAreRespected(GameTestHelper h){
        var p=player(h);var pos=h.absolutePos(new BlockPos(5,6,5));h.getLevel().setBlockAndUpdate(pos,Blocks.OAK_LOG.defaultBlockState());
        net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.BEFORE.register((level,who,at,state,be)->who!=p||!at.equals(pos));
        TechniqueTerrain.sphere(ctx(p,"lapse_blue"),Vec3.atCenterOf(pos),1,16,true);h.assertTrue(h.getLevel().getBlockState(pos).is(Blocks.OAK_LOG),"claim event veto is honored");
        boolean old=com.jjkcraft.config.JjkConfig.server().casting.terrainDamage;
        try{com.jjkcraft.config.JjkConfig.server().casting.terrainDamage=false;h.getLevel().setBlockAndUpdate(pos.above(),Blocks.OAK_LOG.defaultBlockState());h.assertTrue(TechniqueTerrain.sphere(ctx(p,"lapse_blue"),Vec3.atCenterOf(pos.above()),1,16,true)==0&&h.getLevel().getBlockState(pos.above()).is(Blocks.OAK_LOG),"server can disable terrain damage");}finally{com.jjkcraft.config.JjkConfig.server().casting.terrainDamage=old;}h.succeed();
    }
    @GameTest(environment="jjkcraft:expansion",padding=24) public void ordinaryChannelStartsOnPressAndStopsOnReleaseOrMenu(GameTestHelper h){
        var p=player(h);bind(p,"lacquer_ward");TechniqueService.onCastKey(p,0,true);
        h.assertTrue(TechniqueService.active(p).isPresent()&&TechniqueService.chargePercent(p)==0,"guard channels immediately at normal output");
        TechniqueService.onCastKey(p,1,false);h.assertTrue(TechniqueService.active(p).isPresent(),"unrelated release keeps channel");
        TechniqueService.onCastKey(p,0,false);h.assertTrue(TechniqueService.active(p).isEmpty(),"release ends channel");
        TechniqueService.resetCooldowns(p);TechniqueService.onCastKey(p,0,true);TechniqueService.cancelKey(p,0);h.assertTrue(TechniqueService.active(p).isEmpty(),"menu cancels a held channel");h.succeed();
    }
    @GameTest(environment="jjkcraft:expansion",padding=24) public void chargeProfilesDefaultToNormalAndSurvivePersonalisation(GameTestHelper h){
        var p=player(h);int count=0;
        for(var entry:Techniques.all(p.registryAccess())){
            var d=entry.definition();if(d.cast().chargeable())count++;
            var tuned=TechniqueTuning.DEFAULT.with(0,150).with(4,80).apply(d);
            h.assertTrue(tuned.cast().chargeable()==d.cast().chargeable(),"personalisation preserves authored charging for "+entry.id());
            if(d.behavior() instanceof com.jjkcraft.technique.behavior.SummonBehavior||d.behavior() instanceof com.jjkcraft.technique.behavior.FusionBehavior||d.behavior() instanceof com.jjkcraft.technique.behavior.DashBehavior||d.behavior() instanceof com.jjkcraft.technique.behavior.HealBehavior)h.assertFalse(d.cast().chargeable(),"summons, fusion, movement and healing do not charge");
        }
        h.assertTrue(count==18,"only eighteen suitable moves opt into charging");
        var json=com.google.gson.JsonParser.parseString("{\"cost\":10,\"cooldown\":20}");
        h.assertFalse(TechniqueDefinition.Cast.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,json).getOrThrow().chargeable(),"old and datapack moves default to normal activation");h.succeed();
    }
    @GameTest(environment="jjkcraft:expansion",padding=24,maxTicks=60) public void summonsActivateOnPressAndHoldingNeverRecastsThem(GameTestHelper h){
        var p=player(h);bind(p,"ten_shadows_divine_dogs");TechniqueService.onCastKey(p,0,true);
        var owned=com.jjkcraft.entity.shikigami.ShikigamiEntity.ownedBy(h.getLevel(),com.jjkcraft.entity.shikigami.ShikigamiEntity.class,p);h.assertTrue(owned.size()==2&&TechniqueService.chargePercent(p)==0,"summons appear on press without charge");
        var ids=owned.stream().map(net.minecraft.world.entity.Entity::getUUID).collect(java.util.stream.Collectors.toSet());
        h.runAfterDelay(12,()->{TechniqueService.onCastKey(p,0,false);var after=com.jjkcraft.entity.shikigami.ShikigamiEntity.ownedBy(h.getLevel(),com.jjkcraft.entity.shikigami.ShikigamiEntity.class,p);h.assertTrue(after.size()==2&&after.stream().allMatch(e->ids.contains(e.getUUID())),"holding or releasing neither replaces nor duplicates summons");h.succeed();});
    }
    @GameTest(environment="jjkcraft:expansion",padding=24) public void infinityTogglesImmediatelyAndMenuLeavesItRunning(GameTestHelper h){
        var p=player(h);bind(p,"infinity");TechniqueService.onCastKey(p,0,true);h.assertTrue(TechniqueService.active(p).isPresent()&&TechniqueService.chargePercent(p)==0,"Infinity is an immediate toggle");TechniqueService.onCastKey(p,0,false);TechniqueService.cancelKey(p,0);h.assertTrue(TechniqueService.active(p).isPresent(),"release and menu leave toggles running");TechniqueService.onCastKey(p,0,true);h.assertTrue(TechniqueService.active(p).isEmpty(),"next press toggles off");h.succeed();
    }
    @GameTest(environment="jjkcraft:expansion",padding=24) public void boogieWoogieSwapsOnPressWithoutCharging(GameTestHelper h){
        var p=player(h);bind(p,"boogie_woogie");var target=h.spawn(net.minecraft.world.entity.EntityTypes.ZOMBIE,new Vec3(2.5,1,5.5));target.setNoAi(true);var destination=target.position();TechniqueService.onCastKey(p,0,true);h.assertTrue(p.position().distanceToSqr(destination)<.01&&TechniqueService.chargePercent(p)==0,"swap occurs on the key press");TechniqueService.onCastKey(p,0,false);h.assertTrue(p.position().distanceToSqr(destination)<.01,"release never swaps back");h.succeed();
    }
    private static ServerPlayer player(GameTestHelper h){
        var p=TestPlayers.fresh(h);p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
        CharacterAwakening.creative(p,JjkCraft.id("limitless"),false);
        p.setAttached(SorceryPractice.STATE,new PracticeState(3000,10000,false,false,100,100,60,5,true));CursedEnergy.set(p,20000);
        p.setPos(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(2,1,2))));p.setYRot(0);p.setXRot(0);return p;
    }
    private static void bind(ServerPlayer p,String key){TechniqueService.cancel(p);TechniqueService.resetCooldowns(p);TechniqueService.grant(p,JjkCraft.id(key));TechniqueService.setLoadout(p,0,Optional.of(JjkCraft.id(key)));}
    private static CastContext ctx(ServerPlayer p,String key){var id=JjkCraft.id(key);return new CastContext(p,p.level(),id,Techniques.get(p.registryAccess(),id).orElseThrow(),1,1);}
    @GameTest(environment="jjkcraft:expansion",padding=24,maxTicks=80) public void keyPressChargesAndReleaseCastsExactlyOnce(GameTestHelper h){
        var p=player(h);bind(p,"lapse_blue");double before=CursedEnergy.current(p);TechniqueService.onCastKey(p,0,true);
        h.assertTrue(TechniqueService.chargePercent(p)==25&&CursedEnergy.current(p)<before,"press reserves cost and starts charge");
        h.assertTrue(com.jjkcraft.technique.field.SpatialFields.active(h.getLevel()).stream().noneMatch(f->f.caster()==p),"press does not fire");
        h.runAfterDelay(20,()->{h.assertTrue(TechniqueService.chargePercent(p)>=50,"server advances held charge");double paid=CursedEnergy.current(p);TechniqueService.onCastKey(p,1,false);h.assertTrue(TechniqueService.chargePercent(p)>0,"unrelated release leaves charge alone");TechniqueService.onCastKey(p,0,false);h.assertTrue(TechniqueService.chargePercent(p)==0&&TechniqueService.readyAt(p,JjkCraft.id("lapse_blue"))>p.level().getGameTime(),"release fires and starts cooldown");h.assertTrue(Math.abs(CursedEnergy.current(p)-paid)<.01,"release never charges base twice");TechniqueService.onCastKey(p,0,false);h.succeed();});
    }
    @GameTest(environment="jjkcraft:expansion",padding=24,maxTicks=220) public void purpleActuallyReachesTwoHundredOnServer(GameTestHelper h){var p=player(h);bind(p,"hollow_purple");TechniqueService.onCastKey(p,0,true);h.runAfterDelay(145,()->{h.assertTrue(TechniqueService.chargePercent(p)==200,"purple charge caps at 200");TechniqueService.onCastKey(p,0,false);h.assertTrue(TechniqueService.chargePercent(p)==0,"release clears meter");h.succeed();});}
    @GameTest(environment="jjkcraft:expansion",padding=24) public void cancellingAndUnbindingNeverFireOrRefund(GameTestHelper h){var p=player(h);bind(p,"lapse_blue");double before=CursedEnergy.current(p);TechniqueService.onCastKey(p,0,true);TechniqueService.setLoadout(p,0,Optional.empty());h.assertTrue(TechniqueService.chargePercent(p)==0&&CursedEnergy.current(p)<before,"unbind cancels without refund");TechniqueService.onCastKey(p,0,false);h.assertTrue(com.jjkcraft.technique.field.SpatialFields.active(h.getLevel()).stream().noneMatch(f->f.caster()==p),"cancel never fires");h.succeed();}
    @GameTest(environment="jjkcraft:expansion",padding=24) public void exhaustionCancelsInsteadOfGivingFreePower(GameTestHelper h){var p=player(h);bind(p,"lapse_blue");TechniqueService.onCastKey(p,0,true);CursedEnergy.set(p,0);h.runAfterDelay(3,()->{h.assertTrue(TechniqueService.chargePercent(p)==0,"cannot hold with no CE");h.succeed();});}
    @GameTest(environment="jjkcraft:expansion",padding=24,maxTicks=60) public void bluePullsRealTreeBlocksIntoTransientDebris(GameTestHelper h){
        var p=player(h);var pos=h.absolutePos(new BlockPos(5,5,5));h.getLevel().setBlockAndUpdate(pos,Blocks.OAK_LOG.defaultBlockState());h.getLevel().setBlockAndUpdate(pos.above(),Blocks.OAK_LEAVES.defaultBlockState());
        var c=ctx(p,"lapse_blue");int n=TechniqueTerrain.sphere(c,Vec3.atCenterOf(pos),2,8,true);h.assertTrue(n>=2&&h.getLevel().getBlockState(pos).isAir(),"logs and leaves are removed");
        var displays=h.getLevel().getEntitiesOfClass(Display.BlockDisplay.class,new net.minecraft.world.phys.AABB(pos).inflate(3));h.assertTrue(!displays.isEmpty()&&displays.stream().noneMatch(Display.BlockDisplay::shouldBeSaved),"debris is visible and cannot persist or duplicate blocks");h.runAfterDelay(22,()->{h.assertTrue(displays.stream().allMatch(Display.BlockDisplay::isRemoved),"debris is consumed at core");h.succeed();});
    }
    @GameTest(environment="jjkcraft:expansion",padding=24) public void terrainPreservesContainersAndUnbreakableBlocks(GameTestHelper h){
        var p=player(h);var pos=h.absolutePos(new BlockPos(5,6,5));h.getLevel().setBlockAndUpdate(pos,Blocks.CHEST.defaultBlockState());h.getLevel().setBlockAndUpdate(pos.above(),Blocks.BEDROCK.defaultBlockState());var c=ctx(p,"hollow_purple");TechniqueTerrain.sphere(c,Vec3.atCenterOf(pos),2,64,false);h.assertTrue(h.getLevel().getBlockState(pos).is(Blocks.CHEST)&&h.getLevel().getBlockState(pos.above()).is(Blocks.BEDROCK),"containers and bedrock protected");h.succeed();
    }
    @GameTest(environment="jjkcraft:expansion",padding=24) public void purpleDrillsThroughWoodInsteadOfStoppingAtFirstTree(GameTestHelper h){var p=player(h);var pos=BlockPos.containing(p.getEyePosition().add(0,0,3));h.getLevel().setBlockAndUpdate(pos,Blocks.OAK_LOG.defaultBlockState());TechniqueTerrain.line(ctx(p,"hollow_purple"),p.getEyePosition(),p.getEyePosition().add(0,0,6),1.6);h.assertTrue(h.getLevel().getBlockState(pos).isAir(),"purple clears its flight corridor");h.succeed();}
}
