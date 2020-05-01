package com.harmony.harmonymod;

import com.harmony.harmonymod.sounds.SoundDB;
import com.harmony.harmonymod.Traits.TRAIT;
import com.harmony.harmonymod.items.ItemDiagnostic;
import com.harmony.harmonymod.items.GenericEntitySpawnEgg;
import com.harmony.harmonymod.FeedAnimal;
import com.harmony.harmonymod.RespawnAnimal;
import com.harmony.harmonymod.horsefix.RiddingMoveUpdate;
import net.minecraft.init.Blocks;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.registry.EntityRegistry;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraft.item.Item;
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

	public static boolean isRiddingUpdated;

	@EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		//Read in configuration values
		config = new Configuration(event.getSuggestedConfigurationFile());
		syncConfig();

		int mobID = 0;

		//add blocks and items
		/*
		 * Example Item
		Item skillpoint = new Item() {
		public ItemStack onItemRightClick(ItemStack is, World w, EntityPlayer p) {
			return is;
		}
		}.setUnlocalizedName("skill_point").setCreativeTab(CreativeTabs.tabMisc);
		GameRegistry.registerItem(skillpoint, "skill_point");
		 */

		/*
		 * Armor bundle WIP
		Item armor_bundle = new ItemArmorBundle().setUnlocalizedName("armor_bundle").setCreativeTab(CreativeTabs.tabMisc);
		armor_bundle.setTextureName(ExampleMod.MODID + ":" + "ArmorBag");

		GameRegistry.registerItem(armor_bundle, "armor_bundle");
		 */

		// Item itemDiag = new ItemDiagnostic("Diagnostics");

		// add Entities
		/*
		EntityRegistry.registerModEntity(HarmonyHorse.class, "HarmonyHorse", mobID++, this, 80, 3, false);

		Item horseSpawnEgg = new GenericEntitySpawnEgg("HarmonyHorse", 0xE18519, 0x000000)
			.setUnlocalizedName("spawn_egg_harmony_horse")
			.setTextureName(HarmonyMod.MODID+":spawn_egg");
		GameRegistry.registerItem(horseSpawnEgg, "spawnEggHarmonyHorse");*/
	}

	/*
	 * Stage 2 initialization, after all items and entities exist
	 */
	@EventHandler
	public void init(FMLInitializationEvent event)
	{
		//add crafting recipes and event handlers
		HarmonyProps.register();
		SoundDB.getSoundDB();
		FeedAnimal.register();
		RespawnAnimal.register();
		RiddingMoveUpdate.register();
		Traits.register();
	}

	/*
	 * Load configuration data from file
	 */
	public static void syncConfig() {
		try {
			config.load();
			//useful methods
			//config.get("category name","key","default value","comment");
			//config.getCategoryNames();
			//config.hasKey(c,Integer.toString(i)))
			
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
					System.out.println("HarmonyMod: config setup for " + s);
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

		config.get("EntityCow", "slot1", "VICIOUS");
		config.get("EntityCow", "slot2", "VICIOUS");
		config.get("EntityCow", "slot3", "VICIOUS");
		config.get("EntityCow", "needsAttackAttr", true);

		config.get("EntitySheep", "slot1", "HARDY");
		config.get("EntitySheep", "slot2", "HARDY");
		config.get("EntitySheep", "slot3", "HARDY");
		config.get("EntitySheep", "needsAttackAttr", true);


		config.get("EntityPig", "slot1", "FAST");
		config.get("EntityPig", "slot2", "FAST");
		config.get("EntityPig", "slot3", "FAST");
		config.get("EntityPig", "needsAttackAttr", true);


		config.get("EntityChicken", "slot1", "FERTILE");
		config.get("EntityChicken", "slot2", "FERTILE");
		config.get("EntityChicken", "slot3", "FERTILE");
		config.get("EntityChicken", "needsAttackAttr", true);


		config.get("EntityWolf", "slot1", "JUMP,HARDY,FAST,VICIOUS");
		config.get("EntityWolf", "slot2", "JUMP,HARDY,FAST,VICIOUS");
		config.get("EntityWolf", "slot3", "JUMP,HARDY,FAST,VICIOUS");
		config.get("EntityWolf", "needsAttackAttr", true);

		config.get("EntityHorse", "slot1", "HARDY");
		config.get("EntityHorse", "slot2", "HARDY");
		config.get("EntityHorse", "slot3", "HARDY");
		config.get("EntityHorse", "needsAttackAttr", true);

		config.save();

	}

	private static List<TRAIT> parse(String in) {
		List<TRAIT> out = new ArrayList<TRAIT>();
		
		String[] traits = in.split(",");
		for(String s : traits) {
			TRAIT toAdd = TRAIT.NONE;
			for(TRAIT t : TRAIT.values()) {
				if(t.toString().equals(s)) {
					toAdd = t;
				}
			}

			if(toAdd == TRAIT.NONE) {
				System.out.println("HarmonyMod: Could not parse config value " + s);
			}

			out.add(toAdd);
		}

		if(out.size() == 0)
			out.add(TRAIT.NONE);

		return out;
	}

}
