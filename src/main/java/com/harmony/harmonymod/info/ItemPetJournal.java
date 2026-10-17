package com.harmony.harmonymod.info;

import com.harmony.harmonymod.HarmonyProps;
import com.harmony.harmonymod.client.JournalOverlay;
import com.harmony.harmonymod.items.HarmonyItem;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.EntityInteractEvent;
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
		MinecraftForge.EVENT_BUS.register(this);
	}

	/*
	 * Right click turns the page
	 */
	@Override
	public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
		if (world.isRemote) {
			// Only ever runs on the client, so JournalOverlay is never loaded on a server
			JournalOverlay.nextPage();
			player.playSound("random.click", 0.3F, 1.6F);
		}
		return stack;
	}

	/*
	 * Clicking an animal with it turns the page too, rather than e.g. mounting a horse or sitting a
	 * wolf. Cancelled on both sides; the client then uses the item as a normal right click.
	 */
	@SubscribeEvent
	public void onInteract(EntityInteractEvent event) {
		ItemStack held = event.entityPlayer.getCurrentEquippedItem();
		if (held != null && held.getItem() == this && HarmonyProps.get(event.target) != null) {
			event.setCanceled(true);
		}
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack stack, EntityPlayer player, List lines, boolean advanced) {
		lines.add(StatCollector.translateToLocal("harmony.journal.tooltip"));
	}
}
