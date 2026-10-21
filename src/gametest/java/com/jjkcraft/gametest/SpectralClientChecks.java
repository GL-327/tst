package com.jjkcraft.gametest;

import java.util.*;
import com.jjkcraft.JjkCraft;
import com.jjkcraft.awakening.CharacterAwakening;
import com.jjkcraft.client.JjkKeys;
import com.jjkcraft.client.combat.CombatAnimations;
import com.jjkcraft.client.config.*;
import com.jjkcraft.client.state.ClientTechniques;
import com.jjkcraft.client.vfx.*;
import com.jjkcraft.entity.shikigami.*;
import com.jjkcraft.item.CursedEquipment;
import com.jjkcraft.network.payload.TechniqueFxPayload;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Native graphics checks: both camera modes, armor assets, creature rigs and technique materials. */
final class SpectralClientChecks {
    private static void shot(ClientGameTestContext c,String name){c.takeScreenshot(name);if(c.computeOnClient(mc->mc.gui.screen()!=null))throw new AssertionError("Unexpected screen in "+name+": "+c.computeOnClient(mc->mc.gui.screen().getClass().getSimpleName()));}
    static void run(ClientGameTestContext c){
        TestWorldSave save;
        try(var world=c.worldBuilder().create()){
            var server=world.getServer();world.getConnection().waitForChunksRender();
            server.runCommand("gamemode creative @a");server.runCommand("gamerule spawn_mobs false");server.runCommand("time set noon");
            server.computeOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();CharacterAwakening.creative(p,JjkCraft.id("limitless"),false);
                var b=BlockPos.containing(p.position());for(int x=-12;x<=12;x++)for(int z=-12;z<=14;z++){p.level().setBlockAndUpdate(b.offset(x,-1,z),Blocks.SMOOTH_STONE.defaultBlockState());for(int y=0;y<12;y++)p.level().setBlockAndUpdate(b.offset(x,y,z),Blocks.AIR.defaultBlockState());}
                p.setYRot(0);p.setXRot(0);com.jjkcraft.sorcerer.SorcererLifecycle.fullSync(p);return true;});
            c.waitFor(mc->ClientTechniques.known().contains(JjkCraft.id("hollow_purple")));c.waitTicks(5);c.setScreen(()->null);
            c.runOnClient(mc->{mc.options.guiScale().set(2);mc.resizeGui();mc.player.setYRot(0);mc.player.setXRot(0);mc.options.chatOpacity().set(0d);ClientConfig.get().autoCombat=false;});c.waitTicks(4);
            shot(c,"jjkcraft-spectral-first-person-stance");
            // Exercise the real inverted Attack mapping, rather than calling a server attack method.
            server.computeOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var t=net.minecraft.world.entity.EntityTypes.HUSK.create(p.level(),EntitySpawnReason.EVENT);t.setPos(p.position().add(0,0,2.3));t.setNoAi(true);p.level().addFreshEntity(t);return true;});c.waitTicks(3);
            c.runOnClient(mc->{mc.options.keyAttack.setKey(com.mojang.blaze3d.platform.InputConstants.Type.MOUSE.getOrCreate(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT));mc.options.keyUse.setKey(com.mojang.blaze3d.platform.InputConstants.Type.MOUSE.getOrCreate(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT));net.minecraft.client.KeyMapping.resetMapping();ClientConfig.get().autoCombat=true;});
            c.getInput().pressMouse(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT);c.waitTicks(18);
            if(!server.computeOnServer(s->com.jjkcraft.combat.CursedCombat.autoStats(s.getPlayerList().getPlayers().getFirst())[1]==1))throw new AssertionError("real inverted quick Auto attack must land");shot(c,"jjkcraft-spectral-inverted-auto-input");
            c.runOnClient(mc->{mc.options.keyAttack.setKey(mc.options.keyAttack.getDefaultKey());mc.options.keyUse.setKey(mc.options.keyUse.getDefaultKey());net.minecraft.client.KeyMapping.resetMapping();ClientConfig.get().autoCombat=false;});c.waitTicks(24);
            server.computeOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();for(var t:p.level().getEntitiesOfClass(Mob.class,p.getBoundingBox().inflate(8)))t.discard();return true;});
            c.getInput().pressKey(JjkKeys.LIGHT);c.waitFor(mc->{var pose=CombatAnimations.pose(mc.player.getId());return pose!=null&&pose.move().equals("jab");});
            shot(c,"jjkcraft-spectral-first-person-jab");c.waitTicks(25);
            c.getInput().holdKey(JjkKeys.BLOCK);c.waitTicks(4);shot(c,"jjkcraft-spectral-first-person-guard");c.getInput().releaseKey(JjkKeys.BLOCK);
            c.runOnClient(mc->mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));c.waitTicks(5);
            for(String key:List.of("sorcerer_uniform","binding_vestments","heavenly_hunter")){
                server.computeOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var slots=new EquipmentSlot[]{EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET};String[] names={"helmet","chestplate","leggings","boots"};for(int i=0;i<4;i++)p.setItemSlot(slots[i],new ItemStack(CursedEquipment.ITEMS.get(key+"_"+names[i])));return true;});
                c.waitTicks(8);shot(c,"jjkcraft-spectral-armor-"+key);
            }
            c.getInput().pressKey(JjkKeys.HEAVY);c.waitFor(mc->{var pose=CombatAnimations.pose(mc.player.getId());return pose!=null&&pose.move().equals("heavy");});c.waitTicks(3);shot(c,"jjkcraft-spectral-third-person-heavy");c.waitTicks(30);
            c.getInput().holdKey(JjkKeys.BLOCK);c.waitTicks(4);shot(c,"jjkcraft-spectral-third-person-guard");c.getInput().releaseKey(JjkKeys.BLOCK);
            for(String key:List.of("playful_cloud","dragon_bone","stormbell_spear","ember_wakizashi","sealbreaker_tanto")){
                server.computeOnServer(s->{s.getPlayerList().getPlayers().getFirst().setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(CursedEquipment.ITEMS.get(key)));return true;});c.waitTicks(4);shot(c,"jjkcraft-spectral-tool-"+key);
            }
            c.runOnClient(mc->mc.options.setCameraType(CameraType.FIRST_PERSON));
            server.computeOnServer(s->{s.getPlayerList().getPlayers().getFirst().setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);return true;});
            // Load every new rig and atlas in the actual renderer, facing the camera rather than its back.
            if(!"equipment".equals(System.getProperty("jjkcraft.clientTestFocus"))){
            var keys=server.computeOnServer(s->{var list=new ArrayList<String>();for(var type:com.jjkcraft.curse.CurseEntities.ALL)list.add(BuiltInRegistries.ENTITY_TYPE.getKey(type).getPath());list.add("divine_dog");list.add("nue");list.add("toad");list.add("shadow_divine_dog");list.add("transfigured_soul");for(var id:ShadowBeasts.TYPES.keySet())list.add(id.getPath());return list;});
            for(String key:keys){
                int id=server.computeOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();for(var old:p.level().getEntitiesOfClass(Mob.class,p.getBoundingBox().inflate(30)))old.discard();var type=BuiltInRegistries.ENTITY_TYPE.getValue(JjkCraft.id(key));var e=type.create(p.level(),EntitySpawnReason.EVENT);if(!(e instanceof Mob mob))throw new AssertionError("missing creature "+key);
                    mob.snapTo(p.getX(),p.getY(),p.getZ()+Math.max(5,mob.getBbHeight()*1.8),180,0);mob.setNoAi(true);if(mob instanceof com.jjkcraft.curse.CurseEntity curse)curse.getEntityData().set(com.jjkcraft.curse.CurseEntity.EMERGING,0);if(mob instanceof ShikigamiEntity pet)pet.bind(p,600,1);p.level().addFreshEntity(mob);return mob.getId();});
                c.waitFor(mc->mc.level.getEntity(id)!=null);c.waitTicks(4);
                c.runOnClient(mc->{if(mc.level.getEntity(id) instanceof LivingEntity e){e.setYRot(180);e.yBodyRot=180;e.yBodyRotO=180;e.yHeadRot=180;e.yHeadRotO=180;Vec3 aim=e.getBoundingBox().getCenter().subtract(mc.player.getEyePosition());mc.player.setYRot(0);mc.player.setXRot((float)-Math.toDegrees(Math.atan2(aim.y,Math.sqrt(aim.x*aim.x+aim.z*aim.z))));}});
                shot(c,"jjkcraft-spectral-creature-"+key);
                if(key.equals("ash_oni")||key.equals("nue")){
                    server.computeOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var e=p.level().getEntity(id);if(e instanceof com.jjkcraft.curse.CurseEntity curse)curse.triggerAnim("action","heavy");if(e instanceof ShikigamiEntity pet)pet.triggerAnim("action","attack");return true;});c.waitTicks(8);shot(c,"jjkcraft-spectral-animated-"+key);
                }
            }
            server.computeOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();for(var old:p.level().getEntitiesOfClass(Mob.class,p.getBoundingBox().inflate(30)))old.discard();return true;});
            c.runOnClient(mc->{mc.player.setYRot(0);mc.player.setXRot(0);});
            for(String key:List.of("lapse_blue","reversal_red","hollow_purple","piercing_blood","ice_lance","shrine_furnace","ten_shadows_divine_dogs","reverse_cursed_technique")){
                c.runOnClient(mc->{TechniqueFxClient.reset();WorldFx.clear();Vec3 o=mc.player.position().add(0,1.4,5);TechniqueFxClient.handle(new TechniqueFxPayload(mc.player.getId(),JjkCraft.id(key),TechniqueFxPayload.Phase.FIELD,o,new Vec3(2.2,60,1),List.of(),1.2f));});c.waitTicks(6);shot(c,"jjkcraft-spectral-vfx-"+key);c.waitTicks(2);
            }
            c.runOnClient(mc->{ClientConfig.get().vfx.level=JjkClientConfig.VfxLevel.LOW;TechniqueFxClient.reset();WorldFx.clear();TechniqueFxClient.handle(new TechniqueFxPayload(mc.player.getId(),JjkCraft.id("hollow_purple"),TechniqueFxPayload.Phase.BURST,mc.player.position().add(0,1.4,4),new Vec3(0,0,1),List.of(),1));});c.waitTicks(4);shot(c,"jjkcraft-spectral-low-vfx");
            c.runOnClient(mc->{ClientConfig.get().vfx.level=JjkClientConfig.VfxLevel.OFF;});c.waitTicks(3);shot(c,"jjkcraft-spectral-effects-off");
            c.runOnClient(mc->{ClientConfig.get().vfx.level=JjkClientConfig.VfxLevel.MEDIUM;TechniqueFxClient.reset();WorldFx.clear();});
            c.getInput().pressKey(JjkKeys.DOMAIN);c.waitFor(mc->!com.jjkcraft.client.state.ClientDomains.own().isEmpty());c.waitTicks(48);
            c.runOnClient(mc->mc.player.setXRot(16));shot(c,"jjkcraft-spectral-domain-interior");server.runCommand("jjk domain close @p");c.waitTicks(25);c.runOnClient(mc->mc.player.setXRot(0));
            }
            c.runOnClient(mc->{ClientConfig.get().vfx.level=JjkClientConfig.VfxLevel.MEDIUM;ClientConfig.get().autoCombat=true;});
            if("reborn".equals(System.getProperty("jjkcraft.clientTestFocus")))RebornClientChecks.run(c,server);
            save=world.getWorldSave();
        }
        try(var reopen=save.open()){
            reopen.getConnection().waitForChunksRender();
            if(!reopen.getServer().computeOnServer(s->CursedEquipment.pieces(s.getPlayerList().getPlayers().getFirst(),"heavenly_hunter")==4))throw new AssertionError("wearable equipment did not survive world reopen");
            if("reborn".equals(System.getProperty("jjkcraft.clientTestFocus"))&&!reopen.getServer().computeOnServer(s->s.getPlayerList().getPlayers().getFirst().getAttachedOrCreate(com.jjkcraft.technique.ProjectionPlans.PLAN).steps().size()==1))throw new AssertionError("custom Projection route did not survive reopen");
            c.waitTicks(4);shot(c,"jjkcraft-spectral-reopened-equipment");
        }
    }
}
