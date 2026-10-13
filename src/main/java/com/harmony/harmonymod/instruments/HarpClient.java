package com.harmony.harmonymod.instruments;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;

/*
 * Client half of the harp: opens the sheet music
 */
@SideOnly(Side.CLIENT)
public class HarpClient {
	public static void openSheet(ItemStack stack) {
		Minecraft.getMinecraft().displayGuiScreen(new GuiHarp(stack));
	}
}
