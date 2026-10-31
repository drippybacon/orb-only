/* Native operation projection follows Interface Layout / Custom Game Tabs.
 * See THIRD-PARTY-NOTICES.md for attribution and license. */
package dev.orbonly;

import java.awt.Rectangle;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.Menu;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.Point;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetConfig;
import net.runelite.api.widgets.WidgetConfigNode;
import net.runelite.client.eventbus.EventBus;

/** Overlay hit regions; native widget positions and dimensions are never changed. */
@Singleton
final class OrbInteractions
{
	private final Client client;
	private final OrbUiState uiState;
	private final EventBus eventBus;

	@Inject
	private OrbInteractions(Client client, OrbUiState uiState, EventBus eventBus)
	{
		this.client = client;
		this.uiState = uiState;
		this.eventBus = eventBus;
	}

	boolean hovered(OrbUiState.Surface surface)
	{
		Point mouse = client.getMouseCanvasPosition();
		Rectangle circle = surface.orb.getCircleBounds(client);
		return !client.isMenuOpen() && uiState.current(surface) && circle != null && mouse != null
			&& OrbUiState.contains(circle, mouse.getX(), mouse.getY())
			&& !foreignMenu(client.getMenu(), uiState.surfaces());
	}

	void updateMenu()
	{
		if (client.isMenuOpen())
		{
			return;
		}
		Menu menu = client.getMenu();
		List<OrbUiState.Surface> surfaces = uiState.surfaces();
		if (menu == null || surfaces.isEmpty() || foreignMenu(menu, surfaces))
		{
			return;
		}

		// Drop any stale native entries from the old rectangular number panels.
		for (MenuEntry entry : menu.getMenuEntries())
		{
			if (managed(entry.getWidget(), surfaces))
			{
				menu.removeMenuEntry(entry);
			}
		}
		Point mouse = client.getMouseCanvasPosition();
		if (mouse == null)
		{
			return;
		}
		for (OrbUiState.Surface surface : surfaces)
		{
			Rectangle circle = surface.orb.getCircleBounds(client);
			if (!uiState.current(surface) || circle == null
				|| !OrbUiState.contains(circle, mouse.getX(), mouse.getY()))
			{
				continue;
			}
			// The visible circle occupies this area, even when it has no action (healthy HP).
			for (MenuEntry entry : menu.getMenuEntries())
			{
				if (entry.getType() != MenuAction.CANCEL)
				{
					menu.removeMenuEntry(entry);
				}
			}
			Widget button = surface.button;
			String[] actions = button.getActions();
			if (uiState.nativeHidden(button) || actions == null)
			{
				return;
			}
			boolean scripted = button.getOnOpListener() != null;
			WidgetConfigNode serverConfig = client.getWidgetConfig(button);
			int clickMask = serverConfig == null ? button.getClickMask() : serverConfig.getClickMask();
			for (int slot = Math.min(actions.length, 10) - 1; slot >= 0; slot--)
			{
				String action = actions[slot];
				if (action == null || action.isBlank()
					|| (!scripted && (clickMask & WidgetConfig.transmitAction(slot)) == 0))
				{
					continue;
				}
				MenuEntry entry = menu.createMenuEntry(-1)
					.setOption(action).setTarget("")
					.setType(slot < 5 ? MenuAction.CC_OP : MenuAction.CC_OP_LOW_PRIORITY)
					.setIdentifier(slot + 1).setParam0(-1).setParam1(button.getId());
				// A real user click executes the game's original widget operation.
				// Notify other plugins so their registered orb menu options remain available.
				eventBus.post(new MenuEntryAdded(entry));
			}
			return;
		}
	}

	private boolean foreignMenu(Menu menu, List<OrbUiState.Surface> surfaces)
	{
		if (menu == null)
		{
			return false;
		}
		for (MenuEntry entry : menu.getMenuEntries())
		{
			if (entry.getType() == MenuAction.RUNELITE_OVERLAY
				|| entry.getType() == MenuAction.RUNELITE_OVERLAY_CONFIG)
			{
				return true;
			}
			Widget widget = entry.getWidget();
			if (widget != null && !managed(widget, surfaces))
			{
				return true;
			}
		}
		return false;
	}

	private boolean managed(Widget widget, List<OrbUiState.Surface> surfaces)
	{
		for (int depth = 0; widget != null && depth < 8; depth++, widget = widget.getParent())
		{
			for (OrbUiState.Surface surface : surfaces)
			{
				if (uiState.current(surface) && (widget == surface.container || widget == surface.button))
				{
					return true;
				}
			}
		}
		return false;
	}
}
