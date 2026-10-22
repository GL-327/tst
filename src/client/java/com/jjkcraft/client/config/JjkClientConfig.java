package com.jjkcraft.client.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment;

/** Client-only settings, stored in {@code config/jjkcraft-client.json5}. */
@Config(name = "jjkcraft-client")
public class JjkClientConfig implements ConfigData {
    @Comment("Assist attack choice and defensive reactions while holding attack. Manual hit/block inputs take priority.")
    public boolean autoCombat = true;
    public boolean creativeAutoCalamity = false;
    // Migration markers: internal, so they stay out of the config screen.
    @ConfigEntry.Gui.Excluded
    public int controlsRevision = 0;
    /** Bumped when the default HUD layout changes; elements still at an old default move to the new one. */
    @ConfigEntry.Gui.Excluded
    public int hudRevision = 0;
    @ConfigEntry.Gui.Excluded
    public boolean irisBindingsAdjusted = false;
	@ConfigEntry.Gui.CollapsibleObject
	public Hud hud = new Hud();

	@ConfigEntry.Gui.CollapsibleObject
	public Vfx vfx = new Vfx();

	@ConfigEntry.Gui.CollapsibleObject
	public Accessibility accessibility = new Accessibility();

	@ConfigEntry.Gui.CollapsibleObject
	public Audio audio = new Audio();

	public enum Anchor {
		TOP_LEFT, TOP_CENTER, TOP_RIGHT,
		CENTER_LEFT, CENTER, CENTER_RIGHT,
		BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT
	}

	public enum VfxLevel {
		OFF, LOW, MEDIUM, HIGH
	}

	/** Position, scale, opacity and visibility of one HUD element. */
	public static class HudElement {
		public boolean visible = true;
		@ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
		public Anchor anchor = Anchor.TOP_LEFT;
		public int offsetX = 0;
		public int offsetY = 0;
		@Comment("50 to 300 percent")
		@ConfigEntry.BoundedDiscrete(min = 50, max = 300)
		public int scalePercent = 100;
		@Comment("10 to 100 percent")
		@ConfigEntry.BoundedDiscrete(min = 10, max = 100)
		public int opacityPercent = 100;

		public HudElement() {
		}

		public HudElement(Anchor anchor, int offsetX, int offsetY) {
			this.anchor = anchor;
			this.offsetX = offsetX;
			this.offsetY = offsetY;
		}

		public float scale() {
			return scalePercent / 100f;
		}

		public float opacity() {
			return opacityPercent / 100f;
		}
	}

	public static class Hud {
		// The status column (CE, grade, combat, then event panels) stacks down the top-left corner;
		// panels lower in the column step below the ones above them when those are showing.
		@ConfigEntry.Gui.CollapsibleObject
		public HudElement energyBar = new HudElement(Anchor.TOP_LEFT, 4, 4);
		@ConfigEntry.Gui.CollapsibleObject
		public HudElement techniqueHotbar = new HudElement(Anchor.BOTTOM_RIGHT, -4, -4);
		@ConfigEntry.Gui.CollapsibleObject
		public HudElement combatPanel = new HudElement(Anchor.TOP_LEFT, 4, 46);
		@ConfigEntry.Gui.CollapsibleObject
		public HudElement chargeMeter = new HudElement(Anchor.CENTER, 0, 0);
        @ConfigEntry.Gui.CollapsibleObject
        public HudElement ritualTracker = new HudElement(Anchor.TOP_LEFT, 4, 46);
		@ConfigEntry.Gui.CollapsibleObject
		public HudElement gradeBadge = new HudElement(Anchor.TOP_LEFT, 4, 28);
		@ConfigEntry.Gui.CollapsibleObject
		public HudElement domainMeter = new HudElement(Anchor.TOP_CENTER, 0, 34);
		@ConfigEntry.Gui.CollapsibleObject
		public HudElement senseRadar = new HudElement(Anchor.CENTER_RIGHT, -4, 0);
		@ConfigEntry.Gui.CollapsibleObject
		public HudElement cullingPanel = new HudElement(Anchor.TOP_LEFT, 4, 46);
		@ConfigEntry.Gui.CollapsibleObject
		public HudElement mulMeter = new HudElement(Anchor.TOP_LEFT, 4, 46);
		@ConfigEntry.Gui.CollapsibleObject
		public HudElement missionTracker = new HudElement(Anchor.TOP_LEFT, 4, 46);
		@ConfigEntry.Gui.CollapsibleObject
		public HudElement resonance = new HudElement(Anchor.TOP_LEFT, 4, 46);
		@ConfigEntry.Gui.CollapsibleObject
		public HudElement dialogue = new HudElement(Anchor.BOTTOM_CENTER, 0, -62);
		@Comment("Show numbers (for example 87 / 100) on bars.")
		public boolean showNumbers = true;
	}

	public static class Vfx {
		public VfxLevel aura = VfxLevel.HIGH;
		public VfxLevel trails = VfxLevel.HIGH;
		public VfxLevel afterimages = VfxLevel.HIGH;
		public VfxLevel impacts = VfxLevel.HIGH;
		public VfxLevel domains = VfxLevel.HIGH;
		public VfxLevel healing = VfxLevel.HIGH;
		public VfxLevel environment = VfxLevel.MEDIUM;
		public int maxWorldQuads = 4000;
		public int maxEffectMillis = 3;
		public boolean logProfiling = false;
		@ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
		@Comment("LOW lowers particle counts, overlay rates and effect distance.")
		public VfxLevel level = VfxLevel.HIGH;
		@Comment("Turn off to disable every custom shader and post effect.")
		public boolean customShaders = true;
		public boolean cameraShake = true;
		@Comment("Brief visual freeze on heavy hits (client only).")
		public boolean hitStop = true;
		@ConfigEntry.BoundedDiscrete(min = 50, max = 2000)
		public int maxParticlesPerFrame = 600;
	}

