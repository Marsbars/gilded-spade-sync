package com.gildedspade.sync;

import net.runelite.api.gameval.VarPlayerID;
import java.util.HashMap;
import java.util.Map;

/**
 * Maps achievement diary tasks to their varplayer IDs and bit positions.
 * Based on quest-helper repository patterns.
 *
 * Each diary uses 1-2 varplayer IDs:
 * - Primary varplayer: Stores individual task completion as bits
 * - Secondary varplayer (if needed): Additional tasks when bits exceed 32
 *
 * Bit positions are NOT sequential and can have gaps. Must be manually mapped
 * from official implementation (quest-helper source).
 *
 * IMPLEMENTATION STATUS: COMPLETE - ALL 48 DIARY TIERS MAPPED (12 diaries x 4 tiers)
 *
 * [x] Varrock: All 4 tiers complete (14/13/10/5 tasks)
 * [x] Ardougne: All 4 tiers complete (10/13/12/8 tasks)
 * [x] Desert: All 4 tiers complete (9/12/10/5 tasks)
 * [x] Falador: All 4 tiers complete (11/14/11/6 tasks)
 * [x] Fremennik: All 4 tiers complete (10/9/9/6 tasks)
 * [x] Kandarin: All 4 tiers complete (11/14/11/7 tasks)
 * [x] Morytania: All 4 tiers complete (11/11/10/6 tasks)
 * [x] Wilderness: All 4 tiers complete (12/11/10/7 tasks)
 * [x] Kourend & Kebos: All 4 tiers complete (12/13/10/8 tasks)
 * [x] Lumbridge & Draynor: All 4 tiers complete (12/12/11/6 tasks)
 * [x] Western Provinces: All 4 tiers complete (11/13/13/7 tasks)
 * [x] Karamja: All 4 tiers complete (10/19/10/5 tasks) - uses individual VarbitIDs per task
 *     - Easy/Medium/Hard: TaskMapping.ofVarbit() with VarbitID.ATJUN_EASY/MED/HARD_*
 *     - BANANA, SEAWEED, PALM are count-based (done when varbitValue >= 5)
 *     - Elite: VarPlayerID.ATJUN_TASKS_4 bits 1-5 (standard varplayer bitpacking)
 *
 * Source: quest-helper repository (Zoinkwiz/quest-helper)
 * https://github.com/Zoinkwiz/quest-helper/tree/master/src/main/java/com/questhelper/helpers/achievementdiaries
 */
public class DiaryVarplayerMapping
{
	// Inner class to store varplayer ID, bit position, and task description
	public static class TaskMapping
	{
		public final int varplayerId;  // varplayer ID, or varbit ID when bitPosition == -1
		public final int bitPosition;  // -1 signals a varbit-based task
		public final String description;
		public final int varbitMinValue;  // minimum varbit value to be "complete" (varbit tasks only)

		// Constructor with description (preferred)
		public TaskMapping(int varplayerId, int bitPosition, String description)
		{
			this.varplayerId = varplayerId;
			this.bitPosition = bitPosition;
			this.description = description;
			this.varbitMinValue = 0;
		}

		// Backwards-compatible constructor without description
		// TODO: Remove once all diaries have descriptions extracted from quest-helper
		public TaskMapping(int varplayerId, int bitPosition)
		{
			this(varplayerId, bitPosition, "TODO: Extract from quest-helper");
		}

		private TaskMapping(int varbitId, String description, int minValue)
		{
			this.varplayerId = varbitId;
			this.bitPosition = -1;
			this.description = description;
			this.varbitMinValue = minValue;
		}

		/** Factory for a varbit task completed when varbitValue >= 1. */
		public static TaskMapping ofVarbit(int varbitId, String description)
		{
			return new TaskMapping(varbitId, description, 1);
		}

		/** Factory for a varbit task completed when varbitValue >= minCompletedValue. */
		public static TaskMapping ofVarbit(int varbitId, String description, int minCompletedValue)
		{
			return new TaskMapping(varbitId, description, minCompletedValue);
		}
	}
	
	// Map structure: diaryName -> tierName -> task index -> TaskMapping
	private static final Map<String, Map<String, TaskMapping[]>> DIARY_MAPPINGS = new HashMap<>();
	
	static
	{
		initializeArdougneMappings();
		initializeDesertMappings();
		initializeFaladorMappings();
		initializeFremennikMappings();
		initializeKandarinMappings();
		initializeKaramjaMappings();
		initializeKourendMappings();
		initializeLumbridgeMappings();
		initializeMorytaniaMappings();
		initializeVarrockMappings();
		initializeWesternMappings();
		initializeWildernessMappings();
	}
	
	private static void initializeArdougneMappings()
	{
		Map<String, TaskMapping[]> ardougne = new HashMap<>();
		
		// Ardougne Easy - VarPlayerID.ARDOUNGE_ACHIEVEMENT_DIARY
		// Note: Bit positions are NOT sequential! Gaps at bits 3, 8, 10
		ardougne.put("easy", new TaskMapping[]{
			new TaskMapping(VarPlayerID.ARDOUNGE_ACHIEVEMENT_DIARY, 0, "Have Wizard Cromperty teleport you to the Rune essence mine"),
			new TaskMapping(VarPlayerID.ARDOUNGE_ACHIEVEMENT_DIARY, 1, "Steal a cake from the East Ardougne market stalls"),
			new TaskMapping(VarPlayerID.ARDOUNGE_ACHIEVEMENT_DIARY, 2, "Sell silk to the Silk trader in East Ardougne"),
			new TaskMapping(VarPlayerID.ARDOUNGE_ACHIEVEMENT_DIARY, 4, "Use the altar in East Ardougne's church"),
			new TaskMapping(VarPlayerID.ARDOUNGE_ACHIEVEMENT_DIARY, 5, "Go out fishing on the Fishing Trawler"),
			new TaskMapping(VarPlayerID.ARDOUNGE_ACHIEVEMENT_DIARY, 6, "Enter the Combat Training Camp north of West Ardougne"),
			new TaskMapping(VarPlayerID.ARDOUNGE_ACHIEVEMENT_DIARY, 7, "Have Tindel Marchant identify a rusty sword for you"),
			new TaskMapping(VarPlayerID.ARDOUNGE_ACHIEVEMENT_DIARY, 9, "Use the Ardougne lever to teleport to the Wilderness"),
			new TaskMapping(VarPlayerID.ARDOUNGE_ACHIEVEMENT_DIARY, 11, "View Aleck's Hunter Emporium in Yanille"),
			new TaskMapping(VarPlayerID.ARDOUNGE_ACHIEVEMENT_DIARY, 12, "Check what pets you have insured with Probita in East Ardougne"),
		});
		
		// Ardougne Medium - VarPlayerID.ARDOUNGE_ACHIEVEMENT_DIARY bits 13-26 (skip 22)
		ardougne.put("medium", new TaskMapping[]{
			new TaskMapping(VarPlayerID.ARDOUNGE_ACHIEVEMENT_DIARY, 13, "Enter the unicorn pen in Ardougne Zoo using Fairy rings"),
			new TaskMapping(VarPlayerID.ARDOUNGE_ACHIEVEMENT_DIARY, 14, "Grapple up Yanille's south wall"),
			new TaskMapping(VarPlayerID.ARDOUNGE_ACHIEVEMENT_DIARY, 15, "Harvest some strawberries from the Ardougne farming patch"),
			new TaskMapping(VarPlayerID.ARDOUNGE_ACHIEVEMENT_DIARY, 16, "Cast the Teleport to Ardougne spell"),
			new TaskMapping(VarPlayerID.ARDOUNGE_ACHIEVEMENT_DIARY, 17, "Travel to Castlewars and back to Ardougne via hot air balloon three times in a row"),
			new TaskMapping(VarPlayerID.ARDOUNGE_ACHIEVEMENT_DIARY, 18, "Claim buckets of sand from Bert in Yanille"),
			new TaskMapping(VarPlayerID.ARDOUNGE_ACHIEVEMENT_DIARY, 19, "Catch any fish on the Fishing Platform"),
			new TaskMapping(VarPlayerID.ARDOUNGE_ACHIEVEMENT_DIARY, 20, "Pickpocket the Master Farmer north of East Ardougne"),
			new TaskMapping(VarPlayerID.ARDOUNGE_ACHIEVEMENT_DIARY, 21, "Collect some cave nightshade from the Skavid caves"),
			new TaskMapping(VarPlayerID.ARDOUNGE_ACHIEVEMENT_DIARY, 23, "Kill a swordchick in the Tower of Life"),
			new TaskMapping(VarPlayerID.ARDOUNGE_ACHIEVEMENT_DIARY, 24, "Upgrade Iban's staff"),
			new TaskMapping(VarPlayerID.ARDOUNGE_ACHIEVEMENT_DIARY, 25, "Visit the island east of the Necromancer Tower"),
			new TaskMapping(VarPlayerID.ARDOUNGE_ACHIEVEMENT_DIARY, 26, "Pickpocket a knight in Ardougne"),
		});
		
		// Ardougne Hard - bits 26-31 primary, then DIARY2 bits 0-5
		ardougne.put("hard", new TaskMapping[]{
			new TaskMapping(VarPlayerID.ARDOUNGE_ACHIEVEMENT_DIARY, 26, "Recharge some jewellery at the Totem in the Legends' Guild"),
			new TaskMapping(VarPlayerID.ARDOUNGE_ACHIEVEMENT_DIARY, 27, "Enter the Magic Guild"),
			new TaskMapping(VarPlayerID.ARDOUNGE_ACHIEVEMENT_DIARY, 28, "Attempt to steal from a chest in Ardougne castle"),
			new TaskMapping(VarPlayerID.ARDOUNGE_ACHIEVEMENT_DIARY, 29, "Equip a Karamja monkey greegree in the Ardougne Zoo"),
			new TaskMapping(VarPlayerID.ARDOUNGE_ACHIEVEMENT_DIARY, 30, "Teleport to the Watchtower"),
			new TaskMapping(VarPlayerID.ARDOUNGE_ACHIEVEMENT_DIARY, 31, "Catch a red salamander"),
			new TaskMapping(VarPlayerID.ARDOUNGE_ACHIEVEMENT_DIARY2, 0, "Check the health of a palm tree by the Tree Gnome Village"),
			new TaskMapping(VarPlayerID.ARDOUNGE_ACHIEVEMENT_DIARY2, 1, "Pick some poison ivy berries from the patch south of Ardougne"),
			new TaskMapping(VarPlayerID.ARDOUNGE_ACHIEVEMENT_DIARY2, 2, "Smith a mithril platebody in Yanille"),
			new TaskMapping(VarPlayerID.ARDOUNGE_ACHIEVEMENT_DIARY2, 3, "Enter your POH from Yanille"),
			new TaskMapping(VarPlayerID.ARDOUNGE_ACHIEVEMENT_DIARY2, 4, "Smith a Dragon square shield in West Ardougne"),
			new TaskMapping(VarPlayerID.ARDOUNGE_ACHIEVEMENT_DIARY2, 5, "Craft some death runes"),
		});
		
		// Ardougne Elite - DIARY2 bits 6-13
		ardougne.put("elite", new TaskMapping[]{
			new TaskMapping(VarPlayerID.ARDOUNGE_ACHIEVEMENT_DIARY2, 6, "Catch a manta ray in the Fishing Trawler and cook it in Port Khazard"),
			new TaskMapping(VarPlayerID.ARDOUNGE_ACHIEVEMENT_DIARY2, 7, "Complete a lap of Ardougne's rooftop agility course"),
			new TaskMapping(VarPlayerID.ARDOUNGE_ACHIEVEMENT_DIARY2, 8, "Make a rune crossbow from scratch in and around Ardougne"),
			new TaskMapping(VarPlayerID.ARDOUNGE_ACHIEVEMENT_DIARY2, 9, "Pickpocket a hero in Ardougne"),
			new TaskMapping(VarPlayerID.ARDOUNGE_ACHIEVEMENT_DIARY2, 10, "Imbue a Salve amulet at the Nightmare Zone"),
			new TaskMapping(VarPlayerID.ARDOUNGE_ACHIEVEMENT_DIARY2, 11, "Harvest a torstol from the Ardougne herb patch"),
			new TaskMapping(VarPlayerID.ARDOUNGE_ACHIEVEMENT_DIARY2, 12, "Cast Ice Barrage on another player within Castle Wars"),
			new TaskMapping(VarPlayerID.ARDOUNGE_ACHIEVEMENT_DIARY2, 13, "Pick the lock to the Yanille Agility Dungeon"),
		});
		
		DIARY_MAPPINGS.put("ardougne", ardougne);
	}
	
