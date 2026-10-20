package com.jjkcraft.gametest;

import java.util.*;
import com.jjkcraft.JjkCraft;
import com.jjkcraft.awakening.*;
import com.jjkcraft.energy.CursedEnergy;
import com.jjkcraft.modulo.*;
import com.jjkcraft.modulo.mission.Missions;
import com.jjkcraft.sorcerer.*;
import com.jjkcraft.technique.*;
import com.jjkcraft.technique.behavior.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.GameType;
import net.minecraft.world.entity.ai.attributes.Attributes;

public final class SandboxGameTests {
    private static net.minecraft.server.level.ServerPlayer player(GameTestHelper h){var p=TestPlayers.fresh(h);p.setGameMode(GameType.SURVIVAL);Awakening.awaken(p,false);Progression.setGrade(p,JjkCraft.id("special_grade"));CursedEnergy.fill(p);return p;}
    @GameTest(environment="jjkcraft:expansion") public void everyClearedSlotCanBeRefilledAndChannelRemovalStopsIt(GameTestHelper h){
        var p=player(h);var id=JjkCraft.id("lacquer_ward");TechniqueService.grant(p,id);
        for(int slot=0;slot<TechniqueState.SLOTS;slot++){
            TechniqueService.setLoadout(p,slot,Optional.empty());TechniqueService.selectSlot(p,slot);TechniqueService.setLoadout(p,slot,Optional.of(id));
            h.assertTrue(TechniqueService.get(p).slot(slot).filter(id::equals).isPresent(),"cleared slot "+slot+" accepts rebind");
        }
        TechniqueService.cast(p,id,11);h.assertTrue(TechniqueService.active(p).isPresent(),"channel started");
        TechniqueService.setLoadout(p,11,Optional.empty());h.assertTrue(TechniqueService.active(p).isEmpty(),"unbinding ends its channel");h.succeed();
    }
    @GameTest(environment="jjkcraft:expansion") public void moduloIsSandboxAtEveryGradeEvenWithOldStoryData(GameTestHelper h){
        var p=player(h);var before=p.position();boolean enabled=com.jjkcraft.config.JjkConfig.server().modulo.enabled;com.jjkcraft.config.JjkConfig.server().modulo.enabled=true;
        try{
            Modulo.setProgress(p,ModuloProgress.EMPTY.starting(net.minecraft.resources.Identifier.fromNamespaceAndPath("modulo","chaos_and_harmony")));
            for(String grade:List.of("grade_4","grade_2","calamity")){
                Progression.setGrade(p,JjkCraft.id(grade));Missions.update(h.getLevel().getServer(),List.of(p));
                h.assertFalse(NaunaxDescent.eligible(p)||NaunaxDescent.active(h.getLevel().getServer())||ModuloTravel.board(p),"no story entry at "+grade);
                h.assertTrue(Missions.current(p).isEmpty()&&Modulo.view(p).objective().isEmpty(),"old saves cannot revive missions");
                h.assertTrue(p.position().equals(before),"promotion cannot teleport player");
            }
        }finally{com.jjkcraft.config.JjkConfig.server().modulo.enabled=enabled;}h.succeed();
    }
    @GameTest(environment="jjkcraft:expansion") public void moduloFamiliesProvidePlayableStartersWithoutQuests(GameTestHelper h){
        var p=player(h);p.setGameMode(GameType.CREATIVE);
        for(String family:List.of("modulo_harmony_chaos","modulo_light_darkness")){
            h.assertTrue(CharacterAwakening.creative(p,JjkCraft.id(family),false),"creative sandbox family available");
            var f=CharacterAwakening.family(p).orElseThrow();h.assertTrue(f.techniques().size()>=4,"four authored family moves");
            var starter=Techniques.get(p.registryAccess(),f.techniques().getFirst()).orElseThrow();
            h.assertTrue(com.jjkcraft.grade.Grades.rank(p.registryAccess(),starter.grade())==0&&starter.cast().mulCost()==0,"starter uses ordinary CE at Grade 4");
            h.assertTrue(Modulo.progress(p).mission().isEmpty(),"grant never starts story");
        }h.succeed();
    }
    @GameTest(environment="jjkcraft:expansion") public void allExpressionAxesRoundTripAndDefaultsLoadOldSaves(GameTestHelper h){
        var t=TechniqueTuning.DEFAULT;for(int axis=16;axis<=31;axis++)t=t.with(axis,axis*3);t=t.named("A personal expression");
        var encoded=TechniqueTuning.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,t).getOrThrow();
        h.assertTrue(TechniqueTuning.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,encoded).getOrThrow().equals(t),"all new fields persist");
        h.assertTrue(TechniqueTuning.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,new com.google.gson.JsonObject()).getOrThrow().equals(TechniqueTuning.DEFAULT),"old profile defaults preserved");h.succeed();
    }
    @GameTest(environment="jjkcraft:expansion") public void expressionsAreIndependentAndChangeProjectileMechanics(GameTestHelper h){
        var a=player(h);var b=player(h);var id=JjkCraft.id("grudge_bolt");TechniqueService.grant(a,id);TechniqueService.grant(b,id);
        var t=TechniqueTuning.DEFAULT.with(7,4).with(16,30).with(18,50).with(19,200).with(20,175).with(21,1).with(30,5).with(15,0x2266DD);
        Personalization.save(a,id,t);var raw=Techniques.get(a.registryAccess(),id).orElseThrow();var def=Personalization.definition(a,id,raw);var shot=(ProjectileBehavior)def.behavior();
        h.assertTrue(shot.count()==4&&Math.abs(shot.gravity()-0.05)<1e-6&&shot.size()>((ProjectileBehavior)raw.behavior()).size(),"count, gravity and size affect authored behavior");
        h.assertTrue(def.visuals().trail()==TechniqueDefinition.Trail.RING,"custom trail applied");
        h.assertTrue(Personalization.get(b,id).equals(TechniqueTuning.DEFAULT)&&Techniques.get(a.registryAccess(),id).orElseThrow().equals(raw),"another player and shared registry remain independent");
        var ctx=new CastContext(a,h.getLevel(),id,def,1,1);h.assertTrue(ctx.look().distanceTo(a.getLookAngle())>0.1,"aim offset applied on server");h.succeed();
    }
    @GameTest(environment="jjkcraft:expansion",padding=16) public void summonExpressionChangesCountHealthAndMovement(GameTestHelper h){
        var p=player(h);var id=JjkCraft.id("ten_shadows_divine_dogs");TechniqueService.grant(p,id);
        var raw=Techniques.get(p.registryAccess(),id).orElseThrow();var t=TechniqueTuning.DEFAULT.with(23,3).with(24,200).with(25,150);Personalization.save(p,id,t);
        var def=t.apply(raw);var ctx=new CastContext(p,h.getLevel(),id,def,1,1);def.behavior().cast(ctx);
        var pets=com.jjkcraft.entity.shikigami.ShikigamiEntity.ownedBy(h.getLevel(),com.jjkcraft.entity.shikigami.ShikigamiEntity.class,p);
        h.assertTrue(pets.size()==3,"three summons created");h.assertTrue(pets.getFirst().getMaxHealth()>20&&pets.getFirst().getAttributeValue(Attributes.MOVEMENT_SPEED)>0.3,"vitality and movement controls apply");
        pets.forEach(e->e.discard());h.succeed();
    }
    @GameTest(environment="jjkcraft:expansion") public void sacrificeIsPaidAndCannotKillTheCasterOrSpendOnRefusal(GameTestHelper h){
        var p=player(h);var id=JjkCraft.id("grudge_bolt");TechniqueService.grant(p,id);Personalization.save(p,id,TechniqueTuning.DEFAULT.with(28,4));p.setHealth(5);
        double before=CursedEnergy.current(p);h.assertTrue(TechniqueService.cast(p,id,0)==CastFailure.NOT_ENOUGH_HEALTH,"unsafe pact refused");h.assertTrue(CursedEnergy.current(p)==before&&p.getHealth()==5,"refused cast pays nothing");
        p.setHealth(p.getMaxHealth());float hp=p.getHealth();h.assertTrue(TechniqueService.cast(p,id,0)==CastFailure.NONE&&p.getHealth()==hp-4,"successful cast pays exact health");h.succeed();
    }
    @GameTest(environment="jjkcraft:expansion") public void sandboxScrollRecipesAreLoaded(GameTestHelper h){
        for(String key:List.of("harmony_step","chaos_twist","harmony_field","chaos_fracture","light_ray","darkness_field","twin_radiance","darkness_nova")){
            var id=JjkCraft.id("scroll_"+key);h.assertTrue(h.getLevel().getServer().getRecipeManager().byKey(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE,id)).isPresent(),"recipe loaded "+key);
        }h.succeed();
    }
}
