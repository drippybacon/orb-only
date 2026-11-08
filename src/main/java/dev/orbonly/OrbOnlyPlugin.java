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

import com.google.inject.Provides;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.runelite.api.ScriptEvent;
import net.runelite.api.ScriptID;
import net.runelite.api.events.BeforeRender;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.PostMenuSort;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ProfileChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
		name = "Orb Only",
		description = "Circular HP, prayer, run energy and special attack orbs with optional labels"
)
public class OrbOnlyPlugin extends Plugin
{
	@Inject
	private OrbOnlyConfig config;

	@Inject
	private ConfigManager configManager;

	@Inject
	private ClientThread clientThread;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private OrbValueOverlay valueOverlay;

	@Inject
	private OrbUiState uiState;

	@Inject
	private OrbInteractions interactions;

	@Inject
	private OrbPoisonTimer poisonTimer;

	@Inject
	private OrbPrayerHelper prayerHelper;

	private int scriptDepth;
	private boolean nativeStateReleased;
	private volatile boolean active;
	private final List<OrbLabelOverlay> labelOverlays = new ArrayList<>();

	@Provides
	OrbOnlyConfig provideConfig(ConfigManager configManager)
	{
		migratePrayerLabelConfig(configManager);
		return configManager.getConfig(OrbOnlyConfig.class);
	}

	static void migratePrayerLabelConfig(ConfigManager configManager)
	{
		// The renamed toggle has the opposite meaning; preserve the saved display mode.
		String previous = configManager.getConfiguration("orbonly", "showPrayerTimeRemaining");
		if (!"true".equals(previous) && !"false".equals(previous))
		{
			return;
		}
		if (configManager.getConfiguration("orbonly", "showPrayerPoints") == null)
		{
			configManager.setConfiguration("orbonly", "showPrayerPoints", !Boolean.parseBoolean(previous));
		}
		configManager.unsetConfiguration("orbonly", "showPrayerTimeRemaining");
	}

	@Subscribe(priority = 100)
	public void onProfileChanged(ProfileChanged event)
	{
		migratePrayerLabelConfig(configManager);
	}

	@Override
	protected void startUp()
	{
		migratePrayerLabelConfig(configManager);
		active = true;
		scriptDepth = 0;
		nativeStateReleased = false;
		overlayManager.add(valueOverlay);
		for (OrbType orb : OrbType.values())
		{
			OrbLabelOverlay label = new OrbLabelOverlay(config, uiState, valueOverlay, this, orb);
			labelOverlays.add(label);
			overlayManager.add(label);
		}
		clientThread.invokeLater(() ->
		{
			if (active)
			{
				poisonTimer.start();
				prayerHelper.start();
				prepare();
			}
		});
	}

	@Override
	protected void shutDown()
	{
		active = false;
		overlayManager.remove(valueOverlay);
		labelOverlays.forEach(overlayManager::remove);
		labelOverlays.clear();
		clientThread.invokeLater(() ->
		{
			if (!active)
			{
				uiState.restore();
				poisonTimer.reset();
				prayerHelper.reset();
			}
		});
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		poisonTimer.onGameStateChanged(event.getGameState());
		prepare();
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		poisonTimer.onGameTick();
		prayerHelper.onGameTick();
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (event.getContainerId() == InventoryID.WORN || event.getContainerId() == InventoryID.INV)
		{
			prayerHelper.updateEquipment();
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		if (event.getVarpId() == VarPlayerID.POISON)
		{
			poisonTimer.refreshState();
		}
	}

	@Subscribe
	public void onHitsplatApplied(HitsplatApplied event)
	{
		poisonTimer.onHitsplat(event);
	}

	@Subscribe
	public void onBeforeRender(BeforeRender event)
	{
		// Recover at the frame boundary if a script-finished event was missed.
		if (scriptDepth != 0)
		{
			scriptDepth = 0;
			nativeStateReleased = false;
		}
		prepare();
	}

	@Subscribe(priority = 100)
	public void onScriptPreFired(ScriptPreFired event)
	{
		scriptDepth++;
		if (!nativeStateReleased && affectsOrbs(event))
		{
			uiState.releaseForNativeUpdates();
			nativeStateReleased = true;
		}
	}

	@Subscribe(priority = -100)
	public void onScriptPostFired(ScriptPostFired event)
	{
		scriptDepth = Math.max(0, scriptDepth - 1);
		if (scriptDepth == 0 && nativeStateReleased)
		{
			nativeStateReleased = false;
			prepare();
		}
	}

	private boolean affectsOrbs(ScriptPreFired event)
	{
		int scriptId = event.getScriptId();
		if (scriptId == ScriptID.TOPLEVEL_REDRAW || scriptId == ScriptID.TOPLEVEL_RESIZE_CUSTOMISE
			|| scriptId == ScriptID.ORBS_UPDATE_RUNENERGY)
		{
			return true;
		}
		ScriptEvent script = event.getScriptEvent();
		Widget source = script == null ? null : script.getSource();
		if (source == null)
		{
			return false;
		}
		int group = source.getId() >>> 16;
		return group == InterfaceID.ORBS || group == InterfaceID.ORBS_NOMAP;
	}

	@Subscribe(priority = -20)
	public void onPostMenuSort(PostMenuSort event)
	{
		if (active)
		{
			interactions.updateMenu();
		}
	}

	private void prepare()
	{
		if (active && scriptDepth == 0)
		{
			uiState.prepare();
		}
	}
}
