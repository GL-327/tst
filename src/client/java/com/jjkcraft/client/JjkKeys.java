package com.jjkcraft.client;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import com.jjkcraft.JjkCraft;
import com.jjkcraft.client.screen.ProfileScreen;
import com.jjkcraft.client.screen.TechniqueWheelScreen;
import com.jjkcraft.client.state.ClientTechniques;
import com.jjkcraft.network.payload.CastPayload;
import com.jjkcraft.network.payload.SelectSlotPayload;

/** Rebindable casting and combat keys, independent of vanilla inventory/hotbar defaults. */
public final class JjkKeys {
	public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(JjkCraft.id("main"));

	public static final KeyMapping PROFILE = key("profile", InputConstants.KEY_J);
	public static final KeyMapping CAST = key("cast", InputConstants.KEY_R);
	public static final KeyMapping NEXT_SLOT = key("next_slot", InputConstants.KEY_V);
	public static final KeyMapping WHEEL = key("wheel", InputConstants.KEY_G);
	public static final KeyMapping SENSE = key("sense", InputConstants.KEY_B);
	public static final KeyMapping DOMAIN = key("domain", InputConstants.KEY_K);
	public static final KeyMapping SIMPLE_DOMAIN = key("simple_domain", InputConstants.KEY_N);
	public static final KeyMapping REVERSE = key("reverse", InputConstants.KEY_H);
	public static final KeyMapping CULLING = key("culling", InputConstants.KEY_M);
	public static final KeyMapping COMBAT = key("combat", InputConstants.KEY_Y);
	public static final KeyMapping DASH = key("dash", InputConstants.KEY_LALT);
	public static final KeyMapping LOCK_ON = key("lock_on", InputConstants.KEY_Z);
	public static final KeyMapping AUTO_COMBAT = key("auto_combat",InputConstants.KEY_F7);
    public static final KeyMapping BANK = key("bank",InputConstants.KEY_BACKSLASH);
    public static final KeyMapping LIGHT = key("light_hit",InputConstants.KEY_U);
    public static final KeyMapping HEAVY = key("heavy_hit",InputConstants.KEY_I);
    public static final KeyMapping BLOCK = key("block",InputConstants.KEY_O);
	public static final KeyMapping[] SKILLS={key("skill_1",InputConstants.KEY_NUMPAD1),key("skill_2",InputConstants.KEY_NUMPAD2),key("skill_3",InputConstants.KEY_NUMPAD3),key("skill_4",InputConstants.KEY_NUMPAD4)};
	private static final boolean[] skillDown=new boolean[4];
	private static int heldCastSlot=-1;
    private static final int[] heldSkillSlots={-1,-1,-1,-1};
    public static int bankStart(){return (ClientTechniques.state().selected()/4)*4;}

	/** Extra per-tick key handlers added by later systems (sense, domains). */
	private static final List<Consumer<Minecraft>> HANDLERS = new ArrayList<>();
	private static boolean castWasDown;

	private JjkKeys() {
	}

	public static void addHandler(Consumer<Minecraft> handler) {
		HANDLERS.add(handler);
	}

	public static void init() {
		ClientTickEvents.END_CLIENT_TICK.register(JjkKeys::tick);
		net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register((h,m)->{castWasDown=false;heldCastSlot=-1;java.util.Arrays.fill(skillDown,false);});
	}

	public static boolean clicked(KeyMapping key){boolean any=false;while(key.consumeClick())any=true;return any;}
    private static void tick(Minecraft mc) {
		boolean inGame = mc.player != null && mc.gui.screen() == null;

		while (PROFILE.consumeClick()) {
			if (inGame) {
				mc.gui.setScreen(new ProfileScreen());
			}
		}

		// Keep real key edges, including quick taps, so the server owns charge duration and release.
		boolean castDown = CAST.isDown() && inGame; boolean tapped=clicked(CAST);
        if(tapped&&!castDown&&!castWasDown&&inGame){int slot=ClientTechniques.state().selected();ClientPlayNetworking.send(new CastPayload(slot,true));ClientPlayNetworking.send(new CastPayload(slot,false));}
		if (castDown != castWasDown && mc.player != null) {
			if(castDown)heldCastSlot=ClientTechniques.state().selected();
			ClientPlayNetworking.send(new CastPayload(heldCastSlot, castDown,!inGame));
		}

		castWasDown = castDown;
		while(BANK.consumeClick())if(inGame)ClientPlayNetworking.send(new SelectSlotPayload((bankStart()+4)%com.jjkcraft.technique.TechniqueState.SLOTS));
        for(int slot=0;slot<4;slot++){
			boolean down=inGame&&com.jjkcraft.client.combat.CombatClient.enabled()&&SKILLS[slot].isDown();
            boolean tap=clicked(SKILLS[slot]);
            if(tap&&!down&&!skillDown[slot]&&inGame&&com.jjkcraft.client.combat.CombatClient.enabled()){int target=bankStart()+slot;ClientPlayNetworking.send(new SelectSlotPayload(target));ClientPlayNetworking.send(new CastPayload(target,true));ClientPlayNetworking.send(new CastPayload(target,false));}
			if(down!=skillDown[slot]&&mc.player!=null){if(down){heldSkillSlots[slot]=bankStart()+slot;ClientPlayNetworking.send(new SelectSlotPayload(heldSkillSlots[slot]));}ClientPlayNetworking.send(new CastPayload(heldSkillSlots[slot],down,!inGame));}
			skillDown[slot]=down;
		}
		while (CAST.consumeClick()) {
			// Presses are handled through isDown above; drain the queue.
		}

		while (NEXT_SLOT.consumeClick()) {
			if (inGame) {
				int slots = Math.max(1, ClientTechniques.usableSlots());
				ClientPlayNetworking.send(new SelectSlotPayload((ClientTechniques.state().selected() + 1) % slots));
			}
		}

		while (WHEEL.consumeClick()) {
			if (inGame) {
				mc.gui.setScreen(new TechniqueWheelScreen());
			}
		}

		for (Consumer<Minecraft> handler : HANDLERS) {
			handler.accept(mc);
		}
	}

	private static KeyMapping key(String name, int defaultKey) {
		return KeyMappingHelper.registerKeyMapping(new KeyMapping("key.jjkcraft." + name, InputConstants.Type.KEYBOARD, defaultKey, CATEGORY));
	}
}
