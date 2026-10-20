package com.jjkcraft.gametest;

import java.util.List;
import java.util.Optional;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import com.jjkcraft.JjkCraft;
import com.jjkcraft.core.domain.DomainMath;
import com.jjkcraft.core.progress.ProgressionMath;
import com.jjkcraft.curse.CurseEntities;
import com.jjkcraft.curse.CurseEntity;
import com.jjkcraft.domain.ActiveDomain;
import com.jjkcraft.domain.DomainService;
import com.jjkcraft.domain.Domains;
import com.jjkcraft.domain.behavior.RepetitionBehavior;
import com.jjkcraft.effect.JjkEffects;
import com.jjkcraft.energy.CursedEnergy;
import com.jjkcraft.entity.ShadowDivineDogEntity;
import com.jjkcraft.grade.Grades;
import com.jjkcraft.item.JjkComponents;
import com.jjkcraft.sorcerer.Awakening;
import com.jjkcraft.sorcerer.Progression;
import com.jjkcraft.technique.CastFailure;
import com.jjkcraft.technique.TechniqueService;
import com.jjkcraft.technique.TechniqueSources;

/**
 * Domain tests. Domains reach far past a test's own space, so every test here uses wide padding
 * (no two tests' domains overlap) and closes what it opens.
 */
public final class DomainGameTests {
	private static final int PAD = 48;

	private static Identifier id(String path) {
		return JjkCraft.id(path);
	}

	/** An awakened sorcerer at the given grade with a full pool, standing at the test origin. */
	private static ServerPlayer sorcerer(GameTestHelper helper, String grade) {
		ServerPlayer player = TestPlayers.fresh(helper);
		Awakening.awaken(player, false);
		Progression.setGrade(player, id(grade));
		CursedEnergy.fill(player);
		return player;
	}

