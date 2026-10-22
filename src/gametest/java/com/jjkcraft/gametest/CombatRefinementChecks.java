package com.jjkcraft.gametest;

import java.util.*;
import com.jjkcraft.JjkCraft;
import com.jjkcraft.awakening.CharacterAwakening;
import com.jjkcraft.client.JjkKeys;
import com.jjkcraft.client.state.*;
import com.jjkcraft.client.combat.PunchImbuementClient;
import com.jjkcraft.client.screen.TechniqueWheelScreen;
import com.jjkcraft.combat.*;
import com.jjkcraft.energy.CursedEnergy;
import com.jjkcraft.technique.*;
import com.jjkcraft.technique.field.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ai.attributes.Attributes;

final class CombatRefinementChecks {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    static void run(ClientGameTestContext c){
        var config=com.jjkcraft.config.JjkConfig.server();boolean modulo=config.modulo.enabled,terrain=config.casting.terrainDamage;
        config.modulo.enabled=false;config.casting.terrainDamage=false;
        boolean auto=c.computeOnClient(mc->com.jjkcraft.client.config.ClientConfig.get().autoCombat);c.runOnClient(mc->com.jjkcraft.client.config.ClientConfig.get().autoCombat=false);
        try(var world=c.worldBuilder().create()){
            var server=world.getServer();world.getConnection().waitForChunksRender();server.runCommand("gamemode creative @a");server.runCommand("gamerule spawn_mobs false");server.runCommand("time set noon");
            server.runCommand("fill -24 99 -24 24 99 24 minecraft:smooth_stone");server.runCommand("tp @a 0 100 0 0 0");
            server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();CharacterAwakening.creative(p,JjkCraft.id("limitless"),false);CursedEnergy.set(p,20000);TechniqueService.setLoadout(p,0,Optional.of(TechniqueBindings.binding(JjkCraft.id("limitless"))));});
            c.waitFor(mc->ClientTechniques.known().contains(JjkCraft.id("hollow_purple")));c.setScreen(()->null);c.runOnClient(mc->mc.gui.hud.getChat().clearMessages(false));
            c.getInput().pressKey(JjkKeys.LIGHT);c.waitTicks(4);c.takeScreenshot("refinement-hands-jab");c.waitTicks(18);
            c.runOnClient(mc->mc.player.setXRot(58));c.waitTicks(3);c.takeScreenshot("refinement-hands-looking-down");c.runOnClient(mc->mc.player.setXRot(0));
            c.runOnClient(mc->mc.player.setMainArm(net.minecraft.world.entity.HumanoidArm.LEFT));c.getInput().pressKey(JjkKeys.LIGHT);c.waitTicks(4);c.takeScreenshot("refinement-left-handed-jab");c.runOnClient(mc->mc.player.setMainArm(net.minecraft.world.entity.HumanoidArm.RIGHT));
            c.setScreen(()->new TechniqueWheelScreen(TechniqueBindings.binding(JjkCraft.id("limitless")),0));c.waitTicks(3);c.takeScreenshot("refinement-wheel-and-domain-only");c.clickScreenButton("CE fists: OFF");c.waitFor(mc->PunchImbuementClient.ce());c.setScreen(()->null);
            server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();check(TechniqueService.cast(p,JjkCraft.id("blue_punch"),0)==CastFailure.NONE,"Blue coating activates without a target");});
            c.waitFor(mc->PunchImbuementClient.mode()==1);c.takeScreenshot("refinement-independent-ce-and-blue-fists");
            int[] contact={-1};
            server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var e=net.minecraft.world.entity.EntityTypes.HUSK.create(p.level(),EntitySpawnReason.COMMAND);e.setNoAi(true);e.getAttribute(Attributes.MAX_HEALTH).setBaseValue(500);e.setHealth(500);e.snapTo(p.getX(),p.getY(),p.getZ()+2,180,0);p.level().addFreshEntity(e);contact[0]=e.getId();CursedCombat.input(p,new com.jjkcraft.network.payload.CombatPayloads.Input(0,0));});
            c.waitTicks(16);
            server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var e=(net.minecraft.world.entity.LivingEntity)p.level().getEntity(contact[0]);check(e!=null&&e.getHealth()<460,"real combat contact applies the prepared coatings");CursedCombat.input(p,new com.jjkcraft.network.payload.CombatPayloads.Input(4,0));e.setHealth(500);e.damageCooldownTime=0;e.hurtServer(p.level(),p.level().damageSources().playerAttack(p),1);check(e.getHealth()<460,"vanilla unarmed damage also applies the coatings");CursedCombat.input(p,new com.jjkcraft.network.payload.CombatPayloads.Input(4,0));e.discard();});
            server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var enemy=net.minecraft.world.entity.EntityTypes.HUSK.create(p.level(),EntitySpawnReason.COMMAND);enemy.setNoAi(true);enemy.getAttribute(Attributes.MAX_HEALTH).setBaseValue(500);enemy.getAttribute(Attributes.ARMOR).setBaseValue(0);enemy.setHealth(500);enemy.snapTo(p.getX(),p.getY(),p.getZ()+2,180,0);p.level().addFreshEntity(enemy);
                double before=CursedEnergy.current(p);PunchImbuement.contact(p,enemy);check(enemy.getHealth()<460,"CE and Blue deal substantial independent bonus damage");check(CursedEnergy.current(p)<before,"a landed coating hit pays CE");check(enemy.getDeltaMovement().z<0,"Blue pulls toward its user");
                TechniqueService.cast(p,JjkCraft.id("red_punch"),0);enemy.setHealth(500);enemy.damageCooldownTime=0;PunchImbuement.contact(p,enemy);check(enemy.getDeltaMovement().z>0,"Red pushes away from its user");check(PunchImbuement.ce(p)&&PunchImbuement.mode(p)==2,"CE and Limitless toggles coexist");
                PunchImbuement.clear(p);enemy.discard();TechniqueService.resetCooldowns(p);TechniqueService.setLoadout(p,0,Optional.of(JjkCraft.id("lapse_blue")));CursedEnergy.set(p,20000);
            });
            c.waitFor(mc->ClientTechniques.state().slot(0).filter(JjkCraft.id("lapse_blue")::equals).isPresent());c.getInput().holdKey(JjkKeys.CAST);c.waitTicks(25);
            c.getInput().holdKey(net.minecraft.client.KeyMapping.get("key.forward"));c.waitTicks(10);c.getInput().releaseKey(net.minecraft.client.KeyMapping.get("key.forward"));
            check(c.computeOnClient(mc->com.jjkcraft.client.vfx.ChargeFx.count()==1),"moving charge has one persistent effect");c.takeScreenshot("refinement-charge-follows-moving-user");
            check(c.computeOnClient(mc->com.jjkcraft.client.vfx.WorldFx.lastFrameQuads()<600),"charging stays within a compact geometry budget");
            c.getInput().releaseKey(JjkKeys.CAST);c.waitTicks(2);check(server.computeOnServer(s->SpatialShots.active()>0),"Blue fires outward before becoming a field");c.takeScreenshot("refinement-blue-outward-release");c.waitTicks(17);c.takeScreenshot("refinement-blue-arrival-field");
            server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();TechniqueService.cancel(p);TechniqueService.resetCooldowns(p);TechniqueService.setLoadout(p,0,Optional.of(JjkCraft.id("maximum_output_red")));CursedEnergy.set(p,20000);});
            c.waitFor(mc->ClientTechniques.state().slot(0).filter(JjkCraft.id("maximum_output_red")::equals).isPresent());c.getInput().holdKey(JjkKeys.CAST);c.waitTicks(60);check(c.computeOnClient(mc->ClientTechniques.charge().percent()<80),"maximum output charges more slowly than ordinary Blue");c.waitTicks(63);c.takeScreenshot("refinement-maximum-red-full-charge");c.getInput().releaseKey(JjkKeys.CAST);c.waitTicks(8);c.takeScreenshot("refinement-maximum-red-travel");c.waitTicks(27);check(server.computeOnServer(s->SpatialFields.active(s.getPlayerList().getPlayers().getFirst().level()).stream().anyMatch(f->f.technique().equals(JjkCraft.id("maximum_output_red")))),"Maximum Red actually arrives before its field screenshot");c.takeScreenshot("refinement-maximum-red-scale");
            server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();TechniqueService.cancel(p);TechniqueService.resetCooldowns(p);TechniqueService.setLoadout(p,0,Optional.of(JjkCraft.id("hollow_purple")));CursedEnergy.set(p,20000);});
            c.waitFor(mc->ClientTechniques.state().slot(0).filter(JjkCraft.id("hollow_purple")::equals).isPresent());c.getInput().holdKey(JjkKeys.CAST);c.waitTicks(122);c.takeScreenshot("refinement-purple-100-percent");c.waitTicks(200);check(c.computeOnClient(mc->ClientTechniques.charge().percent()==200),"Purple reaches its slower two-hundred-percent cap");c.takeScreenshot("refinement-purple-200-percent");c.getInput().releaseKey(JjkKeys.CAST);c.waitTicks(3);
            int bolt=server.computeOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var shots=p.level().getEntitiesOfClass(com.jjkcraft.entity.CursedBoltEntity.class,p.getBoundingBox().inflate(16));check(!shots.isEmpty()&&shots.getFirst().size()>=4,"charged Purple launches a large real projectile");return shots.getFirst().getId();});
            c.waitFor(mc->mc.level.getEntity(bolt) instanceof com.jjkcraft.entity.CursedBoltEntity shot&&shot.size()>=4);c.waitTicks(5);c.takeScreenshot("refinement-purple-outward-release");
            server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();for(var previousShot:p.level().getEntitiesOfClass(com.jjkcraft.entity.CursedBoltEntity.class,p.getBoundingBox().inflate(100),b->b.getOwner()==p))previousShot.discard();CharacterAwakening.creative(p,JjkCraft.id("shrine"),false);var enemy=net.minecraft.world.entity.EntityTypes.HUSK.create(p.level(),EntitySpawnReason.COMMAND);enemy.setNoAi(true);enemy.getAttribute(Attributes.MAX_HEALTH).setBaseValue(500);enemy.getAttribute(Attributes.ARMOR).setBaseValue(0);enemy.setHealth(500);enemy.snapTo(p.getX(),p.getY(),p.getZ()+3,180,0);p.level().addFreshEntity(enemy);p.setYRot(0);p.setXRot(0);check(TechniqueService.cast(p,JjkCraft.id("cleave"),0)==CastFailure.NONE,"Cleave casts");check(enemy.getHealth()<400,"adaptive Cleave is substantially stronger against a powerful target");enemy.discard();});
            c.waitTicks(3);c.takeScreenshot("refinement-shrine-cleave");
        } finally {config.modulo.enabled=modulo;config.casting.terrainDamage=terrain;c.runOnClient(mc->com.jjkcraft.client.config.ClientConfig.get().autoCombat=auto);}
    }
}
