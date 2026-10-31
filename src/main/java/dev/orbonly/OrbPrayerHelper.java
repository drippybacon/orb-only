/* Prayer timing and drain calculations adapted from RuneLite's Prayer plugin.
 * See THIRD-PARTY-NOTICES.md for attribution and license. */
package dev.orbonly;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Ellipse2D;
import java.util.Locale;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Prayer;
import net.runelite.api.Skill;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStats;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.prayer.PrayerConfig;
import net.runelite.client.plugins.prayer.PrayerFlickLocation;
import net.runelite.client.plugins.prayer.PrayerPlugin;

@Singleton
final class OrbPrayerHelper
{
	private static final Prayer[] PRAYERS = Prayer.values();
	private static final long GAME_TICK_NANOS = 600_000_000L;

	private final Client client;
	private final ItemManager itemManager;
	private final PluginManager pluginManager;
	private final PrayerConfig prayerConfig;
	private PrayerPlugin corePrayerPlugin;
	private long lastTickNanos;
	private int prayerBonus;

	@Inject
	private OrbPrayerHelper(Client client, ItemManager itemManager, PluginManager pluginManager,
		ConfigManager configManager)
	{
		this.client = client;
		this.itemManager = itemManager;
		this.pluginManager = pluginManager;
		this.prayerConfig = configManager.getConfig(PrayerConfig.class);
	}

	void start()
	{
		lastTickNanos = System.nanoTime();
		updateEquipment();
	}

	void reset()
	{
		lastTickNanos = 0;
		prayerBonus = 0;
		corePrayerPlugin = null;
	}

	void onGameTick()
	{
		lastTickNanos = System.nanoTime();
		updateEquipment();
	}

	void updateEquipment()
	{
		int bonus = 0;
		ItemContainer equipment = client.getItemContainer(InventoryID.WORN);
		if (equipment != null)
		{
			for (Item item : equipment.getItems())
			{
				if (item.getId() < 0)
				{
					continue;
				}
				ItemStats stats = itemManager.getItemStats(item.getId());
				if (stats != null && stats.getEquipment() != null)
				{
					bonus += stats.getEquipment().getPrayer();
				}
			}
		}
		prayerBonus = bonus;
	}

	String label(boolean showTime, boolean compact)
	{
		int points = Math.max(0, client.getBoostedSkillLevel(Skill.PRAYER));
		if (showTime && points > 0)
		{
			int drain = totalDrainEffect();
			if (drain > 0)
			{
				// Same estimate as RuneLite Prayer: current points, active prayers,
				// and the prayer bonus of equipped items.
				double resistance = Math.max(1, 2 * prayerBonus + 60);
				long seconds = (long) (points * 0.6 * resistance / drain);
				return formatTime(seconds, compact);
			}
		}
		// Read points directly, since the core plugin may replace PRAYER_TEXT.
		return Integer.toString(points);
	}

	void drawTickHelper(Graphics2D graphics, Rectangle circle)
	{
		PrayerFlickLocation location = prayerConfig.prayerFlickLocation();
		if ((location != PrayerFlickLocation.PRAYER_ORB && location != PrayerFlickLocation.BOTH)
			|| !corePrayerActive() || (!prayerConfig.prayerFlickAlwaysOn() && !anyPrayerActive())
			|| lastTickNanos == 0)
		{
			return;
		}
		double phase = (Math.max(0, System.nanoTime() - lastTickNanos) % GAME_TICK_NANOS)
			/ (double) GAME_TICK_NANOS;
		drawTickSweep(graphics, circle, phase, prayerConfig.prayerFlickColor());
	}

	static void drawTickSweep(Graphics2D graphics, Rectangle circle, double phase, Color color)
	{
		Graphics2D draw = (Graphics2D) graphics.create();
		try
		{
			// Fit the sweep inside this plugin's circle instead of the hidden,
			// rectangular native button used by the core overlay.
			int padding = Math.max(2, (int) Math.round(circle.width * 4 / 34.0));
			Rectangle inner = new Rectangle(circle.x + padding, circle.y + padding,
				circle.width - padding * 2, circle.height - padding * 2);
			if (inner.isEmpty())
			{
				return;
			}
			double t = Math.max(0, Math.min(1, phase)) * Math.PI;
			int x = inner.x + (int) Math.round((1 - Math.cos(t)) * (inner.width - 1) / 2.0);
			int height = (int) Math.round(Math.sin(t) * inner.height);
			int y = inner.y + (inner.height - height) / 2;
			draw.clip(new Ellipse2D.Double(inner.x, inner.y, inner.width, inner.height));
			draw.setColor(color);
			draw.fillRect(x, y, 1, height);
		}
		finally
		{
			draw.dispose();
		}
	}

