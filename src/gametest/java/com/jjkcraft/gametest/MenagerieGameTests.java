package com.jjkcraft.gametest;

import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.GameType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import com.jjkcraft.JjkCraft;
import com.jjkcraft.awakening.*;
import com.jjkcraft.energy.*;
import com.jjkcraft.entity.shikigami.*;
import com.jjkcraft.sorcerer.*;
import com.jjkcraft.technique.*;

public final class MenagerieGameTests {
    @GameTest(environment="jjkcraft:expansion",padding=24) public void chaliceLootConditionsSurviveMinecraftCodec(GameTestHelper h){
        var table=h.getLevel().getServer().reloadableRegistries().getLootTable(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,JjkCraft.id("entities/ten_mouth_sovereign")));
        var ops=net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE,h.getLevel().registryAccess());
        var json=net.minecraft.world.level.storage.loot.LootTable.DIRECT_CODEC.encodeStart(ops,table).getOrThrow().getAsJsonObject();
        var pool=java.util.stream.StreamSupport.stream(json.getAsJsonArray("pools").spliterator(),false).map(com.google.gson.JsonElement::getAsJsonObject).filter(p->p.get("entries").toString().contains("divine_chalice")).findFirst().orElseThrow();
        h.assertTrue(pool.has("condition")&&pool.get("condition").toString().contains("killed_by_player")&&pool.get("condition").toString().contains("random_chance")&&pool.get("condition").toString().contains("0.01"),"rare chance and player-kill requirements are actually loaded, not ignored JSON");h.succeed();
    }
    @GameTest(environment="jjkcraft:expansion",padding=24) public void guardianInterceptionIsBoundedAndCostsItsOwnHealth(GameTestHelper h){
        var p=player(h);cast(h,p,"summon_rika");var guardian=ShikigamiEntity.ownedBy(h.getLevel(),ShadowBeastEntity.class,p).getFirst();float before=guardian.getHealth();float result=com.jjkcraft.damage.DamageModifiers.apply(h.getLevel(),p,h.getLevel().damageSources().generic(),10);h.assertTrue(result>0&&result>=7.5f&&guardian.getHealth()<before,"guardian cannot make its owner immortal");h.assertTrue(ShadowBeasts.healReady(p)&&!ShadowBeasts.healReady(p),"healing budget is shared by all owned deer and chimeras");h.succeed();
    }
    @GameTest(environment="jjkcraft:expansion",padding=24) public void adaptationNeverReachesDamageImmunity(GameTestHelper h){
        var p=player(h);cast(h,p,"ten_shadows_mahoraga");var beast=ShikigamiEntity.ownedBy(h.getLevel(),ShadowBeastEntity.class,p).getFirst();for(int n=0;n<12;n++){beast.damageCooldownTime=0;beast.hurtServer(h.getLevel(),h.getLevel().damageSources().generic(),1);}h.assertTrue(beast.adaptedResistance("minecraft:generic")<=.6,"adaptation capped");float before=beast.getHealth();beast.damageCooldownTime=0;h.assertTrue(beast.hurtServer(h.getLevel(),h.getLevel().damageSources().generic(),5)&&beast.getHealth()<before,"adapted beast still takes damage");h.succeed();
    }
    @GameTest(environment="jjkcraft:expansion",padding=24) public void chaliceRequiresDeliberateUseAndConsumesExactlyOne(GameTestHelper h){
        var p=player(h);var stack=new net.minecraft.world.item.ItemStack(com.jjkcraft.item.JjkItems.DIVINE_CHALICE);p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,stack);String old=CharacterAwakening.get(p).family();p.setShiftKeyDown(false);com.jjkcraft.item.JjkItems.DIVINE_CHALICE.use(h.getLevel(),p,net.minecraft.world.InteractionHand.MAIN_HAND);h.assertTrue(stack.getCount()==1&&CharacterAwakening.get(p).family().equals(old),"ordinary use is safe");p.setShiftKeyDown(true);com.jjkcraft.item.JjkItems.DIVINE_CHALICE.use(h.getLevel(),p,net.minecraft.world.InteractionHand.MAIN_HAND);h.assertTrue(stack.isEmpty()&&!CharacterAwakening.get(p).family().equals(old),"one chalice buys one different technique");h.succeed();
    }
    private static net.minecraft.server.level.ServerPlayer player(GameTestHelper h){var p=TestPlayers.fresh(h);p.setGameMode(GameType.CREATIVE);CharacterAwakening.creative(p,JjkCraft.id("ten_shadows"),false);var pos=h.absolutePos(new net.minecraft.core.BlockPos(2,1,2));p.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);p.setYRot(0);p.setXRot(0);return p;}
    private static CastResult cast(GameTestHelper h,net.minecraft.server.level.ServerPlayer p,String key){var id=JjkCraft.id(key);var d=Personalization.definition(p,id,Techniques.get(p.registryAccess(),id).orElseThrow());return d.behavior().cast(new CastContext(p,h.getLevel(),id,d,1,1));}
    @GameTest(environment="jjkcraft:expansion",padding=24) public void missingCreativeFamilyResolvesToRegisteredFallback(GameTestHelper h){
        var p=TestPlayers.fresh(h);for(String missing:List.of("","jjkcraft:missing_family")){p.setAttached(CharacterAwakening.RECORD,new AwakeningRecord("legacy",missing,"",false,0));var id=CreativeCharacters.familyId(p);h.assertTrue(p.registryAccess().lookupOrThrow(CharacterAwakening.FAMILIES).getValue(id)!=null,"creative read always returns a renderable family");}h.succeed();
    }
    @GameTest(environment="jjkcraft:expansion",padding=24) public void birthPotentialMigrationIsNarrowOrderedAndIdempotent(GameTestHelper h){
        var p=player(h);p.setAttached(CharacterAwakening.RECORD,new AwakeningRecord("inherit","jjkcraft:ten_shadows","Zen'in",false,0));
        p.setAttached(SorceryPractice.STATE,new PracticeState(100,80,false,false,100,100,60,1,true,0));SorceryPractice.ensureBirth(p);var low=SorceryPractice.get(p);h.assertTrue(low.birth()==350&&low.regenLevel()==4&&low.trained()==80,"weak old spawn is raised without losing training");SorceryPractice.ensureBirth(p);h.assertTrue(low.equals(SorceryPractice.get(p)),"migration happens once");
        p.setAttached(SorceryPractice.STATE,new PracticeState(3000,80,false,false,100,100,60,10,true,0));SorceryPractice.ensureBirth(p);h.assertTrue(SorceryPractice.get(p).birth()==550&&SorceryPractice.get(p).regenLevel()==6,"lucky old spawn remains modestly stronger");h.succeed();
    }
    @GameTest(environment="jjkcraft:expansion",padding=24) public void customCreativePotentialIsNotRebalancedOnJoin(GameTestHelper h){
        var p=player(h);p.setAttached(CharacterAwakening.RECORD,new AwakeningRecord("creative_custom","jjkcraft:ten_shadows","Zen'in",false,0));p.setAttached(SorceryPractice.STATE,new PracticeState(123,45,false,false,100,100,60,9,true,0));SorceryPractice.ensureBirth(p);h.assertTrue(SorceryPractice.get(p).birth()==123&&SorceryPractice.get(p).regenLevel()==9,"explicit custom values survive migration");h.succeed();
    }
    @GameTest(environment="jjkcraft:expansion",padding=24) public void allTenShadowsHaveOwnedEntityTypes(GameTestHelper h){
        var p=player(h);for(String key:List.of("great_serpent","max_elephant","rabbit_escape","round_deer","piercing_ox","tiger_funeral","mahoraga")){var d=Techniques.get(p.registryAccess(),JjkCraft.id("ten_shadows_"+key)).orElseThrow();h.assertTrue(d.behavior() instanceof com.jjkcraft.technique.behavior.SummonBehavior,"summon move exists");var b=(com.jjkcraft.technique.behavior.SummonBehavior)d.behavior();h.assertTrue(ShadowBeasts.TYPES.containsKey(b.entity()),"real owned entity type");}h.assertTrue(ShadowBeasts.TYPES.size()==18,"eighteen additional owned beasts");h.succeed();
    }
    @GameTest(environment="jjkcraft:expansion",padding=24) public void agitoConsumesItsFourRealComponents(GameTestHelper h){
        var p=player(h);for(String key:List.of("ten_shadows_nue","ten_shadows_great_serpent","ten_shadows_round_deer","ten_shadows_tiger_funeral"))cast(h,p,key);
        h.assertTrue(cast(h,p,"merged_beast_agito").landed(),"fusion succeeds");var owned=ShikigamiEntity.ownedBy(h.getLevel(),ShikigamiEntity.class,p);h.assertTrue(owned.size()==1&&owned.getFirst() instanceof ShadowBeastEntity,"four components become one beast");var agito=(ShadowBeastEntity)owned.getFirst();int expected=ShadowBeasts.NUE|ShadowBeasts.SERPENT|ShadowBeasts.DEER|ShadowBeasts.TIGER;h.assertTrue((agito.features()&expected)==expected,"inherits lightning binding healing and claws");h.succeed();
    }
    @GameTest(environment="jjkcraft:expansion",padding=24) public void failedFusionDoesNotConsumeOtherSummons(GameTestHelper h){
        var p=player(h);cast(h,p,"ten_shadows_nue");var pet=ShikigamiEntity.ownedBy(h.getLevel(),ShikigamiEntity.class,p).getFirst();h.assertFalse(cast(h,p,"merged_beast_agito").landed(),"missing ingredients refuse fusion");h.assertFalse(pet.isRemoved(),"existing component remains");h.succeed();
    }
    @GameTest(environment="jjkcraft:expansion",padding=24) public void personalFusionRecipeCombinesChosenAbilities(GameTestHelper h){
        var p=player(h);TechniqueService.grant(p,JjkCraft.id("summon_lantern_moth"));h.assertTrue(ShikigamiFusion.save(p,List.of(JjkCraft.id("nue"),JjkCraft.id("lantern_moth"))),"known component recipe saved");cast(h,p,"ten_shadows_nue");cast(h,p,"summon_lantern_moth");h.assertTrue(cast(h,p,"chimera_fusion").landed(),"custom merge succeeds");var beast=ShikigamiEntity.ownedBy(h.getLevel(),ShadowBeastEntity.class,p).getFirst();h.assertTrue((beast.features()&(ShadowBeasts.NUE|ShadowBeasts.FIRE))==(ShadowBeasts.NUE|ShadowBeasts.FIRE),"mechanics follow chosen components");h.succeed();
    }
    @GameTest(environment="jjkcraft:expansion",padding=24) public void fusionRejectsUnlearnedAndDuplicateChoices(GameTestHelper h){
        var p=player(h);h.assertFalse(ShikigamiFusion.save(p,List.of(JjkCraft.id("nue"),JjkCraft.id("nue"))),"duplicates rejected");h.assertFalse(ShikigamiFusion.save(p,List.of(JjkCraft.id("nue"),JjkCraft.id("rika"))),"unlearned component rejected");h.succeed();
    }
    @GameTest(environment="jjkcraft:expansion",padding=24) public void divineDogsCanMergeIntoTotality(GameTestHelper h){
        var p=player(h);cast(h,p,"ten_shadows_divine_dogs");h.assertTrue(cast(h,p,"divine_dog_totality").landed(),"both hounds merge");h.assertTrue(ShikigamiEntity.ownedBy(h.getLevel(),ShikigamiEntity.class,p).size()==1,"components consumed once");h.succeed();
    }
    @GameTest(environment="jjkcraft:expansion",padding=24) public void divineChaliceResetsTechniquesAndPreservesTheBody(GameTestHelper h){
        var p=player(h);var old=JjkCraft.id("ten_shadows_divine_dogs");Personalization.save(p,old,TechniqueTuning.DEFAULT.named("Old hounds"));TechniqueService.set(p,TechniqueService.get(p).withMastery(Map.of(old,500)));cast(h,p,"ten_shadows_divine_dogs");ShikigamiFusion.save(p,List.of(JjkCraft.id("nue"),JjkCraft.id("toad")));
        CharacterTraits.set(p,new TraitState(true,List.of(),"minecraft:villager",List.of(),40));p.setHealth(35);var progress=Progression.get(p);var practice=SorceryPractice.get(p);var traits=CharacterTraits.get(p);h.assertTrue(com.jjkcraft.item.DivineChaliceItem.reroll(p),"reroll succeeds");
        h.assertFalse(CharacterAwakening.get(p).family().equals("jjkcraft:ten_shadows"),"different family");h.assertFalse(TechniqueService.knows(p,old),"old family gone");h.assertTrue(TechniqueService.get(p).mastery().isEmpty()&&p.getAttachedOrCreate(Personalization.TUNINGS).isEmpty()&&p.getAttachedOrCreate(ShikigamiFusion.RECIPE).isEmpty(),"all technique mastery expressions and fusion recipes reset");
        h.assertTrue(Progression.get(p).grade().equals(progress.grade())&&Progression.get(p).growth().equals(progress.growth())&&SorceryPractice.get(p).equals(practice)&&CharacterTraits.get(p).traits().equals(traits.traits()),"grade growth birth training and body preserved");h.assertTrue(ShikigamiEntity.ownedBy(h.getLevel(),ShikigamiEntity.class,p).isEmpty(),"old summons dismissed");h.assertTrue(p.getHealth()==35&&p.getMaxHealth()==40,"current health and host body health are preserved");h.succeed();
    }
    @GameTest(environment="jjkcraft:expansion",padding=24) public void bloodAndMassBuffsCannotGrantPlayerImmunity(GameTestHelper h){
        var cookie=net.minecraft.server.network.CommonListenerCookie.createInitial(new com.mojang.authlib.GameProfile(UUID.randomUUID(),"DamageRegression"),false);var p=new net.minecraft.server.level.ServerPlayer(h.getLevel().getServer(),h.getLevel(),cookie.gameProfile(),cookie.clientInformation());var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);new io.netty.channel.embedded.EmbeddedChannel(connection);h.getLevel().getServer().getPlayerList().placeNewPlayer(connection,p,cookie);
        try{
            p.setGameMode(GameType.CREATIVE);p.setAttached(com.jjkcraft.death.SoulSystem.STATE,com.jjkcraft.death.SoulState.EMPTY.phase("choosing"));CharacterAwakening.creative(p,JjkCraft.id("blood_manipulation"),false);h.assertTrue(com.jjkcraft.death.SoulSystem.state(p).phase().equals("alive"),"character replacement clears stale death protection");p.setGameMode(GameType.SURVIVAL);p.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);p.setHealth(100);
            var blood=Techniques.get(p.registryAccess(),JjkCraft.id("flowing_red_scale")).orElseThrow();blood.behavior().cast(new CastContext(p,h.getLevel(),JjkCraft.id("flowing_red_scale"),blood,500,1));h.assertTrue(p.hurtServer(h.getLevel(),h.getLevel().damageSources().generic(),8)&&p.getHealth()<100,"blood user takes real damage");
            var mass=Techniques.get(p.registryAccess(),JjkCraft.id("star_rage")).orElseThrow();mass.behavior().cast(new CastContext(p,h.getLevel(),JjkCraft.id("star_rage"),mass,500,1));h.assertTrue(p.getEffect(net.minecraft.world.effect.MobEffects.RESISTANCE).getAmplifier()<=2,"extreme output never reaches Resistance V");p.setHealth(100);p.damageCooldownTime=0;h.assertTrue(p.hurtServer(h.getLevel(),h.getLevel().damageSources().generic(),8)&&p.getHealth()<100,"mass buff is resistant but still damageable");h.succeed();
        }finally{h.getLevel().getServer().getPlayerList().remove(p);p.discard();}
    }
}
