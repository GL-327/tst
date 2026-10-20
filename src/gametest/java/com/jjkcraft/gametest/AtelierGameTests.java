package com.jjkcraft.gametest;
import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.GameType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.phys.Vec3;
import com.jjkcraft.JjkCraft;
import com.jjkcraft.awakening.*;
import com.jjkcraft.energy.*;
import com.jjkcraft.sorcerer.*;
import com.jjkcraft.technique.*;
import com.jjkcraft.entity.shikigami.*;

public final class AtelierGameTests {
    @GameTest(environment="jjkcraft:expansion",padding=16,maxTicks=100) public void soulServantFightsItsCreatorsTarget(GameTestHelper h){
        var p=player(h);var target=h.spawn(EntityTypes.HUSK,new Vec3(1,1,3));target.setNoAi(true);p.setPos(target.getX(),target.getY(),target.getZ()-2);p.setYRot(0);p.setLastHurtMob(target);float health=target.getHealth();cast(h,p,"transfigured_soul");
        h.startSequence().thenWaitUntil(()->h.assertTrue(target.getHealth()<health,"servant follows its owner's combat target and lands real damage")).thenSucceed();
    }
    @GameTest(environment="jjkcraft:expansion",padding=16,maxTicks=100) public void furnaceArrowIgnitesAndKeepsItsFireWhenTuned(GameTestHelper h){
        var p=player(h);var target=h.spawn(EntityTypes.HUSK,new Vec3(1,1,3));target.setNoAi(true);p.setPos(target.getX(),target.getY(),target.getZ()-2);p.setYRot(0);p.setXRot(0);
        var id=JjkCraft.id("shrine_furnace");var raw=Techniques.get(p.registryAccess(),id).orElseThrow();var tuned=TechniqueTuning.DEFAULT.with(3,200).apply(raw);
        h.assertTrue(((com.jjkcraft.technique.behavior.ProjectileBehavior)tuned.behavior()).burnSeconds()==8,"tuning preserves and scales the fire payload");cast(h,p,"shrine_furnace");
        h.startSequence().thenWaitUntil(()->h.assertTrue(target.isOnFire(),"furnace applies actual fire on contact")).thenSucceed();
    }
    private static net.minecraft.server.level.ServerPlayer player(GameTestHelper h){var p=TestPlayers.fresh(h);p.setGameMode(GameType.CREATIVE);CharacterAwakening.creative(p,JjkCraft.id("idle_transfiguration"),false);return p;}
    private static CastResult cast(GameTestHelper h,net.minecraft.server.level.ServerPlayer p,String key){var id=JjkCraft.id(key);var raw=Techniques.get(p.registryAccess(),id).orElseThrow();var d=Personalization.definition(p,id,raw);return d.behavior().cast(new CastContext(p,h.getLevel(),id,d,1,1));}
    @GameTest(environment="jjkcraft:expansion",padding=16) public void allFamiliesContainRealMoveSets(GameTestHelper h){
        var registry=h.getLevel().registryAccess().lookupOrThrow(CharacterAwakening.FAMILIES);
        for(var id:registry.keySet()){var f=registry.getValue(id);h.assertTrue(f.techniques().size()>=4,"at least four moves in "+id);h.assertTrue(new HashSet<>(f.techniques()).size()==f.techniques().size(),"unique moves in family");for(var move:f.techniques())h.assertTrue(Techniques.get(h.getLevel().registryAccess(),move).isPresent(),"registered move");}
        h.assertTrue(Techniques.all(h.getLevel().registryAccess()).size()>=147,"expanded technique library");h.succeed();
    }
    @GameTest(environment="jjkcraft:expansion",padding=16) public void shapedSoulsAreOwnedAdditiveAndCapped(GameTestHelper h){
        var p=player(h);h.assertTrue(cast(h,p,"transfigured_soul").landed(),"first servant created");cast(h,p,"soul_multiplicity");cast(h,p,"transfigured_soul");cast(h,p,"transfigured_soul");
        var souls=ShikigamiEntity.ownedBy(h.getLevel(),TransfiguredSoulEntity.class,p);h.assertTrue(souls.size()==6,"six additive owned souls");h.assertFalse(cast(h,p,"transfigured_soul").landed(),"seventh servant refused");
        for(var soul:souls)h.assertTrue(soul.getOwner()==p&&soul.lifetime()==600,"bound owner and lifetime");
        h.assertTrue(cast(h,p,"soul_recall").landed(),"recall succeeds");h.assertTrue(cast(h,p,"soul_detonation").hits().isEmpty(),"no enemies means no friendly damage");h.assertTrue(ShikigamiEntity.ownedBy(h.getLevel(),TransfiguredSoulEntity.class,p).isEmpty(),"detonation consumes servants");h.succeed();
    }
    @GameTest(environment="jjkcraft:expansion",padding=16) public void soulSummonsHonorIndividualTuning(GameTestHelper h){
        var p=player(h);Personalization.save(p,JjkCraft.id("transfigured_soul"),TechniqueTuning.DEFAULT.with(23,2).with(24,200).with(25,150).with(3,150));cast(h,p,"transfigured_soul");var souls=ShikigamiEntity.ownedBy(h.getLevel(),TransfiguredSoulEntity.class,p);
        h.assertTrue(souls.size()==2&&souls.getFirst().getMaxHealth()==28&&souls.getFirst().lifetime()==900,"count vitality and lifetime are real mechanics");h.succeed();
    }
    @GameTest(environment="jjkcraft:expansion",padding=16) public void soulContactDamagesAndBindsTarget(GameTestHelper h){
        var p=player(h);var e=h.spawn(EntityTypes.HUSK,new Vec3(1,1,3));e.setNoAi(true);p.setPos(e.getX(),e.getY(),e.getZ()-2);p.setYRot(0);p.setXRot(0);float before=e.getHealth();
        h.assertTrue(cast(h,p,"soul_touch").landed()&&e.getHealth()<before&&e.hasEffect(com.jjkcraft.effect.JjkEffects.BOUND)&&e.hasEffect(com.jjkcraft.effect.JjkEffects.SHRUNK),"real soul damage and distortion");h.succeed();
    }
    @GameTest(environment="jjkcraft:expansion",padding=16) public void customCreativeDoesNotRemaxOrGrantUnwantedAttributes(GameTestHelper h){
        var p=player(h);var s=CreativeSettings.DEFAULT;var move=JjkCraft.id("lapse_blue");
        var custom=new CreativeSettings(s.grade(),17,new SorcererProgress.Growth(1,2,3,4),new CreativeSettings.Energy(123,45,2,41,true,false,125,75,50,false,false),List.of(),"",20,false,List.of(move),Map.of(move,7),List.of(move.toString()),"",false);
        h.assertTrue(CreativeCharacters.apply(p,JjkCraft.id("limitless"),custom),"custom creative applied");CharacterAwakening.unlock(p);CharacterTraits.ensure(p);
        var actual=CreativeCharacters.snapshot(p);h.assertTrue(actual.grade().equals(s.grade())&&actual.growth().total()==10&&!CharacterTraits.has(p,"six_eyes")&&actual.learned().equals(List.of(move))&&actual.mastery().get(move)==7,"no implicit Calamity, extra moves or Six Eyes");
        Progression.setGrade(p,JjkCraft.id("calamity"));com.jjkcraft.domain.DomainService.grantIfEligible(p);h.assertTrue(com.jjkcraft.domain.DomainService.state(p).known().isEmpty(),"custom No domain is not automatically regranted");
        h.assertTrue(actual.energy().birth()==123&&actual.energy().regen()==2&&actual.energy().stored()==41&&!actual.energy().rct()&&actual.domain().isEmpty(),"exact potential and skill choices");h.succeed();
    }
    @GameTest(environment="jjkcraft:expansion",padding=16) public void creativeRejectsSpoofedAndInvalidDraftsAtomically(GameTestHelper h){
        var p=player(h);var before=CreativeCharacters.snapshot(p);p.setGameMode(GameType.SURVIVAL);h.assertFalse(CreativeCharacters.apply(p,JjkCraft.id("limitless"),before),"survival denied");p.setGameMode(GameType.CREATIVE);
        var bad=new CreativeSettings(before.grade(),before.xp(),new SorcererProgress.Growth(99,0,0,0),before.energy(),before.traits(),before.form(),before.bodyHealth(),false,before.learned(),before.mastery(),before.slots(),before.domain(),before.special());
        h.assertFalse(CreativeCharacters.apply(p,JjkCraft.id("limitless"),bad),"bad growth denied");h.assertTrue(CreativeCharacters.snapshot(p).equals(before),"rejected draft makes no partial changes");h.succeed();
    }
    @GameTest(environment="jjkcraft:expansion",padding=16) public void creativeMechamaruAcceptsAnExactUncappedBank(GameTestHelper h){
        var p=player(h);var s=CreativeSettings.DEFAULT;var e=new CreativeSettings.Energy(0,0,3,2000000,false,false,100,100,60,false,false);
        var custom=new CreativeSettings(s.grade(),0,s.growth(),e,List.of(JjkCraft.id("mechamaru_restriction")),"",20,false,List.of(),Map.of(),List.of(),"",false);
        h.assertTrue(CreativeCharacters.apply(p,JjkCraft.id("idle_transfiguration"),custom),"explicit creative bank accepted");h.assertTrue(CursedEnergy.uncapped(p)&&CursedEnergy.current(p)==2000000&&SorceryPractice.get(p).birth()==0,"uncapped with exact zero birth and bank");h.succeed();
    }
    @GameTest(environment="jjkcraft:expansion",padding=16) public void familyHealingAndMovementCannotBypassVows(GameTestHelper h){
        var p=player(h);TechniqueService.grant(p,JjkCraft.id("harmony_restore"));TechniqueService.grant(p,JjkCraft.id("shadow_escape"));
        Personalization.save(p,JjkCraft.id("idle_transfiguration"),TechniqueTuning.DEFAULT.withPact(new TechniquePact((1<<TechniquePact.HEALING)|(1<<TechniquePact.DASH))));
        h.assertTrue(PersonalPacts.check(p,JjkCraft.id("harmony_restore"),Techniques.get(p.registryAccess(),JjkCraft.id("harmony_restore")).orElseThrow())==CastFailure.VOW,"healing vow enforced");
        h.assertTrue(PersonalPacts.check(p,JjkCraft.id("shadow_escape"),Techniques.get(p.registryAccess(),JjkCraft.id("shadow_escape")).orElseThrow())==CastFailure.VOW,"movement vow enforced");h.succeed();
    }
}
