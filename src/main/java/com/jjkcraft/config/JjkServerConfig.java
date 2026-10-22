package com.jjkcraft.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment;

/**
 * Server-owned settings, stored in {@code config/jjkcraft-server.json5}. The server is authoritative:
 * clients receive only the values they need for prediction (see {@code SettingsPayload}).
 * Defaults and their reasoning are documented in docs/BALANCE.md.
 */
@Config(name = "jjkcraft-server")
public class JjkServerConfig implements ConfigData {
	@ConfigEntry.Gui.CollapsibleObject
	public Systems systems = new Systems();

	@ConfigEntry.Gui.CollapsibleObject
	public Energy energy = new Energy();

	@ConfigEntry.Gui.CollapsibleObject
	public Death death = new Death();

	@ConfigEntry.Gui.CollapsibleObject
	public Progression progression = new Progression();

	@ConfigEntry.Gui.CollapsibleObject
	public Casting casting = new Casting();

	@ConfigEntry.Gui.CollapsibleObject
	public Domains domains = new Domains();

	@ConfigEntry.Gui.CollapsibleObject
	public Curses curses = new Curses();

	@ConfigEntry.Gui.CollapsibleObject
	public CullingGame cullingGame = new CullingGame();

	@ConfigEntry.Gui.CollapsibleObject
	public Modulo modulo = new Modulo();

	public static class Systems {
		@Comment("Use framed Cursed Combat for awakened players and curse melee. C toggles it per player.")
		public boolean cursedCombat = true;
		@Comment("Enable the Culling Game event (colonies, points, rules).")
		public boolean cullingGame = true;
		@Comment("Allow Domain Expansions and Simple Domains.")
		public boolean domains = true;
		@Comment("Spawn curses from the fear field. Commands and spawn eggs still work when this is off.")
		public boolean curseSpawns = true;
	}

	public static class Energy {
		@Comment("CE capacity of a Grade 4 sorcerer with no growth points.")
		public double baseCapacity = 100.0;
		@Comment("Percent of max CE restored per second while regenerating normally.")
		public double regenPercentPerSecond = 1.5;
		@Comment("Regen multiplier after being idle (no casts or CE actions).")
		public double idleRegenMultiplier = 2.5;
		@Comment("Seconds without a CE action before idle regen starts.")
		public double idleDelaySeconds = 5.0;
		@Comment("Spending that leaves CE below this percent of max triggers overexertion.")
		public double overexertThresholdPercent = 10.0;
		@Comment("Seconds of blocked regen after overexertion.")
		public double overexertSeconds = 3.0;
	}

	public static class Death {
		@Comment("Enable Survival death choices, cursed objects, Vessels and hardcore pacts.")
		public boolean choices = true;
		public int objectMinutes = 20;
		public boolean newCharacterOnLoss = true;
		public int vesselCost = 50;
		public int possessionThreshold = 3;
		@Comment("KEEP (default), PARTIAL or RESET_TECHNIQUES. See README.")
		public DeathPolicy policy = DeathPolicy.KEEP;
		@Comment("Percent of max CE a player has after respawning.")
		public double respawnEnergyPercent = 50.0;
		@Comment("PARTIAL: percent of grade experience in the current grade that is lost.")
		public double partialXpLossPercent = 50.0;
		@Comment("PARTIAL: percent of each technique's mastery that is lost.")
		public double partialMasteryLossPercent = 25.0;
	}

	public static class Progression {
		@Comment("Culling Game points charged by a colony Vessel for an Awakening Rite.")
		public int riteCost = 100;
		@Comment("Allow choosing an innate family instead of the Inherit/Random paths. Server owned.")
		public boolean allowTechniqueChoice = false;
		@Comment("Chance of Six Eyes for a Gojo awakening, in percent.")
		public double sixEyesChancePercent = 1.0;
		@Comment("Awaken players (roll an innate technique or Heavenly Restriction) the first time they join.")
		public boolean awakenOnFirstJoin = true;
		@Comment("Chance (percent) that awakening rolls Heavenly Restriction instead of a technique.")
		public double heavenlyRestrictionChancePercent = 2.0;
		@Comment("Multiplier on all grade experience gained.")
		public double gradeXpMultiplier = 1.0;
	}

