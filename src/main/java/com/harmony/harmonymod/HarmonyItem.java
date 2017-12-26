package com.harmony.harmonymod;

import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.item.Item;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;

import java.util.*;

/*
 * Basics for armor bundles
 */
public class HarmonyItem extends Item {

    public String name;
    public Map<String, IIcon> icons;

    public HarmonyItem(String item_name) {
        name = item_name;
        this.setUnlocalizedName(item_name).setCreativeTab(CreativeTabs.tabMisc);
        icons = new HashMap<String, IIcon>();
        this.setTextureName(HarmonyMod.MODID + ":" + item_name);
        GameRegistry.registerItem(this, item_name);
    }

}


