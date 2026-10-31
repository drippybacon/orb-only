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
import java.util.LinkedHashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.SpriteID;
import net.runelite.api.SpritePixels;
import net.runelite.api.widgets.Widget;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.util.ImageUtil;

@Singleton
final class OrbFrameImages
{
	private final Client client;
	private final SpriteManager spriteManager;
	private final Map<String, CachedFrame> frames = new LinkedHashMap<String, CachedFrame>(32, 0.75f, true)
	{
		@Override
		protected boolean removeEldestEntry(Map.Entry<String, CachedFrame> eldest)
		{
			return size() > 96;
		}
	};
	private final Map<Integer, NativeBacking> backings = new HashMap<>();
	private long pass;

	@Inject
	OrbFrameImages(Client client, SpriteManager spriteManager)
	{
		this.client = client;
		this.spriteManager = spriteManager;
	}

	BufferedImage get(Widget widget, int fallbackSpriteId, int diameter, int innerDiameter)
	{
		if (diameter <= 0)
		{
			return null;
		}
		int spriteId = spriteId(widget, fallbackSpriteId);
		String key = (widget == null ? "global" : widget.getId() + ":" + widget.getIndex())
			+ ":" + spriteId + ":" + diameter + ":" + innerDiameter;
		Object source = source(widget, spriteId);
		if (source == null)
		{
			return null;
		}
		CachedFrame cached = frames.get(key);
		if (cached == null || cached.source != source)
		{
			cached = new CachedFrame(source, createRing(sourceImage(source), diameter, innerDiameter));
			frames.put(key, cached);
		}
		return cached.image;
	}

	private int spriteId(Widget widget, int fallback)
	{
		return widget != null && widget.getSpriteId() >= 0 ? widget.getSpriteId() : fallback;
	}

	private Object source(Widget widget, int spriteId)
	{
		SpritePixels override = widget == null ? null : client.getWidgetSpriteOverrides().get(widget.getId());
		NativeBacking state = widget == null ? null : backings.get(widget.getId());
		if (state != null && state.widget == widget && override == state.owned)
		{
			override = state.previous;
		}
		if (override == null)
		{
			override = client.getSpriteOverrides().get(spriteId);
		}
		return override != null ? override : spriteManager.getSprite(spriteId, 0);
	}