	public static class Casting {
        @Comment("Damage multiplier for cursed techniques. Does not multiply ordinary physical attacks, healing or crowd-control durations.")
        public double techniqueDamageMultiplier = com.jjkcraft.core.technique.TechniqueMath.DEFAULT_DAMAGE_MULTIPLIER;
        @Comment("Allow destructive techniques to damage terrain. Containers, unbreakable blocks and block-break event claims are protected; mobs respect mobGriefing.")
        public boolean terrainDamage = true;
        @Comment("Oldest spatial fields retire when a caster reaches this limit. A Mini Blue Cluster uses three fields.")
        public int maxSpatialFieldsPerCaster = 16;
        @Comment("Maximum simultaneous attraction/repulsion fields per dimension; oldest fields retire first.")
        public int maxSpatialFieldsPerLevel = 128;
		@Comment("Max cast requests a client may send per second before the rest are dropped.")
		public double maxCastRequestsPerSecond = 8.0;
		@Comment("Chance (percent) that a reinforced strike becomes a Black Flash before modifiers.")
		public double blackFlashBaseChancePercent = 4.0;
		@Comment("Black Flash damage exponent: damage = base * ce_ratio ^ exponent, ce_ratio 1.3 (empty pool) to 1.7 (full).")
		public double blackFlashExponent = 2.5;
		@Comment("Chance (percent) that a lethal blow awakens Reverse Cursed Technique in an eligible sorcerer.")
		public double reverseUnlockChancePercent = 35.0;
		@Comment("Lowest grade rank that can awaken Reverse Cursed Technique (4 = Semi-Grade 1).")
		public int reverseUnlockMinRank = 4;
	}

	public static class Domains {
		@Comment("Multiplier on every domain's CE cost per second.")
		public double costMultiplier = 1.0;
		@Comment("Seconds a caster is stunned after losing a domain clash.")
		public double clashBacklashStunSeconds = 3.0;
		@Comment("Percent of max CE lost by the loser of a domain clash.")
		public double clashBacklashEnergyPercent = 30.0;
		@Comment("Lowest grade rank that can raise a Simple Domain (0 = Grade 4, 1 = Grade 3, ...).")
		public int simpleDomainMinRank = 1;
		@Comment("CE to raise a Simple Domain.")
		public double simpleDomainCost = 20.0;
		@Comment("CE per second to hold a Simple Domain.")
		public double simpleDomainUpkeep = 3.0;
		@Comment("Simple Domain radius in blocks.")
		public double simpleDomainRadius = 3.5;
		@Comment("Seconds a Simple Domain can be held.")
		public double simpleDomainMaxSeconds = 30.0;
		@Comment("CE the holder pays each time the Simple Domain cancels a sure-hit effect.")
		public double simpleDomainCostPerCancel = 1.5;
		@Comment("Percent of damage from outside a Simple Domain that its barrier blocks.")
		public double simpleDomainBlockPercent = 50.0;
	}

	public static class Curses {
		@Comment("Multiplier on fear-driven curse spawn rates.")
		public double spawnRateMultiplier = 1.0;
		@Comment("Max naturally spawned curses near a single player.")
		public int maxCursesPerPlayer = 12;
		@Comment("Spawn encounters around Creative players too; curses still do not target them.")
		public boolean spawnAroundCreative = true;
		@Comment("Candidate locations checked every six seconds per player. Loaded chunks only.")
		public int spawnAttempts = 4;
		@Comment("Fear value (0-100) a chunk section must reach before curses can spawn there.")
		public double fearSpawnThreshold = 20.0;
		@Comment("Chance per attempt of a weak ambient encounter in a calm area, as a percentage.")
		public double calmEncounterPercent = 8.0;
	}

