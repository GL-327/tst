package com.jjkcraft.gametest;

import java.util.List;
import java.util.Optional;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import com.jjkcraft.JjkCraft;
import com.jjkcraft.attachment.JjkAttachments;
import com.jjkcraft.config.JjkConfig;
import com.jjkcraft.damage.JjkDamageTypes;
import com.jjkcraft.energy.CursedEnergy;
import com.jjkcraft.modulo.Modulo;
import com.jjkcraft.modulo.ModuloBlocks;
import com.jjkcraft.modulo.ModuloItems;
import com.jjkcraft.modulo.ModuloProgress;
import com.jjkcraft.modulo.ModuloTravel;
import com.jjkcraft.modulo.MulEnergy;
import com.jjkcraft.modulo.NaunaxDescent;
import com.jjkcraft.modulo.NaunaxGateBlock;
import com.jjkcraft.modulo.data.SiteDefinition;
import com.jjkcraft.modulo.entity.MahoragaEntity;
import com.jjkcraft.modulo.entity.ModuloBossEntity;
import com.jjkcraft.modulo.entity.ModuloEntities;
import com.jjkcraft.modulo.entity.ModuloNpcEntity;
import com.jjkcraft.modulo.mission.Missions;
import com.jjkcraft.modulo.world.SiteBuilder;
import com.jjkcraft.sorcerer.Progression;
import com.jjkcraft.sorcerer.SorcererProgress;
import com.jjkcraft.technique.CastFailure;
import com.jjkcraft.technique.TechniqueService;

/**
 * The Modulo expansion (docs/MODULO.md): its dimensions, sites, travel, Mul energy, mediums,
 * Mahoraga's adaptation, bosses for several fighters, the mission chain with its choice and
 * checkpoints, the epilogue, and the off switch.
 */
public class ModuloGameTests {
	private static Identifier modulo(String path) {
		return Identifier.fromNamespaceAndPath(Modulo.NAMESPACE, path);
	}

	/** Turns the expansion on, with the Descent's timer pushed far away so no test starts one by itself. */
	/**
	 * Turns the expansion on for one test. Every test here runs within a single tick, and
	 * {@link #done} turns it off again before the next test runs, so other suites keep the default.
	 */
	private static MinecraftServer on(GameTestHelper helper) {
		JjkConfig.server().modulo.enabled = true;
		MinecraftServer server = helper.getLevel().getServer();
		Modulo.state(server).nextDescent = Long.MAX_VALUE / 4;
		return server;
	}

	private static void done(GameTestHelper helper) {
		JjkConfig.server().modulo.enabled = false;
		helper.succeed();
	}

	private static ServerPlayer gradeOne(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.fresh(helper);
		Progression.setGrade(player, JjkCraft.id("grade_1"));
		return player;
	}

	@GameTest
	public void theModuloDimensionsLoadAndGenerate(GameTestHelper helper) {
		MinecraftServer server = on(helper);
		ServerLevel naunax = server.getLevel(Modulo.NAUNAX);
		ServerLevel tokyo = server.getLevel(Modulo.TOKYO);
		helper.assertTrue(naunax != null && tokyo != null, "both Modulo dimensions are loaded");
		helper.assertTrue(naunax.getBlockState(new BlockPos(5, 63, 5)).is(ModuloBlocks.NAUNAX_DECK_PLATE), "the Naunax has a deck");
		helper.assertTrue(naunax.getBlockState(new BlockPos(5, 77, 5)).is(ModuloBlocks.NAUNAX_HULL), "and a hull ceiling over it");
		helper.assertTrue(naunax.getBlockState(new BlockPos(5, 64, 5)).isAir(), "with room to stand between");
		helper.assertFalse(tokyo.getBlockState(new BlockPos(8, 63, 8)).isAir(), "Tokyo 2086 has ground");
		helper.assertTrue(tokyo.getBlockState(new BlockPos(0, 63, 0)).is(net.minecraft.world.level.block.Blocks.CONCRETE.black())
				|| tokyo.getBlockState(new BlockPos(0, 63, 0)).is(net.minecraft.world.level.block.Blocks.CONCRETE.white())
				|| tokyo.getBlockState(new BlockPos(0, 63, 0)).is(net.minecraft.world.level.block.Blocks.GRAVEL), "and streets on the chunk grid");
		done(helper);
	}

	@GameTest
	public void theShipsArrivalHallIsBuiltWithItsGates(GameTestHelper helper) {
		MinecraftServer server = on(helper);
		Optional<SiteDefinition> site = SiteBuilder.ensure(server, ModuloTravel.SHIP_SITE);
		helper.assertTrue(site.isPresent(), "the arrival hall builds");
		ServerLevel naunax = server.getLevel(Modulo.NAUNAX);
		var earth = naunax.getBlockState(site.get().marker("gate_earth"));
		var tokyo = naunax.getBlockState(site.get().marker("gate_tokyo"));
		helper.assertTrue(earth.is(ModuloBlocks.NAUNAX_GATE) && earth.getValue(NaunaxGateBlock.DESTINATION) == NaunaxGateBlock.Destination.EARTH, "a gate home");
		helper.assertTrue(tokyo.is(ModuloBlocks.NAUNAX_GATE) && tokyo.getValue(NaunaxGateBlock.DESTINATION) == NaunaxGateBlock.Destination.TOKYO, "a gate to Tokyo");
		done(helper);
	}

