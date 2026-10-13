package com.harmony.harmonymod.instruments;

import com.harmony.harmonymod.HarmonyProps;
import com.harmony.harmonymod.items.HarmonyItem;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.event.entity.player.EntityInteractEvent;
import java.util.List;

/*
 * A small harp that stores phrases to command animals away from noteblocks. Right click plays the
 * active phrase, sneak + right click opens the sheet music to compose them (GuiHarp).
 */
public class ItemHarp extends HarmonyItem {
	public static final int SLOTS = 4;

	public ItemHarp(String name) {
		super(name);
		this.setMaxStackSize(1);
	}

	/*
	 * Recipe and handlers, during init
	 */
	public void register() {
		GameRegistry.addShapedRecipe(new ItemStack(this),
				" G ",
				"SNS",
				" T ",
				'G', Items.gold_ingot, 'S', Items.stick, 'N', Blocks.noteblock, 'T', Items.string);
		MinecraftForge.EVENT_BUS.register(this);
	}

	@Override
	public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
		if (player.isSneaking()) {
			if (world.isRemote) {
				// Only ever runs on the client, so HarpClient is never loaded on a server
				HarpClient.openSheet(stack);
			}
			return stack;
		}
		if (!world.isRemote) {
			String phrase = getSlot(stack, getActive(stack));
			if (!phrase.isEmpty()) {
				HarpPlayer.play(player, phrase);
			}
		}
		player.swingItem();
		return stack;
	}

	/*
	 * Playing at an animal: don't let vanilla act on the click too (e.g. mounting a horse). The client
	 * then plays the harp as a normal right click. Cancelled on both sides.
	 */
	@SubscribeEvent
	public void onInteract(EntityInteractEvent event) {
		ItemStack held = event.entityPlayer.getCurrentEquippedItem();
		if (held != null && held.getItem() == this && HarmonyProps.get(event.target) != null) {
			event.setCanceled(true);
		}
	}

	public static String getSlot(ItemStack stack, int slot) {
		if (!stack.hasTagCompound()) {
			return "";
		}
		NBTTagList slots = stack.getTagCompound().getTagList("Slots", Constants.NBT.TAG_STRING);
		return slot < slots.tagCount() ? slots.getStringTagAt(slot) : "";
	}

	public static int getActive(ItemStack stack) {
		return stack.hasTagCompound() ? stack.getTagCompound().getInteger("Active") : 0;
	}

	public static void setSlots(ItemStack stack, String[] phrases, int active) {
		NBTTagCompound tag = stack.hasTagCompound() ? stack.getTagCompound() : new NBTTagCompound();
		NBTTagList slots = new NBTTagList();
		for (String phrase : phrases) {
			slots.appendTag(new NBTTagString(phrase));
		}
		tag.setTag("Slots", slots);
		tag.setInteger("Active", active);
		stack.setTagCompound(tag);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack stack, EntityPlayer player, List lines, boolean advanced) {
		int active = getActive(stack);
		String phrase = getSlot(stack, active);
		lines.add(StatCollector.translateToLocalFormatted("harmony.harp.tooltip.active", active + 1,
				phrase.isEmpty() ? StatCollector.translateToLocal("harmony.harp.tooltip.empty") : phrase));
		lines.add(StatCollector.translateToLocal("harmony.harp.tooltip.help"));
	}
}
