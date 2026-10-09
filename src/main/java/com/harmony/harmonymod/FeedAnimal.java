package com.harmony.harmonymod;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.EntityInteractEvent;
import net.minecraftforge.common.MinecraftForge;
import com.harmony.harmonymod.ownership.Ownership;
import com.harmony.harmonymod.sync.AnimalSync;
import net.minecraft.world.WorldServer;

/*
 * Event handler for a player feeding an animal
 */
public class FeedAnimal {

	public static void register() {
		MinecraftForge.EVENT_BUS.register(new FeedAnimal());
	}

	@SubscribeEvent
	public void onEntityInteractEvent(EntityInteractEvent e) {
        /*
         * If entity has harmony props, check if tricks need updating
         *  or home needs to be set.
         */
		Entity e_target = e.target;
		EntityPlayer player = e.entityPlayer;

		if (!(e_target instanceof EntityAnimal)) {
			return;
		}

		ItemStack held_items = player.inventory.getCurrentItem();
		if (held_items == null) {
			return;
		}

		EntityAnimal target = (EntityAnimal) e_target;
		String heldName = held_items.getUnlocalizedName();
		if (target instanceof EntityTameable && HarmonyProps.get(target) != null
				&& ("item.speckledMelon".equals(heldName) || "item.appleGold".equals(heldName))) {
			// Vanilla makes a tamed wolf or ocelot sit (or stand) when its owner right clicks it with
			// anything that isn't food, which gets in the way of training. Cancelled on both sides.
			e.setCanceled(true);
		}
		if (target.worldObj.isRemote) {
			// Only register tricks on server side
			return;
		}

		String itemName = held_items.getUnlocalizedName();
		boolean removeItem = false;
		HarmonyProps hp = HarmonyProps.get(target);

		if (hp == null) {
			System.out.println("HarmonyMod: HarmonyProps are null for " + target);
			return;
		}

		// TODO make our own items for learning tricks, differentiate based on animal
		// If interacting with a speckled melon, indicate animal should begin to learn trick
		if ("item.speckledMelon".equals(itemName)) {
			if (!Ownership.mayUse(player, target, hp)) {
				return;
			}
			if (hp.tricks.learnTrick()) {
				// Learned from the reward
				((WorldServer) target.worldObj).func_147487_a("heart", target.posX, target.posY + target.height + 0.3,
						target.posZ, 7, 0.4, 0.3, 0.4, 0.0);
			}
			// Show the learning question marks starting or stopping straight away
			AnimalSync.syncNow(target);
			removeItem = true;
		// If interacting with a golden apple, animal should save current location and
		//  teleport back to current location instead of dying.
		} else if ("item.appleGold".equals(itemName)) {
			if (!Ownership.mayUse(player, target, hp)) {
				return;
			}
			hp.tricks.xRespawn = target.posX;
			hp.tricks.yRespawn = target.posY;
			hp.tricks.zRespawn = target.posZ;
			removeItem = true;
			System.out.println("HarmonyMod: Set respawn location.");
		}

		if (removeItem) {
			if (held_items.isStackable()) {
				held_items.stackSize--;
			} else {
				player.inventory.setInventorySlotContents(player.inventory.currentItem, null);
			}
		}
	}
}
