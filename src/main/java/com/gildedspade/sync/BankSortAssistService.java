package com.gildedspade.sync;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
class BankSortAssistService
{
	private final Client client;
	private final ClientThread clientThread;
	private final Consumer<List<Integer>> bankFilterSetter;
	private final Consumer<Map<String, Object>> statusBroadcaster;

	private volatile BankSortAssistSession bankSortAssistSession;
	private boolean bankSortAssistModeRefreshQueued;
	private List<Integer> currentFilterIds = Collections.emptyList();

	@Inject
	BankSortAssistService(Client client, ClientThread clientThread,
		Consumer<List<Integer>> bankFilterSetter, Consumer<Map<String, Object>> statusBroadcaster)
	{
		this.client = client;
		this.clientThread = clientThread;
		this.bankFilterSetter = bankFilterSetter;
		this.statusBroadcaster = statusBroadcaster;
	}

	void startBankSortAssist(List<Map<String, Object>> items)
	{
		clientThread.invoke(() ->
		{
			List<BankSortAssistItem> assistItems = new ArrayList<>();
			for (Map<String, Object> item : items)
			{
				int id = getIntValue(item.get("id"), -1);
				if (id <= 0)
				{
					continue;
				}

				String name = getStringValue(item.get("name"), "Item " + id);
				String targetTabId = getStringValue(item.get("targetTabId"), "main");
				String targetTabName = getStringValue(item.get("targetTabName"), "Main");
				int targetPosition = getIntValue(item.get("targetPosition"), assistItems.size());
				String requiredMoveMode = getMoveModeValue(item.get("requiredMoveMode"));
				String actionType = getSortActionType(item.get("actionType"));
				int anchorItemId = getIntValue(item.get("anchorItemId"), -1);
				String anchorItemName = getStringValue(item.get("anchorItemName"), "");

				assistItems.add(new BankSortAssistItem(id, name, targetTabId, targetTabName, targetPosition,
					requiredMoveMode, actionType, anchorItemId, anchorItemName));
			}

			bankSortAssistSession = new BankSortAssistSession(assistItems);
			applyCurrentBankSortAssistStep();
			broadcastBankSortAssistStatus();
		});
	}

	void stopBankSortAssist()
	{
		clientThread.invoke(() ->
		{
			if (bankSortAssistSession != null)
			{
				bankSortAssistSession.active = false;
			}
			currentFilterIds = Collections.emptyList();
			bankFilterSetter.accept(currentFilterIds);
			broadcastBankSortAssistStatus();
		});
	}

	void skipBankSortAssistItem()
	{
		clientThread.invoke(() ->
		{
			if (bankSortAssistSession == null || !bankSortAssistSession.active)
			{
				broadcastBankSortAssistStatus();
				return;
			}

			bankSortAssistSession.skipCurrent();
			applyCurrentBankSortAssistStep();
			broadcastBankSortAssistStatus();
		});
	}

	void confirmBankSortAssistItem()
	{
		clientThread.invoke(() ->
		{
			completeCurrentBankSortAssistStep();
			broadcastBankSortAssistStatus();
		});
	}

	Map<String, Object> getBankSortAssistStatus()
	{
		Map<String, Object> status = new HashMap<>();
		BankSortAssistSession session = bankSortAssistSession;
		if (session == null)
		{
			status.put("active", false);
			status.put("total", 0);
			status.put("completed", 0);
			status.put("skipped", 0);
			status.put("remaining", 0);
			return status;
		}

		status.put("active", session.active);
		status.put("total", session.items.size());
		status.put("completed", session.completedCount);
		status.put("skipped", session.skippedCount);
		status.put("remaining", Math.max(0, session.items.size() - session.currentIndex));
		status.put("currentIndex", session.currentIndex);

		BankSortAssistItem current = session.getCurrent();
		if (current != null && session.active)
		{
			status.put("currentStep", current.toMap());
			status.put("currentMoveMode", getCurrentBankSortAssistMoveMode());
			status.put("awaitingMoveMode", !isBankSortAssistMoveModeSatisfied(current));
		}

		return status;
	}

