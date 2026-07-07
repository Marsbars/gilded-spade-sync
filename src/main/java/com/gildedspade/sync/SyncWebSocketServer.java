package com.gildedspade.sync;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.callback.ClientThread;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okhttp3.internal.ws.RealWebSocket;
import okhttp3.internal.ws.WebSocketProtocol;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Slf4j
public class SyncWebSocketServer
{
	private final GildedSpadeSyncPlugin plugin;
	private final ClientThread clientThread;
	private final Gson gson;
	private final int port;
	private final AtomicBoolean serverActive = new AtomicBoolean(false);
	private final AtomicBoolean startupFailed = new AtomicBoolean(false);
	private final Set<Connection> connections = ConcurrentHashMap.newKeySet();

	private ServerSocket serverSocket;
	private Thread acceptThread;

	public SyncWebSocketServer(int port, GildedSpadeSyncPlugin plugin, ClientThread clientThread, Gson gson)
	{
		this.plugin = plugin;
		this.clientThread = clientThread;
		this.gson = Objects.requireNonNull(gson);
		this.port = port;
	}

	public int getPort()
	{
		return port;
	}

	public boolean isActive()
	{
		return serverActive.get();
	}

	public boolean hasStartupFailed()
	{
		return startupFailed.get();
	}

	public int getConnectionCount()
	{
		return connections.size();
	}