	private boolean corePrayerActive()
	{
		if (corePrayerPlugin != null && pluginManager.isPluginActive(corePrayerPlugin))
		{
			return true;
		}
		for (Plugin plugin : pluginManager.getPlugins())
		{
			if (plugin instanceof PrayerPlugin)
			{
				corePrayerPlugin = (PrayerPlugin) plugin;
				return pluginManager.isPluginActive(plugin);
			}
		}
		return false;
	}

	private boolean anyPrayerActive()
	{
		for (Prayer prayer : PRAYERS)
		{
			if (client.isPrayerActive(prayer))
			{
				return true;
			}
		}
		return false;
	}

	private int totalDrainEffect()
	{
		boolean inLms = client.getVarbitValue(VarbitID.BR_INGAME) != 0;
		boolean deadeye = !inLms && client.getVarbitValue(VarbitID.PRAYER_DEADEYE_UNLOCKED) != 0;
		boolean vigour = !inLms && client.getVarbitValue(VarbitID.PRAYER_MYSTIC_VIGOUR_UNLOCKED) != 0;
		boolean zeal = inLms || client.getVarbitValue(VarbitID.PRAYER_ZEAL_UNLOCKED) != 0;
		int total = 0;
		for (Prayer prayer : PRAYERS)
		{
			if (!client.isPrayerActive(prayer))
			{
				continue;
			}
			// Match the core plugin's upgraded-prayer and LMS selection rules.
			if ((prayer == Prayer.EAGLE_EYE && deadeye) || (prayer == Prayer.DEADEYE && !deadeye)
				|| (prayer == Prayer.MYSTIC_MIGHT && vigour) || (prayer == Prayer.MYSTIC_VIGOUR && !vigour)
				|| (prayer == Prayer.PIETY && zeal) || (prayer == Prayer.ZEAL && !zeal))
			{
				continue;
			}
			int effect = drainEffect(prayer);
			if (effect <= 0)
			{
				// Do not display a misleading estimate for an unknown future prayer.
				return 0;
			}
			total += effect;
		}
		return total;
	}

	static String formatTime(long seconds, boolean compact)
	{
		seconds = Math.max(0, seconds);
		if (compact && seconds >= 600)
		{
			return (seconds / 60) + "m";
		}
		if (seconds >= 3600)
		{
			return String.format(Locale.ROOT, "%d:%02d:%02d", seconds / 3600, seconds / 60 % 60, seconds % 60);
		}
		return String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
	}

	private static int drainEffect(Prayer prayer)
	{
		switch (prayer)
		{
			case THICK_SKIN:
			case BURST_OF_STRENGTH:
			case CLARITY_OF_THOUGHT:
			case SHARP_EYE:
			case MYSTIC_WILL:
			case RAPID_RESTORE:
			case RP_RUINOUS_GRACE:
				return 1;
			case RAPID_HEAL:
			case PROTECT_ITEM:
			case PRESERVE:
			case RP_BERSERKER:
				return 2;
			case RETRIBUTION:
			case RP_WRATH:
				return 3;
			case RP_REJUVENATION:
				return 4;
			case ROCK_SKIN:
			case SUPERHUMAN_STRENGTH:
			case IMPROVED_REFLEXES:
			case HAWK_EYE:
			case MYSTIC_LORE:
			case REDEMPTION:
				return 6;
			case RP_VINDICATION:
				return 9;
			case STEEL_SKIN:
			case ULTIMATE_STRENGTH:
			case INCREDIBLE_REFLEXES:
			case PROTECT_FROM_MAGIC:
			case PROTECT_FROM_MISSILES:
			case PROTECT_FROM_MELEE:
			case EAGLE_EYE:
			case MYSTIC_MIGHT:
			case CHIVALRY:
			case DEADEYE:
			case MYSTIC_VIGOUR:
			case RP_METABOLISE:
			case RP_REBUKE:
				return 12;
			case RP_DAMPEN_MAGIC:
			case RP_DAMPEN_RANGED:
			case RP_DAMPEN_MELEE:
			case RP_FUMUS_VOW:
			case RP_UMBRA_VOW:
			case RP_CRUORS_VOW:
			case RP_GLACIES_VOW:
				return 14;
			case SMITE:
			case RP_ANCIENT_STRENGTH:
			case RP_ANCIENT_SIGHT:
			case RP_ANCIENT_WILL:
			case RP_PROTECT_ITEM:
			case RP_PURGE:
				return 18;
			case RP_TRINITAS:
				return 22;
			case PIETY:
			case ZEAL:
			case RIGOUR:
			case AUGURY:
				return 24;
			case RP_DECIMATE:
			case RP_ANNIHILATE:
			case RP_VAPORISE:
			case RP_INTENSIFY:
				return 28;
			default:
				return 0;
		}
	}
}
