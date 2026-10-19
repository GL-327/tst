package com.jjkcraft.network;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

/**
 * Coalesces "this player's X changed" into at most one send per player per tick. Each system
 * creates a channel with the function that sends its state.
 */
public final class PlayerSyncQueue {
	private static final List<Channel> CHANNELS = new ArrayList<>();
	private static boolean registered;

	private PlayerSyncQueue() {
	}

	public static synchronized Channel channel(String name, Consumer<ServerPlayer> sender) {
		if (!registered) {
			ServerTickEvents.END_SERVER_TICK.register(PlayerSyncQueue::flush);
			registered = true;
		}

		Channel channel = new Channel(name, sender);
		CHANNELS.add(channel);
		return channel;
	}

	private static void flush(MinecraftServer server) {
		for (Channel channel : CHANNELS) {
			Set<UUID> dirty = channel.drain();
			if (dirty.isEmpty()) {
				continue;
			}

			for (UUID id : dirty) {
				ServerPlayer player = server.getPlayerList().getPlayer(id);
				if (player != null) {
					channel.sender.accept(player);
				}
			}
		}
	}

	public static final class Channel {
		private final String name;
		private final Consumer<ServerPlayer> sender;
		private final Set<UUID> dirty = new LinkedHashSet<>();

		private Channel(String name, Consumer<ServerPlayer> sender) {
			this.name = name;
			this.sender = sender;
		}

		public synchronized void markDirty(ServerPlayer player) {
			dirty.add(player.getUUID());
		}

		/** Sends now and clears any pending send. */
		public void sendNow(ServerPlayer player) {
			synchronized (this) {
				dirty.remove(player.getUUID());
			}

			sender.accept(player);
		}

		private synchronized Set<UUID> drain() {
			if (dirty.isEmpty()) {
				return Set.of();
			}

			Set<UUID> copy = new LinkedHashSet<>(dirty);
			dirty.clear();
			return copy;
		}

		@Override
		public String toString() {
			return "PlayerSyncQueue.Channel[" + name + "]";
		}
	}
}