	public static class Accessibility {
		@Comment("Removes shakes, slow motion and animated HUD motion.")
		public boolean reducedMotion = false;
		@Comment("Replaces flashes and strobes with steady tints.")
		public boolean reduceFlashing = false;
		@Comment("Uses a palette that stays distinct for common color vision deficiencies.")
		public boolean colorblindPalette = false;
		@Comment("Extra scale for every mod HUD element and screen, 75 to 200 percent.")
		@ConfigEntry.BoundedDiscrete(min = 75, max = 200)
		public int uiScalePercent = 100;
	}

	public static class Audio {
		@Comment("Max mod sounds playing at once; lower-priority sounds stop first when the mix is busy.")
		@ConfigEntry.BoundedDiscrete(min = 4, max = 48)
		public int maxConcurrentSounds = 16;
		@ConfigEntry.BoundedDiscrete(min = 0, max = 100)
		public int volumePercent = 100;
	}

	/**
	 * Revision 1 gathered the HUD into a top-left status column with the selected power beside the
	 * hotbar and the charge ring on the crosshair. Elements a player moved keep their position.
	 */
	private void migrateHud() {
		Hud fresh = new Hud();
		move(hud.energyBar, Anchor.TOP_LEFT, 6, 6, fresh.energyBar);
		move(hud.gradeBadge, Anchor.TOP_LEFT, 6, 30, fresh.gradeBadge);
		move(hud.techniqueHotbar, Anchor.CENTER_RIGHT, -6, 0, fresh.techniqueHotbar);
		move(hud.combatPanel, Anchor.BOTTOM_LEFT, 6, -6, fresh.combatPanel);
		move(hud.chargeMeter, Anchor.CENTER_RIGHT, -6, 39, fresh.chargeMeter);
		move(hud.ritualTracker, Anchor.TOP_LEFT, 6, 90, fresh.ritualTracker);
		move(hud.senseRadar, Anchor.BOTTOM_RIGHT, -6, -6, fresh.senseRadar);
		move(hud.cullingPanel, Anchor.TOP_LEFT, 6, 66, fresh.cullingPanel);
		move(hud.mulMeter, Anchor.TOP_LEFT, 6, 50, fresh.mulMeter);
		move(hud.missionTracker, Anchor.TOP_LEFT, 6, 66, fresh.missionTracker);
		hudRevision = 1;
	}

	private static void move(HudElement element, Anchor oldAnchor, int oldX, int oldY, HudElement target) {
		if (element != null && element.anchor == oldAnchor && element.offsetX == oldX && element.offsetY == oldY) {
			element.anchor = target.anchor;
			element.offsetX = target.offsetX;
			element.offsetY = target.offsetY;
		}
	}

	@Override
	public void validatePostLoad() {
		if (hud == null) {
			hud = new Hud();
		}

		if (vfx == null) {
			vfx = new Vfx();
		}

		if (accessibility == null) {
			accessibility = new Accessibility();
		}

		if (audio == null) {
			audio = new Audio();
		}

		if (hud.mulMeter == null) {
			hud.mulMeter = new HudElement(Anchor.TOP_LEFT, 4, 46);
		}

		if (hud.missionTracker == null) {
			hud.missionTracker = new HudElement(Anchor.TOP_LEFT, 4, 46);
		}

		if (hud.resonance == null) {
			hud.resonance = new HudElement(Anchor.TOP_LEFT, 4, 46);
		}

		if (hud.dialogue == null) {
			hud.dialogue = new HudElement(Anchor.BOTTOM_CENTER, 0, -62);
		}
		if(hud.combatPanel==null)hud.combatPanel=new HudElement(Anchor.TOP_LEFT,4,46);
		if(hud.chargeMeter==null)hud.chargeMeter=new HudElement(Anchor.CENTER,0,0);
		if(hud.ritualTracker==null)hud.ritualTracker=new HudElement(Anchor.TOP_LEFT,4,46);

		for (HudElement element : new HudElement[] {hud.energyBar, hud.techniqueHotbar, hud.gradeBadge, hud.domainMeter, hud.senseRadar, hud.cullingPanel,
				hud.mulMeter, hud.missionTracker, hud.dialogue, hud.combatPanel, hud.chargeMeter, hud.ritualTracker, hud.resonance}) {
			if (element != null) {
				element.scalePercent = Math.clamp(element.scalePercent, 50, 300);
				element.opacityPercent = Math.clamp(element.opacityPercent, 10, 100);
				if (element.anchor == null) {
					element.anchor = Anchor.TOP_LEFT;
				}
			}
		}

		if (hudRevision < 1) {
			migrateHud();
		}

		accessibility.uiScalePercent = Math.clamp(accessibility.uiScalePercent, 75, 200);
		vfx.maxParticlesPerFrame = Math.clamp(vfx.maxParticlesPerFrame, 50, 2000);
		audio.maxConcurrentSounds = Math.clamp(audio.maxConcurrentSounds, 4, 48);
		audio.volumePercent = Math.clamp(audio.volumePercent, 0, 100);
		if (vfx.level == null) {
			vfx.level = VfxLevel.HIGH;
		}
	}
}
