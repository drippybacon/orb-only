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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.Collections;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Prayer;
import net.runelite.api.Skill;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemEquipmentStats;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStats;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.prayer.PrayerConfig;
import net.runelite.client.plugins.prayer.PrayerPlugin;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class OrbPrayerHelperTest
{
	private final Client client = mock(Client.class);
	private final ItemManager items = mock(ItemManager.class);
	private final PluginManager plugins = mock(PluginManager.class);
	private final ConfigManager configs = mock(ConfigManager.class);
	private final PrayerConfig config = mock(PrayerConfig.class);
	private final PrayerPlugin core = mock(PrayerPlugin.class);
	private final TooltipManager tooltips = mock(TooltipManager.class);
	private OrbPrayerHelper helper;

	@Before
	public void setUp()
	{
		when(configs.getConfig(PrayerConfig.class)).thenReturn(config);
		when(plugins.getPlugins()).thenReturn(Collections.singletonList(core));
		when(plugins.isPluginActive(core)).thenReturn(true);
		when(client.getRealSkillLevel(Skill.PRAYER)).thenReturn(99);
		when(client.getBoostedSkillLevel(Skill.PRAYER)).thenReturn(60);
		when(config.showPrayerDoseIndicator()).thenReturn(true);
		when(config.prayerDoseOrbStartColor()).thenReturn(Color.CYAN);
		helper = new OrbPrayerHelper(client, items, plugins, configs, tooltips);
		helper.start();
	}

	private void inventory(int... ids)
	{
		ItemContainer inventory = container(ids);
		when(client.getItemContainer(InventoryID.INV)).thenReturn(inventory);
		helper.updateEquipment();
	}

	private ItemContainer container(int... ids)
	{
		Item[] contents = new Item[ids.length];
		for (int i = 0; i < ids.length; i++)
		{
			contents[i] = new Item(ids[i], 1);
		}
		ItemContainer container = mock(ItemContainer.class);
		when(container.getItems()).thenReturn(contents);
		return container;
	}

	private BufferedImage dose(boolean time, boolean hovered)
	{
		BufferedImage image = new BufferedImage(60, 60, BufferedImage.TYPE_INT_ARGB);
		Graphics2D draw = image.createGraphics();
		try
		{
			helper.drawDoseAndTooltip(draw, new Rectangle(10, 10, 34, 34), time, hovered);
		}
		finally
		{
			draw.dispose();
		}
		return image;
	}

	private static int pixels(BufferedImage image)
	{
		int count = 0;
		for (int y = 0; y < image.getHeight(); y++)
		{
			for (int x = 0; x < image.getWidth(); x++)
			{
				if ((image.getRGB(x, y) >>> 24) != 0)
				{
					count++;
				}
			}
		}
		return count;
	}

	@Test
	public void inactiveOrDisabledTimeLabelsUseActualPoints()
	{
		when(config.replaceOrbText()).thenReturn(true);
		assertEquals("60", helper.label(false, true));
		assertEquals("60", helper.label(true, true));
		when(client.isPrayerActive(Prayer.PROTECT_FROM_MELEE)).thenReturn(true);
		assertEquals("60", helper.label(false, true));
		when(client.getBoostedSkillLevel(Skill.PRAYER)).thenReturn(0);
		assertEquals("0", helper.label(true, true));
	}

	@Test
	public void estimateCombinesActivePrayersAndEquipmentBonus()
	{
		when(client.isPrayerActive(Prayer.PROTECT_FROM_MELEE)).thenReturn(true);
		assertEquals("3:00", helper.label(true, false));
		when(client.isPrayerActive(Prayer.PIETY)).thenReturn(true);
		assertEquals("1:00", helper.label(true, false));
		ItemEquipmentStats equipment = ItemEquipmentStats.builder().prayer(20).build();
		when(items.getItemStats(12345)).thenReturn(new ItemStats(true, 0, 0, equipment));
		ItemContainer equipmentItems = container(12345);
		when(client.getItemContainer(InventoryID.WORN)).thenReturn(equipmentItems);
		helper.updateEquipment();
		assertEquals("1:40", helper.label(true, false));
	}

	@Test
	public void upgradedPrayerAliasesAreNotDoubleCountedIncludingLms()
	{
		for (Prayer prayer : new Prayer[]{Prayer.EAGLE_EYE, Prayer.DEADEYE, Prayer.MYSTIC_MIGHT,
			Prayer.MYSTIC_VIGOUR, Prayer.PIETY, Prayer.ZEAL})
		{
			when(client.isPrayerActive(prayer)).thenReturn(true);
		}
		assertEquals("0:45", helper.label(true, false));
		when(client.getVarbitValue(VarbitID.PRAYER_DEADEYE_UNLOCKED)).thenReturn(1);
		when(client.getVarbitValue(VarbitID.PRAYER_MYSTIC_VIGOUR_UNLOCKED)).thenReturn(1);
		when(client.getVarbitValue(VarbitID.PRAYER_ZEAL_UNLOCKED)).thenReturn(1);
		assertEquals("0:45", helper.label(true, false));
		when(client.getVarbitValue(VarbitID.BR_INGAME)).thenReturn(1);
		assertEquals("0:45", helper.label(true, false));
	}

	@Test
	public void dosePulseRequiresEnoughMissingPointsForCarriedPotion()
	{
		for (int[] potion : new int[][]{
			{ItemID._4DOSEPRAYERRESTORE, 31}, {ItemID._4DOSE2RESTORE, 32},
			{ItemID.SANFEW_SALVE_4_DOSE, 33}, {ItemID.BLIGHTED_1DOSE2RESTORE, 32},
			{ItemID.GAUNTLET_POTION_1, 31}})
		{
			inventory(potion[0]);
			when(client.getBoostedSkillLevel(Skill.PRAYER)).thenReturn(99 - potion[1] + 1);
			assertEquals("Partial dose for item " + potion[0], 0, pixels(dose(false, false)));
			when(client.getBoostedSkillLevel(Skill.PRAYER)).thenReturn(99 - potion[1]);
			assertTrue("Full dose for item " + potion[0], pixels(dose(false, false)) > 0);
		}
	}

	@Test
	public void carriedAndEquippedWrenchEffectsChangeDoseThreshold()
	{
		for (boolean worn : new boolean[]{false, true})
		{
			if (worn)
			{
				ItemContainer equipment = container(ItemID.SKILLCAPE_PRAYER);
				when(client.getItemContainer(InventoryID.WORN)).thenReturn(equipment);
				inventory(ItemID._1DOSEPRAYERRESTORE);
			}
			else
			{
				inventory(ItemID._1DOSEPRAYERRESTORE, ItemID.DEAL_WRENCH_BLESSED);
			}
			when(client.getBoostedSkillLevel(Skill.PRAYER)).thenReturn(67);
			assertEquals(0, pixels(dose(false, false)));
			when(client.getBoostedSkillLevel(Skill.PRAYER)).thenReturn(66);
			assertTrue(pixels(dose(false, false)) > 0);
		}
	}

	@Test
	public void highestRestorationAmountDeterminesPulseAndUsesConfiguredColour()
	{
		inventory(ItemID._1DOSEPRAYERRESTORE, ItemID._1DOSE2RESTORE, ItemID.SANFEW_SALVE_1_DOSE);
		when(client.getBoostedSkillLevel(Skill.PRAYER)).thenReturn(67);
		assertEquals(0, pixels(dose(false, false)));
		when(client.getBoostedSkillLevel(Skill.PRAYER)).thenReturn(66);
		when(config.prayerDoseOrbStartColor()).thenReturn(Color.RED);
		BufferedImage image = dose(false, false);
		assertTrue(pixels(image) > 0);
		for (int y = 0; y < image.getHeight(); y++)
		{
			for (int x = 0; x < image.getWidth(); x++)
			{
				int pixel = image.getRGB(x, y);
				if ((pixel >>> 24) != 0)
				{
					assertTrue((pixel & 0xff0000) != 0);
					assertEquals(0, pixel & 0xffff);
				}
			}
		}
	}

	@Test
	public void pulseHonoursCoreSettingsAndPluginEnabledState()
	{
		inventory(ItemID._1DOSEPRAYERRESTORE);
		assertTrue(pixels(dose(false, false)) > 0);
		when(config.showPrayerDoseIndicator()).thenReturn(false);
		assertEquals(0, pixels(dose(false, false)));
		when(config.showPrayerDoseIndicator()).thenReturn(true);
		when(plugins.isPluginActive(core)).thenReturn(false);
		assertEquals(0, pixels(dose(false, true)));
		verify(tooltips, never()).add(any());
	}

	@Test
	public void statisticsTooltipShowsCounterpartToSelectedLabel()
	{
		when(config.showPrayerStatistics()).thenReturn(true);
		when(client.isPrayerActive(Prayer.PROTECT_FROM_MELEE)).thenReturn(true);
		dose(false, true);
		dose(true, true);
		ArgumentCaptor<Tooltip> tooltip = ArgumentCaptor.forClass(Tooltip.class);
		verify(tooltips, times(2)).add(tooltip.capture());
		assertEquals("Time remaining: 3:00</br>Prayer bonus: 0", tooltip.getAllValues().get(0).getText());
		assertEquals("Prayer points: 60</br>Prayer bonus: 0", tooltip.getAllValues().get(1).getText());
	}

	@Test
	public void statisticsSettingAndHoverAreRespected()
	{
		dose(false, true);
		when(config.showPrayerStatistics()).thenReturn(true);
		dose(false, false);
		verify(tooltips, never()).add(any());
	}

	@Test
	public void tickSweepIsClippedToOrbAndUsesConfiguredColour()
	{
		BufferedImage image = new BufferedImage(60, 60, BufferedImage.TYPE_INT_ARGB);
		Graphics2D draw = image.createGraphics();
		try
		{
			OrbPrayerHelper.drawTickSweep(draw, new Rectangle(10, 10, 34, 34), 0.5, Color.MAGENTA);
		}
		finally
		{
			draw.dispose();
		}
		assertTrue(pixels(image) > 0);
		for (int y = 0; y < 60; y++)
		{
			for (int x = 0; x < 60; x++)
			{
				if ((image.getRGB(x, y) >>> 24) != 0)
				{
					assertTrue(x >= 14 && x < 40 && y >= 14 && y < 40);
					assertEquals(Color.MAGENTA.getRGB(), image.getRGB(x, y));
				}
			}
		}
	}

	@Test
	public void timeFormattingHandlesCompactAndHourBoundaries()
	{
		assertEquals("0:00", OrbPrayerHelper.formatTime(-1, false));
		assertEquals("9:59", OrbPrayerHelper.formatTime(599, true));
		assertEquals("10m", OrbPrayerHelper.formatTime(600, true));
		assertEquals("1:00:00", OrbPrayerHelper.formatTime(3600, false));
	}
}
