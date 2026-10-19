package com.jjkcraft.client.compat;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import com.jjkcraft.client.state.ClientSensing;
import com.jjkcraft.curse.CurseEntity;
import com.jjkcraft.curse.mechanic.CurseMechanic;
import com.jjkcraft.curse.mechanic.MimicMechanic;
import com.jjkcraft.curse.mechanic.SaintMechanic;

/**
 * What a look-at overlay from another mod (Jade) may show for an entity, so a curse's disguise holds
 * there too: the Redacted Saint is the player whose face it wears, the Gilded Mimic is a chest.
 * Sensing sees through a disguise in the world (the outline), so a sensed curse shows as itself.
 */
public final class DisguiseView {
	public sealed interface Shown permits Real, AsPlayer, AsBlock, Nothing {
	}

	/** Show the entity as it is. */
	public record Real() implements Shown {
	}

	/** Show this player instead. */
	public record AsPlayer(Player player) implements Shown {
	}

	/** Show this block instead. */
	public record AsBlock(BlockState state) implements Shown {
	}

	/** Show nothing (a disguise with no vanilla counterpart, such as the faceless pilgrim). */
	public record Nothing() implements Shown {
	}

	private static final Shown REAL = new Real();
	private static final Shown NOTHING = new Nothing();

	private DisguiseView() {
	}

	public static Shown of(Entity entity) {
		if (!(entity instanceof CurseEntity curse) || !curse.isDisguised() || ClientSensing.outlineColor(curse) != 0) {
			return REAL;
		}

		CurseMechanic mechanic = curse.mechanic();
		if (mechanic instanceof SaintMechanic) {
			return curse.level().getEntity(curse.mechB()) instanceof Player face ? new AsPlayer(face) : NOTHING;
		}

		if (mechanic instanceof MimicMechanic) {
			return new AsBlock(Blocks.CHEST.defaultBlockState());
		}

		return NOTHING;
	}
}
