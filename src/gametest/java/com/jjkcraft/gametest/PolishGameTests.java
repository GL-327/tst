package com.jjkcraft.gametest;

import java.util.*;
import com.jjkcraft.JjkCraft;
import com.jjkcraft.awakening.CharacterAwakening;
import com.jjkcraft.energy.CursedEnergy;
import com.jjkcraft.reverse.ReverseTechnique;
import com.jjkcraft.technique.*;
import com.jjkcraft.technique.behavior.InfinityBehavior;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

public final class PolishGameTests {
    private static void floor(GameTestHelper h){for(int x=-4;x<=12;x++)for(int z=-4;z<=14;z++)h.setBlock(new BlockPos(x,0,z),net.minecraft.world.level.block.Blocks.STONE);}
    @GameTest(environment="jjkcraft:expansion",padding=32,maxTicks=65) public void aBriefStunPermanentlyInterruptsThePaidWindup(GameTestHelper h){
        var p=player(h);var id=JjkCraft.id("grudge_bolt");TechniqueService.grant(p,id);Personalization.save(p,id,TechniqueTuning.DEFAULT.with(12,40));h.assertTrue(TechniqueService.cast(p,id,0)==CastFailure.NONE,"long windup begins");
        h.runAfterDelay(2,()->p.addEffect(new net.minecraft.world.effect.MobEffectInstance(com.jjkcraft.effect.JjkEffects.STUNNED,3,0)));
        h.runAfterDelay(48,()->{h.assertTrue(p.level().getEntitiesOfClass(com.jjkcraft.entity.CursedBoltEntity.class,p.getBoundingBox().inflate(80),e->e.getOwner()==p).isEmpty(),"windup cannot resume when a brief interruption has already worn off");h.assertTrue(TechniqueService.readyAt(p,id)==0,"interrupted cast never begins a success cooldown");h.succeed();});
    }
    @GameTest(environment="jjkcraft:expansion",padding=32,maxTicks=40) public void realCombatHitCancelsChargeAndPreventsCastingDuringStun(GameTestHelper h){
        floor(h);var cookie=net.minecraft.server.network.CommonListenerCookie.createInitial(new com.mojang.authlib.GameProfile(UUID.randomUUID(),"PolishSurvival"),false);var p=new ServerPlayer(h.getLevel().getServer(),h.getLevel(),cookie.gameProfile(),cookie.clientInformation());var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);new io.netty.channel.embedded.EmbeddedChannel(connection);h.getLevel().getServer().getPlayerList().placeNewPlayer(connection,p,cookie);
        p.setGameMode(GameType.CREATIVE);CharacterAwakening.creative(p,JjkCraft.id("limitless"),false);p.setGameMode(GameType.SURVIVAL);p.setPos(h.absoluteVec(new Vec3(2.5,1,4.5)));p.getAbilities().invulnerable=false;p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(100);p.setHealth(100);p.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
        TechniqueService.setLoadout(p,0,Optional.of(JjkCraft.id("lapse_blue")));TechniqueService.onCastKey(p,0,true);h.assertTrue(TechniqueService.chargePercent(p)>0,"real player is holding Blue");var attacker=h.spawn(com.jjkcraft.curse.CurseEntities.LEDGER_CRAWLER,new Vec3(2.5,1,2.5));attacker.setNoAi(true);CursedEnergy.fill(attacker);h.assertTrue(com.jjkcraft.combat.CursedCombat.start(attacker,JjkCraft.id("jab"),p.getEyePosition().subtract(attacker.getEyePosition())),"actual jab begins");
        h.startSequence().thenWaitUntil(()->h.assertTrue(com.jjkcraft.combat.CursedCombat.interrupted(p),"jab causes combat stagger")).thenIdle(2).thenExecute(()->{h.assertTrue(TechniqueService.chargePercent(p)==0,"combat stagger cancels held energy");h.assertTrue(TechniqueService.cast(p,JjkCraft.id("lapse_blue"),0)==CastFailure.STUNNED,"cannot start a power during combat stagger");h.getLevel().getServer().getPlayerList().remove(p);p.discard();}).thenSucceed();
    }
    @GameTest(environment="jjkcraft:expansion",padding=32) public void unrelatedSlotEditsPreserveAChargedFamilyVariant(GameTestHelper h){
        var p=player(h);TechniqueService.setLoadout(p,0,Optional.of(TechniqueBindings.binding(JjkCraft.id("limitless"))));TechniqueService.onVariant(p,0,JjkCraft.id("lapse_blue"));h.assertTrue(TechniqueService.chargePercent(p)>0,"family variant starts charging");
        TechniqueService.setLoadout(p,11,Optional.empty());h.assertTrue(TechniqueService.chargePercent(p)>0,"unrelated edit cannot mistake a family selector for a different charged move");
        TechniqueService.setLoadout(p,0,Optional.of(JjkCraft.id("lapse_blue")));h.assertTrue(TechniqueService.chargePercent(p)>0,"pinning the same charged variant preserves its cast");
        TechniqueService.setLoadout(p,0,Optional.empty());h.assertTrue(TechniqueService.chargePercent(p)==0,"actually removing its binding cancels the charge");h.succeed();
    }
    @GameTest(environment="jjkcraft:expansion",padding=32) public void spatialPunchForceIsIndependentAndInstantDurationCannotInflateCost(GameTestHelper h){
        floor(h);var p=player(h);var id=JjkCraft.id("blue_punch");TechniqueService.grant(p,id);var raw=Techniques.get(p.registryAccess(),id).orElseThrow();var target=h.spawn(EntityTypes.HUSK,new Vec3(2.5,1,4.5));target.setNoAi(true);
        h.assertTrue(TechniqueDetails.applicable(raw,11),"spatial punches expose their actual pull/push force");
        h.assertTrue(TechniqueTuning.DEFAULT.with(3,300).normalized(raw).equals(TechniqueTuning.DEFAULT),"instant punches cannot pay CE for an unused duration setting");
        LimitlessArts.cast(new CastContext(p,h.getLevel(),id,raw,1,1),"blue_punch",1,1,2);double baseline=target.getDeltaMovement().length();target.setDeltaMovement(Vec3.ZERO);target.damageCooldownTime=0;
        Personalization.save(p,id,TechniqueTuning.DEFAULT.with(11,300));var tuned=Personalization.definition(p,id,raw);LimitlessArts.cast(new CastContext(p,h.getLevel(),id,tuned,1,1),"blue_punch",1,1,2);
        h.assertTrue(Math.abs(target.getDeltaMovement().length()-baseline*3)<.001,"force changes the real pull independently of damage");h.succeed();
    }
    @GameTest(environment="jjkcraft:expansion",padding=32) public void projectionUsesExactSlabHeightAndCrossesASafeHalfStep(GameTestHelper h){
        floor(h);var p=player(h);h.setBlock(new BlockPos(2,1,5),net.minecraft.world.level.block.Blocks.STONE_SLAB);Vec3 requested=h.absoluteVec(new Vec3(2.5,1,5.5)),foot=ProjectionPlans.ground(p,requested);
        h.assertTrue(foot!=null&&Math.abs(foot.y-requested.y-.5)<.001,"feet land exactly on the half slab, without hovering");h.assertTrue(ProjectionPlans.sweep(p,foot),"safe half-step can be traversed");h.succeed();
    }
    @GameTest(environment="jjkcraft:expansion",padding=32) public void projectionRejectsNarrowGapsAndOverHighSteps(GameTestHelper h){
        floor(h);var p=player(h);Vec3 target=h.absoluteVec(new Vec3(2.5,1,6.5));for(int x=-2;x<=7;x++)for(int y=-3;y<=0;y++)h.setBlock(new BlockPos(x,y,4),net.minecraft.world.level.block.Blocks.AIR);
        h.assertTrue(ProjectionPlans.ground(p,target)!=null&&!ProjectionPlans.sweep(p,target),"safe endpoint cannot hide a one-block gap along the path");
        for(int x=-2;x<=7;x++)for(int y=0;y<=2;y++)h.setBlock(new BlockPos(x,y,4),net.minecraft.world.level.block.Blocks.STONE);h.assertFalse(ProjectionPlans.sweep(p,target),"cannot climb a two-block wall");h.succeed();
    }
    @GameTest(environment="jjkcraft:expansion",padding=32,maxTicks=15) public void targetlessProjectionPresetMovesWithoutThrowingAirAttacks(GameTestHelper h){
        floor(h);var p=player(h);TechniqueService.grant(p,JjkCraft.id("projection_sorcery"));var d=Techniques.get(p.registryAccess(),JjkCraft.id("projection_sorcery")).orElseThrow();Vec3 from=p.position();ProjectionPlans.cast(new CastContext(p,h.getLevel(),JjkCraft.id("projection_sorcery"),d,1,1),8);
        h.runAfterDelay(2,()->{h.assertTrue(p.position().distanceTo(from)>3,"targetless preset still provides movement");h.assertTrue(com.jjkcraft.combat.CursedCombat.currentMove(p).isEmpty(),"no phantom combo against empty space");h.succeed();});
    }
    private static ServerPlayer player(GameTestHelper h){var p=TestPlayers.fresh(h);p.setGameMode(GameType.CREATIVE);CharacterAwakening.creative(p,JjkCraft.id("limitless"),false);p.setGameMode(GameType.SURVIVAL);p.setPos(h.absoluteVec(new Vec3(2.5,1,2.5)));p.setYRot(0);p.setXRot(0);return p;}
    @GameTest(environment="jjkcraft:expansion",padding=32) public void infinityProjectileFilterPersistsAndDefaultsRestoreCombinedProtection(GameTestHelper h){
        var p=player(h);var id=JjkCraft.id("infinity");var raw=Techniques.get(p.registryAccess(),id).orElseThrow();
        h.assertTrue(TechniqueTuning.defaultFor(raw).apply(raw).equals(raw),"authored default does not silently inflate CE upkeep");
        h.assertTrue(Personalization.save(p,id,TechniqueTuning.DEFAULT),"projectile-only filter saves");h.assertTrue(p.getAttachedOrCreate(Personalization.TUNINGS).containsKey(id),"explicit projectile filter must not be erased as a default");
        h.assertTrue(((InfinityBehavior)Personalization.definition(p,id,raw).behavior()).filter()==0,"saved filter applies to the actual field");TechniqueService.cast(p,id,-1);
        var enemy=h.spawn(EntityTypes.HUSK,new Vec3(2.5,1,4));enemy.setNoAi(true);h.assertTrue(com.jjkcraft.damage.DamageModifiers.apply(h.getLevel(),p,h.getLevel().damageSources().mobAttack(enemy),8)>0,"projectile-only field permits contact");
        var copy=TestPlayers.fresh(h);com.jjkcraft.sorcerer.SorcererLifecycle.copyOnDeath(p,copy);h.assertTrue(Personalization.get(copy,id).mode()==0,"filter persists through ordinary death");
        h.assertTrue(Personalization.save(p,id,TechniqueTuning.defaultFor(raw)),"reset to combined");h.assertFalse(p.getAttachedOrCreate(Personalization.TUNINGS).containsKey(id),"authored default can be stored sparsely");TechniqueService.cast(p,id,-1);h.assertTrue(com.jjkcraft.damage.DamageModifiers.apply(h.getLevel(),p,h.getLevel().damageSources().mobAttack(enemy),8)==0,"restored default blocks contact");h.succeed();
    }
    @GameTest(environment="jjkcraft:expansion",padding=32,maxTicks=30) public void revokedTechniqueCannotExecuteItsAlreadyPaidWindup(GameTestHelper h){
        var p=player(h);var id=JjkCraft.id("grudge_bolt");TechniqueService.grant(p,id);Personalization.save(p,id,TechniqueTuning.DEFAULT.with(12,16));CursedEnergy.fill(p);h.assertTrue(TechniqueService.cast(p,id,0)==CastFailure.NONE,"paid windup begins");TechniqueService.revoke(p,id);
        h.runAfterDelay(22,()->{h.assertTrue(p.level().getEntitiesOfClass(com.jjkcraft.entity.CursedBoltEntity.class,p.getBoundingBox().inflate(45),e->e.getOwner()==p).isEmpty(),"lost knowledge prevents the delayed projectile");h.assertTrue(TechniqueService.readyAt(p,id)==0,"an aborted cast does not start its success cooldown");h.succeed();});
    }
    @GameTest(environment="jjkcraft:expansion",padding=32,maxTicks=30) public void enteringSpectatorCancelsPendingAndSustainedPowers(GameTestHelper h){
        var p=player(h);TechniqueService.grant(p,JjkCraft.id("grudge_bolt"));Personalization.save(p,JjkCraft.id("grudge_bolt"),TechniqueTuning.DEFAULT.with(12,16));TechniqueService.cast(p,JjkCraft.id("grudge_bolt"),0);
        var ward=player(h);TechniqueService.grant(ward,JjkCraft.id("lacquer_ward"));TechniqueService.cast(ward,JjkCraft.id("lacquer_ward"),0);p.setGameMode(GameType.SPECTATOR);ward.setGameMode(GameType.SPECTATOR);
        h.runAfterDelay(22,()->{h.assertTrue(TechniqueService.active(ward).isEmpty(),"spectator cannot retain a ward");h.assertTrue(p.level().getEntitiesOfClass(com.jjkcraft.entity.CursedBoltEntity.class,p.getBoundingBox().inflate(45),e->e.getOwner()==p).isEmpty(),"spectator cannot fire a pending attack");h.succeed();});
    }
    @GameTest(environment="jjkcraft:expansion",padding=32) public void unavailableReplacementDoesNotCancelTheExistingChannel(GameTestHelper h){
        var p=player(h);TechniqueService.grant(p,JjkCraft.id("lacquer_ward"));TechniqueService.cast(p,JjkCraft.id("lacquer_ward"),0);CursedEnergy.set(p,1);
        h.assertTrue(TechniqueService.cast(p,JjkCraft.id("reverse_cursed_technique"),1)==CastFailure.NOT_ENOUGH_ENERGY,"replacement fails for insufficient CE");h.assertTrue(TechniqueService.active(p).filter(a->a.id().equals(JjkCraft.id("lacquer_ward"))).isPresent(),"existing paid ward survives a failed replacement");h.succeed();
    }
    @GameTest(environment="jjkcraft:expansion",padding=32) public void deadCastersCannotSpendEnergyOrPerformNewCasts(GameTestHelper h){
        var p=player(h);TechniqueService.grant(p,JjkCraft.id("grudge_bolt"));p.setHealth(0);double ce=CursedEnergy.current(p);h.assertTrue(TechniqueService.cast(p,JjkCraft.id("grudge_bolt"),0)==CastFailure.DISABLED,"dead caster rejected at entry");h.assertTrue(CursedEnergy.current(p)==ce,"rejected dead cast does not spend CE");h.succeed();
    }
    @GameTest(environment="jjkcraft:expansion",padding=32) public void allyMendingCannotHealThroughSolidTerrain(GameTestHelper h){
        var p=player(h);var ally=h.spawn(com.jjkcraft.entity.JjkEntities.TOAD,new Vec3(2.5,1,6));ally.bind(p,600,1);ally.setNoAi(true);ally.setHealth(5);
        for(int x=1;x<=4;x++)for(int y=1;y<=4;y++)h.setBlock(new BlockPos(x,y,4),net.minecraft.world.level.block.Blocks.STONE);p.setHealth(10);TechniqueService.cast(p,JjkCraft.id("positive_energy_output"),0);h.assertTrue(ally.getHealth()==5,"wall blocks mending beam");h.assertTrue(p.getHealth()>10,"unobstructed self fallback still works");h.succeed();
    }
}
