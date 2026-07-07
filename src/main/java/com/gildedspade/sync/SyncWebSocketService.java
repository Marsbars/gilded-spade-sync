package com.gildedspade.sync;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.callback.ClientThread;

import javax.inject.Inject;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
class SyncWebSocketService
{
	private static final int WEBSOCKET_BASE_PORT = 37780;
	private static final int WEBSOCKET_PORT_COUNT = 10;

	private final ClientThread clientThread;
	private final Gson gson;
	private final ScheduledExecutorService executor;

	private SyncWebSocketServer webSocketServer;
	private ScheduledFuture<?> healthCheckFuture;
	private GildedSpadeSyncPlugin plugin;

	@Inject
	SyncWebSocketService(ClientThread clientThread, Gson gson, ScheduledExecutorService executor)
	{
		this.clientThread = clientThread;
		this.gson = gson;
		this.executor = executor;
	}

	synchronized void start(GildedSpadeSyncPlugin plugin)
	{
		this.plugin = plugin;
		webSocketServer = startWebSocketServer(plugin);
		log.info("WebSocket server started on port {}", webSocketServer.getPort());

		healthCheckFuture = executor.scheduleAtFixedRate(this::ensureServerActive, 30, 30, TimeUnit.SECONDS);
	}

	synchronized void stop()
	{
		if (healthCheckFuture != null)
		{
			healthCheckFuture.cancel(false);
			healthCheckFuture = null;
		}

		stopCurrentServer();
		plugin = null;
	}

	synchronized int getConnectionCount()
	{
		return webSocketServer != null ? webSocketServer.getConnectionCount() : 0;
	}

	synchronized void broadcastBankSortAssistStatus(Map<String, Object> status)
	{
		if (webSocketServer != null)
		{
			webSocketServer.broadcastBankSortAssistStatus(status);
		}
	}

	private synchronized void ensureServerActive()
	{
		if (webSocketServer != null && webSocketServer.isActive())
		{
			return;
		}

		GildedSpadeSyncPlugin currentPlugin = plugin;
		if (currentPlugin == null)
		{
			return;
		}

		log.warn("WebSocket server is down, attempting restart...");
		stopCurrentServer();
		try
		{
			webSocketServer = startWebSocketServer(currentPlugin);
			log.info("WebSocket server restarted on port {}", webSocketServer.getPort());
		}
		catch (Exception e)
		{
			log.error("Failed to restart WebSocket server", e);
		}
	}

	private SyncWebSocketServer startWebSocketServer(GildedSpadeSyncPlugin plugin)
	{
		for (int port = WEBSOCKET_BASE_PORT; port < WEBSOCKET_BASE_PORT + WEBSOCKET_PORT_COUNT; port++)
		{
			SyncWebSocketServer candidate = new SyncWebSocketServer(port, plugin, clientThread, gson, executor);
			try
			{
				candidate.start();
				return candidate;
			}
			catch (Exception e)
			{
				log.debug("Unable to start WebSocket server on port {}, trying {}...", port, port + 1, e);
				stopServer(candidate);
			}
		}

		throw new RuntimeException("No available port found in range "
			+ WEBSOCKET_BASE_PORT + "-" + (WEBSOCKET_BASE_PORT + WEBSOCKET_PORT_COUNT - 1));
	}

	private void stopCurrentServer()
	{
		if (webSocketServer != null)
		{
			stopServer(webSocketServer);
			webSocketServer = null;
		}
	}

	private void stopServer(SyncWebSocketServer server)
	{
		server.stop();
	}
}