	private static BufferedImage sourceImage(Object source)
	{
		if (source instanceof BufferedImage)
		{
			return (BufferedImage) source;
		}
		SpritePixels sprite = (SpritePixels) source;
		BufferedImage image = sprite.toBufferedImage();
		if (sprite.getMaxWidth() <= 0 || sprite.getMaxHeight() <= 0
			|| (sprite.getMaxWidth() == image.getWidth() && sprite.getMaxHeight() == image.getHeight()
				&& sprite.getOffsetX() == 0 && sprite.getOffsetY() == 0))
		{
			return image;
		}
		BufferedImage canvas = new BufferedImage(sprite.getMaxWidth(), sprite.getMaxHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D draw = canvas.createGraphics();
		try
		{
			draw.drawImage(image, sprite.getOffsetX(), sprite.getOffsetY(), null);
		}
		finally
		{
			draw.dispose();
		}
		return canvas;
	}

	void beginPass()
	{
		pass++;
	}

	boolean prepareBackground(Widget widget, Rectangle circle)
	{
		Rectangle bounds = widget.getBounds();
		if (bounds.isEmpty())
		{
			return false;
		}
		NativeBacking state = backings.get(widget.getId());
		if (state != null && state.widget != widget)
		{
			restoreBacking(state);
			backings.remove(widget.getId());
			state = null;
		}
		SpritePixels current = client.getWidgetSpriteOverrides().get(widget.getId());
		if (state == null)
		{
			state = new NativeBacking(widget, current);
			backings.put(widget.getId(), state);
		}
		else if (current != state.owned)
		{
			// Track external replacements so cleanup restores their latest artwork.
			state.previous = current;
		}
		Object source = source(widget, spriteId(widget, SpriteID.MINIMAP_ORB_FRAME));
		if (source == null)
		{
			return false;
		}
		Rectangle placement = new Rectangle(circle.x - bounds.x, circle.y - bounds.y, circle.width, circle.height);
		if (state.source != source || !placement.equals(state.placement)
			|| state.width != bounds.width || state.height != bounds.height || state.owned == null)
		{
			BufferedImage original = sourceImage(source);
			BufferedImage circleImage = createRing(original, circle.width, 0);
			BufferedImage canvas = new BufferedImage(original.getWidth(), original.getHeight(), BufferedImage.TYPE_INT_ARGB);
			Graphics2D draw = canvas.createGraphics();
			try
			{
				int x = (int) Math.round(placement.x * original.getWidth() / (double) bounds.width);
				int y = (int) Math.round(placement.y * original.getHeight() / (double) bounds.height);
				int width = (int) Math.round(placement.width * original.getWidth() / (double) bounds.width);
				int height = (int) Math.round(placement.height * original.getHeight() / (double) bounds.height);
				draw.drawImage(circleImage, x, y, width, height, null);
			}
			finally
			{
				draw.dispose();
			}
			state.owned = ImageUtil.getImageSpritePixels(canvas, client);
			state.source = source;
			state.placement = placement;
			state.width = bounds.width;
			state.height = bounds.height;
		}
		state.lastPass = pass;
		if (client.getWidgetSpriteOverrides().get(widget.getId()) != state.owned)
		{
			client.getWidgetSpriteOverrides().put(widget.getId(), state.owned);
		}
		return true;
	}

	void finishPass()
	{
		backings.entrySet().removeIf(entry ->
		{
			if (entry.getValue().lastPass == pass)
			{
				return false;
			}
			restoreBacking(entry.getValue());
			return true;
		});
	}

	private void restoreBacking(NativeBacking state)
	{
		if (state.owned == null || client.getWidgetSpriteOverrides().get(state.widget.getId()) != state.owned)
		{
			return;
		}
		if (state.previous == null)
		{
			client.getWidgetSpriteOverrides().remove(state.widget.getId());
		}
		else
		{
			client.getWidgetSpriteOverrides().put(state.widget.getId(), state.previous);
		}
	}

	static BufferedImage createRing(BufferedImage source, int diameter, int innerDiameter)
	{
		int size = Math.min(source.getWidth(), source.getHeight());
		BufferedImage ring = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		double center = (size - 1) / 2.0;
		double holeRadius = (innerDiameter / 2.0 + 0.5) * size / diameter;
		for (int y = 0; y < size; y++)
		{
			for (int x = 0; x < size; x++)
			{
				double dx = x - center;
				double dy = y - center;
				if (innerDiameter > 0 && dx * dx + dy * dy <= holeRadius * holeRadius)
				{
					continue;
				}
				// Mirror the right half to close the gap left by the number panel.
				int sourceX = source.getWidth() - size + (x < size / 2 ? size - 1 - x : x);
				ring.setRGB(x, y, source.getRGB(sourceX, y));
			}
		}
		if (innerDiameter <= 0)
		{
			// Fill transparent gauge edges so the scene cannot show through.
			int centerColor = ring.getRGB(size / 2, size / 2);
			if ((centerColor >>> 24) == 0)
			{
				centerColor = 0xff101010;
			}
			BufferedImage background = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
			Graphics2D graphics = background.createGraphics();
			try
			{
				graphics.setColor(new Color(centerColor | 0xff000000, true));
				graphics.fillOval(1, 1, size - 2, size - 2);
				graphics.drawImage(ring, 0, 0, null);
			}
			finally
			{
				graphics.dispose();
			}
			return background;
		}
		return ring;
	}

	void clear()
	{
		backings.values().forEach(this::restoreBacking);
		backings.clear();
		frames.clear();
	}

	private static final class NativeBacking
	{
		private final Widget widget;
		private SpritePixels previous;
		private SpritePixels owned;
		private Object source;
		private Rectangle placement;
		private int width;
		private int height;
		private long lastPass;

		private NativeBacking(Widget widget, SpritePixels previous)
		{
			this.widget = widget;
			this.previous = previous;
		}
	}

	private static final class CachedFrame
	{
		private final Object source;
		private final BufferedImage image;

		private CachedFrame(Object source, BufferedImage image)
		{
			this.source = source;
			this.image = image;
		}
	}
}