	void advanceBankSortAssistAfterBankChange()
	{
		if (bankSortAssistSession == null || !bankSortAssistSession.active)
		{
			return;
		}

		if (!bankSortAssistSession.awaitingBankChange)
		{
			applyCurrentBankSortAssistStep();
			broadcastBankSortAssistStatus();
			return;
		}

		completeCurrentBankSortAssistStep();
		broadcastBankSortAssistStatus();
	}

	void handleVarbitChanged(int varbitId, int varpId)
	{
		if (varbitId != VarbitID.BANK_INSERTMODE && varpId != net.runelite.api.gameval.VarPlayerID.BANKINSERT)
		{
			return;
		}

		BankSortAssistItem current = getCurrentBankSortAssistItem();
		if (current == null || current.requiredMoveMode == null || bankSortAssistSession == null || bankSortAssistModeRefreshQueued)
		{
			return;
		}

		bankSortAssistModeRefreshQueued = true;
		clientThread.invokeLater(() ->
		{
			bankSortAssistModeRefreshQueued = false;
			BankSortAssistItem refreshedCurrent = getCurrentBankSortAssistItem();
			if (refreshedCurrent == null || refreshedCurrent.requiredMoveMode == null || bankSortAssistSession == null)
			{
				return;
			}

			boolean modeSatisfied = isBankSortAssistMoveModeSatisfied(refreshedCurrent);
			if (!modeSatisfied || !bankSortAssistSession.awaitingBankChange)
			{
				applyCurrentBankSortAssistStep();
				broadcastBankSortAssistStatus();
			}
		});
	}

	BankSortAssistItem getCurrentBankSortAssistItem()
	{
		return bankSortAssistSession == null ? null : bankSortAssistSession.getCurrent();
	}

	boolean isAwaitingBankSortAssistMoveMode()
	{
		BankSortAssistItem current = getCurrentBankSortAssistItem();
		return current != null && !isBankSortAssistMoveModeSatisfied(current);
	}

	private void completeCurrentBankSortAssistStep()
	{
		if (bankSortAssistSession == null || !bankSortAssistSession.active)
		{
			return;
		}
		if (!isBankSortAssistMoveModeSatisfied(bankSortAssistSession.getCurrent()))
		{
			applyCurrentBankSortAssistStep();
			return;
		}

		bankSortAssistSession.completeCurrent();
		applyCurrentBankSortAssistStep();
	}

	private void applyCurrentBankSortAssistStep()
	{
		if (bankSortAssistSession == null)
		{
			return;
		}

		BankSortAssistItem current = bankSortAssistSession.getCurrent();
		if (current == null)
		{
			bankSortAssistSession.active = false;
			bankSortAssistSession.awaitingBankChange = false;
			currentFilterIds = Collections.emptyList();
			bankFilterSetter.accept(currentFilterIds);
			return;
		}
		if (!isBankSortAssistMoveModeSatisfied(current))
		{
			bankSortAssistSession.awaitingBankChange = false;
			if (!currentFilterIds.isEmpty())
			{
				currentFilterIds = Collections.emptyList();
				bankFilterSetter.accept(currentFilterIds);
			}
			return;
		}

		Widget bankContainer = client.getWidget(InterfaceID.Bankmain.ITEMS);
		if (bankContainer == null || bankContainer.isHidden())
		{
			bankSortAssistSession.awaitingBankChange = false;
			return;
		}

		bankSortAssistSession.awaitingBankChange = true;
		List<Integer> filterIds = new ArrayList<>();
		filterIds.add(current.id);
		if (current.anchorItemId > 0 && current.anchorItemId != current.id)
		{
			filterIds.add(current.anchorItemId);
		}
		currentFilterIds = filterIds;
		bankFilterSetter.accept(filterIds);
	}

