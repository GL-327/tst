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

		TestWorldSave save;
		try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
			singleplayer.getConnection().waitForChunksRender();
			// Awakening on first join can roll Heavenly Restriction (no CE); force a normal sorcerer.
			singleplayer.getServer().runCommand("jjk awaken @a false");
			// The Culling Game may have started by itself on join; earlier scenes run without it.
			singleplayer.getServer().runCommand("jjk culling reset");
			context.waitTicks(5);
			context.takeScreenshot("jjkcraft-smoke-singleplayer");

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

			singleplayer.getServer().runCommand("jjk energy set @a 33");
			context.waitTicks(2);
			save = singleplayer.getWorldSave();
		}

		// Relog: reopen the same save and check the stored value survived.
		try (TestSingleplayerContext reopened = save.open()) {
			reopened.getConnection().waitForChunksRender();
			double after = reopened.getServer().computeOnServer(SmokeClientGameTest::firstPlayerEnergy);
			if (after < 33 || after > 40) {
				throw new AssertionError("CE should persist across relog (33 plus a little regen), was " + after);
			}

			context.waitFor(mc -> ClientEnergy.hasData());
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
