package com.gildedspade.sync;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.callback.ClientThread;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Slf4j
public class SyncWebSocketServer extends WebSocketServer
{
	private final GildedSpadeSyncPlugin plugin;
	private final ClientThread clientThread;
	private final Gson gson;
	private final int port;
	private final AtomicBoolean serverActive = new AtomicBoolean(false);
	private final AtomicBoolean startupFailed = new AtomicBoolean(false);

	public SyncWebSocketServer(int port, GildedSpadeSyncPlugin plugin, ClientThread clientThread)
	{
		super(new InetSocketAddress("localhost", port));
		this.plugin = plugin;
		this.clientThread = clientThread;
		this.gson = new Gson();
		this.port = port;
		setReuseAddr(true);
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
		return getConnections().size();
	}

	@Override
	public void onOpen(WebSocket conn, ClientHandshake handshake)
	{
		if (getConnections().size() > 1)
		{
			log.warn("Connection limit reached, rejecting {}", conn.getRemoteSocketAddress());
			conn.close(1008, "Connection limit reached");
			return;
		}
		log.info("New connection from: {}", conn.getRemoteSocketAddress());
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
	public void onClose(WebSocket conn, int code, String reason, boolean remote)
	{
		log.info("Connection closed: {} - Reason: {}", conn.getRemoteSocketAddress(), reason);
	}

	@Override
	public void onMessage(WebSocket conn, String message)
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

	private <T> void handleRequest(WebSocket conn, int sequenceId, String responseType,
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

	private void handleSortAction(WebSocket conn, int sequenceId, String logName, Consumer<Runnable> action)
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

	private void handleSetBankFilter(WebSocket conn, int sequenceId, JsonObject request)
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

	private void handleStartBankSortAssist(WebSocket conn, int sequenceId, JsonObject request)
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

	private void sendBankSortAssistStatus(WebSocket conn, int sequenceId)
	{
		Map<String, Object> response = new HashMap<>();
		response.put("type", "BANK_SORT_ASSIST_STATUS");
		response.put("sequenceId", sequenceId);
		response.put("data", plugin.getBankSortAssistStatus());
		conn.send(gson.toJson(response));
	}

	private void handlePing(WebSocket conn, int sequenceId)
	{
		Map<String, Object> response = new HashMap<>();
		response.put("type", "PONG");
		response.put("sequenceId", sequenceId);
		conn.send(gson.toJson(response));
	}

	private void sendError(WebSocket conn, int sequenceId, String errorMessage)
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

	@Override
	public void onError(WebSocket conn, Exception ex)
	{
		if (conn != null)
		{
			log.error("WebSocket error for connection: {}", conn.getRemoteSocketAddress(), ex);
		}
		else
		{
			log.error("WebSocket server error", ex);
			startupFailed.set(true);
			serverActive.set(false);
		}
	}

	@Override
	public void onStart()
	{
		log.info("WebSocket server started successfully");
		startupFailed.set(false);
		serverActive.set(true);
	}
}
