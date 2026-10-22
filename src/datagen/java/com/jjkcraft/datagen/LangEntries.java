package com.jjkcraft.datagen;

import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider.TranslationBuilder;

/** Hand-written English strings, grouped by system. Each milestone adds a section. */
final class LangEntries {
	private LangEntries() {
	}

	static void all(TranslationBuilder b) {
		energy(b);
		grades(b);
		sounds(b);
		progression(b);
		screens(b);
		keys(b);
		techniques(b);
		curses(b);
		domains(b);
		sorcery(b);
		culling(b);
		tools(b);
	}

	private static void technique(TranslationBuilder b, String id, String name, String description) {
		b.add("technique.jjkcraft." + id, name);
		b.add("technique.jjkcraft." + id + ".desc", description);
	}

	private static void techniques(TranslationBuilder b) {
		technique(b, "ink_iron_palm", "Ink-Iron Palm", "A palm strike sheathed in cursed energy. Timed perfectly, it can become a Black Flash.");
		technique(b, "grudge_bolt", "Grudge Bolt", "A fast bolt of condensed cursed energy.");
		technique(b, "seeking_grudge_bolt", "Seeking Grudge Bolt", "A slower bolt that bends toward the nearest enemy ahead.");
		technique(b, "cleave", "Cleave", "A wide cutting arc. Cuts deeper into those who carry more cursed energy.");
		technique(b, "dismantle", "Dismantle", "A single thin cut along a line. Part of it passes through armor.");
		technique(b, "lapse_blue", "Cursed Technique Lapse: Blue", "Converges space toward a point, dragging enemies in. Meets Reversal: Red in a burst.");
		technique(b, "reversal_red", "Cursed Technique Reversal: Red", "Diverges space away from a point, throwing enemies back. Meets Lapse: Blue in a burst.");
		technique(b, "hollow_purple", "Hollow Purple", "Hollow Technique: Purple. Blue and Red forced together into a slow imaginary mass that erases what it passes.");
		technique(b, "cursed_speech_dont_move", "Cursed Speech: Don't Move", "A cursed word that freezes everyone in earshot. Commanding the strong strains your throat.");
		technique(b, "cursed_speech_crumble_away", "Cursed Speech: Crumble Away", "A cursed word that slams listeners to the ground.");
		technique(b, "cursed_speech_blast_away", "Cursed Speech: Blast Away", "A cursed word that throws listeners into the air.");
		technique(b, "reverse_cursed_technique", "Reverse Cursed Technique", "Hold to turn cursed energy into positive energy that heals you. Glows while it works.");
		technique(b, "grave_chains", "Grave Chains", "Cursed chains that pin the target where it stands.");
		technique(b, "lacquer_ward", "Lacquer Ward", "Hold to raise a barrier in front of you. Blocked hits cost cursed energy.");
		technique(b, "infinity", "Infinity", "A passive field that slows projectiles flying at you. Toggle it on and off.");

		for (String[] c : new String[][] {{"strike", "Strike"}, {"projectile", "Projectile"}, {"area", "Area"}, {"restriction", "Restriction"},
				{"summon", "Summon"}, {"domain", "Domain"}, {"passive", "Passive"}, {"healing", "Healing"}, {"utility", "Utility"}, {"ultimate", "Ultimate"}}) {
			b.add("technique_category.jjkcraft." + c[0], c[1]);
		}

		b.add("cast_failure.jjkcraft.none", "");
		b.add("cast_failure.jjkcraft.empty_slot", "Empty slot");
		b.add("cast_failure.jjkcraft.not_known", "Not learned");
		b.add("cast_failure.jjkcraft.grade_too_low", "Grade too low");
		b.add("cast_failure.jjkcraft.on_cooldown", "Cooling down");
		b.add("cast_failure.jjkcraft.not_enough_energy", "Not enough CE");
		b.add("cast_failure.jjkcraft.heavenly_restriction", "No cursed energy");
		b.add("cast_failure.jjkcraft.stunned", "Stunned");
		b.add("cast_failure.jjkcraft.disabled", "Disabled here");
		b.add("cast_failure.jjkcraft.busy", "Busy");

		b.add("message.jjkcraft.innate", "Your innate technique: %s");
		b.add("message.jjkcraft.mastered", "Mastered %s");
		b.add("tooltip.jjkcraft.cost", "CE cost: %s");
		b.add("tooltip.jjkcraft.upkeep", "Upkeep: %s CE/s");
		b.add("tooltip.jjkcraft.cooldown", "Cooldown: %ss");
		b.add("tooltip.jjkcraft.cast_time", "Cast time: %ss");
		b.add("tooltip.jjkcraft.grade", "Requires: %s");
		b.add("tooltip.jjkcraft.mode.instant", "Press to cast");
		b.add("tooltip.jjkcraft.mode.channel", "Hold to channel");
		b.add("tooltip.jjkcraft.mode.toggle", "Press to toggle");
		b.add("tooltip.jjkcraft.mastery", "Mastery: %s/%s");
		b.add("tooltip.jjkcraft.mastered", "Mastery milestone reached");
		b.add("hud.jjkcraft.cast_hint", "[%s]");
		b.add("screen.jjkcraft.loadout", "Techniques");
		b.add("screen.jjkcraft.loadout.known", "Known techniques (%s)");
		b.add("screen.jjkcraft.loadout.slots", "Hotbar");
		b.add("screen.jjkcraft.loadout.slot", "Slot %s");
		b.add("screen.jjkcraft.loadout.locked", "Locked");
		b.add("screen.jjkcraft.loadout.hint", "Drag a technique to a slot. Right-click a slot to clear it.");
		b.add("screen.jjkcraft.profile.techniques", "Techniques");
		b.add("screen.jjkcraft.wheel", "Technique Wheel");
		b.add("screen.jjkcraft.wheel.hint", "Point and release");
		b.add("screen.jjkcraft.wheel.empty", "No techniques yet");

		b.add("commands.jjkcraft.technique.unknown", "Unknown technique: %s");
		b.add("commands.jjkcraft.technique.grant", "Granted %s to %s players");
		b.add("commands.jjkcraft.technique.revoke", "Revoked %s from %s players");
		b.add("commands.jjkcraft.technique.innate", "Set the innate technique of %2$s players to %1$s");
		b.add("commands.jjkcraft.technique.equip", "Equipped %s in slot %s for %s players");
		b.add("commands.jjkcraft.technique.list", "%s knows: %s. Hotbar: %s");
		b.add("commands.jjkcraft.technique.cast", "Cast %s by %s entities");
		b.add("commands.jjkcraft.technique.cast_failed", "%s could not cast: %s");
		b.add("commands.jjkcraft.technique.cooldowns", "Reset cooldowns of %s players");

		b.add("effect.jjkcraft.bound", "Bound");
		b.add("effect.jjkcraft.stunned", "Stunned");
		b.add("entity.jjkcraft.cursed_bolt", "Cursed Bolt");
		b.add("death.attack.jjkcraft.cursed_technique", "%1$s was struck down by a cursed technique");
		b.add("death.attack.jjkcraft.cursed_technique.player", "%1$s was struck down by %2$s's cursed technique");
		b.add("death.attack.jjkcraft.black_flash", "%1$s met a Black Flash");
		b.add("death.attack.jjkcraft.black_flash.player", "%1$s met %2$s's Black Flash");
		b.add("death.attack.jjkcraft.sure_hit", "%1$s could not escape a domain");
		b.add("death.attack.jjkcraft.sure_hit.player", "%1$s could not escape %2$s's domain");
		b.add("death.attack.jjkcraft.backlash", "%1$s was torn apart by their own cursed energy");
		b.add("death.attack.jjkcraft.backlash.player", "%1$s was torn apart by their own cursed energy");
	}

