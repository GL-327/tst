package com.jjkcraft.gametest;

import java.util.*;
import com.jjkcraft.JjkCraft;
import com.jjkcraft.awakening.*;
import com.jjkcraft.client.state.*;
import com.jjkcraft.client.screen.*;
import com.jjkcraft.entity.shikigami.*;
import com.jjkcraft.technique.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;

final class MenagerieClientChecks {
    private static String button(ClientGameTestContext c,String text){return c.computeOnClient(mc->mc.gui.screen().children().stream().filter(e->e instanceof net.minecraft.client.gui.components.Button b&&b.getMessage().getString().contains(text)).map(e->((net.minecraft.client.gui.components.Button)e).getMessage().getString()).findFirst().orElseThrow());}
    static void run(ClientGameTestContext c){
        TestWorldSave save;
        try(var world=c.worldBuilder().create()){
            var server=world.getServer();world.getConnection().waitForChunksRender();server.runCommand("gamemode creative @a");server.runCommand("gamerule spawn_mobs false");server.runCommand("time set noon");
            server.computeOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();com.jjkcraft.sorcerer.Awakening.reset(p);p.setAttached(CharacterAwakening.RECORD,new AwakeningRecord("legacy","jjkcraft:removed_family","",false,0));com.jjkcraft.sorcerer.SorcererLifecycle.fullSync(p);return true;});c.waitTicks(3);
            c.runOnClient(mc->{mc.options.guiScale().set(1);mc.resizeGui();});c.setScreen(CreativeTechniqueScreen::new);c.waitTicks(10);c.takeScreenshot("jjkcraft-menagerie-missing-family-safe");
            c.runOnClient(mc->{for(var child:mc.gui.screen().children())if(child instanceof net.minecraft.client.gui.components.EditBox b&&b.getMessage().getString().equals("Search"))b.setValue("ten_shadows");});c.waitTicks(3);c.clickScreenButton("Ten Shadows");
            if(server.computeOnServer(s->com.jjkcraft.sorcerer.Progression.get(s.getPlayerList().getPlayers().getFirst()).awakened()))throw new AssertionError("browsing a family must remain a draft");
            c.clickScreenButton("Calamity preset");c.clickScreenButton("Apply character");c.waitFor(mc->ClientTechniques.known().contains(JjkCraft.id("merged_beast_agito"))&&ClientTechniques.known().contains(JjkCraft.id("chimera_fusion")));c.waitTicks(3);c.takeScreenshot("jjkcraft-menagerie-creative-family-applied");
            c.setScreen(FamilyTechniqueScreen::new);c.waitTicks(3);c.takeScreenshot("jjkcraft-menagerie-ten-shadows-library");
            c.setScreen(ShikigamiFusionScreen::new);c.waitTicks(5);c.clickScreenButton(button(c,"Nue"));c.clickScreenButton(button(c,"Toad"));c.clickScreenButton("Save fusion recipe");c.waitTicks(5);c.takeScreenshot("jjkcraft-menagerie-fusion-workshop");
            if(!server.computeOnServer(s->s.getPlayerList().getPlayers().getFirst().getAttachedOrCreate(ShikigamiFusion.RECIPE).size()==2))throw new AssertionError("native fusion choices must reach server");
            c.setScreen(()->null);
            server.computeOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.setYRot(0);p.setXRot(0);for(String key:List.of("ten_shadows_nue","ten_shadows_toad")){var id=JjkCraft.id(key);var d=Techniques.get(p.registryAccess(),id).orElseThrow();d.behavior().cast(new CastContext(p,p.level(),id,d,1,1));}if(TechniqueService.cast(p,JjkCraft.id("chimera_fusion"),0)!=CastFailure.NONE)throw new AssertionError("actual chimera cast failed");return true;});c.waitTicks(8);
            if(!server.computeOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();return ShikigamiEntity.ownedBy(p.level(),ShikigamiEntity.class,p).size()==1;}))throw new AssertionError("components must become one live chimera");
            String[] gallery={"great_serpent","max_elephant","rabbit_escape","round_deer","piercing_ox","tiger_funeral","shadow_mahoraga","agito","custom_chimera","moon_dregs","rika","paper_heron","lantern_moth","iron_tortoise","grave_hound","garuda","divine_dog_totality","well_unknown_abyss"};
            for(String key:gallery){
                server.computeOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();for(var old:ShikigamiEntity.ownedBy(p.level(),ShikigamiEntity.class,p))old.discard();var beast=ShadowBeasts.TYPES.get(JjkCraft.id(key)).create(p.level(),net.minecraft.world.entity.EntitySpawnReason.EVENT);beast.snapTo(p.getX(),p.getY(),p.getZ()+5,0,0);beast.bind(p,600,1);beast.setNoAi(true);p.level().addFreshEntity(beast);return true;});
                c.runOnClient(mc->{mc.player.setYRot(0);mc.player.setXRot(0);});c.waitTicks(12);c.takeScreenshot("jjkcraft-menagerie-beast-"+key);
            }
            c.setScreen(ShikigamiFusionScreen::new);c.waitTicks(5);save=world.getWorldSave();
        }
        try(var reopen=save.open()){
            reopen.getConnection().waitForChunksRender();c.waitFor(mc->ClientTechniques.known().contains(JjkCraft.id("chimera_fusion")));
            if(!reopen.getServer().computeOnServer(s->s.getPlayerList().getPlayers().getFirst().getAttachedOrCreate(ShikigamiFusion.RECIPE).size()==2))throw new AssertionError("fusion recipe must survive world reopen");
            c.setScreen(CreativeTechniqueScreen::new);c.waitTicks(5);c.takeScreenshot("jjkcraft-menagerie-reopened-creative");
        }
    }
}
