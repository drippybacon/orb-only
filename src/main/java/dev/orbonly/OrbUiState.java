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
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.SpriteID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetType;

@Singleton
final class OrbUiState
{
	private final Client client;
	private final OrbFrameImages frames;
	private final Map<Widget, State> states = new IdentityHashMap<>();
	private final Map<OrbType, Surface> surfaces = new EnumMap<>(OrbType.class);
	private long pass;

	@Inject
	OrbUiState(Client client, OrbFrameImages frames)
	{
		this.client = client;
		this.frames = frames;
	}

	void prepare()
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			restore();
			return;
		}
		pass++;
		frames.beginPass();
		for (OrbType orb : OrbType.values())
		{
			OrbType.Layout layout = orb.visibleLayout(client);
			if (layout == null)
			{
				surfaces.remove(orb);
				continue;
			}
			Widget container = client.getWidget(layout.containerId);
			Widget icon = client.getWidget(layout.iconId);
			Widget backing = client.getWidget(layout.backingId);
			Widget button = client.getWidget(layout.buttonId);
			Widget value = client.getWidget(layout.valueId);
			Rectangle circle = OrbType.circle(container, icon);
			if (circle == null || backing == null || button == null)
			{
				surfaces.remove(orb);
				continue;
			}
			Surface surface = surfaces.get(orb);
			if (surface == null || surface.container != container || surface.icon != icon
				|| surface.backing != backing || surface.button != button || surface.value != value)
			{
				surfaces.put(orb, new Surface(orb, layout, container, icon, backing, button, value));
			}
			// Keep native draw order so the opaque backing stays beneath the gauge.
			hide(button);
			hide(value);
			disableBlocking(container);
			disableBlocking(backing);
			disableBlocking(button);
			// Keep replacement labels active even if sprite art is temporarily unavailable.
			frames.prepareBackground(backing, circle);
			Widget[] children = container.getChildren();
			if (children != null)
			{
				for (Widget child : children)
				{
					if (child != null && child.getType() == WidgetType.LAYER && !child.hasListener())
					{
						disableBlocking(child);
					}
				}
			}
		}
		frames.finishPass();
		// Release retired widgets and controls that fell back to native rendering.
		states.entrySet().removeIf(entry ->
		{
			if (entry.getValue().lastPass == pass)
			{
				return false;
			}
			entry.getValue().restore(entry.getKey());
			return true;
		});
	}

	List<Surface> surfaces()
	{
		return new ArrayList<>(surfaces.values());
	}

	Surface surface(OrbType orb)
	{
		Surface surface = surfaces.get(orb);
		return surface != null && current(surface) ? surface : null;
	}

	boolean current(Surface surface)
	{
		return client.getGameState() == GameState.LOGGED_IN
			&& client.getWidget(surface.layout.containerId) == surface.container
			&& client.getWidget(surface.layout.buttonId) == surface.button
			&& surface.circle() != null;
	}

	boolean nativeHidden(Widget widget)
	{
		State state = states.get(widget);
		return state != null && state.hiddenApplied ? state.hidden : widget.isSelfHidden();
	}

	private void hide(Widget widget)
	{
		if (widget == null)
		{
			return;
		}
		State state = states.computeIfAbsent(widget, ignored -> new State());
		state.lastPass = pass;
		if (!state.hiddenApplied || !widget.isSelfHidden())
		{
			state.hidden = widget.isSelfHidden();
		}
		state.hiddenApplied = true;
		if (!widget.isSelfHidden())
		{
			widget.setHidden(true);
		}
	}

	private void disableBlocking(Widget widget)
	{
		State state = states.computeIfAbsent(widget, ignored -> new State());
		state.lastPass = pass;
		if (!state.blockingApplied || widget.getNoClickThrough())
		{
			state.blocking = widget.getNoClickThrough();
		}
		state.blockingApplied = true;
		if (widget.getNoClickThrough())
		{
			widget.setNoClickThrough(false);
		}
	}

	void releaseForNativeUpdates()
	{
		states.forEach((widget, state) -> state.restore(widget));
	}

	void restore()
	{
		releaseForNativeUpdates();
		states.clear();
		surfaces.clear();
		frames.clear();
	}

	static boolean contains(Rectangle circle, int x, int y)
	{
		if (circle.isEmpty())
		{
			return false;
		}
		double dx = (x - circle.getCenterX()) / (circle.width / 2.0);
		double dy = (y - circle.getCenterY()) / (circle.height / 2.0);
		return dx * dx + dy * dy <= 1;
	}

	static final class Surface
	{
		final OrbType orb;
		final OrbType.Layout layout;
		final Widget container;
		final Widget icon;
		final Widget backing;
		final Widget button;
		final Widget value;

		private Surface(OrbType orb, OrbType.Layout layout, Widget container, Widget icon,
			Widget backing, Widget button, Widget value)
		{
			this.orb = orb;
			this.layout = layout;
			this.container = container;
			this.icon = icon;
			this.backing = backing;
			this.button = button;
			this.value = value;
		}

		Rectangle circle()
		{
			return OrbType.circle(container, icon);
		}
	}

	private static final class State
	{
		private boolean hidden;
		private boolean blocking;
		private boolean hiddenApplied;
		private boolean blockingApplied;
		private long lastPass;

		private void restore(Widget widget)
		{
			if (hiddenApplied)
			{
				widget.setHidden(hidden);
				hiddenApplied = false;
			}
			if (blockingApplied)
			{
				widget.setNoClickThrough(blocking);
				blockingApplied = false;
			}
		}
	}
}
