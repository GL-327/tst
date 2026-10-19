package com.jjkcraft.datagen;

import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider.TranslationBuilder;

/** English text of the Modulo expansion's code: items, blocks, entities, messages, HUD and commands. */
final class ModuloLang {
	private ModuloLang() {
	}

	static void add(TranslationBuilder b) {
		b.add("itemGroup.jjkcraft.modulo", "Jujutsu Craft: Modulo");

		b.add("block.jjkcraft.mul_ore", "Mul Ore");
		b.add("block.jjkcraft.naunax_hull", "Naunax Hull");
		b.add("block.jjkcraft.naunax_deck_plate", "Naunax Deck Plate");
		b.add("block.jjkcraft.mul_conduit", "Mul Conduit");
		b.add("block.jjkcraft.hard_light", "Solid Light");
		b.add("block.jjkcraft.naunax_gate", "Naunax Gate");

		b.add("item.jjkcraft.mul", "Mul");
		b.add("item.jjkcraft.high_purity_mul", "High-Purity Mul");
		b.add("item.jjkcraft.rumelian_blade", "Rumelian Blade");
		b.add("item.jjkcraft.gravity_harness", "Gravity Harness");
		b.add("item.jjkcraft.gravity_harness.desc", "Simurian work. Switched on, gravity loosens its hold: you fall slowly and jump high.");
		b.add("item.jjkcraft.mul_ward", "Mul Ward");
		b.add("item.jjkcraft.mul_ward.desc", "Held, it takes half of each blow and pays for it in Mul energy.");
		b.add("item.jjkcraft.naunax_beacon", "Naunax Beacon");
		b.add("item.jjkcraft.naunax_beacon.desc", "Calls you to the Naunax's arrival hall once you have boarded it; used aboard, it sends you back where you boarded.");
		b.add("item.jjkcraft.honoyagi", "Honoyagi");
		b.add("item.jjkcraft.wancha_octopus", "Wancha Octopus");
		b.add("item.jjkcraft.wancha_octopus.desc", "A mutated octopus with one tentacle that is said to hold the flavor of all eight.");
		b.add("item.jjkcraft.takoyaki", "Takoyaki");
		b.add("item.jjkcraft.takoyaki.desc", "From the cultural exchange at Kyoto Jujutsu High. Very hot.");
		b.add("item.jjkcraft.tiger_cub", "Tiger Cub");
		b.add("item.jjkcraft.tiger_cub.desc", "Toranosuke's cub. Osada promised to raise it well.");
		b.add("item.jjkcraft.yuji_cursed_object", "Yuji Itadori's Cursed Object");
		b.add("item.jjkcraft.yuji_cursed_object.desc",
				"Yuji Itadori resolved that after his death he would become a cursed object, so the energy could be taken back if it is ever needed again.");

		b.add("entity.jjkcraft.modulo_npc", "Modulo Character");
		b.add("entity.jjkcraft.modulo_boss", "Modulo Boss");
		b.add("entity.jjkcraft.toranosuke", "Toranosuke");
		b.add("entity.jjkcraft.mahoraga", "Mahoraga");
		b.add("entity.jjkcraft.wancha_octopus", "Wancha Octopus");

		b.add("tooltip.jjkcraft.modulo.mul", "Holds %s of %s cursed energy");
		b.add("tooltip.jjkcraft.modulo.mul_use", "Hold use to pour your cursed energy in and make Mul energy");
		b.add("tooltip.jjkcraft.modulo.mul_cost", "Costs %s Mul energy");
		b.add("tooltip.jjkcraft.modulo.mul_cost_second", "%s Mul energy a second");
		b.add("tooltip.jjkcraft.modulo.ward", "%s Mul energy per point of damage taken");
		b.add("tooltip.jjkcraft.modulo.special_grade_object", "Special grade cursed object");
		b.add("tooltip.jjkcraft.modulo.vessel_later", "Kept for a vessel (not usable yet)");

		b.add("message.jjkcraft.modulo.disabled", "The Modulo expansion is turned off on this server.");
		b.add("message.jjkcraft.modulo.mul_full", "You hold all the Mul energy you can.");
		b.add("message.jjkcraft.modulo.mul_no_energy", "You have no cursed energy to pour.");
		b.add("message.jjkcraft.modulo.mul_short", "Not enough Mul energy.");
		b.add("message.jjkcraft.modulo.harness_on", "The harness hums; gravity lets go a little.");
		b.add("message.jjkcraft.modulo.harness_off", "The harness falls quiet.");
		b.add("message.jjkcraft.modulo.harness_dry", "The harness runs out of Mul energy.");
		b.add("message.jjkcraft.modulo.beacon_unknown", "The beacon knows no ship you have boarded.");
		b.add("message.jjkcraft.modulo.descent", "The Naunax descends above the badlands at %s, %s. A ramp of light reaches the ground.");
		b.add("message.jjkcraft.modulo.no_site", "Mission site %s is missing.");
		b.add("message.jjkcraft.modulo.harmony", "Maru's Harmony brings you to the ship.");
		b.add("message.jjkcraft.modulo.companion", "Maru joins you. In the Modulo dimensions he follows you; sneak and talk to him to return to the ship.");
		b.add("message.jjkcraft.modulo.mission_done", "Mission complete: %s");
		b.add("message.jjkcraft.modulo.no_sleep", "There is nowhere to sleep here.");
		b.add("message.jjkcraft.modulo.masaki.reveal", "The boy's skin splits: Masaki Takeda steps out.");
		b.add("message.jjkcraft.modulo.masaki.hide", "Masaki sews himself into his guardian. Strike the guardian to tear him out.");
		b.add("message.jjkcraft.modulo.masaki.torn", "Masaki is torn out of the guardian!");
		b.add("message.jjkcraft.modulo.maru.chaos", "Chaos: the arena's gravity is about to turn over.");
		b.add("message.jjkcraft.modulo.maru.yield", "Maru lowers his hands. \"Enough.\"");
		b.add("message.jjkcraft.modulo.dabura.darkness", "Darkness gathers between Dabura's hands. Get behind something solid!");
		b.add("message.jjkcraft.modulo.dabura.harmony", "Maru's Harmony reaches across the field and takes Dabura away.");
		b.add("message.jjkcraft.modulo.mahoraga.adapted", "Mahoraga's wheel turns once more: it has adapted to Light.");

		b.add("objective.jjkcraft.modulo.wait_descent", "Wait for the Naunax Descent, then board the ship");
		b.add("objective.jjkcraft.modulo.reach_grade", "Reach Grade 1 to see the Naunax Descent");
		b.add("objective.jjkcraft.modulo.done", "Every Modulo mission is complete");

		b.add("cast_failure.jjkcraft.not_enough_mul", "Not enough Mul energy");
		b.add("death.attack.jjkcraft.light", "%1$s was pierced by Light");
		b.add("death.attack.jjkcraft.light.player", "%1$s was pierced by %2$s's Light");
		b.add("death.attack.jjkcraft.darkness", "%1$s was swallowed by darkness");
		b.add("death.attack.jjkcraft.darkness.player", "%1$s was swallowed by %2$s's darkness");

		b.add("hud.jjkcraft.modulo.mul", "MUL");
		b.add("hud.jjkcraft.modulo.mission", "Modulo %s/%s");
		b.add("hud.jjkcraft.modulo.distance", "%s m");
		b.add("hud.jjkcraft.modulo.elsewhere", "elsewhere");
		b.add("hud.jjkcraft.modulo.descent", "The Naunax: %s, %s");
		b.add("hud.jjkcraft.modulo.continue", "Right-click to continue");
		b.add("hud.jjkcraft.modulo.stage_done", "Done: %s");

		b.add("commands.jjkcraft.modulo.descent_stopped", "The Naunax Descent ends");
		b.add("commands.jjkcraft.modulo.descent_started", "The Naunax descends; its ramp reaches %s, %s, %s");
		b.add("commands.jjkcraft.modulo.descent_status", "Modulo: %s. Descent ramp: %s. Next Descent at game time %s");
		b.add("commands.jjkcraft.modulo.done", "Done for %s players");
		b.add("commands.jjkcraft.modulo.no_site", "No site %s (or its dimension is not loaded)");
		b.add("commands.jjkcraft.modulo.site_built", "Built site %s");
		b.add("commands.jjkcraft.modulo.no_mission", "No mission %s");
		b.add("commands.jjkcraft.modulo.mission_status", "%s: mission %s, stage %s (counter %s), %s missions done, %s Mul energy, companion %s");
	}
}