	private static void initializeDesertMappings()
	{
		Map<String, TaskMapping[]> desert = new HashMap<>();
		
		// Desert Easy - bits 1-9
		desert.put("easy", new TaskMapping[]{
			new TaskMapping(VarPlayerID.DESERT_ACHIEVEMENT_DIARY, 1, "Catch a Golden Warbler in the Uzer hunter area"),
			new TaskMapping(VarPlayerID.DESERT_ACHIEVEMENT_DIARY, 2, "Mine five clay in the north east of the desert"),
			new TaskMapping(VarPlayerID.DESERT_ACHIEVEMENT_DIARY, 3, "Enter the Kalphite Hive"),
			new TaskMapping(VarPlayerID.DESERT_ACHIEVEMENT_DIARY, 4, "Enter the desert (equipped with Desert gear)"),
			new TaskMapping(VarPlayerID.DESERT_ACHIEVEMENT_DIARY, 5, "Kill a vulture"),
			new TaskMapping(VarPlayerID.DESERT_ACHIEVEMENT_DIARY, 6, "Have Zahur in Nardah clean a grimy herb for you"),
			new TaskMapping(VarPlayerID.DESERT_ACHIEVEMENT_DIARY, 7, "Pickup and drop cacti until task is completed"),
			new TaskMapping(VarPlayerID.DESERT_ACHIEVEMENT_DIARY, 8, "Sell your pyramid plunder artefact to Simon Templeton"),
			new TaskMapping(VarPlayerID.DESERT_ACHIEVEMENT_DIARY, 9, "Open the sarcophagus in the first room of Pyramid Plunder"),
		});
		
		// Desert Medium - bits 12-23
		desert.put("medium", new TaskMapping[]{
			new TaskMapping(VarPlayerID.DESERT_ACHIEVEMENT_DIARY, 12, "Climb the Agility Pyramid and collect the pyramid top"),
			new TaskMapping(VarPlayerID.DESERT_ACHIEVEMENT_DIARY, 13, "Use an Ice cooler on a Desert Lizard"),
			new TaskMapping(VarPlayerID.DESERT_ACHIEVEMENT_DIARY, 14, "Catch an Orange Salamander in the Uzer hunter area"),
			new TaskMapping(VarPlayerID.DESERT_ACHIEVEMENT_DIARY, 15, "Pluck a feather from a Desert Phoenix"),
			new TaskMapping(VarPlayerID.DESERT_ACHIEVEMENT_DIARY, 16, "Travel to Uzer by magic carpet"),
			new TaskMapping(VarPlayerID.DESERT_ACHIEVEMENT_DIARY, 17, "Use a rope on the Desert Eagle to travel to the Desert"),
			new TaskMapping(VarPlayerID.DESERT_ACHIEVEMENT_DIARY, 18, "Pray at the Elidinis Statuette in Nardah"),
			new TaskMapping(VarPlayerID.DESERT_ACHIEVEMENT_DIARY, 19, "Create a combat potion in the desert"),
			new TaskMapping(VarPlayerID.DESERT_ACHIEVEMENT_DIARY, 20, "Teleport to Enakhra's Temple with the Camulet"),
			new TaskMapping(VarPlayerID.DESERT_ACHIEVEMENT_DIARY, 21, "Visit the genie beneath Nardah"),
			new TaskMapping(VarPlayerID.DESERT_ACHIEVEMENT_DIARY, 22, "Teleport to Pollnivneach"),
			new TaskMapping(VarPlayerID.DESERT_ACHIEVEMENT_DIARY, 23, "Chop some teak logs near Uzer"),
		});
		
		// Desert Hard - bits 24-31, then DIARY2 0-1
		desert.put("hard", new TaskMapping[]{
			new TaskMapping(VarPlayerID.DESERT_ACHIEVEMENT_DIARY, 24, "Knockout and pickpocket a Menaphite Thug"),
			new TaskMapping(VarPlayerID.DESERT_ACHIEVEMENT_DIARY, 25, "Mine granite in the mine south of the Bandit Camp"),
			new TaskMapping(VarPlayerID.DESERT_ACHIEVEMENT_DIARY, 26, "Refill your waterskins in the Desert using the Humidify spell"),
			new TaskMapping(VarPlayerID.DESERT_ACHIEVEMENT_DIARY, 27, "Kill the Kalphite Queen"),
			new TaskMapping(VarPlayerID.DESERT_ACHIEVEMENT_DIARY, 28, "Complete a lap of the Pollnivneach Rooftop course"),
			new TaskMapping(VarPlayerID.DESERT_ACHIEVEMENT_DIARY, 29, "Kill a Dust devil with a Slayer helmet equipped"),
			new TaskMapping(VarPlayerID.DESERT_ACHIEVEMENT_DIARY, 30, "Activate Ancient Magicks by praying at the Jaldraocht altar"),
			new TaskMapping(VarPlayerID.DESERT_ACHIEVEMENT_DIARY, 31, "Kill a Locust rider with Keris"),
			new TaskMapping(VarPlayerID.DESERT_ACHIEVEMENT_DIARY2, 0, "Burn yew logs on the balcony in Nardah"),
			new TaskMapping(VarPlayerID.DESERT_ACHIEVEMENT_DIARY2, 1, "Smith a Mithril platebody in Nardah"),
		});
		
		// Desert Elite - DIARY2 bits 2,4-8 (note: bit 3 skipped)
		desert.put("elite", new TaskMapping[]{
			new TaskMapping(VarPlayerID.DESERT_ACHIEVEMENT_DIARY2, 2, "Bake a wild pie at the Nardah clay oven"),
			new TaskMapping(VarPlayerID.DESERT_ACHIEVEMENT_DIARY2, 4, "Cast Ice Barrage against a foe in the Desert"),
			new TaskMapping(VarPlayerID.DESERT_ACHIEVEMENT_DIARY2, 5, "Fletch dragon darts in the Kharidian Desert"),
			new TaskMapping(VarPlayerID.DESERT_ACHIEVEMENT_DIARY2, 6, "Talk to the mounted Kalphite Queen head in your POH"),
			new TaskMapping(VarPlayerID.DESERT_ACHIEVEMENT_DIARY2, 7, "Loot the grand gold chest in Pyramid Plunder"),
			new TaskMapping(VarPlayerID.DESERT_ACHIEVEMENT_DIARY2, 8, "Restore at least 85 prayer points at the Sophanem altar"),
		});
		
		DIARY_MAPPINGS.put("desert", desert);
	}
	
	private static void initializeFaladorMappings()
	{
		Map<String, TaskMapping[]> falador = new HashMap<>();
		
		// Falador Easy - bits 0-10 (starts at 0!)
		falador.put("easy", new TaskMapping[]{
			new TaskMapping(VarPlayerID.FALADOR_ACHIEVEMENT_DIARY, 0, "Find out what your family crest is from Sir Renitee"),
			new TaskMapping(VarPlayerID.FALADOR_ACHIEVEMENT_DIARY, 1, "Climb over the western Falador wall"),
			new TaskMapping(VarPlayerID.FALADOR_ACHIEVEMENT_DIARY, 2, "Browse Sarah's Farming Shop"),
			new TaskMapping(VarPlayerID.FALADOR_ACHIEVEMENT_DIARY, 3, "Get a Haircut from the Falador hairdresser"),
			new TaskMapping(VarPlayerID.FALADOR_ACHIEVEMENT_DIARY, 4, "Fill a bucket from the pump north of Falador Park"),
			new TaskMapping(VarPlayerID.FALADOR_ACHIEVEMENT_DIARY, 5, "Kill a duck in Falador park"),
			new TaskMapping(VarPlayerID.FALADOR_ACHIEVEMENT_DIARY, 6, "Make a mind tiara"),
			new TaskMapping(VarPlayerID.FALADOR_ACHIEVEMENT_DIARY, 7, "Take the boat to Entrana"),
			new TaskMapping(VarPlayerID.FALADOR_ACHIEVEMENT_DIARY, 8, "Repair a broken strut in the Motherlode Mine"),
			new TaskMapping(VarPlayerID.FALADOR_ACHIEVEMENT_DIARY, 9, "Claim a security book from the Security Guard at Port Sarim jail"),
			new TaskMapping(VarPlayerID.FALADOR_ACHIEVEMENT_DIARY, 10, "Smith some Blurite Limbs on Doric's Anvil"),
		});
		
		// Falador Medium - bits 11-25 (skip bit 19)
		falador.put("medium", new TaskMapping[]{
			new TaskMapping(VarPlayerID.FALADOR_ACHIEVEMENT_DIARY, 11, "Light a Bullseye lantern in the Chemist's in Rimmington"),
			new TaskMapping(VarPlayerID.FALADOR_ACHIEVEMENT_DIARY, 12, "Telegrab a Wine of Zamorak from the Chaos Temple"),
			new TaskMapping(VarPlayerID.FALADOR_ACHIEVEMENT_DIARY, 13, "Unlock the crystal chest in Taverley"),
			new TaskMapping(VarPlayerID.FALADOR_ACHIEVEMENT_DIARY, 14, "Place a Scarecrow in the Falador farm flower patch"),
			new TaskMapping(VarPlayerID.FALADOR_ACHIEVEMENT_DIARY, 15, "Kill a Mogre"),
			new TaskMapping(VarPlayerID.FALADOR_ACHIEVEMENT_DIARY, 16, "Enter the Port Sarim Rat Pits"),
			new TaskMapping(VarPlayerID.FALADOR_ACHIEVEMENT_DIARY, 17, "Grapple up and then jump off the north Falador wall"),
			new TaskMapping(VarPlayerID.FALADOR_ACHIEVEMENT_DIARY, 18, "Pickpocket a Falador guard"),
			new TaskMapping(VarPlayerID.FALADOR_ACHIEVEMENT_DIARY, 20, "Pray at the altar in Taverley while wearing full Initiate"),
			new TaskMapping(VarPlayerID.FALADOR_ACHIEVEMENT_DIARY, 21, "Mine some Gold in the Crafting Guild"),
			new TaskMapping(VarPlayerID.FALADOR_ACHIEVEMENT_DIARY, 22, "Use the wall shortcut to the Dwarven Mine"),
			new TaskMapping(VarPlayerID.FALADOR_ACHIEVEMENT_DIARY, 23, "Chop and burn some Willow logs in Taverley"),
			new TaskMapping(VarPlayerID.FALADOR_ACHIEVEMENT_DIARY, 24, "Make a basket on the loom at the Falador farm"),
			new TaskMapping(VarPlayerID.FALADOR_ACHIEVEMENT_DIARY, 25, "Cast the Falador Teleport spell"),
		});
		
		// Falador Hard - bits 26-31, then DIARY2 0-4
		falador.put("hard", new TaskMapping[]{
			new TaskMapping(VarPlayerID.FALADOR_ACHIEVEMENT_DIARY, 26, "Craft some Mind runes"),
			new TaskMapping(VarPlayerID.FALADOR_ACHIEVEMENT_DIARY, 27, "Change your family crest to the Saradomin symbol"),
			new TaskMapping(VarPlayerID.FALADOR_ACHIEVEMENT_DIARY, 28, "Kill the Giant Mole beneath Falador Park"),
			new TaskMapping(VarPlayerID.FALADOR_ACHIEVEMENT_DIARY, 29, "Kill a Skeletal Wyvern in the Asgarnia Ice Dungeon"),
			new TaskMapping(VarPlayerID.FALADOR_ACHIEVEMENT_DIARY, 30, "Complete a lap of the Falador rooftop course"),
			new TaskMapping(VarPlayerID.FALADOR_ACHIEVEMENT_DIARY, 31, "Enter the Mining Guild wearing full prospector"),
			new TaskMapping(VarPlayerID.FALADOR_ACHIEVEMENT_DIARY2, 0, "Kill a Blue dragon in the Heroes' Guild"),
			new TaskMapping(VarPlayerID.FALADOR_ACHIEVEMENT_DIARY2, 1, "Crack the safe in the Rogues' Den"),
			new TaskMapping(VarPlayerID.FALADOR_ACHIEVEMENT_DIARY2, 2, "Pray at the Port Sarim altar while wearing full Proselyte"),
			new TaskMapping(VarPlayerID.FALADOR_ACHIEVEMENT_DIARY2, 3, "Enter the Warriors' Guild"),
			new TaskMapping(VarPlayerID.FALADOR_ACHIEVEMENT_DIARY2, 4, "Equip a Dwarven helmet within the Dwarven Mines"),
		});
		
		// Falador Elite - DIARY2 bits 5-10
		falador.put("elite", new TaskMapping[]{
			new TaskMapping(VarPlayerID.FALADOR_ACHIEVEMENT_DIARY2, 5, "Craft 56 Air runes simultaneously"),
			new TaskMapping(VarPlayerID.FALADOR_ACHIEVEMENT_DIARY2, 6, "Purchase a White 2h sword from Sir Vyvin"),
			new TaskMapping(VarPlayerID.FALADOR_ACHIEVEMENT_DIARY2, 7, "Grow and check the health of a Magic tree in Falador Park"),
			new TaskMapping(VarPlayerID.FALADOR_ACHIEVEMENT_DIARY2, 8, "Perform the Skillcape emote at the top of Falador castle"),
			new TaskMapping(VarPlayerID.FALADOR_ACHIEVEMENT_DIARY2, 9, "Jump the strange floor in Taverley dungeon"),
			new TaskMapping(VarPlayerID.FALADOR_ACHIEVEMENT_DIARY2, 10, "Mix a Saradomin brew in Falador east bank"),
		});
		
		DIARY_MAPPINGS.put("falador", falador);
	}
	