	@GameTest
	public void gradeChangesNeverOfferStoryBoarding(GameTestHelper helper) {
        var server=on(helper);var player=gradeOne(helper);var before=player.position();
        helper.assertFalse(NaunaxDescent.eligible(player),"no expedition offered");
        NaunaxDescent.start(server,helper.absolutePos(new BlockPos(2,1,2)),200);
        helper.assertFalse(NaunaxDescent.active(server)||ModuloTravel.board(player),"story boarding disabled");
        helper.assertValueEqual(player.position(),before,"player stays in the normal world");done(helper);
    }

	@GameTest
	public void pouringCursedEnergyIntoMulMakesMulEnergyAtTheRatio(GameTestHelper helper) {
		on(helper);
		ServerPlayer player = TestPlayers.fresh(helper);
		CursedEnergy.set(player, 100);
		double poured = MulEnergy.pour(player, 20, 40);
		helper.assertValueEqual(poured, 20.0, "twenty cursed energy poured");
		helper.assertValueEqual(MulEnergy.get(player), 20 * JjkConfig.server().modulo.mulEnergyRatio, "Mul energy at the ratio");
		helper.assertTrue(Math.abs(CursedEnergy.current(player) - 80) < 0.5, "the cursed energy is gone");
		helper.assertValueEqual(MulEnergy.pour(player, 20, 5), 5.0, "a nearly spent piece takes only what it holds");

		ServerPlayer restricted = TestPlayers.fresh(helper);
		restricted.setAttached(JjkAttachments.PROGRESS, SorcererProgress.DEFAULT.withHeavenlyRestriction(true));
		helper.assertValueEqual(MulEnergy.pour(restricted, 20, 40), 0.0, "Heavenly Restriction has nothing to pour");
		done(helper);
	}

