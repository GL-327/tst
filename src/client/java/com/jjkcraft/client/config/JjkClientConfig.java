package com.jjkcraft.client.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment;

/** Client-only settings, stored in {@code config/jjkcraft-client.json5}. */
@Config(name = "jjkcraft-client")
public class JjkClientConfig implements ConfigData {
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
		LOW, MEDIUM, HIGH
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
		@ConfigEntry.Gui.CollapsibleObject
		public HudElement energyBar = new HudElement(Anchor.TOP_LEFT, 6, 6);
		@ConfigEntry.Gui.CollapsibleObject
		public HudElement techniqueHotbar = new HudElement(Anchor.CENTER_RIGHT, -6, 0);
		@ConfigEntry.Gui.CollapsibleObject
		public HudElement gradeBadge = new HudElement(Anchor.TOP_LEFT, 6, 30);
		@ConfigEntry.Gui.CollapsibleObject
		public HudElement domainMeter = new HudElement(Anchor.TOP_CENTER, 0, 34);
		@ConfigEntry.Gui.CollapsibleObject
		public HudElement senseRadar = new HudElement(Anchor.BOTTOM_RIGHT, -6, -6);
		@ConfigEntry.Gui.CollapsibleObject
		public HudElement cullingPanel = new HudElement(Anchor.TOP_LEFT, 6, 66);
		@ConfigEntry.Gui.CollapsibleObject
		public HudElement mulMeter = new HudElement(Anchor.TOP_LEFT, 6, 50);
		@ConfigEntry.Gui.CollapsibleObject
		public HudElement missionTracker = new HudElement(Anchor.TOP_LEFT, 6, 66);
		@ConfigEntry.Gui.CollapsibleObject
		public HudElement dialogue = new HudElement(Anchor.BOTTOM_CENTER, 0, -62);
		@Comment("Show numbers (for example 87 / 100) on bars.")
		public boolean showNumbers = true;
	}

	public static class Vfx {
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
			hud.mulMeter = new HudElement(Anchor.TOP_LEFT, 6, 50);
		}

		if (hud.missionTracker == null) {
			hud.missionTracker = new HudElement(Anchor.TOP_LEFT, 6, 66);
		}

		if (hud.dialogue == null) {
			hud.dialogue = new HudElement(Anchor.BOTTOM_CENTER, 0, -62);
		}

		for (HudElement element : new HudElement[] {hud.energyBar, hud.techniqueHotbar, hud.gradeBadge, hud.domainMeter, hud.senseRadar, hud.cullingPanel,
				hud.mulMeter, hud.missionTracker, hud.dialogue}) {
			if (element != null) {
				element.scalePercent = Math.clamp(element.scalePercent, 50, 300);
				element.opacityPercent = Math.clamp(element.opacityPercent, 10, 100);
				if (element.anchor == null) {
					element.anchor = Anchor.TOP_LEFT;
				}
			}
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
