package dev.orbonly;

import java.awt.Rectangle;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;

enum OrbType
{
	HITPOINTS(InterfaceID.Orbs.ORB_HEALTH, InterfaceID.Orbs.ORB_HEALTH_HEART_ICON,
		InterfaceID.Orbs.HEALTH_TEXT, InterfaceID.Orbs.HEALTHBUTTON, InterfaceID.Orbs.HEALTH_BACKING),
	PRAYER(InterfaceID.Orbs.ORB_PRAYER, InterfaceID.Orbs.PRAYER_ICON,
		InterfaceID.Orbs.PRAYER_TEXT, InterfaceID.Orbs.PRAYERBUTTON, InterfaceID.Orbs.PRAYER_BACKING),
	RUN_ENERGY(InterfaceID.Orbs.ORB_RUNENERGY, InterfaceID.Orbs.RUNENERGY_ICON,
		InterfaceID.Orbs.RUNENERGY_TEXT, InterfaceID.Orbs.RUNBUTTON, InterfaceID.Orbs.RUNENERGY_BACKING),
	SPECIAL_ATTACK(InterfaceID.Orbs.ORB_SPECENERGY, InterfaceID.Orbs.SPECENERGY_ICON,
		InterfaceID.Orbs.SPECENERGY_TEXT, InterfaceID.Orbs.SPECBUTTON, InterfaceID.Orbs.SPECENERGY_BACKING);

	final int containerId;
	final int iconId;
	final int valueId;
	final int buttonId;
	final int backingId;

	OrbType(int containerId, int iconId, int valueId, int buttonId, int backingId)
	{
		this.containerId = containerId;
		this.iconId = iconId;
		this.valueId = valueId;
		this.buttonId = buttonId;
		this.backingId = backingId;
	}

	Rectangle getCircleBounds(Client client)
	{
		Widget container = client.getWidget(containerId);
		Widget icon = client.getWidget(iconId);
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
			(int) Math.round(iconBounds.getCenterY() - diameter / 2.0),
			diameter, diameter);
	}
}
