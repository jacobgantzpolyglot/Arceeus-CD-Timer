package com.arceuusbuffoverlay;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.HashSet;
import java.util.Set;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

public class ArceuusBuffOverlay extends Overlay
{
    private static final Color MISSING_FILL = new Color(180, 0, 0, 75);
    private static final Color MISSING_BORDER = new Color(255, 40, 40, 210);
    private static final Color TIMER_FILL = new Color(0, 0, 0, 145);
    private static final Color TIMER_TEXT = Color.WHITE;
    private static final Color TIMER_SHADOW = Color.BLACK;
    private static final Color COOLDOWN_FILL = new Color(0, 25, 120, 95);
    private static final Color COOLDOWN_BORDER = new Color(40, 90, 255, 210);

    private final Client client;
    private final ArceuusBuffOverlayPlugin plugin;
    private final ArceuusBuffOverlayConfig config;

    @Inject
    ArceuusBuffOverlay(Client client, ArceuusBuffOverlayPlugin plugin, ArceuusBuffOverlayConfig config)
    {
        this.client = client;
        this.plugin = plugin;
        this.config = config;

        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        boolean inCombat = plugin.isInCombat();

        if (!inCombat && !plugin.hasAnyActiveTimer())
        {
            return null;
        }

        Widget[] roots = client.getWidgetRoots();
        if (roots == null)
        {
            return null;
        }

        Set<Integer> renderedWidgetIds = new HashSet<>();
        for (Widget root : roots)
        {
            renderRecursive(graphics, root, renderedWidgetIds, inCombat);
        }

        return null;
    }

    private void renderRecursive(Graphics2D graphics, Widget widget, Set<Integer> renderedWidgetIds, boolean inCombat)
    {
        if (widget == null || widget.isHidden())
        {
            return;
        }

        if (!renderedWidgetIds.contains(widget.getId()))
        {
            TrackedSpell spell = findSpell(widget);
            if (spell != null)
            {
                Rectangle bounds = widget.getBounds();
                if (bounds != null && bounds.width > 5 && bounds.height > 5)
                {
                    renderSpell(graphics, bounds, spell, inCombat);
                    renderedWidgetIds.add(widget.getId());
                }
            }
        }

        renderChildren(graphics, widget.getStaticChildren(), renderedWidgetIds, inCombat);
        renderChildren(graphics, widget.getDynamicChildren(), renderedWidgetIds, inCombat);
        renderChildren(graphics, widget.getNestedChildren(), renderedWidgetIds, inCombat);
    }

    private void renderChildren(Graphics2D graphics, Widget[] children, Set<Integer> renderedWidgetIds, boolean inCombat)
    {
        if (children == null)
        {
            return;
        }

        for (Widget child : children)
        {
            renderRecursive(graphics, child, renderedWidgetIds, inCombat);
        }
    }

    private TrackedSpell findSpell(Widget widget)
    {
        if (!config.showOnlyOnSpellWidgets())
        {
            return null;
        }

        Rectangle bounds = widget.getBounds();
        if (bounds == null || !isInBottomRightPanel(bounds))
        {
            return null;
        }

        TrackedSpell byName = TrackedSpell.fromText(widget.getName());
        if (byName != null)
        {
            return byName;
        }

        return TrackedSpell.fromText(widget.getText());
    }

    private boolean isInBottomRightPanel(Rectangle bounds)
    {
        if (client.getCanvas() == null)
        {
            return false;
        }

        int canvasWidth = client.getCanvas().getWidth();
        int canvasHeight = client.getCanvas().getHeight();

        // Only allow widgets in the lower-right interface area where the spellbook sits.
        // This prevents matching chatbox lines like "You have placed a Mark of Darkness upon yourself."
        int minX = canvasWidth - 260;
        int minY = canvasHeight - 360;

        return bounds.x >= minX && bounds.y >= minY;
    }

    private void renderSpell(Graphics2D graphics, Rectangle bounds, TrackedSpell spell, boolean inCombat)
    {
        if (plugin.shouldShowTimer(spell))
        {
            renderTimer(graphics, bounds, plugin.remainingSeconds(spell));
            return;
        }

        // Greater Corruption:
        // - On cooldown / cannot be cast: dark blue overlay
        // - Ready while in combat: red only if the setting is enabled
        if (spell == TrackedSpell.GREATER_CORRUPTION)
        {
            if (plugin.isOnCooldown(spell))
            {
                renderCooldown(graphics, bounds);
                return;
            }

            if (inCombat && config.highlightGreaterCorruptionReady())
            {
                renderMissing(graphics, bounds);
            }

            return;
        }

        // Death Charge / Mark of Darkness:
        // expired or not active while still in combat = red
        if (inCombat)
        {
            renderMissing(graphics, bounds);
        }
    }

    private void renderCooldown(Graphics2D graphics, Rectangle bounds)
    {
        graphics.setColor(COOLDOWN_FILL);
        graphics.fill(bounds);

        graphics.setColor(COOLDOWN_BORDER);
        graphics.setStroke(new BasicStroke(2));
        graphics.draw(bounds);
    }

    private void renderMissing(Graphics2D graphics, Rectangle bounds)
    {
        graphics.setColor(MISSING_FILL);
        graphics.fill(bounds);

        graphics.setColor(MISSING_BORDER);
        graphics.setStroke(new BasicStroke(2));
        graphics.draw(bounds);
    }

    private void renderTimer(Graphics2D graphics, Rectangle bounds, int seconds)
    {
        String text = seconds + "s";
        FontMetrics metrics = graphics.getFontMetrics();

        int paddingX = 3;
        int paddingY = 2;
        int textWidth = metrics.stringWidth(text);
        int textHeight = metrics.getAscent();

        int boxWidth = textWidth + paddingX * 2;
        int boxHeight = textHeight + paddingY * 2;
        int boxX = bounds.x + (bounds.width - boxWidth) / 2;
        int boxY = bounds.y + bounds.height - boxHeight - 2;

        graphics.setColor(TIMER_FILL);
        graphics.fillRoundRect(boxX, boxY, boxWidth, boxHeight, 6, 6);

        int textX = boxX + paddingX;
        int textY = boxY + paddingY + metrics.getAscent();

        graphics.setColor(TIMER_SHADOW);
        graphics.drawString(text, textX + 1, textY + 1);

        graphics.setColor(TIMER_TEXT);
        graphics.drawString(text, textX, textY);
    }
}
