package com.jjkcraft.client.compat;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

import snownee.jade.api.Accessor;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.ui.Rect2f;

import com.jjkcraft.client.hud.HudLayout;
import com.jjkcraft.core.ui.HudClearance;

/**
 * Jade integration. Only loaded when Jade is installed (the {@code jade} entrypoint).
 *
 * <ul>
 *   <li>Jade's look-at tooltip moves clear of the mod's HUD elements, the way Jade already moves it
 *   below boss bars. By default both sit at the top centre: the tooltip covered the domain meter.</li>
 *   <li>Disguised curses show what they look like ({@link DisguiseView}). Jade showed the Redacted
 *   Saint's real health and "Jujutsu Craft" under the player's name it wears, and the Gilded Mimic as
 *   itself instead of a chest.</li>
 * </ul>
 */
public final class JjkJadePlugin implements IWailaPlugin {
	@Override
	public void registerClient(IWailaClientRegistration registration) {
		registration.addRayTraceCallback((hit, accessor, original) -> disguise(registration, accessor));
		registration.addBeforeRenderCallback((box, animation, graphics, accessor) -> {
			Rect2f rect = animation.rect;
			HudClearance.Box tooltip = new HudClearance.Box(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight());
			rect.setY(HudClearance.clearTop(tooltip, HudLayout.recentBoxes(), graphics.guiHeight()));
			return false;
		});
	}

	private static Accessor<?> disguise(IWailaClientRegistration registration, Accessor<?> accessor) {
		if (!(accessor instanceof EntityAccessor entityAccessor)) {
			return accessor;
		}

		EntityHitResult hit = entityAccessor.getHitResult();
		return switch (DisguiseView.of(entityAccessor.getEntity())) {
			case DisguiseView.Real real -> accessor;
			case DisguiseView.AsPlayer(var face) -> registration.entityAccessor()
					.from(entityAccessor)
					.entity(face)
					.hit(new EntityHitResult(face, hit.getLocation()))
					.build();
			// Client-side only: there is no block for the server to describe.
			case DisguiseView.AsBlock(var state) -> registration.blockAccessor()
					.level(accessor.getLevel())
					.player(accessor.getPlayer())
					.blockState(state)
					.hit(new BlockHitResult(hit.getLocation(), Direction.UP, hit.getEntity().blockPosition(), false))
					.serverConnected(false)
					.build();
			case DisguiseView.Nothing nothing -> null;
		};
	}
}
