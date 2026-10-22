package com.jjkcraft.gametest;

import java.util.*;
import com.jjkcraft.JjkCraft;
import com.jjkcraft.client.JjkKeys;
import com.jjkcraft.client.design.*;
import com.jjkcraft.client.screen.*;
import com.jjkcraft.combat.CursedCombat;
import com.jjkcraft.training.*;
import com.jjkcraft.energy.*;
import com.jjkcraft.awakening.CharacterAwakening;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.GameType;

final class DojoClientChecks{
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private static void cursor(ClientGameTestContext c,int x,int y){var point=c.computeOnClient(m->new double[]{x*m.getWindow().getScreenWidth()/(double)m.gui.screen().width,y*m.getWindow().getScreenHeight()/(double)m.gui.screen().height});c.getInput().setCursorPos(point[0],point[1]);c.waitTicks(2);}
    static void run(ClientGameTestContext c){
        var cfg=com.jjkcraft.config.JjkConfig.server();boolean modulo=cfg.modulo.enabled,terrain=cfg.casting.terrainDamage;cfg.modulo.enabled=false;cfg.casting.terrainDamage=false;
        String original=c.computeOnClient(m->UiDesign.exportText()),clipboard=c.computeOnClient(m->m.keyboardHandler.getClipboard());boolean auto=c.computeOnClient(m->com.jjkcraft.client.config.ClientConfig.get().autoCombat);
        c.runOnClient(m->{com.jjkcraft.client.config.ClientConfig.get().autoCombat=false;m.getWindow().setWindowed(1280,800);});
        try(var world=c.worldBuilder().create()){
            var server=world.getServer();world.getConnection().waitForChunksRender();server.runCommand("gamemode creative @a");server.runCommand("gamerule spawn_mobs false");server.runCommand("time set noon");server.runCommand("weather clear");server.runCommand("fill -32 99 -32 32 99 32 minecraft:smooth_stone");server.runCommand("tp @a 0 100 0 0 0");
            int dummy=server.computeOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();CharacterAwakening.creative(p,JjkCraft.id("blood_manipulation"),false);p.setGameMode(GameType.SURVIVAL);SorceryPractice.set(p,SorceryPractice.get(p).withReinforcement(false,false));CursedEnergy.fill(p);var doll=com.jjkcraft.entity.JjkEntities.TRAINING_DUMMY.create(p.level(),EntitySpawnReason.COMMAND);doll.snapTo(.5,100,2.5,180,0);p.level().addFreshEntity(doll);p.level().setBlockAndUpdate(new BlockPos(3,100,1),com.jjkcraft.block.JjkBlocks.TRAINING_MAT.defaultBlockState());p.level().setBlockAndUpdate(new BlockPos(5,100,1),com.jjkcraft.block.JjkBlocks.FOCUS_STONE.defaultBlockState());p.level().setBlockAndUpdate(new BlockPos(-3,100,1),com.jjkcraft.block.JjkBlocks.BALANCE_BEAM.defaultBlockState());return doll.getId();});
            c.waitFor(m->com.jjkcraft.client.state.ClientProgress.get().awakened());c.setScreen(()->null);c.runOnClient(m->{m.gui.hud.getChat().clearMessages(false);m.player.setYRot(0);m.player.setXRot(0);});c.waitTicks(5);c.takeScreenshot("dojo-before-contact");c.getInput().pressKey(JjkKeys.LIGHT);c.waitTicks(6);
            server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();check(Training.get(p).xp("strength")>0,"fist contact failed: stamina="+CursedCombat.stamina(p)+" move="+CursedCombat.currentMove(p)+" aim="+p.getLookAngle()+" player="+p.position()+" doll="+p.level().getEntity(dummy).position()+" eligible="+Training.eligible(p));});
            c.waitFor(m->com.jjkcraft.client.training.TrainingClient.state.xp("strength")>0);c.takeScreenshot("dojo-actual-fist-contact-training");
            for(String item:List.of("jjkcraft:split_soul_katana","jjkcraft:stormbell_spear","jjkcraft:playful_cloud","minecraft:diamond_sword","minecraft:apple")){
                server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(item))));CursedCombat.clearActions(p);CursedCombat.restoreStamina(p);});c.waitTicks(12);c.takeScreenshot("dojo-weapon-rest-"+item.replace(':','-'));c.getInput().pressKey(JjkKeys.LIGHT);c.waitFor(m->com.jjkcraft.client.combat.WeaponAnimations.active(m.player.getMainHandItem()));c.waitTicks(2);c.takeScreenshot("dojo-weapon-first-person-"+item.replace(':','-'));c.waitTicks(2);c.takeScreenshot("dojo-weapon-contact-"+item.replace(':','-'));
                c.runOnClient(m->m.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK));c.waitTicks(2);c.takeScreenshot("dojo-weapon-third-person-"+item.replace(':','-'));c.runOnClient(m->m.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON));c.waitTicks(20);
            }
            check(server.computeOnServer(s->Training.get(s.getPlayerList().getPlayers().getFirst()).xp("weaponry")>0),"actual weapons train physical weapon skill on real contact");
            server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,net.minecraft.world.item.ItemStack.EMPTY);p.setGameMode(GameType.CREATIVE);});
            for(String name:UiDesign.PAGES){if(name.equals("HUD")||name.equals("ModuloChoiceScreen"))continue;final String screen=name;c.setScreen(()->DesignPages.create(screen));c.waitTicks(3);c.takeScreenshot("dojo-ui-"+name);}
            c.setScreen(()->new com.jjkcraft.client.modulo.ModuloChoiceScreen(new com.jjkcraft.modulo.ModuloPayloads.Dialogue("Kogane","A training challenge is available.",0,1,List.of("Accept","Later"))));c.waitTicks(3);c.takeScreenshot("dojo-ui-ModuloChoiceScreen");
            c.setScreen(PracticeScreen::new);c.waitTicks(3);c.getInput().pressKey(JjkKeys.DESIGN_STUDIO);c.waitFor(m->m.gui.screen() instanceof DesignStudioScreen);c.clickScreenButton("+ panel");
            var point=c.computeOnClient(m->new int[]{83+250*(m.gui.screen().width-191)/640,55+180*(m.gui.screen().height-90)/360});cursor(c,point[0],point[1]);c.getInput().holdMouse(InputConstants.MOUSE_BUTTON_LEFT);cursor(c,point[0]+30,point[1]+12);c.getInput().releaseMouse(InputConstants.MOUSE_BUTTON_LEFT);
            check(c.computeOnClient(m->UiDesign.document().pages.get("PracticeScreen").values().stream().anyMatch(p->p.kind().equals("panel")&&p.x()>240)),"native drag changes a UI part");c.takeScreenshot("dojo-designer-drag-layout");c.clickScreenButton("Copy");c.clickScreenButton("Import");c.clickScreenButton("Save");
            c.clickScreenButton("Props");c.clickScreenButton("Add block");c.runOnClient(m->{for(var child:m.gui.screen().children())if(child instanceof net.minecraft.client.gui.components.EditBox b)switch(b.getMessage().getString()){case "X","Z"->b.setValue("-2");case "Y"->b.setValue("29");case "Width","Depth"->b.setValue("4");case "Height"->b.setValue("3");}});c.clickScreenButton("Apply values");c.clickScreenButton("Material");c.takeScreenshot("dojo-designer-block-prop");c.clickScreenButton("Apply prop");
            c.waitFor(m->{try{var r=m.getResourceManager().getResource(JjkCraft.id("geckolib/models/entity/training_dummy.geo.json"));if(r.isEmpty())return false;try(var reader=r.get().openAsReader()){return com.google.gson.JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject().getAsJsonArray("bones").get(1).getAsJsonObject().getAsJsonArray("cubes").size()==8;}}catch(Exception e){return false;}});
            c.waitFor(m->m.gui.overlay()==null);c.waitTicks(10);c.setScreen(()->null);c.waitTicks(3);c.takeScreenshot("dojo-custom-doll-resource-pack-live");
            c.runOnClient(m->{String saved=UiDesign.exportText();try{UiDesign.importText("{\"version\":99}");throw new AssertionError("invalid import accepted");}catch(IllegalArgumentException expected){}check(saved.equals(UiDesign.exportText()),"rejected import cannot change the active design");});
            server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var e=p.level().getEntity(dummy);if(e!=null)e.discard();});
            c.getInput().pressKey(options->options.keyToggleGui);
            var curses=server.computeOnServer(s->s.registryAccess().lookupOrThrow(com.jjkcraft.registry.JjkRegistries.CURSE).keySet().stream().sorted().map(Identifier::getPath).toList());
            for(String curse:curses){server.runCommand("jjk curse spawn jjkcraft:"+curse+" 0 100 6");server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var e=p.level().getEntitiesOfClass(com.jjkcraft.curse.CurseEntity.class,p.getBoundingBox().inflate(100)).getFirst();e.setNoAi(true);e.getEntityData().set(com.jjkcraft.curse.CurseEntity.EMERGING,0);e.setDisguised(false);e.setYRot(0);e.setYHeadRot(0);e.setYBodyRot(0);double d=Math.max(2,e.getBbHeight()*1.7),eye=Math.max(101.62,100+e.getBbHeight()*.6),pitch=Math.toDegrees(Math.atan2(eye-100-e.getBbHeight()*.5,d));p.teleportTo(.5,eye-1.62,6-d);p.setYRot(0);p.setXRot((float)pitch);p.connection.teleport(.5,eye-1.62,6-d,0,(float)pitch);});c.waitTicks(5);c.runOnClient(m->{for(var e:m.level.getEntitiesOfClass(com.jjkcraft.curse.CurseEntity.class,m.player.getBoundingBox().inflate(100))){e.setYRot(180);e.yRotO=180;e.setYHeadRot(180);e.yHeadRotO=180;e.setYBodyRot(180);e.yBodyRotO=180;}});c.waitTicks(1);c.takeScreenshot("dojo-curse-"+curse);server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();for(var e:p.level().getEntitiesOfClass(com.jjkcraft.curse.CurseEntity.class,p.getBoundingBox().inflate(100)))e.discard();});}
            c.getInput().pressKey(options->options.keyToggleGui);
        }finally{cfg.modulo.enabled=modulo;cfg.casting.terrainDamage=terrain;c.runOnClient(m->{UiDesign.importText(original);m.keyboardHandler.setClipboard(clipboard);com.jjkcraft.client.config.ClientConfig.get().autoCombat=auto;m.getWindow().setWindowed(854,480);});}
    }
}
