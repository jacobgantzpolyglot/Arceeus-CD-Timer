package com.arceuusbuffoverlay;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class ArceuusBuffOverlayPluginTest
{
    public static void main(String[] args) throws Exception
    {
        ExternalPluginManager.loadBuiltin(ArceuusBuffOverlayPlugin.class);
        RuneLite.main(args);
    }
}
