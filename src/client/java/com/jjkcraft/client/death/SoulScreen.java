package com.jjkcraft.client.death;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import com.jjkcraft.client.ui.Ink;
import com.jjkcraft.client.ui.InkButton;
import com.jjkcraft.client.ui.Palette;
import com.jjkcraft.network.payload.SoulPayloads;

public final class SoulScreen extends Screen {
    private final boolean deathChoice;
    private String confirm = "";
    private int x, y, w, h;
    public SoulScreen(boolean death) { super(Component.literal(death ? "A soul at the threshold" : "Vessel · Pacts and Awakening")); deathChoice = death; }
    public boolean deathChoice() { return deathChoice; }
    @Override protected void init() {
        w = Math.min(420, width - 20); h = Math.min(236, height - 20); x = (width-w)/2; y = (height-h)/2;
        if (!confirm.isEmpty()) {
            addRenderableWidget(new InkButton(x+20, y+h-55, w-40, 22, Component.literal("Confirm"), b -> { ClientPlayNetworking.send(new SoulPayloads.Action(confirm)); if (!deathChoice) onClose(); }));
            addRenderableWidget(new InkButton(x+20, y+h-28, w-40, 20, Component.literal("Back"), b -> { confirm = ""; rebuildWidgets(); }));
        } else if (deathChoice) {
            int by=y+85;
            if(!SoulClient.view().hardcore()) { addRenderableWidget(new InkButton(x+20,by,w-40,24,Component.literal("Respawn at my bed / respawn point"), b->ClientPlayNetworking.send(new SoulPayloads.Action("respawn")))); by+=29; }
            addRenderableWidget(new InkButton(x+20,by,w-40,24,Component.literal("Be reborn as a Cursed Spirit"), b->{confirm="spirit";rebuildWidgets();})); by+=29;
            if(SoulClient.view().state().pact().equals("kenjaku")) { addRenderableWidget(new InkButton(x+20,by,w-40,24,Component.literal("Use my Kenjaku pact: colony reincarnation"), b->ClientPlayNetworking.send(new SoulPayloads.Action("reincarnate")))); by+=29; }
            if(!SoulClient.view().hardcore() || !SoulClient.view().state().pact().isEmpty()) addRenderableWidget(new InkButton(x+20,by,w-40,24,Component.literal("Become a Cursed Object"), b->{confirm="object";rebuildWidgets();}));
        } else {
            String[] actions = {"kenjaku", "sukuna", "vessel", "respec", "break"};
            String[] titles = {"Kenjaku Pact", "Sukuna Pact", "Train as a Vessel", "Awakening Rite", "Break my pact"};
            for (int n=0; n<actions.length; n++) { final String action = actions[n]; int bw=(w-48)/2; addRenderableWidget(new InkButton(x+20+(n%2)*(bw+8), y+48+(n/2)*42,bw,32,Component.literal(titles[n]), b -> { confirm = action; rebuildWidgets(); })); }
            addRenderableWidget(new InkButton(x+20, y+h-25, w-40, 18, Component.literal("Close"), b -> onClose()));
        }
    }
    private String[] terms() {
        var terms = SoulClient.view().terms();
        var registry = minecraft.level == null ? null : minecraft.level.registryAccess().lookupOrThrow(com.jjkcraft.death.SoulSystem.PACTS);
        var kenjaku = registry == null ? null : registry.getValue(com.jjkcraft.JjkCraft.id("kenjaku"));
        var sukuna = registry == null ? null : registry.getValue(com.jjkcraft.JjkCraft.id("sukuna"));
        String kenCost = kenjaku == null ? "Unavailable" : kenjaku.price() + " points OR " + kenjaku.offeringCount() + " " + new net.minecraft.world.item.ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(kenjaku.offering())).getHoverName().getString();
        String suCost = sukuna == null ? "Unavailable" : sukuna.offeringCount() + " " + new net.minecraft.world.item.ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(sukuna.offering())).getHoverName().getString();
        return switch(confirm) {
            case "spirit" -> new String[]{"Return as a cursed spirit, keeping this character's technique.","Your cursed body uses CE to regenerate and has its own form.","If this spirit is exorcised, the character is lost.","A new human character awakens with a different innate technique.","This applies in both Survival and Hardcore."};
            case "object" -> new String[]{"Your body leaves a tradable cursed object at your death location.", "A Vessel must consume it to revive you at 25% health.", "Revival causes 60 seconds of vessel sickness.", "Lava, destruction or " + terms.objectMinutes() + "-minute decay can lose this character.", "In Survival, the configured new-character rule then applies."};
            case "kenjaku" -> new String[]{"Contract: " + kenCost + ".", "Hardcore death becomes a cursed object instead of permanent death.", "Each revival also costs " + (kenjaku == null ? "?" : kenjaku.price()) + " points from YOUR colony ledger.", "Keep enough points before you fight; an unpaid soul cannot revive.", "Destroying the object or waiting too long still loses the run."};
            case "sukuna" -> new String[]{"Requires an approved active Binding Vow and " + suCost + ".", "Your first death splits your soul into " + (sukuna == null ? "?" : sukuna.fingers()) + " Special Grade objects.", "Each Vessel revival consumes one finger and increases Possession.", "Every " + terms.possessionThreshold() + " revivals, your body attacks nearby curses for five seconds.", "Losing every remaining finger leaves your next death permanent."};
            case "vessel" -> new String[]{"Vessel training costs " + terms.vesselCost() + " Culling Game points.", "Yuji's cursed object can pay for the training instead.", "Other players may hand you objects to revive their owners.", "A Vessel cannot consume their own soul while dead."};
            case "respec" -> new String[]{"The Awakening Rite costs " + terms.riteCost() + " Culling Game points.", "Your innate family, techniques, domains and vows are reset.", "Your grade, XP, trials and growth points are retained.", "Choose Inherit or Random again; there is no direct picker."};
            case "break" -> new String[]{"The Vessel releases you from your current pact.", "Your cursed energy is exhausted and Binding Vow penalties apply.", "Hardcore death is permanent after breaking the pact."};
            default -> new String[0];
        };
    }
    @Override public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float a) { g.fill(0,0,width,height,0xE008060D); }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float a) {
        Ink.panel(g,x,y,w,h,Palette.accent(),1); Ink.centered(g,title,width/2,y+15,Palette.gold(),1);
        if (confirm.isEmpty() && deathChoice) {
            Ink.centered(g,Component.literal("Your technique and grade can survive this death."),width/2,y+45,Palette.TEXT_DIM,1);
            Ink.centered(g,Ink.fit(Component.literal(SoulClient.view().hardcore()?"Choose a cursed-spirit life, or use a prior pact.":"Your bed, anchor, or world spawn is available. Inventory uses world rules."),w-30),width/2,y+63,Palette.TEXT_DIM,1);
        } else if (!confirm.isEmpty()) {
            int dy = y+44;
            for (String text : terms()) for (var line : Ink.font().split(Component.literal(text),w-40)) { g.text(Ink.font(),line,x+20,dy,Palette.TEXT_DIM,true); dy+=11; }
        }
        super.extractRenderState(g,mx,my,a);
    }
    @Override public boolean shouldCloseOnEsc() { return !deathChoice; }
    @Override public boolean isPauseScreen() { return false; }
}
