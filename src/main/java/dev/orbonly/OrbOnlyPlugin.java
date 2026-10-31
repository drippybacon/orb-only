package dev.orbonly;

import com.google.inject.Provides;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.events.BeforeRender;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.PostClientTick;
import net.runelite.api.events.PostMenuSort;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
	name = "Orb Only",
	description = "Compact hitpoints, prayer, run energy, and special attack orbs with customizable labels"
)
public class OrbOnlyPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private OrbOnlyConfig config;

	@Inject
	private ClientThread clientThread;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private OrbValueOverlay valueOverlay;

	@Inject
	private OrbBackgroundOverlay backgroundOverlay;

	@Inject
	private OrbUiState uiState;

	@Inject
	private OrbInteractions interactions;

	@Inject
	private OrbFrameImages frames;

	@Inject
	private OrbPoisonTimer poisonTimer;

	@Inject
	private OrbPrayerHelper prayerHelper;

	private int scriptDepth;
	private volatile boolean active;
	private final List<OrbLabelOverlay> labelOverlays = new ArrayList<>();

	@Provides
	OrbOnlyConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(OrbOnlyConfig.class);
	}

	@Override
	protected void startUp()
	{
		active = true;
		scriptDepth = 0;
		overlayManager.add(backgroundOverlay);
		overlayManager.add(valueOverlay);
		for (OrbType orb : OrbType.values())
		{
			OrbLabelOverlay label = new OrbLabelOverlay(client, config, uiState, valueOverlay, this, orb);
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
		overlayManager.remove(backgroundOverlay);
		overlayManager.remove(valueOverlay);
		labelOverlays.forEach(overlayManager::remove);
		labelOverlays.clear();
		clientThread.invokeLater(() ->
		{
			if (!active)
			{
				uiState.restore();
				frames.clear();
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
		if (event.getContainerId() == InventoryID.WORN)
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
		prepare();
	}

	@Subscribe(priority = -100)
	public void onClientTick(ClientTick event)
	{
		prepare();
	}

	@Subscribe(priority = -100)
	public void onPostClientTick(PostClientTick event)
	{
		prepare();
	}

	@Subscribe(priority = 100)
	public void onScriptPreFired(ScriptPreFired event)
	{
		if (scriptDepth++ == 0)
		{
			uiState.releaseForNativeUpdates();
		}
	}

	@Subscribe(priority = -100)
	public void onScriptPostFired(ScriptPostFired event)
	{
		scriptDepth = Math.max(0, scriptDepth - 1);
		prepare();
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
