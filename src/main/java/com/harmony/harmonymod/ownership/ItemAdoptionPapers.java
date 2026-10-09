package com.harmony.harmonymod.ownership;

import com.harmony.harmonymod.HarmonyProps;
import com.harmony.harmonymod.items.HarmonyItem;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/*
 * Hands a bonded animal to another player.
 *
 * The papers collect, in any order: the giver's signature, the receiver's signature and the animal.
 * Right clicking signs them, the first signature is the giver (the animal's owner) and the second
 * the receiver. Right clicking an animal also attaches it, so a breeder can sign and hand the papers
 * over, and the receiver picks which animal they want. Once everything is filled in the animal
 * becomes the receiver's and the papers are used up.
 */
public class ItemAdoptionPapers extends HarmonyItem {
	// When each player last used papers on an animal. The client follows a cancelled entity click
	// with a plain right click, which is ignored if it's on the same tick.
	private static final Map<UUID, Long> usedOnAnimalAt = new HashMap<UUID, Long>();

	public ItemAdoptionPapers(String name) {
		super(name);
		this.setMaxStackSize(1);
	}

	/*
	 * Right click in the air: sign
	 */
	@Override
	public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
		if (world.isRemote) {
			return stack;
		}
		Long usedAt = usedOnAnimalAt.remove(player.getUniqueID());
		if (usedAt != null && usedAt == world.getTotalWorldTime()) {
			return stack;
		}

