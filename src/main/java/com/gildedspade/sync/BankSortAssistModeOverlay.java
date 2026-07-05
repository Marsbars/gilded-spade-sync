package com.gildedspade.sync;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/** Draws attention to the bank insert/swap control when a sort step requires it. */
class BankSortAssistModeOverlay extends Overlay
{
	private static final Color AMBER_FILL = new Color(255, 179, 0, 70);
	private static final Color AMBER_BORDER = new Color(255, 179, 0, 255);
	private final Client client;
	private final GildedSpadeSyncPlugin plugin;

	@Inject
	BankSortAssistModeOverlay(Client client, GildedSpadeSyncPlugin plugin)
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
		if (!plugin.isAwaitingBankSortAssistMoveMode())
		{
			return null;
		}

		Widget modeToggle = client.getWidget(InterfaceID.Bankmain.SWAP_INSERT);
		if (modeToggle == null || modeToggle.isHidden())
		{
			return null;
		}

		Rectangle bounds = modeToggle.getBounds();
		if (bounds == null || bounds.width <= 0 || bounds.height <= 0)
		{
			return null;
		}

		graphics.setColor(AMBER_FILL);
		graphics.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 5, 5);
		graphics.setColor(AMBER_BORDER);
		graphics.setStroke(new BasicStroke(2f));
		graphics.drawRoundRect(bounds.x, bounds.y, bounds.width - 1, bounds.height - 1, 5, 5);
		return null;
	}
}