	/** A target that stands still and does not burn in daylight. */
	private static Zombie dummy(GameTestHelper helper, double x, double y, double z) {
		Zombie husk = helper.spawn(EntityTypes.HUSK, new Vec3(x, y, z));
		husk.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 20 * 60, 9, false, false));
		return husk;
	}

	private static ActiveDomain open(GameTestHelper helper, ServerPlayer caster, String domain, DomainMath.Variant variant) {
		helper.assertValueEqual(DomainService.expand(caster, id(domain), variant, true), CastFailure.NONE, "expand " + domain);
		return DomainService.active(caster).orElseThrow();
	}

	@GameTest
	public void everyDomainLoadsWithValidData(GameTestHelper helper) {
		List<Domains.Entry> all = Domains.all(helper.getLevel().registryAccess());
		helper.assertTrue(all.size() >= 6, "at least 6 domains, got " + all.size());
		for (Domains.Entry entry : all) {
			helper.assertTrue(Grades.get(helper.getLevel().registryAccess(), entry.definition().grade()).isPresent(), entry.id() + " grade");
			helper.assertFalse(entry.definition().behaviors().isEmpty(), entry.id() + " has behaviors");
			int rank = Grades.rank(helper.getLevel().registryAccess(), entry.definition().grade());
			helper.assertTrue(rank >= Grades.rank(helper.getLevel().registryAccess(), id("grade_1")), entry.id() + " needs Grade 1 or higher");
		}

		helper.succeed();
	}

	@GameTest(padding = PAD)
	public void castingChecksDomainGradeAndEnergy(GameTestHelper helper) {
		ServerPlayer player = sorcerer(helper, "grade_4");
		helper.assertValueEqual(DomainService.toggle(player, DomainMath.Variant.FULL), CastFailure.NO_DOMAIN, "no domain yet");
		DomainService.setState(player, DomainService.state(player).withKnown(Optional.of(id("malevolent_shrine"))));
		helper.assertValueEqual(DomainService.toggle(player, DomainMath.Variant.FULL), CastFailure.GRADE_TOO_LOW, "Grade 4 can't");

		Progression.setGrade(player, id("grade_1"));
		CursedEnergy.set(player, 50);
		helper.assertValueEqual(DomainService.toggle(player, DomainMath.Variant.FULL), CastFailure.NOT_ENOUGH_ENERGY, "50 CE is not enough");
		helper.assertTrue(Math.abs(CursedEnergy.current(player) - 50) < 0.5, "a refused cast spends nothing");

		CursedEnergy.fill(player);
		double before = CursedEnergy.current(player);
		helper.assertValueEqual(DomainService.toggle(player, DomainMath.Variant.FULL), CastFailure.NONE, "opens");
		helper.assertTrue(Math.abs(before - CursedEnergy.current(player) - 140) < 0.5, "cast cost 140");
		helper.assertTrue(DomainService.active(player).isPresent(), "domain is open");
		helper.assertValueEqual(DomainService.expand(player, id("malevolent_shrine"), DomainMath.Variant.FULL, true), CastFailure.BUSY, "one at a time");

		DomainService.toggle(player, DomainMath.Variant.FULL);
		helper.assertValueEqual(DomainService.active(player).get().phase(), ActiveDomain.Phase.CLOSING, "pressing again closes it");
		helper.assertTrue(DomainService.state(player).burntOut(helper.getLevel().getGameTime()), "burnout after closing");
		helper.assertValueEqual(TechniqueService.check(player, id("grudge_bolt"),
				com.jjkcraft.technique.Techniques.get(helper.getLevel().registryAccess(), id("grudge_bolt")).orElseThrow()), CastFailure.BURNOUT,
				"techniques are burnt out");
		helper.succeed();
	}

	@GameTest
	public void reachingGradeOneGrantsADomain(GameTestHelper helper) {
		ServerPlayer player = sorcerer(helper, "grade_2");
		helper.assertTrue(DomainService.state(player).known().isEmpty(), "no domain at Grade 2");
		Progression.setGrade(player, id("grade_1"));
		helper.assertTrue(DomainService.state(player).known().isPresent(), "a domain at Grade 1");
		helper.succeed();
	}

	@GameTest
	public void inheritedLimitlessWaitsForItsOwnDomain(GameTestHelper helper) {
		ServerPlayer player = sorcerer(helper, "grade_2");
		player.setAttached(com.jjkcraft.awakening.CharacterAwakening.RECORD,
				new com.jjkcraft.awakening.AwakeningRecord("inherit", "jjkcraft:limitless", "Gojo", false, 0));
		Progression.setGrade(player, id("grade_1"));
		helper.assertTrue(DomainService.state(player).known().isEmpty(), "Limitless waits for Unlimited Void's grade");
		Progression.setGrade(player, id("special_grade"));
		helper.assertValueEqual(DomainService.state(player).known(), Optional.of(id("unlimited_void")), "Limitless grants Unlimited Void");
		helper.succeed();
	}

	@GameTest(padding = PAD, maxTicks = 120)
	public void sureHitCutsThroughArmor(GameTestHelper helper) {
		ServerPlayer caster = sorcerer(helper, "grade_1");
		Zombie target = dummy(helper, 4, 1, 4);
		target.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, new ItemStack(net.minecraft.world.item.Items.NETHERITE_CHESTPLATE));
		float max = target.getHealth();
		ActiveDomain domain = open(helper, caster, "malevolent_shrine", DomainMath.Variant.FULL);
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(domain.phase() == ActiveDomain.Phase.ACTIVE, "opened"))
				.thenWaitUntil(() -> helper.assertTrue(target.getHealth() <= max - 2.9f, "two pulses of 1.5 land in full through netherite"))
				.thenExecute(() -> {
					helper.assertTrue(domain.everyoneInside().contains(target), "target tracked inside");
					DomainService.close(caster);
				})
				.thenSucceed();
	}

	@GameTest(padding = PAD, maxTicks = 120)
	public void barrierHoldsTrappedTargets(GameTestHelper helper) {
		ServerPlayer caster = sorcerer(helper, "grade_1");
		Zombie target = dummy(helper, 4, 1, 4);
		ActiveDomain domain = open(helper, caster, "malevolent_shrine", DomainMath.Variant.FULL);
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(domain.phase() == ActiveDomain.Phase.ACTIVE, "opened"))
				.thenExecute(() -> target.teleportTo(domain.center().x + domain.radius() + 6, target.getY(), domain.center().z))
				.thenIdle(2)
				.thenExecute(() -> {
					helper.assertTrue(target.position().distanceTo(domain.center()) <= domain.radius(), "pulled back inside the barrier");
					DomainService.close(caster);
				})
				.thenSucceed();
	}

	@GameTest(padding = PAD)
	public void barrierlessCostsMoreAndTrapsNoOne(GameTestHelper helper) {
		ServerPlayer caster = sorcerer(helper, "special_grade");
		dummy(helper, 3, 1, 3);
		double before = CursedEnergy.current(caster);
		ActiveDomain domain = open(helper, caster, "malevolent_shrine", DomainMath.Variant.BARRIERLESS);
		helper.assertTrue(Math.abs(before - CursedEnergy.current(caster) - 140 * 1.6) < 0.5, "barrierless costs 1.6x");
		helper.assertTrue(Math.abs(domain.radius() - 12 * 1.4) < 1e-6, "and reaches 1.4x as far");
		helper.assertFalse(domain.hasBarrier(), "no barrier");
		DomainService.close(caster);
		helper.succeed();
	}

	@GameTest(padding = PAD, maxTicks = 100)
	public void upkeepRunsOutAndTheDomainCloses(GameTestHelper helper) {
		ServerPlayer caster = sorcerer(helper, "grade_1");
		ActiveDomain domain = open(helper, caster, "malevolent_shrine", DomainMath.Variant.FULL);
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(domain.phase() == ActiveDomain.Phase.ACTIVE, "opened"))
				.thenExecute(() -> CursedEnergy.set(caster, 5))
				.thenWaitUntil(() -> helper.assertTrue(domain.phase() == ActiveDomain.Phase.CLOSING, "closes when upkeep can't be paid"))
				.thenSucceed();
	}

	@GameTest(padding = PAD, maxTicks = 120)
	public void simpleDomainCancelsSureHit(GameTestHelper helper) {
		ServerPlayer caster = sorcerer(helper, "grade_1");
		CurseEntity shielded = helper.spawn(CurseEntities.LEDGER_CRAWLER, 4, 1, 1, EntitySpawnReason.MOB_SUMMONED);
		CurseEntity exposed = helper.spawn(CurseEntities.LEDGER_CRAWLER, -4, 1, 1, EntitySpawnReason.MOB_SUMMONED);
		shielded.setNoAi(true);
		exposed.setNoAi(true);
		helper.assertValueEqual(DomainService.raiseSimple(shielded), CastFailure.NONE, "simple domain raised");
		float shieldedMax = shielded.getHealth();
		float exposedMax = exposed.getHealth();
		ActiveDomain domain = open(helper, caster, "malevolent_shrine", DomainMath.Variant.FULL);
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(exposed.getHealth() < exposedMax, "the exposed curse is cut"))
				.thenExecute(() -> {
					helper.assertTrue(shielded.getHealth() == shieldedMax, "the shielded curse is not");
					helper.assertTrue(domain.enemiesInside().contains(shielded), "though it is inside");
					DomainService.close(caster);
					DomainService.dropSimple(shielded);
				})
				.thenSucceed();
	}

	@GameTest(padding = PAD, maxTicks = 160)
	public void largerPoolWinsTheClash(GameTestHelper helper) {
		ServerPlayer strong = sorcerer(helper, "special_grade");
		ServerPlayer weak = sorcerer(helper, "grade_1");
		Vec3 base = strong.position();
		weak.setPos(base.x + 10, base.y, base.z);
		ActiveDomain big = open(helper, strong, "malevolent_shrine", DomainMath.Variant.FULL);
		ActiveDomain small = open(helper, weak, "sevenfold_recitation", DomainMath.Variant.FULL);
		int winsBefore = Progression.get(strong).counter(ProgressionMath.CLASH_WINS);
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(big.phase() == ActiveDomain.Phase.CLASHING || small.phase() == ActiveDomain.Phase.CLOSING, "they clash"))
				.thenWaitUntil(() -> helper.assertTrue(small.phase() == ActiveDomain.Phase.CLOSING, "the smaller pool's domain collapses"))
				.thenExecute(() -> {
					helper.assertTrue(big.phase() == ActiveDomain.Phase.ACTIVE, "the winner's domain stands");
					helper.assertTrue(weak.hasEffect(JjkEffects.STUNNED), "the loser is stunned");
					helper.assertValueEqual(Progression.get(strong).counter(ProgressionMath.CLASH_WINS), winsBefore + 1, "a clash win is counted");
					DomainService.close(strong);
				})
				.thenSucceed();
	}

	@GameTest(padding = PAD, maxTicks = 120)
	public void floodOverloadsAndStopsRegen(GameTestHelper helper) {
		ServerPlayer caster = sorcerer(helper, "special_grade");
		CurseEntity curse = helper.spawn(CurseEntities.LEDGER_CRAWLER, 4, 1, 4, EntitySpawnReason.MOB_SUMMONED);
		ActiveDomain domain = open(helper, caster, "unlimited_void", DomainMath.Variant.FULL);
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(curse.hasEffect(JjkEffects.OVERLOADED), "overloaded"))
				.thenExecute(() -> {
					helper.assertTrue(CursedEnergy.regenModifier(curse) == 0, "regen stopped inside");
					helper.assertValueEqual(TechniqueService.check(curse, id("grudge_bolt"),
							com.jjkcraft.technique.Techniques.get(helper.getLevel().registryAccess(), id("grudge_bolt")).orElseThrow()),
							CastFailure.STUNNED, "can't cast");
					DomainService.close(caster);
				})
				.thenIdle(1)
				.thenExecute(() -> helper.assertTrue(CursedEnergy.regenModifier(curse) > 0, "regen returns when the domain ends"))
				.thenSucceed();
	}

	@GameTest(padding = PAD, maxTicks = 120)
	public void shadowGardenDogsHuntAndMeltAway(GameTestHelper helper) {
		ServerPlayer caster = sorcerer(helper, "grade_1");
		dummy(helper, 5, 1, 5);
		ActiveDomain domain = open(helper, caster, "chimera_shadow_garden", DomainMath.Variant.FULL);
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(hounds(helper, domain) >= 4, "four shadow Divine Dogs"))
				.thenExecute(() -> DomainService.close(caster))
				.thenIdle(2)
				.thenExecute(() -> helper.assertValueEqual(hounds(helper, domain), 0, "the dogs go with the domain"))
				.thenSucceed();
	}

	private static int hounds(GameTestHelper helper, ActiveDomain domain) {
		return helper.getLevel().getEntitiesOfClass(ShadowDivineDogEntity.class, domain.bounds().inflate(4), h -> h.domainInstance() == domain.id() && h.isAlive()).size();
	}

	@GameTest(padding = PAD, maxTicks = 120)
	public void gambleRollsOutcomes(GameTestHelper helper) {
		ServerPlayer caster = sorcerer(helper, "grade_1");
		dummy(helper, 4, 1, 4);
		ActiveDomain domain = open(helper, caster, "idle_death_gamble", DomainMath.Variant.FULL);
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(domain.memory().get("gamble_last") instanceof Integer, "a roll happened"))
				.thenExecute(() -> DomainService.close(caster))
				.thenSucceed();
	}

	@GameTest(padding = PAD, maxTicks = 120)
	public void arsenalBladesWorkOnlyInside(GameTestHelper helper) {
		ServerPlayer caster = sorcerer(helper, "special_grade");
		ActiveDomain domain = open(helper, caster, "authentic_mutual_love", DomainMath.Variant.FULL);
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(blades(caster) == 4, "four blades forged"))
				.thenExecute(() -> {
					ItemStack blade = firstBlade(caster);
					Identifier loaded = blade.get(JjkComponents.LOADED_TECHNIQUE);
					caster.setItemInHand(InteractionHand.MAIN_HAND, blade.copy());
					helper.assertTrue(TechniqueSources.grants(caster, loaded), "a blade grants its technique inside");
					Vec3 out = domain.center().add(domain.radius() + 2, 0, 0);
					caster.setPos(out.x, out.y, out.z);
					helper.assertFalse(TechniqueSources.grants(caster, loaded), "but not outside");
					caster.setPos(domain.center().x, domain.center().y, domain.center().z);
					DomainService.close(caster);
					helper.assertValueEqual(blades(caster), 0, "blades vanish with the domain");
				})
				.thenSucceed();
	}

	private static int blades(ServerPlayer player) {
		int count = 0;
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			if (player.getInventory().getItem(i).has(JjkComponents.ARSENAL_DOMAIN)) {
				count++;
			}
		}

		return count;
	}

	private static ItemStack firstBlade(ServerPlayer player) {
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			if (player.getInventory().getItem(i).has(JjkComponents.ARSENAL_DOMAIN)) {
				return player.getInventory().getItem(i);
			}
		}

		return ItemStack.EMPTY;
	}

	@GameTest(padding = PAD, maxTicks = 420)
	public void recitationRepeatsExactlySevenTimes(GameTestHelper helper) {
		ServerPlayer caster = sorcerer(helper, "grade_1");
		Zombie target = dummy(helper, 4, 1, 4);
		target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 60, 4, false, false));
		ActiveDomain domain = open(helper, caster, "sevenfold_recitation", DomainMath.Variant.FULL);
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(domain.phase() == ActiveDomain.Phase.CLOSING, "closes itself"))
				.thenExecute(() -> helper.assertValueEqual(RepetitionBehavior.done(domain), 7, "exactly seven recitations"))
				.thenSucceed();
	}
}
