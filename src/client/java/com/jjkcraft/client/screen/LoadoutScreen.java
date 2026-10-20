package com.jjkcraft.client.screen;

import java.util.Comparator;

import java.util.List;

import java.util.Optional;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.gui.GuiGraphicsExtractor;

import net.minecraft.client.input.MouseButtonEvent;

import net.minecraft.client.renderer.RenderPipelines;

import net.minecraft.network.chat.Component;

import net.minecraft.resources.Identifier;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import com.jjkcraft.client.state.ClientProgress;

import com.jjkcraft.client.state.ClientTechniques;

import com.jjkcraft.client.ui.Ink;

import com.jjkcraft.client.ui.InkButton;

import com.jjkcraft.client.ui.Palette;

import com.jjkcraft.client.ui.TechniqueTooltip;

import com.jjkcraft.network.payload.SelectSlotPayload;

import com.jjkcraft.network.payload.SetLoadoutPayload;

import com.jjkcraft.technique.Techniques;

import com.jjkcraft.technique.TechniqueState;

/** Named catalogue with scrolling, descriptions, explicit eligibility and click/drag to equip. */
public final class LoadoutScreen extends JjkScreen {
    private Identifier selected, dragging;
    private boolean showAll;
    private int offset;
    private String search="";
    private double dragX, dragY;
    private double downX,downY;
    private boolean draggingActive;
    private int selectedSlot=ClientTechniques.state().selected();
    private InkButton equipButton;
    public LoadoutScreen() { super(Component.translatable("screen.jjkcraft.loadout")); }
    public LoadoutScreen(Identifier focus) { this();selected=focus;search=focus.getPath();showAll=true; }
    private int left() { return panelX() + 8; }
    private int right() { return panelX() + panelW() / 2 + 5; }
    private int listWidth() { return panelW() / 2 - 12; }
    private int listTop() { return contentY() + 43; }
    private int rows() { return Math.max(1, (panelH() - 108) / 28); }
    private int slotX(int s) { return right() + (s % 4) * 32; }
    private int slotY(int s) { return contentY() + 18 + (s / 4) * 29; }
    private List<Identifier> entries() {

        List<Identifier> ids = showAll ? ClientProgress.registries().map(r -> Techniques.all(r).stream().map(Techniques.Entry::id).toList()).orElse(List.of()) : ClientTechniques.known();
        return ids.stream().filter(id->search.isBlank()||id.getPath().contains(search.toLowerCase(java.util.Locale.ROOT))||ClientTechniques.definition(id).map(d->Component.translatable(d.name()).getString().toLowerCase(java.util.Locale.ROOT).contains(search.toLowerCase(java.util.Locale.ROOT))).orElse(false)).sorted(Comparator.<Identifier>comparingInt(id -> ClientTechniques.known().contains(id) ? 0 : 1)

            .thenComparing(id -> ClientTechniques.definition(id).map(d -> Component.translatable(d.name()).getString()).orElse(id.toString()))).toList();

    }

