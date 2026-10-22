package com.jjkcraft.gametest;

import java.util.*;
import com.jjkcraft.JjkCraft;
import com.jjkcraft.client.vfx.*;
import com.jjkcraft.curse.CurseEntity;
import com.jjkcraft.technique.*;
import com.jjkcraft.network.payload.TechniqueFxPayload;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.phys.Vec3;

/** Real network casts and multi-angle runtime art inspection, under Sodium and Iris. */
final class StudioClientChecks {
    static void run(ClientGameTestContext c){
        var config=com.jjkcraft.config.JjkConfig.server();boolean modulo=config.modulo.enabled,terrain=config.casting.terrainDamage;
        config.modulo.enabled=false;config.casting.terrainDamage=false;
        c.runOnClient(mc->{mc.getWindow().setWindowed(1280,800);mc.options.fov().set(62);});
        try(var world=c.worldBuilder().create()){
            var server=world.getServer();world.getConnection().waitForChunksRender();
            server.runCommand("gamemode creative @a");server.runCommand("gamerule spawn_mobs false");server.runCommand("time set noon");server.runCommand("weather clear");
            server.runCommand("fill -32 99 -32 32 99 32 minecraft:smooth_stone");server.runCommand("tp @a 0 100 0 0 0");
            server.runOnServer(s->{var player=s.getPlayerList().getPlayers().getFirst();com.jjkcraft.awakening.CharacterAwakening.creative(player,JjkCraft.id("limitless"),false);player.getAbilities().flying=true;player.onUpdateAbilities();});
            c.waitFor(mc->com.jjkcraft.client.state.ClientTechniques.known().contains(JjkCraft.id("hollow_purple")));c.setScreen(()->null);
            c.getInput().pressKey(options->options.keyToggleGui);c.waitTicks(3);
            int caster=server.computeOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var mob=EntityTypes.HUSK.create(p.level(),net.minecraft.world.entity.EntitySpawnReason.COMMAND);mob.setNoAi(true);mob.snapTo(0,104,7,0,0);p.level().addFreshEntity(mob);return mob.getId();});
            c.waitFor(mc->mc.level.getEntity(caster)!=null);
            for(int yaw:new int[]{0,90,180,-90})for(int pitch:new int[]{-45,0,45}){
                double y=Math.toRadians(yaw),p=Math.toRadians(pitch);Vec3 aim=new Vec3(-Math.sin(y)*Math.cos(p),-Math.sin(p),Math.cos(y)*Math.cos(p));
                // Observe from the side so shells must have depth, including upward/downward casts.
                Vec3 target=new Vec3(0,105.6,7).add(aim.scale(3)),camera=target.add(new Vec3(Math.cos(y)*8,0,Math.sin(y)*8));
                camera=new Vec3(camera.x,Math.max(107.5,camera.y),camera.z);Vec3 view=target.subtract(camera);double cameraYaw=Math.toDegrees(Math.atan2(-view.x,view.z)),cameraPitch=-Math.toDegrees(Math.atan2(view.y,view.horizontalDistance()));
                server.runCommand(String.format(java.util.Locale.ROOT,"tp @a %.4f %.4f %.4f %.4f %.4f",camera.x,camera.y-1.62,camera.z,cameraYaw,cameraPitch));c.waitTicks(4);
                c.runOnClient(mc->{TechniqueFxClient.reset();WorldFx.clear();});
                server.runOnServer(s->{var mob=(net.minecraft.world.entity.LivingEntity)s.overworld().getEntity(caster);mob.setYRot(yaw);mob.setXRot(pitch);var id=JjkCraft.id("cursed_speech_dont_move");var ctx=new CastContext(mob,s.overworld(),id,Techniques.get(s.registryAccess(),id).orElseThrow(),1,1);ctx.technique().behavior().cast(ctx);});
                c.waitFor(mc->SpeechFx.lastAim(caster).filter(v->v.distanceTo(aim)<1e-6).isPresent());
                server.runOnServer(s->{var mob=s.overworld().getEntity(caster);mob.setYRot(yaw+85);mob.setXRot(0);});c.waitTicks(5);
                if(!c.computeOnClient(mc->SpeechFx.lastAim(caster).orElseThrow().distanceTo(aim)<1e-6))throw new AssertionError("turning changed the already cast speech direction");
                c.takeScreenshot("studio-speech-"+yaw+"-"+pitch);
            }
            server.runOnServer(s->s.overworld().getEntity(caster).discard());c.waitTicks(30);
            var curses=server.computeOnServer(s->s.registryAccess().lookupOrThrow(com.jjkcraft.registry.JjkRegistries.CURSE).keySet().stream().map(id->id.getPath()).sorted().toList());
            for(int page=0;page<(curses.size()+2)/3;page++){
                int offset=page*3;
                server.runCommand("tp @a 0 101 -4 0 5");
                for(int n=offset;n<Math.min(curses.size(),offset+3);n++)server.runCommand("jjk curse spawn jjkcraft:"+curses.get(n)+" "+((n-offset)*5-5)+" 100 7");
                server.runOnServer(s->{for(var e:s.overworld().getEntitiesOfClass(CurseEntity.class,new net.minecraft.world.phys.AABB(-30,90,-30,30,130,30))){e.setNoAi(true);e.getEntityData().set(CurseEntity.EMERGING,0);e.setDisguised(false);e.setYRot(180);e.setYHeadRot(180);e.setYBodyRot(180);}});
                c.waitTicks(35);c.takeScreenshot("studio-curses-front-"+page);
                server.runCommand("tp @a 10 101 -2 48 5");
                c.waitTicks(5);c.takeScreenshot("studio-curses-side-"+page);
                server.runOnServer(s->{for(var e:s.overworld().getEntitiesOfClass(CurseEntity.class,new net.minecraft.world.phys.AABB(-30,90,-30,30,130,30)))e.discard();});
            }
            server.runCommand("tp @a 0 100 0 0 0");c.waitTicks(5);
            for(int tile=0;tile<16;tile++){
                final int n=tile;
                c.runOnClient(mc->{TechniqueFxClient.reset();WorldFx.clear();float born=mc.level.getGameTime();WorldFx.addMaterial(f->{if(f.time()-born>60)return false;var b=com.jjkcraft.vfx.EffectOrientation.of(new Vec3(.6,.2,1));VolumeMesh.orb(f,new Vec3(-2,102,7),new Vec3(1,1,1),n,0x89aaff,.8f,f.time()*.02);VolumeMesh.pressure(f,new Vec3(1,102,7),b,1,.15,n,0xb9a5ed,.7f,0);VolumeMesh.shard(f,new Vec3(3,102,7),b,.5,1,n,0xb5dfff,.9f);return true;});});
                c.waitTicks(3);c.takeScreenshot("studio-volume-material-"+tile);
            }
            c.getInput().pressKey(options->options.keyToggleGui);
        }finally{config.modulo.enabled=modulo;config.casting.terrainDamage=terrain;c.runOnClient(mc->mc.getWindow().setWindowed(854,480));}
    }
}
