package dev.orbonly;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.SpritePixels;
import net.runelite.api.widgets.Widget;
import net.runelite.client.game.SpriteManager;

@Singleton
final class OrbFrameImages
{
	private final Client client;
	private final SpriteManager spriteManager;
	private final Map<String, CachedFrame> frames = new HashMap<>();

	@Inject
	private OrbFrameImages(Client client, SpriteManager spriteManager)
	{
		this.client = client;
		this.spriteManager = spriteManager;
	}

	BufferedImage get(Widget widget, int fallbackSpriteId, int diameter, int innerDiameter)
	{
		int spriteId = widget != null && widget.getSpriteId() >= 0 ? widget.getSpriteId() : fallbackSpriteId;
		SpritePixels override = widget == null ? null : client.getWidgetSpriteOverrides().get(widget.getId());
		if (override == null)
		{
			override = client.getSpriteOverrides().get(spriteId);
		}
		String key = spriteId + ":" + diameter + ":" + innerDiameter;
		CachedFrame cached = frames.get(key);
		if (override != null && cached != null && cached.source == override)
		{
			return cached.image;
		}
		BufferedImage source = override == null ? spriteManager.getSprite(spriteId, 0) : override.toBufferedImage();
		if (source == null || diameter <= 0)
		{
			return null;
		}
		Object identity = override != null ? override : source;
		if (cached == null || cached.source != identity)
		{
			cached = new CachedFrame(identity, createRing(source, diameter, innerDiameter));
			frames.put(key, cached);
		}
		return cached.image;
	}

	BufferedImage getBackground(Widget widget, int fallbackSpriteId, int diameter)
	{
		return get(widget, fallbackSpriteId, diameter, 0);
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
				// Complete the curve where the native number panel joins the frame.
				int sourceX = source.getWidth() - size + (x < size / 2 ? size - 1 - x : x);
				ring.setRGB(x, y, source.getRGB(sourceX, y));
			}
		}
		if (innerDiameter <= 0)
		{
			// The native fill has transparent edge pixels. Keep an opaque, themed
			// background below it so the game scene cannot show through those gaps.
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
		frames.clear();
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
