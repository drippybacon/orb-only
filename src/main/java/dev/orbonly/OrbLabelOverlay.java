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

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import net.runelite.api.MenuAction;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.OverlayPosition;

/** Independently movable value label; RuneLite saves its position. */
final class OrbLabelOverlay extends Overlay
{
	private final OrbOnlyConfig config;
	private final OrbUiState uiState;
	private final OrbValueOverlay values;
	private final OrbType orb;

	OrbLabelOverlay(OrbOnlyConfig config, OrbUiState uiState,
		OrbValueOverlay values, OrbOnlyPlugin plugin, OrbType orb)
	{
		super(plugin);
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
		OrbUiState.Surface surface = uiState.surface(orb);
		if (!config.showOrbValues() || config.labelPosition() != OrbLabelPosition.FREE
			|| surface == null)
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
				// Follow the orb until a Free position is saved.
				Rectangle circle = surface.circle();
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
