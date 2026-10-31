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
	private OrbUiState(Client client, OrbFrameImages frames)
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
		surfaces.clear();
		pass++;
		for (OrbType orb : OrbType.values())
		{
			Widget container = client.getWidget(orb.containerId);
			Widget backing = client.getWidget(orb.backingId);
			Widget button = client.getWidget(orb.buttonId);
			Widget value = client.getWidget(orb.valueId);
			Rectangle circle = orb.getCircleBounds(client);
			Widget icon = client.getWidget(orb.iconId);
			if (circle == null || backing == null || button == null)
			{
				continue;
			}
			int innerDiameter = Math.min(icon.getWidth(), icon.getHeight());
			if (frames.get(backing, SpriteID.MINIMAP_ORB_FRAME, circle.width, innerDiameter) == null)
			{
				// Keep the native frame until SpriteManager has loaded its artwork.
				continue;
			}
			// Rendering does not depend on the button having a client script.
			// HP's cure operation can be handled by the server without an onOp listener.
			surfaces.put(orb, new Surface(orb, container, backing, button));
			hide(backing);
			hide(button);
			hide(value);
			disableBlocking(container);
			disableBlocking(backing);
			disableBlocking(button);
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
			&& client.getWidget(surface.orb.containerId) == surface.container
			&& client.getWidget(surface.orb.buttonId) == surface.button
			&& surface.orb.getCircleBounds(client) != null;
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
		widget.setHidden(true);
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
		widget.setNoClickThrough(false);
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
		final Widget container;
		final Widget backing;
		final Widget button;

		private Surface(OrbType orb, Widget container, Widget backing, Widget button)
		{
			this.orb = orb;
			this.container = container;
			this.backing = backing;
			this.button = button;
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
