package com.jjkcraft.gametest;

import java.util.*;
import com.jjkcraft.JjkCraft;
import com.jjkcraft.awakening.CharacterAwakening;
import com.jjkcraft.client.screen.*;
import com.jjkcraft.client.state.*;
import com.jjkcraft.entity.shikigami.*;
import com.jjkcraft.technique.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.world.entity.EntitySpawnReason;

final class QolClientChecks {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static net.minecraft.client.gui.components.EditBox search(net.minecraft.client.Minecraft mc){return mc.gui.screen().children().stream().filter(e->e instanceof net.minecraft.client.gui.components.EditBox).map(e->(net.minecraft.client.gui.components.EditBox)e).findFirst().orElseThrow();}
    static void run(ClientGameTestContext c){
        boolean old=com.jjkcraft.config.JjkConfig.server().modulo.enabled;com.jjkcraft.config.JjkConfig.server().modulo.enabled=false;
        boolean auto=c.computeOnClient(mc->com.jjkcraft.client.config.ClientConfig.get().autoCombat);c.runOnClient(mc->com.jjkcraft.client.config.ClientConfig.get().autoCombat=false);
        try(var world=c.worldBuilder().create()){
            var server=world.getServer();world.getConnection().waitForChunksRender();server.runCommand("gamemode creative @a");server.runCommand("gamerule spawn_mobs false");server.runCommand("time set noon");
            server.runCommand("fill -24 99 -24 24 99 24 minecraft:smooth_stone");server.runCommand("tp @a 0 100 0 0 0");
            server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();CharacterAwakening.creative(p,JjkCraft.id("limitless"),false);TechniqueService.setLoadout(p,0,Optional.of(TechniqueBindings.binding(JjkCraft.id("limitless"))));});
            c.waitFor(mc->ClientTechniques.known().contains(JjkCraft.id("hollow_purple")));c.setScreen(()->null);
            c.setScreen(()->new TechniqueAbilityScreen(JjkCraft.id("limitless")));c.waitTicks(3);c.runOnClient(mc->{search(mc).setValue("purple");mc.gui.screen().setFocused(search(mc));});
            c.waitTicks(3);check(c.computeOnClient(mc->((TechniqueAbilityScreen)mc.gui.screen()).visibleMoves().equals(List.of(JjkCraft.id("hollow_purple")))),"search filters translated names / identifiers");check(c.computeOnClient(mc->search(mc).isFocused()),"search retains typing focus after filtering");
            c.takeScreenshot("qol-ability-search");
            server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();TechniqueService.set(p,TechniqueService.get(p).withCooldown(JjkCraft.id("hollow_purple"),p.level().getGameTime()+200,p.level().getGameTime()));});
            c.waitFor(mc->ClientTechniqueStatus.get(JjkCraft.id("hollow_purple")).label().startsWith("Ready in"));c.clickScreenButton("Filter: All");c.clickScreenButton("Filter: Learned");c.waitTicks(3);
            check(c.computeOnClient(mc->((TechniqueAbilityScreen)mc.gui.screen()).visibleMoves().isEmpty()),"Ready filter excludes cooling-down moves");c.takeScreenshot("qol-ready-filter-cooldown");c.setScreen(()->null);
            var binding=TechniqueBindings.binding(JjkCraft.id("limitless"));c.setScreen(()->new TechniqueWheelScreen(binding,0));c.clickScreenButton("Purple");c.setScreen(()->null);c.setScreen(()->new TechniqueWheelScreen(binding,0));c.waitTicks(2);
            check(c.computeOnClient(mc->{var w=(TechniqueWheelScreen)mc.gui.screen();return w.entryCount()==1&&w.entry(w.selection()).filter(JjkCraft.id("hollow_purple")::equals).isPresent();}),"wheel restores the last branch and selected variant");c.takeScreenshot("qol-wheel-remembers-branch");
            server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();TechniqueService.grant(p,JjkCraft.id("lacquer_ward"));check(TechniqueService.cast(p,JjkCraft.id("lacquer_ward"),0)==CastFailure.NONE,"independent channel starts");});
            c.clickScreenButton("Infinity");var xy=c.computeOnClient(mc->{var w=(TechniqueWheelScreen)mc.gui.screen();int[] at=w.nodePoint(0);return new double[]{at[0]*mc.getWindow().getScreenWidth()/(double)w.width,at[1]*mc.getWindow().getScreenHeight()/(double)w.height};});c.getInput().setCursorPos(xy[0],xy[1]);c.waitTicks(2);
            c.getInput().pressMouse(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT);c.waitFor(mc->mc.gui.screen()==null&&!ClientTechniques.infinityEnabled());
            server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();check(TechniqueService.active(p).filter(a->a.id().equals(JjkCraft.id("lacquer_ward"))).isPresent(),"toggling Infinity through the wheel does not release another channel");TechniqueService.cancel(p);InfinityField.toggle(p);com.jjkcraft.reverse.ReverseTechnique.set(p,com.jjkcraft.reverse.ReverseState.EMPTY,false);});
            c.waitFor(mc->ClientTechniques.infinityActive()&&ClientTechniques.infinityUpkeep()>0);c.takeScreenshot("qol-infinity-live-upkeep");
            server.runOnServer(s->com.jjkcraft.reverse.ReverseTechnique.set(s.getPlayerList().getPlayers().getFirst(),com.jjkcraft.reverse.ReverseState.EMPTY.withUnlocked(true),false));
            c.waitFor(mc->ClientTechniques.infinityUpkeep()==0&&ClientTechniqueStatus.infinity().contains("no upkeep"));c.takeScreenshot("qol-infinity-rct-status");
            server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();CharacterAwakening.creative(p,JjkCraft.id("ten_shadows"),false);p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(100);p.setHealth(p.getMaxHealth());TechniqueService.setLoadout(p,0,Optional.of(TechniqueBindings.binding(JjkCraft.id("ten_shadows"))));});
            c.waitFor(mc->ClientTechniques.known().contains(JjkCraft.id("ten_shadows_mahoraga")));server.runCommand("gamemode survival @a");
            c.setScreen(()->new TechniqueAbilityScreen(JjkCraft.id("ten_shadows"),JjkCraft.id("ten_shadows_mahoraga")));c.waitTicks(3);
            check(c.computeOnClient(mc->((TechniqueAbilityScreen)mc.gui.screen()).visibleMoves().contains(JjkCraft.id("ten_shadows_mahoraga"))),"deep-linked abilities are scrolled into view");c.takeScreenshot("qol-deep-link-subjugation");
            c.clickScreenButton("Subjugate");c.waitFor(mc->mc.gui.screen()==null&&ClientTechniqueMenus.view(JjkCraft.id("ten_shadows"))!=null&&!ClientTechniqueMenus.view(JjkCraft.id("ten_shadows")).trial().isEmpty());c.waitTicks(4);
            check(c.computeOnClient(mc->ClientTechniqueMenus.trialSeconds()>0&&ClientTechniqueMenus.trialSeconds()<=180),"ritual tracker receives remaining time");c.runOnClient(mc->mc.gui.hud.getChat().clearMessages(false));c.takeScreenshot("qol-ritual-countdown");
            c.setScreen(()->new TechniqueAbilityScreen(JjkCraft.id("ten_shadows")));c.waitTicks(3);c.clickScreenButton("End ritual");c.waitFor(mc->ClientTechniqueMenus.view(JjkCraft.id("ten_shadows")).trial().isEmpty());
            server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();check(TenShadows.trial(p).isEmpty()&&!TenShadows.tamed(p).contains(JjkCraft.id("shadow_mahoraga")),"ending a ritual gives no unlock");
                for(String key:List.of("divine_dog","agito")){var pet=(ShikigamiEntity)net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getValue(JjkCraft.id(key)).create(p.level(),EntitySpawnReason.COMMAND);pet.bind(p,600,1);pet.snapTo(p.getX()+3,p.getY(),p.getZ()+4,180,0);pet.setNoAi(true);p.level().addFreshEntity(pet);}
                var other=net.minecraft.world.entity.EntityTypes.HUSK.create(p.level(),EntitySpawnReason.COMMAND);other.snapTo(p.getX()+16,p.getY(),p.getZ(),0,0);other.setNoAi(true);p.level().addFreshEntity(other);
                var pet=com.jjkcraft.entity.JjkEntities.NUE.create(p.level(),EntitySpawnReason.COMMAND);pet.bind(other,600,1);pet.snapTo(other.getX(),other.getY(),other.getZ()+2,0,0);pet.setNoAi(true);p.level().addFreshEntity(pet);
            });
            c.waitFor(mc->ClientTechniqueMenus.view(JjkCraft.id("ten_shadows")).owned()==2);c.waitTicks(2);c.takeScreenshot("qol-summon-count-and-dismiss");c.clickScreenButton("Dismiss (2)");c.waitFor(mc->ClientTechniqueMenus.view(JjkCraft.id("ten_shadows")).owned()==0);
            server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();check(ShikigamiEntity.ownedBy(p.level(),ShikigamiEntity.class,p).isEmpty(),"dismiss clears both base summons and fusion results");check(p.level().getEntitiesOfClass(ShikigamiEntity.class,p.getBoundingBox().inflate(30)).size()==1,"another owner's summons remain intact");});
            c.takeScreenshot("qol-ritual-ended-and-summons-dismissed");c.setScreen(()->null);
        }finally{com.jjkcraft.config.JjkConfig.server().modulo.enabled=old;c.runOnClient(mc->com.jjkcraft.client.config.ClientConfig.get().autoCombat=auto);}
    }
}