	private static void initializeFremennikMappings()
	{
		Map<String, TaskMapping[]> fremennik = new HashMap<>();
		
		// Fremennik Easy - bits 1-10
		fremennik.put("easy", new TaskMapping[]{
			new TaskMapping(VarPlayerID.FREMENNIK_ACHIEVEMENT_DIARY, 1, "Catch a Cerulean Twitch in the Rellekka Hunter area."),
			new TaskMapping(VarPlayerID.FREMENNIK_ACHIEVEMENT_DIARY, 2, "Change your boots at Yrsa's Shoe Store."),
			new TaskMapping(VarPlayerID.FREMENNIK_ACHIEVEMENT_DIARY, 3, "Kill 5 Rock crabs."),
			new TaskMapping(VarPlayerID.FREMENNIK_ACHIEVEMENT_DIARY, 4, "Craft a tiara in Rellekka."),
			new TaskMapping(VarPlayerID.FREMENNIK_ACHIEVEMENT_DIARY, 5, "Browse the Stonemason's Shop."),
			new TaskMapping(VarPlayerID.FREMENNIK_ACHIEVEMENT_DIARY, 6, "Collect 5 snape grass on Waterbirth Island. Speak with Jarvald to return to Rellekka when complete."),
			new TaskMapping(VarPlayerID.FREMENNIK_ACHIEVEMENT_DIARY, 7, "Steal from the bakery stall."),
			new TaskMapping(VarPlayerID.FREMENNIK_ACHIEVEMENT_DIARY, 8, "Fill a bucket at the Rellekka well."),
			new TaskMapping(VarPlayerID.FREMENNIK_ACHIEVEMENT_DIARY, 9, "Enter the Troll Stronghold."),
			new TaskMapping(VarPlayerID.FREMENNIK_ACHIEVEMENT_DIARY, 10, "Chop some oak logs in Rellekka and burn them."),
		});
		
		// Fremennik Medium - bits 11-20 (skip bit 16)
		fremennik.put("medium", new TaskMapping[]{
			new TaskMapping(VarPlayerID.FREMENNIK_ACHIEVEMENT_DIARY, 11, "Kill a brine rat then roll the boulder and exit the cave.."),
			new TaskMapping(VarPlayerID.FREMENNIK_ACHIEVEMENT_DIARY, 12, "Use rope on the Polar Eagle to travel to the Snowy Hunter area."),
			new TaskMapping(VarPlayerID.FREMENNIK_ACHIEVEMENT_DIARY, 13, "Mine some coal."),
			new TaskMapping(VarPlayerID.FREMENNIK_ACHIEVEMENT_DIARY, 14, "Steal from the Rellekka fish stall."),
			new TaskMapping(VarPlayerID.FREMENNIK_ACHIEVEMENT_DIARY, 15, "Use a fairy ring and travel to (CIP)."),
			new TaskMapping(VarPlayerID.FREMENNIK_ACHIEVEMENT_DIARY, 17, "Catch a Snowy Knight at the Fremennik Hunter Area."),
			new TaskMapping(VarPlayerID.FREMENNIK_ACHIEVEMENT_DIARY, 18, "Use a pet rock on your pet house in your menagerie in your player owned house and then pick it up off the GROUND."),
			new TaskMapping(VarPlayerID.FREMENNIK_ACHIEVEMENT_DIARY, 19, "Keep current protection and continue through the cave."),
			new TaskMapping(VarPlayerID.FREMENNIK_ACHIEVEMENT_DIARY, 20, "Mine the nearby gold. Remove helmet to escape area."),
		});
		
		// Fremennik Hard - bits 21-30 (skip bit 22)
		fremennik.put("hard", new TaskMapping[]{
			new TaskMapping(VarPlayerID.FREMENNIK_ACHIEVEMENT_DIARY, 21, "Teleport to Trollheim."),
			new TaskMapping(VarPlayerID.FREMENNIK_ACHIEVEMENT_DIARY, 23, "Place logs over a pit in the hunter area, and poke a kyatt with a teasing stick."),
			new TaskMapping(VarPlayerID.FREMENNIK_ACHIEVEMENT_DIARY, 24, "Mix a Super defence potion within the Fremennik Province (only near Rellekka)."),
			new TaskMapping(VarPlayerID.FREMENNIK_ACHIEVEMENT_DIARY, 25, "Steal from the gem stall."),
			new TaskMapping(VarPlayerID.FREMENNIK_ACHIEVEMENT_DIARY, 26, "Craft a shield on the woodcutting stump."),
			new TaskMapping(VarPlayerID.FREMENNIK_ACHIEVEMENT_DIARY, 27, "Mine 5 Adamantite ores."),
			new TaskMapping(VarPlayerID.FREMENNIK_ACHIEVEMENT_DIARY, 28, "Rake the herb and flax patch until 100% support."),
			new TaskMapping(VarPlayerID.FREMENNIK_ACHIEVEMENT_DIARY, 29, "Teleport to Waterbirth."),
			new TaskMapping(VarPlayerID.FREMENNIK_ACHIEVEMENT_DIARY, 30, "Speak with the Foreman."),
		});
		
		// Fremennik Elite - bit 31, then DIARY2 0-4
		fremennik.put("elite", new TaskMapping[]{
			new TaskMapping(VarPlayerID.FREMENNIK_ACHIEVEMENT_DIARY, 31, "Enter the Kings' lair."),
			new TaskMapping(VarPlayerID.FREMENNIK_ACHIEVEMENT_DIARY2, 0, "Craft 56 astral runes"),
			new TaskMapping(VarPlayerID.FREMENNIK_ACHIEVEMENT_DIARY2, 1, "Smelt a dragonstone amulet on the clay forge."),
			new TaskMapping(VarPlayerID.FREMENNIK_ACHIEVEMENT_DIARY2, 2, "Complete a lap of the Rellekka Rooftop course."),
			new TaskMapping(VarPlayerID.FREMENNIK_ACHIEVEMENT_DIARY2, 3, "Get kills for a faction then kill its respective general."),
			new TaskMapping(VarPlayerID.FREMENNIK_ACHIEVEMENT_DIARY2, 4, "Kill a spiritual mage"),
		});
		
		DIARY_MAPPINGS.put("fremennik", fremennik);
	}
	
