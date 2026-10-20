package com.jjkcraft.gametest;
import com.jjkcraft.JjkCraft;
import com.jjkcraft.awakening.*;
import com.jjkcraft.client.state.*;
import com.jjkcraft.client.screen.*;
import com.jjkcraft.technique.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import com.mojang.blaze3d.platform.InputConstants;

/** Real inverted mouse input, editor ACK/persistence, family navigation and visual QA. */
final class AtelierClientChecks {
    static void run(ClientGameTestContext c){
        TestWorldSave save;
        try(var world=c.worldBuilder().create()){
            var server=world.getServer();world.getConnection().waitForChunksRender();server.runCommand("gamemode creative @a");server.runCommand("gamerule spawn_mobs false");server.runCommand("time set noon");
            server.computeOnServer(s->{CharacterAwakening.creative(s.getPlayerList().getPlayers().getFirst(),JjkCraft.id("idle_transfiguration"),false);return true;});
            c.waitFor(mc->ClientTechniques.known().contains(JjkCraft.id("soul_detonation")));
            if(!"input".equals(System.getProperty("jjkcraft.clientTestFocus"))){
            c.runOnClient(mc->{mc.options.guiScale().set(1);mc.resizeGui();});
            c.setScreen(FamilyTechniqueScreen::new);c.waitTicks(3);c.takeScreenshot("jjkcraft-atelier-family-library");
            c.setScreen(()->new LoadoutScreen(JjkCraft.id("transfigured_soul")));c.waitTicks(3);c.takeScreenshot("jjkcraft-atelier-loadout-details");
            c.setScreen(()->new TechniqueWorkshopScreen(JjkCraft.id("transfigured_soul"),5));c.waitTicks(2);c.takeScreenshot("jjkcraft-atelier-soul-personalization");
            c.setScreen(CreativeTechniqueScreen::new);c.waitTicks(6);c.takeScreenshot("jjkcraft-atelier-creative-family");
            c.clickScreenButton("Auto Calamity: off");c.clickScreenButton("Auto Calamity: ON");
            c.clickScreenButton("Potential");c.waitTicks(2);c.takeScreenshot("jjkcraft-atelier-creative-potential");
            // Exact values are typed into the actual edit boxes, then sent by the Apply button.
            c.runOnClient(mc->{for(var child:mc.gui.screen().children())if(child instanceof net.minecraft.client.gui.components.EditBox box){if(box.getMessage().getString().equals("Birth CE"))box.setValue("137");if(box.getMessage().getString().equals("Regeneration level"))box.setValue("2");}});
            c.clickScreenButton("Apply character");c.waitFor(mc->ClientPersonalization.practice().birth()==137&&ClientPersonalization.practice().regenLevel()==2);
            for(String page:new String[]{"Training","Attributes","Powers","Domain","Skills"}){c.clickScreenButton(page);c.waitTicks(2);c.takeScreenshot("jjkcraft-atelier-creative-"+page.toLowerCase(java.util.Locale.ROOT));}
            c.runOnClient(mc->{mc.options.guiScale().set(2);mc.resizeGui();});c.waitTicks(2);c.takeScreenshot("jjkcraft-atelier-creative-compact");
            }else{server.computeOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();com.jjkcraft.energy.SorceryPractice.set(p,com.jjkcraft.energy.SorceryPractice.get(p).withPotential(137,2));return true;});}
            c.setScreen(()->null);server.runCommand("gamemode survival @a");server.runCommand("tp @a ~ ~ ~ 0 0");c.runOnClient(mc->{mc.player.setYRot(0);mc.player.setXRot(0);});c.waitTicks(2);
            server.computeOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var dummy=net.minecraft.world.entity.EntityTypes.HUSK.create(p.level(),net.minecraft.world.entity.EntitySpawnReason.EVENT);dummy.setPos(p.getX(),p.getY(),p.getZ()+2);dummy.setNoAi(true);dummy.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(1000);dummy.setHealth(1000);p.level().addFreshEntity(dummy);return true;});c.waitTicks(4);
            c.runOnClient(mc->{mc.options.keyAttack.setKey(InputConstants.Type.MOUSE.getOrCreate(InputConstants.MOUSE_BUTTON_RIGHT));mc.options.keyUse.setKey(InputConstants.Type.MOUSE.getOrCreate(InputConstants.MOUSE_BUTTON_LEFT));net.minecraft.client.KeyMapping.resetMapping();com.jjkcraft.client.config.ClientConfig.get().autoCombat=true;});
            int before=server.computeOnServer(s->com.jjkcraft.sorcerer.Progression.get(s.getPlayerList().getPlayers().getFirst()).counter("combat_hits"));
            c.getInput().pressMouse(InputConstants.MOUSE_BUTTON_RIGHT);c.waitTicks(16);
            if(!server.computeOnServer(s->com.jjkcraft.sorcerer.Progression.get(s.getPlayerList().getPlayers().getFirst()).counter("combat_hits")>before)){c.takeScreenshot("jjkcraft-atelier-inverted-failure");throw new AssertionError("quick inverted Auto attack did not hit; client="+c.computeOnClient(mc->"enabled="+com.jjkcraft.client.combat.CombatClient.enabled()+", hit="+mc.hitResult+", yaw="+mc.player.getYRot()+", pitch="+mc.player.getXRot()+", key="+mc.options.keyAttack.getTranslatedKeyMessage().getString()+", pose="+com.jjkcraft.client.combat.CombatAnimations.pose(mc.player.getId()))+"; server="+server.computeOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();return "yaw="+p.getYRot()+", pitch="+p.getXRot()+", move="+com.jjkcraft.combat.CursedCombat.currentMove(p)+", enabled="+com.jjkcraft.combat.CursedCombat.enabled(p)+", CE="+com.jjkcraft.energy.CursedEnergy.current(p)+", targets="+p.level().getEntitiesOfClass(net.minecraft.world.entity.monster.zombie.Husk.class,p.getBoundingBox().inflate(8)).stream().map(e->e.position().toString()).toList();}));};
            int afterTap=server.computeOnServer(s->com.jjkcraft.sorcerer.Progression.get(s.getPlayerList().getPlayers().getFirst()).counter("combat_hits"));
            c.getInput().holdKeyFor(o->o.keyAttack,45);c.waitTicks(3);
            if(!server.computeOnServer(s->com.jjkcraft.sorcerer.Progression.get(s.getPlayerList().getPlayers().getFirst()).counter("combat_hits")>afterTap))throw new AssertionError("held inverted Auto attack did not hit");
            c.waitTicks(30);c.runOnClient(mc->com.jjkcraft.client.config.ClientConfig.get().autoCombat=false);c.getInput().pressMouse(InputConstants.MOUSE_BUTTON_RIGHT);
            c.waitFor(mc->{var pose=com.jjkcraft.client.combat.CombatAnimations.pose(mc.player.getId());return pose!=null&&!pose.move().isEmpty();});c.takeScreenshot("jjkcraft-atelier-inverted-manual-combat");
            c.waitTicks(35);int beforeUse=server.computeOnServer(s->com.jjkcraft.sorcerer.Progression.get(s.getPlayerList().getPlayers().getFirst()).counter("combat_hits"));c.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);c.waitTicks(12);
            if(server.computeOnServer(s->com.jjkcraft.sorcerer.Progression.get(s.getPlayerList().getPlayers().getFirst()).counter("combat_hits"))!=beforeUse)throw new AssertionError("Use mouse button triggered a combat hit");
            c.runOnClient(mc->{mc.options.keyAttack.setKey(mc.options.keyAttack.getDefaultKey());mc.options.keyUse.setKey(mc.options.keyUse.getDefaultKey());net.minecraft.client.KeyMapping.resetMapping();com.jjkcraft.client.config.ClientConfig.get().autoCombat=true;mc.options.guiScale().set(1);mc.resizeGui();});
            server.runCommand("gamemode creative @a");
            server.computeOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var d=Techniques.get(p.registryAccess(),JjkCraft.id("soul_multiplicity")).orElseThrow();d.behavior().cast(new CastContext(p,p.level(),JjkCraft.id("soul_multiplicity"),d,1,1));return true;});
            c.waitTicks(8);c.takeScreenshot("jjkcraft-atelier-soul-servants");save=world.getWorldSave();
        }
        try(var reopened=save.open()){reopened.getConnection().waitForChunksRender();c.waitFor(mc->ClientPersonalization.practice().birth()==137&&ClientPersonalization.practice().regenLevel()==2);}
    }
}