	private static void progression(TranslationBuilder b) {
		b.add("message.jjkcraft.awakened", "Your cursed technique has awakened.");
		b.add("message.jjkcraft.awakened.heavenly", "You were born under a Heavenly Restriction: no cursed energy, but a body beyond human limits.");
		b.add("message.jjkcraft.promoted", "Promoted to %s");
		b.add("commands.jjkcraft.grade.get", "%s: %s, %s grade experience, %s unspent growth points");
		b.add("commands.jjkcraft.grade.set", "Set the grade of %s players to %s");
		b.add("commands.jjkcraft.grade.unknown", "Unknown grade: %s");
		b.add("commands.jjkcraft.xp.add", "Gave %s grade experience to %s players");
		b.add("commands.jjkcraft.awaken", "Awakened %s players");
		b.add("commands.jjkcraft.counter.add", "Added %2$s to counter %1$s for %3$s players");
		b.add("commands.jjkcraft.progress.reset", "Reset progression of %s players");
		b.add("growth.jjkcraft.output", "Output");
		b.add("growth.jjkcraft.control", "Control");
		b.add("growth.jjkcraft.efficiency", "Efficiency");
		b.add("growth.jjkcraft.domain", "Domain");
		b.add("growth.jjkcraft.effect.capacity", "+%s%% max CE");
		b.add("growth.jjkcraft.effect.damage", "+%s%% technique damage");
		b.add("growth.jjkcraft.effect.cost", "-%s%% CE cost");
		b.add("growth.jjkcraft.effect.black_flash", "+%s Black Flash chance");
		b.add("growth.jjkcraft.effect.regen", "+%s%% CE regen");
		b.add("growth.jjkcraft.effect.cooldown", "-%s%% cooldowns");
		b.add("growth.jjkcraft.effect.clash", "+%s%% clash power");
		b.add("growth.jjkcraft.effect.domain_cost", "-%s%% domain cost");
		b.add("trial.jjkcraft.defeat_curse", "Defeat curses of %3$s or higher: %1$s/%2$s");
		b.add("trial.jjkcraft.master_techniques", "Master techniques: %s/%s");
		b.add("trial.jjkcraft.culling_points", "Culling Game points: %s/%s");
		b.add("trial.jjkcraft.vow_days", "Days under a Binding Vow: %s/%s");
		b.add("trial.jjkcraft.domain_clash_wins", "Domain clashes won: %s/%s");
		b.add("trial.jjkcraft.unbound_ritual", "The Unbound Ritual: %s/%s");
		b.add("ceremony.jjkcraft.promoted", "PROMOTED");
		b.add("toast.jjkcraft.promoted", "Grade promotion");
		b.add("hud.jjkcraft.heavenly_short", "HR");
	}

	private static void screens(TranslationBuilder b) {
		b.add("screen.jjkcraft.profile", "Profile");
		b.add("screen.jjkcraft.growth", "Growth");
		b.add("screen.jjkcraft.none", "None yet");
		b.add("screen.jjkcraft.more", "...and %s more");
		b.add("screen.jjkcraft.profile.heavenly", "Heavenly Restriction");
		b.add("screen.jjkcraft.profile.awakened", "Awakened sorcerer");
		b.add("screen.jjkcraft.profile.dormant", "Not yet awakened");
		b.add("screen.jjkcraft.profile.stats", "Stats");
		b.add("screen.jjkcraft.profile.capacity", "Max CE");
		b.add("screen.jjkcraft.profile.domain_tier", "tier %s");
		b.add("screen.jjkcraft.profile.unspent", "Unspent growth points: %s");
		b.add("screen.jjkcraft.profile.next", "Next: %s");
		b.add("screen.jjkcraft.profile.top_grade", "Highest grade reached");
		b.add("screen.jjkcraft.profile.trials", "Trials");
		b.add("screen.jjkcraft.req.xp", "Grade experience %s/%s");
		b.add("screen.jjkcraft.req.control", "Cast accuracy %s%% (10+ casts)");
		b.add("screen.jjkcraft.req.output", "CE spent on techniques %s/%s");
		b.add("screen.jjkcraft.growth.spend", "+1");
		b.add("screen.jjkcraft.growth.hint", "One point per %s grade experience, two per grade reached");
	}

	private static void keys(TranslationBuilder b) {
		b.add("key.category.jjkcraft.main", "Jujutsu Craft");
		b.add("key.jjkcraft.profile", "Sorcerer profile");
		b.add("key.jjkcraft.cast", "Cast selected technique");
		b.add("key.jjkcraft.next_slot", "Next technique slot");
		b.add("key.jjkcraft.wheel", "Technique wheel (hold)");
		b.add("key.jjkcraft.sense", "Toggle sensing");
		b.add("key.jjkcraft.domain", "Domain Expansion");
		b.add("key.jjkcraft.simple_domain", "Simple Domain");
	}

	private static void energy(TranslationBuilder b) {
		b.add("commands.jjkcraft.energy.get", "%s has %s / %s CE (%s)");
		b.add("commands.jjkcraft.energy.normal", "normal");
		b.add("commands.jjkcraft.energy.overexerted", "overexerted");
		b.add("commands.jjkcraft.energy.none", "No cursed-capable entities selected");
		b.add("commands.jjkcraft.energy.set", "Set CE of %s entities to %s");
		b.add("commands.jjkcraft.energy.fill", "Filled CE of %s entities");
		b.add("commands.jjkcraft.energy.spend", "%s entities spent %s CE");
		b.add("commands.jjkcraft.config.reloaded", "Reloaded Jujutsu Craft server config");
		b.add("commands.jjkcraft.config.reload_failed", "Could not read the server config file; the old values stay in use");
	}

	private static void grades(TranslationBuilder b) {
		b.add("grade.jjkcraft.grade_4", "Grade 4");
		b.add("grade.jjkcraft.grade_3", "Grade 3");
		b.add("grade.jjkcraft.semi_grade_2", "Semi-Grade 2");
		b.add("grade.jjkcraft.grade_2", "Grade 2");
		b.add("grade.jjkcraft.semi_grade_1", "Semi-Grade 1");
		b.add("grade.jjkcraft.grade_1", "Grade 1");
		b.add("grade.jjkcraft.special_grade", "Special Grade");
	}

	private static void sounds(TranslationBuilder b) {
		b.add("subtitles.jjkcraft.energy.overexert", "Cursed energy strains");
		b.add("subtitles.jjkcraft.grade.promote", "Grade promotion");
		b.add("subtitles.jjkcraft.technique.cast", "Technique cast");
		b.add("subtitles.jjkcraft.technique.strike", "Reinforced strike");
		b.add("subtitles.jjkcraft.technique.black_flash", "Black Flash");
		b.add("subtitles.jjkcraft.technique.bolt_fire", "Cursed bolt fired");
		b.add("subtitles.jjkcraft.technique.bolt_hit", "Cursed bolt hits");
		b.add("subtitles.jjkcraft.technique.slash", "Cursed slash");
		b.add("subtitles.jjkcraft.technique.attract", "Space pulls inward");
		b.add("subtitles.jjkcraft.technique.repel", "Space bursts outward");
		b.add("subtitles.jjkcraft.technique.merge", "Space collapses");
		b.add("subtitles.jjkcraft.technique.speech", "Cursed command");
		b.add("subtitles.jjkcraft.technique.heal", "Reverse technique");
		b.add("subtitles.jjkcraft.technique.chain", "Cursed chains rattle");
		b.add("subtitles.jjkcraft.technique.barrier", "Barrier hums");
		b.add("subtitles.jjkcraft.technique.summon", "Shikigami summoned");
		b.add("subtitles.jjkcraft.domain.open", "Domain expands");
		b.add("subtitles.jjkcraft.domain.close", "Domain collapses");
		b.add("subtitles.jjkcraft.domain.clash", "Domains clash");
		b.add("subtitles.jjkcraft.curse.spawn", "Curse emerges");
		b.add("subtitles.jjkcraft.curse.hurt", "Curse hurts");
		b.add("subtitles.jjkcraft.curse.death", "Curse dissolves");
		b.add("subtitles.jjkcraft.curse.ambient", "Curse murmurs");
		b.add("subtitles.jjkcraft.curse.toll", "Cracked bell tolls");
		b.add("subtitles.jjkcraft.curse.roar", "Curse roars");
	}