	private static void initializeKandarinMappings()
	{
		Map<String, TaskMapping[]> kandarin = new HashMap<>();
		
		// Kandarin Easy - bits 1-11
		kandarin.put("easy", new TaskMapping[]{
			new TaskMapping(VarPlayerID.KANDARIN_ACHIEVEMENT_DIARY, 1, "Fish on Catherby beach at the Big Net fishing spots for a mackerel."),
			new TaskMapping(VarPlayerID.KANDARIN_ACHIEVEMENT_DIARY, 2, "Buy a candle from the candle maker in Catherby."),
			new TaskMapping(VarPlayerID.KANDARIN_ACHIEVEMENT_DIARY, 3, "Pick 5 flax at the flax field west of Catherby."),
			new TaskMapping(VarPlayerID.KANDARIN_ACHIEVEMENT_DIARY, 4, "Play the organ in Seers' Village Church."),
			new TaskMapping(VarPlayerID.KANDARIN_ACHIEVEMENT_DIARY, 5, "Plant 3 jute seeds in the hops patch north west of Seers' Village."),
			new TaskMapping(VarPlayerID.KANDARIN_ACHIEVEMENT_DIARY, 6, "Talk with Galahad west of McGrubor's Wood until he gives you some tea."),
			new TaskMapping(VarPlayerID.KANDARIN_ACHIEVEMENT_DIARY, 7, "Kill an elemental in the Elemental Workshop in Seers' Village."),
			new TaskMapping(VarPlayerID.KANDARIN_ACHIEVEMENT_DIARY, 8, "Speak with Harry in the Catherby Fishing Shop to get a tiny net."),
			new TaskMapping(VarPlayerID.KANDARIN_ACHIEVEMENT_DIARY, 9, "Talk with the bartender in Seers' Village and buy a stew."),
			new TaskMapping(VarPlayerID.KANDARIN_ACHIEVEMENT_DIARY, 10, "Speak with Sherlock west of Catherby."),
			new TaskMapping(VarPlayerID.KANDARIN_ACHIEVEMENT_DIARY, 11, "Cross the log shortcut near to Galahad."),
		});
		
		// Kandarin Medium - bits 12-25
		kandarin.put("medium", new TaskMapping[]{
			new TaskMapping(VarPlayerID.KANDARIN_ACHIEVEMENT_DIARY, 12, "Complete a lap of the Barbarian Outpost agility course."),
			new TaskMapping(VarPlayerID.KANDARIN_ACHIEVEMENT_DIARY, 13, "Create a super antipoison potion."),
			new TaskMapping(VarPlayerID.KANDARIN_ACHIEVEMENT_DIARY, 14, "Enter the Ranging Guild."),
			new TaskMapping(VarPlayerID.KANDARIN_ACHIEVEMENT_DIARY, 15, "Grapple across!"),
			new TaskMapping(VarPlayerID.KANDARIN_ACHIEVEMENT_DIARY, 16, "Catch a bass on Catherby Beach and cook it on the range there."),
			new TaskMapping(VarPlayerID.KANDARIN_ACHIEVEMENT_DIARY, 17, "Teleport to Camelot."),
			new TaskMapping(VarPlayerID.KANDARIN_ACHIEVEMENT_DIARY, 18, "String a maple shortbow."),
			new TaskMapping(VarPlayerID.KANDARIN_ACHIEVEMENT_DIARY, 19, "Plant a limpwurt seed in the Catherby Flower Patch, wait for it to grow then pick it."),
			new TaskMapping(VarPlayerID.KANDARIN_ACHIEVEMENT_DIARY, 20, "Make a mind helm in the Elemental Workshop."),
			new TaskMapping(VarPlayerID.KANDARIN_ACHIEVEMENT_DIARY, 21, "Kill a Fire giant."),
			new TaskMapping(VarPlayerID.KANDARIN_ACHIEVEMENT_DIARY, 22, "Complete a wave of Barbarian Assault. If it's your first time here, speak with Captain Cain for the"),
			new TaskMapping(VarPlayerID.KANDARIN_ACHIEVEMENT_DIARY, 23, "Steal from the chest in Hemenster."),
			new TaskMapping(VarPlayerID.KANDARIN_ACHIEVEMENT_DIARY, 24, "Take a fairy ring to McGrubor's Woods (ALS)"),
			new TaskMapping(VarPlayerID.KANDARIN_ACHIEVEMENT_DIARY, 25, "Mine coal near the Coal Trucks."),
		});
		
		// Kandarin Hard - bits 26-31, then DIARY2 0-4
		kandarin.put("hard", new TaskMapping[]{
			new TaskMapping(VarPlayerID.KANDARIN_ACHIEVEMENT_DIARY, 26, "Catch a leaping Sturgeon south of Barbarian Assault."),
			new TaskMapping(VarPlayerID.KANDARIN_ACHIEVEMENT_DIARY, 27, "Complete a lap of the Seers' village Rooftop course."),
			new TaskMapping(VarPlayerID.KANDARIN_ACHIEVEMENT_DIARY, 28, "Cut some yew logs near Seers' Village. Make sure to use the knife on the ones you cut."),
			new TaskMapping(VarPlayerID.KANDARIN_ACHIEVEMENT_DIARY, 29, "Activate piety then enter the Seers' Village courthouse."),
			new TaskMapping(VarPlayerID.KANDARIN_ACHIEVEMENT_DIARY, 30, "Use the charge water orb spell on the obelisk."),
			new TaskMapping(VarPlayerID.KANDARIN_ACHIEVEMENT_DIARY, 31, "Burn some maple logs with a bow."),
			new TaskMapping(VarPlayerID.KANDARIN_ACHIEVEMENT_DIARY2, 0, "Kill a shadow hound."),
			new TaskMapping(VarPlayerID.KANDARIN_ACHIEVEMENT_DIARY2, 1, "Kill a mithril dragon."),
			new TaskMapping(VarPlayerID.KANDARIN_ACHIEVEMENT_DIARY2, 2, "Buy and equip a granite body from Commander Connad. (Requires at least 1 Penance Queen kill)"),
			new TaskMapping(VarPlayerID.KANDARIN_ACHIEVEMENT_DIARY2, 3, "TALK to the estate agent to redecorate your house to Fancy Stone. Must be done through dialog."),
			new TaskMapping(VarPlayerID.KANDARIN_ACHIEVEMENT_DIARY2, 4, "Smith an adamant spear on the barbarian anvil south of Barbarian Assault."),
		});
		
		// Kandarin Elite - DIARY2 bits 5-11
		kandarin.put("elite", new TaskMapping[]{
			new TaskMapping(VarPlayerID.KANDARIN_ACHIEVEMENT_DIARY2, 5, "Reach level 5 in all four roles at Barbarian Assault."),
			new TaskMapping(VarPlayerID.KANDARIN_ACHIEVEMENT_DIARY2, 6, "Plant and harvest the dwarf weed from the Catherby patch."),
			new TaskMapping(VarPlayerID.KANDARIN_ACHIEVEMENT_DIARY2, 7, "Catch and successfully cook 5 sharks in Catherby."),
			new TaskMapping(VarPlayerID.KANDARIN_ACHIEVEMENT_DIARY2, 8, "Create a stamina mix."),
			new TaskMapping(VarPlayerID.KANDARIN_ACHIEVEMENT_DIARY2, 9, "Smith a rune hasta on the barbarian anvil near Otto."),
			new TaskMapping(VarPlayerID.KANDARIN_ACHIEVEMENT_DIARY2, 10, "Construct a pyre ship from magic logs."),
			new TaskMapping(VarPlayerID.KANDARIN_ACHIEVEMENT_DIARY2, 11, "Teleport to Catherby."),
		});
		
		DIARY_MAPPINGS.put("kandarin", kandarin);
	}
	
	private static void initializeKaramjaMappings()
	{
		Map<String, TaskMapping[]> karamja = new HashMap<>();

		// Karamja uses VarbitIDs for individual task tracking (one varbit per task),
		// unlike every other diary which bitpacks tasks into varplayers.
		// Easy/Medium/Hard use VarbitID.ATJUN_*; Elite uses VarPlayerID.ATJUN_TASKS_4 bits 1-5.
		// BANANA, SEAWEED (Easy) and PALM (Hard) are count-based: done when varbitValue >= 5.
		// Source: VarbitID.java lines 2639-2684; VarPlayerID.ATJUN_TASKS_4 = 1200.

		// Karamja Easy - 10 tasks (VarbitID.ATJUN_EASY_*)
		karamja.put("easy", new TaskMapping[]{
			TaskMapping.ofVarbit(3566, "Pick 5 bananas from the Karamja banana plantation.", 5),  // ATJUN_EASY_BANANA
			TaskMapping.ofVarbit(3567, "Swing on the ropeswing west of Brimhaven."),              // ATJUN_EASY_SWING
			TaskMapping.ofVarbit(3568, "Mine a gold rock north west of Brimhaven."),              // ATJUN_EASY_GOLD
			TaskMapping.ofVarbit(3569, "Travel to Port Sarim via the Musa Point customs officer."), // ATJUN_EASY_BOAT_SARIM
			TaskMapping.ofVarbit(3570, "Travel to Ardougne from Brimhaven with Captain Barnaby."), // ATJUN_EASY_BOAT_ARDY
			TaskMapping.ofVarbit(3571, "Travel to Cairn Island by climbing the rocks and crossing the bridge."), // ATJUN_EASY_CAIRN
			TaskMapping.ofVarbit(3572, "Fish north of the banana plantation."),                    // ATJUN_EASY_FISHING
			TaskMapping.ofVarbit(3573, "Pick up 5 seaweed on Karamja's coast.", 5),               // ATJUN_EASY_SEAWEED
			TaskMapping.ofVarbit(3574, "Enter the Fight Pits or Fight Caves in Mor Ul Rek."),     // ATJUN_EASY_TZHAAR
			TaskMapping.ofVarbit(3575, "Kill a Jogre in the Pothole Dungeon east of Tai Bwo Wannai."), // ATJUN_EASY_JOGRE
		});

		// Karamja Medium - 19 tasks (VarbitID.ATJUN_MED_*)
		karamja.put("medium", new TaskMapping[]{
			TaskMapping.ofVarbit(3579, "Complete a lap of the Brimhaven Agility Arena and claim a ticket."), // ATJUN_MED_AGILITY
			TaskMapping.ofVarbit(3580, "Use the stepping stone shortcut inside Karamja Volcano."),           // ATJUN_MED_VOLCANO
			TaskMapping.ofVarbit(3581, "Climb the rope to enter Crandor Isle."),                             // ATJUN_MED_CRANDOR
			TaskMapping.ofVarbit(3582, "Travel using Vigroy & Hajedy's cart service."),                      // ATJUN_MED_CART
			TaskMapping.ofVarbit(3583, "Earn 100% favour in a game of Tai Bwo Wannai Cleanup."),             // ATJUN_MED_CLEANUP
			TaskMapping.ofVarbit(3584, "Cook a spider on a stick on Karamja."),                              // ATJUN_MED_SPIDER
			TaskMapping.ofVarbit(3585, "Mine a red topaz from gem rocks in Shilo Village."),                 // ATJUN_MED_TOPAZ
			TaskMapping.ofVarbit(3586, "Chop a teak tree in the Hardwood Grove."),                           // ATJUN_MED_TEAK
			TaskMapping.ofVarbit(3587, "Chop a mahogany tree in the Hardwood Grove."),                       // ATJUN_MED_MAHOGANY
			TaskMapping.ofVarbit(3588, "Catch a karambwan from the north east coast of Karamja."),           // ATJUN_MED_KARAMBWAN
			TaskMapping.ofVarbit(3589, "Exchange gems with Safta Doc in Tai Bwo Wannai for a machete."),     // ATJUN_MED_MACHETTE
			TaskMapping.ofVarbit(3590, "Use the gnome glider to fly to Karamja."),                           // ATJUN_MED_GLIDER
			TaskMapping.ofVarbit(3591, "Grow a fruit tree in the Brimhaven farming patch."),                 // ATJUN_MED_FARMING
			TaskMapping.ofVarbit(3592, "Trap a horned graahk using box traps."),                             // ATJUN_MED_GRAAHK
			TaskMapping.ofVarbit(3593, "Chop the vines blocking the path in Brimhaven Dungeon."),            // ATJUN_MED_SHILO_VINES
			TaskMapping.ofVarbit(3594, "Cross the lava using the stepping stones in Brimhaven Dungeon."),    // ATJUN_MED_SHILO_LAVA
			TaskMapping.ofVarbit(3595, "Climb the stairs to the top floor of Brimhaven Dungeon."),           // ATJUN_MED_SHILO_STAIRS
			TaskMapping.ofVarbit(3596, "Travel to Port Khazard using the charter ship from Shilo Village."), // ATJUN_MED_KHAZARD
			TaskMapping.ofVarbit(3597, "Charter a ship from the Shipyard."),                                 // ATJUN_MED_CHARTER
		});

		// Karamja Hard - 10 tasks (VarbitID.ATJUN_HARD_*)
		karamja.put("hard", new TaskMapping[]{
			TaskMapping.ofVarbit(3600, "Win in the Fight Pits in Mor Ul Rek."),                              // ATJUN_HARD_FIGHTPITS
			TaskMapping.ofVarbit(3601, "Reach at least wave 31 to defeat Ket-Zek in the Fight Cave."),      // ATJUN_HARD_FIGHTCAVE
			TaskMapping.ofVarbit(3602, "Cook and eat an oomlie wrap."),                                      // ATJUN_HARD_OOMLIE
			TaskMapping.ofVarbit(3603, "Craft a nature rune at the Nature Altar."),                          // ATJUN_HARD_NATURE
			TaskMapping.ofVarbit(3604, "Cook a raw karambwan."),                                             // ATJUN_HARD_KARAMBWAN
			TaskMapping.ofVarbit(3605, "Kill a deathwing in the Kharazi Jungle."),                           // ATJUN_HARD_DEATHWING
			TaskMapping.ofVarbit(3606, "Use the crossbow shortcut in Brimhaven Dungeon."),                   // ATJUN_HARD_XBOW
			TaskMapping.ofVarbit(3607, "Collect palm tree leaves from the Brimhaven farming patch.", 5),     // ATJUN_HARD_PALM
			TaskMapping.ofVarbit(3608, "Get assigned a slayer task by Duradel in Shilo Village."),           // ATJUN_HARD_DURADEL
			TaskMapping.ofVarbit(3609, "Kill a metal dragon in Brimhaven Dungeon."),                         // ATJUN_HARD_DRAGON
		});

		// Karamja Elite - 5 tasks (VarPlayerID.ATJUN_TASKS_4 = 1200, bits 1-5)
		karamja.put("elite", new TaskMapping[]{
			new TaskMapping(VarPlayerID.ATJUN_TASKS_4, 1, "Craft a full inventory of nature runes."),
			new TaskMapping(VarPlayerID.ATJUN_TASKS_4, 2, "Equip a fire or infernal cape."),
			new TaskMapping(VarPlayerID.ATJUN_TASKS_4, 3, "Grow and check the health of a palm tree in the Brimhaven patch."),
			new TaskMapping(VarPlayerID.ATJUN_TASKS_4, 4, "Make an antivenom potion whilst standing in the horse shoe mine."),
			new TaskMapping(VarPlayerID.ATJUN_TASKS_4, 5, "Grow and check the health of a Calquat in Tai Bwo Wannai."),
		});

		DIARY_MAPPINGS.put("karamja", karamja);
	}
	
