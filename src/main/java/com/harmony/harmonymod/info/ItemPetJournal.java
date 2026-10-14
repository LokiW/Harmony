package com.harmony.harmonymod.info;

import com.harmony.harmonymod.items.HarmonyItem;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import java.util.List;

/*
 * Hold it and look at an animal to read about it (JournalOverlay draws the page)
 */
public class ItemPetJournal extends HarmonyItem {

	public ItemPetJournal(String name) {
		super(name);
		this.setMaxStackSize(1);
	}

	public void register() {
		GameRegistry.addShapelessRecipe(new ItemStack(this), Items.book, Items.bone);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack stack, EntityPlayer player, List lines, boolean advanced) {
		lines.add(StatCollector.translateToLocal("harmony.journal.tooltip"));
	}
}
