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

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import net.runelite.api.SpriteID;
import net.runelite.api.SpritePixels;
import net.runelite.api.widgets.Widget;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class OrbFrameImagesTest
{
	private OrbTestContext context;
	private OrbTestContext.OrbWidgets prayer;

	@Before
	public void setUp()
	{
		context = new OrbTestContext();
		prayer = context.prayer(false);
	}

	private SpritePixels prepare()
	{
		context.frames.beginPass();
		assertTrue(context.frames.prepareBackground(prayer.backing, OrbType.circle(prayer.container, prayer.icon)));
		context.frames.finishPass();
		return context.widgetSprites.get(prayer.backing.getId());
	}

	@Test
	public void backingIsOpaqueInsideCircleAndTransparentOverNumberPanel()
	{
		BufferedImage image = prepare().toBufferedImage();
		assertEquals(57, image.getWidth());
		assertEquals(34, image.getHeight());
		assertEquals(0, image.getRGB(10, 17) >>> 24);
		assertEquals(255, image.getRGB(40, 17) >>> 24);
		BufferedImage foreground = context.frames.get(prayer.backing, SpriteID.MINIMAP_ORB_FRAME, 34, 26);
		assertEquals(0, foreground.getRGB(17, 17) >>> 24);
	}

	@Test
	public void fixedModeCoordinatesUseNativeBackingWithoutMovingOrHidingIt()
	{
		Rectangle bounds = new Rectangle(600, 20, 57, 34);
		Widget backing = context.widget(12345, bounds);
		context.frames.beginPass();
		assertTrue(context.frames.prepareBackground(backing, new Rectangle(623, 20, 34, 34)));
		context.frames.finishPass();
		assertNotNull(context.widgetSprites.get(12345));
		assertEquals(bounds, backing.getBounds());
		verify(backing, never()).setHidden(anyBoolean());
		verify(backing, never()).setOriginalWidth(anyInt());
		verify(backing, never()).setOriginalHeight(anyInt());
	}

	@Test
	public void restoresExistingResourcePackOverride()
	{
		SpritePixels previous = OrbTestContext.sprite(OrbTestContext.frame(Color.RED));
		context.widgetSprites.put(prayer.backing.getId(), previous);
		assertNotSame(previous, prepare());
		context.frames.clear();
		assertSame(previous, context.widgetSprites.get(prayer.backing.getId()));
	}

	@Test
	public void restoresNewestResourcePackOverrideAndDoesNotClobberLaterChanges()
	{
		prepare();
		SpritePixels replacement = OrbTestContext.sprite(OrbTestContext.frame(Color.BLUE));
		context.widgetSprites.put(prayer.backing.getId(), replacement);
		assertNotSame(replacement, prepare());
		context.frames.clear();
		assertSame(replacement, context.widgetSprites.get(prayer.backing.getId()));
		prepare();
		SpritePixels later = OrbTestContext.sprite(OrbTestContext.frame(Color.GREEN));
		context.widgetSprites.put(prayer.backing.getId(), later);
		context.frames.clear();
		assertSame(later, context.widgetSprites.get(prayer.backing.getId()));
	}

	@Test
	public void unchangedGeometryReusesSpriteAndRetiredBackingIsRemoved()
	{
		SpritePixels first = prepare();
		assertSame(first, prepare());
		context.frames.beginPass();
		context.frames.finishPass();
		assertFalse(context.widgetSprites.containsKey(prayer.backing.getId()));
	}

	@Test
	public void cacheDistinguishesWidgetSpecificArtwork()
	{
		context.widgetSprites.put(prayer.backing.getId(), OrbTestContext.sprite(OrbTestContext.frame(Color.RED)));
		context.widgetSprites.put(prayer.button.getId(), OrbTestContext.sprite(OrbTestContext.frame(Color.BLUE)));
		BufferedImage first = context.frames.get(prayer.backing, SpriteID.MINIMAP_ORB_FRAME, 34, 26);
		BufferedImage second = context.frames.get(prayer.button, SpriteID.MINIMAP_ORB_FRAME, 34, 26);
		assertNotSame(first, second);
		assertSame(first, context.frames.get(prayer.backing, SpriteID.MINIMAP_ORB_FRAME, 34, 26));
		assertSame(second, context.frames.get(prayer.button, SpriteID.MINIMAP_ORB_FRAME, 34, 26));
	}

	@Test
	public void cacheEvictsOldEntriesAndRefreshesChangedSource()
	{
		BufferedImage first = context.frames.get(prayer.backing, SpriteID.MINIMAP_ORB_FRAME, 34, 26);
		for (int diameter = 35; diameter < 132; diameter++)
		{
			context.frames.get(prayer.backing, SpriteID.MINIMAP_ORB_FRAME, diameter, 26);
		}
		BufferedImage reloaded = context.frames.get(prayer.backing, SpriteID.MINIMAP_ORB_FRAME, 34, 26);
		assertNotSame(first, reloaded);
		context.globalSprites.put(SpriteID.MINIMAP_ORB_FRAME, OrbTestContext.sprite(OrbTestContext.frame(Color.CYAN)));
		assertNotSame(reloaded, context.frames.get(prayer.backing, SpriteID.MINIMAP_ORB_FRAME, 34, 26));
	}
}
