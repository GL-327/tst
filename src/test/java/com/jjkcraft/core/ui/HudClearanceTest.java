package com.jjkcraft.core.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.jjkcraft.core.ui.HudClearance.Box;

class HudClearanceTest {
	// The client test's default screen: 854x480 at GUI scale 2.
	private static final float HEIGHT = 240;

	@Test
	void aBoxClearOfEverythingStays() {
		Box tooltip = new Box(170, 1, 90, 40);
		assertEquals(1, HudClearance.clearTop(tooltip, List.of(new Box(0, 0, 60, 30)), HEIGHT));
		assertEquals(1, HudClearance.clearTop(tooltip, List.of(), HEIGHT));
	}

	@Test
	void aTopElementPushesTheBoxBelowIt() {
		// Jade's tooltip at the top centre over the domain meter (offset 34, 26 tall).
		Box tooltip = new Box(170, 1, 90, 46);
		Box meter = new Box(147, 34, 160, 26);
		assertEquals(60 + HudClearance.GAP, HudClearance.clearTop(tooltip, List.of(meter), HEIGHT));
	}

	@Test
	void aBottomElementPushesTheBoxAboveIt() {
		Box tooltip = new Box(170, 200, 90, 30);
		Box bar = new Box(150, 210, 140, 20);
		assertEquals(210 - 30 - HudClearance.GAP, HudClearance.clearTop(tooltip, List.of(bar), HEIGHT));
	}

	@Test
	void aBoxPushedIntoASecondElementMovesAgain() {
		Box tooltip = new Box(170, 1, 90, 20);
		Box meter = new Box(150, 10, 140, 20);
		Box panel = new Box(200, 34, 100, 30);
		assertEquals(64 + HudClearance.GAP, HudClearance.clearTop(tooltip, List.of(meter, panel), HEIGHT));
	}

	@Test
	void theBoxStaysOnScreen() {
		Box tooltip = new Box(0, 5, 90, 100);
		Box tall = new Box(0, 0, 90, 150);
		assertEquals(HEIGHT - 100, HudClearance.clearTop(tooltip, List.of(tall), HEIGHT));
	}
}
