package com.jjkcraft.client.state;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import com.jjkcraft.client.ui.InkToast;
import com.jjkcraft.client.vfx.BlackFlashFx;
import com.jjkcraft.client.vfx.ClientFx;
import com.jjkcraft.network.payload.BlackFlashPayload;
import com.jjkcraft.network.payload.ReversePayload;
import com.jjkcraft.network.payload.VowPayloads;
import com.jjkcraft.vow.VowState;

/** The local player's M6 state: Black Flash zone, Reverse Cursed Technique and Binding Vow. */
public final class ClientSorcery {
	private static long zoneUntil;
	private static int zoneStacks;
	private static ReversePayload reverse = new ReversePayload(false, true, false, false);
	private static VowState vow = VowState.EMPTY;

	private ClientSorcery() {
	}

	public static void accept(BlackFlashPayload payload) {
		com.jjkcraft.client.vfx.AdvancedFx.blackFlash(payload.target());
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) {
			return;
		}

		boolean mine = payload.striker() == mc.player.getId();
		ClientFx.hitStop(2, payload.target());
		if (mine) {
			zoneUntil = payload.zoneUntil();
			zoneStacks = payload.zoneStacks();
			BlackFlashFx.trigger();
			ClientFx.shake(0.9f, 450);
			InkToast.show(Component.translatable("toast.jjkcraft.black_flash"),
					Component.translatable("toast.jjkcraft.black_flash.body", String.format("%.1f", payload.multiplier())), 0xFFE0303F);
		} else if (mc.level != null && mc.level.getEntity(payload.target()) != null
				&& mc.player.distanceTo(mc.level.getEntity(payload.target())) < 16) {
			ClientFx.shake(0.35f, 250);
		}
	}

	public static void accept(ReversePayload payload) {
		reverse = payload;
		if (payload.awakenedNow()) {
			InkToast.show(Component.translatable("toast.jjkcraft.reverse"), Component.translatable("toast.jjkcraft.reverse.body"), 0xFF5BE38C);
			ClientFx.shake(0.4f, 600);
		}
	}

	public static void accept(VowPayloads.Sync payload) {
		vow = payload.state();
	}

	public static boolean inZone() {
		Minecraft mc = Minecraft.getInstance();
		return mc.level != null && mc.level.getGameTime() < zoneUntil;
	}

	public static int zoneStacks() {
		return inZone() ? zoneStacks : 0;
	}

	public static ReversePayload reverse() {
		return reverse;
	}

	public static VowState vow() {
		return vow;
	}

	public static boolean inDebt() {
		Minecraft mc = Minecraft.getInstance();
		return mc.level != null && vow.inDebt(mc.level.getGameTime());
	}

	public static void reset() {
		zoneUntil = 0;
		zoneStacks = 0;
		reverse = new ReversePayload(false, true, false, false);
		vow = VowState.EMPTY;
	}
}
