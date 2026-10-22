package com.jjkcraft.gametest;

import java.util.*;
import com.jjkcraft.JjkCraft;
import com.jjkcraft.awakening.CharacterAwakening;
import com.jjkcraft.client.JjkKeys;
import com.jjkcraft.client.combat.*;
import com.jjkcraft.client.domain.DomainEditor;
import com.jjkcraft.client.screen.*;
import com.jjkcraft.client.state.*;
import com.jjkcraft.client.vessel.VesselClient;
import com.jjkcraft.combat.CursedCombat;
import com.jjkcraft.domain.*;
import com.jjkcraft.energy.CursedEnergy;
import com.jjkcraft.technique.*;
import com.jjkcraft.vessel.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Real key bindings, authoritative possession packets and native domain editor rendering. */
final class VesselCombatClientChecks {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    static void run(ClientGameTestContext c){
        var cfg=com.jjkcraft.config.JjkConfig.server();boolean modulo=cfg.modulo.enabled,terrain=cfg.casting.terrainDamage;cfg.modulo.enabled=false;cfg.casting.terrainDamage=false;
        boolean auto=c.computeOnClient(mc->com.jjkcraft.client.config.ClientConfig.get().autoCombat);c.runOnClient(mc->{com.jjkcraft.client.config.ClientConfig.get().autoCombat=false;mc.getWindow().setWindowed(1280,720);});
        try(var world=c.worldBuilder().create()){
            var server=world.getServer();world.getConnection().waitForChunksRender();server.runCommand("gamemode creative @a");server.runCommand("gamerule spawn_mobs false");server.runCommand("time set noon");server.runCommand("weather clear");server.runCommand("fill -40 99 -40 40 99 40 minecraft:smooth_stone");server.runCommand("tp @a 0 100 0 0 0");
            server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();CharacterAwakening.creative(p,JjkCraft.id("blood_manipulation"),false);CursedEnergy.set(p,20000);CursedCombat.restoreStamina(p);});
            c.waitFor(mc->ClientProgress.get().awakened());c.setScreen(()->null);c.runOnClient(mc->mc.gui.hud.getChat().clearMessages(false));
            c.getInput().pressKey(JjkKeys.LIGHT);c.waitFor(mc->{var p=CombatAnimations.pose(mc.player.getId());return p!=null&&p.hand()==1;});c.waitTicks(2);c.takeScreenshot("vessel-right-punch-corner-stance");
            c.waitTicks(5);c.getInput().pressKey(JjkKeys.LEFT_PUNCH);c.waitFor(mc->{var p=CombatAnimations.pose(mc.player.getId());return p!=null&&p.hand()==-1;});c.waitTicks(2);c.takeScreenshot("vessel-left-punch");
            check(c.computeOnClient(mc->CombatClient.endurance().stamina()<100),"actual key punches spend server-synced stamina");
            c.waitTicks(12);c.runOnClient(mc->{mc.player.setMainArm(net.minecraft.world.entity.HumanoidArm.LEFT);mc.getWindow().setWindowed(1600,720);});c.getInput().pressKey(JjkKeys.LEFT_PUNCH);c.waitTicks(4);c.takeScreenshot("vessel-left-dominant-ultrawide");c.runOnClient(mc->mc.player.setMainArm(net.minecraft.world.entity.HumanoidArm.RIGHT));
            c.waitTicks(90);c.takeScreenshot("vessel-relaxed-vanilla-hands");
            server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();DomainService.setState(p,DomainService.state(p).withKnown(Optional.of(JjkCraft.id("malevolent_shrine"))));});
            c.waitFor(mc->ClientDomains.self().known().filter(JjkCraft.id("malevolent_shrine")::equals).isPresent());
            c.setScreen(()->new DomainWorkshopScreen(null));c.waitTicks(8);
            c.runOnClient(mc->{for(var child:mc.gui.screen().children())if(child instanceof net.minecraft.client.gui.components.EditBox box){switch(box.getMessage().getString()){case "Radius %"->box.setValue("125");case "Primary RGB"->box.setValue("9A3355");case "Domain name"->box.setValue("Crimson Sanctuary");}}mc.getWindow().setWindowed(854,480);});c.waitTicks(3);
            check(c.computeOnClient(mc->mc.gui.screen().children().stream().anyMatch(e->e instanceof net.minecraft.client.gui.components.EditBox b&&b.getMessage().getString().equals("Radius %")&&b.getValue().equals("125"))),"unsaved field edits survive window resizing");c.runOnClient(mc->mc.getWindow().setWindowed(1600,720));c.waitTicks(3);
            c.takeScreenshot("vessel-domain-workshop-ultrawide");c.clickScreenButton("Closed domain");c.clickScreenButton("Save domain");c.waitFor(mc->DomainEditor.profile.open());check(server.computeOnServer(s->{var p=DomainCustomization.get(s.getPlayerList().getPlayers().getFirst());return p.open()&&p.size()==125&&p.color()==0x9A3355&&p.title().equals("Crimson Sanctuary");}),"edited workshop values save on the real server");
            c.runOnClient(mc->mc.getWindow().setWindowed(854,480));c.waitTicks(3);c.takeScreenshot("vessel-domain-workshop-small");c.setScreen(()->null);
            server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();DomainService.setState(p,DomainService.state(p).withKnown(Optional.of(JjkCraft.id("unlimited_void"))));});c.waitFor(mc->ClientDomains.self().known().filter(JjkCraft.id("unlimited_void")::equals).isPresent());c.setScreen(()->new DomainWorkshopScreen(null));c.waitTicks(6);c.takeScreenshot("vessel-void-closed-policy");c.setScreen(()->null);
            server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);p.setAttached(DomainCustomization.PROFILE,DomainProfile.DEFAULT);CursedEnergy.fill(p);check(DomainService.expand(p,JjkCraft.id("malevolent_shrine"),com.jjkcraft.core.domain.DomainMath.Variant.FULL,true)==CastFailure.NONE,"closed player domain opens");});
            c.waitFor(mc->ClientDomains.own().filter(d->d.phase()==ActiveDomain.Phase.ACTIVE).isPresent());
            server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var d=DomainService.active(p).orElseThrow();p.teleportTo(d.center().x+d.radius()+8,d.center().y,d.center().z);});c.waitTicks(7);
            check(server.computeOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();return DomainService.active(p).orElseThrow().contains(p);}),"real survival player cannot teleport through closed wall");c.takeScreenshot("vessel-closed-wall-confines-player");
            server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var d=DomainService.active(p).orElseThrow();DomainService.close(p);p.teleportTo(d.center().x+d.radius()+8,d.center().y,d.center().z);});c.waitTicks(24);
            check(server.computeOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();return DomainService.active(p).isEmpty();}),"closed barrier and visual retire cleanly");
            server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();CursedEnergy.fill(p);check(DomainService.expand(p,JjkCraft.id("malevolent_shrine"),com.jjkcraft.core.domain.DomainMath.Variant.BARRIERLESS,true)==CastFailure.NONE,"open player domain opens");});c.waitFor(mc->ClientDomains.own().filter(d->d.phase()==ActiveDomain.Phase.ACTIVE).isPresent());
            server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var d=DomainService.active(p).orElseThrow();p.teleportTo(d.center().x+d.radius()+8,d.center().y,d.center().z);});c.waitTicks(7);
            check(server.computeOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();return !DomainService.active(p).orElseThrow().contains(p);}),"real survival player can leave open domain");c.takeScreenshot("vessel-open-domain-allows-player-exit");server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();DomainService.close(p);p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);});c.waitTicks(24);server.runCommand("tp @a 0 100 0 0 0");
            int[] friendly={-1};
            server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();CursedCombat.clearActions(p);var cow=EntityTypes.COW.create(p.level(),EntitySpawnReason.COMMAND);cow.setNoAi(true);cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(500);cow.setHealth(500);cow.snapTo(p.getX(),p.getY(),p.getZ()+3,180,0);p.level().addFreshEntity(cow);friendly[0]=cow.getId();CursedVessels.consume(p);CursedVessels.begin(p,1,100);});
            c.waitFor(mc->VesselClient.hallucinated(friendly[0]));c.runOnClient(mc->mc.gui.hud.getChat().clearMessages(false));c.waitTicks(5);c.takeScreenshot("vessel-false-curse-hallucination");
            server.runOnServer(s->CursedVessels.begin(s.getPlayerList().getPlayers().getFirst(),2,100));c.waitFor(mc->VesselClient.controlled());
            c.getInput().holdKey(options->options.keyUp);c.getInput().pressKey(JjkKeys.LIGHT);c.waitTicks(20);c.takeScreenshot("vessel-takeover-blackout");c.getInput().releaseKey(options->options.keyUp);
            check(server.computeOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var cow=(LivingEntity)p.level().getEntity(friendly[0]);return cow!=null&&cow.getHealth()<500;}),"server-owned character actually attacks an innocent during blackout");
            c.waitFor(mc->!VesselClient.controlled());c.waitTicks(5);c.takeScreenshot("vessel-recovered-control");check(server.computeOnServer(s->!CursedVessels.controlled(s.getPlayerList().getPlayers().getFirst())),"server releases control too");
        }finally{cfg.modulo.enabled=modulo;cfg.casting.terrainDamage=terrain;c.runOnClient(mc->{com.jjkcraft.client.config.ClientConfig.get().autoCombat=auto;mc.getWindow().setWindowed(854,480);});}
    }
}
