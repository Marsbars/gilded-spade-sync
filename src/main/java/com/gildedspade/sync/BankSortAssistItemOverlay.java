package com.gildedspade.sync;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import javax.inject.Inject;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.ui.overlay.WidgetItemOverlay;

/** Draws the current bank-sort source item in amber. */
class BankSortAssistItemOverlay extends WidgetItemOverlay
{
	private static final Color AMBER_FILL = new Color(255, 179, 0, 70);
	private static final Color AMBER_BORDER = new Color(255, 179, 0, 255);
	private static final Color GREEN_FILL = new Color(0, 210, 125, 70);
	private static final Color GREEN_BORDER = new Color(0, 210, 125, 255);
	private final GildedSpadeSyncPlugin plugin;

	@Inject
	BankSortAssistItemOverlay(GildedSpadeSyncPlugin plugin)
	{
		this.plugin = plugin;
		showOnBank();
	}

	@Override
	public void renderItemOverlay(Graphics2D graphics, int itemId, WidgetItem widgetItem)
	{
		BankSortAssistService.BankSortAssistItem current = plugin.getCurrentBankSortAssistItem();
		if (current == null || widgetItem.getWidget().getId() != InterfaceID.Bankmain.ITEMS)
		{
			return;
		}

		Rectangle bounds = widgetItem.getCanvasBounds();
		if (itemId == current.id)
		{
			graphics.setColor(AMBER_FILL);
			graphics.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 5, 5);
			graphics.setColor(AMBER_BORDER);
		}
		else if (itemId == current.anchorItemId)
		{
			graphics.setColor(GREEN_FILL);
			graphics.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 5, 5);
			graphics.setColor(GREEN_BORDER);
		}
		else
		{
			return;
		}
		graphics.setStroke(new BasicStroke(1f));
		graphics.drawRoundRect(bounds.x, bounds.y, bounds.width - 1, bounds.height - 1, 5, 5);
	}
}
