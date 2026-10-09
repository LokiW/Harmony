package com.harmony.harmonymod.items;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.util.*;
import cpw.mods.fml.relauncher.*;

public class ItemArmorBundle extends HarmonyItem {
    
    private static final String[] keys = {"Boot", "Leggings", "Chestplate", "Head"}; 

    public ItemArmorBundle(String name) {
        super(name);
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        NBTTagCompound tag;
        boolean is_armor_bundle_empty;
        if (stack.hasTagCompound()) {
            tag = stack.getTagCompound();
            is_armor_bundle_empty = false;
        } else {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
            is_armor_bundle_empty = true;
        }

        ItemStack[] armor = new ItemStack[4];

        for (int i = 0; i < armor.length; i++) {
            armor[i] = player.getCurrentArmor(i);
        }


        //Get armor off player and bundle it
        if (is_armor_bundle_empty) {
            for (int i = 0; i < armor.length; i++) {
                if (armor[i] != null) {
                    NBTTagCompound nbtcompound = new NBTTagCompound();
                    armor[i].writeToNBT(nbtcompound);
                    tag.setTag(keys[i], nbtcompound);
                    player.setCurrentItemOrArmor(i+1, null);
                }
            }
        //Return armor to player
        } else {
            //Verify you can return armor to player, return if not
            for (int i = 0; i < armor.length; i++) {
                if (tag.hasKey(keys[i]) && armor[i] != null) {
                    return stack;
                }
            }
            
            //Return each armor
            for (int i = 0; i < armor.length; i++) {
                if (tag.hasKey(keys[i]) && armor[i] == null) {
                    NBTTagCompound nbtcompound = tag.getCompoundTag(keys[i]);
                    ItemStack armor_piece = ItemStack.loadItemStackFromNBT(nbtcompound);
                    player.setCurrentItemOrArmor(i+1, armor_piece);
                    tag.removeTag(keys[i]);
                }
            }
            stack.setTagCompound(null);
        }
        return stack;
    }

	/*
	 * Produce Hover-over text
	 */
    @Override
    public void addInformation(ItemStack stack, EntityPlayer player, List lores, boolean b) {
        if (stack.hasTagCompound()) {
            NBTTagCompound tag = stack.getTagCompound();
            for (int i = 0; i < keys.length; i++) {
                if (tag.hasKey(keys[i])) {
                    NBTTagCompound nbtcompound = tag.getCompoundTag(keys[i]);
                    ItemStack armor_piece = ItemStack.loadItemStackFromNBT(nbtcompound);
                    lores.add(armor_piece.getDisplayName());
                }
            }
        }
    }
    
	//TODO get rendering to work
    /*
    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister register) {
        int icon_count = 0;
        if (!icons.containsKey("default")) {
            String textureName = ExampleMod.MODID + ":" + "TestTexture";
            if (!TextureHelper.itemTextureExists(textureName)) {
                System.out.println("TEXTURE DOES NOT EXIST");
            }
            icons.put("default", register.registerIcon(ExampleMod.MODID + ":" + "TestTexture"));
            System.out.println("}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}REGISTERING ICONS=====================");
            System.out.println(icons.get("default"));
            if (name == "armor_bundle") {
                icons.put("open", register.registerIcon(this.iconString + "_open"));
                System.out.println(icons.get("open"));
            }
       
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(ItemStack stack, int render_pass) {
        return icons.get("default");
        
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIconFromDamage(int meta)
    {
        return icons.get("default");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean requiresMultipleRenderPasses() {
        return false;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public final int getRenderPasses(int metadata) {
        return 0;
    }*/
    
}


