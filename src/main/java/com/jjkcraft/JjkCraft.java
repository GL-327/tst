package com.jjkcraft;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.resources.Identifier;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

import com.jjkcraft.command.CullingCommands;
import com.jjkcraft.culling.CullingGame;
import com.jjkcraft.culling.entity.CullingEntities;
import com.jjkcraft.culling.rule.RuleEffects;
import com.jjkcraft.culling.rule.RuleTriggers;
import com.jjkcraft.attachment.JjkAttachments;
import com.jjkcraft.blackflash.BlackFlash;
import com.jjkcraft.block.JjkBlocks;
import com.jjkcraft.block.TemporaryBlocks;
import com.jjkcraft.command.CurseCommands;
import com.jjkcraft.command.DomainCommands;
import com.jjkcraft.command.SorceryCommands;
import com.jjkcraft.command.JjkCommands;
import com.jjkcraft.command.ProgressCommands;
import com.jjkcraft.command.TechniqueCommands;
import com.jjkcraft.config.JjkConfig;
import com.jjkcraft.curse.CurseEntities;
import com.jjkcraft.curse.mechanic.CurseMechanics;
import com.jjkcraft.curse.spawn.CurseSpawner;
import com.jjkcraft.domain.DomainService;
import com.jjkcraft.domain.behavior.ArsenalBehavior;
import com.jjkcraft.domain.behavior.DomainBehaviors;
import com.jjkcraft.effect.JjkEffects;
import com.jjkcraft.energy.EnergyProfiles;
import com.jjkcraft.energy.EnergySync;
import com.jjkcraft.entity.JjkEntities;
import com.jjkcraft.fear.Fear;
import com.jjkcraft.item.JjkComponents;
import com.jjkcraft.item.JjkItems;
import com.jjkcraft.network.JjkNetworking;
import com.jjkcraft.particle.JjkParticles;
import com.jjkcraft.registry.JjkRegistries;
import com.jjkcraft.registry.LegacyIds;
import com.jjkcraft.reverse.ReverseTechnique;
import com.jjkcraft.sense.Sensing;
import com.jjkcraft.sorcerer.Awakening;
import com.jjkcraft.sorcerer.HeavenlyRestriction;
import com.jjkcraft.sorcerer.Progression;
import com.jjkcraft.sorcerer.PlayerActions;
import com.jjkcraft.sorcerer.SorcererLifecycle;
import com.jjkcraft.sorcerer.UnboundRitual;
import com.jjkcraft.sound.JjkSounds;
import com.jjkcraft.technique.TechniqueBehaviors;
import com.jjkcraft.technique.TechniqueService;
import com.jjkcraft.technique.field.SpatialFields;
import com.jjkcraft.vow.Vows;

/**
 * Common entrypoint. Everything registered here exists on both the dedicated server and the client.
 */
public final class JjkCraft implements ModInitializer {
	public static final String MOD_ID = "jjkcraft";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static boolean initialized;

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	public static String version() {
		return FabricLoader.getInstance().getModContainer(MOD_ID)
				.map(container -> container.getMetadata().getVersion().getFriendlyString())
				.orElse("unknown");
	}

	/** True once the common entrypoint has run. Used by the smoke GameTest. */
	public static boolean isInitialized() {
		return initialized;
	}

	@Override
	public void onInitialize() {
		JjkConfig.init();
		TechniqueBehaviors.init();
		DomainBehaviors.init();
		RuleTriggers.init();
		RuleEffects.init();
		JjkRegistries.init();
		JjkAttachments.init();
		JjkSounds.init();
		JjkParticles.init();
		JjkEffects.init();
		JjkEntities.init();
		JjkBlocks.init();
		JjkComponents.init();
		CurseEntities.init();
		CullingEntities.init();
		JjkItems.init();
		com.jjkcraft.modulo.Modulo.init();
		LegacyIds.registerAliases();
		JjkNetworking.init();

		EnergyProfiles.init();
		EnergySync.init();
		SorcererLifecycle.init();
		Progression.init();
		HeavenlyRestriction.init();
		Awakening.init();
		SpatialFields.init();
        com.jjkcraft.technique.TechniqueTerrain.init();
		TechniqueService.init();
        com.jjkcraft.technique.FleshSacrifice.init();
        com.jjkcraft.technique.ProjectionPlans.init();
        com.jjkcraft.technique.OriginalArts.init();
		com.jjkcraft.technique.Personalization.init();
		com.jjkcraft.energy.SorceryPractice.init();
		com.jjkcraft.awakening.CharacterTraits.init();
		com.jjkcraft.awakening.BodyTransfer.init();
		com.jjkcraft.awakening.ControlledCurses.init();
		com.jjkcraft.entity.shikigami.ShikigamiFusion.init();
        com.jjkcraft.entity.shikigami.TenShadows.init();
        com.jjkcraft.technique.TechniqueFamilies.init();
		com.jjkcraft.awakening.CharacterAwakening.init();

		CurseMechanics.init();
		Fear.init();
		CurseSpawner.init();
		TemporaryBlocks.init();
		Sensing.init();
		DomainService.init();
		ArsenalBehavior.init();
		BlackFlash.init();
		com.jjkcraft.combat.CursedCombat.init();
        com.jjkcraft.vessel.CursedVessels.init();
        com.jjkcraft.training.Training.init();
		ReverseTechnique.init();
		PlayerActions.init();
		Vows.init();
		UnboundRitual.init();
		CullingGame.init();
		com.jjkcraft.death.SoulSystem.init();
		com.jjkcraft.item.tool.CursedTools.init();
		com.jjkcraft.technique.behavior.ThornsAuraBehavior.init();

		JjkCommands.init();
		ProgressCommands.init();
		TechniqueCommands.init();
		CurseCommands.init();
		DomainCommands.init();
		SorceryCommands.init();
		CullingCommands.init();

		initialized = true;
		LOGGER.info("Jujutsu Craft {} initialized (common)", version());
	}
}
