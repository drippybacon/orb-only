/*
 * Copyright (c) 2026, drippybacon
 * Copyright (c) 2017, Adam <Adam@sigterm.info>
 * Copyright (c) 2018, Raqes <j.raqes@gmail.com>
 * Copyright (c) 2018, Ethan <https://github.com/shmeeps>
 * Copyright (c) 2018, Tomas Slusny <slusnucky@gmail.com>
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
/* Prayer display and calculations adapted from RuneLite's Prayer plugin.
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
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStats;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.prayer.PrayerConfig;
import net.runelite.client.plugins.prayer.PrayerFlickLocation;
import net.runelite.client.plugins.prayer.PrayerPlugin;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;
import net.runelite.client.util.ColorUtil;

@Singleton
final class OrbPrayerHelper
{
	private static final Prayer[] PRAYERS = Prayer.values();
	private static final long GAME_TICK_NANOS = 600_000_000L;

	private final Client client;
	private final ItemManager itemManager;
	private final PluginManager pluginManager;
	private final PrayerConfig prayerConfig;
	private final TooltipManager tooltipManager;
	private PrayerPlugin corePrayerPlugin;
	private long lastTickNanos;
	private int prayerBonus;
	private int restoreAmount;
	private long dosePulseStartNanos;
	private boolean restartDosePulse = true;

	@Inject
	OrbPrayerHelper(Client client, ItemManager itemManager, PluginManager pluginManager,
		ConfigManager configManager, TooltipManager tooltipManager)
	{
		this.client = client;
		this.itemManager = itemManager;
		this.pluginManager = pluginManager;
		this.prayerConfig = configManager.getConfig(PrayerConfig.class);
		this.tooltipManager = tooltipManager;
	}

	void start()
	{
		lastTickNanos = System.nanoTime();
		dosePulseStartNanos = lastTickNanos;
		restartDosePulse = true;
		updateEquipment();
	}

	void reset()
	{
		lastTickNanos = 0;
		prayerBonus = 0;
		restoreAmount = 0;
		dosePulseStartNanos = 0;
		restartDosePulse = true;
		corePrayerPlugin = null;
	}

	void onGameTick()
	{
		lastTickNanos = System.nanoTime();
		if (prayerConfig.showPrayerDoseIndicator())
		{
			if (restartDosePulse)
			{
				dosePulseStartNanos = lastTickNanos;
			}
			restartDosePulse = !restartDosePulse;
		}
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
		updateRestoreAmount(equipment);
	}

	private void updateRestoreAmount(ItemContainer equipment)
	{
		boolean prayerPotion = false;
		boolean superRestore = false;
		boolean sanfew = false;
		boolean wrench = false;
		ItemContainer inventory = client.getItemContainer(InventoryID.INV);
		if (inventory != null)
		{
			for (Item item : inventory.getItems())
			{
				switch (restoreType(item.getId()))
				{
					case 1:
						superRestore = true;
						break;
					case 2:
						prayerPotion = true;
						break;
					case 3:
						sanfew = true;
						break;
					case 4:
						wrench = true;
						break;
					default:
						break;
				}
			}
		}
		if (!wrench && equipment != null)
		{
			for (Item item : equipment.getItems())
			{
				wrench |= restoreType(item.getId()) == 4;
			}
		}
		int level = client.getRealSkillLevel(Skill.PRAYER);
		int amount = 0;
		if (prayerPotion)
		{
			amount = 7 + (int) Math.floor(level * (wrench ? 0.27 : 0.25));
		}
		if (superRestore)
		{
			amount = Math.max(amount, 8 + (int) Math.floor(level * (wrench ? 0.27 : 0.25)));
		}
		if (sanfew)
		{
			amount = Math.max(amount, 4 + (int) Math.floor(level * (wrench ? 0.32 : 0.30)));
		}
		restoreAmount = amount;
	}

	String label(boolean showTime, boolean compact)
	{
		int points = Math.max(0, client.getBoostedSkillLevel(Skill.PRAYER));
		if (showTime && points > 0)
		{
			String time = estimatedTime(compact);
			if (time != null)
			{
				return time;
			}
		}
		// Read points directly, since the core plugin may replace PRAYER_TEXT.
		return Integer.toString(points);
	}

	private String estimatedTime(boolean compact)
	{
		int drain = totalDrainEffect();
		if (drain <= 0)
		{
			return null;
		}
		double resistance = Math.max(1, 2 * prayerBonus + 60);
		long seconds = (long) (Math.max(0, client.getBoostedSkillLevel(Skill.PRAYER)) * 0.6 * resistance / drain);
		return formatTime(seconds, compact);
	}

	void drawDoseAndTooltip(Graphics2D graphics, Rectangle circle, boolean timeLabel, boolean hovered)
	{
		if (!corePrayerActive())
		{
			return;
		}
		int points = client.getBoostedSkillLevel(Skill.PRAYER);
		if (hovered && prayerConfig.showPrayerStatistics())
		{
			String time = estimatedTime(false);
			String information = timeLabel && time != null
				? "Prayer points: " + points
				: "Time remaining: " + (time == null ? "N/A" : time);
			tooltipManager.add(new Tooltip(information + "</br>Prayer bonus: " + prayerBonus));
		}
		int missing = client.getRealSkillLevel(Skill.PRAYER) - points;
		if (!prayerConfig.showPrayerDoseIndicator() || restoreAmount <= 0 || missing < restoreAmount)
		{
			return;
		}
		Color start = prayerConfig.prayerDoseOrbStartColor();
		Color end = new Color((int) (start.getRed() * 0.36078), (int) (start.getGreen() * 0.36078),
			(int) (start.getBlue() * 0.36078), start.getAlpha());
		double progress = Math.max(0, Math.min(1, (System.nanoTime() - dosePulseStartNanos)
			/ (2.0 * GAME_TICK_NANOS)));
		Color pulse = ColorUtil.colorLerp(start, end, Math.sin(progress * Math.PI));
		OrbValueOverlay.drawActiveHighlight(graphics, circle, pulse);
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
			// Clip to our circle; the core overlay uses the hidden rectangular button.
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
			// Aliases share activation bits; count only the unlocked/LMS variant.
			if ((prayer == Prayer.EAGLE_EYE && deadeye) || (prayer == Prayer.DEADEYE && !deadeye)
				|| (prayer == Prayer.MYSTIC_MIGHT && vigour) || (prayer == Prayer.MYSTIC_VIGOUR && !vigour)
				|| (prayer == Prayer.PIETY && zeal) || (prayer == Prayer.ZEAL && !zeal))
			{
				continue;
			}
			int effect = drainEffect(prayer);
			if (effect <= 0)
			{
				// Fall back to points if an active prayer has no known drain rate.
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

	private static int restoreType(int itemId)
	{
		switch (itemId)
		{
			case ItemID._4DOSE2RESTORE:
			case ItemID._3DOSE2RESTORE:
			case ItemID._2DOSE2RESTORE:
			case ItemID._1DOSE2RESTORE:
			case ItemID.BLIGHTED_4DOSE2RESTORE:
			case ItemID.BLIGHTED_3DOSE2RESTORE:
			case ItemID.BLIGHTED_2DOSE2RESTORE:
			case ItemID.BLIGHTED_1DOSE2RESTORE:
				return 1;
			case ItemID._4DOSEPRAYERRESTORE:
			case ItemID._3DOSEPRAYERRESTORE:
			case ItemID._2DOSEPRAYERRESTORE:
			case ItemID._1DOSEPRAYERRESTORE:
			case ItemID.GAUNTLET_POTION_4:
			case ItemID.GAUNTLET_POTION_3:
			case ItemID.GAUNTLET_POTION_2:
			case ItemID.GAUNTLET_POTION_1:
				return 2;
			case ItemID.SANFEW_SALVE_4_DOSE:
			case ItemID.SANFEW_SALVE_3_DOSE:
			case ItemID.SANFEW_SALVE_2_DOSE:
			case ItemID.SANFEW_SALVE_1_DOSE:
				return 3;
			case ItemID.SKILLCAPE_PRAYER:
			case ItemID.SKILLCAPE_PRAYER_TRIMMED:
			case ItemID.SKILLCAPE_MAX:
			case ItemID.SKILLCAPE_MAX_WORN:
			case ItemID.DEAL_WRENCH_BLESSED:
			case ItemID.NZONE_ROTG:
				return 4;
			default:
				return 0;
		}
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