    @Override protected void initContent() {
        var query=addRenderableWidget(new net.minecraft.client.gui.components.EditBox(Ink.font(),left(),contentY()+21,listWidth(),16,Component.literal("Search techniques")));query.setMaxLength(40);query.setValue(search);query.setHint(Component.literal("Search techniques…"));query.setResponder(s->{search=s;offset=0;});
        addRenderableWidget(new InkButton(left()+listWidth()/2+2,contentY(),listWidth()/2-2,17,Component.literal("Families"),b->minecraft.gui.setScreen(new FamilyTechniqueScreen())));
        addRenderableWidget(new InkButton(left(), contentY(), listWidth()/2-2, 17, Component.literal(showAll ? "All moves" : "Known moves"), b -> { showAll = !showAll; offset = 0; rebuildWidgets(); }));
        if(minecraft.player!=null&&minecraft.player.isCreative())addRenderableWidget(new InkButton(right()+78,contentY(),listWidth()-78,16,Component.literal("Character editor"),b->minecraft.gui.setScreen(new CreativeTechniqueScreen())));
        addRenderableWidget(new InkButton(left(), panelY() + panelH() - 27, 35, 18, Component.literal("<"), b -> offset = Math.max(0, offset - rows())));
        addRenderableWidget(new InkButton(left() + listWidth() - 35, panelY() + panelH() - 27, 35, 18, Component.literal(">"), b -> offset = Math.min(Math.max(0, entries().size() - rows()), offset + rows())));

        equipButton = addRenderableWidget(new InkButton(right(), panelY() + panelH() - 27, panelW() / 2 - 16, 18,

            Component.literal("Equip in selected slot"), b -> equip(selectedSlot)));
        addRenderableWidget(new InkButton(right(),contentY()+121,(panelW()/2-20)/2,18,Component.literal("Personalize"),b->{

            Identifier id=selected!=null?selected:ClientTechniques.state().slot(selectedSlot).orElse(null);
            if(id!=null&&ClientTechniques.known().contains(id))minecraft.gui.setScreen(new TechniqueWorkshopScreen(id));

        }));
        addRenderableWidget(new InkButton(right()+(panelW()/2-20)/2+3,contentY()+121,(panelW()/2-20)/2,18,Component.literal("Bindings"),b->minecraft.gui.setScreen(new TechniqueBindingsScreen())));
        addRenderableWidget(new InkButton(right()+134,contentY()+20,Math.max(40,listWidth()-136),18,Component.literal("Clear slot"),b->{selected=null;ClientPlayNetworking.send(new SetLoadoutPayload(selectedSlot,Optional.empty()));}));

        var fusion=addRenderableWidget(new InkButton(right()+134,contentY()+86,Math.max(40,listWidth()-136),18,Component.literal("Fusion"),b->minecraft.gui.setScreen(new ShikigamiFusionScreen())));fusion.active=ClientTechniques.known().contains(com.jjkcraft.JjkCraft.id("chimera_fusion"));

    }
    private boolean canEquip() { return selected != null && ClientTechniques.known().contains(selected) && ClientTechniques.definition(selected).map(ClientTechniques::gradeAllows).orElse(false); }
    private void equip(int slot) {
        if (canEquip() && slot < Math.min(TechniqueState.SLOTS, ClientTechniques.usableSlots()))

            ClientPlayNetworking.send(new SetLoadoutPayload(slot, Optional.of(selected)));

    }

