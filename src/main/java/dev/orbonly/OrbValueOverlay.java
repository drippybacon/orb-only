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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.Arc2D;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Prayer;
import net.runelite.api.SpriteID;
import net.runelite.api.VarPlayer;
import net.runelite.api.Varbits;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.util.Text;

public class OrbValueOverlay extends Overlay
{
	private static final Prayer[] PRAYERS = Prayer.values();

	private final Client client;
	private final OrbOnlyConfig config;
	private final OrbUiState uiState;
	private final OrbFrameImages frames;
	private final OrbInteractions interactions;
	private final OrbPoisonTimer poisonTimer;
	private final OrbPrayerHelper prayerHelper;

	@Inject
	OrbValueOverlay(Client client, OrbOnlyConfig config, OrbOnlyPlugin plugin,
		OrbUiState uiState, OrbFrameImages frames, OrbInteractions interactions, OrbPoisonTimer poisonTimer,
		OrbPrayerHelper prayerHelper)
	{
		super(plugin);
		this.client = client;
		this.config = config;
		this.uiState = uiState;
		this.frames = frames;
		this.interactions = interactions;
		this.poisonTimer = poisonTimer;
		this.prayerHelper = prayerHelper;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.MANUAL);
		drawAfterInterface(InterfaceID.ORBS);
		drawAfterInterface(InterfaceID.ORBS_NOMAP);
		setMovable(false);
		setSnappable(false);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return null;
		}

		Graphics2D valuesGraphics = (Graphics2D) graphics.create();
		try
		{
			valuesGraphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
				RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
			valuesGraphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
				RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
			for (OrbUiState.Surface surface : uiState.surfaces())
			{
				if (!uiState.current(surface))
				{
					continue;
				}
				OrbType orb = surface.orb;
				Rectangle circle = surface.circle();
				Widget icon = surface.icon;
				int innerDiameter = Math.min(icon.getWidth(), icon.getHeight());
				boolean hover = !uiState.nativeHidden(surface.button) && interactions.hovered(surface);
				Widget frame = hover ? surface.button : surface.backing;
				BufferedImage ring = frames.get(frame, hover ? SpriteID.MINIMAP_ORB_FRAME_HOVERED
					: SpriteID.MINIMAP_ORB_FRAME, circle.width, innerDiameter);
				if (ring == null && hover)
				{
					ring = frames.get(surface.backing, SpriteID.MINIMAP_ORB_FRAME, circle.width, innerDiameter);
				}
				if (ring != null)
				{
					// Leave the native gauge, icon and timers visible beneath this frame.
					valuesGraphics.drawImage(ring, circle.x, circle.y, circle.width, circle.height, null);
				}
				Color highlight = highlightColor(orb);
				if (highlight != null)
				{
					drawActiveHighlight(valuesGraphics, circle, highlight);
				}
				if (orb == OrbType.HITPOINTS && config.showPoisonCountdown() && poisonTimer.active())
				{
					drawPoisonCountdown(valuesGraphics, circle, poisonTimer.remainingFraction(),
						poisonTimer.venomed() ? config.venomRingColor() : config.poisonRingColor(),
						config.poisonRingThickness());
				}
				if (config.showOrbValues() && config.labelPosition() == OrbLabelPosition.CENTER)
				{
					String valueText = valueText(orb);
					if (valueText != null)
					{
						drawValue(valuesGraphics, circle, valueText, config.valueFontSize(),
							highlight != null ? highlight : config.labelColor());
					}
				}
				if (orb == OrbType.PRAYER && !uiState.nativeHidden(surface.button))
				{
					// Keep the dose pulse and tick sweep visible above centered labels.
					prayerHelper.drawDoseAndTooltip(valuesGraphics, circle, !config.showPrayerPoints(),
						interactions.hovered(surface));
					if (config.showPrayerTickHelper())
					{
						prayerHelper.drawTickHelper(valuesGraphics, circle);
					}
				}
			}
		}
		finally
		{
			valuesGraphics.dispose();
		}
		return null;
	}

	String valueText(OrbType orb)
	{
		if (orb == OrbType.PRAYER)
		{
			return prayerHelper.label(!config.showPrayerPoints(),
				config.labelPosition() == OrbLabelPosition.CENTER);
		}
		OrbUiState.Surface surface = uiState.surface(orb);
		Widget value = surface == null ? null : surface.value;
		if (value == null || value.getText() == null)
		{
			return null;
		}
		String text = Text.removeTags(value.getText()).trim();
		return text.isEmpty() ? null : text;
	}

	Color valueColor(OrbType orb)
	{
		Color highlight = highlightColor(orb);
		return highlight != null ? highlight : config.labelColor();
	}

	private Color highlightColor(OrbType orb)
	{
		if (orb == OrbType.SPECIAL_ATTACK && config.highlightActiveSpecialAttack()
			&& client.getVarpValue(VarPlayer.SPECIAL_ATTACK_ENABLED) > 0)
		{
			return config.activeSpecialAttackColor();
		}
		if (orb == OrbType.PRAYER && config.highlightActivePrayer() && prayerActive())
		{
			return config.activePrayerColor();
		}
		return null;
	}

	private boolean prayerActive()
	{
		if (client.getVarbitValue(Varbits.QUICK_PRAYER) == 1)
		{
			return true;
		}
		for (Prayer prayer : PRAYERS)
		{
			if (client.isPrayerActive(prayer))
			{
				return true;
			}
		}
		return false;
	}

	static void drawValue(Graphics2D graphics, Rectangle circle, String text, int configuredFontSize)
	{
		drawValue(graphics, circle, text, configuredFontSize, Color.WHITE);
	}

	static Dimension valueSize(Graphics2D graphics, String text, int configuredFontSize)
	{
		Font font = FontManager.getRunescapeBoldFont().deriveFont((float) Math.max(10, Math.min(24, configuredFontSize)));
		Rectangle bounds = font.createGlyphVector(graphics.getFontRenderContext(), text)
			.getPixelBounds(graphics.getFontRenderContext(), 0, 0);
		return new Dimension(Math.max(18, bounds.width + 4), Math.max(18, bounds.height + 4));
	}

	static void drawActiveHighlight(Graphics2D graphics, Rectangle circle, Color color)
	{
		Stroke previousStroke = graphics.getStroke();
		graphics.setStroke(new BasicStroke(2f));
		graphics.setColor(color);
		graphics.drawOval(circle.x + 1, circle.y + 1, circle.width - 3, circle.height - 3);
		graphics.setStroke(previousStroke);
	}

	static void drawPoisonCountdown(Graphics2D graphics, Rectangle circle, double remaining,
		Color color, int configuredThickness)
	{
		Graphics2D draw = (Graphics2D) graphics.create();
		try
		{
			int thickness = Math.max(1, Math.min(4, configuredThickness));
			// Use the frame's bounds so the countdown follows moved orbs.
			double inset = thickness / 2.0;
			Arc2D.Double arc = new Arc2D.Double(circle.x + inset, circle.y + inset,
				circle.width - thickness - 1, circle.height - thickness - 1, 90, -360, Arc2D.OPEN);
			draw.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			draw.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
			draw.setStroke(new BasicStroke(thickness, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
			draw.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 45));
			draw.draw(arc);
			double fraction = Math.max(0, Math.min(1, remaining));
			arc.setAngleStart(90 - 360 * (1 - fraction));
			arc.setAngleExtent(-360 * fraction);
			draw.setColor(color);
			draw.draw(arc);
		}
		finally
		{
			draw.dispose();
		}
	}

	static void drawValue(Graphics2D graphics, Rectangle circle, String text, int configuredFontSize,
		Color color)
	{
		Font baseFont = FontManager.getRunescapeBoldFont();
		int fontSize = Math.max(10, Math.min(24, configuredFontSize));
		Rectangle textBounds;
		do
		{
			Font font = baseFont.deriveFont((float) fontSize);
			graphics.setFont(font);
			textBounds = font.createGlyphVector(graphics.getFontRenderContext(), text)
				.getPixelBounds(graphics.getFontRenderContext(), 0, 0);
			if (fontSize <= 8 || (textBounds.width <= circle.width - 4 && textBounds.height <= circle.height - 4))
			{
				break;
			}
			fontSize--;
		}
		while (true);

		int x = (int) Math.round(circle.getCenterX() - textBounds.getCenterX());
		int y = (int) Math.round(circle.getCenterY() - textBounds.getCenterY());
		graphics.setColor(Color.BLACK);
		for (int dx = -1; dx <= 1; dx++)
		{
			for (int dy = -1; dy <= 1; dy++)
			{
				if (dx != 0 || dy != 0)
				{
					graphics.drawString(text, x + dx, y + dy);
				}
			}
		}
		graphics.setColor(color);
		graphics.drawString(text, x, y);
	}
}
