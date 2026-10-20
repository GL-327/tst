package com.jjkcraft.client.config;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;

/** Access to the client config. */
public final class ClientConfig {
	private static ConfigHolder<JjkClientConfig> holder;
	private static final JjkClientConfig DEFAULTS = new JjkClientConfig();

	private ClientConfig() {
	}

	public static void init() {
		if (holder == null) {
			holder = AutoConfig.register(JjkClientConfig.class, JanksonConfigSerializer::new);
		}
	}

	public static JjkClientConfig get() {
		return holder == null ? DEFAULTS : holder.getConfig();
	}

	public static void save() {
		if (holder != null) {
			holder.save();
		}
	}

	public static boolean reducedMotion() {
		return get().accessibility.reducedMotion;
	}

	public static boolean reduceFlashing() {
		return get().accessibility.reduceFlashing;
	}

	public static float uiScale() {
		return get().accessibility.uiScalePercent / 100f;
	}

	public static boolean lowVfx() {
		return get().vfx.level.ordinal() <= JjkClientConfig.VfxLevel.LOW.ordinal();
	}

	public static boolean effectsOff() { return get().vfx.level == JjkClientConfig.VfxLevel.OFF; }
	public static int effect(JjkClientConfig.VfxLevel level) { return Math.min(get().vfx.level.ordinal(), level == null ? 3 : level.ordinal()); }
}
