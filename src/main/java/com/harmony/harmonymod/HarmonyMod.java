package com.harmony.harmonymod;

import net.minecraft.init.Blocks;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraft.item.*;
import net.minecraft.world.*;
import net.minecraft.entity.player.*;
import java.util.*;


/*
 * Root file for mod, that causes all other Components to link up
 */
@Mod(modid = HarmonyMod.MODID, name = HarmonyMod.NAME, version = HarmonyMod.VERSION)
public class HarmonyMod
{
	public static final String NAME = "Harmony";
	public static final String MODID = "harmony";
	public static final String VERSION = "1.0";

	public static Configuration config;

	@EventHandler
	public void preInit(FMLPreInitializationEvent event) {
	    //Read in configuration values
	    config = new Configuration(event.getSuggestedConfigurationFile());
	    syncConfig();

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
	}

	/*
	 * Stage 2 initialization, after all items and entities exist
	 */
	@EventHandler
	public void init(FMLInitializationEvent event)
	{
		//add crafting recipes and event handlers
        HarmonyProps.register();
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
            //config.hasKey(c,Integer.toString(i))) {

        } catch (Exception e) {
            System.out.println("Could not find config for " + MODID + " using defaults");
            e.printStackTrace();
        } finally {
            if(config.hasChanged()) {
                config.save();
            }
        }
            
    }

}
