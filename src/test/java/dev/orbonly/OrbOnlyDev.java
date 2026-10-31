package dev.orbonly;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public final class OrbOnlyDev
{
	private OrbOnlyDev()
	{
	}

	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(OrbOnlyPlugin.class);
		RuneLite.main(args);
	}
}
