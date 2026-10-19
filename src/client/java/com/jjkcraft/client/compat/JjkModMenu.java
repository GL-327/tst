package com.jjkcraft.client.compat;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.autoconfig.AutoConfigClient;

import com.jjkcraft.client.config.JjkClientConfig;

/** Mod Menu integration. Only loaded when Mod Menu is installed. */
public final class JjkModMenu implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return parent -> AutoConfigClient.getConfigScreen(JjkClientConfig.class, parent).get();
	}
}
