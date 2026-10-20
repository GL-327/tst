package com.jjkcraft.modulo;

import java.util.List;
import java.util.Optional;

import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import com.jjkcraft.config.JjkConfig;
import com.jjkcraft.modulo.data.MissionDefinition;
import com.jjkcraft.modulo.data.MissionStage;
import com.jjkcraft.modulo.data.ModuloRegistries;
import com.jjkcraft.modulo.data.SiteDefinition;
import com.jjkcraft.modulo.entity.ModuloEntities;
import com.jjkcraft.modulo.mission.Missions;
import com.jjkcraft.modulo.world.ModuloFeatures;
import com.jjkcraft.network.JjkNetworking;
import com.jjkcraft.network.PlayerSyncQueue;

/**
 * The Modulo expansion (docs/MODULO.md). Everything it adds is registered here; everything it does
 * is gated by {@code modulo.enabled}.
 */
public final class Modulo {
	public static final String NAMESPACE = "modulo";
	public static final ResourceKey<Level> NAUNAX = ResourceKey.create(Registries.DIMENSION, Identifier.fromNamespaceAndPath(NAMESPACE, "naunax"));
	public static final ResourceKey<Level> TOKYO = ResourceKey.create(Registries.DIMENSION, Identifier.fromNamespaceAndPath(NAMESPACE, "tokyo_2086"));
	/** Standing height in both Modulo dimensions (their flat ground's top is one below). */
	public static final int GROUND_Y = 64;
	/** West of this x, Tokyo 2086 is the Kyoto region. */
	public static final int KYOTO_BOUNDARY_X = 0;

	public static final AttachmentType<ModuloProgress> PROGRESS = AttachmentRegistry.create(com.jjkcraft.JjkCraft.id("modulo"),
			builder -> builder.persistent(ModuloProgress.CODEC).copyOnDeath().initializer(() -> ModuloProgress.EMPTY));

	private static final PlayerSyncQueue.Channel SYNC = PlayerSyncQueue.channel("modulo", Modulo::sendView);

	private Modulo() {
	}

	public static void init() {
		ModuloRegistries.init();
		ModuloBlocks.init();
		ModuloItems.init();
		ModuloEntities.init();
		ModuloFeatures.init();
		MulEnergy.init();

		PayloadTypeRegistry.clientboundPlay().register(ModuloPayloads.Sync.TYPE, ModuloPayloads.Sync.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(ModuloPayloads.Dialogue.TYPE, ModuloPayloads.Dialogue.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(ModuloPayloads.Event.TYPE, ModuloPayloads.Event.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(ModuloPayloads.Choose.TYPE, ModuloPayloads.Choose.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(ModuloPayloads.Choose.TYPE, (payload, context) -> Missions.onChoose(context.player(), payload.option()));

		ModuloTravel.init();
		ModuloTechniques.init();
		ModuloCommands.init();
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			var p=handler.getPlayer();if(isModuloLevel(p.level()))ModuloTravel.leave(p);
			if(!progress(p).equals(ModuloProgress.EMPTY))setProgress(p,ModuloProgress.EMPTY);
			SYNC.markDirty(p);
		});
	}

	/** Modulo is a collection of sandbox abilities. Legacy story data is retained only for save compatibility. */
	public static boolean storyEnabled(){return false;}

	public static boolean enabled() {
		return JjkConfig.server().modulo.enabled;
	}

	public static boolean isModuloLevel(Level level) {
		return level.dimension() == NAUNAX || level.dimension() == TOKYO;
	}

	public static ModuloState state(MinecraftServer server) {
		return server.getDataStorage().computeIfAbsent(ModuloState.TYPE);
	}

	public static ModuloProgress progress(Player player) {
		return player.getAttachedOrElse(PROGRESS, ModuloProgress.EMPTY);
	}

	public static void setProgress(ServerPlayer player, ModuloProgress progress) {
		player.setAttached(PROGRESS, progress);
		SYNC.markDirty(player);
	}

	public static void markDirty(ServerPlayer player) {
		SYNC.markDirty(player);
	}

	public static void send(ServerPlayer player, net.minecraft.network.protocol.common.custom.CustomPacketPayload payload) {
		JjkNetworking.send(player, payload);
	}

	/** Builds and sends the player's view: Mul energy, the current objective, the Descent. */
	public static void sendView(ServerPlayer player) {
		JjkNetworking.send(player, new ModuloPayloads.Sync(view(player)));
	}

	public static ModuloPayloads.View view(ServerPlayer player) {
		if(!storyEnabled())return new ModuloPayloads.View(enabled(),MulEnergy.get(player),MulEnergy.max(),"",List.of(0,0),"",List.of(),Optional.empty(),"",Optional.empty(),0,false,false);
		if (!enabled()) {
			return ModuloPayloads.View.EMPTY;
		}

		ModuloProgress progress = progress(player);
		String title = "";
		String objective = "";
		List<String> args = List.of();
		Optional<GlobalPos> target = Optional.empty();
		String siteName = "";
		int order = 0;
		int total = ModuloRegistries.missions(player.registryAccess()).size();
		Optional<Missions.Current> current = Missions.current(player);
		if (current.isPresent()) {
			MissionDefinition mission = current.get().definition();
			MissionStage stage = current.get().stage();
			title = mission.title();
			objective = stage.base().objective();
			args = Missions.objectiveArgs(player, progress, stage);
			order = mission.order();
			Optional<SiteDefinition> site = stage.base().site().flatMap(id -> ModuloRegistries.site(player.registryAccess(), id));
			if (site.isPresent()) {
				siteName = site.get().name();
				target = Optional.of(GlobalPos.of(site.get().dimension(),
						stage instanceof MissionStage.Travel travel ? travel.marker().map(site.get()::marker).orElse(site.get().center()) : site.get().center()));
			}
		} else if (Missions.finishedAll(player)) {
			objective = "objective.jjkcraft.modulo.done";
			order = total;
		} else if (!progress.boarded()) {
			objective = NaunaxDescent.eligible(player) ? "objective.jjkcraft.modulo.wait_descent" : "objective.jjkcraft.modulo.reach_grade";
		}

		ModuloState state = state(player.level().getServer());
		Optional<net.minecraft.core.BlockPos> descent = NaunaxDescent.active(player.level().getServer()) && NaunaxDescent.eligible(player)
				? state.descentPos : Optional.empty();
		return new ModuloPayloads.View(true, MulEnergy.get(player), MulEnergy.max(), title, List.of(order, total), objective, args, target, siteName,
				descent, state.descentStart + state.descentLength, progress.companion(), isModuloLevel(player.level()));
	}

	/** The server level of a Modulo dimension, if it is loaded. */
	public static Optional<ServerLevel> level(MinecraftServer server, ResourceKey<Level> key) {
		return Optional.ofNullable(server.getLevel(key));
	}
}
