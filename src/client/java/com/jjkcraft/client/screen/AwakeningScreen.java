package com.jjkcraft.client.screen;

import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import com.jjkcraft.awakening.CharacterAwakening;
import com.jjkcraft.client.state.ClientAwakening;
import com.jjkcraft.client.ui.Ink;
import com.jjkcraft.client.ui.InkButton;
import com.jjkcraft.client.ui.Palette;
import com.jjkcraft.network.payload.AwakeningPayloads;

/** Two deliberate paths, a server-configurable picker, and an acknowledged reveal. */
public final class AwakeningScreen extends Screen {
    private boolean picker;
    private int page;
    private int x, y, w, h;
    private boolean wasPending;
    public AwakeningScreen() { super(Component.literal("Awakening")); }
    @Override protected void init() {
        w = Math.min(400, width - 20); h = Math.min(230, height - 20);
        x = (width - w) / 2; y = (height - h) / 2;
        wasPending = ClientAwakening.view().pending();
        if(minecraft.player!=null&&minecraft.player.isCreative())addRenderableWidget(new InkButton(x+20,y+h-24,w-40,20,Component.literal("Creative: choose at Calamity grade"),b->minecraft.gui.setScreen(new CreativeTechniqueScreen())));
        if (!wasPending) {
            addRenderableWidget(new InkButton(x + 20, y + h - 28, w - 40, 20, Component.literal("Continue"), b -> onClose()));
        } else if (!picker) {
            addRenderableWidget(new InkButton(x + 20, y + 95, w - 40, 26, Component.literal("Inherit a clan technique"), b -> choose("inherit", "")));
            addRenderableWidget(new InkButton(x + 20, y + 128, w - 40, 26, Component.literal("Roll a random innate technique"), b -> choose("random", "")));
            if (ClientAwakening.view().allowChoice())
                addRenderableWidget(new InkButton(x + 20, y + 161, w - 40, 24, Component.literal("Choose my technique"), b -> { picker = true; rebuildWidgets(); }));
        } else if (minecraft.level != null) {
            var registry = minecraft.level.registryAccess().lookupOrThrow(CharacterAwakening.FAMILIES);
            List<Identifier> ids = registry.keySet().stream().sorted().toList();
            int rows = Math.max(1, (h - 92) / 25);
            page = Math.clamp(page, 0, Math.max(0, (ids.size() - 1) / rows));
            for (int n = page * rows; n < Math.min(ids.size(), (page + 1) * rows); n++) {
                Identifier id = ids.get(n);
                addRenderableWidget(new InkButton(x + 20, y + 46 + (n % rows) * 25, w - 40, 22,
                    Component.translatable(registry.getValue(id).name()), b -> choose("choose", id.toString())));
            }
            addRenderableWidget(new InkButton(x + 20, y + h - 28, 70, 20, Component.literal("Back"), b -> { picker = false; rebuildWidgets(); }));
            addRenderableWidget(new InkButton(x + w - 166, y + h - 28, 65, 20, Component.literal("Previous"), b -> { page--; rebuildWidgets(); }));
            addRenderableWidget(new InkButton(x + w - 90, y + h - 28, 70, 20, Component.literal("Next"), b -> { page++; rebuildWidgets(); }));
        }
    }
    private void choose(String path, String family) { ClientPlayNetworking.send(new AwakeningPayloads.Choose(path, family)); }
    @Override public void tick() { if (wasPending != ClientAwakening.view().pending()) rebuildWidgets(); }
    @Override public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float a) { g.fill(0, 0, width, height, 0xDC08060F); }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float a) {
        Ink.panel(g, x, y, w, h, Palette.accent(), 1);
        Ink.centered(g, title, width / 2, y + 14, Palette.gold(), 1);
        if (ClientAwakening.view().pending() && !picker) {
            line(g, "Your cursed energy is stirring.", y + 40);
            line(g, "Inherit: a weighted clan and its techniques.", y + 59);
            line(g, "Random: a technique outside the clan pools.", y + 74);
            line(g, "One choice per character. Progress unlocks more moves.", y + h - 16);
        } else if (!ClientAwakening.view().pending()) {
            var record = ClientAwakening.view().record();
            String family = record.family();
            Component name = Component.literal(record.path().equals("heavenly") ? "Heavenly Restriction" : "Awakened sorcerer");
            if (minecraft.level != null && Identifier.tryParse(family) != null) {
                var def = minecraft.level.registryAccess().lookupOrThrow(CharacterAwakening.FAMILIES).getValue(Identifier.parse(family));
                if (def != null) name = Component.translatable(def.name());
            }
            Ink.centered(g, Ink.fit(name, w - 30), width / 2, y + 65, Palette.energyHigh(), 1);
            line(g, record.clan().isEmpty() ? "An innate technique of your own." : record.clan() + " clan", y + 88);
            if (record.sixEyes()) line(g, "Six Eyes — reduced cursed energy costs", y + 108);
            line(g, "J: profile and loadout   R: cast   V: next slot", y + h - 60);
            line(g, "G: technique wheel   Explore, fight, and grow.", y + h - 46);
        }
        super.extractRenderState(g, mx, my, a);
    }
    private void line(GuiGraphicsExtractor g, String text, int atY) { Ink.centered(g, Ink.fit(Component.literal(text), w - 24), width / 2, atY, Palette.TEXT_DIM, 1); }
    @Override public boolean shouldCloseOnEsc() { return !ClientAwakening.view().pending(); }
    @Override public boolean isPauseScreen() { return false; }
}
