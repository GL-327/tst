package com.jjkcraft.gametest;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import com.jjkcraft.JjkCraft;
import com.jjkcraft.curse.CurseEntities;
import com.jjkcraft.curse.CurseEntity;
import com.jjkcraft.effect.JjkEffects;
import com.jjkcraft.energy.CursedEnergy;
import com.jjkcraft.entity.shikigami.DivineDogEntity;
import com.jjkcraft.entity.shikigami.NueEntity;
import com.jjkcraft.entity.shikigami.ShikigamiEntity;
import com.jjkcraft.entity.shikigami.ToadEntity;
import com.jjkcraft.entity.CursedBoltEntity;
import com.jjkcraft.item.CursedFingerItem;
import com.jjkcraft.item.DriftingGourdItem;
import com.jjkcraft.item.StrawDollLink;
import com.jjkcraft.item.JjkComponents;
import com.jjkcraft.item.JjkItems;
import com.jjkcraft.item.TechniqueScrollItem;
import com.jjkcraft.item.tool.CursedToolDefinition;
import com.jjkcraft.item.tool.CursedTools;
import com.jjkcraft.registry.JjkRegistries;
import com.jjkcraft.sorcerer.Awakening;
import com.jjkcraft.sorcerer.Progression;
import com.jjkcraft.technique.CastFailure;
import com.jjkcraft.technique.TechniqueCategory;
import com.jjkcraft.technique.TechniqueService;
import com.jjkcraft.technique.TechniqueSources;
import com.jjkcraft.technique.Techniques;
import com.jjkcraft.vow.Vows;

/** M8: the fifteen new techniques (Ten Shadows, Straw Doll, Idle Transfiguration, Projection Sorcery, ultimates and the rest) and the cursed tools. */
public class ArsenalGameTests {
	private static Identifier id(String path) {
		return JjkCraft.id(path);
	}

	/** A sorcerer of {@code grade} at (1, 1, 1) facing +Z, full CE, knowing {@code technique}. */
	private static ServerPlayer caster(GameTestHelper helper, String grade, String technique) {
		ServerPlayer player = TestPlayers.fresh(helper);
		BlockPos base = helper.absolutePos(new BlockPos(1, 1, 1));
		player.setPos(base.getX() + 0.5, base.getY(), base.getZ() + 0.5);
		player.setYRot(0);
		player.setXRot(0);
		player.setYHeadRot(0);
		Awakening.awaken(player, false);
		Progression.setGrade(player, id(grade));
		TechniqueService.grant(player, id(technique));
		CursedEnergy.fill(player);
		return player;
	}

	private static Zombie dummy(GameTestHelper helper, double x, double z) {
		Zombie zombie = helper.spawn(EntityTypes.ZOMBIE, new Vec3(x, 1, z));
		zombie.setNoAi(true);
		return zombie;
	}

	@GameTest
	public void thirtyTechniquesAcrossEveryCategory(GameTestHelper helper) {
		List<Techniques.Entry> all = Techniques.all(helper.getLevel().registryAccess());
		helper.assertTrue(all.size() >= 30, "30 techniques, found " + all.size());
		Set<TechniqueCategory> categories = EnumSet.noneOf(TechniqueCategory.class);
		long ultimates = 0;
		for (Techniques.Entry entry : all) {
			categories.add(entry.definition().category());
			if (entry.definition().category() == TechniqueCategory.ULTIMATE) {
				ultimates++;
			}
		}

		helper.assertValueEqual((int) ultimates, 3, "three ultimates");
		for (TechniqueCategory category : TechniqueCategory.values()) {
			if (category != TechniqueCategory.DOMAIN) {
				helper.assertTrue(categories.contains(category), "a technique in " + category);
			}
		}

		var tools = helper.getLevel().registryAccess().lookupOrThrow(JjkRegistries.CURSED_TOOL);
		helper.assertTrue(tools.size() >= 8, "cursed tool data loads");
		for (CursedToolDefinition tool : tools) {
			helper.assertTrue(BuiltInRegistries.ITEM.containsKey(tool.item()), tool.item() + " is an item");
			tool.technique().ifPresent(t -> helper.assertTrue(Techniques.get(helper.getLevel().registryAccess(), t).isPresent(), t + " exists"));
		}

		helper.succeed();
	}

