package com.gildedspade.sync;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.ScriptCallbackEvent;
import net.runelite.api.events.VarClientIntChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.events.WidgetClosed;
import net.runelite.api.gameval.DBTableID;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarClientID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.vars.InputType;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;
import org.apache.commons.lang3.ArrayUtils;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@PluginDescriptor(
	name = "Gilded Spade Sync",
	description = "Syncs player progress data (quests, stats, diaries, etc.) to Gilded Spade web app",
	tags = {"quest", "sync", "gilded", "spade", "stats", "diary", "collection"}
)
public class GildedSpadeSyncPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private BankSortAssistItemOverlay bankSortAssistItemOverlay;

	@Inject
	private BankSortAssistTabOverlay bankSortAssistTabOverlay;

	@Inject
	private BankSortAssistModeOverlay bankSortAssistModeOverlay;

	@Inject
	private CombatAchievementService combatAchievementService;

	@Inject
	private BankCacheService bankCacheService;

	@Inject
	private PlayerDataService playerDataService;

	@Inject
	private SyncWebSocketService webSocketService;

	private BankSortAssistService bankSortAssistService;

	private GildedSpadeSyncPanel syncPanel;
	private NavigationButton navigationButton;

	private static final int[] BANK_TAB_VARBITS = {
		VarbitID.BANK_TAB_1, VarbitID.BANK_TAB_2, VarbitID.BANK_TAB_3,
		VarbitID.BANK_TAB_4, VarbitID.BANK_TAB_5, VarbitID.BANK_TAB_6,
		VarbitID.BANK_TAB_7, VarbitID.BANK_TAB_8, VarbitID.BANK_TAB_9
	};

	private Set<Integer> bankFilterIds = Collections.emptySet();
	private boolean applyingBankFilter = false;

	private static final String[][] DIARY_VARGS = {
		{"ardougne", String.valueOf(VarPlayerID.ARDOUNGE_ACHIEVEMENT_DIARY), String.valueOf(VarPlayerID.ARDOUNGE_ACHIEVEMENT_DIARY2)},
		{"desert", String.valueOf(VarPlayerID.DESERT_ACHIEVEMENT_DIARY), String.valueOf(VarPlayerID.DESERT_ACHIEVEMENT_DIARY2)},
		{"falador", String.valueOf(VarPlayerID.FALADOR_ACHIEVEMENT_DIARY), String.valueOf(VarPlayerID.FALADOR_ACHIEVEMENT_DIARY2)},
		{"fremennik", String.valueOf(VarPlayerID.FREMENNIK_ACHIEVEMENT_DIARY), String.valueOf(VarPlayerID.FREMENNIK_ACHIEVEMENT_DIARY2)},
		{"kandarin", String.valueOf(VarPlayerID.KANDARIN_ACHIEVEMENT_DIARY), String.valueOf(VarPlayerID.KANDARIN_ACHIEVEMENT_DIARY2)},
		{"karamja", "3578", "3599"},
		{"kourend_kebos", String.valueOf(VarPlayerID.KOUREND_ACHIEVEMENT_DIARY), String.valueOf(VarPlayerID.KOUREND_ACHIEVEMENT_DIARY2)},
		{"lumbridge_draynor", String.valueOf(VarPlayerID.LUMB_DRAY_ACHIEVEMENT_DIARY), String.valueOf(VarPlayerID.LUMB_DRAY_ACHIEVEMENT_DIARY2)},
		{"morytania", String.valueOf(VarPlayerID.MORYTANIA_ACHIEVEMENT_DIARY), String.valueOf(VarPlayerID.MORYTANIA_ACHIEVEMENT_DIARY2)},
		{"varrock", String.valueOf(VarPlayerID.VARROCK_ACHIEVEMENT_DIARY), String.valueOf(VarPlayerID.VARROCK_ACHIEVEMENT_DIARY2)},
		{"western_provinces", String.valueOf(VarPlayerID.WESTERN_ACHIEVEMENT_DIARY), String.valueOf(VarPlayerID.WESTERN_ACHIEVEMENT_DIARY2)},
		{"wilderness", String.valueOf(VarPlayerID.WILDERNESS_ACHIEVEMENT_DIARY), String.valueOf(VarPlayerID.WILDERNESS_ACHIEVEMENT_DIARY2)}
	};

	private static final String[] BIRDHOUSE_NAMES = {
		"Bird House", "Oak Bird House", "Willow Bird House", "Teak Bird House",
		"Maple Bird House", "Mahogany Bird House", "Yew Bird House", "Magic Bird House", "Redwood Bird House"
	};

	private static final String[] BIRDHOUSE_LOCATION_NAMES = {
		"Mushroom Meadow (North)", "Mushroom Meadow (South)",
		"Verdant Valley (Northeast)", "Verdant Valley (Southwest)"
	};

	private static final int[] BIRDHOUSE_VARP_IDS = {
		VarPlayerID.BIRDHOUSE_TRANSMIT_A, VarPlayerID.BIRDHOUSE_TRANSMIT_B,
		VarPlayerID.BIRDHOUSE_TRANSMIT_C, VarPlayerID.BIRDHOUSE_TRANSMIT_D
	};

	@Override
	protected void startUp() throws Exception
	{
		log.info("Gilded Spade Sync started!");

		bankSortAssistService = new BankSortAssistService(
			client, clientThread,
			this::setBankFilter,
			status -> webSocketService.broadcastBankSortAssistStatus(status));

		overlayManager.add(bankSortAssistItemOverlay);
		overlayManager.add(bankSortAssistTabOverlay);
		overlayManager.add(bankSortAssistModeOverlay);

		syncPanel = new GildedSpadeSyncPanel(this);
		navigationButton = NavigationButton.builder()
			.tooltip("Gilded Spade")
			.icon(createNavigationIcon())
			.panel(syncPanel)
			.priority(5)
			.build();
		clientToolbar.addNavigation(navigationButton);
		syncPanel.start();

		webSocketService.start(this);
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.info("Gilded Spade Sync stopped!");
		overlayManager.remove(bankSortAssistItemOverlay);
		overlayManager.remove(bankSortAssistTabOverlay);
		overlayManager.remove(bankSortAssistModeOverlay);

		if (syncPanel != null)
		{
			syncPanel.stop();
		}
		if (navigationButton != null)
		{
			clientToolbar.removeNavigation(navigationButton);
		}

		webSocketService.stop();
		bankSortAssistService = null;
	}

	int getWebSocketConnectionCount()
	{
		return webSocketService.getConnectionCount();
	}

	private java.awt.image.BufferedImage createNavigationIcon()
	{
		return ImageUtil.loadImageResource(GildedSpadeSyncPlugin.class, "gildedspade.png");
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			bankCacheService.clearBankCacheIfDifferentPlayer();
		}
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (event.getContainerId() == InventoryID.BANK.getId())
		{
			try
			{
				if (!playerDataService.isReady())
				{
					return;
				}

				ItemContainer bankContainer = event.getItemContainer();
				if (bankContainer != null)
				{
					bankCacheService.cacheBankItems(bankContainer);
					log.debug("Updated bank cache: {} items", bankCacheService.getCachedItemCount());
					bankSortAssistService.advanceBankSortAssistAfterBankChange();
				}
			}
			catch (Exception e)
			{
				log.error("Error caching bank items", e);
			}
		}
	}

	@Subscribe
	public void onWidgetClosed(WidgetClosed event)
	{
		if (event.getGroupId() == InterfaceID.BANKMAIN)
		{
			bankFilterIds = Collections.emptySet();
		}
	}

	@Subscribe
	public void onVarClientIntChanged(VarClientIntChanged event)
	{
		if (applyingBankFilter)
		{
			return;
		}
		if (event.getIndex() == VarClientID.MESLAYERMODE
			&& client.getVarcIntValue(VarClientID.MESLAYERMODE) == InputType.NONE.getType()
			&& !bankFilterIds.isEmpty())
		{
			bankFilterIds = Collections.emptySet();
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		bankSortAssistService.handleVarbitChanged(event.getVarbitId(), event.getVarpId());
	}

	@Subscribe(priority = -1)
	public void onScriptCallbackEvent(ScriptCallbackEvent event)
	{
		if (!event.getEventName().equals("bankSearchFilter") || bankFilterIds.isEmpty())
		{
			return;
		}

		int[] intStack = client.getIntStack();
		int intStackSize = client.getIntStackSize();
		int itemId = intStack[intStackSize - 1];

		intStack[intStackSize - 2] = bankFilterIds.contains(itemId) ? 1 : 0;
	}

	public void setBankFilter(List<Integer> itemIds)
	{
		clientThread.invoke(() ->
		{
			bankFilterIds = itemIds.isEmpty() ? Collections.emptySet() : new HashSet<>(itemIds);

			Widget bankContainer = client.getWidget(InterfaceID.Bankmain.ITEMS);
			if (bankContainer == null || bankContainer.isHidden())
			{
				return;
			}

			Object[] bankBuildArgs = bankContainer.getOnInvTransmitListener();
			if (bankBuildArgs == null)
			{
				return;
			}

			applyingBankFilter = true;
			try
			{
				if (bankFilterIds.isEmpty())
				{
					client.setVarcIntValue(VarClientID.MESLAYERMODE, InputType.NONE.getType());
					client.setVarcStrValue(VarClientID.MESLAYERINPUT, "");
					client.runScript(bankBuildArgs);
				}
				else
				{
					client.runScript(ScriptID.MESSAGE_LAYER_CLOSE, 1, 1, 0);
					Object[] searchToggleArgs = ArrayUtils.insert(1, bankBuildArgs, 1);
					searchToggleArgs[0] = ScriptID.BANKMAIN_SEARCH_TOGGLE;
					client.runScript(searchToggleArgs);

					client.setVarcStrValue(VarClientID.MESLAYERINPUT, "\u00A0");

					Widget refreshedContainer = client.getWidget(InterfaceID.Bankmain.ITEMS);
					if (refreshedContainer != null)
					{
						Object[] refreshArgs = refreshedContainer.getOnInvTransmitListener();
						if (refreshArgs != null)
						{
							client.runScript(refreshArgs);
						}
					}
				}
			}
			finally
			{
				applyingBankFilter = false;
			}
		});
	}

	public void startBankSortAssist(List<Map<String, Object>> items)
	{
		bankSortAssistService.startBankSortAssist(items);
	}

	public void stopBankSortAssist()
	{
		bankSortAssistService.stopBankSortAssist();
	}

	public void skipBankSortAssistItem()
	{
		bankSortAssistService.skipBankSortAssistItem();
	}

	public void confirmBankSortAssistItem()
	{
		bankSortAssistService.confirmBankSortAssistItem();
	}

	public Map<String, Object> getBankSortAssistStatus()
	{
		return bankSortAssistService.getBankSortAssistStatus();
	}

	BankSortAssistService.BankSortAssistItem getCurrentBankSortAssistItem()
	{
		return bankSortAssistService.getCurrentBankSortAssistItem();
	}

	boolean isAwaitingBankSortAssistMoveMode()
	{
		return bankSortAssistService.isAwaitingBankSortAssistMoveMode();
	}

	private int[] getBankTabCounts()
	{
		int[] counts = new int[BANK_TAB_VARBITS.length];
		for (int i = 0; i < BANK_TAB_VARBITS.length; i++)
		{
			counts[i] = Math.max(0, client.getVarbitValue(BANK_TAB_VARBITS[i]));
		}
		return counts;
	}

	private List<BankLayoutSlot> getBankLayoutSlots(ItemContainer bankContainer)
	{
		List<BankLayoutSlot> slots = new ArrayList<>();
		Item[] items = bankContainer.getItems();

		for (int slot = 0; slot < items.length; slot++)
		{
			Item item = items[slot];
			if (item == null || item.getId() <= 0)
			{
				continue;
			}

			ItemComposition itemComp = client.getItemDefinition(item.getId());
			boolean isPlaceholder = itemComp != null && itemComp.getPlaceholderTemplateId() != -1;
			boolean isBankFiller = item.getId() == ItemID.BANK_FILLER;
			boolean shouldReturn = !isBankFiller && itemComp != null && (isPlaceholder || item.getQuantity() > 0);
			int iconId = isPlaceholder && itemComp.getPlaceholderId() > 0
				? itemComp.getPlaceholderId()
				: item.getId();
			String name = itemComp != null ? itemComp.getName() : "Unknown";

			slots.add(new BankLayoutSlot(item.getId(), iconId, name, item.getQuantity(), slot, isPlaceholder, shouldReturn));
		}

		return slots;
	}

	public List<Map<String, Object>> getInventoryItems()
	{
		List<Map<String, Object>> items = new ArrayList<>();

		if (!playerDataService.isReady())
		{
			return items;
		}

		ItemContainer inventory = client.getItemContainer(InventoryID.INVENTORY);
		if (inventory != null)
		{
			Item[] inventoryItems = inventory.getItems();
			for (int i = 0; i < inventoryItems.length; i++)
			{
				Item item = inventoryItems[i];
				if (item != null && item.getId() != -1 && item.getQuantity() > 0)
				{
					Map<String, Object> itemData = new HashMap<>();
					ItemComposition itemComp = client.getItemDefinition(item.getId());

					itemData.put("id", item.getId());
					itemData.put("name", itemComp != null ? itemComp.getName() : "Unknown");
					itemData.put("quantity", item.getQuantity());
					itemData.put("slot", i);

					items.add(itemData);
				}
			}
		}

		return items;
	}

	public List<Map<String, Object>> getEquippedItems()
	{
		List<Map<String, Object>> items = new ArrayList<>();

		if (!playerDataService.isReady())
		{
			return items;
		}

		ItemContainer equipment = client.getItemContainer(InventoryID.EQUIPMENT);
		if (equipment != null)
		{
			Item[] equippedItems = equipment.getItems();
			for (int i = 0; i < equippedItems.length; i++)
			{
				Item item = equippedItems[i];
				if (item != null && item.getId() != -1 && item.getQuantity() > 0)
				{
					Map<String, Object> itemData = new HashMap<>();
					ItemComposition itemComp = client.getItemDefinition(item.getId());
					String slotName = getEquipmentSlotName(i);

					itemData.put("id", item.getId());
					itemData.put("name", itemComp != null ? itemComp.getName() : "Unknown");
					itemData.put("quantity", item.getQuantity());
					itemData.put("slot", slotName != null ? slotName : i);
					itemData.put("slotIndex", i);

					items.add(itemData);
				}
			}
		}

		return items;
	}

	private String getEquipmentSlotName(int slotIndex)
	{
		switch (slotIndex)
		{
			case 0:
				return "head";
			case 1:
				return "cape";
			case 2:
				return "neck";
			case 3:
				return "weapon";
			case 4:
				return "body";
			case 5:
				return "shield";
			case 7:
				return "legs";
			case 9:
				return "hands";
			case 10:
				return "feet";
			case 12:
				return "ring";
			case 13:
				return "ammo";
			default:
				return null;
		}
	}

	public List<Map<String, Object>> getBankedItems()
	{
		if (!playerDataService.isReady())
		{
			return new ArrayList<>();
		}

		return bankCacheService.getBankedItems();
	}

	public Map<String, Object> getBankLayout()
	{
		Map<String, Object> layout = new HashMap<>();
		List<Map<String, Object>> items = new ArrayList<>();

		if (!playerDataService.isReady())
		{
			layout.put("items", items);
			layout.put("tabCounts", new int[BANK_TAB_VARBITS.length]);
			layout.put("bankOpen", false);
			return layout;
		}

		try
		{
			ItemContainer bankContainer = client.getItemContainer(InventoryID.BANK);
			if (bankContainer != null)
			{
				bankCacheService.cacheBankItems(bankContainer);
			}

			List<BankLayoutSlot> layoutSlots = bankContainer != null
				? getBankLayoutSlots(bankContainer)
				: Collections.emptyList();
			int[] tabCounts = getBankTabCounts();
			int cursor = 0;

			for (int tabOffset = 0; tabOffset < tabCounts.length; tabOffset++)
			{
				int count = Math.min(tabCounts[tabOffset], layoutSlots.size() - cursor);
				for (int i = 0; i < count; i++)
				{
					BankLayoutSlot slot = layoutSlots.get(cursor);
					if (slot.shouldReturn)
					{
						items.add(slot.toMap(tabOffset + 1));
					}
					cursor++;
				}
			}

			while (cursor < layoutSlots.size())
			{
				BankLayoutSlot slot = layoutSlots.get(cursor);
				if (slot.shouldReturn)
				{
					items.add(slot.toMap(0));
				}
				cursor++;
			}

			layout.put("items", items);
			layout.put("tabCounts", tabCounts);
			layout.put("bankOpen", bankContainer != null);
		}
		catch (Exception e)
		{
			log.error("Error getting bank layout", e);
			layout.put("items", items);
			layout.put("tabCounts", new int[BANK_TAB_VARBITS.length]);
			layout.put("bankOpen", false);
		}

		return layout;
	}

	public List<Map<String, Object>> getAllQuests()
	{
		return playerDataService.getAllQuests();
	}

	public int getQuestPoints()
	{
		return playerDataService.getQuestPoints();
	}

	public int getCombatLevel()
	{
		return playerDataService.getCombatLevel();
	}

	public String getUsername()
	{
		return playerDataService.getUsername();
	}

	public Map<String, Map<String, Object>> getStats()
	{
		return playerDataService.getStats();
	}

	public Map<String, Object> getStatsPayload()
	{
		return playerDataService.getStatsPayload();
	}

	public Map<String, Object> getPlayerStatus()
	{
		return playerDataService.getPlayerStatus();
	}

	public Map<String, Map<String, Object>> getAchievementDiariesDetailed()
	{
		Map<String, Map<String, Object>> diaries = new HashMap<>();

		if (!playerDataService.isReady())
		{
			return diaries;
		}

		try
		{
			for (String[] diaryDef : DIARY_VARGS)
			{
				String diaryName = diaryDef[0];
				int[] diaryVarps = new int[]{Integer.parseInt(diaryDef[1]), Integer.parseInt(diaryDef[2])};
				diaries.put(diaryName, getDetailedDiaryData(diaryName, diaryVarps));
			}
		}
		catch (Exception e)
		{
			log.error("Error getting detailed achievement diaries", e);
		}

		return diaries;
	}

	private Map<String, Object> getDetailedDiaryData(String diaryName, int[] diaryVarps)
	{
		Map<String, Object> tierData = new HashMap<>();
		String[] tiers = {"easy", "medium", "hard", "elite"};

		if (diaryVarps.length < 2)
		{
			log.warn("Invalid diaryVarps array length: {}", diaryVarps.length);
			return tierData;
		}

		for (int i = 0; i < tiers.length; i++)
		{
			try
			{
				Map<String, Object> data = new HashMap<>();

				DiaryVarplayerMapping.TaskMapping[] taskMappings =
					DiaryVarplayerMapping.getTaskMappings(diaryName, tiers[i]);

				List<Map<String, Object>> tasks = new ArrayList<>();
				int completedCount = 0;

				if (taskMappings != null)
				{
					for (int taskIndex = 0; taskIndex < taskMappings.length; taskIndex++)
					{
						Map<String, Object> task = new HashMap<>();
						DiaryVarplayerMapping.TaskMapping mapping = taskMappings[taskIndex];

						task.put("description", mapping.description);

						boolean taskCompleted = DiaryVarplayerMapping.isTaskCompleted(client, mapping);
						task.put("completed", taskCompleted);

						if (taskCompleted)
						{
							completedCount++;
						}

						tasks.add(task);
					}
				}
				else
				{
					log.debug("No task mapping found for {}/{}", diaryName, tiers[i]);
				}

				int totalCount = taskMappings != null ? taskMappings.length : 0;
				boolean tierCompleted = totalCount > 0 && completedCount == totalCount;

				data.put("completed", tierCompleted);
				data.put("tasks", tasks);
				data.put("tasksComplete", completedCount);
				data.put("tasksTotal", totalCount);

				tierData.put(tiers[i], data);
			}
			catch (Exception e)
			{
				log.error("Error processing tier {} for diary {}", tiers[i], diaryName, e);
				Map<String, Object> data = new HashMap<>();
				data.put("completed", false);
				data.put("tasks", new ArrayList<>());
				data.put("tasksComplete", 0);
				data.put("tasksTotal", 0);
				tierData.put(tiers[i], data);
			}
		}

		return tierData;
	}

	public Map<String, Object> getCombatAchievements()
	{
		if (!playerDataService.isReady())
		{
			return new HashMap<>();
		}

		return combatAchievementService.getCombatAchievements();
	}

	public Map<String, Integer> getCollectionLog()
	{
		return playerDataService.getCollectionLog();
	}

	public Map<String, Object> getAccountInfo()
	{
		return playerDataService.getAccountInfo();
	}

	public Map<String, Object> getSlayerTask()
	{
		Map<String, Object> task = new HashMap<>();

		if (!playerDataService.isReady())
		{
			return task;
		}

		try
		{
			int amount = client.getVarpValue(VarPlayerID.SLAYER_COUNT);
			task.put("killsRemaining", amount);
			task.put("initialCount", client.getVarpValue(VarPlayerID.SLAYER_COUNT_ORIGINAL));
			task.put("streak", client.getVarbitValue(VarbitID.SLAYER_TASKS_COMPLETED));
			task.put("points", client.getVarbitValue(VarbitID.SLAYER_POINTS));

			if (amount > 0)
			{
				int taskId = client.getVarpValue(VarPlayerID.SLAYER_TARGET);
				String taskName = null;

				if (taskId == 98)
				{
					var bossRows = client.getDBRowsByValue(
						DBTableID.SlayerTaskSublist.ID,
						DBTableID.SlayerTaskSublist.COL_TASK_SUBTABLE_ID,
						0,
						client.getVarbitValue(VarbitID.SLAYER_TARGET_BOSSID));
					if (!bossRows.isEmpty())
					{
						int bossRow = (Integer) client.getDBTableField(bossRows.get(0), DBTableID.SlayerTaskSublist.COL_TASK, 0)[0];
						taskName = (String) client.getDBTableField(bossRow, DBTableID.SlayerTask.COL_NAME_UPPERCASE, 0)[0];
					}
				}
				else
				{
					var taskRows = client.getDBRowsByValue(DBTableID.SlayerTask.ID, DBTableID.SlayerTask.COL_ID, 0, taskId);
					if (!taskRows.isEmpty())
					{
						taskName = (String) client.getDBTableField(taskRows.get(0), DBTableID.SlayerTask.COL_NAME_UPPERCASE, 0)[0];
					}
				}

				if (taskName != null)
				{
					task.put("taskName", taskName);
				}

				int areaId = client.getVarpValue(VarPlayerID.SLAYER_AREA);
				if (areaId > 0)
				{
					var areaRows = client.getDBRowsByValue(DBTableID.SlayerArea.ID, DBTableID.SlayerArea.COL_AREA_ID, 0, areaId);
					if (!areaRows.isEmpty())
					{
						String location = (String) client.getDBTableField(areaRows.get(0), DBTableID.SlayerArea.COL_AREA_NAME_IN_HELPER, 0)[0];
						task.put("location", location);
					}
				}
			}
		}
		catch (Exception e)
		{
			log.error("Error getting slayer task", e);
		}

		return task;
	}

	public List<Map<String, Object>> getBirdhouseStatus()
	{
		List<Map<String, Object>> birdhouseList = new ArrayList<>();

		if (!playerDataService.isReady())
		{
			return birdhouseList;
		}

		try
		{
			for (int i = 0; i < BIRDHOUSE_VARP_IDS.length; i++)
			{
				Map<String, Object> entry = new HashMap<>();
				entry.put("location", BIRDHOUSE_LOCATION_NAMES[i]);

				int varp = client.getVarpValue(BIRDHOUSE_VARP_IDS[i]);
				entry.put("rawVarp", varp);

				String state;
				String houseType = null;
				if (varp == 0)
				{
					state = "EMPTY";
				}
				else if (varp % 3 == 0)
				{
					state = "SEEDED";
					int typeIndex = (varp / 3) - 1;
					if (typeIndex >= 0 && typeIndex < BIRDHOUSE_NAMES.length)
					{
						houseType = BIRDHOUSE_NAMES[typeIndex];
					}
				}
				else
				{
					state = "BUILT";
					int typeIndex = (varp - 1) / 3;
					if (typeIndex >= 0 && typeIndex < BIRDHOUSE_NAMES.length)
					{
						houseType = BIRDHOUSE_NAMES[typeIndex];
					}
				}

				entry.put("state", state);
				if (houseType != null)
				{
					entry.put("houseType", houseType);
				}

				birdhouseList.add(entry);
			}
		}
		catch (Exception e)
		{
			log.error("Error getting birdhouse status", e);
		}

		return birdhouseList;
	}

	public Map<String, Integer> getWorldLocation()
	{
		return playerDataService.getWorldLocation();
	}

	public Map<String, Object> getDailyTasks()
	{
		Map<String, Object> tasks = new HashMap<>();

		if (!playerDataService.isReady())
		{
			return tasks;
		}

		try
		{
			boolean isUim = client.getVarbitValue(VarbitID.IRONMAN) == 2;
			boolean isIronman = client.getVarbitValue(VarbitID.IRONMAN) != 0;

			boolean varrockEasy = client.getVarbitValue(VarbitID.VARROCK_DIARY_EASY_COMPLETE) == 1;
			boolean handInSandDone = client.getVarbitValue(VarbitID.HANDSAND_QUEST) >= 160;
			boolean ardougneMedium = client.getVarbitValue(VarbitID.ARDOUGNE_DIARY_MEDIUM_COMPLETE) == 1;
			boolean wildernessEasy = client.getVarbitValue(VarbitID.WILDERNESS_DIARY_EASY_COMPLETE) == 1;
			boolean kandarinEasy = client.getVarbitValue(VarbitID.KANDARIN_DIARY_EASY_COMPLETE) == 1;
			boolean westernEasy = client.getVarbitValue(VarbitID.WESTERN_DIARY_EASY_COMPLETE) == 1;
			boolean morytaniaMedium = client.getVarbitValue(VarbitID.MORYTANIA_DIARY_MEDIUM_COMPLETE) == 1;
			boolean kourendMedium = client.getVarbitValue(VarbitID.KOUREND_DIARY_MEDIUM_COMPLETE) == 1;

			addSimpleDailyTask(tasks, "battlestaves", varrockEasy,
				client.getVarbitValue(VarbitID.ZAFF_LAST_CLAIMED) == 0);
			addSimpleDailyTask(tasks, "sand", !isUim && handInSandDone,
				!isUim && handInSandDone && client.getVarbitValue(VarbitID.YANILLE_SAND_CLAIMED) == 0);
			addSimpleDailyTask(tasks, "essence", ardougneMedium,
				ardougneMedium && client.getVarbitValue(VarbitID.ARDOUGNE_FREE_ESSENCE) == 0);
			addSimpleDailyTask(tasks, "runes", wildernessEasy,
				wildernessEasy && client.getVarbitValue(VarbitID.LUNDAIL_LAST_CLAIMED) == 0);
			addSimpleDailyTask(tasks, "flax", kandarinEasy,
				kandarinEasy && client.getVarbitValue(VarbitID.SEERS_FREE_FLAX) == 0);
			addSimpleDailyTask(tasks, "arrows", westernEasy,
				westernEasy && client.getVarbitValue(VarbitID.WESTERN_RANTZ_ARROWS) == 0);
			addSimpleDailyTask(tasks, "dynamite", kourendMedium,
				kourendMedium && client.getVarbitValue(VarbitID.KOUREND_FREE_DYNAMITE) == 0);

			int bonemealMax = morytaniaMedium ? 13
				+ (client.getVarbitValue(VarbitID.MORYTANIA_DIARY_HARD_COMPLETE) == 1 ? 13
					+ (client.getVarbitValue(VarbitID.MORYTANIA_DIARY_ELITE_COMPLETE) == 1 ? 13 : 0) : 0) : 0;
			int bonemealCollected = client.getVarbitValue(VarbitID.MORYTANIA_SLIME_CLAIMED);
			Map<String, Object> bonemeal = new HashMap<>();
			bonemeal.put("requirementMet", morytaniaMedium);
			bonemeal.put("collected", bonemealCollected);
			bonemeal.put("max", bonemealMax);
			bonemeal.put("available", morytaniaMedium && bonemealCollected < bonemealMax);
			tasks.put("bonemeal", bonemeal);

			int nmzPoints = client.getVarpValue(VarPlayerID.NZONE_REWARDPOINTS);
			int herbBoxesPurchased = client.getVarbitValue(VarbitID.NZONE_HERBBOXES_PURCHASED);
			Map<String, Object> herbBoxes = new HashMap<>();
			herbBoxes.put("requirementMet", !isIronman && nmzPoints >= 9500);
			herbBoxes.put("purchased", herbBoxesPurchased);
			herbBoxes.put("max", 15);
			herbBoxes.put("available", !isIronman && nmzPoints >= 9500 && herbBoxesPurchased < 15);
			tasks.put("herbBoxes", herbBoxes);

			int togCountdown = client.getVarbitValue(VarbitID.TOG_COUNTDOWN);
			Map<String, Object> tog = new HashMap<>();
			tog.put("available", togCountdown == 0);
			tog.put("countdown", togCountdown);
			tasks.put("tearsOfGuthix", tog);
		}
		catch (Exception e)
		{
			log.error("Error getting daily tasks", e);
		}

		return tasks;
	}

	private void addSimpleDailyTask(Map<String, Object> tasks, String key, boolean requirementMet, boolean available)
	{
		Map<String, Object> task = new HashMap<>();
		task.put("requirementMet", requirementMet);
		task.put("available", available);
		tasks.put(key, task);
	}

	public Map<String, Object> getAllPlayerData()
	{
		Map<String, Object> data = new HashMap<>();

		try
		{
			data.put("username", getUsername());
			data.put("accountInfo", getAccountInfo());
			data.put("quests", getAllQuests());
			data.put("questPoints", getQuestPoints());
			data.put("combatLevel", getCombatLevel());
			data.put("stats", getStats());
			data.put("playerStatus", getPlayerStatus());
			data.put("achievementDiaries", getAchievementDiariesDetailed());
			data.put("combatAchievements", getCombatAchievements());
			data.put("collectionLog", getCollectionLog());
			data.put("slayerTask", getSlayerTask());
			data.put("birdhouseStatus", getBirdhouseStatus());
			data.put("dailyTasks", getDailyTasks());
			data.put("worldLocation", getWorldLocation());
		}
		catch (Exception e)
		{
			log.error("Error getting all player data", e);
		}

		return data;
	}

	private static class BankLayoutSlot
	{
		final int id;
		final int iconId;
		final String name;
		final int quantity;
		final int slot;
		final boolean placeholder;
		final boolean shouldReturn;

		BankLayoutSlot(int id, int iconId, String name, int quantity, int slot, boolean placeholder, boolean shouldReturn)
		{
			this.id = id;
			this.iconId = iconId;
			this.name = name;
			this.quantity = quantity;
			this.slot = slot;
			this.placeholder = placeholder;
			this.shouldReturn = shouldReturn;
		}

		Map<String, Object> toMap(int tabIndex)
		{
			Map<String, Object> map = new HashMap<>();
			map.put("id", id);
			map.put("iconId", iconId);
			map.put("name", name);
			map.put("quantity", quantity);
			map.put("slot", slot);
			map.put("tabIndex", tabIndex);
			map.put("placeholder", placeholder);
			return map;
		}
	}
}