	private static void initializeKourendMappings()
	{
		Map<String, TaskMapping[]> kourend = new HashMap<>();
		
		// Kourend Easy - bits 1-12
		kourend.put("easy", new TaskMapping[]{
			new TaskMapping(VarPlayerID.KOUREND_ACHIEVEMENT_DIARY, 1, "Mine some iron ore at the Mount Karuulm mine."),
			new TaskMapping(VarPlayerID.KOUREND_ACHIEVEMENT_DIARY, 2, "Kill a sand crab."),
			new TaskMapping(VarPlayerID.KOUREND_ACHIEVEMENT_DIARY, 3, "Collect a book for a patron in the Arceuus Library."),
			new TaskMapping(VarPlayerID.KOUREND_ACHIEVEMENT_DIARY, 4, "Steal from a Hosidius fruit stall."),
			new TaskMapping(VarPlayerID.KOUREND_ACHIEVEMENT_DIARY, 5, "Browse the Warrens General Store."),
			new TaskMapping(VarPlayerID.KOUREND_ACHIEVEMENT_DIARY, 6, "Take a boat to Land's End."),
			new TaskMapping(VarPlayerID.KOUREND_ACHIEVEMENT_DIARY, 7, "Pray at the Kourend Castle altar."),
			new TaskMapping(VarPlayerID.KOUREND_ACHIEVEMENT_DIARY, 8, "Dig up some Saltpetre in Hosidius."),
			new TaskMapping(VarPlayerID.KOUREND_ACHIEVEMENT_DIARY, 9, "Enter your player-owned house from Hosidius."),
			new TaskMapping(VarPlayerID.KOUREND_ACHIEVEMENT_DIARY, 10, "Complete the Shayzien Agility Course."),
			new TaskMapping(VarPlayerID.KOUREND_ACHIEVEMENT_DIARY, 11, "Create a strength Potion in The Deeper Lode pub."),
			new TaskMapping(VarPlayerID.KOUREND_ACHIEVEMENT_DIARY, 12, "Fish a trout from the River Molch."),
		});
		
		// Kourend Medium - bits 13-25
		kourend.put("medium", new TaskMapping[]{
			new TaskMapping(VarPlayerID.KOUREND_ACHIEVEMENT_DIARY, 13, "Kill a lizardman."),
			new TaskMapping(VarPlayerID.KOUREND_ACHIEVEMENT_DIARY, 14, "Teleport to each of the five cities via the memoirs."),
			new TaskMapping(VarPlayerID.KOUREND_ACHIEVEMENT_DIARY, 15, "Mine some volcanic sulfur in Lovakengj."),
			new TaskMapping(VarPlayerID.KOUREND_ACHIEVEMENT_DIARY, 16, "Switch to the Arceuus spellbook via Tyss."),
			new TaskMapping(VarPlayerID.KOUREND_ACHIEVEMENT_DIARY, 17, "Repair a crane within Port Piscarilius."),
			new TaskMapping(VarPlayerID.KOUREND_ACHIEVEMENT_DIARY, 18, "Turn in the intelligence to Captain Ginea."),
			new TaskMapping(VarPlayerID.KOUREND_ACHIEVEMENT_DIARY, 19, "Catch a bluegill."),
			new TaskMapping(VarPlayerID.KOUREND_ACHIEVEMENT_DIARY, 20, "Subdue the Wintertodt (earn at least 500 points)."),
			new TaskMapping(VarPlayerID.KOUREND_ACHIEVEMENT_DIARY, 21, "Enter the Farming Guild."),
			new TaskMapping(VarPlayerID.KOUREND_ACHIEVEMENT_DIARY, 22, "Use the boulder leap shortcut from the path to the Soul Altar."),
			new TaskMapping(VarPlayerID.KOUREND_ACHIEVEMENT_DIARY, 23, "Catch a chinchompa in the Kourend Woodland."),
			new TaskMapping(VarPlayerID.KOUREND_ACHIEVEMENT_DIARY, 24, "Chop some logs from a mahogany tree North of the Farming Guild."),
			new TaskMapping(VarPlayerID.KOUREND_ACHIEVEMENT_DIARY, 25, "Travel from any fairy ring to south of Mount Karuulm (CIR)."),
		});
		
		// Kourend Hard - bits 26-31, then DIARY2 0-3
		kourend.put("hard", new TaskMapping[]{
			new TaskMapping(VarPlayerID.KOUREND_ACHIEVEMENT_DIARY, 26, "Enter the Woodcutting Guild."),
			new TaskMapping(VarPlayerID.KOUREND_ACHIEVEMENT_DIARY, 27, "Smelt an adamantite bar."),
			new TaskMapping(VarPlayerID.KOUREND_ACHIEVEMENT_DIARY, 28, "Kill a Lizardman Shaman."),
			new TaskMapping(VarPlayerID.KOUREND_ACHIEVEMENT_DIARY, 29, "Mine some lovakite ore in Lovakengj."),
			new TaskMapping(VarPlayerID.KOUREND_ACHIEVEMENT_DIARY, 30, "Kill a zombie in the shayzien crypt."),
			new TaskMapping(VarPlayerID.KOUREND_ACHIEVEMENT_DIARY, 31, "Plant logavano seeds in the Tithe Farm."),
			new TaskMapping(VarPlayerID.KOUREND_ACHIEVEMENT_DIARY2, 0, "Teleport to Xeric's Heart using the Xeric's Talisman."),
			new TaskMapping(VarPlayerID.KOUREND_ACHIEVEMENT_DIARY2, 1, "Deliver the stolen artifact to Captain Khaled."),
			new TaskMapping(VarPlayerID.KOUREND_ACHIEVEMENT_DIARY2, 2, "Kill a wyrm."),
			new TaskMapping(VarPlayerID.KOUREND_ACHIEVEMENT_DIARY2, 3, "Cast Monster Examine on a Mountain Troll."),
		});
		
		// Kourend Elite - DIARY2 bits 4-11
		kourend.put("elite", new TaskMapping[]{
			new TaskMapping(VarPlayerID.KOUREND_ACHIEVEMENT_DIARY2, 4, "Craft some blood runes."),
			new TaskMapping(VarPlayerID.KOUREND_ACHIEVEMENT_DIARY2, 5, "Chop the redwood tree."),
			new TaskMapping(VarPlayerID.KOUREND_ACHIEVEMENT_DIARY2, 6, "Defeat Skotizo."),
			new TaskMapping(VarPlayerID.KOUREND_ACHIEVEMENT_DIARY2, 7, "Catch a raw anglerfish."),
			new TaskMapping(VarPlayerID.KOUREND_ACHIEVEMENT_DIARY2, 8, "Kill a hydra."),
			new TaskMapping(VarPlayerID.KOUREND_ACHIEVEMENT_DIARY2, 9, "Create an Ape Atoll teleport tablet using the Arceuus spellbook."),
			new TaskMapping(VarPlayerID.KOUREND_ACHIEVEMENT_DIARY2, 10, "Complete a Chambers of Xeric raid."),
			new TaskMapping(VarPlayerID.KOUREND_ACHIEVEMENT_DIARY2, 11, "Fletch a battlestaff."),
		});
		
		DIARY_MAPPINGS.put("kourend_kebos", kourend);
	}
	
