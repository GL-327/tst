package com.jjkcraft.gametest;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import com.jjkcraft.JjkCraft;
import com.jjkcraft.blackflash.BlackFlash;
import com.jjkcraft.core.domain.DomainMath;
import com.jjkcraft.core.vow.VowMath;
import com.jjkcraft.curse.CurseEntities;
import com.jjkcraft.curse.CurseEntity;
import com.jjkcraft.curse.mechanic.FerrymanMechanic;
import com.jjkcraft.curse.mechanic.PaperTideMechanic;
import com.jjkcraft.curse.mechanic.SovereignMechanic;
import com.jjkcraft.curse.mechanic.WardenMechanic;
import com.jjkcraft.domain.ActiveDomain;
import com.jjkcraft.domain.DomainService;
import com.jjkcraft.effect.JjkEffects;
import com.jjkcraft.energy.CursedEnergy;
import com.jjkcraft.reverse.ReverseTechnique;
import com.jjkcraft.sorcerer.Awakening;
import com.jjkcraft.sorcerer.PlayerActions;
import com.jjkcraft.sorcerer.Progression;
import com.jjkcraft.technique.CastFailure;
import com.jjkcraft.technique.TechniqueDefinition;
import com.jjkcraft.technique.TechniqueService;
import com.jjkcraft.technique.Techniques;
import com.jjkcraft.vow.Vows;

/** M6: Black Flash, Reverse Cursed Technique, Binding Vows, Vow Kin, the Special Grade curses and the Unbound Ritual. */
public final class SorceryGameTests {
	private static final int PAD = 48;

	private static Identifier id(String path) {
		return JjkCraft.id(path);
	}

	private static ServerPlayer sorcerer(GameTestHelper helper, String grade) {
		ServerPlayer player = TestPlayers.fresh(helper);
		Awakening.awaken(player, false);
		Progression.setGrade(player, id(grade));
		CursedEnergy.fill(player);
		return player;
	}