	public static class CullingGame {
		@Comment("Points for killing a sorcerer (a player or NPC with an awakened technique).")
		public int sorcererPoints = 5;
		@Comment("Points for killing a non-sorcerer participant.")
		public int nonSorcererPoints = 1;
		@Comment("Points a player spends to add a rule through the game master.")
		public int ruleCost = 100;
		@Comment("In-game days after awakening a technique to join a colony.")
		public int entryWindowDays = 19;
		@Comment("End condition: game ends after this many in-game days (0 = no day limit).")
		public int endAfterDays = 60;
		@Comment("End condition: game ends when one colony has this many or fewer players left (0 = off).")
		public int endWhenPlayersLeft = 0;
		@Comment("Penalty when the entry window passes without joining: REMOVE_TECHNIQUE or NONE.")
		public String missedEntryPenalty = "REMOVE_TECHNIQUE";
		@Comment("Start the game by itself, around world spawn, when the first awakened player joins a world where it has not run yet.")
		public boolean autoStart = true;
		@Comment("Colonies raised when the game starts.")
		public int colonies = 4;
		@Comment("Colony radius in blocks.")
		public int colonyRadius = 64;
		@Comment("Distance from the start point (world spawn) to each colony's centre.")
		public int colonyDistance = 400;
		@Comment("Entrants (non-player participants) brought into each colony when it rises.")
		public int entrantsPerColony = 6;
		@Comment("Share of entrants who are sorcerers, in percent.")
		public int sorcererEntrantPercent = 35;
		@Comment("New entrants the game master brings into each colony every day.")
		public int entrantsPerDay = 2;
		@Comment("Most entrants a colony holds at once.")
		public int maxEntrantsPerColony = 10;
		@Comment("Share of a player participant's points lost on death, in percent.")
		public int deathPointLossPercent = 50;
	}