	private static void initializeLumbridgeMappings()
	{
		Map<String, TaskMapping[]> lumbridge = new HashMap<>();
		
		// Lumbridge Easy - bits 1-12
		lumbridge.put("easy", new TaskMapping[]{
			new TaskMapping(VarPlayerID.LUMB_DRAY_ACHIEVEMENT_DIARY, 1, "Complete a lap of the Draynor Rooftop Course"),
			new TaskMapping(VarPlayerID.LUMB_DRAY_ACHIEVEMENT_DIARY, 2, "Kill a Cave Bug"),
			new TaskMapping(VarPlayerID.LUMB_DRAY_ACHIEVEMENT_DIARY, 3, "Have Sedridor teleport you to the Rune essence mine"),
			new TaskMapping(VarPlayerID.LUMB_DRAY_ACHIEVEMENT_DIARY, 4, "Craft some Water runes"),
			new TaskMapping(VarPlayerID.LUMB_DRAY_ACHIEVEMENT_DIARY, 5, "Talk to Hans to learn your age"),
			new TaskMapping(VarPlayerID.LUMB_DRAY_ACHIEVEMENT_DIARY, 6, "Pickpocket a man or woman"),
			new TaskMapping(VarPlayerID.LUMB_DRAY_ACHIEVEMENT_DIARY, 7, "Chop and burn some oak logs"),
			new TaskMapping(VarPlayerID.LUMB_DRAY_ACHIEVEMENT_DIARY, 8, "Kill a Zombie in Draynor Sewer"),
			new TaskMapping(VarPlayerID.LUMB_DRAY_ACHIEVEMENT_DIARY, 9, "Catch some anchovies in Al-Kharid"),
			new TaskMapping(VarPlayerID.LUMB_DRAY_ACHIEVEMENT_DIARY, 10, "Bake some bread on the Lumbridge Castle range"),
			new TaskMapping(VarPlayerID.LUMB_DRAY_ACHIEVEMENT_DIARY, 11, "Mine some iron ore in Al-Kharid"),
			new TaskMapping(VarPlayerID.LUMB_DRAY_ACHIEVEMENT_DIARY, 12, "Enter the H.A.M. Hideout"),
		});
		
		// Lumbridge Medium - bits 13-24
		lumbridge.put("medium", new TaskMapping[]{
			new TaskMapping(VarPlayerID.LUMB_DRAY_ACHIEVEMENT_DIARY, 13, "Complete the Al Kharid Rooftop Course"),
			new TaskMapping(VarPlayerID.LUMB_DRAY_ACHIEVEMENT_DIARY, 14, "Grapple across the River Lum"),
			new TaskMapping(VarPlayerID.LUMB_DRAY_ACHIEVEMENT_DIARY, 15, "Purchase Ava's Accumulator"),
			new TaskMapping(VarPlayerID.LUMB_DRAY_ACHIEVEMENT_DIARY, 16, "Travel to the Wizards' Tower using a fairy ring"),
			new TaskMapping(VarPlayerID.LUMB_DRAY_ACHIEVEMENT_DIARY, 17, "Cast the Lumbridge Teleport spell"),
			new TaskMapping(VarPlayerID.LUMB_DRAY_ACHIEVEMENT_DIARY, 18, "Catch a Salmon in the River Lum"),
			new TaskMapping(VarPlayerID.LUMB_DRAY_ACHIEVEMENT_DIARY, 19, "Craft a Coif"),
			new TaskMapping(VarPlayerID.LUMB_DRAY_ACHIEVEMENT_DIARY, 20, "Chop some Willow logs in Draynor Village"),
			new TaskMapping(VarPlayerID.LUMB_DRAY_ACHIEVEMENT_DIARY, 21, "Pickpocket Martin the Master Gardener"),
			new TaskMapping(VarPlayerID.LUMB_DRAY_ACHIEVEMENT_DIARY, 22, "Get a Slayer task from Chaeldar"),
			new TaskMapping(VarPlayerID.LUMB_DRAY_ACHIEVEMENT_DIARY, 23, "Catch an Essence or Eclectic impling in Puro-Puro"),
			new TaskMapping(VarPlayerID.LUMB_DRAY_ACHIEVEMENT_DIARY, 24, "Craft some Lava runes"),
		});
		
		// Lumbridge Hard - bits 25-31, then DIARY2 0-3
		lumbridge.put("hard", new TaskMapping[]{
			new TaskMapping(VarPlayerID.LUMB_DRAY_ACHIEVEMENT_DIARY, 25, "Cast Bones to Peaches in Al-Kharid Palace"),
			new TaskMapping(VarPlayerID.LUMB_DRAY_ACHIEVEMENT_DIARY, 26, "Squeeze through the jutting wall in Lumbridge swamp caves"),
			new TaskMapping(VarPlayerID.LUMB_DRAY_ACHIEVEMENT_DIARY, 27, "Craft 56 Cosmic runes"),
			new TaskMapping(VarPlayerID.LUMB_DRAY_ACHIEVEMENT_DIARY, 28, "Take a Waka Canoe to Edgeville"),
			new TaskMapping(VarPlayerID.LUMB_DRAY_ACHIEVEMENT_DIARY, 29, "Collect a minimum of 100 Tears of Guthix"),
			new TaskMapping(VarPlayerID.LUMB_DRAY_ACHIEVEMENT_DIARY, 30, "Travel to Keldagrim via the train system"),
			new TaskMapping(VarPlayerID.LUMB_DRAY_ACHIEVEMENT_DIARY, 31, "Purchase some Barrows gloves"),
			new TaskMapping(VarPlayerID.LUMB_DRAY_ACHIEVEMENT_DIARY2, 0, "Grow a Belladonna patch in Draynor Manor"),
			new TaskMapping(VarPlayerID.LUMB_DRAY_ACHIEVEMENT_DIARY2, 1, "Light a mining helmet in the Lumbridge Castle basement"),
			new TaskMapping(VarPlayerID.LUMB_DRAY_ACHIEVEMENT_DIARY2, 2, "Pray at the Emir's Arena altar with Smite activated"),
			new TaskMapping(VarPlayerID.LUMB_DRAY_ACHIEVEMENT_DIARY2, 3, "Craft a Power amulet"),
		});
		
		// Lumbridge Elite - DIARY2 bits 4-9
		lumbridge.put("elite", new TaskMapping[]{
			new TaskMapping(VarPlayerID.LUMB_DRAY_ACHIEVEMENT_DIARY2, 4, "Pickpock the average chest in Dorgesh-Kaan"),
			new TaskMapping(VarPlayerID.LUMB_DRAY_ACHIEVEMENT_DIARY2, 5, "Pickpocket Movario at the Dorgesh-Kaan agility course"),
			new TaskMapping(VarPlayerID.LUMB_DRAY_ACHIEVEMENT_DIARY2, 6, "Chop some Magic logs at the Mage Training Arena"),
			new TaskMapping(VarPlayerID.LUMB_DRAY_ACHIEVEMENT_DIARY2, 7, "Smith an Adamant platebody in Draynor Sewer"),
			new TaskMapping(VarPlayerID.LUMB_DRAY_ACHIEVEMENT_DIARY2, 8, "Craft 140 Water runes simultaneously"),
			new TaskMapping(VarPlayerID.LUMB_DRAY_ACHIEVEMENT_DIARY2, 9, "Perform the Quest Cape emote in the Legends' Guild"),
		});
		
		DIARY_MAPPINGS.put("lumbridge_draynor", lumbridge);
	}
	
	private static void initializeMorytaniaMappings()
	{
		Map<String, TaskMapping[]> morytania = new HashMap<>();
		
		// Morytania Easy - bits 1-11
		morytania.put("easy", new TaskMapping[]{
			new TaskMapping(VarPlayerID.MORYTANIA_ACHIEVEMENT_DIARY, 1, "Craft a snelm in Morytania. Note: Do not be in the swamp when completing"),
			new TaskMapping(VarPlayerID.MORYTANIA_ACHIEVEMENT_DIARY, 2, "Cook a thin snail in Port Phasmatys."),
			new TaskMapping(VarPlayerID.MORYTANIA_ACHIEVEMENT_DIARY, 3, "Get a slayer task from Mazchna."),
			new TaskMapping(VarPlayerID.MORYTANIA_ACHIEVEMENT_DIARY, 4, "Kill a banshee."),
			new TaskMapping(VarPlayerID.MORYTANIA_ACHIEVEMENT_DIARY, 5, "Tan a hide using Sbott's services."),
			new TaskMapping(VarPlayerID.MORYTANIA_ACHIEVEMENT_DIARY, 6, "Enter the Mort Myre Swamp."),
			new TaskMapping(VarPlayerID.MORYTANIA_ACHIEVEMENT_DIARY, 7, "Kill a ghoul in Morytania."),
			new TaskMapping(VarPlayerID.MORYTANIA_ACHIEVEMENT_DIARY, 8, "Place a scarecrow at the Morytania flower patch, West of Port Phasmatys."),
			new TaskMapping(VarPlayerID.MORYTANIA_ACHIEVEMENT_DIARY, 9, "Worship the ectofuntus."),
			new TaskMapping(VarPlayerID.MORYTANIA_ACHIEVEMENT_DIARY, 10, "Kill any attackable NPC in Canifis with the wolfbane dagger."),
			new TaskMapping(VarPlayerID.MORYTANIA_ACHIEVEMENT_DIARY, 11, "Pray at the altar."),
		});
		
		// Morytania Medium - bits 12-22
		morytania.put("medium", new TaskMapping[]{
			new TaskMapping(VarPlayerID.MORYTANIA_ACHIEVEMENT_DIARY, 12, "Catch a swamp lizard."),
			new TaskMapping(VarPlayerID.MORYTANIA_ACHIEVEMENT_DIARY, 13, "Complete a lap of the Canifis Rooftop Course."),
			new TaskMapping(VarPlayerID.MORYTANIA_ACHIEVEMENT_DIARY, 14, "Chop some bark off the hollow tree south of Port phasmatys."),
			new TaskMapping(VarPlayerID.MORYTANIA_ACHIEVEMENT_DIARY, 15, "Talk to the Ghost captain at Port Phasmatys to travel to Dragontooth Island."),
			new TaskMapping(VarPlayerID.MORYTANIA_ACHIEVEMENT_DIARY, 16, "Kill a terror dog. You can enter the room with the diary if you need a safe zone."),
			new TaskMapping(VarPlayerID.MORYTANIA_ACHIEVEMENT_DIARY, 17, "Start playing Trouble brewing. You don't need to win. You will need to empty your inventory and unequip"),
			new TaskMapping(VarPlayerID.MORYTANIA_ACHIEVEMENT_DIARY, 18, "Board the Swamp boaty at the Hollows."),
			new TaskMapping(VarPlayerID.MORYTANIA_ACHIEVEMENT_DIARY, 19, "Make a batch of cannonballs at Port Phasmatys."),
			new TaskMapping(VarPlayerID.MORYTANIA_ACHIEVEMENT_DIARY, 20, "Kill a Fever spider."),
			new TaskMapping(VarPlayerID.MORYTANIA_ACHIEVEMENT_DIARY, 21, "Use your Ectophial to teleport to Port Phasmatys."),
			new TaskMapping(VarPlayerID.MORYTANIA_ACHIEVEMENT_DIARY, 22, "Mix a Guthix balance potion while in Morytania."),
		});
		
		// Morytania Hard - bits 23-30, then DIARY2 1-2 (DIARY2 bit 0 skipped!)
		morytania.put("hard", new TaskMapping[]{
			new TaskMapping(VarPlayerID.MORYTANIA_ACHIEVEMENT_DIARY, 23, "Enter the Kharyrll portal in your POH. Through a Portal Chamber or"),
			new TaskMapping(VarPlayerID.MORYTANIA_ACHIEVEMENT_DIARY, 24, "Climb the advanced spike chain. Go down and back up if you rip your hands as you climb."),
			new TaskMapping(VarPlayerID.MORYTANIA_ACHIEVEMENT_DIARY, 25, "Plant and harvest watermelon on Harmony Island. It takes 80 minutes to fully grow."),
			new TaskMapping(VarPlayerID.MORYTANIA_ACHIEVEMENT_DIARY, 26, "Burn mahogany logs on the island."),
			new TaskMapping(VarPlayerID.MORYTANIA_ACHIEVEMENT_DIARY, 27, "Complete a Hard Temple Trek. Alternatively complete a Hard Burgh de Rott Ramble. You can use Route 1 to"),
			new TaskMapping(VarPlayerID.MORYTANIA_ACHIEVEMENT_DIARY, 28, "Enter the Mos Le'Harmless Cave."),
			new TaskMapping(VarPlayerID.MORYTANIA_ACHIEVEMENT_DIARY, 29, "Plant and harvest the bittercap mushrooms in Canifis. It takes 4 hours to fully grow."),
			new TaskMapping(VarPlayerID.MORYTANIA_ACHIEVEMENT_DIARY, 30, "Pray at the altar with Piety activated."),
			new TaskMapping(VarPlayerID.MORYTANIA_ACHIEVEMENT_DIARY2, 1, "Use the shortcut to get to the bridge. This achievement only works one-way, so you must go from"),
			new TaskMapping(VarPlayerID.MORYTANIA_ACHIEVEMENT_DIARY2, 2, "Mine the mithril ore in the north-east of the area."),
		});
		
		// Morytania Elite - DIARY2 bits 3-8
		morytania.put("elite", new TaskMapping[]{
			new TaskMapping(VarPlayerID.MORYTANIA_ACHIEVEMENT_DIARY2, 3, "Bare hand fish a shark in Burgh de Rott."),
			new TaskMapping(VarPlayerID.MORYTANIA_ACHIEVEMENT_DIARY2, 4, "Place the pyre logs and shade remains on the funeral pyre and light them with a tinderbox"),
			new TaskMapping(VarPlayerID.MORYTANIA_ACHIEVEMENT_DIARY2, 5, "Cast Fertile Soil on the herb patch in Morytania."),
			new TaskMapping(VarPlayerID.MORYTANIA_ACHIEVEMENT_DIARY2, 6, "Craft a black dragon hide body."),
			new TaskMapping(VarPlayerID.MORYTANIA_ACHIEVEMENT_DIARY2, 7, "Kill an Abyssal demon."),
			new TaskMapping(VarPlayerID.MORYTANIA_ACHIEVEMENT_DIARY2, 8, "Loot the chest wearing a complete set of barrows gear."),
		});
		
		DIARY_MAPPINGS.put("morytania", morytania);
	}
	
