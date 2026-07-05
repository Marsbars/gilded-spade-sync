package com.gildedspade.sync;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.LinkBrowser;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;

class GildedSpadeSyncPanel extends PluginPanel
{
	static final String WEB_APP_URL = "https://gildedspade.com";

	private final GildedSpadeSyncPlugin plugin;
	private final JLabel webAppStatus = createValueLabel();
	private final Timer refreshTimer;

	GildedSpadeSyncPanel(GildedSpadeSyncPlugin plugin)
	{
		this.plugin = plugin;

		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		JPanel content = new JPanel();
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		content.setBorder(new EmptyBorder(12, 12, 12, 12));
		content.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JLabel title = new JLabel("Gilded Spade");
		title.setFont(FontManager.getRunescapeBoldFont());
		title.setForeground(Color.WHITE);
		title.setAlignmentX(LEFT_ALIGNMENT);
		content.add(title);

		JLabel subtitle = new JLabel("<html>Sync your RuneLite data with the Gilded Spade web app.</html>");
		subtitle.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		subtitle.setAlignmentX(LEFT_ALIGNMENT);
		content.add(Box.createRigidArea(new Dimension(0, 8)));
		content.add(subtitle);

		JButton openButton = new JButton("Open Gilded Spade");
		openButton.setFocusable(false);
		openButton.setAlignmentX(LEFT_ALIGNMENT);
		openButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
		openButton.addActionListener(e -> LinkBrowser.browse(WEB_APP_URL));
		content.add(Box.createRigidArea(new Dimension(0, 12)));
		content.add(openButton);

		JPanel statusPanel = new JPanel();
		statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.Y_AXIS));
		statusPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		statusPanel.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(ColorScheme.BORDER_COLOR),
			new EmptyBorder(10, 10, 10, 10)));
		statusPanel.setAlignmentX(LEFT_ALIGNMENT);
		statusPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
		statusPanel.add(createStatusRow("Web app", webAppStatus));

		content.add(Box.createRigidArea(new Dimension(0, 16)));
		content.add(statusPanel);

		add(content, BorderLayout.NORTH);

		refreshTimer = new Timer(1000, e -> refresh());
		refresh();
	}

	void start()
	{
		refreshTimer.start();
		refresh();
	}

	void stop()
	{
		refreshTimer.stop();
	}

	void refresh()
	{
		int connectionCount = plugin.getWebSocketConnectionCount();

		webAppStatus.setText(connectionCount > 0 ? "Connected" : "Waiting for browser");
		webAppStatus.setForeground(connectionCount > 0 ? ColorScheme.PROGRESS_COMPLETE_COLOR : ColorScheme.LIGHT_GRAY_COLOR);
	}

	private JPanel createStatusRow(String label, JLabel value)
	{
		JPanel row = new JPanel(new BorderLayout(8, 0));
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		row.setAlignmentX(LEFT_ALIGNMENT);

		JLabel name = new JLabel(label);
		name.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		row.add(name, BorderLayout.WEST);
		row.add(value, BorderLayout.EAST);
		return row;
	}

	private static JLabel createValueLabel()
	{
		JLabel label = new JLabel();
		label.setForeground(Color.WHITE);
		return label;
	}
}
