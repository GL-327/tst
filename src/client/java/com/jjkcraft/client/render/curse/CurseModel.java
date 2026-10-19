package com.jjkcraft.client.render.curse;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import com.jjkcraft.curse.CurseDefinition;
import com.jjkcraft.curse.CurseEntity;

/**
 * One GeckoLib model for every curse. The model, texture and animation come from the curse's data
 * (synced to the client), so a datapack curse can reuse an entity type with new art. Without data
 * it falls back to assets named after the entity type.
 */
public final class CurseModel extends GeoModel<CurseEntity> {
	public static final DataTicket<Identifier> GEO = DataTicket.create("jjkcraft_curse_geo", Identifier.class);
	public static final DataTicket<Identifier> TEXTURE = DataTicket.create("jjkcraft_curse_texture", Identifier.class);
	private static final Identifier MISSING = Identifier.withDefaultNamespace("missingno");

	@Override
	public Identifier getModelResource(GeoRenderState renderState) {
		Identifier geo = renderState.getGeckolibData(GEO);
		return geo != null ? geo : MISSING;
	}

	@Override
	public Identifier getTextureResource(GeoRenderState renderState) {
		Identifier texture = renderState.getGeckolibData(TEXTURE);
		return texture != null ? texture : MISSING;
	}

	@Override
	public Identifier getAnimationResource(CurseEntity curse) {
		return curse.definition().map(d -> d.model().animation()).orElseGet(() -> fallback(curse));
	}

	@Override
	public void addAdditionalStateData(CurseEntity curse, @Nullable Object relatedObject, GeoRenderState renderState) {
		renderState.addGeckolibData(GEO, geo(curse));
		renderState.addGeckolibData(TEXTURE, texture(curse));
	}

	public static Identifier geo(CurseEntity curse) {
		CurseDefinition.Model model = curse.definition().map(CurseDefinition::model).orElse(null);
		if (model == null) {
			return fallback(curse);
		}

		if (curse.isDisguised() && model.disguise().isPresent()) {
			return model.disguise().get().geo();
		}

		return model.geo();
	}

	public static Identifier texture(CurseEntity curse) {
		CurseDefinition.Model model = curse.definition().map(CurseDefinition::model).orElse(null);
		if (model == null) {
			Identifier type = BuiltInRegistries.ENTITY_TYPE.getKey(curse.getType());
			return type.withPath("textures/entity/" + type.getPath() + ".png");
		}

		if (curse.isDisguised() && model.disguise().isPresent()) {
			// Wearing a player's face: the disguise model uses the player-skin layout, so their skin fits.
			if (curse.level().getEntity(curse.mechB()) instanceof AbstractClientPlayer player) {
				return player.getSkin().body().texturePath();
			}

			return model.disguise().get().texture();
		}

		return model.texture();
	}

	private static Identifier fallback(CurseEntity curse) {
		return BuiltInRegistries.ENTITY_TYPE.getKey(curse.getType()).withPrefix("entity/");
	}
}