	private static void initializeVarrockMappings()
	{
		Map<String, TaskMapping[]> varrock = new HashMap<>();
		
		// Varrock Easy - VarPlayerID.VARROCK_ACHIEVEMENT_DIARY
		// Based on quest-helper VarrockEasy.java
		varrock.put("easy", new TaskMapping[]{
			new TaskMapping(VarPlayerID.VARROCK_ACHIEVEMENT_DIARY, 1, "Browse Thessalia's store"),
			new TaskMapping(VarPlayerID.VARROCK_ACHIEVEMENT_DIARY, 2, "Teleport to the essence mine via Aubury"),
			new TaskMapping(VarPlayerID.VARROCK_ACHIEVEMENT_DIARY, 3, "Mine iron south-east of Varrock"),
			new TaskMapping(VarPlayerID.VARROCK_ACHIEVEMENT_DIARY, 4, "Make a regular plank at the sawmill"),
			new TaskMapping(VarPlayerID.VARROCK_ACHIEVEMENT_DIARY, 5, "Go to the 2nd floor of the Stronghold of Security"),
			new TaskMapping(VarPlayerID.VARROCK_ACHIEVEMENT_DIARY, 6, "Jump the fence south of Varrock"),
			new TaskMapping(VarPlayerID.VARROCK_ACHIEVEMENT_DIARY, 7, "Chop down a dying tree in the sawmill area"),
			new TaskMapping(VarPlayerID.VARROCK_ACHIEVEMENT_DIARY, 8, "Speak with Benny in the Varrock Square to purchase a newspaper"),
			new TaskMapping(VarPlayerID.VARROCK_ACHIEVEMENT_DIARY, 9, "Give the stray dog a bone"),
			new TaskMapping(VarPlayerID.VARROCK_ACHIEVEMENT_DIARY, 10, "Put the unfired bowl in the oven"),
			new TaskMapping(VarPlayerID.VARROCK_ACHIEVEMENT_DIARY, 11, "Get more kudos (50+ Kudos)"),
			new TaskMapping(VarPlayerID.VARROCK_ACHIEVEMENT_DIARY, 12, "Craft an earth rune"),
			new TaskMapping(VarPlayerID.VARROCK_ACHIEVEMENT_DIARY, 13, "Fish a trout in the River Lum at Barbarian Village"),
			new TaskMapping(VarPlayerID.VARROCK_ACHIEVEMENT_DIARY, 14, "Steal from the tea stall in Varrock"),
		});
		
		// Varrock Medium - SAME VarPlayerID, different bit range!
		// Based on quest-helper VarrockMedium.java
		// Note: Bit 17 is skipped!
		varrock.put("medium", new TaskMapping[]{
			new TaskMapping(VarPlayerID.VARROCK_ACHIEVEMENT_DIARY, 15, "Speak with the apothecary to create a strength potion"),
			new TaskMapping(VarPlayerID.VARROCK_ACHIEVEMENT_DIARY, 16, "Enter the Champions' Guild"),
			new TaskMapping(VarPlayerID.VARROCK_ACHIEVEMENT_DIARY, 18, "Select a colour for your kitten"),
			new TaskMapping(VarPlayerID.VARROCK_ACHIEVEMENT_DIARY, 19, "Use the spirit tree north of Varrock"),
			new TaskMapping(VarPlayerID.VARROCK_ACHIEVEMENT_DIARY, 20, "Use the flap, slap head, idea, and stamp emotes"),
			new TaskMapping(VarPlayerID.VARROCK_ACHIEVEMENT_DIARY, 21, "Enter the Tolna dungeon after completing A Soul's Bane"),
			new TaskMapping(VarPlayerID.VARROCK_ACHIEVEMENT_DIARY, 22, "Teleport to the digsite using a Digsite pendant"),
			new TaskMapping(VarPlayerID.VARROCK_ACHIEVEMENT_DIARY, 23, "Cast the teleport to Varrock spell"),
			new TaskMapping(VarPlayerID.VARROCK_ACHIEVEMENT_DIARY, 24, "Get a Slayer task from Vannaka"),
			new TaskMapping(VarPlayerID.VARROCK_ACHIEVEMENT_DIARY, 25, "Make 20 Mahogany Planks in one go"),
			new TaskMapping(VarPlayerID.VARROCK_ACHIEVEMENT_DIARY, 26, "Pick a White tree fruit"),
			new TaskMapping(VarPlayerID.VARROCK_ACHIEVEMENT_DIARY, 27, "Use the balloon to travel from Varrock"),
			new TaskMapping(VarPlayerID.VARROCK_ACHIEVEMENT_DIARY, 28, "Complete a lap of the Varrock Agility course"),
		});
		
		// Varrock Hard - Uses bits 29-31 in primary, then overflows to VARROCK_ACHIEVEMENT_DIARY2
		// Based on quest-helper VarrockHard.java
		varrock.put("hard", new TaskMapping[]{
			new TaskMapping(VarPlayerID.VARROCK_ACHIEVEMENT_DIARY, 29, "Trade furs with the Fancy Dress Seller for a spottier cape and equip it"),
			new TaskMapping(VarPlayerID.VARROCK_ACHIEVEMENT_DIARY, 30, "Speak to Orlando Smith when you have achieved 153 Kudos"),
			new TaskMapping(VarPlayerID.VARROCK_ACHIEVEMENT_DIARY, 31, "Make a Waka canoe near Edgeville"),
			new TaskMapping(VarPlayerID.VARROCK_ACHIEVEMENT_DIARY2, 0, "Teleport to Paddewwa"),
			new TaskMapping(VarPlayerID.VARROCK_ACHIEVEMENT_DIARY2, 1, "Teleport to Barbarian Village with a skull sceptre"),
			new TaskMapping(VarPlayerID.VARROCK_ACHIEVEMENT_DIARY2, 2, "Chop some yew logs in Varrock and burn them at the top of the Varrock church"),
			new TaskMapping(VarPlayerID.VARROCK_ACHIEVEMENT_DIARY2, 3, "Have the Varrock estate agent decorate your house with Fancy Stone"),
			new TaskMapping(VarPlayerID.VARROCK_ACHIEVEMENT_DIARY2, 4, "Collect at least 2 yew roots from the Tree patch in Varrock Palace"),
			new TaskMapping(VarPlayerID.VARROCK_ACHIEVEMENT_DIARY2, 5, "Pray at the altar in Varrock palace with Smite active"),
			new TaskMapping(VarPlayerID.VARROCK_ACHIEVEMENT_DIARY2, 6, "Squeeze through the obstacle pipe in Edgeville dungeon"),
		});
		
		// Varrock Elite - Uses VARROCK_ACHIEVEMENT_DIARY2
		// Based on quest-helper VarrockElite.java
		varrock.put("elite", new TaskMapping[]{
			new TaskMapping(VarPlayerID.VARROCK_ACHIEVEMENT_DIARY2, 7, "Create a super combat potion in Varrock west bank"),
			new TaskMapping(VarPlayerID.VARROCK_ACHIEVEMENT_DIARY2, 8, "Use Lunar magic to make 20 mahogany planks at the Lumberyard"),
			new TaskMapping(VarPlayerID.VARROCK_ACHIEVEMENT_DIARY2, 9, "Bake a summer pie in the Cooking Guild"),
			new TaskMapping(VarPlayerID.VARROCK_ACHIEVEMENT_DIARY2, 10, "Smith and fletch ten rune darts within Varrock"),
			new TaskMapping(VarPlayerID.VARROCK_ACHIEVEMENT_DIARY2, 11, "Craft 100 or more earth runes simultaneously from Essence"),
		});
		
		DIARY_MAPPINGS.put("varrock", varrock);
	}
	
	private static void initializeWesternMappings()
	{
		Map<String, TaskMapping[]> western = new HashMap<>();
		
		// Western Easy - bits 1-11
		western.put("easy", new TaskMapping[]{
			new TaskMapping(VarPlayerID.WESTERN_ACHIEVEMENT_DIARY, 1, "Catch a copper longtail."),
			new TaskMapping(VarPlayerID.WESTERN_ACHIEVEMENT_DIARY, 2, "Complete a novice game of Pest Control."),
			new TaskMapping(VarPlayerID.WESTERN_ACHIEVEMENT_DIARY, 3, "Mine some iron ore near Piscatoris."),
			new TaskMapping(VarPlayerID.WESTERN_ACHIEVEMENT_DIARY, 4, "Complete a lap of the Gnome Agility Course."),
			new TaskMapping(VarPlayerID.WESTERN_ACHIEVEMENT_DIARY, 5, "Score a goal in a Gnome Ball match. Talk to the Referee to start the match."),
			new TaskMapping(VarPlayerID.WESTERN_ACHIEVEMENT_DIARY, 6, "Claim any Chompy bird hat from Rantz. Kill chompy birds until you have 30 kills."),
			new TaskMapping(VarPlayerID.WESTERN_ACHIEVEMENT_DIARY, 7, "Teleport to Pest Control using the minigame teleport."),
			new TaskMapping(VarPlayerID.WESTERN_ACHIEVEMENT_DIARY, 8, "Collect a swamp toad at the Gnome Stronghold."),
			new TaskMapping(VarPlayerID.WESTERN_ACHIEVEMENT_DIARY, 9, "Have Brimstail teleport you to the Essence mine"),
			new TaskMapping(VarPlayerID.WESTERN_ACHIEVEMENT_DIARY, 10, "Fletch an oak shortbow in the Gnome Stronghold."),
			new TaskMapping(VarPlayerID.WESTERN_ACHIEVEMENT_DIARY, 11, "Kill a terrorbird in the terrorbird enclosure."),
		});
		
		// Western Medium - bits 12-24
		western.put("medium", new TaskMapping[]{
			new TaskMapping(VarPlayerID.WESTERN_ACHIEVEMENT_DIARY, 12, "Take the agility shortcut from the Grand Tree to Otto's Grotto."),
			new TaskMapping(VarPlayerID.WESTERN_ACHIEVEMENT_DIARY, 13, "Travel to the Gnome Stronghold by spirit tree."),
			new TaskMapping(VarPlayerID.WESTERN_ACHIEVEMENT_DIARY, 14, "Place logs over a pit in the Feldip hunter area, and poke a larupia with a teasing stick."),
			new TaskMapping(VarPlayerID.WESTERN_ACHIEVEMENT_DIARY, 15, "Travel to Ape Atoll."),
			new TaskMapping(VarPlayerID.WESTERN_ACHIEVEMENT_DIARY, 16, "Travel to Ape Atoll."),
			new TaskMapping(VarPlayerID.WESTERN_ACHIEVEMENT_DIARY, 17, "Complete an intermediate game of Pest Control."),
			new TaskMapping(VarPlayerID.WESTERN_ACHIEVEMENT_DIARY, 18, "Travel to the Feldip Hills by Gnome Glider."),
			new TaskMapping(VarPlayerID.WESTERN_ACHIEVEMENT_DIARY, 19, "Claim any Chompy bird hat from Rantz. Kill chompy birds until you have 125 kills."),
			new TaskMapping(VarPlayerID.WESTERN_ACHIEVEMENT_DIARY, 20, "Use a rope on the Jungle Eagle to travel to the Feldip Hills area."),
			new TaskMapping(VarPlayerID.WESTERN_ACHIEVEMENT_DIARY, 21, "Make a chocolate bomb."),
			new TaskMapping(VarPlayerID.WESTERN_ACHIEVEMENT_DIARY, 22, "Complete a delivery for the Gnome Restaurant."),
			new TaskMapping(VarPlayerID.WESTERN_ACHIEVEMENT_DIARY, 23, "Turn your crystal saw seed into a crystal saw."),
			new TaskMapping(VarPlayerID.WESTERN_ACHIEVEMENT_DIARY, 24, "Mine some gold ore underneath the Grand Tree."),
		});
		
		// Western Hard - bits 25-31, then DIARY2 0-5
		western.put("hard", new TaskMapping[]{
			new TaskMapping(VarPlayerID.WESTERN_ACHIEVEMENT_DIARY, 25, "Kill an elf with a crystal bow."),
			new TaskMapping(VarPlayerID.WESTERN_ACHIEVEMENT_DIARY, 26, "Cook your monkfish in the Piscatoris Fishing Colony."),
			new TaskMapping(VarPlayerID.WESTERN_ACHIEVEMENT_DIARY, 27, "Complete a veteran game of Pest Control."),
			new TaskMapping(VarPlayerID.WESTERN_ACHIEVEMENT_DIARY, 28, "Catch a dashing kebbit with your falcon!"),
			new TaskMapping(VarPlayerID.WESTERN_ACHIEVEMENT_DIARY, 29, " Complete a lap of the Ape Atoll Agility Course."),
			new TaskMapping(VarPlayerID.WESTERN_ACHIEVEMENT_DIARY, 30, "Burn some mahogany logs on Ape Atoll."),
			new TaskMapping(VarPlayerID.WESTERN_ACHIEVEMENT_DIARY, 31, "Mine some adamantite ore in Tirannwn."),
			new TaskMapping(VarPlayerID.WESTERN_ACHIEVEMENT_DIARY2, 0, "Check the health of your palm tree in Lletya. It will take about 16 hours to grow fully."),
			new TaskMapping(VarPlayerID.WESTERN_ACHIEVEMENT_DIARY2, 1, "Claim any Chompy bird hat from Rantz. Kill chompy birds until you have 300 kills."),
			new TaskMapping(VarPlayerID.WESTERN_ACHIEVEMENT_DIARY2, 2, "Build an Isafdar painting in your POH Quest Hall."),
			new TaskMapping(VarPlayerID.WESTERN_ACHIEVEMENT_DIARY2, 3, "Kill Zulrah."),
			new TaskMapping(VarPlayerID.WESTERN_ACHIEVEMENT_DIARY2, 4, "Cast teleport to Ape Atoll."),
			new TaskMapping(VarPlayerID.WESTERN_ACHIEVEMENT_DIARY2, 5, "Pickpocket a gnome."),
		});
		
		// Western Elite - DIARY2 bits 6-9,12-14 (gaps at 10-11)
		western.put("elite", new TaskMapping[]{
			new TaskMapping(VarPlayerID.WESTERN_ACHIEVEMENT_DIARY2, 6, "Fletch a magic longbow in Tirannwn."),
			new TaskMapping(VarPlayerID.WESTERN_ACHIEVEMENT_DIARY2, 7, "Defeat the Thermonuclear smoke devil. You are allowed one kill off-task for the diary."),
			new TaskMapping(VarPlayerID.WESTERN_ACHIEVEMENT_DIARY2, 8, "Have Prissy Scilla protect your magic tree."),
			new TaskMapping(VarPlayerID.WESTERN_ACHIEVEMENT_DIARY2, 9, "Use the advanced elven overpass cliffside shortcut."),
			new TaskMapping(VarPlayerID.WESTERN_ACHIEVEMENT_DIARY2, 12, "Equip any complete void set."),
			new TaskMapping(VarPlayerID.WESTERN_ACHIEVEMENT_DIARY2, 13, "Claim any Chompy bird hat from Rantz. Kill chompy birds until you have 1000 kills."),
			new TaskMapping(VarPlayerID.WESTERN_ACHIEVEMENT_DIARY2, 14, "Pickpocket an elf."),
		});
		
		DIARY_MAPPINGS.put("western_provinces", western);
	}
	
