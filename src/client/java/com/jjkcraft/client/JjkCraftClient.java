package com.jjkcraft.client;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.loader.api.FabricLoader;

import com.jjkcraft.client.compat.IrisCompat;
import com.jjkcraft.client.hud.CullingPanelHud;
import com.jjkcraft.client.render.culling.EntrantRenderer;
import com.jjkcraft.client.render.culling.KoganeRenderer;
import com.jjkcraft.client.screen.CullingScreen;
import com.jjkcraft.client.state.ClientCulling;
import com.jjkcraft.client.vfx.CullingFx;
import com.jjkcraft.culling.entity.CullingEntities;
import com.jjkcraft.network.payload.CullingPayloads;
import com.jjkcraft.JjkCraft;
import com.jjkcraft.client.config.ClientConfig;
import com.jjkcraft.client.hud.EnergyBarHud;
import com.jjkcraft.client.hud.DomainMeterHud;
import com.jjkcraft.client.hud.DomainTitle;
import com.jjkcraft.client.hud.GradeBadgeHud;
import com.jjkcraft.client.hud.SenseRadarHud;
import com.jjkcraft.client.hud.TechniqueHotbarHud;
import com.jjkcraft.client.render.CursedBoltRenderer;
import com.jjkcraft.client.render.ShadowDivineDogRenderer;
import com.jjkcraft.client.render.curse.CurseRenderer;
import com.jjkcraft.client.screen.CodexScreen;
import com.jjkcraft.client.screen.VowBuilderScreen;
import com.jjkcraft.client.screen.GrowthTreeScreen;
import com.jjkcraft.client.screen.JjkScreen;
import com.jjkcraft.client.screen.LoadoutScreen;
import com.jjkcraft.client.screen.ProfileScreen;
import com.jjkcraft.client.state.ClientDomains;
import com.jjkcraft.client.state.ClientEnergy;
import com.jjkcraft.client.state.ClientProgress;
import com.jjkcraft.client.state.ClientSensing;
import com.jjkcraft.client.state.ClientSorcery;
import com.jjkcraft.client.state.ClientTechniques;
import com.jjkcraft.client.vfx.BlackFlashFx;
import com.jjkcraft.client.vfx.ClientFx;
import com.jjkcraft.client.vfx.DomainAmbience;
import com.jjkcraft.client.vfx.DomainFx;
import com.jjkcraft.client.vfx.DomainOverlay;
import com.jjkcraft.client.vfx.Emitters;
import com.jjkcraft.client.vfx.JjkParticle;
import com.jjkcraft.client.vfx.OverexertionVignette;
import com.jjkcraft.client.vfx.PromotionCeremony;
import com.jjkcraft.client.vfx.TechniqueFxClient;
import com.jjkcraft.client.vfx.WorldFx;
import com.jjkcraft.curse.CurseEntities;
import com.jjkcraft.domain.Domains;
import com.jjkcraft.curse.CurseEntity;
import com.jjkcraft.entity.JjkEntities;
import com.jjkcraft.network.payload.BlackFlashPayload;
import com.jjkcraft.network.payload.CastFeedbackPayload;
import com.jjkcraft.network.payload.DomainActionPayload;
import com.jjkcraft.network.payload.DomainEventPayload;
import com.jjkcraft.network.payload.DomainSelfPayload;
import com.jjkcraft.network.payload.DomainWorldPayload;
import com.jjkcraft.network.payload.EnergySyncPayload;
import com.jjkcraft.network.payload.ProgressSyncPayload;
import com.jjkcraft.network.payload.PromotionPayload;
import com.jjkcraft.network.payload.ReversePayload;
import com.jjkcraft.network.payload.ReverseTogglePayload;
import com.jjkcraft.network.payload.SensePayload;
import com.jjkcraft.network.payload.SettingsPayload;
import com.jjkcraft.network.payload.TechniqueFxPayload;
import com.jjkcraft.network.payload.TechniqueSyncPayload;
import com.jjkcraft.network.payload.VowPayloads;
import com.jjkcraft.particle.JjkParticles;

/**
 * Client entrypoint: HUD, screens, rendering, keybinds. Never loaded on a dedicated server.
 */
public final class JjkCraftClient implements ClientModInitializer {
	private static boolean initialized;

	/** True once the client entrypoint has run. Used by the client smoke GameTest. */
	public static boolean isInitialized() {
		return initialized;
	}