	private static Zombie dummy(GameTestHelper helper, double x, double y, double z) {
		Zombie husk = helper.spawn(EntityTypes.HUSK, new Vec3(x, y, z));
		husk.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 20 * 60, 9, false, false));
		return husk;
	}

	private static <T extends CurseEntity> T curse(GameTestHelper helper, net.minecraft.world.entity.EntityType<T> type, double x, double y, double z) {
		T curse = helper.spawn(type, x, y, z, EntitySpawnReason.MOB_SUMMONED);
		curse.getEntityData().set(CurseEntity.EMERGING, 0);
		return curse;
	}

	/** The player at the origin facing +Z with a still husk {@code distance} ahead. */
	private static Zombie faceTarget(GameTestHelper helper, ServerPlayer player, double distance) {
		BlockPos base = helper.absolutePos(new BlockPos(1, 1, 1));
		player.setPos(base.getX() + 0.5, base.getY(), base.getZ() + 0.5);
		player.setYRot(0);
		player.setXRot(0);
		player.setYHeadRot(0);
		Zombie husk = helper.spawn(EntityTypes.HUSK, new Vec3(1.5, 1, 1.5 + distance));
		husk.setNoAi(true);
		return husk;
	}

	// ---------------------------------------------------------------- Black Flash

	@GameTest
	public void forcedBlackFlashMultipliesTheStrike(GameTestHelper helper) {
		ServerPlayer player = sorcerer(helper, "grade_4");
		Zombie target = faceTarget(helper, player, 2.5);
		target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(100);
		target.setHealth(100);
		TechniqueService.grant(player, id("ink_iron_palm"));
		BlackFlash.forceNext(player);
		helper.assertValueEqual(TechniqueService.cast(player, id("ink_iron_palm"), 0), CastFailure.NONE, "cast");
		float dealt = 100 - target.getHealth();
		helper.assertTrue(dealt > 7 * 1.9, "a Black Flash lands for well over the palm's 7, dealt " + dealt);
		helper.assertValueEqual(Progression.get(player).counter(BlackFlash.COUNTER), 1, "counted");
		helper.assertValueEqual(BlackFlash.zoneStacks(player), 1, "in the zone");
		helper.assertTrue(BlackFlash.chance(player) > 4, "the zone raises the next chance");
		helper.succeed();
	}

	@GameTest
	public void aBlackFlashRightAfterAnotherHitStillLandsInFull(GameTestHelper helper) {
		// 26.3 moved the hit cooldown to LivingEntity.damageCooldownTime; the flash must clear that one.
		ServerPlayer player = sorcerer(helper, "grade_4");
		Zombie target = faceTarget(helper, player, 2.5);
		target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(100);
		target.setHealth(100);
		target.hurtServer(helper.getLevel(), helper.getLevel().damageSources().generic(), 5);
		float before = target.getHealth();
		TechniqueService.grant(player, id("ink_iron_palm"));
		BlackFlash.forceNext(player);
		helper.assertValueEqual(TechniqueService.cast(player, id("ink_iron_palm"), 0), CastFailure.NONE, "cast");
		float dealt = before - target.getHealth();
		helper.assertTrue(dealt > 7 * 1.9, "the earlier hit's cooldown does not eat into the flash, dealt " + dealt);
		helper.succeed();
	}

	// ---------------------------------------------------------------- Reverse Cursed Technique

	@GameTest
	public void reverseEmergencySaveHasACooldownAndAPrice(GameTestHelper helper) {
		ServerPlayer player = sorcerer(helper, "semi_grade_1");
		long now = helper.getLevel().getGameTime();
		ReverseTechnique.set(player, new com.jjkcraft.reverse.ReverseState(true, true, now), false);
		CursedEnergy.fill(player);
		helper.assertFalse(ReverseTechnique.saveFromDeath(player), "no save right after the last one");

		ReverseTechnique.set(player, ReverseTechnique.state(player).withLastSave(now - ReverseTechnique.SAVE_COOLDOWN), false);
		double max = CursedEnergy.max(player);
		helper.assertTrue(ReverseTechnique.saveFromDeath(player), "the save is ready again after five minutes");
		helper.assertTrue(Math.abs(player.getHealth() - 4f) < 1e-3, "left at 4 health");
		helper.assertTrue(Math.abs(CursedEnergy.current(player) - max * (1 - ReverseTechnique.SAVE_COST_FRACTION)) < 1, "for 40% of max CE");
		helper.assertFalse(ReverseTechnique.saveFromDeath(player), "and not twice in a row");
		helper.succeed();
	}

	@GameTest(maxTicks = 80)
	public void reverseAwakensAtTheEdgeOfDeathAndHeals(GameTestHelper helper) {
		ServerPlayer player = sorcerer(helper, "grade_3");
		ReverseTechnique.forceAwakening(player);
		helper.assertFalse(ReverseTechnique.saveFromDeath(player), "Grade 3 is too low to awaken it");

		Progression.setGrade(player, id("semi_grade_1"));
		CursedEnergy.fill(player);
		ReverseTechnique.forceAwakening(player);
		helper.assertTrue(ReverseTechnique.saveFromDeath(player), "a Semi-Grade 1 with full CE awakens it");
		helper.assertTrue(ReverseTechnique.unlocked(player), "unlocked");
		helper.assertTrue(player.getHealth() <= 1.01f, "left at a sliver");
		CursedEnergy.fill(player);
		double before = CursedEnergy.current(player);
		helper.startSequence()
				.thenExecuteFor(40, () -> {
					if (helper.getLevel().getGameTime() % 10 == 0) {
						ReverseTechnique.healStep(player);
					}
				})
				.thenWaitUntil(() -> helper.assertTrue(player.getHealth() >= 3, "heals itself"))
				.thenExecute(() -> {
					helper.assertTrue(CursedEnergy.current(player) < before, "for CE");
					helper.assertTrue(ReverseTechnique.burnout(player, 400) == 200, "and halves domain burnout");
				})
				.thenSucceed();
	}

	// ---------------------------------------------------------------- vows

	@GameTest
	public void vowPowerFeedsTechniquesAndTheirVowModifiers(GameTestHelper helper) {
		ServerPlayer player = sorcerer(helper, "grade_3");
		TechniqueService.grant(player, id("ink_iron_palm"));
		TechniqueDefinition palm = Techniques.get(helper.getLevel().registryAccess(), id("ink_iron_palm")).orElseThrow();
		double plain = TechniqueService.modifiers(player, id("ink_iron_palm"), palm).damage();
		helper.assertValueEqual(Vows.swear(player, List.of(id("grounded_oath")), VowMath.Focus.POWER, VowMath.Term.DAY), CastFailure.NONE, "sworn");
		double points = Vows.state(player).points();
		double vowed = TechniqueService.modifiers(player, id("ink_iron_palm"), palm).damage();
		double expected = plain * VowMath.damageMultiplier(VowMath.Focus.POWER, points) * 1.35;
		helper.assertTrue(Math.abs(vowed - expected) < 1e-6, "vow power and the palm's Grounded Oath bonus: " + vowed + " vs " + expected);
		helper.assertValueEqual(Vows.swear(player, List.of(id("open_guard")), VowMath.Focus.POWER, VowMath.Term.DAY), CastFailure.BUSY, "one vow at a time");
		helper.succeed();
	}

	@GameTest
	public void jumpingBreaksAGroundedOath(GameTestHelper helper) {
		ServerPlayer player = sorcerer(helper, "grade_3");
		Vows.swear(player, List.of(id("grounded_oath"), id("measured_pace")), VowMath.Focus.EFFICIENCY, VowMath.Term.BINDING);
		double max = CursedEnergy.max(player);
		CursedEnergy.fill(player);
		double points = Vows.state(player).points();
		PlayerActions.jumped(player);
		helper.assertFalse(Vows.state(player).active(), "the vow is broken");
		helper.assertTrue(Vows.state(player).inDebt(helper.getLevel().getGameTime()), "a debt is owed");
		helper.assertTrue(player.hasEffect(JjkEffects.STUNNED), "stunned");
		helper.assertTrue(CursedEnergy.current(player) < max * (1 - VowMath.breakDrain(points, false)) + 0.5, "CE taken");
		helper.assertValueEqual(Vows.swear(player, List.of(id("open_guard")), VowMath.Focus.POWER, VowMath.Term.DAY), CastFailure.VOW,
				"no new vow while in debt");
		helper.succeed();
	}

	@GameTest(padding = PAD)
	public void enforcedVowsRefuseInsteadOfBreaking(GameTestHelper helper) {
		ServerPlayer player = sorcerer(helper, "grade_1");
		TechniqueService.setInnate(player, id("grudge_bolt"));
		TechniqueService.grant(player, id("cleave"));
		DomainService.setState(player, DomainService.state(player).withKnown(java.util.Optional.of(id("malevolent_shrine"))));
		Vows.swear(player, List.of(id("single_path"), id("closed_heart")), VowMath.Focus.POWER, VowMath.Term.DAY);
		helper.assertValueEqual(TechniqueService.check(player, id("cleave"),
				Techniques.get(helper.getLevel().registryAccess(), id("cleave")).orElseThrow()), CastFailure.VOW, "only the innate technique");
		helper.assertValueEqual(TechniqueService.check(player, id("grudge_bolt"),
				Techniques.get(helper.getLevel().registryAccess(), id("grudge_bolt")).orElseThrow()), CastFailure.NONE, "the innate one works");
		helper.assertValueEqual(DomainService.toggle(player, DomainMath.Variant.FULL), CastFailure.VOW, "no domains");
		helper.assertTrue(Vows.state(player).active(), "refusals don't break the vow");
		helper.succeed();
	}

	@GameTest
	public void halvedReserveHalvesThePool(GameTestHelper helper) {
		ServerPlayer player = sorcerer(helper, "grade_3");
		double before = CursedEnergy.max(player);
		Vows.swear(player, List.of(id("half_vessel")), VowMath.Focus.POWER, VowMath.Term.SKIRMISH);
		helper.assertTrue(Math.abs(CursedEnergy.max(player) - before / 2) < 0.01, "half the pool");
		Vows.renounce(player);
		helper.assertTrue(Math.abs(CursedEnergy.max(player) - before) < 0.01, "back after renouncing");
		helper.succeed();
	}

	@GameTest
	public void vowKinFightsOnlyTheVowedAndFleesABrokenVow(GameTestHelper helper) {
		ServerPlayer player = sorcerer(helper, "grade_3");
		CurseEntity kin = curse(helper, CurseEntities.VOW_KIN, 2, 1, 2);
		Zombie husk = dummy(helper, 2, 1, 4);
		kin.setTarget(husk);
		helper.assertTrue(kin.mechanic().attackMultiplier(kin, husk) == 0, "no harm to the vowless");

		Vows.swear(player, List.of(id("grounded_oath")), VowMath.Focus.POWER, VowMath.Term.DAY);
		kin.setTarget(player);
		kin.setMechA(1);
		helper.assertTrue(kin.mechanic().canAttack(kin), "fights a vow-holder");
		helper.assertTrue(kin.mechanic().attackMultiplier(kin, player) > 1, "harder the heavier the vow");
		Vows.breakVow(player);
		helper.assertValueEqual(kin.mechA(), 2, "flees when the vow breaks");
		helper.assertFalse(kin.mechanic().canAct(kin), "and stops fighting");
		helper.succeed();
	}

	// ---------------------------------------------------------------- Special Grade curses

	@GameTest(padding = PAD)
	public void gravemindAdaptsToRepeatedDamage(GameTestHelper helper) {
		CurseEntity mind = curse(helper, CurseEntities.PAPER_TIDE_GRAVEMIND, 3, 1, 3);
		for (int i = 0; i < 4; i++) {
			mind.mechanic().onHurt(mind, helper.getLevel(), helper.getLevel().damageSources().generic(), 5);
		}

		helper.assertTrue(Math.abs(PaperTideMechanic.resistanceTo(mind, "minecraft:generic") - 0.6) < 1e-6, "first fold after four hits");
		for (int i = 0; i < 4; i++) {
			mind.mechanic().onHurt(mind, helper.getLevel(), helper.getLevel().damageSources().generic(), 5);
		}

		helper.assertTrue(Math.abs(PaperTideMechanic.resistanceTo(mind, "minecraft:generic") - 0.25) < 1e-6, "second fold after eight");
		float fresh = mind.mechanic().onHurt(mind, helper.getLevel(), helper.getLevel().damageSources().inFire(), 5);
		helper.assertTrue(Math.abs(fresh - 5) < 1e-4, "a new kind of harm lands in full");
		helper.assertValueEqual(mind.mechA(), 2, "two adaptations made");
		helper.succeed();
	}

	@GameTest(padding = PAD, maxTicks = 80)
	public void ferrymanCrossingPullsToTheShore(GameTestHelper helper) {
		// Everything stays on the test structure's floor (about 8 blocks across).
		CurseEntity ferryman = curse(helper, CurseEntities.UNFINISHED_FERRYMAN, 1, 1, 1);
		Zombie traveller = dummy(helper, 6, 1, 6);
		ferryman.memory.put("next_crossing", 1);
		double before = traveller.position().distanceTo(FerrymanMechanic.shore(ferryman));
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertValueEqual(ferryman.mechA(), 1, "the crossing opens"))
				.thenIdle(20)
				.thenExecute(() -> helper.assertTrue(traveller.position().distanceTo(FerrymanMechanic.shore(ferryman)) < before - 1,
						"the current drags the traveller toward the shore"))
				.thenSucceed();
	}

	@GameTest(padding = PAD, maxTicks = 60)
	public void wardenCannotDieWhileSealsStand(GameTestHelper helper) {
		CurseEntity warden = curse(helper, CurseEntities.CULLING_WARDEN, 3, 1, 3);
		Zombie husk = dummy(helper, 3, 1, 6);
		warden.setTarget(husk);
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(WardenMechanic.sealsStanding(warden, helper.getLevel()) > 0, "seals raised"))
				.thenExecute(() -> {
					warden.hurtServer(helper.getLevel(), helper.getLevel().damageSources().playerAttack(TestPlayers.fresh(helper)), 100_000);
					helper.assertTrue(warden.isAlive() && warden.getHealth() >= 1, "the rules hold it up");
					for (Object seal : List.copyOf((List<?>) warden.memory.get("seals"))) {
						helper.getLevel().setBlockAndUpdate((BlockPos) seal, Blocks.AIR.defaultBlockState());
					}

					helper.assertValueEqual(WardenMechanic.sealsStanding(warden, helper.getLevel()), 0, "seals broken");
					warden.damageCooldownTime = 0;
					warden.hurtServer(helper.getLevel(), helper.getLevel().damageSources().playerAttack(TestPlayers.fresh(helper)), 100_000);
					helper.assertFalse(warden.isAlive(), "now it can fall");
				})
				.thenSucceed();
	}

	@GameTest
	public void wardenEdictPicksTheWeakPoint(GameTestHelper helper) {
		CurseEntity warden = curse(helper, CurseEntities.CULLING_WARDEN, 3, 1, 3);
		warden.setMechA(WardenMechanic.Edict.ENERGY.ordinal());
		var technique = helper.getLevel().damageSources().source(com.jjkcraft.damage.JjkDamageTypes.TECHNIQUE, warden);
		float on = warden.mechanic().onHurt(warden, helper.getLevel(), technique, 10);
		float off = warden.mechanic().onHurt(warden, helper.getLevel(), helper.getLevel().damageSources().playerAttack(TestPlayers.fresh(helper)), 10);
		helper.assertTrue(on > 10 && off < 2, "Energy edict: techniques " + on + ", blows " + off);
		helper.succeed();
	}

	@GameTest(padding = PAD, maxTicks = 80)
	public void sovereignOpensItsThroatOnTheLastMouths(GameTestHelper helper) {
		CurseEntity sovereign = curse(helper, CurseEntities.TEN_MOUTH_SOVEREIGN, 4, 1, 4);
		Zombie husk = dummy(helper, 4, 1, 9);
		sovereign.setTarget(husk);
		sovereign.setHealth(sovereign.getMaxHealth() * 0.25f);
		helper.assertValueEqual(SovereignMechanic.mouths(sovereign), 3, "three mouths left");
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(DomainService.active(sovereign).isPresent(), "its domain opens"))
				.thenExecute(() -> DomainService.close(sovereign))
				.thenSucceed();
	}

	// ---------------------------------------------------------------- the Unbound Ritual

	@GameTest(padding = PAD, maxTicks = 100)
	public void unboundRitualPromotesToSpecialGrade(GameTestHelper helper) {
		ServerPlayer player = sorcerer(helper, "grade_1");
		TechniqueService.setInnate(player, id("ink_iron_palm"));
		TechniqueDefinition palm = Techniques.get(helper.getLevel().registryAccess(), id("ink_iron_palm")).orElseThrow();
		TechniqueService.set(player, TechniqueService.get(player).withMasteryAdded(id("ink_iron_palm"), palm.masteryUses()));
		Vows.swear(player, List.of(id("open_guard")), VowMath.Focus.POWER, VowMath.Term.DAY);
		helper.assertValueEqual(DomainService.expand(player, id("malevolent_shrine"), DomainMath.Variant.FULL, true), CastFailure.NONE, "domain open");
		CurseEntity mind = curse(helper, CurseEntities.PAPER_TIDE_GRAVEMIND, 3, 1, 4);
		mind.setNoAi(true);
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(DomainService.active(player).map(d -> d.phase() == ActiveDomain.Phase.ACTIVE).orElse(false), "domain holds"))
				.thenExecute(() -> {
					mind.hurtServer(helper.getLevel(), helper.getLevel().damageSources().playerAttack(player), 100_000);
					helper.assertFalse(mind.isAlive(), "the Special Grade curse falls inside the domain");
					helper.assertValueEqual(Progression.get(player).grade(), id("special_grade"), "promoted to Special Grade");
					helper.assertTrue(Progression.get(player).specialUnlocked(), "the one-time unlock is recorded");
					DomainService.close(player);
				})
				.thenSucceed();
	}
}
