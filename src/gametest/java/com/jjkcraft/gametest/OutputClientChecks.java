package com.jjkcraft.gametest;
import com.jjkcraft.JjkCraft;
import com.jjkcraft.awakening.CharacterAwakening;
import com.jjkcraft.client.state.*;
import com.jjkcraft.client.screen.*;
import com.jjkcraft.sorcerer.*;
import com.jjkcraft.technique.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.world.level.GameType;
final class OutputClientChecks {
    static void run(ClientGameTestContext c){
        try(var world=c.worldBuilder().create()){
            var server=world.getServer();world.getConnection().waitForChunksRender();server.runCommand("gamemode creative @a");server.runCommand("gamerule spawn_mobs false");
            server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();CharacterAwakening.creative(p,JjkCraft.id("blood_manipulation"),false);p.setGameMode(GameType.SURVIVAL);com.jjkcraft.training.Training.award(p,"control",100000);SorcererLifecycle.fullSync(p);});
            var id=JjkCraft.id("piercing_blood");c.waitFor(m->m.player!=null&&!m.player.isCreative()&&ClientProgress.get().awakened()&&ClientTechniques.known().contains(id)&&ClientPersonalization.outputLimit(id)==3000&&ClientPersonalization.universalLimit()==3000);
            c.setScreen(()->new TechniqueWorkshopScreen(id));c.runOnClient(m->{for(var child:m.gui.screen().children())if(child instanceof EditBox b&&b.getMessage().getString().equals("CE output"))b.setValue("3000");});c.waitTicks(2);c.clickScreenButton("Apply tuning");c.waitFor(m->ClientPersonalization.get(id).output()==3000);c.takeScreenshot("output-veteran-technique-3000");
            c.setScreen(PracticeSettingsScreen::new);c.runOnClient(m->{for(var child:m.gui.screen().children())if(child instanceof EditBox b&&(b.getMessage().getString().equals("Reinforcement output %")||b.getMessage().getString().equals("RCT healing output %")))b.setValue("3000");});c.clickScreenButton("Apply settings");c.waitFor(m->ClientPersonalization.practice().output()==3000&&ClientPersonalization.practice().healing()==3000);c.takeScreenshot("output-veteran-universal-3000");
            server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();Progression.set(p,Progression.get(p).withGrade(JjkCraft.id("grade_4"),0).withGrowth(SorcererProgress.Growth.NONE));TechniqueService.set(p,TechniqueService.get(p).withMastery(java.util.Map.of()));p.setAttached(com.jjkcraft.training.Training.STATE,com.jjkcraft.training.TrainingState.EMPTY);SorcererLifecycle.fullSync(p);});
            c.waitFor(m->ClientPersonalization.outputLimit(id)==300&&ClientPersonalization.universalLimit()==300&&ClientPersonalization.get(id).output()==300);c.setScreen(()->new TechniqueWorkshopScreen(id));c.waitTicks(3);c.takeScreenshot("output-lower-grade-enforced-ceiling");
        }
    }
}
