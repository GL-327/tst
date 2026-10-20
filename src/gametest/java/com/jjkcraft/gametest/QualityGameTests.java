package com.jjkcraft.gametest;

import com.jjkcraft.JjkCraft;
import com.jjkcraft.combat.CursedCombat;
import com.jjkcraft.core.domain.DomainMath;
import com.jjkcraft.damage.*;
import com.jjkcraft.domain.*;
import com.jjkcraft.energy.CursedEnergy;
import com.jjkcraft.network.payload.CombatPayloads;
import com.jjkcraft.sorcerer.*;
import com.jjkcraft.technique.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.phys.Vec3;

public final class QualityGameTests {
    private static net.minecraft.server.level.ServerPlayer player(GameTestHelper h){var p=TestPlayers.fresh(h);Awakening.awaken(p,false);Progression.setGrade(p,JjkCraft.id("special_grade"));CursedEnergy.fill(p);return p;}
    @GameTest(environment="jjkcraft:expansion") public void slotMovesAreUniqueAndInvalidSlotsCannotEraseThem(GameTestHelper h){
        var p=player(h);var id=JjkCraft.id("ink_step");TechniqueService.grant(p,id);TechniqueService.setLoadout(p,0,java.util.Optional.of(id));TechniqueService.setLoadout(p,3,java.util.Optional.of(id));
        h.assertTrue(TechniqueService.get(p).slot(0).isEmpty()&&TechniqueService.get(p).slot(3).orElseThrow().equals(id),"moving a binding clears the old copy");
        TechniqueService.setLoadout(p,12,java.util.Optional.of(id));h.assertTrue(TechniqueService.get(p).slot(3).orElseThrow().equals(id),"forged thirteenth slot cannot erase a valid binding");h.succeed();
    }
    @GameTest(environment="jjkcraft:expansion") public void sureHitsCannotBeParriedOrDodged(GameTestHelper h){
        var p=player(h);var attacker=h.spawn(EntityTypes.HUSK,new Vec3(0.5,1,2.5));p.setPos(attacker.getX(),attacker.getY(),attacker.getZ()-2);p.setYRot(0);p.setXRot(0);
        CursedCombat.input(p,new CombatPayloads.Input(2,1));
        var sure=new net.minecraft.world.damagesource.DamageSource(h.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE).getOrThrow(JjkDamageTypes.SURE_HIT),attacker);
        h.assertTrue(DamageModifiers.apply(h.getLevel(),p,sure,8)>0,"fresh parry cannot negate sure-hit");
        CursedCombat.input(p,new CombatPayloads.Input(2,-1));CursedCombat.input(p,new CombatPayloads.Input(3,0));
        h.assertTrue(DamageModifiers.apply(h.getLevel(),p,sure,8)>0,"dodge frames cannot negate sure-hit");h.succeed();
    }
    @GameTest(environment="jjkcraft:expansion",padding=48,maxTicks=90) public void outsideAttacksDamageTheBarrierAndBreachIt(GameTestHelper h){
        var p=player(h);h.assertTrue(DomainService.expand(p,JjkCraft.id("malevolent_shrine"),DomainMath.Variant.FULL,true)==CastFailure.NONE,"domain starts");var d=DomainService.active(p).orElseThrow();
        var outside=h.spawn(EntityTypes.HUSK,new Vec3(d.radius()+4,1,0.5));outside.setNoAi(true);
        h.startSequence().thenWaitUntil(()->h.assertTrue(d.phase()==ActiveDomain.Phase.ACTIVE,"active"))
            .thenExecute(()->{float before=d.integrity();var attack=h.getLevel().damageSources().mobAttack(outside);h.assertTrue(DamageModifiers.apply(h.getLevel(),p,attack,20)==0,"closed barrier intercepts outside attack");h.assertTrue(d.integrity()<before,"barrier loses integrity");DamageModifiers.apply(h.getLevel(),p,attack,1000);})
            .thenWaitUntil(()->h.assertTrue(d.phase()==ActiveDomain.Phase.CLOSING,"broken barrier collapses"))
            .thenExecute(()->DomainService.close(p)).thenSucceed();
    }
    @GameTest(environment="jjkcraft:expansion",padding=48,maxTicks=60) public void damageDuringHandSignInterruptsExpansion(GameTestHelper h){
        var p=h.spawn(com.jjkcraft.curse.CurseEntities.ASH_ONI,1,1,1,net.minecraft.world.entity.EntitySpawnReason.EVENT);p.setNoAi(true);CursedEnergy.fill(p);
        h.assertTrue(DomainService.expand(p,JjkCraft.id("malevolent_shrine"),DomainMath.Variant.FULL,true)==CastFailure.NONE,"domain starts");var d=DomainService.active(p).orElseThrow();
        h.assertTrue(p.hurtServer(h.getLevel(),h.getLevel().damageSources().generic(),p.getMaxHealth()*0.5f),"fixture takes real damage");
        h.startSequence().thenWaitUntil(()->h.assertTrue(d.phase()==ActiveDomain.Phase.CLOSING,"damage interrupts opening"))
            .thenExecute(()->{h.assertFalse(d.tookHold(),"no sure-hit or scenery remains active");DomainService.close(p);}).thenSucceed();
    }
    @GameTest(environment="jjkcraft:expansion",maxTicks=60) public void finisherKnocksDownAndEvasiveHasARealCooldown(GameTestHelper h){
        // Vanilla's mock overrides gameMode() to CREATIVE permanently, so it cannot be hit.
        var cookie=net.minecraft.server.network.CommonListenerCookie.createInitial(new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"QualitySurvival"),false);
        var p=new net.minecraft.server.level.ServerPlayer(h.getLevel().getServer(),h.getLevel(),cookie.gameProfile(),cookie.clientInformation());
        var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);new io.netty.channel.embedded.EmbeddedChannel(connection);
        h.getLevel().getServer().getPlayerList().placeNewPlayer(connection,p,cookie);p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);Awakening.reset(p);Awakening.awaken(p,false);CursedEnergy.fill(p);
        var attacker=h.spawn(com.jjkcraft.curse.CurseEntities.LEDGER_CRAWLER,0,1,0,net.minecraft.world.entity.EntitySpawnReason.EVENT);attacker.setNoAi(true);p.setPos(attacker.getX(),attacker.getY(),attacker.getZ()+2);p.getAbilities().invulnerable=false;p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(100);p.setHealth(100);
        // 26.3 also protects newly connected players until their client acknowledges world loading.
        p.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
        h.assertFalse(p.isInvulnerableTo(h.getLevel(),h.getLevel().damageSources().mobAttack(attacker)),"loaded survival fixture can take damage");
        CursedEnergy.fill(attacker);h.assertTrue(CursedCombat.start(attacker,JjkCraft.id("heavy"),p.getEyePosition().subtract(attacker.getEyePosition())),"finisher starts");
        h.startSequence().thenWaitUntil(()->h.assertTrue(CursedCombat.knockedDown(p),"finisher knocks down"))
            .thenExecute(()->{double before=CursedEnergy.current(p);CursedCombat.input(p,new CombatPayloads.Input(3,0));h.assertFalse(CursedCombat.knockedDown(p),"evasive escapes knockdown");h.assertTrue(CursedCombat.escapeReady(p)>p.level().getGameTime()+190,"ten second escape cooldown");h.assertTrue(before-CursedEnergy.current(p)>32,"escape is paid atomically");h.getLevel().getServer().getPlayerList().remove(p);p.discard();}).thenSucceed();
    }
    @GameTest(environment="jjkcraft:expansion") public void releasingAnotherSkillCannotCancelAChannel(GameTestHelper h){
        var p=player(h);var id=JjkCraft.id("lacquer_ward");TechniqueService.grant(p,id);TechniqueService.setLoadout(p,2,java.util.Optional.of(id));
        h.assertTrue(TechniqueService.cast(p,id,2)==CastFailure.NONE,"channel begins");
        TechniqueService.onCastKey(p,0,false);h.assertTrue(TechniqueService.active(p).isPresent(),"another key release leaves channel running");
        TechniqueService.onCastKey(p,2,false);h.assertTrue(TechniqueService.active(p).isEmpty(),"own key release ends channel");h.succeed();
    }
}
