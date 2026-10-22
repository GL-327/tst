package com.jjkcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;

import com.jjkcraft.JjkCraft;
import com.jjkcraft.client.JjkCraftClient;
import com.jjkcraft.client.compat.DisguiseView;
import com.jjkcraft.client.screen.CodexScreen;
import com.jjkcraft.client.screen.GrowthTreeScreen;
import com.jjkcraft.client.screen.LoadoutScreen;
import com.jjkcraft.client.screen.ProfileScreen;
import com.jjkcraft.client.screen.CullingScreen;
import com.jjkcraft.client.screen.TechniqueWheelScreen;
import com.jjkcraft.client.screen.VowBuilderScreen;
import com.jjkcraft.client.state.ClientDomains;
import com.jjkcraft.client.state.ClientEnergy;
import com.jjkcraft.client.state.ClientProgress;
import com.jjkcraft.client.state.ClientCulling;
import com.jjkcraft.client.state.ClientSensing;
import com.jjkcraft.client.state.ClientSorcery;
import com.jjkcraft.client.state.ClientTechniques;
import com.jjkcraft.client.vfx.PromotionCeremony;
import com.jjkcraft.curse.CurseEntity;
import com.jjkcraft.curse.Curses;
import com.jjkcraft.energy.CursedEnergy;
import com.jjkcraft.client.modulo.ClientModulo;
import com.jjkcraft.config.JjkConfig;
import com.jjkcraft.damage.JjkDamageTypes;
import com.jjkcraft.modulo.Modulo;
import com.jjkcraft.modulo.entity.MahoragaEntity;
import com.jjkcraft.modulo.entity.ModuloEntities;
import com.jjkcraft.modulo.entity.ModuloNpcEntity;
import com.jjkcraft.modulo.mission.Missions;

/**
 * Boots a real client, creates a singleplayer world and checks the client side of each system,
 * saving screenshots to {@code build/run/clientGameTest/screenshots} for visual review.
 */
