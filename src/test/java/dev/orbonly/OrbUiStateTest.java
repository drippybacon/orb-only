/*
 * Copyright (c) 2026, drippybacon
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package dev.orbonly;

import java.awt.Rectangle;
import net.runelite.api.GameState;
import net.runelite.api.SpriteID;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class OrbUiStateTest
{
	private OrbTestContext context;
	private OrbTestContext.OrbWidgets prayer;

	@Before
	public void setUp()
	{
		context = new OrbTestContext();
		prayer = context.prayer(false);
	}

	@Test
	public void restoresOriginalVisibilityAndBlockingWithoutHidingBacking()
	{
		prayer.backing.setNoClickThrough(false);
		context.ui.prepare();
		assertTrue(prayer.button.isSelfHidden());
		assertTrue(prayer.value.isSelfHidden());
		assertFalse(prayer.backing.isSelfHidden());
		assertFalse(prayer.container.getNoClickThrough());
		assertFalse(context.ui.nativeHidden(prayer.button));
		context.ui.restore();
		assertFalse(prayer.button.isSelfHidden());
		assertFalse(prayer.value.isSelfHidden());
		assertTrue(prayer.container.getNoClickThrough());
		assertTrue(prayer.button.getNoClickThrough());
		assertFalse(prayer.backing.getNoClickThrough());
		assertTrue(context.widgetSprites.isEmpty());
		assertTrue(context.ui.surfaces().isEmpty());
	}

	@Test
	public void preservesControlsHiddenByTheGame()
	{
		prayer.button.setHidden(true);
		context.ui.prepare();
		assertTrue(context.ui.nativeHidden(prayer.button));
		context.ui.restore();
		assertTrue(prayer.button.isSelfHidden());
	}

	@Test
	public void releasesFlagsForNativeScriptsAndRecordsTheirChanges()
	{
		context.ui.prepare();
		context.ui.releaseForNativeUpdates();
		assertFalse(prayer.button.isSelfHidden());
		assertTrue(prayer.container.getNoClickThrough());
		prayer.button.setHidden(true);
		context.ui.prepare();
		assertTrue(context.ui.nativeHidden(prayer.button));
		context.ui.restore();
		assertTrue(prayer.button.isSelfHidden());
	}

	@Test
	public void reusesSurfaceAndAvoidsRedundantFlagWrites()
	{
		context.ui.prepare();
		OrbUiState.Surface first = context.ui.surface(OrbType.PRAYER);
		clearInvocations(prayer.button, prayer.value, prayer.container);
		context.ui.prepare();
		assertSame(first, context.ui.surface(OrbType.PRAYER));
		verify(prayer.button, never()).setHidden(anyBoolean());
		verify(prayer.value, never()).setHidden(anyBoolean());
		verify(prayer.container, never()).setNoClickThrough(anyBoolean());
	}

	@Test
	public void switchesToNoMinimapAndRestoresRetiredWidgets()
	{
		context.ui.prepare();
		OrbTestContext.OrbWidgets nomap = context.prayer(true);
		prayer.container.setHidden(true);
		context.ui.prepare();
		assertSame(nomap.button, context.ui.surface(OrbType.PRAYER).button);
		assertFalse(prayer.button.isSelfHidden());
		assertTrue(nomap.button.isSelfHidden());
		assertFalse(context.widgetSprites.containsKey(prayer.backing.getId()));
	}

	@Test
	public void keepsReplacementLabelsWhenArtworkIsUnavailable()
	{
		context.ui.prepare();
		when(context.sprites.getSprite(SpriteID.MINIMAP_ORB_FRAME, 0)).thenReturn(null);
		context.ui.prepare();
		assertNotNull(context.ui.surface(OrbType.PRAYER));
		assertTrue(prayer.button.isSelfHidden());
		assertTrue(prayer.value.isSelfHidden());
		assertFalse(prayer.container.getNoClickThrough());
		assertFalse(prayer.button.getNoClickThrough());
		assertTrue(context.widgetSprites.isEmpty());
	}

	@Test
	public void logoutRestoresWidgetsAndArtwork()
	{
		context.ui.prepare();
		when(context.client.getGameState()).thenReturn(GameState.HOPPING);
		context.ui.prepare();
		assertFalse(prayer.button.isSelfHidden());
		assertTrue(prayer.container.getNoClickThrough());
		assertTrue(context.widgetSprites.isEmpty());
		assertNull(context.ui.surface(OrbType.PRAYER));
	}

	@Test
	public void circleFollowsIconAndRejectsNumberPanelAndCorners()
	{
		Rectangle circle = OrbType.circle(prayer.container, prayer.icon);
		assertEquals(new Rectangle(123, 50, 34, 34), circle);
		assertTrue(OrbUiState.contains(circle, 140, 67));
		assertFalse(OrbUiState.contains(circle, 110, 67));
		assertFalse(OrbUiState.contains(circle, 123, 50));
		prayer.icon.setHidden(true);
		assertNull(OrbType.circle(prayer.container, prayer.icon));
	}
}