	private static void curse(TranslationBuilder b, String id, String name, String egg, String description, String lore) {
		b.add("curse.jjkcraft." + id, name);
		b.add("curse.jjkcraft." + id + ".desc", description);
		b.add("curse.jjkcraft." + id + ".lore", lore);
		b.add("entity.jjkcraft." + id, name);
		b.add("item.jjkcraft." + id + "_spawn_egg", egg);
	}

	private static void curses(TranslationBuilder b) {
		curse(b, "hollow_hymnal", "Hollow Hymnal", "Hollow Hymnal Spawn Egg",
				"A floating choir mask. When its mouth glows it is about to sing, and the song stuns. Hit it in the mouth to silence it.",
				"Born from the dread of a hymn sung at too many funerals. It still remembers the tune, and nothing else.");
		curse(b, "ledger_crawler", "Ledger Crawler", "Ledger Crawler Spawn Egg",
				"Keeps a tally of every death nearby and hits harder with each mark. End fights near it quickly.",
				"Debts, unpaid and counted at night. It drags its ledger through the dark and adds a line for everyone who falls near it.");
		curse(b, "wick_moth", "Wick Moth", "Wick Moth Spawn Egg",
				"Swarms in the dark and eats light: candles go out, torches fall. Faster and fiercer where it is dark. Weak to fire.",
				"A child's fear of the moment the candle goes out, multiplied by every child who ever lay awake.");
		curse(b, "pale_stilt", "Pale Stilt", "Pale Stilt Spawn Egg",
				"Grows strong while its prey is airborne: faster, much harder hitting, hard to hurt, with arms that snatch from afar. Weak while you keep both feet down.",
				"Every child who ever jumped across a gap felt it reach. It never caught them on the ground.");
		curse(b, "echo_mimic", "Echo Mimic", "Echo Mimic Spawn Egg",
				"Copies your last five actions, techniques, swings and jumps, and plays each one back at you three seconds later.",
				"It has no voice of its own. It wears what it saw you do, a few seconds late, like a borrowed coat.");
		curse(b, "salt_lantern", "Salt Lantern", "Salt Lantern Spawn Egg",
				"Fires a beam of bitter light that burns cursed energy: the more CE you carry, the worse it burns. Block it with a shield or a wall; step aside when the aim locks.",
				"Salt for the threshold, a lamp for the lost. Something kept both, long after the house had no one left to protect.");
		curse(b, "tidewash", "Tidewash", "Tidewash Spawn Egg",
				"Born in water. Floods the ground around its prey and grabs with long arms that drag you under. Strong when wet, weak on dry ground.",
				"Every river has a place where swimmers are warned not to go. This is what lives in that warning.");
		curse(b, "thorn_matriarch", "Thorn Matriarch", "Thorn Matriarch Spawn Egg",
				"Roots erupt where she marks the ground and hedge off your retreat. She grows stronger as the day goes on; fight her at dawn or at night.",
				"The overgrown garden behind the house nobody visits. She tends it from dawn to dusk, and she does not like guests.");
		curse(b, "cracked_bell", "Cracked Bell", "Cracked Bell Spawn Egg",
				"Swings back, then tolls a shockwave along the ground. Jump over the wave. Takes double damage while still ringing.",
				"A temple bell that cracked on the night it rang for a fire. It has been ringing the alarm ever since.");
		curse(b, "redacted_saint", "Redacted Saint", "Redacted Saint Spawn Egg",
				"Wears the face and name of the nearest player and can't attack until revealed. Sensing shows its true outline. Its first blow lands double.",
				"A face blacked out of every record, every photograph, every prayer. It has been wearing other people's ever since.");
		curse(b, "ash_oni", "Ash Oni", "Ash Oni Spawn Egg",
				"Lowers its horns and charges in a straight line, sweeping all aside: dodge sideways. Into a wall, or beaten hard, it staggers. Immune to fire.",
				"Fear of the fire that took the old town, given horns. It still runs at the walls that would not let it through.");
		curse(b, "gravebound_gate", "Gravebound Gate", "Gravebound Gate Spawn Egg",
				"A rooted gate that walls the clearing and calls lesser curses; kill them to break its guard. Its phases change the arena: the wall closes in, then graves open.",
				"Every graveyard has a gate, and every gate knows the way back. This one has forgotten which side it is meant to keep closed.");
		curse(b, "vow_kin", "Vow Kin", "Vow Kin Spawn Egg",
				"Fights only those who hold a Binding Vow, harder the heavier the vow. Break or renounce your vow and it flees.",
				"Born from promises people did not keep. It cannot stand to see one kept, and cannot stand to watch one broken.");
		curse(b, "paper_tide_gravemind", "Paper Tide Gravemind", "Paper Tide Gravemind Spawn Egg",
				"Adapts: after a few hits of one kind of damage it resists that kind, then all but ignores it. Keep changing how you hurt it.",
				"Ten thousand grave charms, each written for someone else's dead. Together they remember every way the living have tried to end them.");
		curse(b, "unfinished_ferryman", "Ferryman of Unfinished Roads", "Ferryman Spawn Egg",
				"Arena boss. On a timer the crossing opens and a current drags everyone to its shore, where you are ferried: walk against the flow.",
				"It takes travellers the rest of the way: the ones whose roads ended before they did.");
		curse(b, "culling_warden", "Culling Warden", "Culling Warden Spawn Egg",
				"Can't die while its Rule Seals stand: break them. Its weak point follows the current Edict: steel, distance or energy.",
				"The rules of the game, given a body. It does not hate the players. It only enforces.");
		curse(b, "ten_mouth_sovereign", "Sovereign of Ten Mouths", "Sovereign Spawn Egg",
				"Final boss with ten mouths: it speaks commands, then bites from the ground, then opens its own domain. Bring a Simple Domain.",
				"Every word anyone wished they had not said, swallowed and spoken back. It has a mouth for each of the ten worst.");

		curse(b, "doorframe_hunter", "Doorframe Hunter", "Doorframe Hunter Spawn Egg",
				"Waits invisible inside door frames and ambushes whoever comes close or opens the door. In a fight a nearby door rattles before it steps out of it. Away from doors it is exposed.",
				"Every house has a doorway someone was afraid to walk past at night. This is what stood in it.");
		curse(b, "gilded_mimic", "Gilded Mimic", "Gilded Mimic Spawn Egg",
				"Looks like a chest. Get close, or stare at it, and it springs: its tongue grabs and drags you. Hit it hard to break free.",
				"Greed for what someone else left behind, given a lid and a tongue. It has never once been opened willingly.");
		curse(b, "tithe_burrower", "Tithe Burrower", "Tithe Burrower Spawn Egg",
				"Steals from your inventory and burrows away. It wants one item (shown over its head): drop it nearby and it returns everything and leaves.",
				"The dread of debts that are always called in. It keeps a careful account, and it always offers terms.");
		b.add("curse.jjkcraft.tithe_burrower.wants", "%s (wants %s)");
		b.add("message.jjkcraft.tithe_stolen", "It stole %s. It wants %s.");
		b.add("item.jjkcraft.hinge_splinter", "Hinge Splinter");
		b.add("item.jjkcraft.gilded_tongue", "Gilded Tongue");
		b.add("item.jjkcraft.tithe_purse", "Tithe Purse");
		b.add("item.jjkcraft.cursed_energy_shard", "Cursed Energy Shard");
		b.add("item.jjkcraft.cursed_energy_shard.tooltip", "Use to restore 25 CE");
		b.add("item.jjkcraft.hymn_reed", "Hymn Reed");
		b.add("item.jjkcraft.tally_token", "Tally Token");
		b.add("item.jjkcraft.ember_wax", "Ember Wax");
		b.add("item.jjkcraft.stilt_marrow", "Stilt Marrow");
		b.add("item.jjkcraft.echo_shard", "Echo Shard");
		b.add("item.jjkcraft.salt_crystal", "Bitter Salt Crystal");
		b.add("item.jjkcraft.tide_pearl", "Tide Pearl");
		b.add("item.jjkcraft.thorn_heart", "Thorn Heart");
		b.add("item.jjkcraft.bell_fragment", "Bell Fragment");
		b.add("item.jjkcraft.saint_veil", "Saint's Veil");
		b.add("item.jjkcraft.oni_horn", "Oni Horn");
		b.add("item.jjkcraft.gate_seal", "Gate Seal");
		b.add("block.jjkcraft.cursed_root", "Cursed Root");
		b.add("block.jjkcraft.gate_barrier", "Gate Barrier");
		b.add("block.jjkcraft.curse_nest", "Curse Nest");
		b.add("itemGroup.jjkcraft.main", "Jujutsu Craft");

		b.add("screen.jjkcraft.codex", "Codex");
		b.add("screen.jjkcraft.codex.unknown", "???");
		b.add("screen.jjkcraft.codex.empty", "No curses are known to this world.");
		b.add("screen.jjkcraft.codex.locked", "%s more to reveal");
		b.add("screen.jjkcraft.codex.kills", "Defeated: %s");
		b.add("screen.jjkcraft.codex.unseen", "You have not defeated one of these yet. Its grade is all the records hold.");
		b.add("screen.jjkcraft.codex.weak", "Weak to:");
		b.add("screen.jjkcraft.codex.resists", "Resists:");
		b.add("screen.jjkcraft.codex.immune", "Immune to:");
		b.add("screen.jjkcraft.codex.techniques", "Techniques:");
		for (String[] d : new String[][] {{"in_fire", "fire"}, {"on_fire", "burning"}, {"lava", "lava"}, {"arrow", "arrows"}, {"fall", "falling"},
				{"drown", "drowning"}, {"freeze", "freezing"}, {"lightning_bolt", "lightning"}, {"explosion", "explosions"},
				{"player_explosion", "explosions"}, {"magic", "magic"}, {"sonic_boom", "sonic booms"}, {"trident", "tridents"},
				{"cursed_technique", "cursed techniques"}, {"player_attack", "melee"}, {"mob_attack", "melee"}}) {
			b.add("codex.jjkcraft.damage." + d[0], d[1]);
		}

		b.add("hud.jjkcraft.sensing_short", "SENSE");
		b.add("hud.jjkcraft.sense.count", "%s curses");

		b.add("commands.jjkcraft.curse.unknown", "Unknown curse: %s");
		b.add("commands.jjkcraft.curse.failed", "Could not spawn %s there");
		b.add("commands.jjkcraft.curse.spawned", "Spawned %s at %s, %s, %s");
		b.add("commands.jjkcraft.curse.list", "%s curses are registered:");
		b.add("commands.jjkcraft.curse.cleared", "Removed %s curses");
		b.add("commands.jjkcraft.fear.get", "Fear at %s, %s, %s: %s (stored %s, surroundings %s)");
		b.add("commands.jjkcraft.sense.on", "Sensing turned on for %s players");
		b.add("commands.jjkcraft.sense.off", "Sensing turned off for %s players");
	}

