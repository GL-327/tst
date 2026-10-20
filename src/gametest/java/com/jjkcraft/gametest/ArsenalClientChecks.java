package com.jjkcraft.gametest;

import com.jjkcraft.JjkCraft;
import com.jjkcraft.client.JjkKeys;
import com.jjkcraft.client.state.*;
import com.jjkcraft.client.screen.*;
import com.jjkcraft.technique.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import com.mojang.blaze3d.platform.InputConstants;

/** Uses native keys/mouse and the actual server save, not direct widget callbacks for binding. */
final class ArsenalClientChecks {
    private static void click(ClientGameTestContext c,int gx,int gy,int button){
        var p=c.computeOnClient(mc->new double[]{gx*mc.getWindow().getScreenWidth()/(double)mc.gui.screen().width,gy*mc.getWindow().getScreenHeight()/(double)mc.gui.screen().height});
        c.getInput().setCursorPos(p[0],p[1]);c.getInput().pressMouse(button);c.waitTicks(2);
    }
    static void run(ClientGameTestContext c){
        TestWorldSave save;
        String[] powers={"grudge_bolt","lapse_blue","infinity","reversal_red","granite_blast","charge_transfer","lightning_discharge","echo_brand","echo_collect","mirror_guard","mirror_release","mourning_thread"};
        try(var world=c.worldBuilder().create()){
            var server=world.getServer();world.getConnection().waitForChunksRender();server.runCommand("gamemode creative @a");server.runCommand("gamerule spawn_mobs false");server.runCommand("time set noon");
            server.computeOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();com.jjkcraft.awakening.CharacterAwakening.creative(p,JjkCraft.id("limitless"),false);
                for(var key:powers)TechniqueService.grant(p,JjkCraft.id(key));return true;});
            c.waitFor(mc->ClientTechniques.known().contains(JjkCraft.id("mirror_release"))&&ClientTechniques.state().loadout().size()==12);
            c.setScreen(LoadoutScreen::new);c.waitTicks(2);
            for(int slot=0;slot<12;slot++){
                final int n=slot;
                var pos=c.computeOnClient(mc->{var sc=mc.gui.screen();int w=Math.min(560,sc.width-16),h=Math.min(320,sc.height-16),x=(sc.width-w)/2,y=(sc.height-h)/2;return new int[]{x+w/2+5+(n%4)*32+14,y+24+18+(n/4)*29+13,x+8+60,y+24+43+12};});
                click(c,pos[0],pos[1],InputConstants.MOUSE_BUTTON_RIGHT);c.waitFor(mc->ClientTechniques.state().slot(n).isEmpty());
                c.runOnClient(mc->{for(var child:mc.gui.screen().children())if(child instanceof net.minecraft.client.gui.components.EditBox box){box.setValue(powers[n]);break;}});
                c.waitTicks(1);click(c,pos[2],pos[3],InputConstants.MOUSE_BUTTON_LEFT);click(c,pos[0],pos[1],InputConstants.MOUSE_BUTTON_LEFT);
                c.waitFor(mc->ClientTechniques.state().slot(n).filter(JjkCraft.id(powers[n])::equals).isPresent());
            }
            c.getInput().setCursorPos(15,15);c.waitTicks(1);c.takeScreenshot("jjkcraft-arsenal-twelve-native-bindings");c.clickScreenButton("Bindings");c.waitTicks(2);c.takeScreenshot("jjkcraft-arsenal-controls-auto-default");
            if(!c.computeOnClient(mc->com.jjkcraft.client.config.ClientConfig.get().autoCombat))throw new AssertionError("Auto Combat must default ON");
            c.clickScreenButton("Auto Combat: ON");if(c.computeOnClient(mc->com.jjkcraft.client.config.ClientConfig.get().autoCombat))throw new AssertionError("Auto option changes");c.clickScreenButton("Auto Combat: OFF");
            c.setScreen(()->null);server.runCommand("gamemode survival @a");
            c.getInput().pressKey(InputConstants.KEY_E);c.waitFor(mc->mc.gui.screen() instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen);c.getInput().pressKey(InputConstants.KEY_ESCAPE);c.waitFor(mc->mc.gui.screen()==null);
            server.computeOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.getInventory().setSelectedSlot(0);p.getInventory().setItem(0,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STICK,2));p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND,net.minecraft.world.item.ItemStack.EMPTY);return true;});c.waitTicks(4);
            c.getInput().pressKey(InputConstants.KEY_Q);c.waitTicks(3);c.getInput().pressKey(InputConstants.KEY_F);c.waitTicks(3);
            if(!server.computeOnServer(s->s.getPlayerList().getPlayers().getFirst().getOffhandItem().is(net.minecraft.world.item.Items.STICK)))throw new AssertionError("Q drops and F swaps normally");
            server.computeOnServer(s->{TechniqueService.selectSlot(s.getPlayerList().getPlayers().getFirst(),4);return true;});c.waitFor(mc->JjkKeys.bankStart()==4);
            c.getInput().pressKey(JjkKeys.SKILLS[0]);c.waitTicks(3);
            if(!server.computeOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();return TechniqueService.readyAt(p,JjkCraft.id("granite_blast"))>p.level().getGameTime()&&p.getInventory().getSelectedSlot()==0;}))throw new AssertionError("bank two cast: "+server.computeOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();return "ready="+TechniqueService.readyAt(p,JjkCraft.id("granite_blast"))+", now="+p.level().getGameTime()+", hotbar="+p.getInventory().getSelectedSlot()+", selected="+TechniqueService.get(p).selected()+", CE="+com.jjkcraft.energy.CursedEnergy.current(p);}));
            c.getInput().pressKey(JjkKeys.BANK);c.waitFor(mc->JjkKeys.bankStart()==8);c.takeScreenshot("jjkcraft-arsenal-bank-three-hud");
            for(int page:new int[]{8,9}){final int n=page;c.setScreen(()->new TechniqueWorkshopScreen(JjkCraft.id("grudge_bolt"),n));c.waitTicks(2);c.takeScreenshot("jjkcraft-arsenal-vows-"+page);}
            c.setScreen(()->new TechniqueWorkshopScreen(JjkCraft.id("grudge_bolt"),8));c.clickScreenButton("Forsake combat · off");c.clickScreenButton("Apply tuning");c.waitFor(mc->ClientPersonalization.get(JjkCraft.id("grudge_bolt")).pact().has(TechniquePact.COMBAT));c.waitTicks(2);c.takeScreenshot("jjkcraft-arsenal-vow-saved");
            // Remove before combat checks; no cast occurred, so the vow is not sealed yet.
            c.clickScreenButton("Forsake combat · ON");c.clickScreenButton("Apply tuning");c.waitFor(mc->ClientPersonalization.get(JjkCraft.id("grudge_bolt")).pact().flags()==0);c.setScreen(()->null);
            c.runOnClient(mc->com.jjkcraft.client.config.ClientConfig.get().autoCombat=false);
            c.getInput().pressKey(JjkKeys.LIGHT);c.waitTicks(1);c.getInput().pressKey(JjkKeys.LIGHT);
            c.waitFor(mc->{var pose=com.jjkcraft.client.combat.CombatAnimations.pose(mc.player.getId());return pose!=null&&pose.move().equals("uppercut");});
            c.waitTicks(22);c.getInput().pressKey(JjkKeys.HEAVY);c.waitTicks(1);c.getInput().pressKey(JjkKeys.HEAVY);
            c.waitFor(mc->{var pose=com.jjkcraft.client.combat.CombatAnimations.pose(mc.player.getId());return pose!=null&&pose.move().equals("sweep");});
            c.waitTicks(23);server.runCommand("tp @a ~ ~ ~ 0 0");c.waitTicks(3);server.runCommand("execute as @p at @s rotated ~ 0 run summon minecraft:husk ^ ^ ^2 {NoAI:1b,PersistenceRequired:1b}");
            c.runOnClient(mc->com.jjkcraft.client.config.ClientConfig.get().autoCombat=true);c.getInput().holdKeyFor(o->o.keyAttack,40);
            if(!server.computeOnServer(s->com.jjkcraft.sorcerer.Progression.get(s.getPlayerList().getPlayers().getFirst()).counter("combat_hits")>0))throw new AssertionError("Auto M1 failed: "+server.computeOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();return "yaw="+p.getYRot()+", pitch="+p.getXRot()+", move="+com.jjkcraft.combat.CursedCombat.currentMove(p)+", vowed="+PersonalPacts.forbids(p,TechniquePact.COMBAT)+", CE="+com.jjkcraft.energy.CursedEnergy.current(p);}));
            c.takeScreenshot("jjkcraft-arsenal-auto-contact");server.runCommand("gamemode creative @a");
            String[] curses={"paper_tick","gutter_grub","chalk_biter","static_mite","whisper_knot","dustling","damp_effigy","porcelain_nipper","rustjaw_pup","drain_skitter","needle_cricket","worry_doll"};
            server.computeOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();for(String key:curses)com.jjkcraft.sorcerer.Progression.addCounter(p,com.jjkcraft.curse.Curses.codexKey(JjkCraft.id(key)),3);return true;});c.waitTicks(4);
            for(String key:curses){c.setScreen(()->new CodexScreen(JjkCraft.id(key)));c.waitTicks(3);c.takeScreenshot("jjkcraft-arsenal-curse-"+key);}
            c.setScreen(()->new TechniqueWorkshopScreen(JjkCraft.id("grudge_bolt"),8));c.clickScreenButton("Forsake combat · off");c.clickScreenButton("Apply tuning");c.waitFor(mc->ClientPersonalization.get(JjkCraft.id("grudge_bolt")).pact().has(0));c.setScreen(()->null);save=world.getWorldSave();
        }
        try(var reopened=save.open()){reopened.getConnection().waitForChunksRender();c.waitFor(mc->ClientTechniques.state().loadout().size()==12&&ClientPersonalization.get(JjkCraft.id("grudge_bolt")).pact().has(0));}
    }
}