    @Override protected void extractContent(GuiGraphicsExtractor g, int mx, int my, float a) {
        var list = entries(); offset = Math.clamp(offset, 0, Math.max(0, list.size() - rows()));
        Ink.text(g, "12 SLOTS", right(), contentY() + 2, Palette.gold(), 1);
        Ink.text(g,Ink.fit(Component.literal("Select a move"),Math.max(1,listWidth()-134)),right()+134,contentY()+44,Palette.TEXT_DIM,1);
        Ink.text(g,"then a slot.",right()+134,contentY()+57,Palette.TEXT_DIM,1);
        Ink.text(g,Ink.fit(Component.literal("Right click clears."),Math.max(1,listWidth()-134)),right()+134,contentY()+75,Palette.TEXT_DIM,1);
        for (int n = offset; n < Math.min(list.size(), offset + rows()); n++) {

            Identifier id = list.get(n); var def = ClientTechniques.definition(id).orElse(null); if (def == null) continue;
            int y = listTop() + (n - offset) * 28;
            boolean known = ClientTechniques.known().contains(id), ready = known && ClientTechniques.gradeAllows(def);

            g.fill(left(), y, left() + listWidth(), y + 26, id.equals(selected) ? 0xFF33253E : 0xFF15121D);

            g.blit(RenderPipelines.GUI_TEXTURED, def.icon(), left() + 3, y + 3, 0, 0, 20, 20, 20, 20);
            Ink.text(g, Ink.fit(Component.translatable(def.name()), listWidth() - 30), left() + 28, y + 4, ready ? Palette.TEXT : Palette.TEXT_DIM, 1);
            String status = !known ? "Scroll or family progression" : !ready ? "Requires " + Component.translatable("grade." + def.grade().getNamespace() + "." + def.grade().getPath()).getString()

                : String.format(java.util.Locale.ROOT, "%.0f CE  |  %.1fs cooldown", ClientTechniques.cost(id), def.cast().cooldown() / 20.0);
            Ink.text(g, Ink.fit(Component.literal(status), listWidth() - 30), left() + 28, y + 15, ready ? Palette.energyHigh() : Palette.muted(), 1);
            if (mx >= left() && mx < left() + listWidth() && my >= y && my < y + 26) g.setComponentTooltipForNextFrame(Ink.font(), TechniqueTooltip.lines(id, def), mx, my);

        }
        Ink.centered(g, Component.literal(list.isEmpty() ? "No techniques" : (offset + 1) + "–" + Math.min(list.size(), offset + rows()) + " / " + list.size()), left() + listWidth() / 2, panelY() + panelH() - 22, Palette.TEXT_DIM, 1);
        for (int s = 0; s < TechniqueState.SLOTS; s++) {
            int x = slotX(s), y = slotY(s); boolean locked = s >= ClientTechniques.usableSlots();

            g.fill(x - 1, y - 1, x + 29, y + 28, s == selectedSlot ? Palette.gold() : Palette.INK_EDGE);

            g.fill(x, y, x + 28, y + 27, 0xFF141219);
            var id = ClientTechniques.state().slot(s);
            if (!locked && id.isPresent()) ClientTechniques.definition(id.get()).ifPresent(d -> g.blit(RenderPipelines.GUI_TEXTURED, d.icon(), x + 3, y + 2, 0, 0, 22, 22, 22, 22));
            Ink.text(g, locked ? "x" : String.valueOf(s + 1), x + 1, y + 17, locked ? Palette.muted() : Palette.gold(), 1);

        }

        Identifier shown=selected!=null?selected:ClientTechniques.state().slot(selectedSlot).orElse(null);
        if(shown!=null)ClientTechniques.definition(shown).ifPresent(d->{
            Ink.text(g,Ink.fit(Component.translatable(d.name()),panelW()/2-18),right(),contentY()+109,Palette.gold(),1);
            if(mx>=right()&&my>=contentY()+18&&my<contentY()+119)g.setComponentTooltipForNextFrame(Ink.font(),TechniqueTooltip.lines(shown,d),mx,my);
            int yy=contentY()+149,maxY=panelY()+panelH()-34;
            for(var line:Ink.font().split(Component.translatable(d.description()),listWidth()-5)){
                if(yy+9>maxY)break;g.text(Ink.font(),line,right(),yy,Palette.TEXT_DIM,true);yy+=11;
            }

        });
        else Ink.text(g,"Select a move, then a slot.",right(),contentY()+109,Palette.TEXT_DIM,1);

        equipButton.active = canEquip();
        if (dragging != null&&draggingActive) ClientTechniques.definition(dragging).ifPresent(d -> g.blit(RenderPipelines.GUI_TEXTURED, d.icon(), (int) dragX - 12, (int) dragY - 12, 0, 0, 24, 24, 24, 24));

    }

    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        var ids = entries();
        for (int n = offset; n < Math.min(ids.size(), offset + rows()); n++) {
            int y = listTop() + (n - offset) * 28;
            if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && event.x() >= left() && event.x() < left() + listWidth() && event.y() >= y && event.y() < y + 26) {

                selected = ids.get(n); dragging = selected; draggingActive=false;downX=dragX=event.x();downY=dragY=event.y(); return true;

            }

        }
        for (int s = 0; s < Math.min(TechniqueState.SLOTS, ClientTechniques.usableSlots()); s++) if (event.x() >= slotX(s) && event.x() < slotX(s) + 28 && event.y() >= slotY(s) && event.y() < slotY(s) + 27) {
            if (event.button() == InputConstants.MOUSE_BUTTON_RIGHT) {selected=null;selectedSlot=s;ClientPlayNetworking.send(new SelectSlotPayload(s));ClientPlayNetworking.send(new SetLoadoutPayload(s, Optional.empty()));}
            else if(event.button()==InputConstants.MOUSE_BUTTON_LEFT){ selectedSlot=s;ClientPlayNetworking.send(new SelectSlotPayload(s));if(canEquip())equip(s); } return true;

        }
        return super.mouseClicked(event, doubleClick);

    }

    @Override public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (dragging != null) { dragX = event.x(); dragY = event.y();draggingActive|=Math.hypot(dragX-downX,dragY-downY)>4; return true; } return super.mouseDragged(event, dx, dy);

    }

    @Override public boolean mouseReleased(MouseButtonEvent event) {
        if (dragging != null) {
            if(draggingActive)for (int s = 0; s < Math.min(TechniqueState.SLOTS, ClientTechniques.usableSlots()); s++)
                if (event.x() >= slotX(s) && event.x() < slotX(s) + 28 && event.y() >= slotY(s) && event.y() < slotY(s) + 27) equip(s);

            dragging = null;draggingActive=false; return true;

        } return super.mouseReleased(event);

    }

    @Override public boolean mouseScrolled(double x, double y, double sx, double sy) { offset = Math.clamp(offset - (int) Math.signum(sy), 0, Math.max(0, entries().size() - rows())); return true; }

}
