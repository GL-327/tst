package com.jjkcraft.client.state;

import com.jjkcraft.awakening.AwakeningRecord;
import com.jjkcraft.client.screen.AwakeningScreen;
import com.jjkcraft.network.payload.AwakeningPayloads;
import net.minecraft.client.Minecraft;

public final class ClientAwakening {
    private static AwakeningPayloads.View view = new AwakeningPayloads.View(AwakeningRecord.EMPTY, false, false);
    public static AwakeningPayloads.View view() { return view; }
    public static void accept(AwakeningPayloads.View next) { view = next; }
    public static void reset() { view = new AwakeningPayloads.View(AwakeningRecord.EMPTY, false, false); }
    public static void tick(Minecraft mc) {
        if (view.pending() && mc.player != null && mc.player.isAlive() && mc.gui.screen() == null)
            mc.gui.setScreen(new AwakeningScreen());
    }
    private ClientAwakening() { }
}
