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

import java.lang.reflect.Field;
import net.runelite.api.ScriptID;
import net.runelite.api.events.BeforeRender;
import net.runelite.api.events.ScriptPreFired;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class OrbOnlyPluginTest
{
	private OrbTestContext context;
	private OrbTestContext.OrbWidgets prayer;

	@Before
	public void setUp()
	{
		context = new OrbTestContext();
		prayer = context.prayer(false);
	}

	@Test
	public void recoversWhenScriptPostEventIsMissedAtFrameBoundary() throws Exception
	{
		OrbOnlyPlugin plugin = new OrbOnlyPlugin();
		setField(plugin, "uiState", context.ui);
		setBooleanField(plugin, "active", true);

		context.ui.prepare();
		plugin.onScriptPreFired(new ScriptPreFired(ScriptID.TOPLEVEL_REDRAW));
		assertFalse(prayer.value.isSelfHidden());
		assertFalse(prayer.button.isSelfHidden());

		plugin.onBeforeRender(new BeforeRender());

		assertTrue(prayer.value.isSelfHidden());
		assertTrue(prayer.button.isSelfHidden());
	}

	private static void setField(Object target, String name, Object value) throws ReflectiveOperationException
	{
		Field field = OrbOnlyPlugin.class.getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}

	private static void setBooleanField(Object target, String name, boolean value) throws ReflectiveOperationException
	{
		Field field = OrbOnlyPlugin.class.getDeclaredField(name);
		field.setAccessible(true);
		field.setBoolean(target, value);
	}
}
