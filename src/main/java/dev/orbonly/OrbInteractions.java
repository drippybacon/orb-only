/*
 * Copyright (c) 2026, drippybacon
 * Copyright (c) 2016-2017, Adam <Adam@sigterm.info>
 * Copyright (c) 2026, Frederick Campbell
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
/* Native orb menus adapted from Interface Layout / Custom Game Tabs.
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

/** Projects native widget actions onto circular orb hit regions. */
@Singleton
final class OrbInteractions
{
	private final Client client;
	private final OrbUiState uiState;
	private final EventBus eventBus;

	@Inject
	OrbInteractions(Client client, OrbUiState uiState, EventBus eventBus)
	{
		this.client = client;
		this.uiState = uiState;
		this.eventBus = eventBus;
	}

	boolean hovered(OrbUiState.Surface surface)
	{
		Point mouse = client.getMouseCanvasPosition();
		Rectangle circle = surface.circle();
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

		// Remove native entries that still refer to the rectangular number panels.
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
			Rectangle circle = surface.circle();
			if (!uiState.current(surface) || circle == null
				|| !OrbUiState.contains(circle, mouse.getX(), mouse.getY()))
			{
				continue;
			}
			// Block scene actions through a visible orb, including healthy HP with no cure action.
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
					.setOption(action).setTarget(button.getName() == null ? "" : button.getName())
					.setType(slot < 5 ? MenuAction.CC_OP : MenuAction.CC_OP_LOW_PRIORITY)
					.setIdentifier(slot + 1).setParam0(-1).setParam1(button.getId());
				// Let other plugins attach options to the reconstructed native entry.
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