	@Override
	public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(com.jjkcraft.network.payload.ReverseStudyPayload.TYPE,(payload,ctx)->ClientSorcery.accept(payload));
        HudElementRegistry.addLast(JjkCraft.id("reverse_practice"),(g,dt)->{
            var mc=Minecraft.getInstance();var study=ClientSorcery.study();if(!study.running()||mc.player==null||mc.gui.hud.isHidden())return;
            int x=g.guiWidth()/2-92,y=g.guiHeight()/2+44;g.fill(x,y,x+184,y+30,0xD0141726);
            com.jjkcraft.client.ui.Ink.text(g,"REVERSE FLOW · "+(study.lessons()+1)+" / 3",x+5,y+4,0xFF7CDDB6,1);
            com.jjkcraft.client.ui.Ink.text(g,"Stand still · crouch to cancel",x+5,y+15,0xFFCCC7D7,1);g.fill(x+4,y+27,x+4+176*study.ticks()/200,y+29,0xFF7CDDB6);
        });
        ClientPlayNetworking.registerGlobalReceiver(com.jjkcraft.network.payload.InfinityPayload.TYPE,(payload,ctx)->ClientTechniques.accept(payload));
		ClientConfig.init();
        com.jjkcraft.client.design.UiDesign.init();
        com.jjkcraft.client.training.TrainingClient.init();
        EntityRendererRegistry.register(com.jjkcraft.entity.JjkEntities.TRAINING_DUMMY,com.jjkcraft.client.training.TrainingDummyRenderer::new);
        com.jjkcraft.client.vfx.TexturedFx.init();
        com.jjkcraft.client.combat.OriginalArtsHud.init();
        ClientPlayNetworking.registerGlobalReceiver(com.jjkcraft.network.payload.ProjectionPayloads.Reply.TYPE,(reply,context)->com.jjkcraft.client.screen.ProjectionPlannerScreen.accept(reply));
		JjkKeys.init();
		ClientPlayNetworking.registerGlobalReceiver(com.jjkcraft.network.payload.PersonalizationPayloads.TuneReply.TYPE,(reply,context)->com.jjkcraft.client.state.ClientPersonalization.accept(reply));
		com.jjkcraft.client.combat.CombatAnimations.init();
		ClientPlayNetworking.registerGlobalReceiver(com.jjkcraft.network.payload.PersonalizationPayloads.View.TYPE,(payload,context)->com.jjkcraft.client.state.ClientPersonalization.accept(payload));
		ClientPlayNetworking.registerGlobalReceiver(com.jjkcraft.network.payload.PersonalizationPayloads.CreativeReply.TYPE,(payload,context)->com.jjkcraft.client.state.ClientPersonalization.accept(payload));
		ClientPlayNetworking.registerGlobalReceiver(com.jjkcraft.network.payload.ShikigamiPayloads.Reply.TYPE,(payload,context)->com.jjkcraft.client.screen.ShikigamiFusionScreen.accept(payload));
		com.jjkcraft.client.combat.CombatClient.init();
        com.jjkcraft.client.domain.DomainEditor.init();
        com.jjkcraft.client.vessel.VesselClient.init();
		com.jjkcraft.client.death.SoulClient.init();
		com.jjkcraft.client.state.ClientTraits.init();
		ClientPlayNetworking.registerGlobalReceiver(com.jjkcraft.network.payload.AwakeningPayloads.View.TYPE,
			(payload, context) -> com.jjkcraft.client.state.ClientAwakening.accept(payload));
		JjkKeys.addHandler(com.jjkcraft.client.state.ClientAwakening::tick);
		ClientFx.init();
		Emitters.init();
		WorldFx.init();
		com.jjkcraft.client.vfx.AdvancedFx.init();
		DomainFx.init();
		CullingFx.init();
		com.jjkcraft.client.modulo.ModuloClient.init();
		if (FabricLoader.getInstance().isModLoaded("iris")) {
			IrisCompat.init();
		}

