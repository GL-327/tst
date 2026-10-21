package com.jjkcraft.gametest;
import java.util.*;
import com.jjkcraft.JjkCraft;
import com.jjkcraft.awakening.CharacterAwakening;
import com.jjkcraft.client.JjkKeys;
import com.jjkcraft.client.combat.CombatAnimations;
import com.jjkcraft.client.screen.ProjectionPlannerScreen;
import com.jjkcraft.client.state.ClientTechniques;
import com.jjkcraft.client.vfx.*;
import com.jjkcraft.technique.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

final class RebornClientChecks {
    private static void click(ClientGameTestContext c,int gx,int gy){var at=c.computeOnClient(mc->new double[]{gx*mc.getWindow().getScreenWidth()/(double)mc.gui.screen().width,gy*mc.getWindow().getScreenHeight()/(double)mc.gui.screen().height});c.getInput().setCursorPos(at[0],at[1]);c.getInput().pressMouse(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT);c.waitTicks(3);}
    static void run(ClientGameTestContext c,TestServerContext server){
        c.waitTicks(90);c.waitFor(mc->{var pose=CombatAnimations.pose(mc.player.getId());return pose!=null&&pose.move().isEmpty()&&pose.guard()<0;});c.takeScreenshot("jjkcraft-reborn-idle-vanilla-hand");
        String[][] items={{"playful_cloud","dragon_bone","stormbell_spear","ember_wakizashi","sealbreaker_tanto","divine_chalice","cursed_energy_shard","technique_scroll","withered_digit"},{"sorcerer_uniform_helmet","sorcerer_uniform_chestplate","sorcerer_uniform_leggings","sorcerer_uniform_boots","binding_vestments_helmet","binding_vestments_chestplate","binding_vestments_leggings","binding_vestments_boots","heavenly_hunter_chestplate"}};
        for(int batch=0;batch<items.length;batch++){var keys=items[batch];server.computeOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();for(int i=0;i<keys.length;i++){var item=BuiltInRegistries.ITEM.getValue(JjkCraft.id(keys[i]));if(item==net.minecraft.world.item.Items.AIR)throw new AssertionError("missing gallery item "+keys[i]);p.getInventory().setItem(i,new ItemStack(item));}return true;});c.waitTicks(6);c.takeScreenshot("jjkcraft-reborn-inventory-art-"+batch);}
        server.computeOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,ItemStack.EMPTY);return true;});
        for(String family:OriginalArts.FAMILIES){c.runOnClient(mc->{TechniqueFxClient.reset();WorldFx.clear();TechniqueFxClient.handle(new com.jjkcraft.network.payload.TechniqueFxPayload(mc.player.getId(),JjkCraft.id(family+"_guard"),com.jjkcraft.network.payload.TechniqueFxPayload.Phase.FIELD,mc.player.position().add(0,1,5),new Vec3(2,50,0),List.of(),1));});c.waitTicks(5);c.takeScreenshot("jjkcraft-reborn-material-"+family);}
        c.runOnClient(mc->{TechniqueFxClient.reset();WorldFx.clear();});
        server.computeOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();CharacterAwakening.creative(p,JjkCraft.id("projection"),false);TechniqueService.setLoadout(p,0,Optional.of(JjkCraft.id("projection_sorcery")));TechniqueService.selectSlot(p,0);p.setYRot(0);p.setXRot(0);return true;});c.waitFor(mc->ClientTechniques.state().slot(0).filter(JjkCraft.id("projection_sorcery")::equals).isPresent());c.waitTicks(4);c.setScreen(ProjectionPlannerScreen::new);c.waitTicks(6);
        c.clickScreenButton("Preset: terrain-aware");
        var point=c.computeOnClient(mc->{var screen=(ProjectionPlannerScreen)mc.gui.screen();return screen.pointAtOffset(0,4);});click(c,point[0],point[1]);c.clickScreenButton("1 · Jab");c.clickScreenButton("Save route");
        c.waitFor(mc->mc.gui.screen().children().stream().anyMatch(w->w instanceof net.minecraft.client.gui.components.Button b&&b.getMessage().getString().equals("Save route")&&b.active));
        if(!server.computeOnServer(s->{var plan=s.getPlayerList().getPlayers().getFirst().getAttachedOrCreate(ProjectionPlans.PLAN);return !plan.automatic()&&plan.steps().size()==1&&plan.steps().getFirst().action()==2;}))throw new AssertionError("route UI did not save destination and Hook");c.takeScreenshot("jjkcraft-reborn-projection-route-editor");c.setScreen(()->null);
        Vec3 from=server.computeOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var t=net.minecraft.world.entity.EntityTypes.HUSK.create(p.level(),net.minecraft.world.entity.EntitySpawnReason.EVENT);t.setPos(p.position().add(0,0,5.8));t.setNoAi(true);t.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(1000);t.setHealth(1000);p.level().addFreshEntity(t);return p.position();});
        c.runOnClient(mc->{mc.player.setYRot(0);mc.player.setXRot(0);});c.waitTicks(2);c.getInput().pressKey(JjkKeys.CAST);
        c.waitFor(mc->mc.player.position().distanceTo(from)>3);c.takeScreenshot("jjkcraft-reborn-projection-executing");c.waitTicks(50);
        if(!server.computeOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();return p.level().getEntitiesOfClass(net.minecraft.world.entity.monster.zombie.Husk.class,p.getBoundingBox().inflate(8)).stream().anyMatch(t->t.getHealth()<1000);}))throw new AssertionError("saved Projection hook must cause real physical damage");
    }
}
