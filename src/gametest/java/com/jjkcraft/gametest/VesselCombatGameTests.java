package com.jjkcraft.gametest;

import java.util.*;
import com.jjkcraft.JjkCraft;
import com.jjkcraft.awakening.*;
import com.jjkcraft.combat.CursedCombat;
import com.jjkcraft.domain.*;
import com.jjkcraft.energy.CursedEnergy;
import com.jjkcraft.core.domain.DomainMath;
import com.jjkcraft.network.payload.*;
import com.jjkcraft.technique.*;
import com.jjkcraft.vessel.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

public final class VesselCombatGameTests {
    private static ServerPlayer player(GameTestHelper h){var p=TestPlayers.fresh(h);p.setGameMode(GameType.CREATIVE);CharacterAwakening.creative(p,JjkCraft.id("blood_manipulation"),false);p.setGameMode(GameType.SURVIVAL);p.setOnGround(true);p.setYRot(0);p.setXRot(0);CursedEnergy.fill(p);return p;}
    @GameTest(environment="jjkcraft:expansion",padding=48,maxTicks=65)
    public void alternatingHandsCancelRecoveryButSameHandMustRecover(GameTestHelper h){
        var p=player(h);h.assertTrue(CursedCombat.startPunch(p,JjkCraft.id("jab"),new Vec3(0,0,1),1),"right jab starts");
        h.assertFalse(CursedCombat.startPunch(p,JjkCraft.id("jab"),new Vec3(0,0,1),-1),"cannot overlap startup");
        h.runAfterDelay(5,()->{h.assertFalse(CursedCombat.startPunch(p,JjkCraft.id("jab"),new Vec3(0,0,1),1),"same hand still recovering");h.assertTrue(CursedCombat.startPunch(p,JjkCraft.id("jab"),new Vec3(0,0,1),-1),"opposite hand cancels recovery at five ticks");h.assertValueEqual(CursedCombat.currentHand(p),-1,"pose identifies the left fist");h.assertTrue(Math.abs(CursedCombat.stamina(p)-76)<.01,"both fists spend stamina");});
        h.runAfterDelay(56,()->{h.assertTrue(CursedCombat.stamina(p)>95,"rest regenerates stamina");h.succeed();});
    }
    @GameTest(environment="jjkcraft:expansion",padding=48)
    public void emptyStaminaRefusesPunchBeforeSpendingEnergyAndWeaponCannotUseEmptyFist(GameTestHelper h){
        var p=player(h);for(int i=0;i<8;i++){CursedCombat.clearActions(p);h.assertTrue(CursedCombat.startPunch(p,JjkCraft.id("jab"),new Vec3(0,0,1),i%2==0?1:-1),"eight affordable jabs");}
        CursedCombat.clearActions(p);double ce=CursedEnergy.current(p);h.assertFalse(CursedCombat.startPunch(p,JjkCraft.id("jab"),new Vec3(0,0,1),1),"exhaustion refuses spam");h.assertTrue(CursedEnergy.current(p)==ce,"refused punch spends no CE");
        CursedCombat.restoreStamina(p);p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_SWORD));h.assertFalse(CursedCombat.startPunch(p,JjkCraft.id("jab"),new Vec3(0,0,1),-1),"empty left hand cannot inherit right-hand sword damage");h.succeed();
    }
    @GameTest(environment="jjkcraft:expansion",padding=48)
    public void fingerItemIsConsumedOnceAndRejectedAtTheCap(GameTestHelper h){
        var p=player(h);var stack=new net.minecraft.world.item.ItemStack(com.jjkcraft.item.JjkItems.SUKUNAS_FINGER);var result=stack.finishUsingItem(p.level(),p);h.assertTrue(result.isEmpty(),"successful eating consumes one relic");h.assertValueEqual(CursedVessels.get(p).fingers(),1,"one item adds exactly one finger");
        p.setAttached(CursedVessels.STATE,new VesselState(20,0,Long.MAX_VALUE));var capped=new net.minecraft.world.item.ItemStack(com.jjkcraft.item.JjkItems.SUKUNAS_FINGER);h.assertValueEqual(capped.finishUsingItem(p.level(),p).getCount(),1,"cap rejection leaves the rare item intact");CursedVessels.end(p);h.succeed();
    }
    @GameTest(environment="jjkcraft:expansion",padding=48)
    public void fingersKeepInnateTechniqueAndVesselIsPhysicalWithATwentyFingerLimit(GameTestHelper h){
        var p=player(h);var innate=TechniqueService.get(p).innate();double damage=p.getAttributeValue(Attributes.ATTACK_DAMAGE);
        h.assertTrue(CursedVessels.consume(p),"first relic accepted");h.assertTrue(CharacterTraits.has(p,"cursed_vessel"),"physical modifier gained");h.assertTrue(p.getAttributeValue(Attributes.ATTACK_DAMAGE)>damage,"physical strength rises");h.assertTrue(CursedCombat.maximumStamina(p)==140,"vessel has more endurance");h.assertValueEqual(TechniqueService.get(p).innate(),innate,"blood manipulation remains innate");h.assertTrue(TechniqueService.get(p).learned().contains(JjkCraft.id("dismantle")),"first finger teaches Dismantle");
        p.setAttached(CursedVessels.STATE,new VesselState(19,0,Long.MAX_VALUE));h.assertTrue(CursedVessels.consume(p),"twentieth accepted");h.assertFalse(CursedVessels.consume(p),"twenty-one refused");h.assertTrue(TechniqueService.get(p).learned().contains(JjkCraft.id("shrine_furnace")),"higher finger tiers grant the furnace");CursedVessels.end(p);h.succeed();
    }
    @GameTest(environment="jjkcraft:expansion",padding=48,maxTicks=50)
    public void possessionBlocksVoluntaryCastingAndAlwaysExpires(GameTestHelper h){
        var p=player(h);CursedVessels.consume(p);CursedVessels.begin(p,2,20);h.assertTrue(CursedVessels.controlled(p),"takeover starts");
        var id=JjkCraft.id("dismantle");h.assertValueEqual(TechniqueService.check(p,id,Techniques.get(p.registryAccess(),id).orElseThrow()),CastFailure.STUNNED,"human casts blocked");
        h.runAfterDelay(24,()->{h.assertFalse(CursedVessels.controlled(p),"time limit returns control even without client packets");CursedVessels.end(p);h.assertTrue(CursedVessels.get(p).safeUntil()>p.level().getGameTime(),"release grants a recovery interval");h.succeed();});
    }
    @GameTest(environment="jjkcraft:expansion",padding=64,maxTicks=75)
    public void closedBarrierContainsVerticalEscapeAndReleasesAfterClose(GameTestHelper h){
        var p=player(h);var mob=h.spawn(EntityTypes.HUSK,new Vec3(2,1,2));mob.setNoAi(true);mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(500);mob.setHealth(500);
        h.assertValueEqual(DomainService.expand(p,JjkCraft.id("malevolent_shrine"),DomainMath.Variant.FULL,true),CastFailure.NONE,"closed domain opens");var d=DomainService.active(p).orElseThrow();
        h.runAfterDelay(33,()->{h.assertValueEqual(d.phase(),ActiveDomain.Phase.ACTIVE,"barrier active");mob.setPos(d.center().add(0,d.radius()+8,0));});
        h.runAfterDelay(36,()->{h.assertTrue(d.contains(mob),"upward teleport is clamped using all three axes");DomainService.close(p);mob.setPos(d.center().add(d.radius()+10,0,0));});
        h.runAfterDelay(40,()->{h.assertFalse(d.contains(mob),"closed domain releases targets immediately on ending");h.succeed();});
    }
    @GameTest(environment="jjkcraft:expansion",padding=64,maxTicks=55)
    public void casterCannotEndAClosedDomainByTeleportingBeyondItsAnchor(GameTestHelper h){
        var p=player(h);h.assertValueEqual(DomainService.expand(p,JjkCraft.id("malevolent_shrine"),DomainMath.Variant.FULL,true),CastFailure.NONE,"closed domain starts");var d=DomainService.active(p).orElseThrow();
        h.runAfterDelay(33,()->p.setPos(d.center().add(d.radius()*2,0,0)));h.runAfterDelay(37,()->{h.assertValueEqual(d.phase(),ActiveDomain.Phase.ACTIVE,"distance does not cancel containment first");h.assertTrue(d.contains(p),"caster is returned to the sealed domain");DomainService.close(p);h.succeed();});
    }
    @GameTest(environment="jjkcraft:expansion",padding=64,maxTicks=55)
    public void openDomainAllowsExitAndRuleDomainsRejectOpenBeforeCharging(GameTestHelper h){
        var p=player(h);double before=CursedEnergy.current(p);h.assertValueEqual(DomainService.expand(p,JjkCraft.id("unlimited_void"),DomainMath.Variant.BARRIERLESS,true),CastFailure.DISABLED,"Void must remain closed");h.assertTrue(CursedEnergy.current(p)==before,"rejection is free");h.assertFalse(DomainCustomization.allowsOpen(JjkCraft.id("deadly_sentencing")),"courtroom needs a barrier");
        var mob=h.spawn(EntityTypes.HUSK,new Vec3(2,1,2));mob.setNoAi(true);mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(500);mob.setHealth(500);h.assertValueEqual(DomainService.expand(p,JjkCraft.id("malevolent_shrine"),DomainMath.Variant.BARRIERLESS,true),CastFailure.NONE,"Shrine can open");var d=DomainService.active(p).orElseThrow();
        h.runAfterDelay(33,()->mob.setPos(d.center().add(d.radius()+10,0,0)));h.runAfterDelay(37,()->{h.assertFalse(d.contains(mob),"no confinement without a barrier");DomainService.close(p);h.succeed();});
    }
    @GameTest(environment="jjkcraft:expansion",padding=64)
    public void domainExpressionCostsApplyAndObserverPacketKeepsVisuals(GameTestHelper h){
        var p=player(h);DomainService.setState(p,DomainService.state(p).withKnown(Optional.of(JjkCraft.id("malevolent_shrine"))));
        var tune=new DomainProfile(150,125,175,150,0x3366AA,0xAA2244,false,"Crimson Court","lanterns");h.assertTrue(DomainCustomization.save(p,tune),"valid expression saves");var raw=Domains.get(p.registryAccess(),JjkCraft.id("malevolent_shrine")).orElseThrow();var applied=DomainCustomization.apply(raw,tune);h.assertTrue(applied.castCost()>raw.castCost()*2,"stronger expression costs substantially more");
        h.assertFalse(DomainCustomization.save(p,new DomainProfile(201,100,100,100,-1,-1,false,"","")),"server rejects invalid bounds");
        var view=new DomainWorldPayload.View(1,p.getId(),JjkCraft.id("malevolent_shrine"),0,1,0,18,0,1,10,0,.7f,tune);var bytes=io.netty.buffer.Unpooled.buffer();try{DomainWorldPayload.View.CODEC.encode(bytes,view);h.assertValueEqual(DomainWorldPayload.View.CODEC.decode(bytes),view,"all observers receive exact expression");h.assertFalse(bytes.isReadable(),"no packet remainder");}finally{bytes.release();}
        CursedEnergy.set(p,20000);h.assertValueEqual(DomainService.expand(p,JjkCraft.id("malevolent_shrine"),DomainMath.Variant.FULL,true),CastFailure.NONE,"tuned domain starts");var d=DomainService.active(p).orElseThrow();h.assertTrue(Math.abs(d.radius()-raw.radius()*1.5)<.01,"radius applies");h.assertTrue(Math.abs(d.power()-1.25*DomainMath.power(DomainMath.Variant.FULL,com.jjkcraft.sorcerer.Progression.get(p).growth().domain()))<.01,"output multiplies existing domain growth");h.assertFalse(DomainCustomization.save(p,DomainProfile.DEFAULT),"active snapshot cannot be edited");DomainService.close(p);h.succeed();
    }
}
