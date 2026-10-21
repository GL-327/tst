package com.jjkcraft.gametest;
import java.util.*;
import com.jjkcraft.JjkCraft;
import com.jjkcraft.awakening.CharacterAwakening;
import com.jjkcraft.combat.CursedCombat;
import com.jjkcraft.energy.CursedEnergy;
import com.jjkcraft.reverse.*;
import com.jjkcraft.technique.*;
import com.jjkcraft.technique.behavior.MangaAbilityBehavior;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public final class RebornGameTests {
    private static ServerPlayer player(GameTestHelper h,String family){var p=TestPlayers.fresh(h);p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);CharacterAwakening.creative(p,JjkCraft.id(family),false);p.setPos(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(2,1,2))));p.setYRot(0);p.setXRot(0);CursedEnergy.fill(p);return p;}
    private static void floor(GameTestHelper h){for(int x=-12;x<=14;x++)for(int z=-12;z<=14;z++)h.getLevel().setBlockAndUpdate(h.absolutePos(new BlockPos(x,0,z)),Blocks.STONE.defaultBlockState());}
    private static net.minecraft.world.entity.monster.zombie.Husk target(GameTestHelper h){var t=h.spawn(EntityTypes.HUSK,new Vec3(2.5,1,6));t.setNoAi(true);t.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100000);t.setHealth(100000);t.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);return t;}
    private static CastContext ctx(ServerPlayer p,String key){var id=JjkCraft.id(key);return new CastContext(p,p.level(),id,Techniques.get(p.registryAccess(),id).orElseThrow(),1,1);}
    @GameTest(environment="jjkcraft:expansion",padding=32) public void twentyNewFamiliesHaveSixUsableDefinedMovesAndBalancedRandomWeights(GameTestHelper h){
        var p=player(h,"glass");for(String family:OriginalArts.FAMILIES){var f=p.registryAccess().lookupOrThrow(CharacterAwakening.FAMILIES).getValue(JjkCraft.id(family));h.assertTrue(f!=null&&f.techniques().size()==6&&f.randomWeight()==10,"six complete moves and equal Random weight: "+family);for(var id:f.techniques()){var d=Techniques.get(p.registryAccess(),id).orElseThrow();h.assertTrue(d.behavior() instanceof MangaAbilityBehavior m&&OriginalArts.handles(m.ability()),"real original mechanic: "+id);}}
        h.assertTrue(Techniques.all(p.registryAccess()).size()==296,"296 defined techniques");h.succeed();
    }
    @GameTest(environment="jjkcraft:expansion",padding=32) public void everyOriginalPrimerActuallyHitsAndBuildsItsOwnResource(GameTestHelper h){
        var p=player(h,"glass");var t=target(h);var origin=t.position();for(String family:OriginalArts.FAMILIES){t.setPos(origin);t.damageCooldownTime=0;float before=t.getHealth();var c=ctx(p,family+"_mark");c.technique().behavior().cast(c);h.assertTrue(t.getHealth()<before,"primer damages an aimed opponent: "+family);h.assertTrue(OriginalArts.charges(p,family)==1,"each family has independent resonance: "+family);}h.succeed();
    }
    @GameTest(environment="jjkcraft:expansion",padding=32) public void preparedFinisherConsumesMarksAndOutperformsUnpreparedFinisher(GameTestHelper h){
        var p=player(h,"glass");var t=target(h);var c=ctx(p,"glass_finish");float before=t.getHealth();c.technique().behavior().cast(c);float unprepared=before-t.getHealth();
        for(int i=0;i<3;i++){t.damageCooldownTime=0;var mark=ctx(p,"glass_mark");mark.technique().behavior().cast(mark);}t.damageCooldownTime=0;before=t.getHealth();c.technique().behavior().cast(c);h.assertTrue(before-t.getHealth()>unprepared*2,"three-layer payoff rewards setup");h.assertTrue(OriginalArts.charges(p,"glass")==0,"finisher consumes resonance");h.succeed();
    }
    @GameTest(environment="jjkcraft:expansion",padding=32) public void originalMovementDoesNotPhaseThroughSolidWalls(GameTestHelper h){
        floor(h);var p=player(h,"momentum");for(int x=0;x<5;x++)for(int y=1;y<5;y++)h.getLevel().setBlockAndUpdate(h.absolutePos(new BlockPos(x,y,4)),Blocks.STONE.defaultBlockState());Vec3 before=p.position();var c=ctx(p,"momentum_step");c.technique().behavior().cast(c);h.assertTrue(p.position().equals(before),"blocked swept path cannot teleport through wall");h.succeed();
    }
    @GameTest(environment="jjkcraft:expansion",padding=32) public void originalGuardsArePaidBoundedAndExhaustAfterThreeHits(GameTestHelper h){
        var p=player(h,"porcelain");var c=ctx(p,"porcelain_guard");c.technique().behavior().cast(c);double before=CursedEnergy.current(p);
        for(int i=0;i<3;i++)h.assertTrue(com.jjkcraft.damage.DamageModifiers.apply(h.getLevel(),p,h.getLevel().damageSources().generic(),10)==8,"guard wards only 20%");h.assertTrue(CursedEnergy.current(p)<before,"each ward spends real CE");h.assertTrue(com.jjkcraft.damage.DamageModifiers.apply(h.getLevel(),p,h.getLevel().damageSources().generic(),10)==10,"three-hit ward expires");h.succeed();
    }
    @GameTest(environment="jjkcraft:expansion",padding=32) public void explosiveFleshPaysRealHealthEvenInCreativeAndRefusesUnsafeCast(GameTestHelper h){
        var p=player(h,"explosive_flesh");float health=p.getHealth();h.assertTrue(TechniqueService.cast(p,JjkCraft.id("tooth_blast"),-1)==CastFailure.NONE,"body ammunition casts");h.assertTrue(p.getHealth()==health-2,"tooth shot sacrifices two actual health");TechniqueService.resetCooldowns(p);p.setHealth(3);double ce=CursedEnergy.current(p);h.assertTrue(TechniqueService.cast(p,JjkCraft.id("tooth_blast"),-1)==CastFailure.NOT_ENOUGH_HEALTH,"cannot spend the last safe health");h.assertTrue(CursedEnergy.current(p)==ce,"refused cast takes no CE");h.succeed();
    }
    @GameTest(environment="jjkcraft:expansion",padding=32) public void ammunitionRegrowthRequiresLearnedRct(GameTestHelper h){
        var p=player(h,"explosive_flesh");ReverseTechnique.set(p,ReverseTechnique.state(p).withUnlocked(false).withAutoHeal(false),false);h.assertTrue(TechniqueService.cast(p,JjkCraft.id("flesh_regrow"),-1)==CastFailure.NOT_KNOWN,"family membership alone cannot bypass RCT learning");ReverseTechnique.set(p,ReverseTechnique.state(p).withUnlocked(true),false);p.setHealth(10);h.assertTrue(TechniqueService.cast(p,JjkCraft.id("flesh_regrow"),-1)==CastFailure.NONE,"learned RCT permits regeneration");h.assertTrue(p.getHealth()>10,"regrowth heals the body");h.succeed();
    }
    @GameTest(environment="jjkcraft:expansion",padding=32,maxTicks=150) public void automaticProjectionUsesPhysicalAttacksAndFinishesItsRoute(GameTestHelper h){
        floor(h);var p=player(h,"projection");var t=target(h);float before=t.getHealth();Vec3 from=p.position();var c=ctx(p,"projection_sorcery");c.technique().behavior().cast(c);h.assertTrue(ProjectionPlans.running(p),"terrain preset schedules a real route");h.runAfterDelay(130,()->{h.assertTrue(!ProjectionPlans.running(p),"route completes without looping");h.assertTrue(p.position().distanceTo(from)>.5,"route moves the player");h.assertTrue(t.getHealth()<before,"real framed attacks hit the opponent");h.succeed();});
    }
    @GameTest(environment="jjkcraft:expansion",padding=32,maxTicks=25) public void customProjectionRejectsWallPhasingAndPunishesBrokenFrame(GameTestHelper h){
        floor(h);var p=player(h,"projection");h.assertTrue(ProjectionPlans.set(p,new ProjectionPlan(false,List.of(new ProjectionPlan.Step(0,4,1)))),"custom route saved");for(int x=0;x<5;x++)for(int y=1;y<5;y++)h.getLevel().setBlockAndUpdate(h.absolutePos(new BlockPos(x,y,4)),Blocks.STONE.defaultBlockState());Vec3 before=p.position();var c=ctx(p,"projection_sorcery");c.technique().behavior().cast(c);h.runAfterDelay(2,()->{h.assertTrue(p.position().equals(before),"swept body cannot cross wall");h.assertTrue(p.hasEffect(com.jjkcraft.effect.JjkEffects.FROZEN_FRAME),"breaking a frame briefly freezes caster");h.succeed();});
    }
    @GameTest(environment="jjkcraft:expansion",padding=32) public void projectionPlanRejectsNonfiniteAndOversizedRoutes(GameTestHelper h){
        var p=player(h,"projection");h.assertFalse(ProjectionPlans.set(p,new ProjectionPlan(false,List.of(new ProjectionPlan.Step(Double.NaN,0,1)))),"NaN rejected");h.assertFalse(ProjectionPlans.set(p,new ProjectionPlan(false,Collections.nCopies(9,new ProjectionPlan.Step(0,1,1)))),"ninth waypoint rejected");h.assertFalse(ProjectionPlans.set(p,new ProjectionPlan(false,List.of(new ProjectionPlan.Step(30,0,1)))),"out-of-range waypoint rejected");h.succeed();
    }
}