	private static void domain(TranslationBuilder b, String id, String name, String description) {
		b.add("domain.jjkcraft." + id, name);
		b.add("domain.jjkcraft." + id + ".desc", description);
	}

	private static void domains(TranslationBuilder b) {
		domain(b, "malevolent_shrine", "Malevolent Shrine",
				"A cutting field: everything inside is cut on a steady beat, and the caster strikes faster.");
		domain(b, "unlimited_void", "Unlimited Void",
				"An information flood: surges overload everyone inside, drowning their sight and stopping their CE regen.");
		domain(b, "chimera_shadow_garden", "Chimera Shadow Garden",
				"A shadow garden: Divine Dogs rise out of the shadows and hunt everyone inside.");
		domain(b, "idle_death_gamble", "Idle Death Gamble",
				"A gamble: every spin rolls a new outcome. Three of a kind is a jackpot.");
		domain(b, "authentic_mutual_love", "Authentic Mutual Love",
				"An arsenal: blades forged from techniques the caster has copied (Copy), usable only inside.");
		domain(b, "sevenfold_recitation", "Sevenfold Recitation",
				"Perfect repetition: one recitation strikes everyone inside, exactly seven times, then the domain ends.");

		b.add("screen.jjkcraft.profile.domain", "Domain");
		b.add("screen.jjkcraft.profile.no_domain", "None yet (Grade 1)");
		b.add("hud.jjkcraft.domain_expansion", "Domain Expansion");
		b.add("hud.jjkcraft.domain_expansion_barrierless", "Domain Expansion, without a barrier");
		b.add("hud.jjkcraft.domain_ready", "Ready: %s CE");
		b.add("hud.jjkcraft.domain_need", "Needs %s CE");
		b.add("hud.jjkcraft.domain_hold", "Holds ~%s s");
		b.add("hud.jjkcraft.domain_burnout", "Burnout: %s s");
		b.add("hud.jjkcraft.domain_clashing", "Clashing");
		b.add("hud.jjkcraft.domain_sure_hit", "Inside: sure-hit");
		b.add("hud.jjkcraft.domain_inside", "Inside");
		b.add("hud.jjkcraft.domain_shielded", "Simple Domain holds");
		b.add("hud.jjkcraft.simple_domain_short", "SIMPLE");

		b.add("toast.jjkcraft.domain_expansion", "Domain Expansion");
		b.add("toast.jjkcraft.domain_clash", "Domain clash");
		b.add("toast.jjkcraft.clash_won", "Clash won");
		b.add("toast.jjkcraft.clash_lost", "Clash lost");
		b.add("toast.jjkcraft.jackpot", "Jackpot");

		b.add("message.jjkcraft.domain_learned", "Your domain has taken shape: %s");
		b.add("cast_failure.jjkcraft.burnout", "Burnt out");
		b.add("cast_failure.jjkcraft.overloaded", "Overloaded");
		b.add("cast_failure.jjkcraft.no_domain", "No domain");

		b.add("item.jjkcraft.arsenal_blade", "Arsenal Blade");
		b.add("item.jjkcraft.arsenal_blade.loaded", "Arsenal Blade: %s");
		b.add("entity.jjkcraft.shadow_divine_dog", "Shadow Divine Dog");
		b.add("effect.jjkcraft.overloaded", "Overloaded");

		b.add("commands.jjkcraft.domain.unknown", "Unknown domain: %s");
		b.add("commands.jjkcraft.domain.grant", "Gave %s to %s players");
		b.add("commands.jjkcraft.domain.revoke", "Removed the domain of %s players");
		b.add("commands.jjkcraft.domain.expanded", "Expanded %s for %s casters");
		b.add("commands.jjkcraft.domain.refused", "%s could not expand: %s");
		b.add("commands.jjkcraft.domain.closed", "Closed %s domains");
		b.add("commands.jjkcraft.domain.simple", "Toggled the Simple Domain of %s players");
		b.add("commands.jjkcraft.domain.burnout_cleared", "Cleared burnout for %s players");
		b.add("commands.jjkcraft.domain.list", "%s domains are registered, %s open:");
	}

