package dev.orbonly;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.SpriteID;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/** Backgrounds below native gauges; the live fill, icon and timers stay native. */
final class OrbBackgroundOverlay extends Overlay
{
	private final Client client;
	private final OrbUiState uiState;
	private final OrbFrameImages frames;

	@Inject
	private OrbBackgroundOverlay(Client client, OrbUiState uiState, OrbFrameImages frames, OrbOnlyPlugin plugin)
	{
		super(plugin);
		this.client = client;
		this.uiState = uiState;
		this.frames = frames;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.UNDER_WIDGETS);
		// Apply after layout overlays have positioned the native orbs.
		setPriority(100f);
		setMovable(false);
		setSnappable(false);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		Graphics2D draw = (Graphics2D) graphics.create();
		try
		{
			draw.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
				RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
			for (OrbUiState.Surface surface : uiState.surfaces())
			{
				if (!uiState.current(surface))
				{
					continue;
				}
				Rectangle circle = surface.orb.getCircleBounds(client);
				BufferedImage background = frames.getBackground(surface.backing,
					SpriteID.MINIMAP_ORB_FRAME, circle.width);
				if (background != null)
				{
					draw.drawImage(background, circle.x, circle.y, circle.width, circle.height, null);
				}
			}
		}
		finally
		{
			draw.dispose();
		}
		return null;
	}
}
