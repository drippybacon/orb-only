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
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.SpriteID;
import net.runelite.api.SpritePixels;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.game.SpriteManager;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Mutable client/widget doubles with real production helpers. */
final class OrbTestContext
{
	final Client client = mock(Client.class);
	final SpriteManager sprites = mock(SpriteManager.class);
	final Map<Integer, Widget> widgets = new HashMap<>();
	final Map<Integer, SpritePixels> widgetSprites = new HashMap<>();
	final Map<Integer, SpritePixels> globalSprites = new HashMap<>();
	final OrbFrameImages frames = new OrbFrameImages(client, sprites);
	final OrbUiState ui = new OrbUiState(client, frames);

	OrbTestContext()
	{
		when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
		when(client.getWidget(anyInt())).thenAnswer(call -> widgets.get(call.getArgument(0)));
		when(client.getWidgetSpriteOverrides()).thenReturn(widgetSprites);
		when(client.getSpriteOverrides()).thenReturn(globalSprites);
		when(sprites.getSprite(anyInt(), anyInt())).thenReturn(frame(Color.DARK_GRAY));
		when(client.createSpritePixels(any(int[].class), anyInt(), anyInt())).thenAnswer(call ->
		{
			int width = call.getArgument(1);
			int height = call.getArgument(2);
			BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
			image.setRGB(0, 0, width, height, call.getArgument(0), 0, width);
			return sprite(image);
		});
	}

	OrbWidgets prayer(boolean nomap)
	{
		return nomap
			? add(InterfaceID.OrbsNomap.ORB_PRAYER, InterfaceID.OrbsNomap.PRAYER_ICON,
				InterfaceID.OrbsNomap.PRAYER_TEXT, InterfaceID.OrbsNomap.PRAYERBUTTON,
				InterfaceID.OrbsNomap.PRAYER_BACKING)
			: add(InterfaceID.Orbs.ORB_PRAYER, InterfaceID.Orbs.PRAYER_ICON,
				InterfaceID.Orbs.PRAYER_TEXT, InterfaceID.Orbs.PRAYERBUTTON,
				InterfaceID.Orbs.PRAYER_BACKING);
	}

	OrbWidgets special()
	{
		return add(InterfaceID.Orbs.ORB_SPECENERGY, InterfaceID.Orbs.SPECENERGY_ICON,
			InterfaceID.Orbs.SPECENERGY_TEXT, InterfaceID.Orbs.SPECBUTTON,
			InterfaceID.Orbs.SPECENERGY_BACKING);
	}

	private OrbWidgets add(int containerId, int iconId, int valueId, int buttonId, int backingId)
	{
		Widget container = widget(containerId, new Rectangle(100, 50, 57, 34));
		Widget icon = widget(iconId, new Rectangle(127, 54, 26, 26));
		Widget value = widget(valueId, new Rectangle(100, 58, 22, 16));
		Widget button = widget(buttonId, new Rectangle(100, 50, 57, 34));
		Widget backing = widget(backingId, new Rectangle(100, 50, 57, 34));
		for (Widget child : new Widget[]{icon, value, button, backing})
		{
			when(child.getParent()).thenReturn(container);
		}
		return new OrbWidgets(container, icon, value, button, backing);
	}

	Widget widget(int id, Rectangle bounds)
	{
		Widget widget = mock(Widget.class);
		AtomicBoolean hidden = new AtomicBoolean();
		AtomicBoolean blocking = new AtomicBoolean(true);
		when(widget.getId()).thenReturn(id);
		when(widget.getIndex()).thenReturn(-1);
		when(widget.getSpriteId()).thenReturn(SpriteID.MINIMAP_ORB_FRAME);
		when(widget.getBounds()).thenAnswer(call -> new Rectangle(bounds));
		when(widget.getWidth()).thenAnswer(call -> bounds.width);
		when(widget.getHeight()).thenAnswer(call -> bounds.height);
		when(widget.isSelfHidden()).thenAnswer(call -> hidden.get());
		when(widget.isHidden()).thenAnswer(call ->
			hidden.get() || (widget.getParent() != null && widget.getParent().isHidden()));
		doAnswer(call ->
		{
			hidden.set(call.getArgument(0));
			return null;
		}).when(widget).setHidden(anyBoolean());
		when(widget.getNoClickThrough()).thenAnswer(call -> blocking.get());
		doAnswer(call ->
		{
			blocking.set(call.getArgument(0));
			return null;
		}).when(widget).setNoClickThrough(anyBoolean());
		widgets.put(id, widget);
		return widget;
	}

	static BufferedImage frame(Color color)
	{
		BufferedImage image = new BufferedImage(57, 34, BufferedImage.TYPE_INT_ARGB);
		Graphics2D draw = image.createGraphics();
		try
		{
			draw.setColor(color);
			draw.fillRect(0, 8, 25, 18);
			draw.fillOval(23, 0, 34, 34);
		}
		finally
		{
			draw.dispose();
		}
		return image;
	}

	static SpritePixels sprite(BufferedImage image)
	{
		SpritePixels sprite = mock(SpritePixels.class);
		when(sprite.toBufferedImage()).thenReturn(image);
		when(sprite.getWidth()).thenReturn(image.getWidth());
		when(sprite.getHeight()).thenReturn(image.getHeight());
		when(sprite.getMaxWidth()).thenReturn(image.getWidth());
		when(sprite.getMaxHeight()).thenReturn(image.getHeight());
		return sprite;
	}

	static final class OrbWidgets
	{
		final Widget container;
		final Widget icon;
		final Widget value;
		final Widget button;
		final Widget backing;

		OrbWidgets(Widget container, Widget icon, Widget value, Widget button, Widget backing)
		{
			this.container = container;
			this.icon = icon;
			this.value = value;
			this.button = button;
			this.backing = backing;
		}
	}
}
