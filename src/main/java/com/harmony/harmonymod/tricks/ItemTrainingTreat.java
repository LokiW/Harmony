package com.harmony.harmonymod.tricks;

import com.harmony.harmonymod.HarmonyProps;
import com.harmony.harmonymod.items.HarmonyItem;
import com.harmony.harmonymod.ownership.Ownership;
import com.harmony.harmonymod.sounds.Phrases;
import com.harmony.harmonymod.sync.AnimalSync;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.StatCollector;
import net.minecraft.util.WeightedRandomChestContent;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.ChestGenHooks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.EntityInteractEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;

/*
 * Training treats instantly teach an animal one trick for the phrase written on the treat.
 * Found in dungeon, mineshaft, temple, stronghold and village chests with a random phrase.
 */
public class ItemTrainingTreat extends HarmonyItem {
	// Tricks treats can teach: the ones that work without a learned location
	public static final String[] TRICKS = {"STOP", "GO", "ATTACK", "GUARD", "JUMP", "SIT", "FOLLOW", "COME_BACK"};
	// Chest loot weight (a dungeon saddle is 10)
	private static final int LOOT_WEIGHT = 5;
	private static final String[] LOOT_CHESTS = {
		ChestGenHooks.DUNGEON_CHEST, ChestGenHooks.MINESHAFT_CORRIDOR,
		ChestGenHooks.PYRAMID_DESERT_CHEST, ChestGenHooks.PYRAMID_JUNGLE_CHEST,
		ChestGenHooks.STRONGHOLD_CORRIDOR, ChestGenHooks.STRONGHOLD_LIBRARY, ChestGenHooks.VILLAGE_BLACKSMITH,
	};

	public ItemTrainingTreat(String name) {
		super(name);
		this.setMaxStackSize(16);
	}

	/*
	 * Loot and the feeding handler, during init
	 */
	public void register() {
		for (String chest : LOOT_CHESTS) {
			ChestGenHooks.addItem(chest, new TreatLoot(this));
		}
		MinecraftForge.EVENT_BUS.register(this);
	}

	public ItemStack makeTreat(String trick, String phrase) {
		ItemStack stack = new ItemStack(this);
		NBTTagCompound tag = new NBTTagCompound();
		tag.setString("Trick", trick);
		tag.setString("Phrase", phrase);
		stack.setTagCompound(tag);
		return stack;
	}

	/*
	 * A random harp phrase of 1 to 3 notes
	 */
	public static String randomPhrase(Random random) {
		List<SortedSet<Integer>> steps = new ArrayList<SortedSet<Integer>>();
		int length = 1 + random.nextInt(3);
		for (int i = 0; i < length; i++) {
			SortedSet<Integer> step = new TreeSet<Integer>();
			step.add(random.nextInt(25));
			steps.add(step);
		}
		return Phrases.key(steps);
	}

	private static String getTrick(ItemStack stack) {
		return stack.hasTagCompound() ? stack.getTagCompound().getString("Trick") : "";
	}

	private static String getPhrase(ItemStack stack) {
		return stack.hasTagCompound() ? stack.getTagCompound().getString("Phrase") : "";
	}

	public static String trickDisplayName(String trick) {
		return StatCollector.translateToLocal("harmony.trick." + trick);
	}

	@SubscribeEvent
	public void onInteract(EntityInteractEvent event) {
		EntityPlayer player = event.entityPlayer;
		ItemStack held = player.getCurrentEquippedItem();
		HarmonyProps hp = HarmonyProps.get(event.target);
		if (held == null || held.getItem() != this || hp == null || !(event.target instanceof EntityLiving)) {
			return;
		}
		// Handled here, so vanilla doesn't also act on the click (mounting a horse, sitting a wolf).
		// Cancelled on both sides.
		event.setCanceled(true);
		if (player.worldObj.isRemote || !hp.isInitialized() || !Ownership.mayUse(player, event.target, hp)) {
			return;
		}

		String trick = getTrick(held);
		String phrase = getPhrase(held);
		long action = TrickEnum.fromName(trick);
		if (action == 0 || phrase.isEmpty()) {
			return;
		}
		EntityLiving animal = (EntityLiving) event.target;
		hp.tricks.tricks.put(phrase, new ActionSet(action));
		hp.kept = true;
		AnimalSync.syncNow(animal);
		((WorldServer) animal.worldObj).func_147487_a("heart", animal.posX, animal.posY + animal.height + 0.3D,
				animal.posZ, 7, 0.4D, 0.3D, 0.4D, 0.0D);
		player.addChatComponentMessage(new ChatComponentTranslation("harmony.treat.learned",
				animal.getCommandSenderName(), trickDisplayName(trick), phrase));
		if (!player.capabilities.isCreativeMode) {
			held.stackSize--;
			if (held.stackSize <= 0) {
				player.inventory.setInventorySlotContents(player.inventory.currentItem, null);
			}
		}
	}

	@Override
	public String getItemStackDisplayName(ItemStack stack) {
		String trick = getTrick(stack);
		if (trick.isEmpty()) {
			return super.getItemStackDisplayName(stack);
		}
		return StatCollector.translateToLocalFormatted("item.training_treat.named", trickDisplayName(trick));
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack stack, EntityPlayer player, List lines, boolean advanced) {
		String phrase = getPhrase(stack);
		if (!phrase.isEmpty()) {
			lines.add(StatCollector.translateToLocalFormatted("harmony.treat.tooltip", phrase));
		}
	}

	/*
	 * One treat per trick in the creative tab, each on its own single harp note
	 */
	@Override
	@SideOnly(Side.CLIENT)
	public void getSubItems(Item item, CreativeTabs tab, List items) {
		for (int i = 0; i < TRICKS.length; i++) {
			List<SortedSet<Integer>> steps = new ArrayList<SortedSet<Integer>>();
			SortedSet<Integer> step = new TreeSet<Integer>();
			step.add(i * 2);
			steps.add(step);
			items.add(makeTreat(TRICKS[i], Phrases.key(steps)));
		}
	}

	/*
	 * Chest loot that rolls a random trick and phrase for each treat
	 */
	private static class TreatLoot extends WeightedRandomChestContent {
		private final ItemTrainingTreat treat;

		TreatLoot(ItemTrainingTreat treat) {
			super(new ItemStack(treat), 1, 1, LOOT_WEIGHT);
			this.treat = treat;
		}

		@Override
		protected ItemStack[] generateChestContent(Random random, IInventory inventory) {
			String trick = TRICKS[random.nextInt(TRICKS.length)];
			return new ItemStack[] {treat.makeTreat(trick, randomPhrase(random))};
		}
	}
}
