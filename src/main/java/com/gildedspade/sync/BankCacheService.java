package com.gildedspade.sync;

import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.Player;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class BankCacheService
{
	private final Client client;
	private final Map<Integer, ItemData> cachedBankItems = new LinkedHashMap<>();
	private String cachedBankUsername = "";

	@Inject
	BankCacheService(Client client)
	{
		this.client = client;
	}

	void cacheBankItems(ItemContainer bankContainer)
	{
		String username = getUsername();
		if (username.isEmpty())
		{
			return;
		}

		cachedBankItems.clear();
		cachedBankUsername = username;
		Item[] items = bankContainer.getItems();

		for (int i = 0; i < items.length; i++)
		{
			Item item = items[i];
			if (item == null || item.getId() == -1 || item.getQuantity() <= 0)
			{
				continue;
			}

			ItemComposition itemComp = client.getItemDefinition(item.getId());
			if (itemComp == null || itemComp.getPlaceholderTemplateId() != -1)
			{
				continue;
			}

			cachedBankItems.put(i, new ItemData(item.getId(), itemComp.getName(), item.getQuantity(), i));
		}
	}

	void clearBankCacheIfDifferentPlayer()
	{
		String username = getUsername();
		if (!username.isEmpty() && !cachedBankUsername.isEmpty() && !username.equals(cachedBankUsername))
		{
			clearBankCache();
		}
	}

	int getCachedItemCount()
	{
		return cachedBankItems.size();
	}

	List<Map<String, Object>> getBankedItems()
	{
		List<Map<String, Object>> items = new ArrayList<>();
		if (!bankCacheBelongsToCurrentPlayer())
		{
			clearBankCache();
			return items;
		}

		for (ItemData itemData : cachedBankItems.values())
		{
			Map<String, Object> item = new LinkedHashMap<>();
			item.put("id", itemData.id);
			item.put("name", itemData.name);
			item.put("quantity", itemData.quantity);
			item.put("slot", itemData.slot);
			item.put("tabIndex", 0);
			items.add(item);
		}

		return items;
	}

	private void clearBankCache()
	{
		cachedBankItems.clear();
		cachedBankUsername = "";
	}

	private boolean bankCacheBelongsToCurrentPlayer()
	{
		if (cachedBankItems.isEmpty())
		{
			return true;
		}

		String username = getUsername();
		return !username.isEmpty() && username.equals(cachedBankUsername);
	}

	private String getUsername()
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return "";
		}
		Player localPlayer = client.getLocalPlayer();
		if (localPlayer == null || localPlayer.getName() == null)
		{
			return "";
		}
		return localPlayer.getName();
	}

	private static class ItemData
	{
		final int id;
		final String name;
		final int quantity;
		final int slot;

		ItemData(int id, String name, int quantity, int slot)
		{
			this.id = id;
			this.name = name;
			this.quantity = quantity;
			this.slot = slot;
		}
	}
}
