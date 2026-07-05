package com.gildedspade.sync;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/** Draws the current bank-sort destination tab in green. */
class BankSortAssistTabOverlay extends Overlay
{
	private static final Color GREEN_FILL = new Color(0, 210, 125, 70);
	private static final Color GREEN_BORDER = new Color(0, 210, 125, 255);
	private final Client client;
	private final GildedSpadeSyncPlugin plugin;
	private String highlightedTabId;
	private Rectangle lastValidTabBounds;

	@Inject
	BankSortAssistTabOverlay(Client client, GildedSpadeSyncPlugin plugin)
	{
		super(plugin);
		this.client = client;
		this.plugin = plugin;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		BankSortAssistService.BankSortAssistItem current = plugin.getCurrentBankSortAssistItem();
		if (current == null)
		{
			clearHighlightBounds();
			return null;
		}
		if (!"move-tab".equals(current.actionType))
		{
			clearHighlightBounds();
			return null;
		}

		Widget tabContainer = client.getWidget(InterfaceID.Bankmain.TABS);
		if (tabContainer == null || tabContainer.isHidden())
		{
			clearHighlightBounds();
			return null;
		}
		if (!current.targetTabId.equals(highlightedTabId))
		{
			highlightedTabId = current.targetTabId;
			lastValidTabBounds = null;
		}

		int tabIndex = getTabIndex(current.targetTabId);
		Widget targetTab = getTab(tabContainer, tabIndex);
		if (targetTab != null && isLaidOut(targetTab.getBounds()))
		{
			lastValidTabBounds = new Rectangle(targetTab.getBounds());
		}

		// The bank briefly detaches its tab widgets while redrawing. Keep the most
		// recent real bounds during that frame so the destination highlight is steady.
		if (lastValidTabBounds == null)
		{
			return null;
		}

		graphics.setColor(GREEN_FILL);
		graphics.fillRoundRect(lastValidTabBounds.x, lastValidTabBounds.y, lastValidTabBounds.width, lastValidTabBounds.height, 5, 5);
		graphics.setColor(GREEN_BORDER);
		graphics.setStroke(new BasicStroke(1f));
		graphics.drawRoundRect(lastValidTabBounds.x, lastValidTabBounds.y, lastValidTabBounds.width - 1, lastValidTabBounds.height - 1, 5, 5);
		return null;
	}

	private boolean isLaidOut(Rectangle bounds)
	{
		return bounds != null && bounds.x > 0 && bounds.y > 0 && bounds.width > 0 && bounds.height > 0;
	}

	private void clearHighlightBounds()
	{
		highlightedTabId = null;
		lastValidTabBounds = null;
	}

	private int getTabIndex(String targetTabId)
	{
		if ("main".equalsIgnoreCase(targetTabId))
		{
			return 0;
		}

		try
		{
			int tabIndex = Integer.parseInt(targetTabId);
			return tabIndex >= 1 && tabIndex <= 9 ? tabIndex : 0;
		}
		catch (NumberFormatException ignored)
		{
			return 0;
		}
	}

	private Widget getTab(Widget tabContainer, int tabIndex)
	{
		List<Widget> tabs = visibleChildren(tabContainer.getDynamicChildren());
		if (tabs.isEmpty())
		{
			tabs = visibleChildren(tabContainer.getChildren());
		}
		return tabIndex < tabs.size() ? tabs.get(tabIndex) : null;
	}

	private List<Widget> visibleChildren(Widget[] children)
	{
		List<Widget> result = new ArrayList<>();
		if (children == null)
		{
			return result;
		}
		for (Widget child : children)
		{
			if (child != null && !child.isHidden())
			{
				result.add(child);
			}
		}
		return result;
	}
}
