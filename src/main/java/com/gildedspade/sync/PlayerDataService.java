package com.gildedspade.sync;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.VarPlayerID;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
class PlayerDataService
{
	private static final int QUEST_POINTS_VARP = 101;
	private static final int COMBAT_LEVEL_VARBIT = 13027;
	private static final int COLLECTION_LOG_COUNT_VARP = 2943;
	private static final int SPECIAL_ATTACK_PERCENT_VARP = 300;

	private final Client client;

	@Inject
	PlayerDataService(Client client)
	{
		this.client = client;
	}

	boolean isReady()
	{
		return client.getGameState() == GameState.LOGGED_IN && client.getLocalPlayer() != null;
	}

	String getUsername()
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

	List<Map<String, Object>> getAllQuests()
	{
		List<Map<String, Object>> allQuests = new ArrayList<>();

		if (!isReady())
		{
			return allQuests;
		}

		for (Quest quest : Quest.values())
		{
			try
			{
				QuestState state = quest.getState(client);
				Map<String, Object> questData = new HashMap<>();
				questData.put("id", quest.getId());
				questData.put("name", quest.getName());
				questData.put("state", state.name());
				allQuests.add(questData);
			}
			catch (Exception e)
			{
				log.debug("Error checking quest {}: {}", quest.getName(), e.getMessage());
			}
		}

		return allQuests;
	}

	int getQuestPoints()
	{
		if (!isReady())
		{
			return 0;
		}
		return client.getVarpValue(QUEST_POINTS_VARP);
	}

	int getCombatLevel()
	{
		if (!isReady())
		{
			return 0;
		}
		return client.getVarbitValue(COMBAT_LEVEL_VARBIT);
	}

	Map<String, Map<String, Object>> getStats()
	{
		Map<String, Map<String, Object>> stats = new HashMap<>();

		if (!isReady())
		{
			return stats;
		}

		for (Skill skill : Skill.values())
		{
			try
			{
				Map<String, Object> skillData = new HashMap<>();
				skillData.put("level", client.getRealSkillLevel(skill));
				skillData.put("xp", client.getSkillExperience(skill));
				skillData.put("boostedLevel", client.getBoostedSkillLevel(skill));

				stats.put(skill.getName().toLowerCase().replace(" ", "_"), skillData);
			}
			catch (Exception e)
			{
				log.debug("Error getting stat for {}: {}", skill.getName(), e.getMessage());
			}
		}

		return stats;
	}

	Map<String, Object> getStatsPayload()
	{
		Map<String, Object> payload = new HashMap<>();
		payload.put("skills", getStats());
		payload.put("status", getPlayerStatus());
		return payload;
	}

	Map<String, Object> getPlayerStatus()
	{
		Map<String, Object> status = new HashMap<>();

		if (!isReady())
		{
			return status;
		}

		Map<String, Object> hitpoints = new HashMap<>();
		hitpoints.put("current", client.getBoostedSkillLevel(Skill.HITPOINTS));
		hitpoints.put("base", client.getRealSkillLevel(Skill.HITPOINTS));
		status.put("hitpoints", hitpoints);

		Map<String, Object> prayer = new HashMap<>();
		prayer.put("current", client.getBoostedSkillLevel(Skill.PRAYER));
		prayer.put("base", client.getRealSkillLevel(Skill.PRAYER));
		status.put("prayer", prayer);

		status.put("runEnergy", normalizePercentage(client.getEnergy()));
		status.put("specialAttack", normalizePercentage(client.getVarpValue(SPECIAL_ATTACK_PERCENT_VARP)));

		return status;
	}

	Map<String, Integer> getCollectionLog()
	{
		Map<String, Integer> collectionLog = new HashMap<>();

		if (!isReady())
		{
			return collectionLog;
		}

		collectionLog.put("unique_obtained", client.getVarpValue(COLLECTION_LOG_COUNT_VARP));

		return collectionLog;
	}

	Map<String, Object> getAccountInfo()
	{
		Map<String, Object> info = new HashMap<>();

		if (!isReady())
		{
			return info;
		}

		try
		{
			net.runelite.api.vars.AccountType accountType = client.getAccountType();
			info.put("type", accountType.name().toLowerCase());

			if (accountType.isGroupIronman())
			{
				int groupSize = client.getVarbitValue(net.runelite.api.gameval.VarbitID.GIM_GROUPSIZE);
				if (groupSize > 0)
				{
					info.put("groupSize", groupSize);
				}

				net.runelite.api.clan.ClanSettings groupSettings = client.getClanSettings(net.runelite.api.clan.ClanID.GROUP_IRONMAN);
				if (groupSettings != null)
				{
					info.put("groupName", groupSettings.getName());

					List<String> memberNames = new ArrayList<>();
					for (net.runelite.api.clan.ClanMember member : groupSettings.getMembers())
					{
						memberNames.add(member.getName());
					}
					info.put("members", memberNames);
				}
			}
		}
		catch (Exception e)
		{
			log.error("Error getting account info", e);
		}

		return info;
	}

	Map<String, Integer> getWorldLocation()
	{
		Map<String, Integer> location = new HashMap<>();

		if (!isReady())
		{
			return location;
		}

		WorldPoint point = client.getLocalPlayer().getWorldLocation();
		location.put("x", point.getX());
		location.put("y", point.getY());
		location.put("plane", point.getPlane());
		location.put("regionId", point.getRegionID());

		return location;
	}

	private int normalizePercentage(int value)
	{
		return value > 100 ? value / 10 : value;
	}
}
