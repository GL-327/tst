package com.jjkcraft.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import com.jjkcraft.network.payload.BlackFlashPayload;
import com.jjkcraft.network.payload.CastFeedbackPayload;
import com.jjkcraft.network.payload.CastPayload;
import com.jjkcraft.network.payload.CastTechniquePayload;
import com.jjkcraft.network.payload.DomainActionPayload;
import com.jjkcraft.network.payload.DomainEventPayload;
import com.jjkcraft.network.payload.DomainSelfPayload;
import com.jjkcraft.network.payload.DomainWorldPayload;
import com.jjkcraft.network.payload.EnergySyncPayload;
import com.jjkcraft.network.payload.ProgressSyncPayload;
import com.jjkcraft.network.payload.PromotionPayload;
import com.jjkcraft.network.payload.ReversePayload;
import com.jjkcraft.network.payload.ReverseTogglePayload;
import com.jjkcraft.network.payload.SelectSlotPayload;
import com.jjkcraft.network.payload.SensePayload;
import com.jjkcraft.network.payload.SenseTogglePayload;
import com.jjkcraft.network.payload.SetLoadoutPayload;
import com.jjkcraft.network.payload.SettingsPayload;
import com.jjkcraft.network.payload.SpendGrowthPayload;
import com.jjkcraft.network.payload.TechniqueFxPayload;
import com.jjkcraft.network.payload.TechniqueSyncPayload;
import com.jjkcraft.network.payload.CullingPayloads;
import com.jjkcraft.network.payload.VowPayloads;

/** Registers every payload type. Server receivers for client intents are registered by their systems. */
public final class JjkNetworking {
	private JjkNetworking() {
	}

