package com.harmony.harmonymod;

import com.harmony.harmonymod.sounds.SoundDB;
import com.harmony.harmonymod.Traits.TRAIT;
import com.harmony.harmonymod.items.ItemDiagnostic;
import com.harmony.harmonymod.horsefix.HorseControl;
import com.harmony.harmonymod.sync.AnimalSync;
import com.harmony.harmonymod.ownership.Ownership;
import com.harmony.harmonymod.tricks.Sit;
import com.harmony.harmonymod.horsefix.HorseControlClient;
import com.harmony.harmonymod.client.LearningIndicator;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;


/*
 * Root file for mod, that causes all other Components to link up
 */
@Mod(modid = HarmonyMod.MODID, name = HarmonyMod.NAME, version = HarmonyMod.VERSION)
public class HarmonyMod
{
	public static final String NAME = "Harmony";
	public static final String MODID = "harmony";
	public static final String VERSION = "1.0";

	/**
	 * Configuration fields, referenced by other parts of the code as constants
	 */
	public static Configuration config;

	//default traits that can appear on wild animals
	public static Map<String, List<TRAIT>> slot1 = new HashMap<String, List<TRAIT>>();
	public static Map<String, List<TRAIT>> slot2 = new HashMap<String, List<TRAIT>>();
	public static Map<String, List<TRAIT>> slot3 = new HashMap<String, List<TRAIT>>();
	
	//whether we need to add an attack damage attribute to a creature
	public static Set<String> needsAttackAttr = new HashSet<String>();
	//whether this mob needs harmony props
	public static Set<String> harmonyMobs = new HashSet<String>();

	//constants related to breeding's interactions with happiness
	public static int breedingHappiness;
	public static int breedingCost;

	@EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		//Read in configuration values
		config = new Configuration(event.getSuggestedConfigurationFile());
		syncConfig();

		// Before textures are first stitched, so its particle icon gets registered
		if (event.getSide().isClient()) {
			LearningIndicator.register();
		}

		new ItemDiagnostic("diagnostics");
		Ownership.registerItems();
	}

	/*
	 * Stage 2 initialization, after all items and entities exist
	 */
	@EventHandler
	public void init(FMLInitializationEvent event)
	{
		//add crafting recipes and event handlers
		HarmonyNetwork.register();
		HarmonyProps.register();
		AnimalSync.register();
		Ownership.register();
		Sit.register();
		SoundDB.getSoundDB();
		FeedAnimal.register();
		RespawnAnimal.register();
		HorseControl.register();
		if (event.getSide().isClient()) {
			HorseControlClient.register();
		}
		Traits.register();
	}

	@EventHandler
	public void serverStarting(FMLServerStartingEvent event) {
		event.registerServerCommand(new HarmonyCommand());
	}

	/*
	 * Load configuration data from file
	 */
	public static void syncConfig() {
		try {
			config.load();

			if(config.getCategoryNames().size() < 2) {
				generateConfig();
			}

			for(String s : config.getCategoryNames()) {
				if(s.equals(MODID)) {
					//general config values
					Property p;
					p = config.get(MODID, "breedingHappiness", 10);
					breedingHappiness = p.getInt();
					p = config.get(MODID, "breedingCost", 5);
					breedingCost = p.getInt();
				} else {
					//for each mob
					Property p;
					p = config.get(s, "slot1", "");
					slot1.put(s,parse(p.getString()));

					p = config.get(s, "slot2", "");
					slot2.put(s,parse(p.getString()));
					
					p = config.get(s, "slot3", "");
					slot3.put(s,parse(p.getString()));
				
					p = config.get(s, "needsAttackAttr", false);
					if(p.getBoolean())
						needsAttackAttr.add(s);
	
					harmonyMobs.add(s);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if(config.hasChanged()) {
				config.save();
			}
		}
	}

	private static void generateConfig() {

		config.get(MODID, "breedingHappiness", 10, "required happiness to breed naturally");
		config.get(MODID, "breedingCost", 5, "penalty for successfully breeding");

		// Which traits wild animals can get, per species: good ones that fit the species, and bad ones
		addSpecies("EntityCow", "VICIOUS,HARDY", "SLOW,WEAK");
		addSpecies("EntitySheep", "HARDY,FERTILE", "FRAIL,SLOW");
		addSpecies("EntityPig", "FAST,HARDY", "SLOW,CLUMSY");
		addSpecies("EntityChicken", "FERTILE,JUMP", "FRAIL,CLUMSY");
		addSpecies("EntityWolf", "JUMP,HARDY,FAST,VICIOUS", "SLOW,FRAIL,WEAK,CLUMSY");
		addSpecies("EntityHorse", "FAST,HARDY,JUMP", "SLOW,FRAIL,CLUMSY");

		config.save();

	}

	/*
	 * Each slot picks one entry at random from its list, so repeating an entry makes it more likely.
	 * NONE fills two thirds of each list, so a wild animal usually has no more than one trait.
	 */
	private static void addSpecies(String species, String good, String bad) {
		String traits = good + "," + bad;
		int traitCount = traits.split(",").length;
		String slot = traits;
		for (int i = 0; i < traitCount * 2; i++) {
			slot = "NONE," + slot;
		}
		String comment = "Traits wild animals can get in this slot, one picked at random. Repeat an entry to make it more likely.";
		config.get(species, "slot1", slot, comment);
		config.get(species, "slot2", slot, comment);
		config.get(species, "slot3", slot, comment);
		config.get(species, "needsAttackAttr", true);
	}

	private static List<TRAIT> parse(String in) {
		List<TRAIT> out = new ArrayList<TRAIT>();
		
		String[] traits = in.split(",");
		for(String s : traits) {
			TRAIT toAdd = TRAIT.NONE;
			try {
				toAdd = TRAIT.valueOf(s.trim());
			} catch (IllegalArgumentException e) {
				System.out.println("HarmonyMod: Could not parse config value " + s);
			}

			out.add(toAdd);
		}

		if(out.size() == 0)
			out.add(TRAIT.NONE);

		return out;
	}

}
