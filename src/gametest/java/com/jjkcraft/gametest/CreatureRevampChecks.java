package com.jjkcraft.gametest;

import java.util.*;
import com.jjkcraft.JjkCraft;
import com.jjkcraft.awakening.CharacterAwakening;
import com.jjkcraft.curse.CurseEntity;
import com.jjkcraft.entity.shikigami.ShikigamiEntity;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntitySpawnReason;

/** Every runtime curse and summon is actually submitted through GeckoLib's renderer. */
final class CreatureRevampChecks {
    static final List<String> SHIKI=List.of("divine_dog","nue","toad","great_serpent","max_elephant","rabbit_escape","round_deer","piercing_ox","tiger_funeral","shadow_mahoraga","divine_dog_totality","well_unknown_abyss","agito","custom_chimera","moon_dregs","garuda","rika","paper_heron","lantern_moth","iron_tortoise","grave_hound","transfigured_soul");
    static void run(ClientGameTestContext c){
        boolean old=com.jjkcraft.config.JjkConfig.server().modulo.enabled;com.jjkcraft.config.JjkConfig.server().modulo.enabled=false;
        try(var world=c.worldBuilder().create()){
            var server=world.getServer();world.getConnection().waitForChunksRender();
            server.runCommand("gamemode creative @a");server.runCommand("gamerule spawn_mobs false");server.runCommand("time set noon");server.runCommand("weather clear");
            server.runCommand("fill -30 99 -30 30 99 30 minecraft:smooth_stone");server.runCommand("tp @a 0 100 0 0 6");
            server.runOnServer(s->CharacterAwakening.creative(s.getPlayerList().getPlayers().getFirst(),JjkCraft.id("ten_shadows"),false));
            c.waitFor(mc->com.jjkcraft.client.state.ClientTechniques.known().contains(JjkCraft.id("ten_shadows_nue")));c.setScreen(()->null);
            var curses=server.computeOnServer(s->s.registryAccess().lookupOrThrow(com.jjkcraft.registry.JjkRegistries.CURSE).keySet().stream().map(id->id.getPath()).sorted().toList());
            for(int page=0;page<(curses.size()+3)/4;page++){
                int offset=page*4;
                for(int n=offset;n<Math.min(curses.size(),offset+4);n++)server.runCommand("jjk curse spawn jjkcraft:"+curses.get(n)+" "+((n-offset)*4-6)+" 100 7");
                c.waitTicks(32);server.runCommand("execute as @e[type=!minecraft:player] run data merge entity @s {NoAI:1b}");server.runCommand("execute as @e[type=!minecraft:player] at @s run tp @s ~ ~ ~ 180 0");
                int expected=Math.min(4,curses.size()-offset);server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();if(p.level().getEntitiesOfClass(CurseEntity.class,p.getBoundingBox().inflate(64)).size()!=expected)throw new AssertionError("gallery did not spawn every requested curse");});
                c.runOnClient(mc->mc.gui.hud.getChat().clearMessages(false));c.waitTicks(3);c.takeScreenshot("revamp-all-curses-"+page);
                server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();for(var e:p.level().getEntitiesOfClass(CurseEntity.class,p.getBoundingBox().inflate(64)))e.discard();});
            }
            for(String key:SHIKI){
                server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();
                    for(var oldPet:ShikigamiEntity.ownedBy(p.level(),ShikigamiEntity.class,p))oldPet.discard();
                    var entity=BuiltInRegistries.ENTITY_TYPE.getValue(JjkCraft.id(key)).create(p.level(),EntitySpawnReason.COMMAND);
                    if(!(entity instanceof ShikigamiEntity pet))throw new AssertionError("unregistered creature "+key);
                    pet.bind(p,1200,1);pet.snapTo(0,100,6,155,0);pet.setNoAi(true);p.level().addFreshEntity(pet);
                });
                c.runOnClient(mc->mc.gui.hud.getChat().clearMessages(false));c.waitTicks(8);c.takeScreenshot("revamp-shikigami-"+key);
            }
            server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();for(var pet:ShikigamiEntity.ownedBy(p.level(),ShikigamiEntity.class,p))pet.discard();});
            for(String key:List.of("divine_dog","nue","toad","shadow_mahoraga"))server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();
                var pet=(ShikigamiEntity)BuiltInRegistries.ENTITY_TYPE.getValue(JjkCraft.id(key)).create(p.level(),EntitySpawnReason.COMMAND);
                pet.bind(p,1200,1);pet.snapTo((List.of("divine_dog","nue","toad","shadow_mahoraga").indexOf(key)-1.5)*3,100,7,180,0);pet.setNoAi(true);p.level().addFreshEntity(pet);
            });
            c.waitTicks(10);c.takeScreenshot("revamp-ten-shadows-lineup");
        }finally{com.jjkcraft.config.JjkConfig.server().modulo.enabled=old;}
    }
}