	public static void init() {
        PayloadTypeRegistry.clientboundPlay().register(com.jjkcraft.network.payload.VesselPayload.TYPE,com.jjkcraft.network.payload.VesselPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(com.jjkcraft.network.payload.DomainProfilePayload.TYPE,com.jjkcraft.network.payload.DomainProfilePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(com.jjkcraft.network.payload.DomainProfilePayload.TYPE,com.jjkcraft.network.payload.DomainProfilePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(com.jjkcraft.network.payload.CombatPayloads.Endurance.TYPE,com.jjkcraft.network.payload.CombatPayloads.Endurance.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(com.jjkcraft.network.payload.ReverseStudyPayload.TYPE,com.jjkcraft.network.payload.ReverseStudyPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(com.jjkcraft.network.payload.InfinityPayload.TYPE,com.jjkcraft.network.payload.InfinityPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(com.jjkcraft.network.payload.VariantCastPayload.TYPE,com.jjkcraft.network.payload.VariantCastPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(com.jjkcraft.network.payload.OriginalArtsPayload.TYPE,com.jjkcraft.network.payload.OriginalArtsPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(com.jjkcraft.network.payload.ProjectionPayloads.Edit.TYPE,com.jjkcraft.network.payload.ProjectionPayloads.Edit.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(com.jjkcraft.network.payload.ProjectionPayloads.Reply.TYPE,com.jjkcraft.network.payload.ProjectionPayloads.Reply.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(com.jjkcraft.network.payload.CombatPayloads.AutoStats.TYPE,com.jjkcraft.network.payload.CombatPayloads.AutoStats.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(com.jjkcraft.network.payload.ChargePayload.TYPE,com.jjkcraft.network.payload.ChargePayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(com.jjkcraft.network.payload.CombatPayloads.Impact.TYPE,com.jjkcraft.network.payload.CombatPayloads.Impact.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(com.jjkcraft.network.payload.CombatPayloads.Resources.TYPE,com.jjkcraft.network.payload.CombatPayloads.Resources.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(com.jjkcraft.network.payload.PersonalizationPayloads.TuneReply.TYPE,com.jjkcraft.network.payload.PersonalizationPayloads.TuneReply.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(com.jjkcraft.network.payload.PersonalizationPayloads.CreativeFamily.TYPE,com.jjkcraft.network.payload.PersonalizationPayloads.CreativeFamily.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(com.jjkcraft.network.payload.ShikigamiPayloads.Recipe.TYPE,com.jjkcraft.network.payload.ShikigamiPayloads.Recipe.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(com.jjkcraft.network.payload.TechniqueMenuPayloads.Action.TYPE,com.jjkcraft.network.payload.TechniqueMenuPayloads.Action.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(com.jjkcraft.network.payload.TechniqueMenuPayloads.View.TYPE,com.jjkcraft.network.payload.TechniqueMenuPayloads.View.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(com.jjkcraft.network.payload.ShikigamiPayloads.Reply.TYPE,com.jjkcraft.network.payload.ShikigamiPayloads.Reply.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(com.jjkcraft.network.payload.PersonalizationPayloads.CreativeEdit.TYPE,com.jjkcraft.network.payload.PersonalizationPayloads.CreativeEdit.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(com.jjkcraft.network.payload.PersonalizationPayloads.CreativeReply.TYPE,com.jjkcraft.network.payload.PersonalizationPayloads.CreativeReply.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(com.jjkcraft.network.payload.CombatPosePayload.TYPE,com.jjkcraft.network.payload.CombatPosePayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(com.jjkcraft.network.payload.PersonalizationPayloads.Tune.TYPE,com.jjkcraft.network.payload.PersonalizationPayloads.Tune.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(com.jjkcraft.network.payload.PersonalizationPayloads.Practice.TYPE,com.jjkcraft.network.payload.PersonalizationPayloads.Practice.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(com.jjkcraft.network.payload.PersonalizationPayloads.View.TYPE,com.jjkcraft.network.payload.PersonalizationPayloads.View.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(com.jjkcraft.network.payload.TraitPayload.TYPE, com.jjkcraft.network.payload.TraitPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(com.jjkcraft.network.payload.SoulPayloads.Action.TYPE, com.jjkcraft.network.payload.SoulPayloads.Action.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(com.jjkcraft.network.payload.SoulPayloads.View.TYPE, com.jjkcraft.network.payload.SoulPayloads.View.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(com.jjkcraft.network.payload.CombatPayloads.Input.TYPE, com.jjkcraft.network.payload.CombatPayloads.Input.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(com.jjkcraft.network.payload.CombatPayloads.View.TYPE, com.jjkcraft.network.payload.CombatPayloads.View.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(com.jjkcraft.network.payload.CombatPayloads.Imbuement.TYPE,com.jjkcraft.network.payload.CombatPayloads.Imbuement.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(com.jjkcraft.network.payload.SpatialShotPayload.TYPE,com.jjkcraft.network.payload.SpatialShotPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(com.jjkcraft.network.payload.AwakeningPayloads.Choose.TYPE, com.jjkcraft.network.payload.AwakeningPayloads.Choose.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(com.jjkcraft.network.payload.AwakeningPayloads.View.TYPE, com.jjkcraft.network.payload.AwakeningPayloads.View.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(EnergySyncPayload.TYPE, EnergySyncPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(SettingsPayload.TYPE, SettingsPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(ProgressSyncPayload.TYPE, ProgressSyncPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(PromotionPayload.TYPE, PromotionPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(SpendGrowthPayload.TYPE, SpendGrowthPayload.CODEC);

		PayloadTypeRegistry.clientboundPlay().register(TechniqueSyncPayload.TYPE, TechniqueSyncPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(TechniqueFxPayload.TYPE, TechniqueFxPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(CastFeedbackPayload.TYPE, CastFeedbackPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(CastPayload.TYPE, CastPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(CastTechniquePayload.TYPE, CastTechniquePayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(SetLoadoutPayload.TYPE, SetLoadoutPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(SelectSlotPayload.TYPE, SelectSlotPayload.CODEC);

		PayloadTypeRegistry.clientboundPlay().register(SensePayload.TYPE, SensePayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(SenseTogglePayload.TYPE, SenseTogglePayload.CODEC);

		PayloadTypeRegistry.serverboundPlay().register(DomainActionPayload.TYPE, DomainActionPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(DomainSelfPayload.TYPE, DomainSelfPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(DomainWorldPayload.TYPE, DomainWorldPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(DomainEventPayload.TYPE, DomainEventPayload.CODEC);

		PayloadTypeRegistry.clientboundPlay().register(BlackFlashPayload.TYPE, BlackFlashPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(ReversePayload.TYPE, ReversePayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(ReverseTogglePayload.TYPE, ReverseTogglePayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(VowPayloads.Commit.TYPE, VowPayloads.Commit.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(VowPayloads.Renounce.TYPE, VowPayloads.Renounce.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(VowPayloads.Sync.TYPE, VowPayloads.Sync.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(CullingPayloads.Sync.TYPE, CullingPayloads.Sync.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(CullingPayloads.Event.TYPE, CullingPayloads.Event.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(CullingPayloads.Action.TYPE, CullingPayloads.Action.CODEC);
	}

	/**
	 * Sends a payload if the player's client can receive it. Clients without the mod (and GameTest
	 * mock players) are skipped instead of erroring.
	 */
	public static void send(ServerPlayer player, CustomPacketPayload payload) {
		if (ServerPlayNetworking.canSend(player, payload.type())) {
			ServerPlayNetworking.send(player, payload);
		}
	}
}