	@Override
	public void validatePostLoad() {
		energy.baseCapacity = clamp(energy.baseCapacity, 1, 100_000);
		energy.regenPercentPerSecond = clamp(energy.regenPercentPerSecond, 0, 100);
		energy.idleRegenMultiplier = clamp(energy.idleRegenMultiplier, 1, 20);
		energy.idleDelaySeconds = clamp(energy.idleDelaySeconds, 0, 600);
		energy.overexertThresholdPercent = clamp(energy.overexertThresholdPercent, 0, 100);
		energy.overexertSeconds = clamp(energy.overexertSeconds, 0, 120);
		death.respawnEnergyPercent = clamp(death.respawnEnergyPercent, 0, 100);
		death.objectMinutes = Math.clamp(death.objectMinutes, 1, 120);
		death.vesselCost = Math.clamp(death.vesselCost, 0, 10000);
		death.possessionThreshold = Math.clamp(death.possessionThreshold, 1, 20);
		progression.riteCost = Math.clamp(progression.riteCost, 0, 10000);
		death.partialXpLossPercent = clamp(death.partialXpLossPercent, 0, 100);
		death.partialMasteryLossPercent = clamp(death.partialMasteryLossPercent, 0, 100);
		if (death.policy == null) {
			death.policy = DeathPolicy.KEEP;
		}

		progression.heavenlyRestrictionChancePercent = clamp(progression.heavenlyRestrictionChancePercent, 0, 100);
		progression.sixEyesChancePercent = clamp(progression.sixEyesChancePercent, 0, 100);
		progression.gradeXpMultiplier = clamp(progression.gradeXpMultiplier, 0, 100);
		casting.maxCastRequestsPerSecond = clamp(casting.maxCastRequestsPerSecond, 1, 100);
        casting.maxSpatialFieldsPerCaster=Math.clamp(casting.maxSpatialFieldsPerCaster,3,128);
        casting.maxSpatialFieldsPerLevel=Math.clamp(casting.maxSpatialFieldsPerLevel,12,1024);
		casting.blackFlashBaseChancePercent = clamp(casting.blackFlashBaseChancePercent, 0, 100);
		casting.blackFlashExponent = clamp(casting.blackFlashExponent, 1, 5);
		domains.costMultiplier = clamp(domains.costMultiplier, 0, 10);
		domains.clashBacklashStunSeconds = clamp(domains.clashBacklashStunSeconds, 0, 30);
		domains.clashBacklashEnergyPercent = clamp(domains.clashBacklashEnergyPercent, 0, 100);
		domains.simpleDomainMinRank = Math.clamp(domains.simpleDomainMinRank, 0, 32);
		domains.simpleDomainCost = clamp(domains.simpleDomainCost, 0, 10_000);
		domains.simpleDomainUpkeep = clamp(domains.simpleDomainUpkeep, 0, 1000);
		domains.simpleDomainRadius = clamp(domains.simpleDomainRadius, 1, 16);
		domains.simpleDomainMaxSeconds = clamp(domains.simpleDomainMaxSeconds, 1, 3600);
		domains.simpleDomainCostPerCancel = clamp(domains.simpleDomainCostPerCancel, 0, 1000);
		domains.simpleDomainBlockPercent = clamp(domains.simpleDomainBlockPercent, 0, 100);
		curses.spawnRateMultiplier = clamp(curses.spawnRateMultiplier, 0, 20);
		curses.maxCursesPerPlayer = (int) clamp(curses.maxCursesPerPlayer, 0, 64);
		curses.spawnAttempts = (int) clamp(curses.spawnAttempts, 1, 12);
		curses.fearSpawnThreshold = clamp(curses.fearSpawnThreshold, 0, 100);
		curses.calmEncounterPercent = clamp(curses.calmEncounterPercent, 0, 100);
		cullingGame.sorcererPoints = (int) clamp(cullingGame.sorcererPoints, 0, 1000);
		cullingGame.nonSorcererPoints = (int) clamp(cullingGame.nonSorcererPoints, 0, 1000);
		cullingGame.ruleCost = (int) clamp(cullingGame.ruleCost, 1, 100_000);
		cullingGame.entryWindowDays = (int) clamp(cullingGame.entryWindowDays, 1, 1000);
		cullingGame.endAfterDays = (int) clamp(cullingGame.endAfterDays, 0, 10_000);
		cullingGame.endWhenPlayersLeft = (int) clamp(cullingGame.endWhenPlayersLeft, 0, 1000);
		cullingGame.colonies = (int) clamp(cullingGame.colonies, 1, 16);
		cullingGame.colonyRadius = (int) clamp(cullingGame.colonyRadius, 16, 256);
		cullingGame.colonyDistance = (int) clamp(cullingGame.colonyDistance, 0, 100_000);
		cullingGame.entrantsPerColony = (int) clamp(cullingGame.entrantsPerColony, 0, 64);
		cullingGame.sorcererEntrantPercent = (int) clamp(cullingGame.sorcererEntrantPercent, 0, 100);
		cullingGame.entrantsPerDay = (int) clamp(cullingGame.entrantsPerDay, 0, 64);
		cullingGame.maxEntrantsPerColony = (int) clamp(cullingGame.maxEntrantsPerColony, 0, 64);
		cullingGame.deathPointLossPercent = (int) clamp(cullingGame.deathPointLossPercent, 0, 100);
		if (!"NONE".equals(cullingGame.missedEntryPenalty)) {
			cullingGame.missedEntryPenalty = "REMOVE_TECHNIQUE";
		}
	}

	private static double clamp(double value, double min, double max) {
		return Double.isNaN(value) ? min : Math.max(min, Math.min(max, value));
	}

	/** The Modulo expansion (docs/MODULO.md). */
	public static class Modulo {
		@Comment("Integrated Modulo world events, Mul energy and story missions. Enabled by default.")
		public boolean enabled = true;
		@Comment("Mul energy made for each point of cursed energy poured into Mul.")
		public double mulEnergyRatio = 0.5;
		@Comment("Most Mul energy a player can hold.")
		public double maxMulEnergy = 200.0;
		@Comment("Play the epilogue sequence at the end of the last mission. Off skips the sequence; the rewards are still given.")
		public boolean epilogue = true;
		@Comment("Lowest grade that sees the Naunax Descent.")
		public String descentMinGrade = "jjkcraft:grade_2";
		@Comment("Game days between one Naunax Descent and the next.")
		public int descentIntervalDays = 3;
		@Comment("Real minutes the Naunax stays over the badlands.")
		public int descentMinutes = 10;
		@Comment("How far from the first eligible player to look for badlands, in blocks.")
		public int descentSearchRadius = 512;
		@Comment("Share of a boss's base health added for each fighter past the first.")
		public double bossHealthPerExtraPlayer = 0.6;
	}
}
