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
import net.runelite.api.Client;
import net.runelite.api.Prayer;
import net.runelite.api.VarPlayer;
import net.runelite.api.Varbits;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OrbValueOverlayTest
{
	private final Client client = mock(Client.class);

	private OrbValueOverlay overlay(OrbOnlyConfig config)
	{
		// Colour-only tests do not need render or interaction helpers.
		return new OrbValueOverlay(client, config, new OrbOnlyPlugin(), null, null, null, null, null);
	}

	@Test
	public void mainhandAndOffhandSpecialStatesUseActiveColour()
	{
		OrbOnlyConfig config = new OrbOnlyConfig()
		{
		};
		OrbValueOverlay overlay = overlay(config);
		for (int state : new int[]{0, 1, 2})
		{
			when(client.getVarpValue(VarPlayer.SPECIAL_ATTACK_ENABLED)).thenReturn(state);
			assertEquals("Native special state " + state,
				state > 0 ? config.activeSpecialAttackColor() : config.labelColor(),
				overlay.valueColor(OrbType.SPECIAL_ATTACK));
		}
	}

	@Test
	public void specialHighlightCanBeDisabled()
	{
		OrbOnlyConfig config = new OrbOnlyConfig()
		{
			@Override
			public boolean highlightActiveSpecialAttack()
			{
				return false;
			}
		};
		when(client.getVarpValue(VarPlayer.SPECIAL_ATTACK_ENABLED)).thenReturn(2);
		assertEquals(Color.WHITE, overlay(config).valueColor(OrbType.SPECIAL_ATTACK));
	}

	@Test
	public void individualAndQuickPrayersUseActiveColour()
	{
		OrbOnlyConfig config = new OrbOnlyConfig()
		{
		};
		OrbValueOverlay overlay = overlay(config);
		assertEquals(config.labelColor(), overlay.valueColor(OrbType.PRAYER));
		when(client.isPrayerActive(Prayer.PROTECT_FROM_MELEE)).thenReturn(true);
		assertEquals(config.activePrayerColor(), overlay.valueColor(OrbType.PRAYER));
		when(client.isPrayerActive(Prayer.PROTECT_FROM_MELEE)).thenReturn(false);
		when(client.getVarbitValue(Varbits.QUICK_PRAYER)).thenReturn(1);
		assertEquals(config.activePrayerColor(), overlay.valueColor(OrbType.PRAYER));
	}
}
