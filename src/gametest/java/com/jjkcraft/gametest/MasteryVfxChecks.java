package com.jjkcraft.gametest;

import java.util.*;
import com.jjkcraft.JjkCraft;
import com.jjkcraft.awakening.CharacterAwakening;
import com.jjkcraft.client.JjkKeys;
import com.jjkcraft.client.state.*;
import com.jjkcraft.client.vfx.*;
import com.jjkcraft.combat.*;
import com.jjkcraft.energy.CursedEnergy;
import com.jjkcraft.technique.*;
import com.jjkcraft.technique.behavior.InfinityBehavior;
import com.jjkcraft.network.payload.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.Vec3;

/** Native wide-aspect visual review plus real registry, motion, projectile and mastery checks. */
final class MasteryVfxChecks {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    static void run(ClientGameTestContext c){
        var config=com.jjkcraft.config.JjkConfig.server();boolean modulo=config.modulo.enabled,terrain=config.casting.terrainDamage;
        boolean auto=c.computeOnClient(mc->com.jjkcraft.client.config.ClientConfig.get().autoCombat);
        config.modulo.enabled=false;config.casting.terrainDamage=false;
        c.runOnClient(mc->{com.jjkcraft.client.config.ClientConfig.get().autoCombat=false;mc.getWindow().setWindowed(1600,440);});
        try(var world=c.worldBuilder().create()){
            var server=world.getServer();world.getConnection().waitForChunksRender();
            server.runCommand("gamemode creative @a");server.runCommand("gamerule spawn_mobs false");server.runCommand("time set noon");server.runCommand("weather clear");
            server.runCommand("fill -28 99 -28 28 99 28 minecraft:smooth_stone");server.runCommand("tp @a 0 100 0 0 0");
            server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();CharacterAwakening.creative(p,JjkCraft.id("limitless"),false);CursedEnergy.set(p,20000);
                var entries=Techniques.all(p.registryAccess());check(entries.size()==296,"full technique registry present");
                var original=TechniqueService.get(p);Map<net.minecraft.resources.Identifier,Integer> training=new HashMap<>();
                TechniqueService.set(p,original.withMastery(Map.of()));double[] base=new double[entries.size()];
                for(int i=0;i<entries.size();i++){var e=entries.get(i);base[i]=TechniqueService.power(p,e.id(),e.definition(),TechniqueService.modifiers(p,e.id(),e.definition()));training.put(e.id(),e.definition().masteryUses()*4);}
                TechniqueService.set(p,original.withMastery(training));
                for(int i=0;i<entries.size();i++){var e=entries.get(i);double power=TechniqueService.power(p,e.id(),e.definition(),TechniqueService.modifiers(p,e.id(),e.definition()));check(Math.abs(power/base[i]-3)<.00001,"all techniques gain the full mastery curve: "+e.id());}
                var d=Techniques.get(p.registryAccess(),JjkCraft.id("infinity")).orElseThrow();var ctx=new CastContext(p,p.level(),JjkCraft.id("infinity"),d,1,1);var field=(InfinityBehavior)d.behavior();
                var enemy=net.minecraft.world.entity.EntityTypes.HUSK.create(p.level(),EntitySpawnReason.COMMAND);enemy.setNoAi(true);enemy.snapTo(p.getX(),p.getY(),p.getZ()+3,180,0);p.level().addFreshEntity(enemy);
                enemy.setDeltaMovement(.12,-.08,-.4);field.tick(ctx,0);var v=enemy.getDeltaMovement();check(v.z<0&&v.z>-.4,"Infinity slows approach instead of pushing away");check(Math.abs(v.x-.12)<1e-8&&Math.abs(v.y+.08)<1e-8,"Infinity preserves tangent movement and living gravity");
                enemy.setDeltaMovement(Vec3.ZERO);field.tick(ctx,0);check(enemy.getDeltaMovement().lengthSqr()==0,"a stationary outside target is held without repulsion");
                enemy.setPos(p.getX(),p.getY(),p.getZ()+.5);field.tick(ctx,0);check(enemy.getDeltaMovement().z>0&&enemy.getDeltaMovement().z<=.06,"only close contact gets a gentle separation");
                var arrow=net.minecraft.world.entity.EntityTypes.ARROW.create(p.level(),EntitySpawnReason.COMMAND);arrow.setOwner(enemy);arrow.setPos(p.getX(),p.getY()+.9,p.getZ()+2);arrow.setDeltaMovement(0,0,-.5);p.level().addFreshEntity(arrow);
                for(int i=0;i<30;i++)field.tick(ctx,i);check(arrow.getDeltaMovement().lengthSqr()==0&&arrow.isNoGravity(),"incoming projectile is suspended without a minimum-speed leak");
                field.end(ctx);check(!arrow.isNoGravity()&&arrow.getDeltaMovement().z<0,"stopping Infinity restores projectile physics");arrow.discard();enemy.discard();
                var aim=new CastContext(p,p.level(),JjkCraft.id("lapse_blue"),Techniques.get(p.registryAccess(),JjkCraft.id("lapse_blue")).orElseThrow(),1,1);
                check(com.jjkcraft.technique.field.SpatialShots.origin(aim).subtract(aim.eye()).cross(aim.look()).lengthSqr()<1e-10,"server spatial release is exactly on the aim ray");
            });
            c.waitFor(mc->ClientTechniques.known().contains(JjkCraft.id("hollow_purple")));
            c.setScreen(()->null);c.runOnClient(mc->{mc.gui.hud.getChat().clearMessages(false);mc.options.guiScale().set(2);mc.resizeGui();mc.options.fov().set(85);});
            c.getInput().pressKey(JjkKeys.LIGHT);c.waitTicks(18);c.takeScreenshot("mastery-wide-hands-idle");
            c.runOnClient(mc->mc.player.setXRot(62));c.waitTicks(3);c.takeScreenshot("mastery-wide-hands-looking-down");c.runOnClient(mc->mc.player.setXRot(0));
            c.getInput().pressKey(JjkKeys.LIGHT);c.waitTicks(5);c.takeScreenshot("mastery-wide-hands-jab");c.waitTicks(20);
            c.runOnClient(mc->mc.player.setMainArm(net.minecraft.world.entity.HumanoidArm.LEFT));c.getInput().pressKey(JjkKeys.LIGHT);c.waitTicks(5);c.takeScreenshot("mastery-wide-left-handed-jab");c.waitTicks(20);c.runOnClient(mc->mc.player.setMainArm(net.minecraft.world.entity.HumanoidArm.RIGHT));
            for(String move:new String[]{"lapse_blue","reversal_red","hollow_purple"}){
                server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();TechniqueService.cancel(p);TechniqueService.resetCooldowns(p);TechniqueService.setLoadout(p,0,Optional.of(JjkCraft.id(move)));CursedEnergy.set(p,20000);});
                c.waitFor(mc->ClientTechniques.state().slot(0).filter(JjkCraft.id(move)::equals).isPresent());c.getInput().holdKey(JjkKeys.CAST);c.waitTicks(move.contains("purple")?125:62);
                check(c.computeOnClient(mc->ChargeFx.count()==1),"charge has a single moving identity");c.takeScreenshot("mastery-wide-"+move+"-charge");
                c.runOnClient(mc->{mc.player.setYRot(34);mc.player.setXRot(18);});c.getInput().holdKey(net.minecraft.client.KeyMapping.get("key.forward"));c.waitTicks(8);c.takeScreenshot("mastery-wide-"+move+"-moving-aim");c.getInput().releaseKey(net.minecraft.client.KeyMapping.get("key.forward"));
                c.getInput().releaseKey(JjkKeys.CAST);c.waitTicks(move.contains("purple")?4:8);c.takeScreenshot("mastery-wide-"+move+"-release");c.waitTicks(70);
                server.runCommand("tp @a 0 100 0 0 0");c.waitTicks(4);
            }
            c.runOnClient(mc->{mc.getWindow().setWindowed(1100,650);mc.options.fov().set(70);});c.waitTicks(6);
            var families=c.computeOnClient(mc->mc.level.registryAccess().lookupOrThrow(CharacterAwakening.FAMILIES).keySet().stream().sorted().toList());
            check(families.size()==51,"gallery includes every innate family");
            for(int group=0;group<(families.size()+7)/8;group++){
                final int g=group;
                c.runOnClient(mc->{TechniqueFxClient.reset();WorldFx.clear();var registry=mc.level.registryAccess().lookupOrThrow(CharacterAwakening.FAMILIES);
                    for(int i=0;i<8&&g*8+i<families.size();i++){
                        var family=families.get(g*8+i);var id=registry.getValue(family).techniques().getFirst();var d=ClientTechniques.definition(id).orElseThrow();
                        Vec3 at=new Vec3((i%4-1.5)*3.5,101.2+(i/4)*2.8,9);
                        TechniqueFxClient.handle(new TechniqueFxPayload(mc.player.getId(),id,TechniqueFxPayload.Phase.FIELD,at,new Vec3(2,65,1),List.of(),1,d.visuals().color(),d.visuals().trail().ordinal()));
                        check(TechniqueMaterials.tile(id)>=0&&TechniqueMaterials.tile(id)<16,"family has an authored material: "+family);
                    }
                });c.waitTicks(7);c.takeScreenshot("mastery-all-families-materials-"+group);
            }
            c.runOnClient(mc->{TechniqueFxClient.reset();WorldFx.clear();for(var e:Techniques.all(mc.level.registryAccess()))TechniqueFxClient.handle(new TechniqueFxPayload(mc.player.getId(),e.id(),TechniqueFxPayload.Phase.HIT,new Vec3(0,101.2,7),new Vec3(0,0,1),List.of(),1,e.definition().visuals().color(),e.definition().visuals().trail().ordinal()));});
            c.waitTicks(3);check(c.computeOnClient(mc->WorldFx.lastFrameQuads()<=com.jjkcraft.client.config.ClientConfig.get().vfx.maxWorldQuads),"simultaneous roster effects respect the shared geometry budget");
        }finally{config.modulo.enabled=modulo;config.casting.terrainDamage=terrain;c.runOnClient(mc->{com.jjkcraft.client.config.ClientConfig.get().autoCombat=auto;mc.getWindow().setWindowed(854,480);});}
    }
}
