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

import java.util.concurrent.atomic.AtomicLong;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Hitsplat;
import net.runelite.api.HitsplatID;
import net.runelite.api.Player;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.gameval.VarPlayerID;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OrbPoisonTimerTest
{
	private final Client client = mock(Client.class);
	private final Player localPlayer = mock(Player.class);
	private final AtomicLong clock = new AtomicLong(1_000_000_000L);
	private final OrbPoisonTimer timer = new OrbPoisonTimer(client, clock::get);

	@Before
	public void setUp()
	{
		when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
		when(client.getLocalPlayer()).thenReturn(localPlayer);
	}

	private void poison(int value)
	{
		when(client.getVarpValue(VarPlayerID.POISON)).thenReturn(value);
		timer.refreshState();
	}

	private void ticks(int count)
	{
		for (int i = 0; i < count; i++)
		{
			clock.addAndGet(600_000_000L);
			timer.onGameTick();
		}
	}

	private HitsplatApplied hitsplat(Player actor, int type)
	{
		Hitsplat hitsplat = mock(Hitsplat.class);
		when(hitsplat.getHitsplatType()).thenReturn(type);
		HitsplatApplied event = new HitsplatApplied();
		event.setActor(actor);
		event.setHitsplat(hitsplat);
		return event;
	}

	@Test
	public void countdownUsesTickBoundaryAndInterpolatesWithoutSleeping()
	{
		poison(10);
		assertEquals(1, timer.remainingFraction(), 0);
		ticks(1);
		assertEquals(1, timer.remainingFraction(), 0);
		ticks(15);
		assertEquals(0.5, timer.remainingFraction(), 0);
		clock.addAndGet(300_000_000L);
		assertEquals(14.5 / 30, timer.remainingFraction(), 0.000001);
		ticks(30);
		assertEquals(0, timer.remainingFraction(), 0);
	}

	@Test
	public void severityChangesDoNotResetButBecomingVenomedDoes()
	{
		poison(10);
		ticks(11);
		poison(8);
		assertEquals(20.0 / 30, timer.remainingFraction(), 0.000001);
		assertFalse(timer.venomed());
		poison(1_000_000);
		assertTrue(timer.venomed());
		assertEquals(1, timer.remainingFraction(), 0);
		ticks(6);
		poison(1_000_002);
		assertEquals(25.0 / 30, timer.remainingFraction(), 0.000001);
	}

	@Test
	public void onlyLocalPoisonOrVenomHitsplatsResynchronize()
	{
		poison(10);
		ticks(11);
		double before = timer.remainingFraction();
		timer.onHitsplat(hitsplat(mock(Player.class), HitsplatID.POISON));
		assertEquals(before, timer.remainingFraction(), 0);
		timer.onHitsplat(hitsplat(localPlayer, -1));
		assertEquals(before, timer.remainingFraction(), 0);
		timer.onHitsplat(hitsplat(localPlayer, HitsplatID.POISON));
		assertEquals(1, timer.remainingFraction(), 0);
		ticks(1);
		assertEquals(1, timer.remainingFraction(), 0);
	}

	@Test
	public void localVenomDamageResynchronizesExistingVenomCycle()
	{
		poison(1_000_002);
		ticks(15);
		timer.onHitsplat(hitsplat(localPlayer, HitsplatID.VENOM));
		assertTrue(timer.venomed());
		assertEquals(1, timer.remainingFraction(), 0);
	}

	@Test
	public void cureAndImmunityClearCountdown()
	{
		for (int value : new int[]{0, -1, -100})
		{
			poison(10);
			poison(value);
			assertFalse(timer.active());
			assertFalse(timer.venomed());
			assertEquals(0, timer.remainingFraction(), 0);
		}
	}

	@Test
	public void loadingPreservesCycleButSessionChangesClearIt()
	{
		poison(10);
		ticks(11);
		double before = timer.remainingFraction();
		timer.onGameStateChanged(GameState.LOADING);
		assertEquals(before, timer.remainingFraction(), 0);
		for (GameState state : new GameState[]{GameState.HOPPING, GameState.CONNECTION_LOST,
			GameState.LOGIN_SCREEN, GameState.LOGIN_SCREEN_AUTHENTICATOR, GameState.LOGGING_IN})
		{
			poison(10);
			timer.onGameStateChanged(state);
			assertFalse(state.name(), timer.active());
			timer.onGameStateChanged(GameState.LOGGED_IN);
			assertTrue(timer.active());
			assertEquals(1, timer.remainingFraction(), 0);
		}
	}
}