@SuppressWarnings("UnstableApiUsage")
public final class SmokeClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (!JjkCraft.isInitialized() || !JjkCraftClient.isInitialized()) {
			throw new AssertionError("jjkcraft entrypoints did not run on the client");
		}
		if ("revision".equals(System.getProperty("jjkcraft.clientTestFocus"))) {
			revision(context);
			return;
		}
		if("personalization".equals(System.getProperty("jjkcraft.clientTestFocus"))){personalization(context);return;}
        if("refinement".equals(System.getProperty("jjkcraft.clientTestFocus"))){CombatRefinementChecks.run(context);return;}
        if("refinementAll".equals(System.getProperty("jjkcraft.clientTestFocus"))){CombatRefinementChecks.run(context);CreatureRevampChecks.run(context);return;}
        if("refinementArt".equals(System.getProperty("jjkcraft.clientTestFocus"))){CreatureRevampChecks.run(context);return;}
        if("reconcile".equals(System.getProperty("jjkcraft.clientTestFocus"))){ReconciliationClientChecks.run(context);return;}
        if("qol".equals(System.getProperty("jjkcraft.clientTestFocus"))){QolClientChecks.run(context);return;}
        if("qolRegression".equals(System.getProperty("jjkcraft.clientTestFocus"))){QolClientChecks.run(context);ReconciliationClientChecks.run(context);return;}
        if("reconcileArt".equals(System.getProperty("jjkcraft.clientTestFocus"))){CreatureRevampChecks.run(context);EquipmentReworkChecks.run(context);return;}
        if("wheel".equals(System.getProperty("jjkcraft.clientTestFocus"))){WheelClientChecks.run(context);return;}
        if("polish".equals(System.getProperty("jjkcraft.clientTestFocus"))){WheelClientChecks.run(context);PolishClientChecks.run(context);return;}
		if("deep".equals(System.getProperty("jjkcraft.clientTestFocus"))){deepSorcery(context);return;}
		if("overhaul".equals(System.getProperty("jjkcraft.clientTestFocus"))){qualityOverhaul(context);return;}
		if("menagerie".equals(System.getProperty("jjkcraft.clientTestFocus"))){MenagerieClientChecks.run(context);return;}
        if("charge".equals(System.getProperty("jjkcraft.clientTestFocus"))){ChargeClientChecks.run(context);return;}
        if("spectral".equals(System.getProperty("jjkcraft.clientTestFocus"))||"equipment".equals(System.getProperty("jjkcraft.clientTestFocus"))||"reborn".equals(System.getProperty("jjkcraft.clientTestFocus"))){SpectralClientChecks.run(context);return;}
		if("input".equals(System.getProperty("jjkcraft.clientTestFocus"))||"atelier".equals(System.getProperty("jjkcraft.clientTestFocus"))){AtelierClientChecks.run(context);return;}
		if("arsenal".equals(System.getProperty("jjkcraft.clientTestFocus"))){ArsenalClientChecks.run(context);return;}
        if("sandbox".equals(System.getProperty("jjkcraft.clientTestFocus"))){sandboxPowers(context);return;}

		double originalRegen = JjkConfig.server().energy.regenPercentPerSecond;
		try {
		TestWorldSave save;
		try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
			singleplayer.getConnection().waitForChunksRender();
			// Awakening on first join can roll Heavenly Restriction (no CE); force a normal sorcerer.
			singleplayer.getServer().runCommand("jjk awaken @a false");
			// The Culling Game may have started by itself on join; earlier scenes run without it.
			singleplayer.getServer().runCommand("jjk culling reset");
			context.waitTicks(5);
			context.takeScreenshot("jjkcraft-smoke-singleplayer");

			if (!"relog".equals(System.getProperty("jjkcraft.clientTestFocus"))) {
			energyHud(context, singleplayer);
			screensAndCeremony(context, singleplayer);
			techniques(context, singleplayer);
			curses(context, singleplayer);
			domains(context, singleplayer);
			sorcery(context, singleplayer);
			culling(context, singleplayer);
			arsenal(context, singleplayer);
			modulo(context, singleplayer);
			if (FabricLoader.getInstance().isModLoaded("jade")) {
				jadeCloseUps(context, singleplayer);
			}

			dimensionChangeKeepsEnergy(context, singleplayer);
			}

			// Persistence must not depend on how long this machine takes to render the reopened world.
			singleplayer.getServer().computeOnServer(server -> JjkConfig.server().energy.regenPercentPerSecond = 0);
			singleplayer.getServer().runCommand("jjk energy set @a 33");
			context.waitTicks(2);
			save = singleplayer.getWorldSave();
		}

		// Relog: reopen the same save and check the stored value survived.
		try (TestSingleplayerContext reopened = save.open()) {
			reopened.getConnection().waitForChunksRender();
			double after = reopened.getServer().computeOnServer(SmokeClientGameTest::firstPlayerEnergy);
			if (Math.abs(after - 33) > 1e-6) {
				throw new AssertionError("CE should persist across relog with regeneration paused, was " + after);
			}

			context.waitFor(mc -> ClientEnergy.hasData());
		}
		} finally {
			JjkConfig.server().energy.regenPercentPerSecond = originalRegen;
		}
	}

	private static void mouseAt(ClientGameTestContext context,int gx,int gy,int button){
		var pixel=context.computeOnClient(mc->new double[]{gx*mc.getWindow().getScreenWidth()/(double)mc.gui.screen().width,gy*mc.getWindow().getScreenHeight()/(double)mc.gui.screen().height});
		context.getInput().setCursorPos(pixel[0],pixel[1]);context.getInput().pressMouse(button);context.waitTicks(2);
	}
	private static void sandboxPowers(ClientGameTestContext context){
		TestWorldSave saved;
		try(var world=context.worldBuilder().create()){
			var server=world.getServer();world.getConnection().waitForChunksRender();server.runCommand("gamemode creative @a");server.runCommand("gamerule spawn_mobs false");
			server.computeOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();com.jjkcraft.awakening.CharacterAwakening.creative(p,JjkCraft.id("limitless"),false);com.jjkcraft.technique.TechniqueService.grant(p,JjkCraft.id("grudge_bolt"));com.jjkcraft.technique.TechniqueService.grant(p,JjkCraft.id("ten_shadows_divine_dogs"));return true;});
			context.waitFor(mc->ClientTechniques.known().contains(JjkCraft.id("grudge_bolt"))&&ClientProgress.get().grade().equals(JjkCraft.id("calamity")));
			context.waitTicks(40);
			if(!server.computeOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();return p.level().dimension()==net.minecraft.world.level.Level.OVERWORLD&&com.jjkcraft.modulo.mission.Missions.current(p).isEmpty()&&com.jjkcraft.modulo.Modulo.view(p).objective().isEmpty();}))throw new AssertionError("Calamity must stay in ordinary sandbox without a story");
			context.setScreen(com.jjkcraft.client.screen.LoadoutScreen::new);context.waitTicks(2);
			for(int slot=0;slot<4;slot++){
				final int n=slot;var pos=context.computeOnClient(mc->{var sc=mc.gui.screen();int w=Math.min(420,sc.width-16),h=Math.min(236,sc.height-16),x=(sc.width-w)/2,y=(sc.height-h)/2;return new int[]{x+w/2+5+(n%4)*32+14,y+24+18+(n/4)*29+13,x+8+60,y+24+43+12};});
				mouseAt(context,pos[0],pos[1],com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT);
				context.waitFor(mc->ClientTechniques.state().slot(n).isEmpty());
				String[] moves={"grudge_bolt","lapse_blue","infinity","reversal_red"};String key=moves[n];
				context.runOnClient(mc->{for(var child:mc.gui.screen().children())if(child instanceof net.minecraft.client.gui.components.EditBox box){box.setValue(key);break;}});
				context.waitTicks(2);mouseAt(context,pos[2],pos[3],com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT);mouseAt(context,pos[0],pos[1],com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT);
				context.waitFor(mc->ClientTechniques.state().slot(n).filter(JjkCraft.id(key)::equals).isPresent());
			}
			context.takeScreenshot("jjkcraft-sandbox-native-four-slot-rebind");
			for(int page=0;page<8;page++){
				final int n=page;var id=JjkCraft.id(page==5?"ten_shadows_divine_dogs":"grudge_bolt");context.setScreen(()->new com.jjkcraft.client.screen.TechniqueWorkshopScreen(id,n));context.waitTicks(2);context.takeScreenshot("jjkcraft-sandbox-expression-page-"+n);
			}
			context.setScreen(()->new com.jjkcraft.client.screen.TechniqueWorkshopScreen(JjkCraft.id("grudge_bolt"),3));
			context.runOnClient(mc->{for(var child:mc.gui.screen().children())if(child instanceof net.minecraft.client.gui.components.EditBox box){box.setValue("30");break;}});context.waitTicks(1);context.clickScreenButton("Apply tuning");
			context.waitFor(mc->com.jjkcraft.client.state.ClientPersonalization.get(JjkCraft.id("grudge_bolt")).details().expression().yaw()==30);
			context.setScreen(()->null);
			context.computeOnClient(mc->{net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(new com.jjkcraft.network.payload.PersonalizationPayloads.CreativeFamily(JjkCraft.id("modulo_harmony_chaos"),false));return true;});
			context.waitFor(mc->ClientTechniques.known().contains(JjkCraft.id("harmony_step")));context.setScreen(com.jjkcraft.client.screen.LoadoutScreen::new);context.waitTicks(2);context.takeScreenshot("jjkcraft-sandbox-modulo-powers");
			context.computeOnClient(mc->{net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(new com.jjkcraft.network.payload.PersonalizationPayloads.Tune(JjkCraft.id("harmony_step"),com.jjkcraft.technique.TechniqueTuning.DEFAULT.with(16,15).with(31,4).with(15,0x245BBD)));return true;});
			context.waitFor(mc->com.jjkcraft.client.state.ClientPersonalization.get(JjkCraft.id("harmony_step")).details().expression().dash()==4);context.setScreen(()->null);saved=world.getWorldSave();
		}
		try(var reopened=saved.open()){
			reopened.getConnection().waitForChunksRender();context.waitFor(mc->com.jjkcraft.client.state.ClientPersonalization.get(JjkCraft.id("harmony_step")).details().expression().dash()==4);
			if(!context.computeOnClient(mc->com.jjkcraft.client.state.ClientPersonalization.get(JjkCraft.id("harmony_step")).details().expression().yaw()==15&&com.jjkcraft.client.state.ClientPersonalization.get(JjkCraft.id("harmony_step")).details().color()==0x245BBD))throw new AssertionError("expanded profile and RGB color survive world reopen");
		}
	}
	private static void qualityOverhaul(ClientGameTestContext context){
		boolean modulo=JjkConfig.server().modulo.enabled;JjkConfig.server().modulo.enabled=false;
		try(var world=context.worldBuilder().create()){
			var server=world.getServer();world.getConnection().waitForChunksRender();server.runCommand("gamemode creative @a");server.runCommand("gamerule spawn_mobs false");server.runCommand("time set noon");
			server.computeOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();com.jjkcraft.awakening.CharacterAwakening.creative(p,JjkCraft.id("limitless"),false);return true;});
			context.waitFor(mc->ClientTechniques.known().contains(JjkCraft.id("hollow_purple")));
			context.setScreen(com.jjkcraft.client.screen.LoadoutScreen::new);context.waitTicks(2);context.takeScreenshot("jjkcraft-overhaul-loadout");
			context.clickScreenButton("Bindings");context.waitTicks(2);context.takeScreenshot("jjkcraft-overhaul-bindings");
			context.clickScreenButton(com.jjkcraft.client.JjkKeys.SKILLS[0].getTranslatedKeyMessage().getString());context.getInput().pressKey(com.mojang.blaze3d.platform.InputConstants.KEY_5);context.waitTicks(2);
			if(!context.computeOnClient(mc->com.jjkcraft.client.JjkKeys.SKILLS[0].saveString().equals("key.keyboard.5")))throw new AssertionError("native key binding edit must update mapping");
			context.clickScreenButton("Reset bindings");context.setScreen(()->new com.jjkcraft.client.screen.TechniqueWorkshopScreen(JjkCraft.id("hollow_purple")));
			context.runOnClient(mc->{for(var child:mc.gui.screen().children())if(child instanceof net.minecraft.client.gui.components.EditBox box){box.setValue("180");break;}});
			context.waitTicks(1);context.clickScreenButton("Apply tuning");context.waitFor(mc->com.jjkcraft.client.state.ClientPersonalization.get(JjkCraft.id("hollow_purple")).output()==180);
			context.waitTicks(2);context.takeScreenshot("jjkcraft-overhaul-workshop-saved");
			context.clickScreenButton("Core >");context.waitTicks(2);context.takeScreenshot("jjkcraft-overhaul-delivery");
			context.clickScreenButton("Delivery >");context.waitTicks(2);context.takeScreenshot("jjkcraft-overhaul-control");
			context.clickScreenButton("Control >");context.waitTicks(2);context.takeScreenshot("jjkcraft-overhaul-style");context.setScreen(()->null);
			server.computeOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();com.jjkcraft.technique.TechniqueService.grant(p,JjkCraft.id("ink_step"));com.jjkcraft.technique.TechniqueService.setLoadout(p,0,java.util.Optional.of(JjkCraft.id("ink_step")));p.getInventory().setSelectedSlot(8);return true;});
			context.waitFor(mc->ClientTechniques.state().slot(0).filter(JjkCraft.id("ink_step")::equals).isPresent());
			context.runOnClient(mc->mc.player.getInventory().setSelectedSlot(8));context.getInput().holdKey(com.jjkcraft.client.JjkKeys.SKILLS[0]);context.waitTicks(3);context.getInput().releaseKey(com.jjkcraft.client.JjkKeys.SKILLS[0]);context.waitTicks(4);
			if(!server.computeOnServer(s->com.jjkcraft.technique.TechniqueService.get(s.getPlayerList().getPlayers().getFirst()).masteryOf(JjkCraft.id("ink_step"))>0))throw new AssertionError("1 key must cast the equipped technique");
			if(!context.computeOnClient(mc->mc.player.getInventory().getSelectedSlot()==8))throw new AssertionError("skill binding must not also change the inventory hotbar");
			context.runOnClient(mc->mc.gui.hud.getChat().clearMessages(false));server.runCommand("execute as @p at @s run tp @s ~ ~ ~ 0 6");
			lineup(context,server,new String[]{"ash_oni","hollow_hymnal","salt_lantern"},8,3.5);context.takeScreenshot("jjkcraft-overhaul-curse-materials");server.runCommand("jjk curse clear 64");
			for(String id:new String[]{"malevolent_shrine","unlimited_void","idle_death_gamble","self_embodiment_of_perfection"}){
				server.runCommand("jjk domain expand @p jjkcraft:"+id);context.waitTicks(45);
				context.waitFor(mc->ClientDomains.own().isPresent());context.takeScreenshot("jjkcraft-overhaul-domain-"+id);closeOwn(context,server);
			}
			server.runCommand("execute as @p at @s rotated ~ 0 run summon minecraft:husk ^ ^ ^2 {NoAI:1b,PersistenceRequired:1b}");
			context.getInput().holdKeyFor(o->o.keyAttack,7);context.waitTicks(1);context.takeScreenshot("jjkcraft-overhaul-contact-impact");
			if(!server.computeOnServer(s->com.jjkcraft.sorcerer.Progression.get(s.getPlayerList().getPlayers().getFirst()).counter("combat_hits")>0))throw new AssertionError("native M1 input must land an authoritative hit");
			context.waitTicks(25);server.computeOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();return com.jjkcraft.combat.CursedCombat.start(p,JjkCraft.id("heavy"),p.getLookAngle());});
			context.computeOnClient(mc->{mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT);return true;});context.waitTicks(4);context.takeScreenshot("jjkcraft-overhaul-heavy-pose");
		}finally{JjkConfig.server().modulo.enabled=modulo;}
	}
	private static void deepSorcery(ClientGameTestContext context){
		boolean modulo=JjkConfig.server().modulo.enabled;JjkConfig.server().modulo.enabled=false;
		try{
		TestWorldSave save;
		try(var world=context.worldBuilder().create()){
			world.getConnection().waitForChunksRender();world.getServer().runCommand("gamemode creative @a");
			context.setScreen(com.jjkcraft.client.screen.CreativeTechniqueScreen::new);context.waitTicks(2);context.takeScreenshot("jjkcraft-deep-creative-picker");
			context.computeOnClient(mc->{net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(new com.jjkcraft.network.payload.PersonalizationPayloads.CreativeFamily(JjkCraft.id("limitless"),false));return true;});
			context.waitFor(mc->ClientProgress.get().grade().equals(JjkCraft.id("calamity"))&&ClientTechniques.known().contains(JjkCraft.id("hollow_purple")));
			context.setScreen(()->new com.jjkcraft.client.screen.TechniqueWorkshopScreen(JjkCraft.id("hollow_purple")));context.waitTicks(2);context.takeScreenshot("jjkcraft-deep-core");
			context.clickScreenButton("Core >");context.waitTicks(2);context.takeScreenshot("jjkcraft-deep-delivery");
			context.clickScreenButton("Delivery >");context.waitTicks(2);context.takeScreenshot("jjkcraft-deep-control");
			context.clickScreenButton("Control >");context.waitTicks(2);context.takeScreenshot("jjkcraft-deep-style");
			var tune=com.jjkcraft.technique.TechniqueTuning.DEFAULT.with(6,175).with(7,4).with(8,20).with(9,2).with(12,12).with(15,0xD055BB).named("Personal Purple");
			context.computeOnClient(mc->{net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(new com.jjkcraft.network.payload.PersonalizationPayloads.Tune(JjkCraft.id("hollow_purple"),tune));return true;});
			context.waitFor(mc->com.jjkcraft.client.state.ClientPersonalization.get(JjkCraft.id("hollow_purple")).details().title().equals("Personal Purple"));
			context.setScreen(com.jjkcraft.client.screen.PracticeScreen::new);context.waitTicks(2);context.takeScreenshot("jjkcraft-deep-regen-and-growth");
			context.setScreen(()->null);
			world.getServer().computeOnServer(server->{var p=server.getPlayerList().getPlayers().getFirst();com.jjkcraft.awakening.CharacterTraits.set(p,new com.jjkcraft.awakening.TraitState(true,java.util.List.of(JjkCraft.id("mechamaru_restriction")),"",java.util.List.of()));CursedEnergy.restore(p,500000);com.jjkcraft.sorcerer.SorcererLifecycle.fullSync(p);return true;});
			context.waitFor(mc->ClientEnergy.uncapped()&&ClientEnergy.predicted()>100000);
			context.setScreen(com.jjkcraft.client.screen.PracticeScreen::new);context.waitTicks(2);context.takeScreenshot("jjkcraft-deep-uncapped-bank");context.setScreen(()->null);save=world.getWorldSave();
		}
		try(var reopened=save.open()){
			reopened.getConnection().waitForChunksRender();context.waitFor(mc->ClientEnergy.uncapped()&&ClientEnergy.predicted()>100000);
			if(!reopened.getServer().computeOnServer(server->{var p=server.getPlayerList().getPlayers().getFirst();return com.jjkcraft.technique.Personalization.get(p,JjkCraft.id("hollow_purple")).details().title().equals("Personal Purple")&&com.jjkcraft.energy.SorceryPractice.get(p).regenLevel()>0;}))throw new AssertionError("deep tuning, uncapped bank and regeneration persist");
		}
		}finally{JjkConfig.server().modulo.enabled=modulo;}
	}
	private static void personalization(ClientGameTestContext context) {
		boolean modulo=JjkConfig.server().modulo.enabled;JjkConfig.server().modulo.enabled=false;
		try {
		TestWorldSave save;
		try(TestSingleplayerContext world=context.worldBuilder().create()){
			world.getConnection().waitForChunksRender();
			world.getServer().computeOnServer(server->{
				var p=server.getPlayerList().getPlayers().getFirst();com.jjkcraft.sorcerer.Awakening.reset(p);
				p.setAttached(com.jjkcraft.awakening.CharacterAwakening.RECORD,new com.jjkcraft.awakening.AwakeningRecord("inherit","jjkcraft:limitless","Gojo",true,0));
				com.jjkcraft.sorcerer.Awakening.awaken(p,false);
				com.jjkcraft.sorcerer.Progression.set(p,com.jjkcraft.sorcerer.Progression.get(p).withGrade(JjkCraft.id("special_grade"),10000));
				com.jjkcraft.energy.SorceryPractice.set(p,new com.jjkcraft.energy.PracticeState(3000,50,true,false,150,200,90));
				com.jjkcraft.reverse.ReverseTechnique.set(p,com.jjkcraft.reverse.ReverseTechnique.state(p).withUnlocked(true),false);
				CursedEnergy.fill(p);com.jjkcraft.sorcerer.SorcererLifecycle.fullSync(p);return true;
			});
			world.getServer().runCommand("jjk culling reset");
			context.waitFor(mc->com.jjkcraft.client.state.ClientPersonalization.practice().birth()==3000&&ClientTechniques.known().contains(JjkCraft.id("infinity")));
			context.setScreen(()->new com.jjkcraft.client.screen.TechniqueWorkshopScreen(JjkCraft.id("infinity")));context.waitTicks(4);
			context.takeScreenshot("jjkcraft-personal-infinity-workshop");
			var tuning=new com.jjkcraft.technique.TechniqueTuning(175,100,150,100,50,2);
			context.computeOnClient(mc->{net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(new com.jjkcraft.network.payload.PersonalizationPayloads.Tune(JjkCraft.id("infinity"),tuning));return true;});
			context.waitFor(mc->com.jjkcraft.client.state.ClientPersonalization.get(JjkCraft.id("infinity")).equals(tuning));
			context.setScreen(()->new com.jjkcraft.client.screen.TechniqueWorkshopScreen(JjkCraft.id("infinity")));context.waitTicks(3);
			context.takeScreenshot("jjkcraft-personal-infinity-customized");
			context.setScreen(com.jjkcraft.client.screen.PracticeScreen::new);context.waitTicks(3);context.takeScreenshot("jjkcraft-personal-ce-practice");
			context.setScreen(()->null);
			world.getServer().computeOnServer(server->{var p=server.getPlayerList().getPlayers().getFirst();return com.jjkcraft.energy.SorceryPractice.act(p,new com.jjkcraft.network.payload.PersonalizationPayloads.Practice(3,150,200,90));});
			context.waitTicks(12);
			if(!world.getServer().computeOnServer(server->com.jjkcraft.energy.SorceryPractice.reinforced(server.getPlayerList().getPlayers().getFirst())))throw new AssertionError("Learned reinforcement must consume CE and activate");
			context.computeOnClient(mc->{mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT);return true;});
			for(String move:new String[]{"jab","hook","backfist","uppercut","heavy","sweep"}){
				context.waitTicks(30);
				world.getServer().computeOnServer(server->{var p=server.getPlayerList().getPlayers().getFirst();CursedEnergy.fill(p);return com.jjkcraft.combat.CursedCombat.start(p,JjkCraft.id(move),p.getLookAngle());});
				context.waitTicks(3);
				if(!context.computeOnClient(mc->{
					var p=com.jjkcraft.client.combat.CombatAnimations.pose(mc.player.getId());
					if(p==null||!p.move().equals(move))return false;
					var model=new net.minecraft.client.model.player.PlayerModel(mc.getEntityModels().bakeLayer(net.minecraft.client.model.geom.ModelLayers.PLAYER),false);
					var state=new net.minecraft.client.renderer.entity.state.AvatarRenderState();state.id=mc.player.getId();state.ageInTicks=mc.player.tickCount+0.5f;
					model.setupAnim(state);return Math.abs(model.rightArm.xRot)>0.1||Math.abs(model.leftArm.xRot)>0.1;
				}))throw new AssertionError("Authoritative "+move+" must change full-body model pose");
				context.takeScreenshot("jjkcraft-personal-combat-"+move);
			}
			context.waitTicks(25);
			world.getServer().computeOnServer(server->{var p=server.getPlayerList().getPlayers().getFirst();return com.jjkcraft.technique.TechniqueService.cast(p,JjkCraft.id("lapse_blue"),0);});
			context.waitTicks(2);
			if(!context.computeOnClient(mc->{var p=com.jjkcraft.client.combat.CombatAnimations.pose(mc.player.getId());return p!=null&&p.move().equals("cast_area");}))throw new AssertionError("Field casts must animate even without a CAST VFX phase");
			context.takeScreenshot("jjkcraft-personal-casting-gesture");
			context.waitTicks(30);
			context.computeOnClient(mc->{mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);return true;});
			world.getServer().computeOnServer(server->{var p=server.getPlayerList().getPlayers().getFirst();return com.jjkcraft.combat.CursedCombat.start(p,JjkCraft.id("heavy"),p.getLookAngle());});
			context.waitTicks(4);context.takeScreenshot("jjkcraft-personal-first-person-combat");
			save=world.getWorldSave();
		}
		try(TestSingleplayerContext reopened=save.open()){
			reopened.getConnection().waitForChunksRender();
			context.waitFor(mc->com.jjkcraft.client.state.ClientPersonalization.practice().birth()==3000);
			boolean persisted=reopened.getServer().computeOnServer(server->{var p=server.getPlayerList().getPlayers().getFirst();
				return com.jjkcraft.energy.SorceryPractice.get(p).birth()==3000&&com.jjkcraft.energy.SorceryPractice.get(p).trained()==50
					&&com.jjkcraft.technique.Personalization.get(p,JjkCraft.id("infinity")).mode()==2;});
			if(!persisted)throw new AssertionError("Birth potential and personal technique tuning must persist across relog");
		}
		}finally{JjkConfig.server().modulo.enabled=modulo;}
	}

	private static void revision(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getConnection().waitForChunksRender();
			world.getServer().computeOnServer(server -> {
				var p=server.getPlayerList().getPlayers().getFirst();
				com.jjkcraft.sorcerer.Awakening.reset(p);
				p.setAttached(com.jjkcraft.awakening.CharacterAwakening.RECORD,
						new com.jjkcraft.awakening.AwakeningRecord("inherit","jjkcraft:blood_manipulation","Kamo",false,0));
				com.jjkcraft.sorcerer.Awakening.awaken(p,false);
				return true;
			});
			world.getServer().runCommand("jjk culling reset");
			context.waitFor(mc -> ClientTechniques.state().slot(0).filter(JjkCraft.id("crimson_binding")::equals).isPresent());
			context.setScreen(LoadoutScreen::new); context.waitTicks(5);
			context.takeScreenshot("jjkcraft-revision-blood-loadout");
			context.setScreen(() -> null);
			world.getServer().runCommand("spawnpoint @a 10 -60 10");
			world.getServer().runCommand("kill @a");
			context.waitFor(mc -> mc.gui.screen() instanceof com.jjkcraft.client.death.SoulScreen);
			context.waitTicks(2); context.takeScreenshot("jjkcraft-revision-respawn-choices");
			context.computeOnClient(mc -> { net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(new com.jjkcraft.network.payload.SoulPayloads.Action("respawn")); return true; });
			context.waitFor(mc -> mc.player != null && mc.player.isAlive() && com.jjkcraft.client.death.SoulClient.view().state().phase().equals("alive"));
			boolean atRespawn = world.getServer().computeOnServer(server -> {
				var p=server.getPlayerList().getPlayers().getFirst();
				return Math.abs(p.getX()-10.5)<3 && Math.abs(p.getZ()-10.5)<3
						&& com.jjkcraft.technique.TechniqueService.get(p).innate().filter(JjkCraft.id("crimson_binding")::equals).isPresent();
			});
			if (!atRespawn) throw new AssertionError("Normal respawn must use the configured vanilla respawn point and retain Blood Manipulation");
			context.takeScreenshot("jjkcraft-revision-normal-respawn");
			context.setScreen(com.jjkcraft.client.screen.TraitsScreen::new);context.waitTicks(3);context.takeScreenshot("jjkcraft-revision-traits");context.setScreen(()->null);
			context.waitTicks(65); // Vanilla respawn protection must expire before another lethal hit.
			world.getServer().runCommand("kill @a");
			context.waitFor(mc->mc.gui.screen() instanceof com.jjkcraft.client.death.SoulScreen);
			context.computeOnClient(mc->{net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(new com.jjkcraft.network.payload.SoulPayloads.Action("spirit"));return true;});
			context.waitFor(mc->com.jjkcraft.client.death.SoulClient.view().state().phase().equals("spirit") && !com.jjkcraft.client.state.ClientTraits.own().form().isEmpty());
			context.computeOnClient(mc->{mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);return true;});
			context.waitTicks(10);context.takeScreenshot("jjkcraft-revision-cursed-spirit");
			context.waitTicks(65);
			world.getServer().runCommand("kill @a");
			context.waitFor(mc->mc.player!=null && mc.player.isAlive() && com.jjkcraft.client.death.SoulClient.view().state().phase().equals("alive")
					&& !com.jjkcraft.client.state.ClientAwakening.view().record().family().equals("jjkcraft:blood_manipulation"));
			context.takeScreenshot("jjkcraft-revision-spirit-rebirth");
			context.computeOnClient(mc->{mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);return true;});
		}
	}

	private static void energyHud(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
		context.waitFor(mc -> ClientEnergy.hasData());
		double max = context.computeOnClient(mc -> ClientEnergy.max());
		if (Math.abs(max - 100) > 1e-6) {
			throw new AssertionError("client should see 100 max CE at Grade 4, saw " + max);
		}

		singleplayer.getServer().runCommand("jjk energy set @a 62");
		context.waitTicks(30);
		double predicted = context.computeOnClient(mc -> ClientEnergy.predicted());
		if (predicted < 62 || predicted > 70) {
			throw new AssertionError("client prediction should be just above 62 after 1.5 s, was " + predicted);
		}

		context.takeScreenshot("jjkcraft-m1-energy-bar");

		singleplayer.getServer().runCommand("jjk energy spend @a 58");
		context.waitTicks(4);
		if (!context.computeOnClient(mc -> ClientEnergy.overexerted())) {
			throw new AssertionError("client should show overexertion after spending below 10%");
		}

		context.takeScreenshot("jjkcraft-m1-overexerted");
	}

	private static void screensAndCeremony(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
		singleplayer.getServer().runCommand("jjk xp add @a 450");
		context.waitTicks(5);
		context.setScreen(ProfileScreen::new);
		context.waitTicks(10);
		context.takeScreenshot("jjkcraft-m2-profile");
		context.setScreen(GrowthTreeScreen::new);
		context.waitTicks(10);
		context.takeScreenshot("jjkcraft-m2-growth");
		context.setScreen(() -> null);

		singleplayer.getServer().runCommand("jjk grade set @a jjkcraft:grade_3 true");
		context.waitTicks(30);
		if (!context.computeOnClient(mc -> PromotionCeremony.active())) {
			throw new AssertionError("promotion ceremony should be playing");
		}

		context.takeScreenshot("jjkcraft-m2-promotion");
		context.waitFor(mc -> !PromotionCeremony.active(), 200);
		String grade = context.computeOnClient(mc -> ClientProgress.get().grade().toString());
		if (!grade.equals("jjkcraft:grade_3")) {
			throw new AssertionError("client should see Grade 3, saw " + grade);
		}

		context.takeScreenshot("jjkcraft-m2-badge");
		singleplayer.getServer().runCommand("jjk grade set @a jjkcraft:grade_4");
		context.waitTicks(5);
	}

	private static void techniques(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
		TestServerContext server = singleplayer.getServer();
		server.runCommand("jjk grade set @a jjkcraft:grade_1");
		server.runCommand("jjk energy fill @a");
		for (String id : new String[] {"cleave", "grudge_bolt", "grave_chains", "lapse_blue", "reversal_red", "lacquer_ward"}) {
			server.runCommand("jjk technique grant @a jjkcraft:" + id);
		}

		server.runCommand("execute as @p at @s run tp @s ~ ~ ~ 0 10");
		server.runCommand("execute as @p at @s run summon minecraft:zombie ^ ^ ^5 {NoAI:1b,PersistenceRequired:1b}");
		context.waitTicks(10);
		if (context.computeOnClient(mc -> ClientTechniques.known().size()) < 6) {
			throw new AssertionError("client should know the granted techniques");
		}

		context.takeScreenshot("jjkcraft-m3-hotbar");

		cast(context, server, "cleave", 3);
		context.takeScreenshot("jjkcraft-m3-slash");
		cast(context, server, "grudge_bolt", 2);
		context.takeScreenshot("jjkcraft-m3-bolt");
		cast(context, server, "grave_chains", 6);
		context.takeScreenshot("jjkcraft-m3-chains");
		server.runCommand("execute as @p run jjk technique cast @s jjkcraft:lapse_blue");
		context.waitTicks(10);
		context.takeScreenshot("jjkcraft-m3-field");
		cast(context, server, "reversal_red", 2);
		context.takeScreenshot("jjkcraft-m3-burst");

		context.setScreen(TechniqueWheelScreen::new);
		context.waitTicks(10);
		context.takeScreenshot("jjkcraft-m3-wheel");
		context.setScreen(LoadoutScreen::new);
		context.waitTicks(5);
		context.takeScreenshot("jjkcraft-m3-loadout");
		context.setScreen(() -> null);

		server.runCommand("kill @e[type=minecraft:zombie]");
		server.runCommand("jjk grade set @a jjkcraft:grade_4");
		context.waitTicks(5);
	}

	private static void curses(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
		TestServerContext server = singleplayer.getServer();
		server.runCommand("gamerule spawn_mobs false");
		server.runCommand("time set noon");
		server.runCommand("gamemode creative @a");
		server.runCommand("execute as @p at @s run tp @s ~ ~ ~ 0 6");
		context.waitTicks(5);

		lineup(context, server, new String[] {"hollow_hymnal", "ledger_crawler", "wick_moth", "pale_stilt", "echo_mimic", "salt_lantern"}, 6, 2.4);
		context.takeScreenshot("jjkcraft-m4-lesser-curses");
		server.runCommand("jjk curse clear 64");
		lineup(context, server, new String[] {"tidewash", "thorn_matriarch", "cracked_bell", "redacted_saint", "ash_oni", "gravebound_gate"}, 11, 3.6);
		context.takeScreenshot("jjkcraft-m4-greater-curses");
		server.runCommand("jjk curse clear 64");
		context.waitTicks(5);

		// The Redacted Saint arrives disguised.
		server.runCommand("execute as @p at @s rotated ~ 0 run jjk curse spawn jjkcraft:redacted_saint ^ ^ ^4");
		context.waitTicks(40);
		context.takeScreenshot("jjkcraft-m4-saint-disguised");
		// Look-at overlays (Jade) are told the same lie: the player whose face it wears.
		expectDisguise(context, "redacted_saint", "the player");

		// Sensing: presences glow through a wall and show on the radar.
		server.runCommand("execute as @p at @s rotated ~ 0 run fill ^-3 ^ ^6 ^3 ^3 ^6 minecraft:stone_bricks");
		server.runCommand("execute as @p at @s rotated ~ 0 run jjk curse spawn jjkcraft:ledger_crawler ^1 ^ ^8");
		server.runCommand("execute as @p at @s rotated ~ 0 run jjk curse spawn jjkcraft:wick_moth ^-2 ^1 ^9");
		server.runCommand("jjk sense @a on");
		context.waitTicks(40);
		if (!context.computeOnClient(mc -> ClientSensing.isActive() && ClientSensing.sensed().size() >= 3)) {
			throw new AssertionError("sensing should report the three curses, got " + context.computeOnClient(mc -> ClientSensing.sensed().size()));
		}

		context.takeScreenshot("jjkcraft-m4-sensing");
		// Sensing sees through the disguise, in the overlay too.
		expectDisguise(context, "redacted_saint", "itself");
		server.runCommand("jjk sense @a off");
		context.waitTicks(5);
		if (context.computeOnClient(mc -> ClientSensing.isActive())) {
			throw new AssertionError("sensing should turn off");
		}

		// Codex: three Hymnal kills unlock its full entry; one Saint kill shows its name only.
		server.runCommand("jjk curse clear 64");
		for (int i = 0; i < 3; i++) {
			server.runCommand("execute as @p at @s rotated ~ 0 run jjk curse spawn jjkcraft:hollow_hymnal ^ ^ ^3");
			server.runCommand("damage @e[type=jjkcraft:hollow_hymnal,limit=1] 1000 minecraft:player_attack by @p");
			context.waitTicks(2);
		}

		server.runCommand("execute as @p at @s rotated ~ 0 run jjk curse spawn jjkcraft:redacted_saint ^ ^ ^3");
		server.runCommand("damage @e[type=jjkcraft:redacted_saint,limit=1] 1000 minecraft:player_attack by @p");
		context.waitTicks(40);
		int hymnals = context.computeOnClient(mc -> ClientProgress.get().counter(Curses.codexKey(JjkCraft.id("hollow_hymnal"))));
		int saints = context.computeOnClient(mc -> ClientProgress.get().counter(Curses.codexKey(JjkCraft.id("redacted_saint"))));
		if (hymnals != 3 || saints != 1) {
			throw new AssertionError("codex counters should be 3 hymnals and 1 saint, got " + hymnals + " and " + saints);
		}

		context.setScreen(CodexScreen::new);
		context.waitTicks(10);
		context.takeScreenshot("jjkcraft-m4-codex");
		context.setScreen(() -> null);

		server.runCommand("jjk curse clear 64");
		server.runCommand("execute as @p at @s rotated ~ 0 run fill ^-3 ^ ^6 ^3 ^3 ^6 minecraft:air");
		server.runCommand("kill @e[type=minecraft:item]");
		server.runCommand("gamemode survival @a");
		server.runCommand("gamerule spawn_mobs true");
		context.waitTicks(5);
	}

	/** Spawns a row of curses facing the player, waits out their emergence, then freezes them in place. */
	private static void lineup(ClientGameTestContext context, TestServerContext server, String[] curses, double distance, double spacing) {
		for (int i = 0; i < curses.length; i++) {
			double x = (i - (curses.length - 1) / 2.0) * spacing;
			server.runCommand(String.format(java.util.Locale.ROOT, "execute as @p at @s rotated ~ 0 run jjk curse spawn jjkcraft:%s ^%.1f ^ ^%.1f", curses[i], x, distance));
		}

		context.waitTicks(40);
		server.runCommand("execute as @e[type=!minecraft:player,distance=..40] run data merge entity @s {NoAI:1b}");
		server.runCommand("execute as @e[type=!minecraft:player,distance=..40] at @s facing entity @p feet run tp @s ~ ~ ~ ~ 0");
		for (String curse : curses) {
			server.runCommand("data merge entity @e[type=jjkcraft:" + curse + ",limit=1] {CustomName:\"" + curse + "\",CustomNameVisible:1b}");
		}

		context.waitTicks(10);
	}

	private static void sorcery(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
		TestServerContext server = singleplayer.getServer();
		server.runCommand("gamerule spawn_mobs false");
		server.runCommand("gamerule mob_griefing false");
		server.runCommand("time set noon");
		server.runCommand("gamemode survival @a");
		server.runCommand("jjk grade set @a jjkcraft:grade_1");
		server.runCommand("jjk energy fill @a");
		server.runCommand("jjk technique grant @a jjkcraft:ink_iron_palm");
		server.runCommand("execute as @p at @s run tp @s ~ ~ ~ 0 8");
		server.runCommand("execute as @p at @s rotated ~ 0 run summon minecraft:husk ^ ^ ^2.4 {NoAI:1b,PersistenceRequired:1b}");
		context.waitTicks(5);

		// Black Flash: forced, so the shot is certain.
		server.runCommand("jjk blackflash force @p");
		server.runCommand("execute as @p run jjk technique cast @s jjkcraft:ink_iron_palm");
		context.waitTicks(2);
		context.takeScreenshot("jjkcraft-m6-black-flash");
		if (!context.computeOnClient(mc -> ClientSorcery.inZone())) {
			throw new AssertionError("a Black Flash should put the player in the zone");
		}

		context.waitTicks(20);
		server.runCommand("kill @e[type=minecraft:husk]");

		// The Vow Builder, driven like a player would: pick two vows, a focus, look, swear.
		context.setScreen(VowBuilderScreen::new);
		context.waitTicks(5);
		context.clickScreenButton("Grounded Oath +3.0");
		context.clickScreenButton("Measured Pace +2.0");
		context.clickScreenButton("vow_focus.jjkcraft.reserve");
		context.waitTicks(3);
		context.takeScreenshot("jjkcraft-m6-vow-builder");
		context.clickScreenButton("screen.jjkcraft.vows.swear");
		context.waitTicks(10);
		if (!context.computeOnClient(mc -> ClientSorcery.vow().active())) {
			throw new AssertionError("swearing from the Vow Builder should bind the vow");
		}

		context.takeScreenshot("jjkcraft-m6-vow-held");
		context.setScreen(() -> null);

		// Reverse Cursed Technique healing (granted by command; the awakening itself is server-tested).
		server.runCommand("jjk reverse grant @a");
		server.runCommand("damage @p 13 minecraft:generic");
		context.waitTicks(30);
		if (!context.computeOnClient(mc -> ClientSorcery.reverse().unlocked())) {
			throw new AssertionError("the client should know Reverse Cursed Technique is unlocked");
		}

		context.takeScreenshot("jjkcraft-m6-reverse-healing");
		context.setScreen(ProfileScreen::new);
		context.waitTicks(5);
		context.takeScreenshot("jjkcraft-m6-profile");
		context.setScreen(() -> null);

		// The new curses, and the Redacted Saint wearing the player's face.
		server.runCommand("gamemode creative @a");
		lineup(context, server, new String[] {"vow_kin", "paper_tide_gravemind", "unfinished_ferryman", "culling_warden", "ten_mouth_sovereign"}, 13, 5.0);
		context.takeScreenshot("jjkcraft-m6-new-curses");
		server.runCommand("jjk curse clear 64");
		server.runCommand("execute as @p at @s rotated ~ 0 run jjk curse spawn jjkcraft:redacted_saint ^ ^ ^3");
		context.waitTicks(40);
		context.takeScreenshot("jjkcraft-m6-saint-face");

		server.runCommand("jjk curse clear 64");
		server.runCommand("jjk vow clear @a");
		server.runCommand("jjk reverse revoke @a");
		server.runCommand("effect clear @a");
		server.runCommand("jjk grade set @a jjkcraft:grade_4");
		server.runCommand("gamemode survival @a");
		server.runCommand("gamerule spawn_mobs true");
		server.runCommand("gamerule mob_griefing true");
		context.waitTicks(10);
	}

	private static void tpGround(TestServerContext server, int x, int z, BlockPos facing) {
		server.runCommand(String.format(java.util.Locale.ROOT, "execute positioned %d 0 %d positioned over motion_blocking run tp @p ~ ~ ~ facing %d %d %d",
				x, z, facing.getX(), facing.getY(), facing.getZ()));
	}

	private static void culling(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
		TestServerContext server = singleplayer.getServer();
		server.runCommand("gamerule spawn_mobs false");
		server.runCommand("time set noon");
		server.runCommand("gamemode survival @a");
		server.runCommand("jjk culling reset");
		server.runCommand("execute as @p at @s run jjk culling start 2 24 60");
		context.waitFor(mc -> ClientCulling.running() && ClientCulling.view().colonies().size() == 2);
		BlockPos c = context.computeOnClient(mc -> ClientCulling.view().colonies().getFirst().center());
		context.runOnClient(mc -> mc.gui.hud.getChat().clearMessages(false));

		// From outside: the curtain wall and dome.
		tpGround(server, c.getX() + 40, c.getZ() + 8, c.above(14));
		singleplayer.getConnection().waitForChunksRender();
		context.waitTicks(20);
		context.takeScreenshot("jjkcraft-m7-barrier-outside");

		// Lift the frame caps so the frame counter measures drawing: vanilla holds a game at 30 fps after
		// a minute without input, and the test sends none.
		context.runOnClient(mc -> {
			mc.options.inactivityFpsLimit().set(net.minecraft.client.InactivityFpsLimit.MINIMIZED);
			mc.options.enableVsync().set(false);
			mc.options.framerateLimit().set(260);
		});

		// Step inside: a participant now.
		tpGround(server, c.getX() + 12, c.getZ(), c.above(2));
		context.waitFor(mc -> ClientCulling.me().participant());
		server.runCommand("gamemode creative @a");
		context.waitTicks(60);
		server.runCommand("execute as @e[type=jjkcraft:entrant] run data merge entity @s {NoAI:1b}");
		context.runOnClient(mc -> mc.gui.hud.getChat().clearMessages(false));
		tpGround(server, c.getX() + 9, c.getZ() + 3, c.above(2));
		singleplayer.getConnection().waitForChunksRender();
		context.waitTicks(10);
		context.takeScreenshot("jjkcraft-m7-inside");
		// The counter covers the last second: give it two seconds of standing in the scene.
		context.waitTicks(40);
		int colonyFps = context.computeOnClient(mc -> mc.getFps());
		JjkCraft.LOGGER.info("Performance: {} fps inside a colony barrier with entrants and the Kogane", colonyFps);

		// The map and its pages.
		server.runCommand("jjk culling points @p 120");
		context.waitTicks(5);
		context.setScreen(() -> CullingScreen.at(CullingScreen.Page.MAP));
		context.waitTicks(5);
		context.takeScreenshot("jjkcraft-m7-map");
		context.setScreen(() -> CullingScreen.at(CullingScreen.Page.BOARD));
		context.waitTicks(3);
		context.takeScreenshot("jjkcraft-m7-scoreboard");
		context.setScreen(() -> CullingScreen.at(CullingScreen.Page.RULES));
		context.waitTicks(3);
		context.takeScreenshot("jjkcraft-m7-rules");
		context.setScreen(() -> null);

		// Negotiate beside the Kogane: propose a rule and pay for it, like a player would.
		tpGround(server, c.getX() + 2, c.getZ() + 1, c.above(2));
		context.waitFor(mc -> ClientCulling.me().nearMaster());
		context.setScreen(() -> {
			CullingScreen screen = CullingScreen.at(CullingScreen.Page.NEGOTIATE);
			screen.propose(JjkCraft.id("night_hunt"));
			return screen;
		});
		context.waitTicks(5);
		context.takeScreenshot("jjkcraft-m7-negotiate");
		context.clickScreenButton("screen.jjkcraft.culling.propose");
		context.waitFor(mc -> ClientCulling.ruleActive(JjkCraft.id("night_hunt")));
		if (context.computeOnClient(mc -> ClientCulling.me().points()) != 20) {
			throw new AssertionError("the rule should have cost 100 of 120 points");
		}

		context.setScreen(() -> null);
		context.runOnClient(mc -> mc.gui.hud.getChat().clearMessages(false));
		tpGround(server, c.getX() + 3, c.getZ() + 2, c.above(2));
		context.waitTicks(10);
		context.takeScreenshot("jjkcraft-m7-kogane");

		server.runCommand("jjk culling reset");
		server.runCommand("gamemode survival @a");
		server.runCommand("gamerule spawn_mobs true");
		context.waitTicks(10);
	}

	private static void arsenal(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
		TestServerContext server = singleplayer.getServer();
		server.runCommand("gamerule spawn_mobs false");
		server.runCommand("gamerule mob_griefing false");
		server.runCommand("time set noon");
		server.runCommand("gamemode creative @a");
		server.runCommand("jjk grade set @a jjkcraft:special_grade");
		context.runOnClient(mc -> mc.gui.hud.getChat().clearMessages(false));
		server.runCommand("execute as @p at @s run tp @s ~ ~ ~ 0 8");

		// The last three curses of the roster (the Mimic waits as a chest).
		lineup(context, server, new String[] {"doorframe_hunter", "gilded_mimic", "tithe_burrower"}, 6, 3.0);
		context.takeScreenshot("jjkcraft-m8-new-curses");
		expectDisguise(context, "gilded_mimic", "minecraft:chest");
		server.runCommand("execute as @p at @s run jjk curse clear 32");

		// Three shikigami.
		for (String technique : new String[] {"ten_shadows_divine_dogs", "ten_shadows_nue", "ten_shadows_toad", "splinter_volley", "maximum_output_blue", "thousand_lances",
				"piercing_blood", "ink_step", "idle_transfiguration"}) {
			server.runCommand("jjk technique grant @a jjkcraft:" + technique);
		}

		server.runCommand("jjk energy fill @a");
		server.runCommand("execute as @p run jjk technique cast @s jjkcraft:ten_shadows_divine_dogs");
		server.runCommand("execute as @p run jjk technique cast @s jjkcraft:ten_shadows_nue");
		server.runCommand("execute as @p run jjk technique cast @s jjkcraft:ten_shadows_toad");
		context.waitTicks(10);
		// Step back so the summons, which keep to their owner, come into view.
		server.runCommand("execute as @p at @s run tp @s ~ ~ ~-5 0 18");
		context.waitTicks(12);
		context.takeScreenshot("jjkcraft-m8-shikigami");
		server.runCommand("execute as @p at @s run tp @s ~ ~ ~ 0 8");
		server.runCommand("kill @e[type=jjkcraft:divine_dog]");
		server.runCommand("kill @e[type=jjkcraft:nue]");
		server.runCommand("kill @e[type=jjkcraft:toad]");

		// A volley (its bolts carry the technique's colour) and the ultimates against a row of husks.
		server.runCommand("jjk energy fill @a");
		server.runCommand("execute as @p run jjk technique cast @s jjkcraft:splinter_volley");
		context.waitTicks(6);
		context.takeScreenshot("jjkcraft-m8-splinter-volley");
		for (int i = -1; i <= 1; i++) {
			server.runCommand(String.format(java.util.Locale.ROOT,
					"execute as @p at @s rotated ~ 0 run summon minecraft:husk ^%d ^ ^12 {PersistenceRequired:1b}", i * 2));
		}

		context.waitTicks(5);
		server.runCommand("jjk energy fill @a");
		server.runCommand("execute as @p run jjk technique cast @s jjkcraft:maximum_output_blue");
		context.waitTicks(25);
		context.takeScreenshot("jjkcraft-m8-maximum-output-blue");
		context.waitTicks(40);
		server.runCommand("kill @e[type=minecraft:husk]");
		for (int i = -1; i <= 1; i++) {
			server.runCommand(String.format(java.util.Locale.ROOT,
					"execute as @p at @s rotated ~ 0 run summon minecraft:husk ^%d ^ ^10 {PersistenceRequired:1b,NoAI:1b}", i * 2));
		}

		server.runCommand("jjk energy fill @a");
		server.runCommand("jjk technique cooldowns @a");
		server.runCommand("execute as @p at @s run tp @s ~ ~ ~ 0 18");
		server.runCommand("execute as @p run jjk technique cast @s jjkcraft:thousand_lances");
		context.waitTicks(20);
		context.takeScreenshot("jjkcraft-m8-thousand-lances");
		int lanceFps = context.computeOnClient(mc -> mc.getFps());
		JjkCraft.LOGGER.info("Performance: {} fps during Thousand Lances", lanceFps);
		context.waitTicks(50);
		server.runCommand("execute as @p at @s run tp @s ~ ~ ~ 0 5");
		server.runCommand("jjk energy fill @a");
		server.runCommand("execute as @p run jjk technique cast @s jjkcraft:piercing_blood");
		context.waitTicks(32);
		context.takeScreenshot("jjkcraft-m8-piercing-blood");
		server.runCommand("kill @e[type=minecraft:husk]");

		// The cursed tools and objects in the hotbar.
		server.runCommand("clear @a");
		int slot = 0;
		for (String item : new String[] {"split_soul_katana", "chain_of_a_thousand_miles", "inverted_spear_of_heaven", "debt_cleaver", "bonewood_staff",
				"withered_digit", "drifting_gourd", "tally_ledger"}) {
			server.runCommand("item replace entity @p hotbar." + slot++ + " with jjkcraft:" + item);
		}

		context.waitTicks(5);
		context.takeScreenshot("jjkcraft-m8-tools");
		server.runCommand("clear @a");
		server.runCommand("jjk grade set @a jjkcraft:grade_4");
		server.runCommand("gamemode survival @a");
		server.runCommand("gamerule spawn_mobs true");
		server.runCommand("gamerule mob_griefing true");
		context.waitTicks(10);
	}

	/**
	 * The Modulo expansion (docs/MODULO.md): the Naunax over the overworld during a Descent, the
	 * ship's arrival hall with the mission tracker, Mul meter and dialogue, Tokyo 2086 with its
	 * characters and bosses, Mahoraga's wheel, the branch choice and the epilogue.
	 */
	private static void modulo(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
		if(!com.jjkcraft.modulo.Modulo.storyEnabled()){
			if(!singleplayer.getServer().computeOnServer(s->com.jjkcraft.modulo.Modulo.view(s.getPlayerList().getPlayers().getFirst()).objective().isEmpty()))throw new AssertionError("sandbox must have no mission tracker");
			context.takeScreenshot("jjkcraft-modulo-sandbox");return;
		}
		TestServerContext server = singleplayer.getServer();
		server.runOnServer(s -> {
			JjkConfig.server().modulo.enabled = true;
			Modulo.state(s).nextDescent = Long.MAX_VALUE / 4;
		});
		server.runCommand("gamerule spawn_mobs false");
		server.runCommand("time set noon");
		server.runCommand("gamemode creative @a");
		server.runCommand("jjk grade set @a jjkcraft:grade_1");
		server.runCommand("clear @a");
		context.runOnClient(mc -> mc.gui.hud.getChat().clearMessages(false));

		// The Descent: the ship hangs over the ramp's foot, 160 blocks ahead.
		server.runCommand("execute as @p at @s rotated ~ 0 run jjk modulo descent start ^ ^ ^160");
		server.runCommand("execute as @p at @s run tp @s ~ ~ ~ ~ -28");
		context.waitTicks(40);
		context.takeScreenshot("jjkcraft-modulo-descent");
		int shipFps = context.computeOnClient(mc -> mc.getFps());
		JjkCraft.LOGGER.info("Performance: {} fps under the Naunax during a Descent", shipFps);

		// Aboard: the arrival hall, the first mission's tracker and its opening line.
		server.runCommand("jjk modulo board @p");
		context.waitTicks(10);
		singleplayer.getConnection().waitForChunksRender();
		server.runCommand("execute as @p at @s run tp @s ~ ~ ~ 180 0");
		server.runCommand("jjk modulo mul 120 @p");
		context.waitTicks(30);
		context.takeScreenshot("jjkcraft-modulo-naunax");
		int deckFps = context.computeOnClient(mc -> mc.getFps());
		JjkCraft.LOGGER.info("Performance: {} fps in the Naunax arrival hall", deckFps);

		// Tokyo 2086: the school plaza, its people in a row, and two bosses.
		server.runCommand("jjk modulo tokyo @p");
		context.waitTicks(10);
		singleplayer.getConnection().waitForChunksRender();
		server.runCommand("execute as @p at @s run tp @s ~ ~ ~ 180 10");
		context.waitTicks(20);
		context.takeScreenshot("jjkcraft-modulo-tokyo");
		server.runOnServer(s -> {
			ServerPlayer player = s.getPlayerList().getPlayers().getFirst();
			String[] cast = {"maru", "cross", "yuka", "tsurugi", "usami", "mino", "kyoko_tomoe", "osada", "masayoshi", "jabaloma"};
			for (int i = 0; i < cast.length; i++) {
				ModuloEntities.spawnNpc(player.level(), cast[i], ModuloNpcEntity.Role.SITE, null, null, player.blockPosition().offset(-9 + i * 2, 0, -6));
			}
		});
		context.waitTicks(10);
		context.takeScreenshot("jjkcraft-modulo-characters");
		server.runCommand("kill @e[type=jjkcraft:modulo_npc]");
		server.runOnServer(s -> {
			ServerPlayer player = s.getPlayerList().getPlayers().getFirst();
			ModuloEntities.spawnBoss(player.level(), Identifier.fromNamespaceAndPath("modulo", "toranosuke"), player.blockPosition().offset(-4, 0, -8), null, null, 0, 1);
			ModuloEntities.spawnBoss(player.level(), Identifier.fromNamespaceAndPath("modulo", "dabura"), player.blockPosition().offset(4, 0, -9), null, null, 0, 1);
		});
		server.runCommand("execute as @e[type=jjkcraft:modulo_boss] run data merge entity @s {NoAI:1b}");
		server.runCommand("execute as @e[type=jjkcraft:toranosuke] run data merge entity @s {NoAI:1b}");
		context.waitTicks(20);
		context.takeScreenshot("jjkcraft-modulo-bosses");
		server.runCommand("kill @e[type=jjkcraft:modulo_boss]");
		server.runCommand("kill @e[type=jjkcraft:toranosuke]");

		// Mahoraga, after three hits of Light: its wheel has turned three times.
		server.runOnServer(s -> {
			ServerPlayer player = s.getPlayerList().getPlayers().getFirst();
			MahoragaEntity mahoraga = ModuloEntities.MAHORAGA.create(player.level(), net.minecraft.world.entity.EntitySpawnReason.EVENT);
			BlockPos at = player.blockPosition().offset(0, 0, -13);
			mahoraga.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
			mahoraga.setup(player.level());
			mahoraga.setNoAi(true);
			player.level().addFreshEntity(mahoraga);
			for (int i = 0; i < 3; i++) {
				mahoraga.damageCooldownTime = 0;
				mahoraga.hurtServer(player.level(), player.level().damageSources().source(JjkDamageTypes.LIGHT), 4);
			}
		});
		server.runCommand("execute as @p at @s run tp @s ~ ~ ~ 180 4");
		context.waitTicks(12);
		context.takeScreenshot("jjkcraft-modulo-mahoraga");
		server.runCommand("execute as @p at @s run tp @s ~ ~ ~ 180 10");
		server.runCommand("kill @e[type=jjkcraft:mahoraga]");

		// The branch in Chaos and Harmony: Tsurugi asks, the choice screen opens.
		server.runOnServer(s -> {
			ServerPlayer player = s.getPlayerList().getPlayers().getFirst();
			Missions.jumpTo(player, Identifier.fromNamespaceAndPath("modulo", "chaos_and_harmony"), 1);
			ModuloNpcEntity tsurugi = ModuloEntities.spawnNpc(player.level(), "tsurugi", ModuloNpcEntity.Role.SITE, null, null, player.blockPosition().offset(0, 0, -3));
			Missions.onTalk(player, tsurugi);
		});
		context.waitTicks(10);
		context.takeScreenshot("jjkcraft-modulo-choice");
		context.setScreen(() -> null);
		server.runCommand("kill @e[type=jjkcraft:modulo_npc]");

		// The epilogue, a few lines in.
		server.runOnServer(s -> Missions.jumpTo(s.getPlayerList().getPlayers().getFirst(), Identifier.fromNamespaceAndPath("modulo", "a_bright_future"), 1));
		context.waitTicks(150);
		context.takeScreenshot("jjkcraft-modulo-epilogue");

		context.runOnClient(mc -> ClientModulo.reset());
		server.runCommand("jjk modulo leave @p");
		context.waitTicks(10);
		server.runCommand("jjk modulo mission reset @p");
		server.runCommand("jjk modulo descent stop");
		server.runCommand("jjk modulo mul 0 @p");
		server.runOnServer(s -> JjkConfig.server().modulo.enabled = false);
		server.runCommand("clear @a");
		server.runCommand("jjk grade set @a jjkcraft:grade_4");
		server.runCommand("gamemode survival @a");
		server.runCommand("gamerule spawn_mobs true");
		singleplayer.getConnection().waitForChunksRender();
		context.waitTicks(10);
	}

	/**
	 * Compatibility runs with Jade only (build.gradle, -PwithCompat): Jade's tooltip on the two
	 * disguised curses, up close and beside the domain meter. Creative mode keeps both disguised.
	 */
	private static void jadeCloseUps(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
		TestServerContext server = singleplayer.getServer();
		server.runCommand("gamerule spawn_mobs false");
		server.runCommand("time set noon");
		server.runCommand("gamemode creative @a");
		server.runCommand("jjk grade set @a jjkcraft:special_grade");
		context.runOnClient(mc -> mc.gui.hud.getChat().clearMessages(false));
		server.runCommand("execute as @p at @s run tp @s ~ ~ ~ 0 20");
		for (String curse : new String[] {"redacted_saint", "gilded_mimic"}) {
			server.runCommand("execute as @p at @s rotated ~ 0 run jjk curse spawn jjkcraft:" + curse + " ^ ^ ^2.5");
			context.waitTicks(40);
			context.takeScreenshot("jjkcraft-compat-jade-" + curse);
			server.runCommand("execute as @p at @s run jjk curse clear 16");
			context.waitTicks(5);
		}

		server.runCommand("jjk grade set @a jjkcraft:grade_4");
		server.runCommand("gamemode survival @a");
		server.runCommand("gamerule spawn_mobs true");
		context.waitTicks(10);
	}

	/** Checks what a look-at overlay is told about the first curse of this kind the client sees. */
	private static void expectDisguise(ClientGameTestContext context, String curse, String expected) {
		String shown = context.computeOnClient(mc -> {
			for (Entity entity : mc.level.entitiesForRendering()) {
				if (entity instanceof CurseEntity c && c.curseId().equals(JjkCraft.id(curse))) {
					return switch (DisguiseView.of(c)) {
						case DisguiseView.Real real -> "itself";
						case DisguiseView.AsPlayer(var face) -> face == mc.player ? "the player" : "another player";
						case DisguiseView.AsBlock(var state) -> BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
						case DisguiseView.Nothing nothing -> "nothing";
					};
				}
			}

			return "no " + curse + " in view";
		});
		if (!shown.equals(expected)) {
			throw new AssertionError("an overlay should show the " + curse + " as " + expected + ", got " + shown);
		}
	}

	private static void domains(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
		TestServerContext server = singleplayer.getServer();
		server.runCommand("gamerule spawn_mobs false");
		// The Gravebound Gate walls off its clearing when it sees a player; keep the view open.
		server.runCommand("gamerule mob_griefing false");
		server.runCommand("time set noon");
		server.runCommand("gamemode survival @a");
		server.runCommand("jjk grade set @a jjkcraft:special_grade");
		server.runCommand("jjk energy fill @a");
		server.runCommand("execute as @p at @s run tp @s ~ ~ ~ 0 4");
		server.runCommand("execute as @p at @s rotated ~ 0 run summon minecraft:husk ^1 ^ ^5 {NoAI:1b,PersistenceRequired:1b}");
		context.waitTicks(5);

		// The opening sequence: title mid-reveal, dome growing out of the caster.
		server.runCommand("jjk domain expand @p jjkcraft:malevolent_shrine");
		context.waitTicks(14);
		context.takeScreenshot("jjkcraft-m5-domain-opening");
		context.waitTicks(46);
		if (context.computeOnClient(mc -> ClientDomains.own().isEmpty())) {
			throw new AssertionError("the client should know about its own open domain");
		}

		context.takeScreenshot("jjkcraft-m5-cutting-field");
		closeOwn(context, server);

		server.runCommand("jjk domain expand @p jjkcraft:chimera_shadow_garden");
		context.waitTicks(60);
		context.takeScreenshot("jjkcraft-m5-shadow-garden");
		closeOwn(context, server);

		server.runCommand("jjk domain expand @p jjkcraft:idle_death_gamble");
		context.waitTicks(70);
		context.takeScreenshot("jjkcraft-m5-gamble");
		closeOwn(context, server);
		server.runCommand("kill @e[type=minecraft:husk]");

		// A domain seen from outside: a curse holds it 28 blocks away.
		server.runCommand("execute as @p at @s rotated ~ 0 run jjk curse spawn jjkcraft:gravebound_gate ^ ^ ^28");
		context.waitTicks(35);
		server.runCommand("data merge entity @e[type=jjkcraft:gravebound_gate,limit=1] {NoAI:1b}");
		server.runCommand("jjk domain expand @e[type=jjkcraft:gravebound_gate,limit=1] jjkcraft:sevenfold_recitation");
		context.waitTicks(40);
		context.takeScreenshot("jjkcraft-m5-dome-outside");

		// Walk in close enough for the two domains to overlap: a clash.
		server.runCommand("execute as @p at @s rotated ~ 0 run tp @s ^ ^ ^10");
		context.waitTicks(5);
		server.runCommand("jjk domain expand @p jjkcraft:malevolent_shrine");
		context.waitTicks(18);
		context.takeScreenshot("jjkcraft-m5-clash");
		context.waitTicks(60);
		boolean won = context.computeOnClient(mc -> ClientDomains.own().map(d -> d.phase() == com.jjkcraft.domain.ActiveDomain.Phase.ACTIVE).orElse(false));
		if (!won) {
			throw new AssertionError("the Special Grade player (larger pool) should win the clash");
		}

		closeOwn(context, server);
		server.runCommand("jjk domain close @e");
		server.runCommand("jjk curse clear 64");
		context.waitTicks(25);

		// The information flood, from the receiving end.
		server.runCommand("execute as @p at @s rotated ~ 0 run jjk curse spawn jjkcraft:gravebound_gate ^ ^ ^6");
		context.waitTicks(35);
		server.runCommand("data merge entity @e[type=jjkcraft:gravebound_gate,limit=1] {NoAI:1b}");
		server.runCommand("jjk domain expand @e[type=jjkcraft:gravebound_gate,limit=1] jjkcraft:unlimited_void");
		context.waitTicks(40);
		if (!context.computeOnClient(mc -> mc.player.hasEffect(com.jjkcraft.effect.JjkEffects.OVERLOADED))) {
			throw new AssertionError("the player inside the flood should be overloaded");
		}

		context.takeScreenshot("jjkcraft-m5-flood");
		server.runCommand("jjk domain close @e");
		server.runCommand("jjk curse clear 64");
		server.runCommand("effect clear @a");
		server.runCommand("jjk domain burnout clear @a");
		server.runCommand("jjk grade set @a jjkcraft:grade_4");
		server.runCommand("kill @e[type=minecraft:item]");
		server.runCommand("gamerule spawn_mobs true");
		server.runCommand("gamerule mob_griefing true");
		context.waitTicks(25);
	}

	private static void closeOwn(ClientGameTestContext context, TestServerContext server) {
		server.runCommand("jjk domain close @p");
		context.waitTicks(25);
		server.runCommand("jjk domain burnout clear @a");
		server.runCommand("jjk energy fill @a");
		context.waitTicks(2);
	}

	private static void cast(ClientGameTestContext context, TestServerContext server, String technique, int waitTicks) {
		server.runCommand("jjk technique cooldowns @a");
		server.runCommand("jjk energy fill @a");
		server.runCommand("execute as @p run jjk technique cast @s jjkcraft:" + technique);
		context.waitTicks(waitTicks);
	}

	private static void dimensionChangeKeepsEnergy(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
		singleplayer.getServer().runCommand("jjk energy set @a 47");
		context.waitTicks(2);
		singleplayer.getServer().runCommand("execute in minecraft:the_nether run tp @a 0 128 0");
		context.waitTicks(40);
		double after = singleplayer.getServer().computeOnServer(SmokeClientGameTest::firstPlayerEnergy);
		if (after < 47 || after > 56) {
			throw new AssertionError("CE should survive a dimension change (47 plus a little regen), was " + after);
		}

		singleplayer.getServer().runCommand("execute in minecraft:overworld run tp @a 0 100 0");
		context.waitTicks(20);
	}

	private static double firstPlayerEnergy(MinecraftServer server) {
		ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
		return CursedEnergy.current(player);
	}
}
