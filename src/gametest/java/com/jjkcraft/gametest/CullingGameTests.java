package com.jjkcraft.gametest;

import java.util.List;
import java.util.Optional;

import com.mojang.serialization.DataResult;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.npc.villager.Villager;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import com.jjkcraft.JjkCraft;
import com.jjkcraft.core.culling.CullingMath;
import com.jjkcraft.core.progress.ProgressionMath;
import com.jjkcraft.culling.CullingGame;
import com.jjkcraft.culling.CullingState;
import com.jjkcraft.culling.entity.EntrantEntity;
import com.jjkcraft.culling.entity.KoganeEntity;
import com.jjkcraft.culling.rule.RuleEvent;
import com.jjkcraft.curse.CurseEntities;
import com.jjkcraft.curse.CurseEntity;
import com.jjkcraft.curse.mechanic.WardenMechanic;
import com.jjkcraft.energy.CursedEnergy;
import com.jjkcraft.network.payload.CullingPayloads;
import com.jjkcraft.sorcerer.Awakening;
import com.jjkcraft.sorcerer.Progression;
import com.jjkcraft.technique.TechniqueService;

/**
 * The Culling Game. Its state is one per server, so the whole loop is a single test played in
 * order: start, the entry window, joining and the barrier, residents, kill points, transfers and
 * reveals, negotiation and its refusals, the Warden bound by a rule and its edict, stagnation,
 * substitution, withdrawal, the missed window, saving and loading, and the end.
 */
public class CullingGameTests {
	private static Identifier id(String path) {
		return JjkCraft.id(path);
	}

	private static ServerPlayer sorcerer(GameTestHelper helper, BlockPos at) {
		ServerPlayer player = TestPlayers.fresh(helper);
		BlockPos pos = helper.absolutePos(at);
		player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
		Awakening.awaken(player, false);
		CursedEnergy.fill(player);
		return player;
	}

