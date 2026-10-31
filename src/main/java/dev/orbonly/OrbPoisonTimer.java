/*
 * Copyright (c) 2026, drippybacon
 * Copyright (c) 2026, corhen
 * Copyright (c) 2026, Jin-Jiyunsun
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
/* Poison/venom event tracking adapted from Poison Ring.
 * See THIRD-PARTY-NOTICES.md for attribution and license. */
package dev.orbonly;

import java.util.function.LongSupplier;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.HitsplatID;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.gameval.VarPlayerID;

/** Client-thread-only countdown, resynchronised by observed poison/venom hits. */
@Singleton
final class OrbPoisonTimer
{
	private static final int DAMAGE_INTERVAL_TICKS = 30;
	private static final int VENOM_THRESHOLD = 1_000_000;
	private static final long GAME_TICK_NANOS = 600_000_000L;

	private final Client client;
	private final LongSupplier nanoTime;
	private int poisonValue;
	private int ticksUntilDamage;
	private boolean pendingSync;
	private long lastTickNanos;

	@Inject
	private OrbPoisonTimer(Client client)
	{
		this(client, System::nanoTime);
	}

	OrbPoisonTimer(Client client, LongSupplier nanoTime)
	{
		this.client = client;
		this.nanoTime = nanoTime;
	}

	void start()
	{
		reset();
		refreshState();
	}

	void reset()
	{
		poisonValue = 0;
		ticksUntilDamage = 0;
		pendingSync = false;
		lastTickNanos = 0;
	}

	void refreshState()
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}
		int current = client.getVarpValue(VarPlayerID.POISON);
		if (current <= 0)
		{
			// Zero is healthy; negative values represent immunity.
			reset();
			return;
		}
		boolean newCycle = poisonValue <= 0 || (current >= VENOM_THRESHOLD && !venomed());
		poisonValue = current;
		if (newCycle)
		{
			sync();
		}
	}

	void onGameTick()
	{
		refreshState();
		if (!active())
		{
			return;
		}
		if (pendingSync)
		{
			// Count the hitsplat's tick as the start of the new interval.
			ticksUntilDamage = DAMAGE_INTERVAL_TICKS;
			pendingSync = false;
		}
		else if (ticksUntilDamage > 0)
		{
			ticksUntilDamage--;
		}
		lastTickNanos = nanoTime.getAsLong();
	}

	void onHitsplat(HitsplatApplied event)
	{
		if (event.getActor() != client.getLocalPlayer())
		{
			return;
		}
		int type = event.getHitsplat().getHitsplatType();
		if (type == HitsplatID.POISON || type == HitsplatID.VENOM)
		{
			refreshState();
			if (active())
			{
				sync();
			}
		}
	}

	void onGameStateChanged(GameState state)
	{
		switch (state)
		{
			case LOGIN_SCREEN:
			case LOGIN_SCREEN_AUTHENTICATOR:
			case LOGGING_IN:
			case HOPPING:
			case CONNECTION_LOST:
				reset();
				break;
			case LOGGED_IN:
				refreshState();
				break;
			default:
				// Region loading does not restart an existing damage cycle.
				break;
		}
	}

	boolean active()
	{
		return poisonValue > 0;
	}

	boolean venomed()
	{
		return poisonValue >= VENOM_THRESHOLD;
	}

	double remainingFraction()
	{
		if (!active())
		{
			return 0;
		}
		if (pendingSync)
		{
			return 1;
		}
		double subTick = Math.min(1, Math.max(0, (nanoTime.getAsLong() - lastTickNanos) / (double) GAME_TICK_NANOS));
		return Math.max(0, Math.min(1, (ticksUntilDamage - subTick) / DAMAGE_INTERVAL_TICKS));
	}

	private void sync()
	{
		ticksUntilDamage = DAMAGE_INTERVAL_TICKS;
		pendingSync = true;
		lastTickNanos = nanoTime.getAsLong();
	}
}
