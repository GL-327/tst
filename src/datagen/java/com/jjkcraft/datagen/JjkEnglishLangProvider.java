package com.jjkcraft.datagen;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.concurrent.CompletableFuture;

import me.shedaniel.autoconfig.annotation.Config;

import net.minecraft.core.HolderLookup;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;

import com.jjkcraft.client.config.JjkClientConfig;
import com.jjkcraft.config.JjkServerConfig;

/**
 * English strings. Config option labels are derived from the config classes so a new option
 * cannot ship without a label; everything else is listed by hand, grouped by system.
 */
final class JjkEnglishLangProvider extends FabricLanguageProvider {
	JjkEnglishLangProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registryLookup) {
		super(output, "en_us", registryLookup);
	}

	@Override
	public void generateTranslations(HolderLookup.Provider registries, TranslationBuilder b) {
		b.add("mod.jjkcraft.name", "Jujutsu Craft");

		configLabels(b, JjkServerConfig.class, "Jujutsu Craft: Server Settings");
		configLabels(b, JjkClientConfig.class, "Jujutsu Craft: Client Settings");

		LangEntries.all(b);
		ModuloLang.add(b);
		ModuloDataLang.add(b);
	}

	private static void configLabels(TranslationBuilder b, Class<?> configClass, String title) {
		String name = configClass.getAnnotation(Config.class).name();
		String prefix = "text.autoconfig." + name;
		b.add(prefix + ".title", title);
		walk(b, prefix + ".option", configClass);
	}

	private static void walk(TranslationBuilder b, String prefix, Class<?> type) {
		for (Field field : type.getFields()) {
			if (Modifier.isStatic(field.getModifiers())) {
				continue;
			}

			String key = prefix + "." + field.getName();
			b.add(key, humanize(field.getName()));
			Class<?> fieldType = field.getType();
			if (!fieldType.isPrimitive() && !fieldType.isEnum() && fieldType != String.class && fieldType.getName().startsWith("com.jjkcraft")) {
				walk(b, key, fieldType);
			}
		}
	}

	/** "overexertThresholdPercent" -> "Overexert threshold (%)". */
	static String humanize(String camel) {
		boolean percent = camel.endsWith("Percent");
		String base = percent ? camel.substring(0, camel.length() - "Percent".length()) : camel;
		StringBuilder out = new StringBuilder();
		for (int i = 0; i < base.length(); i++) {
			char c = base.charAt(i);
			if (Character.isUpperCase(c) && i > 0) {
				out.append(' ').append(Character.toLowerCase(c));
			} else {
				out.append(i == 0 ? Character.toUpperCase(c) : c);
			}
		}

		String text = out.toString().replace("Hud", "HUD").replace("Vfx", "VFX").replace(" ui ", " UI ").replace("Ui ", "UI ");
		return percent ? text + " (%)" : text;
	}
}
