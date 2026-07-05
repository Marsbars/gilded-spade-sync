package com.gildedspade.sync;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class GildedSpadeSyncPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(GildedSpadeSyncPlugin.class);
		RuneLite.main(args);
	}
}
