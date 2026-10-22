package com.jjkcraft.client.render.curse;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import com.jjkcraft.client.render.ResourcePresence;
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
	/** Beyond this many blocks a curse uses its low-detail model, when its art provides one. */
	private static final int LOD_DISTANCE = 32;
	/** Per model: the LOD model id and the file that must exist for it. */
	private static final Map<Identifier, Identifier[]> LODS = new ConcurrentHashMap<>();
	/** Per texture: the wounded and critical variants. */
	private static final Map<Identifier, Identifier[]> DAMAGED = new ConcurrentHashMap<>();

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

		var player = net.minecraft.client.Minecraft.getInstance().player;
		if (player == null || curse.distanceToSqr(player) <= LOD_DISTANCE * LOD_DISTANCE) {
			return model.geo();
		}

		Identifier[] lod = LODS.computeIfAbsent(model.geo(), geo -> new Identifier[] {
				geo.withPath(geo.getPath() + "_lod"), geo.withPath("geckolib/models/" + geo.getPath() + "_lod.geo.json")});
		return ResourcePresence.exists(lod[1]) ? lod[0] : model.geo();
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

		float health = curse.getHealth() / curse.getMaxHealth();
		if (health >= 0.65f) {
			return model.texture();
		}

		Identifier[] damaged = DAMAGED.computeIfAbsent(model.texture(), texture -> new Identifier[] {
				texture.withPath(texture.getPath().replace(".png", "_wounded.png")),
				texture.withPath(texture.getPath().replace(".png", "_critical.png"))});
		Identifier variant = health < 0.3f ? damaged[1] : damaged[0];
		return ResourcePresence.exists(variant) ? variant : model.texture();
	}

	private static Identifier fallback(CurseEntity curse) {
		return BuiltInRegistries.ENTITY_TYPE.getKey(curse.getType()).withPrefix("entity/");
	}
}