	private static void tools(TranslationBuilder b) {
		b.add("technique.jjkcraft.ten_shadows_divine_dogs", "Ten Shadows: Divine Dogs");
		b.add("technique.jjkcraft.ten_shadows_divine_dogs.desc", "Summons the two Divine Dogs for 30 s. They circle whoever you fight to flank them, then lunge and bite. Casting again summons them anew.");
		b.add("technique.jjkcraft.ten_shadows_nue", "Ten Shadows: Nue");
		b.add("technique.jjkcraft.ten_shadows_nue.desc", "Summons Nue for 45 s. It circles above you, marks enemies hunting you so they glow through walls, and dives at your target.");
		b.add("technique.jjkcraft.ten_shadows_toad", "Ten Shadows: Toad");
		b.add("technique.jjkcraft.ten_shadows_toad.desc", "Summons the Toad for 30 s. It keeps to your side, draws enemies' anger onto itself and swats down projectiles aimed at you.");
		b.add("technique.jjkcraft.straw_doll_resonance", "Straw Doll Technique: Resonance");
		b.add("technique.jjkcraft.straw_doll_resonance.desc", "Binds the one you look at to a straw doll in your hands. Drive a nail into the doll: they are hurt wherever they stand nearby (3 nails, 30 s).");
		b.add("technique.jjkcraft.idle_transfiguration", "Idle Transfiguration");
		b.add("technique.jjkcraft.idle_transfiguration.desc", "Reshapes the soul, and the body follows. Look at someone to shrink them: quicker but feeble. Look at nothing to bulk yourself up: bigger and armored, but slower.");
		b.add("technique.jjkcraft.projection_sorcery", "Projection Sorcery");
		b.add("technique.jjkcraft.projection_sorcery.desc", "You trace your movement in 24 frames: after a clear moment of focus, enemies around you freeze in a frame for an instant while you step forward and move quickly.");
		b.add("technique.jjkcraft.piercing_blood", "Piercing Blood");
		b.add("technique.jjkcraft.piercing_blood.desc", "Ultimate. Blood Manipulation: compresses your blood for a moment, then fires it in a long crimson line that pierces everyone in its path and most armor.");
		b.add("technique.jjkcraft.thousand_lances", "Thousand Lances");
		b.add("technique.jjkcraft.thousand_lances.desc", "Ultimate. Marks the ground where you look; lances rain on the area for three seconds.");
		b.add("technique.jjkcraft.maximum_output_blue", "Maximum Output: Blue");
		b.add("technique.jjkcraft.maximum_output_blue.desc", "Ultimate. Cursed Technique Lapse at maximum cursed energy output: a vast field of attraction ahead drags everything in, then collapses with crushing force.");
		b.add("technique.jjkcraft.oathbound_edge", "Oathbound Edge");
		b.add("technique.jjkcraft.oathbound_edge.desc", "A cut sworn on your vow: 10% harder for every point of your active Binding Vow, half as hard without one. Measured Pace sharpens it further.");
		b.add("technique.jjkcraft.ink_step", "Ink Step");
		b.add("technique.jjkcraft.ink_step.desc", "Slip six blocks along your facing, untouchable for a moment.");
		b.add("technique.jjkcraft.splinter_volley", "Splinter Volley");
		b.add("technique.jjkcraft.splinter_volley.desc", "Throws five splinters of cursed energy in a fan.");
		b.add("technique.jjkcraft.ground_toll", "Ground Toll");
		b.add("technique.jjkcraft.ground_toll.desc", "Slams the ground: a ring of force hurts and throws up everyone around you, harder up close.");
		b.add("technique.jjkcraft.ember_skin", "Ember Skin");
		b.add("technique.jjkcraft.ember_skin.desc", "Toggle. While it burns, whoever strikes you in melee is burned back and set alight. Costs CE every second.");
		b.add("technique.jjkcraft.positive_energy_output", "Positive Energy Output");
		b.add("technique.jjkcraft.positive_energy_output.desc", "Reverse Cursed Technique turned outward: heals the ally you look at and leaves them regenerating. With no ally in sight it heals you, less well.");
		b.add("entity.jjkcraft.divine_dog", "Divine Dog");
		b.add("entity.jjkcraft.nue", "Nue");
		b.add("entity.jjkcraft.toad", "Toad");
		b.add("effect.jjkcraft.bulked", "Bulked");
		b.add("effect.jjkcraft.shrunk", "Shrunk");
		b.add("effect.jjkcraft.frozen_frame", "Frozen Frame");
		b.add("effect.jjkcraft.unsettled", "Unsettled");
		b.add("item.jjkcraft.split_soul_katana", "Split Soul Katana");
		b.add("item.jjkcraft.split_soul_katana.desc", "A long blade that remembers every cut. While held, it lends Dismantle at a quarter off.");
		b.add("item.jjkcraft.chain_of_a_thousand_miles", "Chain of a Thousand Miles");
		b.add("item.jjkcraft.chain_of_a_thousand_miles.desc", "Reaches two blocks further than a sword. While held, it lends Grave Chains.");
		b.add("item.jjkcraft.inverted_spear_of_heaven", "Inverted Spear of Heaven");
		b.add("item.jjkcraft.inverted_spear_of_heaven.desc", "Its strikes force cursed techniques to cancel: a hit ends the target's active technique and drains 15 CE.");
		b.add("item.jjkcraft.debt_cleaver", "Cleaver of Bad Debts");
		b.add("item.jjkcraft.debt_cleaver.desc", "Each hit takes 6 CE from the target and pays it to you. While held, it lends Cleave.");
		b.add("item.jjkcraft.bonewood_staff", "Bonewood Staff");
		b.add("item.jjkcraft.bonewood_staff.desc", "A beginner's focus. While held, it lends Grudge Bolt at a fifth off.");
		b.add("item.jjkcraft.withered_digit", "Withered Digit");
		b.add("item.jjkcraft.withered_digit.desc", "A cursed object. Swallowing it grants a technique you lack, and unsettles your cursed energy for a minute. Below its grade, it fights back.");
		b.add("item.jjkcraft.drifting_gourd", "Drifting Gourd");
		b.add("item.jjkcraft.drifting_gourd.desc", "Uncork it to ride its cloud: you can fly while you hold it, for 3 CE a second. Cork it, or run dry, and the cloud lets you down gently.");
		b.add("item.jjkcraft.tally_ledger", "Tally Ledger");
		b.add("item.jjkcraft.tally_ledger.desc", "Holds Culling Game points: use to enter 5, sneak and use to take them all out. Only works while a rule lets points change hands.");
		b.add("item.jjkcraft.straw_doll", "Straw Doll");
		b.add("item.jjkcraft.technique_scroll", "Technique Scroll");
		b.add("item.jjkcraft.technique_scroll.named", "Scroll: %s");
		b.add("tooltip.jjkcraft.tool.grade", "%s tool");
		b.add("tooltip.jjkcraft.tool.grants", "Lends %s while held");
		b.add("tooltip.jjkcraft.tool.grants_cost", "Lends %s while held (%s CE)");
		b.add("tooltip.jjkcraft.tool.cursed_object", "Grants %s");
		b.add("tooltip.jjkcraft.tool.cursed_object_cost", "Grants %s (%s CE)");
		b.add("tooltip.jjkcraft.tool.usable", "Your grade can wield its power");
		b.add("tooltip.jjkcraft.tool.unusable", "Your grade is too low to wield its power");
		b.add("tooltip.jjkcraft.straw_doll.bound", "Bound to %s");
		b.add("tooltip.jjkcraft.straw_doll.nails", "%s nails left, %s damage each, %s CE a nail");
		b.add("tooltip.jjkcraft.ledger.points", "Holds %s points");
		b.add("tooltip.jjkcraft.scroll.cost", "%s CE, %s s cooldown");
		b.add("tooltip.jjkcraft.scroll.learnable", "You can learn this");
		b.add("tooltip.jjkcraft.scroll.too_low", "Your grade is too low to learn this");
		b.add("message.jjkcraft.straw_doll.far", "%s is out of the straw doll's reach.");
		b.add("message.jjkcraft.straw_doll.energy", "Not enough CE to drive a nail.");
		b.add("message.jjkcraft.straw_doll.crumbled", "The straw doll crumbles.");
		b.add("message.jjkcraft.cursed_object.learned", "Something takes root in you. You learned %s.");
		b.add("message.jjkcraft.cursed_object.backlash", "The cursed object fights back: your grade is too low to hold it.");
		b.add("message.jjkcraft.gourd.uncorked", "The gourd breathes out a cloud.");
		b.add("message.jjkcraft.gourd.corked", "You cork the gourd.");
		b.add("message.jjkcraft.gourd.dry", "Your cursed energy runs dry; the cloud sets you down.");
		b.add("message.jjkcraft.gourd.dropped", "Without the gourd, the cloud thins away.");
		b.add("message.jjkcraft.gourd.energy", "Not enough CE to uncork the gourd.");
		b.add("message.jjkcraft.ledger.bound", "The ledger is bound to the game's rules: no rule lets points change hands now.");
		b.add("message.jjkcraft.ledger.withdrew", "You take %s points out of the ledger.");
		b.add("message.jjkcraft.ledger.deposited", "You enter %s points (the ledger holds %s).");
		b.add("message.jjkcraft.ledger.short", "You need %s points to make an entry.");
		b.add("message.jjkcraft.scroll.known", "You already know this technique.");
		b.add("message.jjkcraft.scroll.grade", "Your grade is too low to learn this.");
		b.add("message.jjkcraft.scroll.learned", "You learned %s.");
		b.add("message.jjkcraft.oath_unsworn", "Without a vow the oath cuts half as deep.");
		b.add("hud.jjkcraft.unsettled_short", "UNSETTLED");
	}