		ClientPlayNetworking.registerGlobalReceiver(EnergySyncPayload.TYPE, (payload, context) -> ClientEnergy.accept(payload));
		ClientPlayNetworking.registerGlobalReceiver(SettingsPayload.TYPE, (payload, context) -> ClientEnergy.accept(payload));
		ClientPlayNetworking.registerGlobalReceiver(ProgressSyncPayload.TYPE, (payload, context) -> ClientProgress.accept(payload));
		ClientPlayNetworking.registerGlobalReceiver(PromotionPayload.TYPE, (payload, context) -> PromotionCeremony.start(payload));
		ClientPlayNetworking.registerGlobalReceiver(TechniqueSyncPayload.TYPE, (payload, context) -> ClientTechniques.accept(payload));
        ClientPlayNetworking.registerGlobalReceiver(com.jjkcraft.network.payload.TechniqueMenuPayloads.View.TYPE,(payload,context)->com.jjkcraft.client.state.ClientTechniqueMenus.accept(payload));
        ClientPlayNetworking.registerGlobalReceiver(com.jjkcraft.network.payload.ChargePayload.TYPE,(payload,context)->ClientTechniques.accept(payload));
		ClientPlayNetworking.registerGlobalReceiver(CastFeedbackPayload.TYPE, (payload, context) -> {
			ClientTechniques.accept(payload);
			ClientDomains.accept(payload);
		});
		ClientPlayNetworking.registerGlobalReceiver(TechniqueFxPayload.TYPE, (payload, context) -> TechniqueFxClient.handle(payload));
		ClientPlayNetworking.registerGlobalReceiver(SensePayload.TYPE, (payload, context) -> ClientSensing.accept(payload));
		ClientPlayNetworking.registerGlobalReceiver(DomainWorldPayload.TYPE, (payload, context) -> ClientDomains.accept(payload));
		ClientPlayNetworking.registerGlobalReceiver(DomainSelfPayload.TYPE, (payload, context) -> ClientDomains.accept(payload));
		ClientPlayNetworking.registerGlobalReceiver(DomainEventPayload.TYPE, (payload, context) -> ClientDomains.accept(payload));
		ClientPlayNetworking.registerGlobalReceiver(BlackFlashPayload.TYPE, (payload, context) -> ClientSorcery.accept(payload));
		ClientPlayNetworking.registerGlobalReceiver(ReversePayload.TYPE, (payload, context) -> ClientSorcery.accept(payload));
		ClientPlayNetworking.registerGlobalReceiver(VowPayloads.Sync.TYPE, (payload, context) -> ClientSorcery.accept(payload));
		ClientPlayNetworking.registerGlobalReceiver(CullingPayloads.Sync.TYPE, (payload, context) -> ClientCulling.accept(payload));
		ClientPlayNetworking.registerGlobalReceiver(CullingPayloads.Event.TYPE, (payload, context) -> ClientCulling.accept(payload));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			ClientEnergy.reset();
			com.jjkcraft.client.state.ClientPersonalization.reset();
			com.jjkcraft.client.state.ClientAwakening.reset();
			ClientProgress.reset();
			ClientTechniques.reset();
            com.jjkcraft.client.state.ClientTechniqueMenus.reset();
			ClientSensing.reset();
			ClientDomains.reset();
			ClientSorcery.reset();
			ClientCulling.reset();
			TechniqueFxClient.reset();
			WorldFx.clear();
			Emitters.clear();
		});

		registerParticles();
		EntityRendererRegistry.register(JjkEntities.CURSED_BOLT, CursedBoltRenderer::new);
		EntityRendererRegistry.register(JjkEntities.SHADOW_DIVINE_DOG, ShadowDivineDogRenderer::new);
		EntityRendererRegistry.register(CullingEntities.ENTRANT, EntrantRenderer::new);
		EntityRendererRegistry.register(JjkEntities.DIVINE_DOG, ctx -> new com.jjkcraft.client.render.ShikigamiRenderer<>(ctx, "divine_dog", 0.4f));
		EntityRendererRegistry.register(JjkEntities.TRANSFIGURED_SOUL,ctx->new com.jjkcraft.client.render.ShikigamiRenderer<>(ctx,"transfigured_soul",0.4f));
		for(var entry:com.jjkcraft.entity.shikigami.ShadowBeasts.TYPES.entrySet()){String model=entry.getKey().getPath();EntityRendererRegistry.register(entry.getValue(),ctx->new com.jjkcraft.client.render.ShikigamiRenderer<>(ctx,model,0.5f));}
		EntityRendererRegistry.register(JjkEntities.NUE, ctx -> new com.jjkcraft.client.render.ShikigamiRenderer<>(ctx, "nue", 0.2f));
		EntityRendererRegistry.register(JjkEntities.TOAD, ctx -> new com.jjkcraft.client.render.ShikigamiRenderer<>(ctx, "toad", 0.7f));
		com.jjkcraft.client.ui.ToolTooltips.init();
		EntityRendererRegistry.register(CullingEntities.KOGANE, KoganeRenderer::new);
		for (EntityType<? extends CurseEntity> type : CurseEntities.ALL) {
			EntityRendererRegistry.register(type, CurseRenderer::new);
		}

		HudElementRegistry.attachElementBefore(VanillaHudElements.MISC_OVERLAYS, JjkCraft.id("overexertion_vignette"), OverexertionVignette::extract);
		HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, JjkCraft.id("energy_bar"), EnergyBarHud::extract);
		HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, JjkCraft.id("grade_badge"), GradeBadgeHud::extract);
		HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, JjkCraft.id("technique_hotbar"), TechniqueHotbarHud::extract);
		HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, JjkCraft.id("sense_radar"), SenseRadarHud::extract);
		HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, JjkCraft.id("domain_meter"), DomainMeterHud::extract);
		HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, JjkCraft.id("culling_panel"), CullingPanelHud::extract);
		HudElementRegistry.attachElementBefore(VanillaHudElements.TITLE_AND_SUBTITLE, JjkCraft.id("domain_title"), DomainTitle::extract);
		HudElementRegistry.addLast(JjkCraft.id("domain_overlay"), DomainOverlay::extract);
		HudElementRegistry.addLast(JjkCraft.id("black_flash"), BlackFlashFx::extract);
		GradeBadgeHud.addIndicator(new GradeBadgeHud.Indicator(Component.translatable("hud.jjkcraft.zone_short"), 0xFFE0303F, ClientSorcery::inZone));
		GradeBadgeHud.addIndicator(new GradeBadgeHud.Indicator(Component.translatable("hud.jjkcraft.reverse_short"), 0xFF5BE38C, () -> ClientSorcery.reverse().healing()));
		GradeBadgeHud.addIndicator(new GradeBadgeHud.Indicator(Component.translatable("hud.jjkcraft.vow_short"), 0xFFF2C14E, () -> ClientSorcery.vow().active()));
		GradeBadgeHud.addIndicator(new GradeBadgeHud.Indicator(Component.translatable("hud.jjkcraft.debt_short"), 0xFF9A93A8, ClientSorcery::inDebt));
		GradeBadgeHud.addIndicator(new GradeBadgeHud.Indicator(Component.translatable("hud.jjkcraft.unsettled_short"), 0xFF8A3A5A,
				() -> net.minecraft.client.Minecraft.getInstance().player != null
						&& net.minecraft.client.Minecraft.getInstance().player.hasEffect(com.jjkcraft.effect.JjkEffects.UNSETTLED)));
		JjkKeys.addHandler(mc -> {
			boolean inGame = mc.player != null && mc.gui.screen() == null;
			while (JjkKeys.DOMAIN.consumeClick()) {
				if (inGame) {
					ClientPlayNetworking.send(new DomainActionPayload(mc.player.isShiftKeyDown()
							? DomainActionPayload.Action.EXPAND_BARRIERLESS : DomainActionPayload.Action.EXPAND));
				}
			}

			while (JjkKeys.REVERSE.consumeClick()) {
				if (inGame && ClientSorcery.reverse().unlocked()) {
					ClientPlayNetworking.send(ReverseTogglePayload.INSTANCE);
				}
			}

			while (JjkKeys.SIMPLE_DOMAIN.consumeClick()) {
				if (inGame) {
					ClientPlayNetworking.send(new DomainActionPayload(DomainActionPayload.Action.SIMPLE));
				}
			}

			DomainAmbience.tickClient(mc);
		});
		GradeBadgeHud.addIndicator(new GradeBadgeHud.Indicator(Component.translatable("hud.jjkcraft.sensing_short"), 0xFF9FD8E8, ClientSensing::isActive));
		JjkKeys.addHandler(mc -> {
			while (JjkKeys.SENSE.consumeClick()) {
				if (mc.player != null && mc.gui.screen() == null) {
					ClientSensing.toggle();
				}
			}
		});
		HudElementRegistry.attachElementBefore(VanillaHudElements.TITLE_AND_SUBTITLE, JjkCraft.id("promotion_ceremony"), PromotionCeremony::extract);

		JjkScreen.addTab(new JjkScreen.Tab(Component.translatable("screen.jjkcraft.profile"), ProfileScreen.class, ProfileScreen::new));
		JjkScreen.addTab(new JjkScreen.Tab(Component.translatable("screen.jjkcraft.growth"), GrowthTreeScreen.class, GrowthTreeScreen::new));
		JjkScreen.addTab(new JjkScreen.Tab(Component.literal("Practice"),com.jjkcraft.client.screen.PracticeScreen.class,com.jjkcraft.client.screen.PracticeScreen::new));
		JjkScreen.addTab(new JjkScreen.Tab(Component.literal("Moves"), LoadoutScreen.class, LoadoutScreen::new));
		JjkScreen.addTab(new JjkScreen.Tab(Component.translatable("screen.jjkcraft.vows"), VowBuilderScreen.class, VowBuilderScreen::new));
		JjkScreen.addTab(new JjkScreen.Tab(Component.translatable("screen.jjkcraft.codex"), CodexScreen.class, CodexScreen::new));
		JjkScreen.addTab(new JjkScreen.Tab(Component.translatable("screen.jjkcraft.culling.tab"), CullingScreen.class, CullingScreen::new));
		JjkKeys.addHandler(mc -> {
			while (JjkKeys.CULLING.consumeClick()) {
				if (mc.player != null && mc.gui.screen() == null) {
					mc.gui.setScreen(new CullingScreen());
				}
			}

			if (ClientCulling.takeOpenNegotiation() && mc.player != null) {
				mc.gui.setScreen(CullingScreen.at(CullingScreen.Page.NEGOTIATE));
			}
		});
		ProfileScreen.addSection(new ProfileScreen.Section(Component.translatable("screen.jjkcraft.profile.techniques"),
				() -> ClientTechniques.known().stream()
						.map(id -> ClientTechniques.definition(id)
								.map(def -> (Component) Component.translatable(def.name()).withColor(def.visuals().color()))
								.orElse(Component.literal(id.toString())))
						.toList()));
		ProfileScreen.addSection(new ProfileScreen.Section(Component.translatable("screen.jjkcraft.profile.vows"),
				() -> {
					var vow = ClientSorcery.vow();
					if (!vow.active() || Minecraft.getInstance().level == null) {
						return List.of(Component.translatable("screen.jjkcraft.profile.no_vow"));
					}

					return vow.vows().stream()
							.map(id -> com.jjkcraft.vow.Vows.get(Minecraft.getInstance().level.registryAccess(), id)
									.map(def -> (Component) Component.translatable(def.name()).withColor(0xF2C14E))
									.orElse(Component.literal(id.toString())))
							.toList();
				}));
		ProfileScreen.addSection(new ProfileScreen.Section(Component.translatable("screen.jjkcraft.profile.reverse"),
				() -> List.of(ClientSorcery.reverse().unlocked()
						? Component.translatable(ClientSorcery.reverse().autoHeal() ? "screen.jjkcraft.profile.reverse_on" : "screen.jjkcraft.profile.reverse_off").withColor(0x5BE38C)
						: Component.translatable("screen.jjkcraft.profile.reverse_locked"))));
		ProfileScreen.addSection(new ProfileScreen.Section(Component.translatable("screen.jjkcraft.profile.domain"),
				() -> ClientDomains.self().known()
						.flatMap(id -> Minecraft.getInstance().level == null ? java.util.Optional.empty()
								: Domains.get(Minecraft.getInstance().level.registryAccess(), id))
						.map(def -> List.of((Component) Component.translatable(def.name()).withColor(def.visuals().color())))
						.orElse(List.of(Component.translatable("screen.jjkcraft.profile.no_domain")))));

		initialized = true;
		JjkCraft.LOGGER.info("Jujutsu Craft {} initialized (client)", JjkCraft.version());
	}

	private static void registerParticles() {
		ParticleProviderRegistry registry = ParticleProviderRegistry.getInstance();
		registry.register(JjkParticles.CURSED_SPARK, sprites -> new JjkParticle.Provider(sprites, JjkParticle.Motion.SPARK));
		registry.register(JjkParticles.CE_AURA, sprites -> new JjkParticle.Provider(sprites, JjkParticle.Motion.AURA));
		registry.register(JjkParticles.REVERSE_GLOW, sprites -> new JjkParticle.Provider(sprites, JjkParticle.Motion.GLOW));
		registry.register(JjkParticles.BLACK_LIGHTNING, sprites -> new JjkParticle.Provider(sprites, JjkParticle.Motion.LIGHTNING));
		registry.register(JjkParticles.DOMAIN_SHARD, sprites -> new JjkParticle.Provider(sprites, JjkParticle.Motion.SHARD));
		registry.register(JjkParticles.DISSOLVE, sprites -> new JjkParticle.Provider(sprites, JjkParticle.Motion.ASH));
	}
}
