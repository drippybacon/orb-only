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
import java.util.ArrayList;
import java.util.List;
import net.runelite.api.Menu;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.Point;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetConfig;
import net.runelite.api.widgets.WidgetConfigNode;
import net.runelite.client.eventbus.EventBus;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class OrbInteractionsTest
{
	private OrbTestContext context;
	private OrbTestContext.OrbWidgets special;
	private Menu menu;
	private EventBus eventBus;
	private OrbInteractions interactions;
	private final List<MenuEntry> entries = new ArrayList<>();
	private final List<MenuEntry> created = new ArrayList<>();

	@Before
	public void setUp()
	{
		context = new OrbTestContext();
		special = context.special();
		menu = mock(Menu.class);
		eventBus = mock(EventBus.class);
		interactions = new OrbInteractions(context.client, context.ui, eventBus);
		when(context.client.getMenu()).thenReturn(menu);
		when(context.client.getMouseCanvasPosition()).thenReturn(new Point(140, 67));
		when(menu.getMenuEntries()).thenAnswer(call -> entries.toArray(new MenuEntry[0]));
		doAnswer(call ->
		{
			entries.remove(call.getArgument(0));
			return null;
		}).when(menu).removeMenuEntry(any());
		when(menu.createMenuEntry(-1)).thenAnswer(call ->
		{
			MenuEntry entry = mock(MenuEntry.class, RETURNS_SELF);
			entries.add(entry);
			created.add(entry);
			return entry;
		});
		MenuEntry cancel = mock(MenuEntry.class);
		when(cancel.getType()).thenReturn(MenuAction.CANCEL);
		entries.add(cancel);
		when(special.button.getActions()).thenReturn(new String[]{"Use", "Release"});
		when(special.button.getName()).thenReturn("Wind Mote");
		when(special.button.getClickMask()).thenReturn(WidgetConfig.transmitAction(0) | WidgetConfig.transmitAction(1));
		context.ui.prepare();
	}

	@Test
	public void preservesNativeOperationIdentityAndTarget()
	{
		interactions.updateMenu();
		assertEquals(2, created.size());
		verify(created.get(0)).setOption("Release");
		verify(created.get(0)).setIdentifier(2);
		verify(created.get(1)).setOption("Use");
		verify(created.get(1)).setIdentifier(1);
		for (MenuEntry entry : created)
		{
			verify(entry).setTarget("Wind Mote");
			verify(entry).setType(MenuAction.CC_OP);
			verify(entry).setParam0(-1);
			verify(entry).setParam1(special.button.getId());
		}
		verify(eventBus, times(2)).post(isA(MenuEntryAdded.class));
	}

	@Test
	public void disabledServerOperationsAreNotRecreated()
	{
		when(special.button.getClickMask()).thenReturn(0);
		interactions.updateMenu();
		assertTrue(created.isEmpty());
		verifyNoInteractions(eventBus);
	}

	@Test
	public void serverConfigTakesPrecedenceOverWidgetMask()
	{
		WidgetConfigNode config = mock(WidgetConfigNode.class);
		when(context.client.getWidgetConfig(special.button)).thenReturn(config);
		when(config.getClickMask()).thenReturn(WidgetConfig.transmitAction(1));
		interactions.updateMenu();
		assertEquals(1, created.size());
		verify(created.get(0)).setIdentifier(2);
	}

	@Test
	public void scriptedOperationsRetainTheirLowPrioritySlot()
	{
		when(special.button.getActions()).thenReturn(new String[]{null, "", " ", null, null, "Configure"});
		when(special.button.getOnOpListener()).thenReturn(new Object[]{123});
		when(special.button.getClickMask()).thenReturn(0);
		interactions.updateMenu();
		assertEquals(1, created.size());
		verify(created.get(0)).setType(MenuAction.CC_OP_LOW_PRIORITY);
		verify(created.get(0)).setIdentifier(6);
	}

	@Test
	public void gameHiddenButtonHasNoReplacementAction()
	{
		context.ui.releaseForNativeUpdates();
		special.button.setHidden(true);
		context.ui.prepare();
		interactions.updateMenu();
		assertTrue(created.isEmpty());
	}

	@Test
	public void numberPanelDoesNotActivateOrbAndStaleNativeEntryIsRemoved()
	{
		MenuEntry stale = mock(MenuEntry.class);
		when(stale.getWidget()).thenReturn(special.button);
		entries.add(stale);
		when(context.client.getMouseCanvasPosition()).thenReturn(new Point(110, 67));
		interactions.updateMenu();
		assertTrue(created.isEmpty());
		assertFalse(entries.contains(stale));
	}

	@Test
	public void foreignInterfaceMenuHasPriority()
	{
		Widget foreign = context.widget(98765, new Rectangle(0, 0, 200, 200));
		MenuEntry entry = mock(MenuEntry.class);
		when(entry.getWidget()).thenReturn(foreign);
		entries.add(entry);
		interactions.updateMenu();
		assertTrue(created.isEmpty());
		verify(menu, never()).removeMenuEntry(any());
		assertFalse(interactions.hovered(context.ui.surface(OrbType.SPECIAL_ATTACK)));
	}

	@Test
	public void overlayControlsHavePriority()
	{
		MenuEntry entry = mock(MenuEntry.class);
		when(entry.getType()).thenReturn(MenuAction.RUNELITE_OVERLAY_CONFIG);
		entries.add(entry);
		interactions.updateMenu();
		assertTrue(created.isEmpty());
		verify(menu, never()).removeMenuEntry(any());
	}

	@Test
	public void openMenuIsNotMutated()
	{
		when(context.client.isMenuOpen()).thenReturn(true);
		interactions.updateMenu();
		verifyNoInteractions(menu);
	}
}