	private static void culling(TranslationBuilder b) {
		b.add("entity.jjkcraft.entrant", "Entrant");
		b.add("entity.jjkcraft.kogane", "Kogane");
		b.add("key.jjkcraft.culling", "Culling Game");
		b.add("colony.jjkcraft.name_0", "Ashgrove Colony");
		b.add("colony.jjkcraft.name_1", "Kilnreach Colony");
		b.add("colony.jjkcraft.name_2", "Saltmarsh Colony");
		b.add("colony.jjkcraft.name_3", "Lantern Ward Colony");
		b.add("colony.jjkcraft.name_4", "Stillwater Colony");
		b.add("colony.jjkcraft.name_5", "Hollowpine Colony");
		b.add("colony.jjkcraft.name_6", "Emberquay Colony");
		b.add("colony.jjkcraft.name_7", "Thornfield Colony");
		b.add("colony.jjkcraft.name_8", "Greyford Colony");
		b.add("colony.jjkcraft.name_9", "Moonwell Colony");
		b.add("colony.jjkcraft.name_10", "Rookhaven Colony");
		b.add("colony.jjkcraft.name_11", "Cinderlane Colony");
		b.add("culling_rule.jjkcraft.entry_window", "Every awakened sorcerer has %s days from awakening to enter a colony. Whoever lets the window close loses their cursed technique.");
		b.add("culling_rule.jjkcraft.entry_window.name", "The Entry Window");
		b.add("culling_rule.jjkcraft.one_way_entry", "Whoever crosses into a colony after its barrier rises becomes a participant and may not cross back out. Those already inside when it rose may come and go.");
		b.add("culling_rule.jjkcraft.one_way_entry.name", "One Way In");
		b.add("culling_rule.jjkcraft.sorcerer_bounty", "Ending a sorcerer participant is worth %s on the tally.");
		b.add("culling_rule.jjkcraft.sorcerer_bounty.name", "Sorcerer Bounty");
		b.add("culling_rule.jjkcraft.ordinary_bounty", "Ending a participant who is not a sorcerer is worth %s on the tally.");
		b.add("culling_rule.jjkcraft.ordinary_bounty.name", "Ordinary Bounty");
		b.add("culling_rule.jjkcraft.game_master", "A participant may spend %s points to negotiate one new rule with the game master through the Kogane. The game master refuses any rule that would sit against one already in force.");
		b.add("culling_rule.jjkcraft.game_master.name", "The Game Master");
		b.add("culling_rule.jjkcraft.shared_ledger", "Participants may hand their points to one another, %s at a time.");
		b.add("culling_rule.jjkcraft.shared_ledger.name", "Shared Ledger");
		b.add("culling_rule.jjkcraft.open_tally", "For %s point, a participant may learn another's name, colony and score, and see where they stand for %s seconds.");
		b.add("culling_rule.jjkcraft.open_tally.name", "Open Tally");
		b.add("culling_rule.jjkcraft.substitution", "For %s points, a participant may change colonies. The game master brings someone new to fill the place they leave.");
		b.add("culling_rule.jjkcraft.substitution.name", "Substitution");
		b.add("culling_rule.jjkcraft.stagnation", "A participant whose score has not risen for %s days has their cursed energy capped at half until it rises again.");
		b.add("culling_rule.jjkcraft.stagnation.name", "Stagnation");
		b.add("culling_rule.jjkcraft.withdrawal", "A participant may spend %s points to leave the game for good.");
		b.add("culling_rule.jjkcraft.withdrawal.name", "Withdrawal");
		b.add("culling_rule.jjkcraft.curse_tithe", "Curses slain inside a colony go on the tally too: 1 point, more for higher grades, 10 for a special grade.");
		b.add("culling_rule.jjkcraft.curse_tithe.name", "Curse Tithe");
		b.add("culling_rule.jjkcraft.leader_mark", "Whoever leads the tally is worth twice as much, and every day everyone is shown where they stand.");
		b.add("culling_rule.jjkcraft.leader_mark.name", "Mark of the Leader");
		b.add("culling_rule.jjkcraft.night_hunt", "Participants ended at night are worth twice as much.");
		b.add("culling_rule.jjkcraft.night_hunt.name", "Night Hunt");
		b.add("culling_rule.jjkcraft.night_truce", "Ending a participant at night earns nothing: the points are struck from the tally.");
		b.add("culling_rule.jjkcraft.night_truce.name", "Night Truce");
		b.add("culling_rule.jjkcraft.warden_mandate", "The game master sends a Culling Warden into the proposer's colony. For one day it cannot fall.");
		b.add("culling_rule.jjkcraft.warden_mandate.name", "Warden's Mandate");
		b.add("culling_rule.jjkcraft.blade_law", "For two days, ending a participant hand to hand earns 1 more point. While it holds, a Warden yields only to steel.");
		b.add("culling_rule.jjkcraft.blade_law.name", "Law of the Blade");
		b.add("culling_rule.jjkcraft.archer_law", "For two days, ending a participant with a shot from 8 blocks or more earns 1 more point. While it holds, a Warden yields only to distance.");
		b.add("culling_rule.jjkcraft.archer_law.name", "Law of the Bow");
		b.add("culling_rule.jjkcraft.sorcery_law", "For two days, ending a participant with a cursed technique earns 1 more point. While it holds, a Warden yields only to cursed energy.");
		b.add("culling_rule.jjkcraft.sorcery_law.name", "Law of Sorcery");
		b.add("culling.jjkcraft.log.started", "The Culling Game began: %s colonies");
		b.add("culling.jjkcraft.log.colony", "%s rose");
		b.add("culling.jjkcraft.log.ended", "The game ended (%s)");
		b.add("culling.jjkcraft.log.joined", "%s entered %s");
		b.add("culling.jjkcraft.log.fell", "%s fell and lost %s points");
		b.add("culling.jjkcraft.log.transfer", "%s gave %s %s points");
		b.add("culling.jjkcraft.log.substitute", "%s moved to %s");
		b.add("culling.jjkcraft.log.withdrew", "%s withdrew");
		b.add("culling.jjkcraft.log.rule_added", "%s added a rule");
		b.add("culling.jjkcraft.log.rule_lapsed", "A rule lapsed");
		b.add("culling.jjkcraft.log.day", "Day %s began");
		b.add("culling.jjkcraft.log.stripped", "%s lost their technique");
		b.add("culling.jjkcraft.log.warden", "A Warden entered %s");
		b.add("culling.jjkcraft.end.days", "the days ran out");
		b.add("culling.jjkcraft.end.last_standing", "one was left standing");
		b.add("culling.jjkcraft.end.stopped", "stopped by an operator");
		b.add("culling.jjkcraft.refused.no_target", "That participant can't be found.");
		b.add("culling.jjkcraft.refused.too_few", "You don't hold enough points.");
		b.add("culling.jjkcraft.refused.no_colony", "Pick another colony first.");
		b.add("culling.jjkcraft.refused.not_participant", "Only participants can do that.");
		b.add("culling.jjkcraft.refused.far", "Stand by the Kogane at your colony's centre to negotiate.");
		b.add("culling.jjkcraft.refused.unknown", "The Kogane knows no such rule.");
		b.add("culling.jjkcraft.refused.active", "That rule is already in force.");
		b.add("culling.jjkcraft.refused.conflict", "The game master refuses: it would sit against a rule in force.");
		b.add("culling.jjkcraft.refused.no_rule", "No rule in force allows that.");
		b.add("culling.jjkcraft.refused.not_running", "The Culling Game is not running.");
		b.add("culling.jjkcraft.warden_arrives", "A Culling Warden has been sent into the game.");
		b.add("culling.jjkcraft.rising_star", "%s of %s has reached %s points.");
		b.add("culling_rule.jjkcraft.rising_star", "When a participant's tally first reaches %s points, every participant is told their name and colony.");
		b.add("culling_rule.jjkcraft.rising_star.name", "Rising Star");
		b.add("message.jjkcraft.culling.started", "The Culling Game has begun. %s colonies stand behind their barriers. Awakened sorcerers have %s days to enter one.");
		b.add("message.jjkcraft.culling.final", "The game is over. You placed %s with %s points and earned %s grade experience.");
		b.add("message.jjkcraft.culling.ended", "The Culling Game is over: %s. First place: %s.");
		b.add("message.jjkcraft.culling.joined", "You crossed into %s. You are a participant now.");
		b.add("message.jjkcraft.culling.held", "The barrier holds you inside your colony.");
		b.add("message.jjkcraft.culling.window", "You have awakened. You have %s days to enter a Culling Game colony.");
		b.add("message.jjkcraft.culling.deadline", "%s days left to enter a colony.");
		b.add("message.jjkcraft.culling.stripped", "The entry window closed. Your cursed technique has been taken.");
		b.add("message.jjkcraft.culling.unstuck", "You scored again: your cursed energy is no longer capped.");
		b.add("message.jjkcraft.culling.gave", "You gave %s points to %s.");
		b.add("message.jjkcraft.culling.received", "%2$s gave you %1$s points.");
		b.add("message.jjkcraft.culling.revealed", "%s stands in %s with %s points.");
		b.add("message.jjkcraft.culling.moved", "You were moved to %s.");
		b.add("message.jjkcraft.culling.withdrew", "You have withdrawn from the game.");
		b.add("message.jjkcraft.culling.stagnant", "Your score has stalled: your cursed energy is capped at %s%%.");
		b.add("message.jjkcraft.culling.leader", "The leader is %s, in %s, with %s points.");
		b.add("message.jjkcraft.culling.rule_added", "%s added a rule: %s");
		b.add("toast.jjkcraft.culling.joined", "Culling Game");
		b.add("toast.jjkcraft.culling.points", "Tally");
		b.add("toast.jjkcraft.culling.points.gain", "+%s points");
		b.add("toast.jjkcraft.culling.points.loss", "-%s points");
		b.add("toast.jjkcraft.culling.rule", "%s added a rule");
		b.add("toast.jjkcraft.culling.deadline", "Entry window");
		b.add("toast.jjkcraft.culling.deadline.body", "%s days left to enter a colony");
		b.add("toast.jjkcraft.culling.started", "The Culling Game");
		b.add("toast.jjkcraft.culling.started.body", "%s colonies have risen");
		b.add("toast.jjkcraft.culling.ended", "The game is over");
		b.add("toast.jjkcraft.culling.ended.body", "First place: %s");
		b.add("hud.jjkcraft.culling.title", "Culling Game, day %s");
		b.add("hud.jjkcraft.culling.points", "%s pts");
		b.add("hud.jjkcraft.culling.enter_within", "Enter a colony: %s days");
		b.add("hud.jjkcraft.culling.rules", "Rules: %s");
		b.add("hud.jjkcraft.culling.stagnant", "STALLED");
		b.add("hud.jjkcraft.culling.master", "KOGANE");
		b.add("screen.jjkcraft.culling", "Culling Game");
		b.add("screen.jjkcraft.culling.tab", "Culling");
		b.add("screen.jjkcraft.culling.map", "Map");
		b.add("screen.jjkcraft.culling.board", "Scoreboard");
		b.add("screen.jjkcraft.culling.rules", "Rules");
		b.add("screen.jjkcraft.culling.negotiate", "Negotiate");
		b.add("screen.jjkcraft.culling.propose", "Propose");
		b.add("screen.jjkcraft.culling.reveal", "Reveal them");
		b.add("screen.jjkcraft.culling.give", "Give points");
		b.add("screen.jjkcraft.culling.next_colony", "Colony >");
		b.add("screen.jjkcraft.culling.move", "Move there");
		b.add("screen.jjkcraft.culling.withdraw", "Withdraw from the game");
		b.add("screen.jjkcraft.culling.not_started", "The Culling Game has not begun. It starts by itself when an awakened sorcerer joins a new world, or an operator can start it with /jjk culling start.");
		b.add("screen.jjkcraft.culling.over", "The Culling Game is over");
		b.add("screen.jjkcraft.culling.idle", "The Culling Game is not running");
		b.add("screen.jjkcraft.culling.header", "Day %s  %s  %s points");
		b.add("screen.jjkcraft.culling.header_outside", "Day %s  Enter a colony within %s days");
		b.add("screen.jjkcraft.culling.header_watching", "Day %s  %s");
		b.add("screen.jjkcraft.culling.status.none", "Not in the game");
		b.add("screen.jjkcraft.culling.status.resident", "A resident: free to come and go");
		b.add("screen.jjkcraft.culling.status.withdrawn", "Withdrawn");
		b.add("screen.jjkcraft.culling.status.eliminated", "Eliminated");
		b.add("screen.jjkcraft.culling.status.participant", "Participant");
		b.add("screen.jjkcraft.culling.results", "Final standings");
		b.add("screen.jjkcraft.culling.colonies", "Colonies");
		b.add("screen.jjkcraft.culling.colony_line", ": %s in, %s pts");
		b.add("screen.jjkcraft.culling.log", "Recent");
		b.add("screen.jjkcraft.culling.tip_people", "Participants: %s");
		b.add("screen.jjkcraft.culling.tip_points", "Points held: %s");
		b.add("screen.jjkcraft.culling.tip_where", "Centre %s, %s, radius %s");
		b.add("screen.jjkcraft.culling.col_name", "Name");
		b.add("screen.jjkcraft.culling.col_colony", "Colony");
		b.add("screen.jjkcraft.culling.col_points", "Points");
		b.add("screen.jjkcraft.culling.unknown", "Unknown participant");
		b.add("screen.jjkcraft.culling.sorcerer_tag", "S");
		b.add("screen.jjkcraft.culling.ordinary_tag", "-");
		b.add("screen.jjkcraft.culling.pick", "Pick a participant");
		b.add("screen.jjkcraft.culling.amount", "Amount: %s");
		b.add("screen.jjkcraft.culling.to_colony", "To: %s");
		b.add("screen.jjkcraft.culling.only_participants", "Only participants can act in the game.");
		b.add("screen.jjkcraft.culling.rule_meta", "Added by %s");
		b.add("screen.jjkcraft.culling.rule_meta_lapse", "Added by %s, lapses in %s days");
		b.add("screen.jjkcraft.culling.go_to_master", "Stand by the Kogane at your colony's centre to negotiate.");
		b.add("screen.jjkcraft.culling.pick_rule", "Pick a rule to propose.");
		b.add("screen.jjkcraft.culling.lasts", "Lasts %s days");
		b.add("screen.jjkcraft.culling.verdict_active", "Already in force.");
		b.add("screen.jjkcraft.culling.verdict_conflict", "The game master would refuse: it sits against a rule in force.");
		b.add("screen.jjkcraft.culling.verdict_short", "You need %s more points.");
		b.add("screen.jjkcraft.culling.verdict_ok", "The game master will hear it for %s points.");
		b.add("commands.jjkcraft.culling.stopped", "The Culling Game was stopped");
		b.add("commands.jjkcraft.culling.reset", "The Culling Game was reset");
		b.add("commands.jjkcraft.culling.running", "The Culling Game is already running");
		b.add("commands.jjkcraft.culling.started", "The Culling Game started with %s colonies");
		b.add("commands.jjkcraft.culling.status", "Day %s, %s colonies, %s participants standing, %s rules");
		b.add("commands.jjkcraft.culling.colony", "Colony #%s raised at %s");
		b.add("commands.jjkcraft.culling.joined", "%s joined colony #%s");
		b.add("commands.jjkcraft.culling.points", "Gave %s points to %s participants");
		b.add("commands.jjkcraft.culling.rule_added", "Rule %s is in force");
		b.add("commands.jjkcraft.culling.rule_removed", "Rule %s was struck");
		b.add("commands.jjkcraft.culling.day", "The game clock moved %s days forward");
		b.add("commands.jjkcraft.culling.no_entrant", "No entrant could be brought in (is the colony loaded?)");
		b.add("commands.jjkcraft.culling.entrant", "An entrant entered colony #%s");
	}