	public void start() throws IOException
	{
		ServerSocket socket = new ServerSocket();
		socket.setReuseAddress(true);
		socket.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), port));

		serverSocket = socket;
		startupFailed.set(false);
		serverActive.set(true);

		acceptThread = new Thread(this::acceptLoop, "gilded-spade-sync-ws-" + port);
		acceptThread.setDaemon(true);
		acceptThread.start();

		log.info("WebSocket server started successfully");
	}

	public void stop(int timeoutMillis) throws InterruptedException
	{
		serverActive.set(false);
		closeQuietly(serverSocket);
		for (Connection connection : new ArrayList<>(connections))
		{
			connection.close(1001, "Server stopping");
			connection.closeQuietly();
		}
		connections.clear();

		Thread thread = acceptThread;
		if (thread != null)
		{
			thread.join(timeoutMillis);
		}
	}

	private void acceptLoop()
	{
		while (serverActive.get())
		{
			try
			{
				Socket socket = serverSocket.accept();
				if (connections.size() >= 1)
				{
					rejectConnection(socket, 503, "Connection limit reached");
					continue;
				}

				Connection connection = new Connection(socket);
				connections.add(connection);

				Thread connectionThread = new Thread(() -> handleConnection(connection),
					"gilded-spade-sync-ws-client-" + port);
				connectionThread.setDaemon(true);
				connectionThread.start();
			}
			catch (SocketException e)
			{
				if (serverActive.get())
				{
					log.error("WebSocket accept loop failed", e);
					startupFailed.set(true);
					serverActive.set(false);
				}
			}
			catch (Exception e)
			{
				log.error("WebSocket accept loop failed", e);
			}
		}
	}

	private void handleConnection(Connection connection)
	{
		try
		{
			connection.handshake();
			connection.loopReader();
		}
		catch (Exception e)
		{
			if (serverActive.get())
			{
				log.debug("WebSocket connection ended for {}", connection.getRemoteSocketAddress(), e);
			}
		}
		finally
		{
			connections.remove(connection);
			connection.closeQuietly();
		}
	}

	private void handleMessage(Connection conn, String message)
	{
		try
		{
			JsonObject request = gson.fromJson(message, JsonObject.class);
			String messageType = request.get("type").getAsString();
			int sequenceId = request.has("sequenceId") ? request.get("sequenceId").getAsInt() : 0;

			log.debug("Received message type: {}", messageType);

			switch (messageType)
			{
				case "GET_QUESTS":
					handleRequest(conn, sequenceId, "QUEST_DATA", "GET_QUESTS", () ->
					{
						Map<String, Object> data = new HashMap<>();
						data.put("quests", plugin.getAllQuests());
						data.put("questPoints", plugin.getQuestPoints());
						data.put("username", plugin.getUsername());
						return data;
					});
					break;
				case "GET_ALL_DATA":
					handleRequest(conn, sequenceId, "ALL_DATA", "GET_ALL_DATA", plugin::getAllPlayerData);
					break;
				case "GET_STATS":
					handleRequest(conn, sequenceId, "STATS_DATA", "GET_STATS", plugin::getStatsPayload);
					break;
				case "GET_INVENTORY":
					handleRequest(conn, sequenceId, "INVENTORY_DATA", "GET_INVENTORY", plugin::getInventoryItems);
					break;
				case "GET_EQUIPMENT":
					handleRequest(conn, sequenceId, "EQUIPMENT_DATA", "GET_EQUIPMENT", plugin::getEquippedItems);
					break;
				case "GET_BANK":
					handleRequest(conn, sequenceId, "BANK_DATA", "GET_BANK", plugin::getBankedItems);
					break;
				case "GET_BANK_LAYOUT":
					handleRequest(conn, sequenceId, "BANK_LAYOUT_DATA", "GET_BANK_LAYOUT", plugin::getBankLayout);
					break;
				case "SET_BANK_FILTER":
					handleSetBankFilter(conn, sequenceId, request);
					break;
				case "START_BANK_SORT_ASSIST":
					handleStartBankSortAssist(conn, sequenceId, request);
					break;
				case "STOP_BANK_SORT_ASSIST":
					handleSortAction(conn, sequenceId, "STOP_BANK_SORT_ASSIST", plugin::stopBankSortAssist);
					break;
				case "SKIP_BANK_SORT_ASSIST_ITEM":
					handleSortAction(conn, sequenceId, "SKIP_BANK_SORT_ASSIST_ITEM", plugin::skipBankSortAssistItem);
					break;
				case "CONFIRM_BANK_SORT_ASSIST_ITEM":
					handleSortAction(conn, sequenceId, "CONFIRM_BANK_SORT_ASSIST_ITEM", plugin::confirmBankSortAssistItem);
					break;
				case "GET_BANK_SORT_ASSIST_STATUS":
					handleRequest(conn, sequenceId, "BANK_SORT_ASSIST_STATUS",
						"GET_BANK_SORT_ASSIST_STATUS", plugin::getBankSortAssistStatus);
					break;
				case "GET_DIARIES_DETAILED":
					handleRequest(conn, sequenceId, "DIARIES_DATA_DETAILED",
						"GET_DIARIES_DETAILED", plugin::getAchievementDiariesDetailed);
					break;
				case "GET_COMBAT_ACHIEVEMENTS":
					handleRequest(conn, sequenceId, "COMBAT_ACHIEVEMENTS_DATA",
						"GET_COMBAT_ACHIEVEMENTS", plugin::getCombatAchievements);
					break;
				case "GET_COLLECTION_LOG":
					handleRequest(conn, sequenceId, "COLLECTION_LOG_DATA",
						"GET_COLLECTION_LOG", plugin::getCollectionLog);
					break;
				case "GET_ACCOUNT_INFO":
					handleRequest(conn, sequenceId, "ACCOUNT_INFO_DATA",
						"GET_ACCOUNT_INFO", plugin::getAccountInfo);
					break;
				case "GET_SLAYER_TASK":
					handleRequest(conn, sequenceId, "SLAYER_TASK_DATA",
						"GET_SLAYER_TASK", plugin::getSlayerTask);
					break;
				case "GET_BIRDHOUSE_STATUS":
					handleRequest(conn, sequenceId, "BIRDHOUSE_STATUS_DATA",
						"GET_BIRDHOUSE_STATUS", plugin::getBirdhouseStatus);
					break;
				case "GET_DAILY_TASKS":
					handleRequest(conn, sequenceId, "DAILY_TASKS_DATA",
						"GET_DAILY_TASKS", plugin::getDailyTasks);
					break;
				case "GET_WORLD_LOCATION":
					handleRequest(conn, sequenceId, "WORLD_LOCATION_DATA",
						"GET_WORLD_LOCATION", plugin::getWorldLocation);
					break;
				case "PING":
					handlePing(conn, sequenceId);
					break;
				default:
					sendError(conn, sequenceId, "Unknown message type: " + messageType);
			}
		}
		catch (Exception e)
		{
			log.error("Error processing message", e);
			sendError(conn, 0, "Error processing message: " + e.getMessage());
		}
	}

	private <T> void handleRequest(Connection conn, int sequenceId, String responseType,
									String logName, Supplier<T> dataSupplier)
	{
		clientThread.invoke(() ->
		{
			try
			{
				Map<String, Object> response = new HashMap<>();
				response.put("type", responseType);
				response.put("sequenceId", sequenceId);
				response.put("data", dataSupplier.get());
				conn.send(gson.toJson(response));
				log.debug("Sent {} to {}", logName, conn.getRemoteSocketAddress());
			}
			catch (Exception e)
			{
				log.error("Error handling {}", logName, e);
				sendError(conn, sequenceId, "Error retrieving " + logName + ": " + e.getMessage());
			}
		});
	}

	private void handleSortAction(Connection conn, int sequenceId, String logName, Consumer<Runnable> action)
	{
		try
		{
			action.accept(() ->
			{
				sendBankSortAssistStatus(conn, sequenceId);
				log.debug("Handled {}", logName);
			});
		}
		catch (Exception e)
		{
			log.error("Error handling {}", logName, e);
			sendError(conn, sequenceId, "Error: " + e.getMessage());
		}
	}

	private void handleSetBankFilter(Connection conn, int sequenceId, JsonObject request)
	{
		try
		{
			List<Integer> itemIds = new ArrayList<>();
			if (request.has("itemIds") && request.get("itemIds").isJsonArray())
			{
				for (var element : request.getAsJsonArray("itemIds"))
				{
					itemIds.add(element.getAsInt());
				}
			}

			plugin.setBankFilter(itemIds);

			Map<String, Object> response = new HashMap<>();
			response.put("type", "BANK_FILTER_SET");
			response.put("sequenceId", sequenceId);
			response.put("itemCount", itemIds.size());
			conn.send(gson.toJson(response));

			log.info("Set bank filter to {} item IDs", itemIds.size());
		}
		catch (Exception e)
		{
			log.error("Error handling SET_BANK_FILTER", e);
			sendError(conn, sequenceId, "Error setting bank filter: " + e.getMessage());
		}
	}

	private void handleStartBankSortAssist(Connection conn, int sequenceId, JsonObject request)
	{
		try
		{
			List<Map<String, Object>> items = new ArrayList<>();
			if (request.has("items") && request.get("items").isJsonArray())
			{
				for (var element : request.getAsJsonArray("items"))
				{
					if (element.isJsonObject())
					{
						@SuppressWarnings("unchecked")
						Map<String, Object> item = gson.fromJson(element, Map.class);
						items.add(item);
					}
				}
			}

			int itemCount = items.size();
			plugin.startBankSortAssist(items, () ->
			{
				sendBankSortAssistStatus(conn, sequenceId);
				log.info("Started bank sort assist with {} items", itemCount);
			});
		}
		catch (Exception e)
		{
			log.error("Error handling START_BANK_SORT_ASSIST", e);
			sendError(conn, sequenceId, "Error starting bank sort assist: " + e.getMessage());
		}
	}

	private void sendBankSortAssistStatus(Connection conn, int sequenceId)
	{
		Map<String, Object> response = new HashMap<>();
		response.put("type", "BANK_SORT_ASSIST_STATUS");
		response.put("sequenceId", sequenceId);
		response.put("data", plugin.getBankSortAssistStatus());
		conn.send(gson.toJson(response));
	}

	private void handlePing(Connection conn, int sequenceId)
	{
		Map<String, Object> response = new HashMap<>();
		response.put("type", "PONG");
		response.put("sequenceId", sequenceId);
		conn.send(gson.toJson(response));
	}

	private void sendError(Connection conn, int sequenceId, String errorMessage)
	{
		Map<String, Object> response = new HashMap<>();
		response.put("type", "ERROR");
		response.put("sequenceId", sequenceId);
		response.put("error", errorMessage);
		conn.send(gson.toJson(response));
	}

	public void broadcastUsername()
	{
		clientThread.invokeLater(() ->
		{
			try
			{
				String username = plugin.getUsername();
				if (username.isEmpty())
				{
					return;
				}

				Map<String, Object> broadcast = new HashMap<>();
				broadcast.put("type", "USERNAME_CHANGED");
				broadcast.put("data", Map.of("username", username));
				broadcast(gson.toJson(broadcast));
				log.debug("Broadcast username: {}", username);
			}
			catch (Exception e)
			{
				log.error("Error broadcasting username", e);
			}
		});
	}

	public void broadcastBankSortAssistStatus(Map<String, Object> status)
	{
		try
		{
			Map<String, Object> broadcast = new HashMap<>();
			broadcast.put("type", "BANK_SORT_ASSIST_STATUS");
			broadcast.put("sequenceId", 0);
			broadcast.put("data", status);
			broadcast(gson.toJson(broadcast));
		}
		catch (Exception e)
		{
			log.error("Error broadcasting bank sort assist status", e);
		}
	}

	public void broadcastQuestUpdate()
	{
		clientThread.invokeLater(() ->
		{
			try
			{
				Map<String, Object> broadcast = new HashMap<>();
				broadcast.put("type", "PLAYER_UPDATE");
				broadcast.put("data", plugin.getAllPlayerData());
				broadcast(gson.toJson(broadcast));
				log.debug("Broadcast player data update");
			}
			catch (Exception e)
			{
				log.error("Error broadcasting player update", e);
			}
		});
	}

	private void broadcast(String message)
	{
		for (Connection connection : connections)
		{
			connection.send(message);
		}
	}

	private void rejectConnection(Socket socket, int statusCode, String message)
	{
		try (Socket ignored = socket;
			BufferedSink sink = Okio.buffer(Okio.sink(socket)))
		{
			sink.writeUtf8("HTTP/1.1 " + statusCode + " " + message + "\r\n");
			sink.writeUtf8("Connection: close\r\n");
			sink.writeUtf8("Content-Length: 0\r\n\r\n");
			sink.flush();
		}
		catch (IOException e)
		{
			log.debug("Unable to reject extra WebSocket connection", e);
		}
	}

	private static String headerValue(Map<String, String> headers, String name)
	{
		return headers.get(name.toLowerCase(Locale.ROOT));
	}

	private static boolean headerContains(Map<String, String> headers, String name, String expectedValue)
	{
		String value = headerValue(headers, name);
		return value != null && value.toLowerCase(Locale.ROOT).contains(expectedValue.toLowerCase(Locale.ROOT));
	}

	private static void closeQuietly(Closeable closeable)
	{
		if (closeable == null)
		{
			return;
		}
		try
		{
			closeable.close();
		}
		catch (IOException ignored)
		{
			// Best-effort shutdown.
		}
	}

	private class Connection extends WebSocketListener
	{
		private final Socket socket;
		private RealWebSocket webSocket;
		private BufferedSource source;
		private BufferedSink sink;

		Connection(Socket socket)
		{
			this.socket = socket;
		}

		void handshake() throws IOException
		{
			socket.setSoTimeout(5000);
			source = Okio.buffer(Okio.source(socket));
			sink = Okio.buffer(Okio.sink(socket));

			String requestLine = source.readUtf8LineStrict();
			String[] requestParts = requestLine.split(" ", 3);
			if (requestParts.length < 2 || !"GET".equals(requestParts[0]))
			{
				throw new IOException("Invalid WebSocket handshake request");
			}

			Map<String, String> headers = new HashMap<>();
			String line;
			while ((line = source.readUtf8LineStrict()).length() > 0)
			{
				int separator = line.indexOf(':');
				if (separator > 0)
				{
					headers.put(line.substring(0, separator).trim().toLowerCase(Locale.ROOT),
						line.substring(separator + 1).trim());
				}
			}

			String key = headerValue(headers, "Sec-WebSocket-Key");
			if (!headerContains(headers, "Upgrade", "websocket")
				|| !headerContains(headers, "Connection", "Upgrade")
				|| key == null
				|| !"13".equals(headerValue(headers, "Sec-WebSocket-Version")))
			{
				throw new IOException("Invalid WebSocket upgrade headers");
			}

			sink.writeUtf8("HTTP/1.1 101 Switching Protocols\r\n");
			sink.writeUtf8("Upgrade: websocket\r\n");
			sink.writeUtf8("Connection: Upgrade\r\n");
			sink.writeUtf8("Sec-WebSocket-Accept: " + WebSocketProtocol.acceptHeader(key) + "\r\n\r\n");
			sink.flush();

			String path = requestParts[1].startsWith("/") ? requestParts[1] : "/";
			Request request = new Request.Builder()
				.url("ws://localhost:" + port + path)
				.build();

			webSocket = new RealWebSocket(request, this, new Random(), 0);
			RealWebSocket.Streams streams = new RealWebSocket.Streams(false, source, sink)
			{
				@Override
				public void close() throws IOException
				{
					socket.close();
				}
			};
			webSocket.initReaderAndWriter("Gilded Spade Sync", streams);
			socket.setSoTimeout(0);
			onOpen(webSocket, response(request));
		}

		void loopReader() throws IOException
		{
			webSocket.loopReader();
		}

		void send(String message)
		{
			WebSocket currentWebSocket = webSocket;
			if (currentWebSocket != null)
			{
				currentWebSocket.send(message);
			}
		}

		void close(int code, String reason)
		{
			WebSocket currentWebSocket = webSocket;
			if (currentWebSocket != null)
			{
				currentWebSocket.close(code, reason);
			}
		}

		void closeQuietly()
		{
			SyncWebSocketServer.closeQuietly(socket);
		}

		String getRemoteSocketAddress()
		{
			return String.valueOf(socket.getRemoteSocketAddress());
		}

		@Override
		public void onOpen(WebSocket webSocket, Response response)
		{
			log.info("New connection from: {}", getRemoteSocketAddress());
			try
			{
				broadcastUsername();
			}
			catch (Exception e)
			{
				log.error("Error broadcasting username on connection open", e);
			}
		}

		@Override
		public void onMessage(WebSocket webSocket, String text)
		{
			handleMessage(this, text);
		}

		@Override
		public void onClosing(WebSocket webSocket, int code, String reason)
		{
			webSocket.close(code, reason);
		}

		@Override
		public void onClosed(WebSocket webSocket, int code, String reason)
		{
			log.info("Connection closed: {} - Reason: {}", getRemoteSocketAddress(), reason);
		}

		@Override
		public void onFailure(WebSocket webSocket, Throwable t, Response response)
		{
			if (serverActive.get())
			{
				log.error("WebSocket error for connection: {}", getRemoteSocketAddress(), t);
			}
		}

		private Response response(Request request)
		{
			return new Response.Builder()
				.request(request)
				.protocol(Protocol.HTTP_1_1)
				.code(101)
				.message("Switching Protocols")
				.build();
		}
	}
}
