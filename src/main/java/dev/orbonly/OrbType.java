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
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;

enum OrbType
{
	HITPOINTS(
		InterfaceID.Orbs.ORB_HEALTH, InterfaceID.Orbs.ORB_HEALTH_HEART_ICON,
		InterfaceID.Orbs.HEALTH_TEXT, InterfaceID.Orbs.HEALTHBUTTON,
		InterfaceID.Orbs.HEALTH_BACKING,
		InterfaceID.OrbsNomap.ORB_HEALTH, InterfaceID.OrbsNomap.ORB_HEALTH_HEART_ICON,
		InterfaceID.OrbsNomap.HEALTH_TEXT, InterfaceID.OrbsNomap.HEALTHBUTTON,
		InterfaceID.OrbsNomap.HEALTH_BACKING),
	PRAYER(
		InterfaceID.Orbs.ORB_PRAYER, InterfaceID.Orbs.PRAYER_ICON,
		InterfaceID.Orbs.PRAYER_TEXT, InterfaceID.Orbs.PRAYERBUTTON,
		InterfaceID.Orbs.PRAYER_BACKING,
		InterfaceID.OrbsNomap.ORB_PRAYER, InterfaceID.OrbsNomap.PRAYER_ICON,
		InterfaceID.OrbsNomap.PRAYER_TEXT, InterfaceID.OrbsNomap.PRAYERBUTTON,
		InterfaceID.OrbsNomap.PRAYER_BACKING),
	RUN_ENERGY(
		InterfaceID.Orbs.ORB_RUNENERGY, InterfaceID.Orbs.RUNENERGY_ICON,
		InterfaceID.Orbs.RUNENERGY_TEXT, InterfaceID.Orbs.RUNBUTTON,
		InterfaceID.Orbs.RUNENERGY_BACKING,
		InterfaceID.OrbsNomap.ORB_RUNENERGY, InterfaceID.OrbsNomap.RUNENERGY_ICON,
		InterfaceID.OrbsNomap.RUNENERGY_TEXT, InterfaceID.OrbsNomap.RUNBUTTON,
		InterfaceID.OrbsNomap.RUNENERGY_BACKING),
	SPECIAL_ATTACK(
		InterfaceID.Orbs.ORB_SPECENERGY, InterfaceID.Orbs.SPECENERGY_ICON,
		InterfaceID.Orbs.SPECENERGY_TEXT, InterfaceID.Orbs.SPECBUTTON,
		InterfaceID.Orbs.SPECENERGY_BACKING,
		InterfaceID.OrbsNomap.ORB_SPECENERGY, InterfaceID.OrbsNomap.SPECENERGY_ICON,
		InterfaceID.OrbsNomap.SPECENERGY_TEXT, InterfaceID.OrbsNomap.SPECBUTTON,
		InterfaceID.OrbsNomap.SPECENERGY_BACKING);

	private final Layout[] layouts;

	OrbType(int container, int icon, int value, int button, int backing,
		int nomapContainer, int nomapIcon, int nomapValue, int nomapButton, int nomapBacking)
	{
		layouts = new Layout[]{
			new Layout(container, icon, value, button, backing),
			new Layout(nomapContainer, nomapIcon, nomapValue, nomapButton, nomapBacking)
		};
	}

	Layout visibleLayout(Client client)
	{
		for (Layout layout : layouts)
		{
			if (circle(client.getWidget(layout.containerId), client.getWidget(layout.iconId)) != null)
			{
				return layout;
			}
		}
		return null;
	}

	Rectangle getCircleBounds(Client client)
	{
		Layout layout = visibleLayout(client);
		return layout == null ? null : circle(client.getWidget(layout.containerId), client.getWidget(layout.iconId));
	}

	static Rectangle circle(Widget container, Widget icon)
	{
		if (container == null || container.isHidden() || icon == null || icon.isHidden())
		{
			return null;
		}
		Rectangle iconBounds = icon.getBounds();
		Rectangle containerBounds = container.getBounds();
		int diameter = Math.min(containerBounds.width, containerBounds.height);
		if (iconBounds.isEmpty() || diameter <= 0)
		{
			return null;
		}
		return new Rectangle(
			(int) Math.round(iconBounds.getCenterX() - diameter / 2.0),
			(int) Math.round(iconBounds.getCenterY() - diameter / 2.0), diameter, diameter);
	}

	static final class Layout
	{
		final int containerId;
		final int iconId;
		final int valueId;
		final int buttonId;
		final int backingId;

		private Layout(int containerId, int iconId, int valueId, int buttonId, int backingId)
		{
			this.containerId = containerId;
			this.iconId = iconId;
			this.valueId = valueId;
			this.buttonId = buttonId;
			this.backingId = backingId;
		}
	}
}
