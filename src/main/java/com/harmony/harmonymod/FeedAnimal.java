package com.harmony.harmonymod;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.Entity;
import net.minecraft.init.Items;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import java.util.List;
import net.minecraftforge.event.entity.player.EntityInteractEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraft.util.ChatComponentTranslation;
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
		if (isDrinkablePotion(held_items) && HarmonyProps.get(target) != null) {
			// Giving it the potion, not vanilla's interaction (e.g. sitting a wolf). Cancelled on both sides.
			e.setCanceled(true);
		}
		if (held_items.getItem() == Items.milk_bucket && HarmonyProps.get(target) != null) {
			// Clearing its training, not vanilla's interaction. Cancelled on both sides.
			e.setCanceled(true);
		}
		if ("item.compass".equals(heldName) && HarmonyProps.get(target) != null) {
			// Setting a place, not vanilla's interaction (e.g. mounting a horse). Cancelled on both sides.
			e.setCanceled(true);
		}
		if (target instanceof EntityHorse && HarmonyProps.get(target) != null && "item.speckledMelon".equals(heldName)) {
			// Same for horses, which the player would mount. Golden apples are horse food, so vanilla keeps those.
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
			// Not an animal Harmony tracks
			return;
		}

		// TODO make our own items for learning tricks, differentiate based on animal
		// If interacting with a speckled melon, indicate animal should begin to learn trick
		if ("item.speckledMelon".equals(itemName)) {
			if (!Ownership.mayUse(player, target, hp)) {
				return;
			}
			hp.kept = true;
			if (hp.tricks.learnTrick()) {
				// Learned from the reward
				((WorldServer) target.worldObj).func_147487_a("heart", target.posX, target.posY + target.height + 0.3,
						target.posZ, 7, 0.4, 0.3, 0.4, 0.0);
				HarmonyAchievements.award(player, HarmonyAchievements.trick);
			} else {
				HarmonyAchievements.award(player, HarmonyAchievements.training);
			}
			// Show the learning question marks starting or stopping straight away
			AnimalSync.syncNow(target);
			removeItem = true;
		// If interacting with a golden apple, animal should save current location and
		//  teleport back to current location instead of dying.
		} else if ("item.compass".equals(itemName)) {
			// Right click with a compass: the spot the animal is on becomes its next place (1, 2, 3, then 1 again)
			if (!Ownership.mayUse(player, target, hp) || !hp.isInitialized()) {
				return;
			}
			hp.kept = true;
			int place = hp.tricks.setNextPlace(target.posX, target.posY, target.posZ);
			HarmonyAchievements.award(player, HarmonyAchievements.place);
			player.addChatComponentMessage(new ChatComponentTranslation("harmony.place.set", place, target.getCommandSenderName()));
			AnimalSync.syncNow(target);
		} else if ("item.appleGold".equals(itemName)) {
			if (!Ownership.mayUse(player, target, hp)) {
				return;
			}
			hp.kept = true;
			hp.tricks.setRespawn(target.posX, target.posY, target.posZ, target.dimension);
			HarmonyAchievements.award(player, HarmonyAchievements.lifesaver);
			// An enchanted golden apple makes it permanent, a plain one after that only moves the point
			if (held_items.getItemDamage() == 1) {
				hp.tricks.permanentRespawn = true;
			}
			player.addChatComponentMessage(new ChatComponentTranslation(hp.tricks.permanentRespawn
					? "harmony.respawn.set_permanent" : "harmony.respawn.set_once", target.getCommandSenderName()));
			removeItem = true;
		}

		if (isDrinkablePotion(held_items) && hp.isInitialized() && Ownership.mayUse(player, target, hp)) {
			// Drinks it, like a player would: its effects, and the bottle back
			for (Object o : ((ItemPotion) held_items.getItem()).getEffects(held_items)) {
				target.addPotionEffect(new PotionEffect((PotionEffect) o));
			}
			target.worldObj.playSoundAtEntity(target, "random.drink", 0.5F, 1.0F);
			if (!player.capabilities.isCreativeMode) {
				held_items.stackSize--;
				if (held_items.stackSize <= 0) {
					player.inventory.setInventorySlotContents(player.inventory.currentItem, new ItemStack(Items.glass_bottle));
				} else if (!player.inventory.addItemStackToInventory(new ItemStack(Items.glass_bottle))) {
					player.dropPlayerItemWithRandomChoice(new ItemStack(Items.glass_bottle), false);
				}
			}
			return;
		}
		if (held_items.getItem() == Items.milk_bucket && hp.isInitialized() && Ownership.mayUse(player, target, hp)) {
			// Milk washes its training away, like it does potion effects
			hp.tricks.clearTraining();
			((WorldServer) target.worldObj).func_147487_a("splash", target.posX, target.posY + target.height,
					target.posZ, 20, 0.4, 0.2, 0.4, 0.0);
			player.addChatComponentMessage(new ChatComponentTranslation("harmony.tricks.cleared", target.getCommandSenderName()));
			AnimalSync.syncNow(target);
			if (!player.capabilities.isCreativeMode) {
				player.inventory.setInventorySlotContents(player.inventory.currentItem, new ItemStack(Items.bucket));
			}
			return;
		}

		if (removeItem) {
			if (held_items.isStackable()) {
				held_items.stackSize--;
			} else {
				player.inventory.setInventorySlotContents(player.inventory.currentItem, null);
			}
		}
	}

	/*
	 * A potion with effects that's drunk rather than thrown
	 */
	private static boolean isDrinkablePotion(ItemStack stack) {
		if (!(stack.getItem() instanceof ItemPotion) || ItemPotion.isSplash(stack.getItemDamage())) {
			return false;
		}
		List effects = ((ItemPotion) stack.getItem()).getEffects(stack);
		return effects != null && !effects.isEmpty();
	}
}
