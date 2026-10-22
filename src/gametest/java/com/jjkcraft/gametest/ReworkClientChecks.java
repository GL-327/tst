package com.jjkcraft.gametest;

import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.world.phys.Vec3;
import com.jjkcraft.JjkCraft;
import com.jjkcraft.network.payload.TechniqueFxPayload;

/** Real renderer coverage for the art bake, UV atlases and animated VFX material pass. */
final class ReworkClientChecks {
    static void run(ClientGameTestContext context){
        boolean old=com.jjkcraft.config.JjkConfig.server().modulo.enabled;
        com.jjkcraft.config.JjkConfig.server().modulo.enabled=false;
        try(var world=context.worldBuilder().create()){
            var server=world.getServer();world.getConnection().waitForChunksRender();
            server.runCommand("gamemode creative @a");server.runCommand("gamerule spawn_mobs false");
            server.runOnServer(s->com.jjkcraft.awakening.CharacterAwakening.creative(s.getPlayerList().getPlayers().getFirst(),JjkCraft.id("limitless"),false));
            context.waitFor(mc->com.jjkcraft.client.state.ClientTechniques.known().contains(JjkCraft.id("hollow_purple")));
            context.setScreen(()->null);
            server.runCommand("time set noon");server.runCommand("fill -24 99 -24 24 99 24 minecraft:smooth_stone");
            server.runCommand("tp @a 0 100 0 0 4");context.waitTicks(20);
            String[][] groups={{"hollow_hymnal","ash_oni","salt_lantern"},{"paper_tide_gravemind","thorn_matriarch","ten_mouth_sovereign"}};
            for(int group=0;group<groups.length;group++){
                int n=0;for(String id:groups[group]){
                    server.runCommand("jjk curse spawn jjkcraft:"+id+" "+(-4+n*4)+" 100 8");n++;
                }
                context.waitTicks(35);
                server.runCommand("execute as @e[type=!minecraft:player] run data merge entity @s {NoAI:1b}");
                server.runCommand("execute as @e[type=!minecraft:player] at @s run tp @s ~ ~ ~ 180 0");
                context.runOnClient(mc->mc.gui.hud.getChat().clearMessages(false));
                context.waitTicks(5);context.takeScreenshot("rework-creatures-"+group);server.runCommand("execute as @p at @s run jjk curse clear 64");
            }
            for(String id:List.of("cursed_technique_lapse_blue","reversal_red","hollow_purple","piercing_blood","dismantle","ten_shadows_divine_dogs")){
                boolean exists=context.computeOnClient(mc->com.jjkcraft.client.state.ClientTechniques.definition(JjkCraft.id(id)).isPresent());
                if(!exists)continue;
                context.runOnClient(mc->{
                    Vec3 at=mc.player.getEyePosition().add(mc.player.getLookAngle().scale(4));
                    com.jjkcraft.client.vfx.TechniqueFxClient.handle(new TechniqueFxPayload(mc.player.getId(),JjkCraft.id(id),TechniqueFxPayload.Phase.HIT,at,new Vec3(0,0,1),List.of(),1.4f));
                });
                context.waitTicks(2);context.takeScreenshot("rework-vfx-"+id);context.waitTicks(25);
            }
            context.runOnClient(mc->{
                if(mc.getResourceManager().getResource(JjkCraft.id("textures/vfx/technique_atlas.png")).isEmpty())throw new AssertionError("effect atlas missing");
                if(mc.getResourceManager().getResource(JjkCraft.id("textures/item/split_soul_katana.png")).isEmpty())throw new AssertionError("equipment artwork missing");
            });
            server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();
                var id=JjkCraft.id("hollow_purple");var d=com.jjkcraft.technique.Techniques.get(p.registryAccess(),id).orElseThrow();
                ((com.jjkcraft.technique.behavior.ProjectileBehavior)d.behavior()).cast(new com.jjkcraft.technique.CastContext(p,p.level(),id,d,1,1));
            });
            context.waitTicks(10);context.takeScreenshot("rework-hollow-purple-projectile");context.waitTicks(75);
            server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();int n=0;
                for(String id:List.of("divine_dog","nue","toad","shadow_mahoraga")){
                    var entity=net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getValue(JjkCraft.id(id)).create(p.level(),net.minecraft.world.entity.EntitySpawnReason.COMMAND);
                    if(!(entity instanceof com.jjkcraft.entity.shikigami.ShikigamiEntity pet))throw new AssertionError("missing shikigami "+id);
                    pet.bind(p,1200,1);pet.snapTo(-4+n*2.7,100,7,180,0);pet.setNoAi(true);p.level().addFreshEntity(pet);n++;
                }
            });
            context.waitTicks(5);context.runOnClient(mc->mc.gui.hud.getChat().clearMessages(false));context.takeScreenshot("rework-shikigami-materials");
        }finally{com.jjkcraft.config.JjkConfig.server().modulo.enabled=old;}
    }
}
