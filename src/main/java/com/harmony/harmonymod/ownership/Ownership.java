package com.harmony.harmonymod.ownership;

import com.harmony.harmonymod.HarmonyProps;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.EntityInteractEvent;
import java.util.UUID;

/*
 * Bonding: an optional owner for an animal.
 *
 * Naming an animal with a name tag bonds it to you. Wolves, ocelots and horses can only be bonded by
 * their tamer. Only the owner can leash, rename, train or set the respawn point of a bonded animal.
 * Bonded animals change owner with adoption papers (ItemAdoptionPapers).
 *
 * Interactions are checked on both sides so the client cancels the same ones the server does,
 * but only the server changes anything or sends messages.
 */
public class Ownership {
	public static ItemAdoptionPapers adoptionPapers;

	/*
	 * Items, during preInit
	 */
	public static void registerItems() {
		adoptionPapers = new ItemAdoptionPapers("adoption_papers");
	}

	/*
	 * Recipes and event handlers, during init
	 */
	public static void register() {
		ItemStack blackDye = new ItemStack(Items.dye, 1, 0);

		// Shaped like a name tag: the paper and ink are the tag, the lead runs up to the string loop
		GameRegistry.addShapedRecipe(new ItemStack(Items.name_tag),
				"  S",
				" L ",
				"PD ",
				'S', Items.string, 'L', Items.lead, 'P', Items.paper, 'D', blackDye);
		GameRegistry.addShapelessRecipe(new ItemStack(adoptionPapers), Items.paper, blackDye, Items.name_tag);

		MinecraftForge.EVENT_BUS.register(new Ownership());
	}

	@SubscribeEvent
	public void onInteract(EntityInteractEvent event) {
		EntityPlayer player = event.entityPlayer;
		ItemStack held = player.getCurrentEquippedItem();
		HarmonyProps hp = HarmonyProps.get(event.target);
		if (held == null || hp == null || !(event.target instanceof EntityLiving)) {
			return;
		}
		EntityLiving animal = (EntityLiving) event.target;
		boolean server = !player.worldObj.isRemote;

		if (held.getItem() == Items.name_tag && held.hasDisplayName()) {
			if (hp.isBonded() && !hp.isOwner(player)) {
				event.setCanceled(true);
				if (server) {
					tell(player, "harmony.bond.other", animal.getCommandSenderName(), hp.ownerName);
				}
			} else if (canBond(player, animal)) {
				// Named here rather than by vanilla, which makes a tamed wolf sit instead of taking the name
				event.setCanceled(true);
				if (server) {
					boolean newBond = !hp.isBonded();
					animal.setCustomNameTag(held.getDisplayName());
					animal.func_110163_bv();
					useUp(player, held);
					if (newBond) {
						setOwner(animal, hp, player.getUniqueID(), player.getCommandSenderName());
						tell(player, "harmony.bond.bonded", animal.getCommandSenderName());
					}
				}
			} else if (server) {
				// Vanilla still names it, there's just no bond
				tell(player, isTamed(animal) ? "harmony.bond.not_tamer" : "harmony.bond.tame_first",
						animal.getCommandSenderName());
			}
		} else if (held.getItem() == Items.lead) {
			if (hp.isBonded() && !hp.isOwner(player)) {
				event.setCanceled(true);
				if (server) {
					tellOwnerOnly(player, animal, hp);
				}
			}
		} else if (held.getItem() instanceof ItemAdoptionPapers) {
			event.setCanceled(true);
			if (server) {
				((ItemAdoptionPapers) held.getItem()).useOnAnimal(held, player, animal);
			}
		}
	}

	/*
	 * Whether player may bond with this unbonded animal: anyone, except that tameable animals
	 * can only be bonded by their tamer
	 */
	public static boolean canBond(EntityPlayer player, EntityLiving animal) {
		if (animal instanceof EntityTameable) {
			EntityTameable tameable = (EntityTameable) animal;
			return tameable.isTamed() && tameable.func_152114_e(player);
		}
		if (animal instanceof EntityHorse) {
			EntityHorse horse = (EntityHorse) animal;
			return horse.isTame() && player.getUniqueID().toString().equals(horse.func_152119_ch());
		}
		return true;
	}

	private static boolean isTamed(EntityLiving animal) {
		return (animal instanceof EntityTameable && ((EntityTameable) animal).isTamed())
				|| (animal instanceof EntityHorse && ((EntityHorse) animal).isTame());
	}

	/*
	 * Make a player the animal's owner. For tamed animals they become the vanilla owner too,
	 * so the two never disagree.
	 */
	public static void setOwner(EntityLiving animal, HarmonyProps hp, UUID id, String name) {
		hp.ownerId = id;
		hp.ownerName = name;

		if (animal instanceof EntityTameable && ((EntityTameable) animal).isTamed()) {
			((EntityTameable) animal).func_152115_b(id.toString());
		} else if (animal instanceof EntityHorse && ((EntityHorse) animal).isTame()) {
			((EntityHorse) animal).func_152120_b(id.toString());
		}
	}

	/*
	 * Checks an owner only action, telling the player if they aren't allowed. Server only.
	 */
	public static boolean mayUse(EntityPlayer player, Entity animal, HarmonyProps hp) {
		if (hp.isBonded() && !hp.isOwner(player)) {
			tellOwnerOnly(player, animal, hp);
			return false;
		}
		return true;
	}

	private static void tellOwnerOnly(EntityPlayer player, Entity animal, HarmonyProps hp) {
		tell(player, "harmony.owner_only", animal.getCommandSenderName(), hp.ownerName);
	}

	static void tell(EntityPlayer player, String key, Object... args) {
		player.addChatComponentMessage(new ChatComponentTranslation(key, args));
	}

	/*
	 * Use up one of the held item, unless in creative
	 */
	static void useUp(EntityPlayer player, ItemStack held) {
		if (player.capabilities.isCreativeMode) {
			return;
		}
		held.stackSize--;
		if (held.stackSize <= 0) {
			player.inventory.setInventorySlotContents(player.inventory.currentItem, null);
		}
	}
}
