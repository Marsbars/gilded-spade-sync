package com.gildedspade.sync;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.EnumComposition;
import net.runelite.api.StructComposition;
import net.runelite.api.gameval.VarPlayerID;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
class CombatAchievementService
{
	private static final int[] TIER_ENUM_IDS = {3981, 3982, 3983, 3984, 3985, 3986};
	private static final String[] TIER_NAMES = {
		"easy", "medium", "hard", "elite", "master", "grandmaster"
	};
	private static final int[] COMPLETION_VARPS = {
		VarPlayerID.CA_TASK_COMPLETED_0, VarPlayerID.CA_TASK_COMPLETED_1,
		VarPlayerID.CA_TASK_COMPLETED_2, VarPlayerID.CA_TASK_COMPLETED_3,
		VarPlayerID.CA_TASK_COMPLETED_4, VarPlayerID.CA_TASK_COMPLETED_5,
		VarPlayerID.CA_TASK_COMPLETED_6, VarPlayerID.CA_TASK_COMPLETED_7,
		VarPlayerID.CA_TASK_COMPLETED_8, VarPlayerID.CA_TASK_COMPLETED_9,
		VarPlayerID.CA_TASK_COMPLETED_10, VarPlayerID.CA_TASK_COMPLETED_11,
		VarPlayerID.CA_TASK_COMPLETED_12, VarPlayerID.CA_TASK_COMPLETED_13,
		VarPlayerID.CA_TASK_COMPLETED_14, VarPlayerID.CA_TASK_COMPLETED_15,
		VarPlayerID.CA_TASK_COMPLETED_16, VarPlayerID.CA_TASK_COMPLETED_17,
		VarPlayerID.CA_TASK_COMPLETED_18, VarPlayerID.CA_TASK_COMPLETED_19
	};

	private final Client client;
	private Map<String, List<TaskDefinition>> taskDefinitionsByTier;

	@Inject
	CombatAchievementService(Client client)
	{
		this.client = client;
	}

	Map<String, Object> getCombatAchievements()
	{
		Map<String, Object> combatAchievements = new HashMap<>();

		try
		{
			Map<String, List<TaskDefinition>> definitionsByTier = getTaskDefinitionsByTier();
			if (definitionsByTier == null)
			{
				return combatAchievements;
			}

			for (String tierName : TIER_NAMES)
			{
				Map<String, Object> tierData = new HashMap<>();
				List<Map<String, Object>> tasks = new ArrayList<>();
				int completedCount = 0;
				List<TaskDefinition> definitions = definitionsByTier.getOrDefault(tierName, Collections.emptyList());

				for (TaskDefinition definition : definitions)
				{
					boolean isCompleted = isTaskCompleted(definition);
					if (isCompleted)
					{
						completedCount++;
					}

					tasks.add(definition.toMap(isCompleted));
				}

				tierData.put("tasks", tasks);
				tierData.put("tasksComplete", completedCount);
				tierData.put("tasksTotal", definitions.size());

				combatAchievements.put(tierName, tierData);
			}

			int totalPoints = 0;
			for (int i = 0; i < TIER_NAMES.length; i++)
			{
				@SuppressWarnings("unchecked")
				Map<String, Object> tierData = (Map<String, Object>) combatAchievements.get(TIER_NAMES[i]);
				if (tierData != null)
				{
					int completed = (int) tierData.getOrDefault("tasksComplete", 0);
					totalPoints += completed * (i + 1);
				}
			}

			combatAchievements.put("totalPoints", totalPoints);
		}
		catch (Exception e)
		{
			log.error("Error getting combat achievements", e);
		}

		return combatAchievements;
	}

	private Map<String, List<TaskDefinition>> getTaskDefinitionsByTier()
	{
		if (taskDefinitionsByTier != null)
		{
			return taskDefinitionsByTier;
		}

		Map<String, List<TaskDefinition>> definitionsByTier = new LinkedHashMap<>();
		for (int tierIndex = 0; tierIndex < TIER_ENUM_IDS.length; tierIndex++)
		{
			int enumId = TIER_ENUM_IDS[tierIndex];
			String tierName = TIER_NAMES[tierIndex];
			EnumComposition tierEnum = client.getEnum(enumId);
			if (tierEnum == null)
			{
				log.warn("Enum {} for tier {} returned null", enumId, tierName);
				return null;
			}

			int[] structIds = tierEnum.getIntVals();
			List<TaskDefinition> definitions = new ArrayList<>(structIds.length);
			for (int structId : structIds)
			{
				try
				{
					StructComposition struct = client.getStructComposition(structId);
					int taskId = struct.getIntValue(1306);
					String taskName = struct.getStringValue(1308);
					int varpIndex = taskId / 32;
					int bitPosition = taskId % 32;

					if (varpIndex >= COMPLETION_VARPS.length)
					{
						log.warn("Task ID {} has varp index {} which exceeds array length {}",
							taskId, varpIndex, COMPLETION_VARPS.length);
					}

					definitions.add(new TaskDefinition(taskId, taskName, varpIndex, bitPosition));
				}
				catch (Exception e)
				{
					log.debug("Error caching combat achievement task {}: {}", structId, e.getMessage());
				}
			}

			definitionsByTier.put(tierName, Collections.unmodifiableList(definitions));
			log.debug("Cached {} combat achievement definitions for {} tier", definitions.size(), tierName);
		}

		taskDefinitionsByTier = Collections.unmodifiableMap(definitionsByTier);
		return taskDefinitionsByTier;
	}

	private boolean isTaskCompleted(TaskDefinition definition)
	{
		if (definition.varpIndex >= COMPLETION_VARPS.length)
		{
			return false;
		}

		int varpValue = client.getVarpValue(COMPLETION_VARPS[definition.varpIndex]);
		return (varpValue & (1 << definition.bitPosition)) != 0;
	}

	private static class TaskDefinition
	{
		final int taskId;
		final String taskName;
		final int varpIndex;
		final int bitPosition;

		TaskDefinition(int taskId, String taskName, int varpIndex, int bitPosition)
		{
			this.taskId = taskId;
			this.taskName = taskName;
			this.varpIndex = varpIndex;
			this.bitPosition = bitPosition;
		}

		Map<String, Object> toMap(boolean completed)
		{
			Map<String, Object> task = new HashMap<>();
			task.put("taskId", taskId);
			task.put("taskName", taskName);
			task.put("completed", completed);
			return task;
		}
	}
}
