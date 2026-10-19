package com.jjkcraft.gametest;

import java.util.List;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import com.jjkcraft.JjkCraft;
import com.jjkcraft.culling.entity.CullingEntities;
import com.jjkcraft.culling.entity.EntrantEntity;
import com.jjkcraft.curse.CurseEntities;
import com.jjkcraft.curse.CurseEntity;
import com.jjkcraft.energy.CursedEnergy;
import com.jjkcraft.sorcerer.Awakening;
import com.jjkcraft.sorcerer.Progression;
import com.jjkcraft.technique.TechniqueService;

/**
 * A crowded arena: forty curses of every kind with their AI on, a dozen entrants and a full set of
 * summons, run for 200 ticks. It logs the server's average tick time for the test report and fails
 * only if the server falls badly behind (other tests share the server, so the number is an upper
 * bound, not a benchmark).
 */
public class PerformanceGameTests {
	private static final List<String> MIX = List.of("hollow_hymnal", "ledger_crawler", "wick_moth", "doorframe_hunter", "pale_stilt", "echo_mimic",
			"salt_lantern", "gilded_mimic", "tidewash", "thorn_matriarch", "cracked_bell", "tithe_burrower", "redacted_saint", "ash_oni",
			"vow_kin");

	@GameTest(maxTicks = 260)
	public void crowdedArenaStaysWithinTheTickBudget(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		int spawned = 0;
		for (int i = 0; i < 40; i++) {
			EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(JjkCraft.id(MIX.get(i % MIX.size())));
			var entity = type.create(helper.getLevel(), EntitySpawnReason.MOB_SUMMONED);
			if (entity instanceof CurseEntity curse) {
				Vec3 at = helper.absoluteVec(new Vec3(1 + (i % 6), 1, 1 + (i / 6) % 6));
				curse.snapTo(at.x, at.y, at.z, 0, 0);
				curse.getEntityData().set(CurseEntity.EMERGING, 0);
				helper.getLevel().addFreshEntity(curse);
				spawned++;
			}
		}

		for (int i = 0; i < 12; i++) {
			EntrantEntity entrant = helper.spawn(CullingEntities.ENTRANT, new Vec3(1 + (i % 6), 1, 6));
			entrant.setup(i % 3 == 0, -1, helper.getLevel().getRandom());
		}

		ServerPlayer sorcerer = TestPlayers.fresh(helper);
		Awakening.awaken(sorcerer, false);
		Progression.setGrade(sorcerer, JjkCraft.id("grade_2"));
		for (String technique : List.of("ten_shadows_divine_dogs", "ten_shadows_nue", "ten_shadows_toad")) {
			TechniqueService.grant(sorcerer, JjkCraft.id(technique));
			CursedEnergy.fill(sorcerer);
			TechniqueService.cast(sorcerer, JjkCraft.id(technique), -1);
		}

		int curses = spawned;
		helper.startSequence()
				.thenIdle(200)
				.thenExecute(() -> {
					double mspt = server.getAverageTickTimeNanos() / 1_000_000.0;
					JjkCraft.LOGGER.info("Performance: {} curses, 12 entrants, 4 summons for 200 ticks; average tick {} ms", curses,
							String.format(java.util.Locale.ROOT, "%.2f", mspt));
					helper.assertTrue(mspt < 50, "the server fell behind: " + mspt + " ms per tick");
				})
				.thenSucceed();
	}
}
