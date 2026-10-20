package com.jjkcraft.gametest;

import java.util.Optional;
import com.jjkcraft.JjkCraft;
import com.jjkcraft.awakening.CharacterAwakening;
import com.jjkcraft.client.JjkKeys;
import com.jjkcraft.client.state.ClientTechniques;
import com.jjkcraft.energy.*;
import com.jjkcraft.technique.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;

/** Real key edges, progress payload/HUD, release cast, debris rendering and menu cancellation. */
final class ChargeClientChecks {
    static void run(ClientGameTestContext c){
        try(var world=c.worldBuilder().create()){
            var server=world.getServer();world.getConnection().waitForChunksRender();server.runCommand("gamemode creative @a");server.runCommand("gamerule spawn_mobs false");server.runCommand("time set noon");
            server.computeOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();CharacterAwakening.creative(p,JjkCraft.id("limitless"),false);p.setAttached(SorceryPractice.STATE,new PracticeState(3000,10000,false,false,100,100,60,5,true));CursedEnergy.set(p,20000);TechniqueService.setLoadout(p,0,Optional.of(JjkCraft.id("maximum_output_blue")));TechniqueService.selectSlot(p,0);
                var base=BlockPos.containing(p.position());for(int x=-8;x<=8;x++)for(int z=-3;z<=12;z++){p.level().setBlockAndUpdate(base.offset(x,-1,z),Blocks.STONE.defaultBlockState());for(int y=0;y<10;y++)p.level().setBlockAndUpdate(base.offset(x,y,z),Blocks.AIR.defaultBlockState());}
                for(int y=0;y<6;y++)p.level().setBlockAndUpdate(base.offset(0,y,7),Blocks.OAK_LOG.defaultBlockState());for(int x=-2;x<=2;x++)for(int z=5;z<=9;z++)for(int y=4;y<=6;y++)if(x!=0||z!=7)p.level().setBlockAndUpdate(base.offset(x,y,z),Blocks.OAK_LEAVES.defaultBlockState());
                p.setYRot(0);p.setXRot(0);com.jjkcraft.sorcerer.SorcererLifecycle.fullSync(p);return true;});
            c.waitFor(mc->ClientTechniques.state().slot(0).filter(JjkCraft.id("maximum_output_blue")::equals).isPresent());c.waitTicks(5);c.setScreen(()->null);c.runOnClient(mc->{mc.player.setYRot(0);mc.player.setXRot(0);mc.options.guiScale().set(2);mc.resizeGui();});c.waitTicks(3);
            // Ordinary moves use the same physical key but never enter the charging state.
            server.computeOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();TechniqueService.setLoadout(p,0,Optional.of(JjkCraft.id("infinity")));return true;});c.waitFor(mc->ClientTechniques.state().slot(0).filter(JjkCraft.id("infinity")::equals).isPresent());
            c.getInput().pressKey(JjkKeys.CAST);c.waitFor(mc->ClientTechniques.activeIs(JjkCraft.id("infinity")));
            if(c.computeOnClient(mc->ClientTechniques.charge().slot())>=0)throw new AssertionError("Infinity must toggle without charging");c.takeScreenshot("jjkcraft-selective-infinity-toggle");
            c.getInput().pressKey(JjkKeys.CAST);c.waitFor(mc->ClientTechniques.active().isEmpty());
            server.computeOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();TechniqueService.grant(p,JjkCraft.id("ten_shadows_divine_dogs"));TechniqueService.setLoadout(p,0,Optional.of(JjkCraft.id("ten_shadows_divine_dogs")));return true;});c.waitFor(mc->ClientTechniques.state().slot(0).filter(JjkCraft.id("ten_shadows_divine_dogs")::equals).isPresent());
            c.getInput().pressKey(JjkKeys.CAST);c.waitTicks(4);
            if(!server.computeOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();return TechniqueService.chargePercent(p)==0&&com.jjkcraft.entity.shikigami.ShikigamiEntity.ownedBy(p.level(),com.jjkcraft.entity.shikigami.ShikigamiEntity.class,p).size()==2;}))throw new AssertionError("quick tap must summon both dogs without a charge");
            c.setScreen(()->new com.jjkcraft.client.screen.LoadoutScreen(JjkCraft.id("ten_shadows_divine_dogs")));c.waitTicks(3);c.takeScreenshot("jjkcraft-selective-summon-activation");c.setScreen(()->null);
            server.computeOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();for(var pet:com.jjkcraft.entity.shikigami.ShikigamiEntity.ownedBy(p.level(),com.jjkcraft.entity.shikigami.ShikigamiEntity.class,p))pet.discard();TechniqueService.grant(p,JjkCraft.id("lacquer_ward"));TechniqueService.setLoadout(p,0,Optional.of(JjkCraft.id("lacquer_ward")));return true;});c.waitFor(mc->ClientTechniques.state().slot(0).filter(JjkCraft.id("lacquer_ward")::equals).isPresent());
            c.getInput().holdKey(JjkKeys.CAST);c.waitFor(mc->ClientTechniques.activeIs(JjkCraft.id("lacquer_ward")));c.waitTicks(5);
            if(c.computeOnClient(mc->ClientTechniques.charge().slot())>=0)throw new AssertionError("guard must sustain without a charge");c.getInput().releaseKey(JjkKeys.CAST);c.waitFor(mc->ClientTechniques.active().isEmpty());
            server.computeOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();TechniqueService.setLoadout(p,0,Optional.of(JjkCraft.id("maximum_output_blue")));CursedEnergy.set(p,20000);return true;});c.waitFor(mc->ClientTechniques.state().slot(0).filter(JjkCraft.id("maximum_output_blue")::equals).isPresent());
            if(!c.computeOnClient(mc->ClientTechniques.definition(JjkCraft.id("maximum_output_blue")).orElseThrow().cast().chargeable()))throw new AssertionError("charge opt-in must reach client registries");
            c.takeScreenshot("jjkcraft-charge-tree-before");
            c.getInput().holdKey(JjkKeys.CAST);c.waitFor(mc->ClientTechniques.charge().slot()==0);
            double chargedStart=server.computeOnServer(s->CursedEnergy.current(s.getPlayerList().getPlayers().getFirst()));
            c.waitTicks(63);c.waitFor(mc->ClientTechniques.charge().percent()==100);
            if(server.computeOnServer(s->CursedEnergy.current(s.getPlayerList().getPlayers().getFirst()))>=chargedStart)throw new AssertionError("holding charge must drain CE even with huge reserves");
            c.takeScreenshot("jjkcraft-charge-blue-full");
            c.getInput().releaseKey(JjkKeys.CAST);c.waitTicks(8);c.takeScreenshot("jjkcraft-charge-blue-pulling-blocks");
            if(!server.computeOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();return p.level().getBlockState(BlockPos.containing(p.position()).offset(0,2,7)).isAir();}))throw new AssertionError("charged Maximum Blue must remove real trunk blocks");
            c.waitTicks(55);server.computeOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();TechniqueService.cancel(p);TechniqueService.resetCooldowns(p);CursedEnergy.set(p,20000);TechniqueService.setLoadout(p,0,Optional.of(JjkCraft.id("hollow_purple")));return true;});c.waitFor(mc->ClientTechniques.state().slot(0).filter(JjkCraft.id("hollow_purple")::equals).isPresent());
            c.getInput().holdKey(JjkKeys.CAST);c.waitTicks(144);c.waitFor(mc->ClientTechniques.charge().percent()==200);c.takeScreenshot("jjkcraft-charge-purple-200");c.getInput().releaseKey(JjkKeys.CAST);c.waitTicks(6);c.takeScreenshot("jjkcraft-charge-purple-release");
            server.computeOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();TechniqueService.cancel(p);TechniqueService.resetCooldowns(p);CursedEnergy.set(p,20000);return true;});c.getInput().holdKey(JjkKeys.CAST);c.waitTicks(5);c.getInput().pressKey(JjkKeys.PROFILE);c.waitTicks(3);c.getInput().releaseKey(JjkKeys.CAST);
            if(server.computeOnServer(s->TechniqueService.chargePercent(s.getPlayerList().getPlayers().getFirst()))!=0)throw new AssertionError("opening menu must cancel charging");c.takeScreenshot("jjkcraft-charge-menu-cancel");
        }
    }
}
