package com.jjkcraft.gametest;

import java.util.*;
import com.jjkcraft.JjkCraft;
import com.jjkcraft.awakening.CharacterAwakening;
import com.jjkcraft.combat.CursedCombat;
import com.jjkcraft.energy.CursedEnergy;
import com.jjkcraft.item.CursedEquipment;
import com.jjkcraft.network.payload.CombatPayloads;
import com.jjkcraft.technique.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public final class SpectralGameTests {
    private static ServerPlayer player(GameTestHelper h){var p=TestPlayers.fresh(h);p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);CharacterAwakening.creative(p,JjkCraft.id("limitless"),false);p.setPos(Vec3.atBottomCenterOf(h.absolutePos(new net.minecraft.core.BlockPos(2,1,2))));p.setYRot(0);p.setXRot(0);CursedEnergy.fill(p);return p;}
    private static net.minecraft.world.entity.monster.zombie.Zombie target(GameTestHelper h){var t=h.spawn(EntityTypes.HUSK,new Vec3(2.5,1,4.8));t.setNoAi(true);t.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100000);t.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);t.setHealth(100000);return t;}
    @GameTest(environment="jjkcraft:expansion",padding=24,maxTicks=30) public void assistedAccuracyCountsOneContactPerSwingEvenWithMultipleTargets(GameTestHelper h){
        var p=player(h);var a=target(h);var b=target(h);b.setPos(a.position().add(.2,0,0));CursedCombat.autoAttack(p,0);
        h.runAfterDelay(15,()->{h.assertTrue(a.getHealth()<100000&&b.getHealth()<100000,"both targets receive the real strike");h.assertTrue(CursedCombat.autoStats(p)[1]==1,"a multi-target swing counts as one landed attack");h.succeed();});
    }
    @GameTest(environment="jjkcraft:expansion",padding=24,maxTicks=360) public void assistedCombatCommitsAndLandsSixOfTenEligibleAttacks(GameTestHelper h){
        var p=player(h);var t=target(h);var pos=t.position();
        for(int n=0;n<10;n++)h.runAfterDelay(n*32+1,()->{t.setPos(pos);t.damageCooldownTime=0;h.assertTrue(CursedCombat.autoAttack(p,0),"assisted opportunity starts after recovery");});
        h.runAfterDelay(330,()->{var s=CursedCombat.autoStats(p);h.assertTrue(s[0]==10&&s[1]==6&&s[2]==4,"stationary in-range benchmark: attempts="+s[0]+" hits="+s[1]+" feints="+s[2]);h.succeed();});
    }
    @GameTest(environment="jjkcraft:expansion",padding=24,maxTicks=360) public void manualAttacksHaveNoArtificialAccuracyCap(GameTestHelper h){
        var p=player(h);var t=target(h);var pos=t.position();var hits=new java.util.concurrent.atomic.AtomicInteger();var before=new float[1];
        for(int n=0;n<10;n++){h.runAfterDelay(n*32+1,()->{t.setPos(pos);t.damageCooldownTime=0;before[0]=t.getHealth();h.assertTrue(CursedCombat.start(p,JjkCraft.id("jab"),p.getLookAngle()),"manual attack starts");});h.runAfterDelay(n*32+15,()->{if(t.getHealth()<before[0])hits.incrementAndGet();});}
        h.runAfterDelay(330,()->{h.assertTrue(hits.get()==10,"all accurately timed eligible manual attacks land: "+hits);h.assertTrue(CursedCombat.autoStats(p)[0]==0,"manual attacks never inherit auto feints");h.succeed();});
    }
    @GameTest(environment="jjkcraft:expansion",padding=24) public void assistedDefenceCannotPerfectlyParryButManualCan(GameTestHelper h){
        var p=player(h);var t=target(h);CursedCombat.input(p,new CombatPayloads.Input(7,1));
        float auto=com.jjkcraft.damage.DamageModifiers.apply(h.getLevel(),p,h.getLevel().damageSources().mobAttack(t),10);h.assertTrue(auto>0,"assisted guard takes chip and cannot perfectly parry");
        CursedCombat.input(p,new CombatPayloads.Input(2,-1));CursedCombat.input(p,new CombatPayloads.Input(2,1));float manual=com.jjkcraft.damage.DamageModifiers.apply(h.getLevel(),p,h.getLevel().damageSources().mobAttack(t),10);h.assertTrue(manual==0,"a properly timed manual guard parries");h.succeed();
    }
    @GameTest(environment="jjkcraft:expansion",padding=24) public void armourProvidesBoundedProtectionPaidWithRealEnergy(GameTestHelper h){
        var p=player(h);var slots=List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET);var parts=List.of("helmet","chestplate","leggings","boots");for(int i=0;i<4;i++)p.setItemSlot(slots.get(i),new ItemStack(CursedEquipment.ITEMS.get("binding_vestments_"+parts.get(i))));
        double before=CursedEnergy.current(p);float protectedDamage=com.jjkcraft.damage.DamageModifiers.apply(h.getLevel(),p,h.getLevel().damageSources().generic(),10);h.assertTrue(protectedDamage==8&&CursedEnergy.current(p)<before,"ward caps at 20% and spends CE");CursedEnergy.set(p,0);h.assertTrue(com.jjkcraft.damage.DamageModifiers.apply(h.getLevel(),p,h.getLevel().damageSources().generic(),10)==10,"empty CE grants no ward");h.succeed();
    }
    @GameTest(environment="jjkcraft:expansion",padding=24) public void uniformDiscountAndAllNewWeaponsResolveThroughNormalSystems(GameTestHelper h){
        var p=player(h);var id=JjkCraft.id("lapse_blue");var d=Techniques.get(p.registryAccess(),id).orElseThrow();double before=TechniqueService.modifiers(p,id,d).cost();p.setItemSlot(EquipmentSlot.CHEST,new ItemStack(CursedEquipment.ITEMS.get("sorcerer_uniform_chestplate")));h.assertTrue(TechniqueService.modifiers(p,id,d).cost()<before,"uniform discounts techniques through the shared cost pipeline");
        for(String key:List.of("playful_cloud","dragon_bone","stormbell_spear","ember_wakizashi","sealbreaker_tanto"))h.assertTrue(com.jjkcraft.item.tool.CursedTools.get(p.registryAccess(),CursedEquipment.ITEMS.get(key)).isPresent(),key+" has real tool data");h.assertTrue(CursedEquipment.ITEMS.size()==26,"expanded wearable pieces, cursed tools and equipment");h.succeed();
    }
    @GameTest(environment="jjkcraft:expansion",padding=24) public void dispellingToolCancelsChargingAndPendingAbilities(GameTestHelper h){
        var p=player(h);var t=player(h);TechniqueService.setLoadout(t,0,Optional.of(JjkCraft.id("lapse_blue")));TechniqueService.onCastKey(t,0,true);h.assertTrue(TechniqueService.chargePercent(t)>0,"target is charging");var tool=CursedEquipment.ITEMS.get("sealbreaker_tanto");var stack=new ItemStack(tool);tool.postHurtEnemy(stack,t,p);h.assertTrue(TechniqueService.chargePercent(t)==0,"dispel cancels held charging");h.succeed();
    }
}