	@GameTest(maxTicks = 200)
	public void tenShadowsDivineDogsSendsTwoHoundsAfterYourTarget(GameTestHelper helper) {
		ServerPlayer player = caster(helper, "grade_3", "ten_shadows_divine_dogs");
		Zombie prey = dummy(helper, 4.5, 5.5);
		helper.assertValueEqual(TechniqueService.cast(player, id("ten_shadows_divine_dogs"), -1), CastFailure.NONE, "cast");
		List<DivineDogEntity> hounds = ShikigamiEntity.ownedBy(helper.getLevel(), DivineDogEntity.class, player);
		helper.assertValueEqual(hounds.size(), 2, "two Divine Dogs");
		TechniqueService.resetCooldowns(player);
		TechniqueService.cast(player, id("ten_shadows_divine_dogs"), -1);
		helper.assertValueEqual(ShikigamiEntity.ownedBy(helper.getLevel(), DivineDogEntity.class, player).size(), 2, "calling again replaces them");
		player.setLastHurtMob(prey);
		float max = prey.getHealth();
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(prey.getHealth() < max, "the Divine Dogs flank and bite"))
				.thenSucceed();
	}

	@GameTest(maxTicks = 200)
	public void nueMarksAndDives(GameTestHelper helper) {
		ServerPlayer player = caster(helper, "grade_4", "ten_shadows_nue");
		Zombie prey = dummy(helper, 5.5, 5.5);
		TechniqueService.cast(player, id("ten_shadows_nue"), -1);
		helper.assertValueEqual(ShikigamiEntity.ownedBy(helper.getLevel(), NueEntity.class, player).size(), 1, "one Nue");
		player.setLastHurtMob(prey);
		float max = prey.getHealth();
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(prey.hasEffect(net.minecraft.world.effect.MobEffects.GLOWING), "the target is marked"))
				.thenWaitUntil(() -> helper.assertTrue(prey.getHealth() < max, "and Nue dives at it"))
				.thenSucceed();
	}

	@GameTest(maxTicks = 100)
	public void toadSwatsArrowsAndDrawsAnger(GameTestHelper helper) {
		ServerPlayer player = caster(helper, "grade_2", "ten_shadows_toad");
		TechniqueService.cast(player, id("ten_shadows_toad"), -1);
		ToadEntity toad = ShikigamiEntity.ownedBy(helper.getLevel(), ToadEntity.class, player).getFirst();
		Zombie angry = helper.spawn(EntityTypes.ZOMBIE, new Vec3(4.5, 1, 4.5));
		angry.setTarget(player);
		Arrow arrow = new Arrow(EntityTypes.ARROW, helper.getLevel());
		arrow.setOwner(angry);
		arrow.setPos(player.getX(), player.getY() + 1, player.getZ() + 3);
		arrow.setDeltaMovement(0, 0, -0.5);
		arrow.setNoGravity(true);
		helper.getLevel().addFreshEntity(arrow);
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(arrow.isRemoved(), "the arrow is swatted down"))
				.thenWaitUntil(() -> helper.assertTrue(angry.getTarget() == toad, "the zombie turns on the Toad"))
				.thenSucceed();
	}

	@GameTest
	public void strawDollResonanceHurtsTheBoundTargetFromAfar(GameTestHelper helper) {
		ServerPlayer player = caster(helper, "grade_3", "straw_doll_resonance");
		Zombie bound = dummy(helper, 1.5, 6.5);
		helper.assertValueEqual(TechniqueService.cast(player, id("straw_doll_resonance"), -1), CastFailure.NONE, "cast");
		ItemStack doll = player.getInventory().getNonEquipmentItems().stream().filter(s -> s.is(JjkItems.STRAW_DOLL)).findFirst().orElse(ItemStack.EMPTY);
		helper.assertFalse(doll.isEmpty(), "a straw doll in hand");
		StrawDollLink link = doll.get(JjkComponents.STRAW_DOLL_LINK);
		helper.assertTrue(link != null && link.target().equals(bound.getUUID()) && link.nails() == 3, "bound to the zombie, three nails");
		player.setItemInHand(InteractionHand.MAIN_HAND, doll);
		float max = bound.getHealth();
		double ce = CursedEnergy.current(player);
		doll.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
		helper.assertTrue(bound.getHealth() < max, "a nail hurts the bound zombie");
		helper.assertTrue(CursedEnergy.current(player) < ce, "for CE");
		helper.assertValueEqual(player.getMainHandItem().get(JjkComponents.STRAW_DOLL_LINK).nails(), 2, "two nails left");
		helper.succeed();
	}

	@GameTest
	public void idleTransfigurationTradesOneStatForAnother(GameTestHelper helper) {
		ServerPlayer player = caster(helper, "grade_3", "idle_transfiguration");
		TechniqueService.cast(player, id("idle_transfiguration"), -1);
		helper.assertTrue(player.hasEffect(JjkEffects.BULKED), "aimed at nothing, the caster bulks up");
		helper.assertTrue(player.getAttributeValue(Attributes.SCALE) > 1.2 && player.getAttributeValue(Attributes.ARMOR) >= 6, "bigger and armored");
		Zombie target = dummy(helper, 1.5, 4.5);
		TechniqueService.resetCooldowns(player);
		TechniqueService.cast(player, id("idle_transfiguration"), -1);
		helper.assertTrue(target.hasEffect(JjkEffects.SHRUNK) && target.getAttributeValue(Attributes.SCALE) < 0.7, "aimed at the zombie, it shrinks");
		helper.succeed();
	}

	@GameTest(maxTicks = 60)
	public void projectionSorceryFreezesEnemiesAfterItsTell(GameTestHelper helper) {
		ServerPlayer player = caster(helper, "grade_2", "projection_sorcery");
		Zombie near = dummy(helper, 3.5, 3.5);
		double startZ = player.getZ();
		TechniqueService.cast(player, id("projection_sorcery"), -1);
		helper.assertFalse(near.hasEffect(JjkEffects.FROZEN_FRAME), "nothing happens during the windup");
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(near.hasEffect(JjkEffects.FROZEN_FRAME), "then the enemy loses the moment"))
				.thenExecute(() -> helper.assertTrue(player.getZ() > startZ + 3, "while the caster steps forward"))
				.thenSucceed();
	}

	@GameTest(maxTicks = 80)
	public void piercingBloodCutsTheLine(GameTestHelper helper) {
		ServerPlayer player = caster(helper, "semi_grade_1", "piercing_blood");
		Zombie far = dummy(helper, 1.5, 7.5);
		float max = far.getHealth();
		helper.assertValueEqual(TechniqueService.cast(player, id("piercing_blood"), -1), CastFailure.NONE, "cast");
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(far.getHealth() < max - 10, "the line cuts deep"))
				.thenSucceed();
	}

	@GameTest(maxTicks = 100)
	public void thousandLancesRainOnTheMark(GameTestHelper helper) {
		ServerPlayer player = caster(helper, "grade_1", "thousand_lances");
		Zombie marked = dummy(helper, 1.5, 6.5);
		float max = marked.getHealth();
		TechniqueService.cast(player, id("thousand_lances"), -1);
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(marked.getHealth() < max, "lances land on the mark"))
				.thenSucceed();
	}

	@GameTest(maxTicks = 100)
	public void maximumOutputBluePullsThenBursts(GameTestHelper helper) {
		ServerPlayer player = caster(helper, "special_grade", "maximum_output_blue");
		Zombie drawn = helper.spawn(EntityTypes.ZOMBIE, new Vec3(5.5, 1, 6.5));
		float max = drawn.getHealth();
		Vec3 start = drawn.position();
		TechniqueService.cast(player, id("maximum_output_blue"), -1);
		helper.startSequence()
				.thenIdle(20)
				.thenExecute(() -> helper.assertTrue(drawn.position().distanceTo(start) > 0.5 || drawn.isRemoved(), "the star drags it in"))
				.thenWaitUntil(() -> helper.assertTrue(drawn.isRemoved() || drawn.getHealth() < max - 15, "then bursts"))
				.thenSucceed();
	}

	@GameTest
	public void oathboundEdgeFollowsTheVow(GameTestHelper helper) {
		ServerPlayer player = caster(helper, "grade_2", "oathbound_edge");
		Zombie first = dummy(helper, 1.5, 3.5);
		float before = first.getHealth();
		TechniqueService.cast(player, id("oathbound_edge"), -1);
		float unsworn = before - first.getHealth();
		first.discard();
		Vows.swear(player, List.of(id("half_vessel"), id("bare_skin")), com.jjkcraft.core.vow.VowMath.Focus.EFFICIENCY,
				com.jjkcraft.core.vow.VowMath.Term.DAY);
		Zombie second = dummy(helper, 1.5, 3.5);
		before = second.getHealth();
		TechniqueService.resetCooldowns(player);
		CursedEnergy.fill(player);
		TechniqueService.cast(player, id("oathbound_edge"), -1);
		float sworn = before - second.getHealth();
		helper.assertTrue(sworn > unsworn * 2, "sworn on a heavy vow it cuts far deeper: " + unsworn + " -> " + sworn);
		helper.succeed();
	}

	@GameTest
	public void inkStepSlipsForward(GameTestHelper helper) {
		ServerPlayer player = caster(helper, "grade_4", "ink_step");
		double z = player.getZ();
		TechniqueService.cast(player, id("ink_step"), -1);
		helper.assertTrue(player.getZ() > z + 4, "the caster slipped forward, from " + z + " to " + player.getZ());
		helper.succeed();
	}

	@GameTest
	public void splinterVolleyFansFiveBolts(GameTestHelper helper) {
		ServerPlayer player = caster(helper, "grade_3", "splinter_volley");
		TechniqueService.cast(player, id("splinter_volley"), -1);
		List<CursedBoltEntity> bolts = helper.getLevel().getEntitiesOfClass(CursedBoltEntity.class, player.getBoundingBox().inflate(4));
		helper.assertValueEqual(bolts.size(), 5, "five bolts");
		double minX = bolts.stream().mapToDouble(b -> b.getDeltaMovement().x).min().orElse(0);
		double maxX = bolts.stream().mapToDouble(b -> b.getDeltaMovement().x).max().orElse(0);
		helper.assertTrue(maxX - minX > 0.4, "fanned out");
		helper.succeed();
	}

	@GameTest
	public void groundTollThrowsEveryoneAround(GameTestHelper helper) {
		ServerPlayer player = caster(helper, "grade_2", "ground_toll");
		Zombie a = helper.spawn(EntityTypes.ZOMBIE, new Vec3(3.5, 1, 1.5));
		Zombie b = helper.spawn(EntityTypes.ZOMBIE, new Vec3(1.5, 1, 4.5));
		float max = a.getMaxHealth();
		TechniqueService.cast(player, id("ground_toll"), -1);
		helper.assertTrue(a.getHealth() < max && b.getHealth() < max, "both hurt");
		helper.assertTrue(a.getDeltaMovement().y > 0.3 && b.getDeltaMovement().y > 0.3, "and thrown up");
		helper.succeed();
	}

	@GameTest(maxTicks = 60)
	public void emberSkinBurnsWhoeverStrikes(GameTestHelper helper) {
		CurseEntity wearer = helper.spawn(CurseEntities.LEDGER_CRAWLER, new Vec3(2.5, 1, 2.5));
		wearer.getEntityData().set(CurseEntity.EMERGING, 0);
		CursedEnergy.fill(wearer);
		helper.assertValueEqual(TechniqueService.cast(wearer, id("ember_skin"), -1), CastFailure.NONE, "the curse raises ember skin");
		Zombie striker = helper.spawn(EntityTypes.ZOMBIE, new Vec3(3.5, 1, 2.5));
		helper.startSequence()
				.thenIdle(2)
				.thenExecute(() -> {
					float max = striker.getHealth();
					striker.doHurtTarget(helper.getLevel(), wearer);
					helper.assertTrue(striker.getRemainingFireTicks() > 0 && striker.getHealth() < max, "the striker is burned back");
				})
				.thenSucceed();
	}

	@GameTest
	public void positiveEnergyOutputHealsTheAllyInSight(GameTestHelper helper) {
		// The test player isn't in the world, so the ally is the healer's own summon.
		ServerPlayer healer = caster(helper, "grade_2", "positive_energy_output");
		ToadEntity ally = helper.spawn(com.jjkcraft.entity.JjkEntities.TOAD, new Vec3(1.5, 1, 4.5));
		ally.bind(healer, 600, 1.0);
		ally.setNoAi(true);
		ally.setHealth(10);
		TechniqueService.cast(healer, id("positive_energy_output"), -1);
		helper.assertTrue(ally.getHealth() >= 17, "the ally is mended, at " + ally.getHealth());
		helper.succeed();
	}

	@GameTest
	public void cursedWeaponsLendTheirTechniqueAtTheirGrade(GameTestHelper helper) {
		ServerPlayer player = caster(helper, "grade_4", "ink_iron_palm");
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JjkItems.SPLIT_SOUL_KATANA));
		helper.assertFalse(TechniqueSources.grants(player, id("dismantle")), "a Grade 4 holds just a blade");
		Progression.setGrade(player, id("grade_1"));
		helper.assertTrue(TechniqueSources.grants(player, id("dismantle")), "a Grade 1 is lent Dismantle");
		var def = Techniques.get(helper.getLevel().registryAccess(), id("dismantle")).orElseThrow();
		double cost = TechniqueService.effectiveCost(player, id("dismantle"), def);
		helper.assertTrue(Math.abs(cost - def.cast().cost() * 0.75) < 0.5, "at a quarter off, cost " + cost);

		CurseEntity curse = helper.spawn(CurseEntities.LEDGER_CRAWLER, new Vec3(3.5, 1, 3.5));
		CursedEnergy.fill(curse);
		Progression.setGrade(player, id("grade_3"));
		CursedEnergy.set(player, 10);
		double curseBefore = CursedEnergy.current(curse);
		ItemStack cleaver = new ItemStack(JjkItems.DEBT_CLEAVER);
		cleaver.getItem().postHurtEnemy(cleaver, curse, player);
		helper.assertTrue(CursedEnergy.current(curse) < curseBefore && CursedEnergy.current(player) > 10, "the cleaver collects a debt");
		helper.succeed();
	}

	@GameTest
	public void witheredDigitGrantsAndUnsettles(GameTestHelper helper) {
		ServerPlayer player = caster(helper, "grade_2", "ink_iron_palm");
		double before = CursedEnergy.max(player);
		int known = TechniqueService.get(player).learned().size();
		CursedFingerItem.swallow(player, helper.getLevel(), CursedTools.get(player, new ItemStack(JjkItems.WITHERED_DIGIT)));
		helper.assertTrue(TechniqueService.get(player).learned().size() > known, "a technique takes root");
		helper.assertTrue(player.hasEffect(JjkEffects.UNSETTLED), "and it unsettles");
		helper.assertTrue(Math.abs(CursedEnergy.max(player) - before * 0.7) < 1e-3, "cursed energy runs thin");
		helper.succeed();
	}

	@GameTest
	public void driftingGourdCarriesAndSetsDown(GameTestHelper helper) {
		ServerPlayer player = caster(helper, "grade_3", "ink_iron_palm");
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JjkItems.DRIFTING_GOURD));
		player.getMainHandItem().getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
		helper.assertTrue(DriftingGourdItem.aloft(player) && player.getAbilities().mayfly, "the cloud carries its holder");
		DriftingGourdItem.land(player, "message.jjkcraft.gourd.corked");
		helper.assertFalse(player.getAbilities().mayfly, "corked, it sets them down");
		helper.assertTrue(player.hasEffect(net.minecraft.world.effect.MobEffects.SLOW_FALLING), "gently");
		helper.succeed();
	}

	@GameTest
	public void techniqueScrollsTeachWithinGrade(GameTestHelper helper) {
		ServerPlayer player = caster(helper, "grade_4", "ink_iron_palm");
		ItemStack scroll = TechniqueScrollItem.of(id("ground_toll"));
		player.setItemInHand(InteractionHand.MAIN_HAND, scroll);
		scroll.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
		helper.assertFalse(TechniqueService.knows(player, id("ground_toll")), "a Grade 4 can't read a Grade 2 scroll");
		Progression.setGrade(player, id("grade_2"));
		scroll.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
		helper.assertTrue(TechniqueService.knows(player, id("ground_toll")), "a Grade 2 learns it");
		helper.assertTrue(player.getMainHandItem().isEmpty(), "the scroll is used up");
		helper.succeed();
	}

	@GameTest
	public void cursedSpeechCrumbleAwayForcesTargetsDown(GameTestHelper helper) {
		ServerPlayer player = caster(helper, "grade_3", "cursed_speech_crumble_away");
		Zombie target = dummy(helper, 1.5, 4.5);
		float max = target.getHealth();
		helper.assertValueEqual(TechniqueService.cast(player, id("cursed_speech_crumble_away"), -1), CastFailure.NONE, "cast");
		helper.assertTrue(target.hasEffect(JjkEffects.BOUND) && target.getHealth() < max, "the target is forced down, hurt and held");
		helper.succeed();
	}

	@GameTest
	public void cursedSpeechBlastAwayThrowsTargetsUp(GameTestHelper helper) {
		ServerPlayer player = caster(helper, "grade_3", "cursed_speech_blast_away");
		Zombie target = dummy(helper, 1.5, 4.5);
		helper.assertValueEqual(TechniqueService.cast(player, id("cursed_speech_blast_away"), -1), CastFailure.NONE, "cast");
		helper.assertTrue(target.getDeltaMovement().y > 0.5, "the target is made to leap, at " + target.getDeltaMovement().y);
		helper.succeed();
	}

	@GameTest(maxTicks = 30)
	public void seekingBoltCurvesOntoATargetOffTheLine(GameTestHelper helper) {
		ServerPlayer player = caster(helper, "grade_3", "seeking_grudge_bolt");
		// Two blocks to the side of the line of fire, five and a half ahead: a straight bolt would miss it.
		Zombie aside = dummy(helper, 3.5, 7.0);
		float max = aside.getHealth();
		helper.assertValueEqual(TechniqueService.cast(player, id("seeking_grudge_bolt"), -1), CastFailure.NONE, "cast");
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(aside.getHealth() < max, "the bolt curves onto it"))
				.thenSucceed();
	}

	@GameTest(maxTicks = 40)
	public void infinitySlowsIncomingProjectiles(GameTestHelper helper) {
		ServerPlayer player = caster(helper, "grade_3", "infinity");
		helper.assertValueEqual(TechniqueService.cast(player, id("infinity"), -1), CastFailure.NONE, "the veil goes up");
		Zombie archer = dummy(helper, 1.5, 6.5);
		Arrow arrow = new Arrow(EntityTypes.ARROW, helper.getLevel());
		arrow.setOwner(archer);
		arrow.setPos(player.getX(), player.getY() + 1, player.getZ() + 3);
		arrow.setDeltaMovement(0, 0, -1.0);
		arrow.setNoGravity(true);
		helper.getLevel().addFreshEntity(arrow);
		helper.startSequence()
				.thenIdle(2)
				.thenExecute(() -> helper.assertTrue(arrow.isRemoved() || arrow.getDeltaMovement().length() < 0.8,
						"the arrow slows in the veil, at " + arrow.getDeltaMovement().length()))
				.thenSucceed();
	}
}