		sign(stack, player);
		if (isComplete(stack)) {
			EntityLiving animal = findAnimal(world, getAnimalId(stack));
			if (animal == null) {
				Ownership.tell(player, "harmony.adoption.find_animal", getName(stack, "Animal"));
			} else {
				finish(stack, player, animal);
			}
		}
		return stack;
	}

	/*
	 * Right click an animal: attach it if the papers don't have one yet, sign, and finish if complete.
	 * Called from Ownership's interact handler, server only.
	 */
	public void useOnAnimal(ItemStack stack, EntityPlayer player, EntityLiving animal) {
		usedOnAnimalAt.put(player.getUniqueID(), player.worldObj.getTotalWorldTime());
		HarmonyProps hp = HarmonyProps.get(animal);
		if (!hp.isBonded()) {
			Ownership.tell(player, "harmony.adoption.not_bonded", animal.getCommandSenderName());
			return;
		}

		UUID attached = getAnimalId(stack);
		if (attached != null && !attached.equals(animal.getUniqueID())) {
			Ownership.tell(player, "harmony.adoption.other_animal", getName(stack, "Animal"));
			return;
		}

		if (attached == null) {
			UUID giver = getId(stack, "Giver");
			if (giver == null && !hp.isOwner(player)) {
				// Blank papers: whoever signs first is the giver, so it has to be the owner
				Ownership.tell(player, "harmony.adoption.not_owner", animal.getCommandSenderName(), hp.ownerName);
				return;
			}
			if (giver != null && !giver.equals(hp.ownerId)) {
				Ownership.tell(player, "harmony.adoption.wrong_owner", animal.getCommandSenderName(), getName(stack, "Giver"));
				return;
			}
			NBTTagCompound tag = getTag(stack);
			NBTTagCompound animalTag = new NBTTagCompound();
			animalTag.setLong("Most", animal.getUniqueID().getMostSignificantBits());
			animalTag.setLong("Least", animal.getUniqueID().getLeastSignificantBits());
			animalTag.setString("Name", animal.getCommandSenderName());
			tag.setTag("Animal", animalTag);
			Ownership.tell(player, "harmony.adoption.attached", animal.getCommandSenderName());
		}

		sign(stack, player);
		if (isComplete(stack)) {
			finish(stack, player, animal);
		}
	}

	/*
	 * Add the player's signature, as giver if nobody has signed yet, otherwise as receiver
	 */
	private void sign(ItemStack stack, EntityPlayer player) {
		UUID id = player.getUniqueID();
		UUID giver = getId(stack, "Giver");
		if (giver == null) {
			setSignature(stack, "Giver", player);
		} else if (!giver.equals(id) && getId(stack, "Receiver") == null) {
			setSignature(stack, "Receiver", player);
		} else {
			return;
		}
		Ownership.tell(player, "harmony.adoption.signed");
	}

	private void finish(ItemStack stack, EntityPlayer player, EntityLiving animal) {
		HarmonyProps hp = HarmonyProps.get(animal);
		UUID giver = getId(stack, "Giver");
		if (hp == null || !giver.equals(hp.ownerId)) {
			// Changed owner since the papers were started
			Ownership.tell(player, "harmony.adoption.wrong_owner", animal.getCommandSenderName(), getName(stack, "Giver"));
			return;
		}

		UUID receiverId = getId(stack, "Receiver");
		String receiverName = getName(stack, "Receiver");
		Ownership.setOwner(animal, hp, receiverId, receiverName);
		stack.stackSize = 0;
		player.inventory.setInventorySlotContents(player.inventory.currentItem, null);

		Ownership.tell(player, "harmony.adoption.done", animal.getCommandSenderName(), receiverName);
		EntityPlayer other = player.worldObj.func_152378_a(player.getUniqueID().equals(giver) ? receiverId : giver);
		if (other != null) {
			Ownership.tell(other, "harmony.adoption.done", animal.getCommandSenderName(), receiverName);
		}
	}

	private static boolean isComplete(ItemStack stack) {
		return getId(stack, "Giver") != null && getId(stack, "Receiver") != null && getAnimalId(stack) != null;
	}

	private static EntityLiving findAnimal(World world, UUID id) {
		for (Object o : world.loadedEntityList) {
			if (o instanceof EntityLiving && id.equals(((Entity) o).getUniqueID())) {
				return (EntityLiving) o;
			}
		}
		return null;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack stack, EntityPlayer player, List lines, boolean advanced) {
		lines.add(line("harmony.adoption.tooltip.animal", getName(stack, "Animal")));
		lines.add(line("harmony.adoption.tooltip.giver", getName(stack, "Giver")));
		lines.add(line("harmony.adoption.tooltip.receiver", getName(stack, "Receiver")));
	}

	@SideOnly(Side.CLIENT)
	private static String line(String key, String value) {
		return StatCollector.translateToLocalFormatted(key,
				value != null ? value : StatCollector.translateToLocal("harmony.adoption.tooltip.blank"));
	}

	/*
	 * Papers' data: Giver / Receiver {Most, Least, Name} and Animal {Most, Least, Name}
	 */
	private static NBTTagCompound getTag(ItemStack stack) {
		if (!stack.hasTagCompound()) {
			stack.setTagCompound(new NBTTagCompound());
		}
		return stack.getTagCompound();
	}

	private static void setSignature(ItemStack stack, String key, EntityPlayer player) {
		NBTTagCompound signature = new NBTTagCompound();
		signature.setLong("Most", player.getUniqueID().getMostSignificantBits());
		signature.setLong("Least", player.getUniqueID().getLeastSignificantBits());
		signature.setString("Name", player.getCommandSenderName());
		getTag(stack).setTag(key, signature);
	}

	private static UUID getId(ItemStack stack, String key) {
		if (!stack.hasTagCompound() || !stack.getTagCompound().hasKey(key)) {
			return null;
		}
		NBTTagCompound tag = stack.getTagCompound().getCompoundTag(key);
		return new UUID(tag.getLong("Most"), tag.getLong("Least"));
	}

	private static UUID getAnimalId(ItemStack stack) {
		return getId(stack, "Animal");
	}

	private static String getName(ItemStack stack, String key) {
		if (!stack.hasTagCompound() || !stack.getTagCompound().hasKey(key)) {
			return null;
		}
		return stack.getTagCompound().getCompoundTag(key).getString("Name");
	}
}