	@GameTest
	public void mulLanceCostsMulEnergyNotCursedEnergy(GameTestHelper helper) {
		on(helper);
		ServerPlayer player = TestPlayers.fresh(helper);
		Progression.setGrade(player, JjkCraft.id("grade_2"));
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModuloItems.RUMELIAN_BLADE));
		CursedEnergy.set(player, 50);
		Identifier lance = JjkCraft.id("mul_lance");
		MulEnergy.set(player, 10);
		helper.assertValueEqual(TechniqueService.cast(player, lance, -1), CastFailure.NOT_ENOUGH_MUL, "too little Mul energy is refused");
		MulEnergy.set(player, 60);
		helper.assertValueEqual(TechniqueService.cast(player, lance, -1), CastFailure.NONE, "enough is accepted");
		helper.assertValueEqual(MulEnergy.get(player), 35.0, "and 25 Mul energy is paid");
		helper.assertTrue(Math.abs(CursedEnergy.current(player) - 50) < 0.5, "but no cursed energy");
		done(helper);
	}

	@GameTest
	public void aMediumPaysWithItsDurabilityEvenUnderHeavenlyRestriction(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.fresh(helper);
		player.setAttached(JjkAttachments.PROGRESS, SorcererProgress.DEFAULT.withHeavenlyRestriction(true));
		Progression.setGrade(player, JjkCraft.id("grade_2"));
		ItemStack honoyagi = new ItemStack(ModuloItems.HONOYAGI);
		player.setItemInHand(InteractionHand.MAIN_HAND, honoyagi);
		CastFailure result = TechniqueService.cast(player, JjkCraft.id("batto_sword_drawing"), -1);
		helper.assertValueEqual(result, CastFailure.NONE, "Tsurugi's way: the tool's energy, not the wielder's");
		ItemStack held = player.getMainHandItem();
		helper.assertValueEqual(held.getMaxDamage(), 288, "a Grade 2 tool's durability as a medium");
		helper.assertValueEqual(held.getDamageValue(), 3, "a 30 CE technique wears it by 3");

		ServerPlayer plain = TestPlayers.fresh(helper);
		plain.setAttached(JjkAttachments.PROGRESS, SorcererProgress.DEFAULT.withHeavenlyRestriction(true));
		Progression.setGrade(plain, JjkCraft.id("grade_2"));
		plain.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(com.jjkcraft.item.JjkItems.DEBT_CLEAVER));
		helper.assertValueEqual(TechniqueService.cast(plain, JjkCraft.id("cleave"), -1), CastFailure.HEAVENLY_RESTRICTION,
				"a tool that is not a medium still needs cursed energy");
		done(helper);
	}

	@GameTest
	public void mahoragaAdaptsToWhatHurtsIt(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		MahoragaEntity mahoraga = ModuloEntities.MAHORAGA.create(level, EntitySpawnReason.EVENT);
		BlockPos at = helper.absolutePos(new BlockPos(2, 1, 2));
		mahoraga.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
		mahoraga.setup(level);
		level.addFreshEntity(mahoraga);

		var light = level.damageSources().source(JjkDamageTypes.LIGHT);
		float[] taken = new float[9];
		for (int i = 0; i < taken.length; i++) {
			mahoraga.damageCooldownTime = 0;
			float before = mahoraga.getHealth();
			mahoraga.hurtServer(level, light, 20);
			taken[i] = before - mahoraga.getHealth();
		}

		helper.assertTrue(taken[0] > taken[3] && taken[3] > taken[7], "each hit of Light hurts less than the last: " + java.util.Arrays.toString(taken));
		helper.assertTrue(mahoraga.fullyAdapted(JjkDamageTypes.LIGHT.identifier().toString()), "after eight hits it has fully adapted to Light");
		helper.assertValueEqual(mahoraga.wheel(), 9, "the wheel turned once per hit");

		mahoraga.damageCooldownTime = 0;
		float before = mahoraga.getHealth();
		mahoraga.hurtServer(level, level.damageSources().source(JjkDamageTypes.DARKNESS), 20);
		helper.assertTrue(before - mahoraga.getHealth() > taken[7] * 2, "a new kind of damage is not resisted yet");
		mahoraga.discard();
		done(helper);
	}

	@GameTest
	public void daburasStandaloneHealthScalesWithoutQuestProgress(GameTestHelper helper) {
		MinecraftServer server = on(helper);
		ServerLevel level = helper.getLevel();
		Identifier mission = modulo("ritual_of_harmony");
		ServerPlayer a = gradeOne(helper);
		ServerPlayer b = gradeOne(helper);
		Missions.jumpTo(a, mission, 1);
		Missions.jumpTo(b, mission, 1);
		ModuloBossEntity dabura = ModuloEntities.spawnBoss(level, modulo("dabura"), helper.absolutePos(new BlockPos(2, 1, 2)), null, mission, 1, 1);
		helper.assertTrue(dabura != null, "Dabura spawns");
		helper.assertValueEqual((double) dabura.getMaxHealth(), 520.0, "alone, his base health");
		dabura.rescale(4);
		helper.assertTrue(Math.abs(dabura.effectiveMaxHealth() - 520 * (1 + 3 * JjkConfig.server().modulo.bossHealthPerExtraPlayer)) < 0.01,
				"with four fighters, more: " + dabura.effectiveMaxHealth());
		helper.assertTrue(dabura.getMaxHealth() <= 1024, "the attribute stays under vanilla's ceiling");
		dabura.rescale(2);
		Missions.onBossDefeated(dabura, List.of(a, b));
		dabura.discard();
		helper.assertValueEqual(Modulo.progress(a).stage(), 0, "standalone fight starts no quest");
		helper.assertValueEqual(Modulo.progress(b).stage(), 0, "no quest for the second fighter either");
		done(helper);
	}

	@GameTest
	public void progressSurvivesASaveAndRestartsTheStageAtItsCheckpoint(GameTestHelper helper) {
		Identifier mission = modulo("berserk");
		ModuloProgress progress = ModuloProgress.EMPTY.withBoarded(GlobalPos.of(Level.OVERWORLD, BlockPos.ZERO))
				.starting(mission).nextStage().withCounter(37).withCompanion(true).withFlag("chaos_and_harmony", "duel");
		JsonElement saved = ModuloProgress.CODEC.encodeStart(JsonOps.INSTANCE, progress).getOrThrow();
		ModuloProgress loaded = ModuloProgress.CODEC.parse(JsonOps.INSTANCE, saved).getOrThrow();
		helper.assertValueEqual(loaded, progress, "the progress round-trips");
		helper.assertValueEqual(loaded.atCheckpoint().counter(), 0, "after a death or relog the stage starts over");
		helper.assertValueEqual(loaded.atCheckpoint().stage(), 1, "but the stage itself is kept");
		done(helper);
	}

	@GameTest(environment = "jjkcraft:modulo_off")
	public void turningTheExpansionOffStopsIt(GameTestHelper helper) {
		JjkConfig.server().modulo.enabled = false;
		try {
			ServerPlayer player = gradeOne(helper);
			helper.assertFalse(NaunaxDescent.eligible(player), "no Descent");
			helper.assertTrue(Missions.current(player).isEmpty(), "no missions");
			helper.assertFalse(ModuloTravel.board(player), "no boarding");
			helper.assertFalse(Modulo.view(player).enabled(), "nothing on the HUD");
		} finally {
			JjkConfig.server().modulo.enabled = false;
		}

		done(helper);
	}
}
