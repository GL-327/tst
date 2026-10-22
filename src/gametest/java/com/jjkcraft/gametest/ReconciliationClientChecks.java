package com.jjkcraft.gametest;

import java.util.*;
import com.jjkcraft.JjkCraft;
import com.jjkcraft.awakening.CharacterAwakening;
import com.jjkcraft.death.SoulSystem;
import com.jjkcraft.energy.CursedEnergy;
import com.jjkcraft.entity.shikigami.*;
import com.jjkcraft.reverse.*;
import com.jjkcraft.technique.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;

/** Covers the boundaries between the independently developed local and GitHub systems. */
final class ReconciliationClientChecks {
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    static void run(ClientGameTestContext c){
        boolean old=com.jjkcraft.config.JjkConfig.server().modulo.enabled;com.jjkcraft.config.JjkConfig.server().modulo.enabled=false;
        boolean auto=c.computeOnClient(mc->com.jjkcraft.client.config.ClientConfig.get().autoCombat);
        c.runOnClient(mc->com.jjkcraft.client.config.ClientConfig.get().autoCombat=false);
        TestWorldSave save;
        try{
            try(var world=c.worldBuilder().create()){
                var server=world.getServer();world.getConnection().waitForChunksRender();
                server.runCommand("gamemode creative @a");server.runCommand("gamerule spawn_mobs false");server.runCommand("gamerule keep_inventory true");server.runCommand("time set noon");
                server.runCommand("fill -30 99 -30 30 99 30 minecraft:smooth_stone");server.runCommand("tp @a 0 100 0 0 0");
                server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();CharacterAwakening.creative(p,JjkCraft.id("limitless"),false);});
                c.waitFor(mc->com.jjkcraft.client.state.ClientTechniques.known().contains(JjkCraft.id("maximum_output_red")));c.setScreen(()->null);server.runCommand("gamemode survival @a");
                c.waitTicks(4);
                server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();
                    check(InfinityField.on(p)&&InfinityField.preferred(p),"Limitless starts Infinity automatically");
                    ReverseTechnique.set(p,ReverseState.EMPTY,false);CursedEnergy.set(p,100);CursedEnergy.checkpoint(p);
                });
                c.waitTicks(5);
                server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();
                    check(CursedEnergy.current(p)<100,"Infinity consumes CE before RCT without recovering the upkeep");
                    ReverseTechnique.set(p,ReverseState.EMPTY.withUnlocked(true).withAutoHeal(false),false);CursedEnergy.set(p,0);
                });
                c.waitTicks(3);
                server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();
                    check(InfinityField.on(p),"learned RCT permits Infinity with an empty CE reserve");
                    check(InfinityField.upkeep(p,Techniques.get(p.registryAccess(),JjkCraft.id("infinity")).orElseThrow())==0,"learned RCT removes upkeep");
                    InfinityField.toggle(p);check(!InfinityField.on(p)&&!InfinityField.preferred(p),"manual off preference");
                    p.setAttached(SoulSystem.STATE,SoulSystem.state(p).phase("respawning"));
                    check(com.jjkcraft.damage.DamageModifiers.apply(p.level(),p,p.damageSources().generic(),3)>0,"a living stale respawn state does not grant damage immunity");
                    SoulSystem.finishPendingRespawn(p);check(SoulSystem.state(p).phase().equals("alive"),"old stuck saves recover");
                    p.hurtServer(p.level(),p.damageSources().genericKill(),100000);check(!p.isAlive(),"real first death occurs");SoulSystem.action(p,"respawn");
                });
                c.waitFor(mc->mc.player!=null&&mc.player.isAlive()&&com.jjkcraft.client.death.SoulClient.view().state().phase().equals("alive"));
                c.setScreen(()->null);c.waitTicks(90); // Vanilla's ordinary respawn grace must expire before checking damage.
                server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();
                    check(!p.isSpectator()&&!p.getAbilities().invulnerable&&!InfinityField.preferred(p),"ordinary mortality and passive preference survive respawn");
                    p.setInvulnerableTime(0);p.damageCooldownTime=0;float health=p.getHealth();p.hurtServer(p.level(),p.damageSources().generic(),3);
                    check(p.getHealth()<health,"normal damage works after the first death");
                    var legacy=JjkCraft.id("family_limitless");TechniqueService.set(p,TechniqueService.get(p).withLearned(legacy).withSlot(0,Optional.of(legacy)));
                    TechniqueFamilies.migrate(p);check(TechniqueService.get(p).slot(0).filter(TechniqueBindings.binding(JjkCraft.id("limitless"))::equals).isPresent(),"local-preview slots migrate to GitHub family bindings");
                    check(!TechniqueService.knows(p,legacy),"migration removes the obsolete menu pseudo-technique");
                });
                c.takeScreenshot("reconciled-respawn-mortality");
                server.runCommand("gamemode creative @a");server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();CharacterAwakening.creative(p,JjkCraft.id("ten_shadows"),false);TechniqueService.setLoadout(p,0,Optional.of(TechniqueBindings.binding(JjkCraft.id("ten_shadows"))));});
                c.waitFor(mc->com.jjkcraft.client.state.ClientTechniques.known().contains(JjkCraft.id("ten_shadows_nue")));c.setScreen(()->null);server.runCommand("gamemode survival @a");server.runCommand("tp @a 0 100 0 0 0");
                c.runOnClient(mc->mc.gui.hud.getChat().clearMessages(false));
                c.setScreen(()->new com.jjkcraft.client.screen.TechniqueWheelScreen(TechniqueBindings.binding(JjkCraft.id("ten_shadows")),0));c.waitTicks(4);c.takeScreenshot("reconciled-ten-shadows-wheel");c.clickScreenButton("Subjugation");
                c.waitFor(mc->mc.gui.screen() instanceof com.jjkcraft.client.screen.TechniqueAbilityScreen);c.waitTicks(3);c.takeScreenshot("reconciled-subjugation-menu");c.setScreen(()->null);
                server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var id=JjkCraft.id("ten_shadows_divine_dogs");var d=Techniques.get(p.registryAccess(),id).orElseThrow();
                    check(TechniqueService.check(p,id,d)==CastFailure.UNTAMED,"GitHub variant casts share the local taming gate");
                    String result=TenShadows.challenge(p,id);check(!TenShadows.trial(p).isEmpty(),"ritual starts: "+result);
                    var beast=p.level().getEntitiesOfClass(ShikigamiEntity.class,p.getBoundingBox().inflate(12),ShikigamiEntity::isRitual).getFirst();
                    check(beast.getOwner()==null&&new CastContext(p,p.level(),id,d,1,1).isEnemy(beast),"ritual enemies stay targetable and cannot become fusion guards");
                    beast.hurtServer(p.level(),p.damageSources().playerAttack(p),10000);check(TenShadows.canSummon(p,JjkCraft.id("divine_dog")),"personal defeat unlocks summoning");
                    check(TechniqueService.check(p,id,d)==CastFailure.NONE,"tamed move passes the same cast gate");
                    String second=TenShadows.challenge(p,JjkCraft.id("ten_shadows_nue"));check(!TenShadows.trial(p).isEmpty(),"independent second ritual: "+second);
                    var nue=p.level().getEntitiesOfClass(ShikigamiEntity.class,p.getBoundingBox().inflate(12),ShikigamiEntity::isRitual).getFirst();
                    var outsider=com.jjkcraft.entity.JjkEntities.DIVINE_DOG.create(p.level(),net.minecraft.world.entity.EntitySpawnReason.COMMAND);
                    nue.hurtServer(p.level(),p.damageSources().mobAttack(outsider),1);nue.hurtServer(p.level(),p.damageSources().playerAttack(p),10000);
                    check(!TenShadows.canSummon(p,JjkCraft.id("nue")),"outside interference cannot unlock a shikigami");
                    com.jjkcraft.sorcerer.SorcererLifecycle.fullSync(p);
                });
                c.waitFor(mc->com.jjkcraft.client.state.ClientTechniqueMenus.tamed().contains(JjkCraft.id("divine_dog")));
                c.setScreen(()->new com.jjkcraft.client.screen.TechniqueAbilityScreen(JjkCraft.id("ten_shadows")));c.waitTicks(3);c.takeScreenshot("reconciled-tamed-shikigami");c.setScreen(()->null);
                // Keep a non-pausing menu open while the server performs one controlled swing.
                // UI-close attack packets and buffered input must not add a second swing.
                c.setScreen(()->new com.jjkcraft.client.screen.TechniqueAbilityScreen(JjkCraft.id("ten_shadows")));c.waitTicks(25);
                long[] before={0,0};
                server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();
                    for(var e:p.level().getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,p.getBoundingBox().inflate(64),e->e!=p))e.discard();
                    p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(com.jjkcraft.item.CursedEquipment.BLACK_ROPE));
                    before[0]=com.jjkcraft.sorcerer.Progression.get(p).counter("combat_hits");before[1]=com.jjkcraft.sorcerer.Progression.get(p).counter("black_flashes");
                    var target=net.minecraft.world.entity.EntityTypes.HUSK.create(p.level(),net.minecraft.world.entity.EntitySpawnReason.COMMAND);
                    target.snapTo(p.getX(),p.getY(),p.getZ()+1.8,180,0);target.setNoAi(true);target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(500);target.setHealth(500);p.level().addFreshEntity(target);
                    com.jjkcraft.blackflash.BlackFlash.forceNext(p);
                    check(com.jjkcraft.combat.CursedCombat.start(p,JjkCraft.id("jab"),target.getEyePosition().subtract(p.getEyePosition())),"manual Black Flash regression swing starts");
                });
                c.waitTicks(8);server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();
                    check(com.jjkcraft.sorcerer.Progression.get(p).counter("black_flashes")==before[1]+1,"forced Black Flash lands through the newer combat system");
                    check(com.jjkcraft.sorcerer.Progression.get(p).counter("combat_hits")==before[0]+1,"Black Flash increments the ordinary hit counter once: "+before[0]+" -> "+com.jjkcraft.sorcerer.Progression.get(p).counter("combat_hits"));
                    check(p.getMainHandItem().getDamageValue()>0,"Black Flash still wears its cursed weapon");
                });c.setScreen(()->null);c.takeScreenshot("reconciled-black-flash-tool-contact");
                save=world.getWorldSave();
            }
            try(var reopened=save.open()){
                reopened.getConnection().waitForChunksRender();reopened.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();
                    check(TenShadows.tamed(p).contains(JjkCraft.id("divine_dog"))&&!TenShadows.tamed(p).contains(JjkCraft.id("nue")),"successful and failed rituals persist across reconnect");
                    check(SoulSystem.state(p).phase().equals("alive"),"no stale death protection after reconnect");
                });
            }
        }finally{com.jjkcraft.config.JjkConfig.server().modulo.enabled=old;c.runOnClient(mc->com.jjkcraft.client.config.ClientConfig.get().autoCombat=auto);}
    }
}