	private static void initializeWildernessMappings()
	{
		Map<String, TaskMapping[]> wilderness = new HashMap<>();
		
		// Wilderness Easy - bits 1-12
		wilderness.put("easy", new TaskMapping[]{
			new TaskMapping(VarPlayerID.WILDERNESS_ACHIEVEMENT_DIARY, 1, "Cast Low Alchemy at the Fountain of Rune"),
			new TaskMapping(VarPlayerID.WILDERNESS_ACHIEVEMENT_DIARY, 2, "Pull the Wilderness Lever in Edgeville"),
			new TaskMapping(VarPlayerID.WILDERNESS_ACHIEVEMENT_DIARY, 3, "Pray at the Chaos Altar"),
			new TaskMapping(VarPlayerID.WILDERNESS_ACHIEVEMENT_DIARY, 4, "Enter the Chaos Temple"),
			new TaskMapping(VarPlayerID.WILDERNESS_ACHIEVEMENT_DIARY, 5, "Kill a Mammoth"),
			new TaskMapping(VarPlayerID.WILDERNESS_ACHIEVEMENT_DIARY, 6, "Kill an Earth Warrior"),
			new TaskMapping(VarPlayerID.WILDERNESS_ACHIEVEMENT_DIARY, 7, "Have a prayer point restored in the Demonic Ruins"),
			new TaskMapping(VarPlayerID.WILDERNESS_ACHIEVEMENT_DIARY, 8, "Enter the King Black Dragon's Lair"),
			new TaskMapping(VarPlayerID.WILDERNESS_ACHIEVEMENT_DIARY, 9, "Collect 5 red spiders' eggs from the Wilderness"),
			new TaskMapping(VarPlayerID.WILDERNESS_ACHIEVEMENT_DIARY, 10, "Mine some Iron Ore in the Wilderness"),
			new TaskMapping(VarPlayerID.WILDERNESS_ACHIEVEMENT_DIARY, 11, "Enter the Abyss"),
			new TaskMapping(VarPlayerID.WILDERNESS_ACHIEVEMENT_DIARY, 12, "Equip a team cape"),
		});
		
		// Wilderness Medium - bits 13-24 (skip bit 17)
		wilderness.put("medium", new TaskMapping[]{
			new TaskMapping(VarPlayerID.WILDERNESS_ACHIEVEMENT_DIARY, 13, "Mine some mithril in the Wilderness"),
			new TaskMapping(VarPlayerID.WILDERNESS_ACHIEVEMENT_DIARY, 14, "Kill an Ent and cut yew logs from its trunk"),
			new TaskMapping(VarPlayerID.WILDERNESS_ACHIEVEMENT_DIARY, 15, "Enter the Wilderness God Wars Dungeon"),
			new TaskMapping(VarPlayerID.WILDERNESS_ACHIEVEMENT_DIARY, 16, "Complete a lap of the Wilderness Agility Course"),
			new TaskMapping(VarPlayerID.WILDERNESS_ACHIEVEMENT_DIARY, 18, "Kill a Green dragon in the Wilderness Slayer Cave"),
			new TaskMapping(VarPlayerID.WILDERNESS_ACHIEVEMENT_DIARY, 19, "Kill an Ankou in the Wilderness Slayer Cave"),
			new TaskMapping(VarPlayerID.WILDERNESS_ACHIEVEMENT_DIARY, 20, "Charge an earth orb"),
			new TaskMapping(VarPlayerID.WILDERNESS_ACHIEVEMENT_DIARY, 21, "Kill a Bloodveld in the Wilderness God Wars Dungeon"),
			new TaskMapping(VarPlayerID.WILDERNESS_ACHIEVEMENT_DIARY, 22, "Speak with the Emblem Trader in Edgeville"),
			new TaskMapping(VarPlayerID.WILDERNESS_ACHIEVEMENT_DIARY, 23, "Smith a gold helmet in the Resource Area"),
			new TaskMapping(VarPlayerID.WILDERNESS_ACHIEVEMENT_DIARY, 24, "Open the Muddy chest"),
		});
		
		// Wilderness Hard - bits 25-31, then DIARY2 0-2
		wilderness.put("hard", new TaskMapping[]{
			new TaskMapping(VarPlayerID.WILDERNESS_ACHIEVEMENT_DIARY, 25, "Cast a god spell against another player in the Wilderness"),
			new TaskMapping(VarPlayerID.WILDERNESS_ACHIEVEMENT_DIARY, 26, "Charge an air orb"),
			new TaskMapping(VarPlayerID.WILDERNESS_ACHIEVEMENT_DIARY, 27, "Catch a Black salamander"),
			new TaskMapping(VarPlayerID.WILDERNESS_ACHIEVEMENT_DIARY, 28, "Smith an Adamant scimitar in the Resource Area"),
			new TaskMapping(VarPlayerID.WILDERNESS_ACHIEVEMENT_DIARY, 29, "Kill a Lava dragon and bury its bones on Lava Dragon Isle"),
			new TaskMapping(VarPlayerID.WILDERNESS_ACHIEVEMENT_DIARY, 30, "Kill the Chaos Elemental"),
			new TaskMapping(VarPlayerID.WILDERNESS_ACHIEVEMENT_DIARY, 31, "Kill Crazy archaeologist, Chaos Fanatic, and Scorpia"),
			new TaskMapping(VarPlayerID.WILDERNESS_ACHIEVEMENT_DIARY2, 0, "Take the agility shortcut from Trollheim to the Wilderness"),
			new TaskMapping(VarPlayerID.WILDERNESS_ACHIEVEMENT_DIARY2, 1, "Kill a Spiritual warrior in the Wilderness God Wars Dungeon"),
			new TaskMapping(VarPlayerID.WILDERNESS_ACHIEVEMENT_DIARY2, 2, "Fish a raw lava eel in the Lava Maze"),
		});
		
		// Wilderness Elite - DIARY2 bits 3,5,7-11 (gaps at 4,6)
		wilderness.put("elite", new TaskMapping[]{
			new TaskMapping(VarPlayerID.WILDERNESS_ACHIEVEMENT_DIARY2, 3, "Kill Callisto, Venenatis, and Vet'ion"),
			new TaskMapping(VarPlayerID.WILDERNESS_ACHIEVEMENT_DIARY2, 5, "Teleport to Ghorrock"),
			new TaskMapping(VarPlayerID.WILDERNESS_ACHIEVEMENT_DIARY2, 7, "Fish a dark crab in the Resource Area"),
			new TaskMapping(VarPlayerID.WILDERNESS_ACHIEVEMENT_DIARY2, 8, "Smith a runite scimitar in the Resource Area"),
			new TaskMapping(VarPlayerID.WILDERNESS_ACHIEVEMENT_DIARY2, 9, "Steal from the chest in Rogues' Castle"),
			new TaskMapping(VarPlayerID.WILDERNESS_ACHIEVEMENT_DIARY2, 10, "Kill a Spiritual mage in the Wilderness God Wars Dungeon"),
			new TaskMapping(VarPlayerID.WILDERNESS_ACHIEVEMENT_DIARY2, 11, "Chop and burn some magic logs in the Resource Area"),
		});
		
		DIARY_MAPPINGS.put("wilderness", wilderness);
	}
	
	/**
	 * Gets the task mappings for a specific diary and tier.
	 * @param diaryName The diary name (e.g., "varrock", "ardougne")
	 * @param tierName The tier name (e.g., "easy", "medium", "hard", "elite")
	 * @return Array of TaskMappings, or null if not found
	 */
	public static TaskMapping[] getTaskMappings(String diaryName, String tierName)
	{
		Map<String, TaskMapping[]> diary = DIARY_MAPPINGS.get(diaryName);
		if (diary == null)
		{
			return null;
		}
		return diary.get(tierName);
	}
	
	/**
	 * Checks if a specific task is completed using bitpacking.
	 * @param client The RuneLite client
	 * @param varplayerId The varplayer ID to check
	 * @param bitPosition The bit position within the varplayer
	 * @return true if the task is completed, false otherwise
	 */
	public static boolean isTaskCompleted(net.runelite.api.Client client, int varplayerId, int bitPosition)
	{
		if (client == null)
		{
			return false;
		}

		int varplayerValue = client.getVarpValue(varplayerId);
		return (varplayerValue & (1 << bitPosition)) != 0;
	}

	/** Overload that dispatches on the TaskMapping type (varbit vs varplayer). */
	public static boolean isTaskCompleted(net.runelite.api.Client client, TaskMapping mapping)
	{
		if (client == null)
		{
			return false;
		}

		if (mapping.bitPosition == -1)
		{
			return client.getVarbitValue(mapping.varplayerId) >= mapping.varbitMinValue;
		}

		return (client.getVarpValue(mapping.varplayerId) & (1 << mapping.bitPosition)) != 0;
	}
}
