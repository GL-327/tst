package com.jjkcraft.config;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;

import com.jjkcraft.core.energy.EnergyParams;

/** Access to the server config. Registered once from the common entrypoint. */
public final class JjkConfig {
	private static ConfigHolder<JjkServerConfig> holder;

	private JjkConfig() {
	}

	public static void init() {
		if (holder == null) {
			holder = AutoConfig.register(JjkServerConfig.class, JanksonConfigSerializer::new);
		}
	}

	public static JjkServerConfig server() {
		if (holder == null) {
			// Unit tests and early class loading: fall back to defaults instead of touching the disk.
			return DEFAULTS;
		}

		return holder.getConfig();
	}

	/** Re-reads the file from disk. Returns false if the file could not be parsed. */
	public static boolean reload() {
		return holder != null && holder.load();
	}

	public static EnergyParams energyParams() {
		JjkServerConfig.Energy e = server().energy;
		return new EnergyParams(
				e.regenPercentPerSecond / 100.0,
				e.idleRegenMultiplier,
				(int) Math.round(e.idleDelaySeconds * 20),
				e.overexertThresholdPercent / 100.0,
				(int) Math.round(e.overexertSeconds * 20));
	}

	private static final JjkServerConfig DEFAULTS = new JjkServerConfig();
}
