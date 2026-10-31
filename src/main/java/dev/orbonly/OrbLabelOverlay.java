package dev.orbonly;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.OverlayPosition;

/** One independently movable label, with its position persisted by RuneLite. */
final class OrbLabelOverlay extends Overlay
{
	private final Client client;
	private final OrbOnlyConfig config;
	private final OrbUiState uiState;
	private final OrbValueOverlay values;
	private final OrbType orb;

	OrbLabelOverlay(Client client, OrbOnlyConfig config, OrbUiState uiState,
		OrbValueOverlay values, OrbOnlyPlugin plugin, OrbType orb)
	{
		super(plugin);
		this.client = client;
		this.config = config;
		this.uiState = uiState;
		this.values = values;
		this.orb = orb;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		setMovable(true);
		setSnappable(false);
		setResizable(false);
		addMenuEntry(MenuAction.RUNELITE_OVERLAY_CONFIG, OverlayManager.OPTION_CONFIGURE, "Orb Only");
	}

	@Override
	public String getName()
	{
		return "OrbOnlyLabel_" + orb.name();
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.showOrbValues() || config.labelPosition() != OrbLabelPosition.FREE
			|| uiState.surface(orb) == null)
		{
			return null;
		}
		String text = values.valueText(orb);
		if (text == null)
		{
			return null;
		}
		Graphics2D draw = (Graphics2D) graphics.create();
		try
		{
			draw.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
				RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
			Dimension size = OrbValueOverlay.valueSize(draw, text, config.valueFontSize());
			if (getPreferredLocation() == null)
			{
				// Until dragged (or after Alt-right-click reset), follow the current orb.
				Rectangle circle = orb.getCircleBounds(client);
				Point anchor = new Point((int) Math.round(circle.getCenterX() - size.width / 2.0),
					(int) Math.round(circle.getCenterY() - size.height / 2.0));
				draw.translate(anchor.x - getBounds().x, anchor.y - getBounds().y);
				getBounds().setLocation(anchor);
			}
			OrbValueOverlay.drawValue(draw, new Rectangle(0, 0, size.width, size.height), text,
				config.valueFontSize(), values.valueColor(orb));
			return size;
		}
		finally
		{
			draw.dispose();
		}
	}
}