	private void broadcastBankSortAssistStatus()
	{
		statusBroadcaster.accept(getBankSortAssistStatus());
	}

	private boolean isBankSortAssistMoveModeSatisfied(BankSortAssistItem item)
	{
		if (item == null || item.requiredMoveMode == null)
		{
			return true;
		}

		return item.requiredMoveMode.equals(getCurrentBankSortAssistMoveMode());
	}

	private String getCurrentBankSortAssistMoveMode()
	{
		return client.getVarbitValue(VarbitID.BANK_INSERTMODE) == 1 ? "insert" : "swap";
	}

	private int getIntValue(Object value, int fallback)
	{
		if (value instanceof Number)
		{
			return ((Number) value).intValue();
		}
		return fallback;
	}

	private String getStringValue(Object value, String fallback)
	{
		return value instanceof String && !((String) value).isBlank() ? (String) value : fallback;
	}

	private String getMoveModeValue(Object value)
	{
		if (!(value instanceof String))
		{
			return null;
		}

		String moveMode = ((String) value).trim().toLowerCase();
		return moveMode.equals("insert") || moveMode.equals("swap") ? moveMode : null;
	}

	private String getSortActionType(Object value)
	{
		if (!(value instanceof String))
		{
			return "move-tab";
		}

		String actionType = ((String) value).trim().toLowerCase();
		return actionType.equals("insert-before") || actionType.equals("insert-after") || actionType.equals("swap")
			? actionType
			: "move-tab";
	}

	private static class BankSortAssistSession
	{
		final List<BankSortAssistItem> items;
		volatile int currentIndex;
		volatile int completedCount;
		volatile int skippedCount;
		volatile boolean active;
		volatile boolean awaitingBankChange;

		BankSortAssistSession(List<BankSortAssistItem> items)
		{
			this.items = items;
			this.active = !items.isEmpty();
		}

		BankSortAssistItem getCurrent()
		{
			if (!active || currentIndex < 0 || currentIndex >= items.size())
			{
				return null;
			}
			return items.get(currentIndex);
		}

		private void advance()
		{
			if (getCurrent() == null)
			{
				active = false;
				awaitingBankChange = false;
				return;
			}
			currentIndex++;
			awaitingBankChange = false;
			if (currentIndex >= items.size())
			{
				active = false;
			}
		}

		void completeCurrent()
		{
			completedCount++;
			advance();
		}

		void skipCurrent()
		{
			skippedCount++;
			advance();
		}
	}

	static class BankSortAssistItem
	{
		final int id;
		final String name;
		final String targetTabId;
		final String targetTabName;
		final int targetPosition;
		final String requiredMoveMode;
		final String actionType;
		final int anchorItemId;
		final String anchorItemName;

		BankSortAssistItem(int id, String name, String targetTabId, String targetTabName, int targetPosition,
			String requiredMoveMode, String actionType, int anchorItemId, String anchorItemName)
		{
			this.id = id;
			this.name = name;
			this.targetTabId = targetTabId;
			this.targetTabName = targetTabName;
			this.targetPosition = targetPosition;
			this.requiredMoveMode = requiredMoveMode;
			this.actionType = actionType;
			this.anchorItemId = anchorItemId;
			this.anchorItemName = anchorItemName;
		}

		Map<String, Object> toMap()
		{
			Map<String, Object> map = new HashMap<>();
			map.put("id", id);
			map.put("name", name);
			map.put("targetTabId", targetTabId);
			map.put("targetTabName", targetTabName);
			map.put("targetPosition", targetPosition);
			map.put("actionType", actionType);
			if (requiredMoveMode != null)
			{
				map.put("requiredMoveMode", requiredMoveMode);
			}
			if (anchorItemId > 0)
			{
				map.put("anchorItemId", anchorItemId);
				map.put("anchorItemName", anchorItemName);
			}
			return map;
		}
	}
}
