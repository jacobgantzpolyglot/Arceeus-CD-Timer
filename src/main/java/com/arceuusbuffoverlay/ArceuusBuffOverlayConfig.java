package com.arceuusbuffoverlay;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup("arceuusbuffoverlay")
public interface ArceuusBuffOverlayConfig extends Config
{
    @Range(min = 1, max = 300)
    @ConfigItem(
        keyName = "greaterCorruptionSeconds",
        name = "Greater Corruption seconds",
        description = "Timer length for Greater Corruption. Default is 18 seconds."
    )
    default int greaterCorruptionSeconds()
    {
        return 18;
    }

    @Range(min = 1, max = 300)
    @ConfigItem(
        keyName = "deathChargeSeconds",
        name = "Death Charge seconds",
        description = "Timer length for Death Charge. Default is 60 seconds."
    )
    default int deathChargeSeconds()
    {
        return 60;
    }

    @Range(min = 1, max = 1800)
    @ConfigItem(
        keyName = "markOfDarknessSeconds",
        name = "Mark of Darkness seconds",
        description = "Timer length for Mark of Darkness without Purging staff. Default is 180 seconds."
    )
    default int markOfDarknessSeconds()
    {
        return 180;
    }

    @Range(min = 1, max = 10)
    @ConfigItem(
        keyName = "purgingStaffMultiplier",
        name = "Purging staff multiplier",
        description = "Multiplier applied to Mark of Darkness if Purging staff is equipped when cast."
    )
    default int purgingStaffMultiplier()
    {
        return 5;
    }

    @Range(min = 0, max = 20)
    @ConfigItem(
        keyName = "combatGraceTicks",
        name = "Combat grace ticks",
        description = "How many game ticks after interaction to keep showing combat overlays. Higher avoids flicker between attacks."
    )
    default int combatGraceTicks()
    {
        return 8;
    }

    @ConfigItem(
        keyName = "showOnlyOnSpellWidgets",
        name = "Only on spell widgets",
        description = "Only render on widgets whose text/name looks like the tracked spell names."
    )
    default boolean showOnlyOnSpellWidgets()
    {
        return true;
    }
    @ConfigItem(
        keyName = "highlightGreaterCorruptionReady",
        name = "Highlight Greater Corruption ready",
        description = "When enabled, Greater Corruption turns red in combat when it is ready to cast again. Default is off."
    )
    default boolean highlightGreaterCorruptionReady()
    {
        return false;
    }

}