	private static void sorcery(TranslationBuilder b) {
		b.add("item.jjkcraft.broken_knot", "Broken Knot");
		b.add("item.jjkcraft.folded_charm", "Folded Grave Charm");
		b.add("item.jjkcraft.ferry_coin", "Ferry Coin");
		b.add("item.jjkcraft.edict_tablet", "Edict Tablet");
		b.add("item.jjkcraft.sovereign_tooth", "Sovereign's Tooth");
		b.add("block.jjkcraft.rule_seal", "Rule Seal");
		b.add("domain.jjkcraft.ten_mouth_throat", "Throat of Ten Thousand Words");
		b.add("domain.jjkcraft.ten_mouth_throat.desc", "The Sovereign's domain: every word lands, and some of them hold you still.");

		b.add("boss.jjkcraft.adapted", "%s, adapted to %s");
		b.add("boss.jjkcraft.ferryman.next", "Ferryman: crossing in %2$ss");
		b.add("boss.jjkcraft.ferryman.crossing", "Ferryman: crossing open, %2$ss");
		b.add("boss.jjkcraft.warden", "%s, %s");
		b.add("boss.jjkcraft.sovereign", "%s, %s mouths");
		b.add("edict.jjkcraft.steel", "Edict of Steel");
		b.add("edict.jjkcraft.distance", "Edict of Distance");
		b.add("edict.jjkcraft.energy", "Edict of Energy");

		vow(b, "grounded_oath", "Grounded Oath", "Never jump. Jumping breaks the vow. The Ink-Iron Palm hits much harder under it.");
		vow(b, "measured_pace", "Measured Pace", "Never sprint. Sprinting breaks the vow.");
		vow(b, "bare_skin", "Bare Skin", "Wear no armor. Putting any on breaks the vow. Dismantle cuts deeper under it.");
		vow(b, "open_guard", "Open Guard", "Never raise a shield. Blocking breaks the vow.");
		vow(b, "single_path", "Single Path", "Cast only your innate technique. Others are refused while it holds.");
		vow(b, "closed_heart", "Closed Heart", "Never expand a domain. Domains are refused while it holds.");
		vow(b, "night_covenant", "Night Covenant", "Cast techniques only in darkness: at night or out of the sky's reach. Grudge Bolt is cheaper and longer under it.");
		vow(b, "half_vessel", "Half Vessel", "Hold only half your usual cursed energy.");
		b.add("vow_focus.jjkcraft.power", "Power");
		b.add("vow_focus.jjkcraft.efficiency", "Efficiency");
		b.add("vow_focus.jjkcraft.reserve", "Reserve");
		b.add("vow_term.jjkcraft.skirmish", "5 minutes");
		b.add("vow_term.jjkcraft.day", "One day");
		b.add("vow_term.jjkcraft.binding", "Binding");

		b.add("screen.jjkcraft.vows", "Vows");
		b.add("screen.jjkcraft.vows.focus", "The power goes into");
		b.add("screen.jjkcraft.vows.term", "It holds for");
		b.add("screen.jjkcraft.vows.points", "Vow power %s (%s of %s restrictions)");
		b.add("screen.jjkcraft.vows.gain.damage", "Technique damage +%s%%");
		b.add("screen.jjkcraft.vows.gain.melee", "Blows +%s%%");
		b.add("screen.jjkcraft.vows.gain.cost", "Technique cost -%s%%");
		b.add("screen.jjkcraft.vows.gain.capacity", "Cursed energy +%s%%");
		b.add("screen.jjkcraft.vows.gain.regen", "Regeneration +%s%%");
		b.add("screen.jjkcraft.vows.price", "If broken: lose %s%% of your CE, a stun, and regen halved for %s minutes.");
		b.add("screen.jjkcraft.vows.swear", "Swear");
		b.add("screen.jjkcraft.vows.renounce", "Renounce");
		b.add("screen.jjkcraft.vows.choose", "Choose up to three restrictions.");
		b.add("screen.jjkcraft.vows.debt", "You are still paying for a broken vow.");
		b.add("screen.jjkcraft.vows.held", "Your Binding Vow (power %s)");
		b.add("screen.jjkcraft.vows.binding", "Binding: it holds until you renounce it.");
		b.add("screen.jjkcraft.vows.left", "Fulfilled in about %s minutes.");
		b.add("screen.jjkcraft.vows.renounce_hint", "Renouncing early costs half the price of breaking (%s%% CE).");

		b.add("screen.jjkcraft.profile.vows", "Binding Vow");
		b.add("screen.jjkcraft.profile.no_vow", "None");
		b.add("screen.jjkcraft.profile.reverse", "Reverse Technique");
		b.add("screen.jjkcraft.profile.reverse_on", "Awakened: auto-heal on");
		b.add("screen.jjkcraft.profile.reverse_off", "Awakened: auto-heal off");
		b.add("screen.jjkcraft.profile.reverse_locked", "Not yet: it wakes at the edge of death");

		b.add("hud.jjkcraft.zone_short", "ZONE");
		b.add("hud.jjkcraft.reverse_short", "RCT");
		b.add("hud.jjkcraft.vow_short", "VOW");
		b.add("hud.jjkcraft.debt_short", "DEBT");
		b.add("toast.jjkcraft.black_flash", "Black Flash");
		b.add("toast.jjkcraft.black_flash.body", "x%s, in the zone");
		b.add("toast.jjkcraft.reverse", "Reverse Cursed Technique");
		b.add("toast.jjkcraft.reverse.body", "Positive energy, from the edge of death");
		b.add("key.jjkcraft.reverse", "Toggle Reverse Technique healing");

		b.add("message.jjkcraft.in_the_zone", "In the zone");
		b.add("message.jjkcraft.reverse_awakened", "At the edge of death, your cursed energy turns over on itself. Reverse Cursed Technique has awakened.");
		b.add("message.jjkcraft.reverse_save", "Reverse technique pulls you back");
		b.add("message.jjkcraft.vow_sworn", "Binding Vow sworn (power %s)");
		b.add("message.jjkcraft.vow_broken", "You broke your vow. It takes its due.");
		b.add("message.jjkcraft.vow_renounced", "You renounced your vow early.");
		b.add("message.jjkcraft.vow_fulfilled", "Your vow is fulfilled.");
		b.add("message.jjkcraft.vow_kin_flees", "The Vow Kin flees from your broken word");
		b.add("message.jjkcraft.unbound_ritual", "The Unbound Ritual is complete. Nothing binds your grade any more.");
		b.add("cast_failure.jjkcraft.vow", "A vow forbids it");

		b.add("commands.jjkcraft.blackflash.forced", "The next eligible strike of %s entities will be a Black Flash");
		b.add("commands.jjkcraft.reverse.granted", "Reverse Cursed Technique awakened for %s players");
		b.add("commands.jjkcraft.reverse.revoked", "Reverse Cursed Technique removed from %s players");
		b.add("commands.jjkcraft.reverse.forced", "The next lethal blow will awaken it for %s eligible players");
		b.add("commands.jjkcraft.vow.sworn", "%s swore %s");
		b.add("commands.jjkcraft.vow.refused", "Vow refused: %s");
		b.add("commands.jjkcraft.vow.bad_option", "Unknown focus or term: %s %s");
		b.add("commands.jjkcraft.vow.broken", "Broke the vows of %s players");
		b.add("commands.jjkcraft.vow.cleared", "Cleared the vows and debts of %s players");
		b.add("commands.jjkcraft.vow.list", "%s vows are registered:");
	}

	private static void vow(TranslationBuilder b, String id, String name, String description) {
		b.add("vow.jjkcraft." + id, name);
		b.add("vow.jjkcraft." + id + ".desc", description);
	}
}
