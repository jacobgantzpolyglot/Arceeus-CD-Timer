package com.arceuusbuffoverlay;

import com.google.inject.Provides;
import java.util.EnumMap;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.MenuAction;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
    name = "Arceuus Buff Overlay",
    description = "Shows Arceuus buff spell timers over the spellbook during combat.",
    tags = {"arceuus", "spellbook", "timer", "overlay", "death charge", "mark of darkness", "greater corruption"}
)
public class ArceuusBuffOverlayPlugin extends Plugin
{
    private static final double SECONDS_PER_TICK = 0.6;

    @Inject
    private Client client;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private ArceuusBuffOverlay overlay;

    @Inject
    private ArceuusBuffOverlayConfig config;

    private final EnumMap<TrackedSpell, Integer> expirationTicks = new EnumMap<>(TrackedSpell.class);

    private int lastCombatTick = -1000;
    private TrackedSpell selectedTargetSpell;
    private int selectedTargetSpellTick = -1000;

    @Provides
    ArceuusBuffOverlayConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(ArceuusBuffOverlayConfig.class);
    }

    @Override
    protected void startUp()
    {
        overlayManager.add(overlay);
    }

    @Override
    protected void shutDown()
    {
        overlayManager.remove(overlay);
        expirationTicks.clear();
        selectedTargetSpell = null;
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        if (client.getGameState() != GameState.LOGGED_IN)
        {
            return;
        }

        Player local = client.getLocalPlayer();
        if (local != null && isCombatDetected(local))
        {
            lastCombatTick = client.getTickCount();
        }

        // Drop stale selected-target spell state after 10 seconds.
        if (selectedTargetSpell != null && client.getTickCount() - selectedTargetSpellTick > secondsToTicks(10))
        {
            selectedTargetSpell = null;
        }
    }

    @Subscribe
    public void onChatMessage(ChatMessage event)
    {
        String message = TrackedSpell.stripTags(event.getMessage()).toLowerCase();

        // If you try to cast Greater Corruption while it is still unavailable,
        // OSRS sends this message. Keep the spell blue instead of red.
        if (message.contains("can only cast corruption spells"))
        {
            if (!isOnCooldown(TrackedSpell.GREATER_CORRUPTION))
            {
                expirationTicks.put(
                    TrackedSpell.GREATER_CORRUPTION,
                    client.getTickCount() + secondsToTicks(3)
                );
            }
            return;
        }

        // Death Charge success message.
        // Do NOT start/reset on: "You can only cast Death Charge once every 60 seconds."
        if (message.contains("upon the death of your next foe"))
        {
            startTimer(TrackedSpell.DEATH_CHARGE);
            return;
        }

        // Mark of Darkness success message.
        if (message.contains("placed a mark of darkness"))
        {
            startTimer(TrackedSpell.MARK_OF_DARKNESS);
            return;
        }

        // Optional cleanup if the game sends a fade/expire message.
        if (message.contains("mark of darkness") && (message.contains("fades") || message.contains("faded") || message.contains("worn off")))
        {
            expirationTicks.remove(TrackedSpell.MARK_OF_DARKNESS);
        }
    }

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event)
    {
        String option = TrackedSpell.stripTags(event.getMenuOption());
        String target = TrackedSpell.stripTags(event.getMenuTarget());
        String combined = option + " " + target;

        TrackedSpell clickedSpell = TrackedSpell.fromText(combined);
        MenuAction action = event.getMenuAction();

        if (clickedSpell == TrackedSpell.GREATER_CORRUPTION)
        {
            // Greater Corruption is target-based. First click usually selects the spell;
            // the second click on NPC/player actually uses it.
            selectedTargetSpell = TrackedSpell.GREATER_CORRUPTION;
            selectedTargetSpellTick = client.getTickCount();

            if (isTargetAction(action))
            {
                if (!isOnCooldown(TrackedSpell.GREATER_CORRUPTION))
                {
                    startTimer(TrackedSpell.GREATER_CORRUPTION);
                }
                selectedTargetSpell = null;
            }

            return;
        }

        if (clickedSpell == TrackedSpell.DEATH_CHARGE || clickedSpell == TrackedSpell.MARK_OF_DARKNESS)
        {
            // Do not start these timers on click.
            // Wait for the real in-game success chat message instead.
            return;
        }

        if (selectedTargetSpell != null && isTargetAction(action))
        {
            if (!isOnCooldown(selectedTargetSpell))
            {
                startTimer(selectedTargetSpell);
            }
            selectedTargetSpell = null;
        }
    }

    private boolean isCombatDetected(Player local)
    {
        if (local.getInteracting() != null)
        {
            return true;
        }

        // Some combat situations drop your local interacting target between attacks.
        // This catches mobs that are still targeting you, so expired Death Charge /
        // Mark of Darkness can turn red while you are still on the same fight.
        for (NPC npc : client.getNpcs())
        {
            if (npc != null && npc.getInteracting() == local)
            {
                return true;
            }
        }

        return false;
    }

    boolean isInCombat()
    {
        return client.getGameState() == GameState.LOGGED_IN
            && client.getTickCount() - lastCombatTick <= config.combatGraceTicks();
    }

    boolean isActive(TrackedSpell spell)
    {
        Integer expires = expirationTicks.get(spell);
        return expires != null && expires > client.getTickCount();
    }

    boolean isOnCooldown(TrackedSpell spell)
    {
        Integer expires = expirationTicks.get(spell);
        return expires != null && expires > client.getTickCount();
    }

    boolean shouldShowTimer(TrackedSpell spell)
    {
        // Greater Corruption should NOT show a countdown.
        // Its timer is treated as cooldown-only.
        if (spell == TrackedSpell.GREATER_CORRUPTION)
        {
            return false;
        }

        return isActive(spell);
    }

    boolean hasAnyActiveTimer()
    {
        for (TrackedSpell spell : TrackedSpell.values())
        {
            if (shouldShowTimer(spell))
            {
                return true;
            }
        }

        return false;
    }

    int remainingSeconds(TrackedSpell spell)
    {
        Integer expires = expirationTicks.get(spell);
        if (expires == null)
        {
            return 0;
        }

        int remainingTicks = expires - client.getTickCount();
        if (remainingTicks <= 0)
        {
            return 0;
        }

        return (int) Math.ceil(remainingTicks * SECONDS_PER_TICK);
    }

    private void startTimer(TrackedSpell spell)
    {
        int seconds = getSpellDurationSeconds(spell);
        expirationTicks.put(spell, client.getTickCount() + secondsToTicks(seconds));
    }

    private int getSpellDurationSeconds(TrackedSpell spell)
    {
        switch (spell)
        {
            case GREATER_CORRUPTION:
                // OSRS corruption spell cooldown is 30 seconds.
                // Use max() so old saved config values like 18 do not make it turn red too early.
                return Math.max(30, config.greaterCorruptionSeconds());

            case DEATH_CHARGE:
                return config.deathChargeSeconds();

            case MARK_OF_DARKNESS:
                int seconds = config.markOfDarknessSeconds();
                if (isPurgingStaffEquipped())
                {
                    seconds *= config.purgingStaffMultiplier();
                }
                return seconds;

            default:
                return 1;
        }
    }

    private int secondsToTicks(int seconds)
    {
        return (int) Math.ceil(seconds / SECONDS_PER_TICK);
    }

    private boolean isTargetAction(MenuAction action)
    {
        return action == MenuAction.WIDGET_TARGET_ON_NPC
            || action == MenuAction.WIDGET_TARGET_ON_PLAYER
            || action == MenuAction.WIDGET_TARGET_ON_GAME_OBJECT
            || action == MenuAction.WIDGET_TARGET_ON_GROUND_ITEM
            || action == MenuAction.WIDGET_TARGET_ON_WIDGET;
    }

    private boolean isPurgingStaffEquipped()
    {
        ItemContainer equipment = client.getItemContainer(InventoryID.WORN);
        if (equipment == null)
        {
            return false;
        }

        Item[] items = equipment.getItems();
        int weaponSlot = EquipmentInventorySlot.WEAPON.getSlotIdx();

        return items.length > weaponSlot
            && items[weaponSlot] != null
            && items[weaponSlot].getId() == ItemID.PURGING_STAFF;
    }
}