	private static void move(GameTestHelper helper, ServerPlayer player, double x, double y, double z) {
		BlockPos origin = helper.absolutePos(BlockPos.ZERO);
		player.setPos(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
	}

	private static void kill(GameTestHelper helper, ServerPlayer killer, Entity victim) {
		ServerLevel level = helper.getLevel();
		victim.hurtServer(level, level.damageSources().playerAttack(killer), 10_000f);
	}

	@GameTest(maxTicks = 400)
	public void cullingGameLoop(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		MinecraftServer server = level.getServer();
		CullingGame.reset(server);

		// Residents: whoever is inside when the barrier rises. (The test player is not in the world,
		// so the scan can't see it; a villager stands in for the scan, and the player is marked by hand.)
		Villager villager = helper.spawn(EntityTypes.VILLAGER, new BlockPos(3, 1, 3));
		ServerPlayer resident = sorcerer(helper, new BlockPos(3, 1, 3));
		helper.assertTrue(CullingGame.start(level, helper.absolutePos(new BlockPos(4, 1, 4)), 1, 6, 0), "the game starts");
		CullingState state = CullingGame.state(server);
		CullingState.Colony colony = state.colonies.getFirst();
		helper.assertValueEqual(state.rules.size(), 9, "the nine base rules are in force");
		helper.assertTrue(colony.residents.contains(villager.getUUID()), "whoever was inside is a resident");
		colony.residents.add(resident.getUUID());

		// The entry window opens on awakening; entering joins and closes it.
		ServerPlayer hunter = sorcerer(helper, new BlockPos(4, 1, 20));
		helper.assertTrue(state.deadlines.containsKey(hunter.getUUID()), "awakening during the game opens an entry window");
		CullingGame.updatePresence(hunter);
		helper.assertTrue(state.participant(hunter.getUUID()) == null, "outside, still not a participant");
		move(helper, hunter, 4.5, 1, 6.5);
		CullingGame.updatePresence(hunter);
		helper.assertTrue(state.active(hunter.getUUID()) != null, "stepping inside makes a participant");
		helper.assertFalse(state.deadlines.containsKey(hunter.getUUID()), "and closes the window");

		// The barrier holds participants; residents come and go.
		move(helper, hunter, 4.5, 1, 14.5);
		CullingGame.updatePresence(hunter);
		double dx = hunter.getX() - (colony.center.getX() + 0.5);
		double dz = hunter.getZ() - (colony.center.getZ() + 0.5);
		helper.assertTrue(Math.sqrt(dx * dx + dz * dz) < colony.radius, "a participant is held inside");
		move(helper, resident, 4.5, 1, 18.5);
		CullingGame.updatePresence(resident);
		helper.assertTrue(state.participant(resident.getUUID()) == null && resident.getZ() > colony.center.getZ() + colony.radius,
				"a resident walks out freely");

		// Kill points: a sorcerer entrant 5, an ordinary one 1.
		EntrantEntity sorcererEntrant = CullingGame.spawnEntrant(level, colony, true);
		EntrantEntity ordinaryEntrant = CullingGame.spawnEntrant(level, colony, false);
		helper.assertTrue(sorcererEntrant != null && ordinaryEntrant != null, "entrants come in");
		helper.assertTrue(state.active(sorcererEntrant.getUUID()) != null && state.participant(sorcererEntrant.getUUID()).sorcerer, "entrants are participants");
		kill(helper, hunter, sorcererEntrant);
		helper.assertValueEqual(state.active(hunter.getUUID()).points, 5, "a sorcerer is worth 5");
		kill(helper, hunter, ordinaryEntrant);
		helper.assertValueEqual(state.active(hunter.getUUID()).points, 6, "anyone else 1");
		helper.assertValueEqual(Progression.get(hunter).counters().getOrDefault(ProgressionMath.CULLING_POINTS, 0), 6, "points feed the trial");
		helper.assertFalse(state.participant(sorcererEntrant.getUUID()).active(), "the fallen entrant is out");

		// Shared Ledger and Open Tally.
		ServerPlayer partner = sorcerer(helper, new BlockPos(5, 1, 5));
		CullingGame.updatePresence(partner);
		helper.assertTrue(CullingGame.request(hunter, RuleEvent.Action.TRANSFER, partner.getUUID(), 5, null, -1), "a transfer is allowed");
		helper.assertValueEqual(state.active(hunter.getUUID()).points, 1, "5 left the giver");
		helper.assertValueEqual(state.active(partner.getUUID()).points, 5, "and reached the partner");
		CullingGame.request(hunter, RuleEvent.Action.TRANSFER, partner.getUUID(), 3, null, -1);
		helper.assertValueEqual(state.active(hunter.getUUID()).points, 1, "transfers move whole steps of 5 only");
		helper.assertTrue(CullingGame.request(partner, RuleEvent.Action.REVEAL, hunter.getUUID(), 0, null, -1), "a reveal is allowed");
		helper.assertValueEqual(state.active(partner.getUUID()).points, 4, "for 1 point");
		CullingPayloads.View seen = CullingGame.view(server, state, partner);
		helper.assertTrue(seen.board().stream().anyMatch(e -> e.id().equals(hunter.getUUID()) && e.known() && e.pos().isPresent()),
				"the revealed participant shows with a position");
		CullingPayloads.View outsider = CullingGame.view(server, state, resident);
		helper.assertTrue(outsider.board().stream().noneMatch(CullingPayloads.Entry::known), "outsiders see no names");

		CurseEntity[] sent = new CurseEntity[1];
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(colony.master != null && level.getEntity(colony.master) instanceof KoganeEntity,
						"the Kogane takes its place"))
				.thenExecute(() -> {
					// Negotiation: refusals first, then a rule added.
					CullingGame.addPoints(server, hunter.getUUID(), 299, null);
					Entity master = level.getEntity(colony.master);
					hunter.setPos(master.getX() + 1.5, master.getY() - 1.5, master.getZ());
					helper.assertTrue(CullingGame.nearMaster(hunter), "beside the Kogane");
					CullingGame.request(hunter, RuleEvent.Action.NEGOTIATE, null, 0, id("one_way_entry"), -1);
					helper.assertValueEqual(state.active(hunter.getUUID()).points, 300, "a base rule can't be bought");
					helper.assertTrue(CullingGame.request(hunter, RuleEvent.Action.NEGOTIATE, null, 0, id("night_hunt"), -1), "negotiation is allowed");
					helper.assertTrue(state.hasRule(id("night_hunt")), "the rule is in force");
					helper.assertValueEqual(state.active(hunter.getUUID()).points, 200, "for 100 points");
					CullingGame.request(hunter, RuleEvent.Action.NEGOTIATE, null, 0, id("night_truce"), -1);
					helper.assertFalse(state.hasRule(id("night_truce")), "a conflicting rule is refused");
					CullingGame.request(hunter, RuleEvent.Action.NEGOTIATE, null, 0, id("night_hunt"), -1);
					helper.assertValueEqual(state.active(hunter.getUUID()).points, 200, "nor bought twice");

					// The Tally Ledger keeps points while a rule lets them change hands.
					net.minecraft.world.item.ItemStack ledger = new net.minecraft.world.item.ItemStack(com.jjkcraft.item.JjkItems.TALLY_LEDGER);
					hunter.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ledger);
					ledger.getItem().use(level, hunter, net.minecraft.world.InteractionHand.MAIN_HAND);
					helper.assertValueEqual(state.active(hunter.getUUID()).points, 195, "5 points entered in the ledger");
					helper.assertValueEqual(ledger.getOrDefault(com.jjkcraft.item.JjkComponents.LEDGER_POINTS, 0), 5, "the ledger holds them");
					hunter.setShiftKeyDown(true);
					ledger.getItem().use(level, hunter, net.minecraft.world.InteractionHand.MAIN_HAND);
					hunter.setShiftKeyDown(false);
					helper.assertValueEqual(state.active(hunter.getUUID()).points, 200, "and gives them back");

					// The Warden's Mandate: a Warden arrives and can't fall while the rule stands.
					helper.assertTrue(CullingGame.request(hunter, RuleEvent.Action.NEGOTIATE, null, 0, id("warden_mandate"), -1), "the mandate is added");
					List<CurseEntity> wardens = level.getEntitiesOfClass(CurseEntity.class, new net.minecraft.world.phys.AABB(colony.center).inflate(12),
							c -> c.getType() == CurseEntities.CULLING_WARDEN);
					helper.assertFalse(wardens.isEmpty(), "a Warden was sent in");
					CurseEntity warden = wardens.getFirst();
					sent[0] = warden;
					helper.assertTrue(WardenMechanic.bound(warden, level), "the rule binds it");
					long now = CullingGame.now(server);
					state.rules.add(new CullingState.ActiveRule(id("archer_law"), Optional.empty(), now, now + 2 * CullingMath.DAY));
					state.changed();
				})
				.thenWaitUntil(() -> {
					CurseEntity warden = sent[0];
					helper.assertTrue(warden != null, "no Warden was sent");
					helper.assertFalse(warden.isRemoved(), "the Warden was removed: " + warden.getRemovalReason() + " at " + warden.position()
							+ ", health " + warden.getHealth());
					helper.assertValueEqual(WardenMechanic.edict(warden), WardenMechanic.Edict.DISTANCE, "the Law of the Bow names its weak point");
				})
				.thenExecute(() -> {
					level.getEntitiesOfClass(CurseEntity.class, new net.minecraft.world.phys.AABB(colony.center).inflate(16),
							c -> c.getType() == CurseEntities.CULLING_WARDEN).forEach(Entity::discard);

					// Stagnation: three days without scoring halves the pool until the next score.
					double full = CursedEnergy.max(partner);
					CullingGame.advance(server, 3);
					CullingGame.runClock(server);
					helper.assertTrue(state.active(partner.getUUID()).energyCap < 1, "stalled");
					helper.assertTrue(Math.abs(CursedEnergy.max(partner) - full / 2) < 1e-6, "cursed energy capped at half");
					CullingGame.addPoints(server, partner.getUUID(), 1, null);
					helper.assertTrue(Math.abs(CursedEnergy.max(partner) - full) < 1e-6, "scoring lifts the cap");
					helper.assertFalse(state.hasRule(id("warden_mandate")), "the one-day mandate lapsed");

					// Substitution: 30 points to change colonies; the old one gets a newcomer.
					CullingState.Colony second = CullingGame.raise(level, helper.absolutePos(new BlockPos(4, 1, 40)), 6, null);
					int pending = colony.pending;
					helper.assertTrue(CullingGame.request(hunter, RuleEvent.Action.SUBSTITUTE, null, 0, null, second.id), "substitution is allowed");
					helper.assertValueEqual(state.active(hunter.getUUID()).colony, second.id, "moved");
					helper.assertValueEqual(colony.pending, pending + 1, "a newcomer is owed to the old colony");
					helper.assertTrue(second.contains(level.dimension(), hunter.getX(), hunter.getY(), hunter.getZ()), "and brought inside the new one");

					// Withdrawal needs its rule.
					helper.assertFalse(CullingGame.request(partner, RuleEvent.Action.WITHDRAW, null, 0, null, -1), "no rule allows withdrawing yet");
					state.rules.add(new CullingState.ActiveRule(id("withdrawal"), Optional.empty(), CullingGame.now(server), CullingState.ActiveRule.NEVER));
					CullingGame.addPoints(server, partner.getUUID(), 100, null);
					helper.assertTrue(CullingGame.request(partner, RuleEvent.Action.WITHDRAW, null, 0, null, -1), "with it, a participant may leave");
					helper.assertValueEqual(state.participant(partner.getUUID()).status, CullingState.Status.WITHDRAWN, "withdrawn");

					// The missed window takes the technique.
					ServerPlayer late = sorcerer(helper, new BlockPos(4, 1, 30));
					Identifier innate = TechniqueService.get(late).innate().orElse(null);
					helper.assertTrue(innate != null, "an innate technique to lose");
					CullingGame.advance(server, 19);
					CullingGame.checkDeadline(late);
					helper.assertTrue(TechniqueService.get(late).innate().isEmpty() && !TechniqueService.knows(late, innate), "the technique is gone");

					// Saving and loading keep everything.
					DataResult<Tag> saved = CullingState.CODEC.encodeStart(NbtOps.INSTANCE, state);
					CullingState loaded = CullingState.CODEC.parse(NbtOps.INSTANCE, saved.getOrThrow()).getOrThrow();
					helper.assertValueEqual(loaded.participants.size(), state.participants.size(), "participants survive a save");
					helper.assertValueEqual(loaded.rules.size(), state.rules.size(), "rules survive a save");
					helper.assertValueEqual(loaded.active(hunter.getUUID()).points, state.active(hunter.getUUID()).points, "points survive a save");
					helper.assertValueEqual(loaded.colonies.size(), 2, "colonies survive a save");

					// The end: standings kept, barriers down, entrants gone.
					CullingGame.end(server, "stopped");
					helper.assertFalse(state.running, "the game is over");
					helper.assertFalse(state.results.isEmpty(), "standings were kept");
					helper.assertTrue(level.getEntitiesOfClass(EntrantEntity.class, new net.minecraft.world.phys.AABB(colony.center).inflate(16)).isEmpty(),
							"entrants left with the barriers");
					CullingGame.reset(server);
				})
				.thenSucceed();
	}

	@GameTest
	public void everyRuleLoadsAndTheBaseRulesNumberNine(GameTestHelper helper) {
		List<CullingGame.RuleEntry> rules = CullingGame.allRules(helper.getLevel().getServer());
		helper.assertTrue(rules.size() >= 19, "19 rules load, found " + rules.size());
		helper.assertValueEqual((int) rules.stream().filter(r -> r.rule().base()).count(), 9, "nine base rules");
		for (CullingGame.RuleEntry entry : rules) {
			for (Identifier conflict : entry.rule().conflicts()) {
				helper.assertTrue(CullingGame.rule(helper.getLevel().getServer(), conflict).isPresent(), entry.id() + " names a real rule");
			}
		}

		helper.succeed();
	}
}
