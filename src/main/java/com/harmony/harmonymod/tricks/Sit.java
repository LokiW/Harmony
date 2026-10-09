package com.harmony.harmonymod.tricks;

import com.harmony.harmonymod.HarmonyProps;
import com.harmony.harmonymod.ownership.ItemAdoptionPapers;
import com.harmony.harmonymod.sync.AnimalSync;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.passive.EntityWolf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.EntityInteractEvent;

/*
 * Stay put until given another trick or Stop. Wolves and ocelots use their vanilla sitting pose,
 * other animals have no sitting animation and just stand still.
 * A wolf's owner can also stand it up by right clicking it, like vanilla's own sit (see StandUpHandler).
 */
public class Sit extends Trick {

	public void setupTrick(EntityLiving pet, Trick currentTrick) {
		this.pet = pet;
	}

	public boolean act() {
		this.pet.getNavigator().clearPathEntity();
		setSitting(true);
		return true;
	}

	public boolean isInstant() {
		return false;
	}

	public boolean consume(Trick newTrick) {
		return newTrick instanceof Sit;
	}

	@Override
	public void stopTrick() {
		setSitting(false);
	}

	private void setSitting(boolean sitting) {
		if (this.pet instanceof EntityTameable) {
			EntityTameable tameable = (EntityTameable) this.pet;
			// The AI task keeps a tamed animal sat, the flag is what shows the pose
			tameable.func_70907_r().setSitting(sitting);
			tameable.setSitting(sitting);
		}
	}

	@Override
	protected String getSaveType() {
		return "sit";
	}

	public static void register() {
		MinecraftForge.EVENT_BUS.register(new StandUpHandler());
	}

	/*
	 * Vanilla stands a sitting wolf up when its owner right clicks it, but its sit AI can't run while
	 * a Harmony trick is running, so a wolf sat by the Sit trick is stood up here instead.
	 * Cancelled on both sides (clients know the current trick from sync) so vanilla doesn't toggle too.
	 */
	public static class StandUpHandler {
		@SubscribeEvent
		public void onInteract(EntityInteractEvent event) {
			if (!(event.target instanceof EntityWolf)) {
				return;
			}
			EntityWolf wolf = (EntityWolf) event.target;
			EntityPlayer player = event.entityPlayer;
			HarmonyProps hp = HarmonyProps.get(wolf);
			if (hp == null || !hp.isInitialized() || !(hp.tricks.currentTrick instanceof Sit)
					|| !wolf.isTamed() || !wolf.func_152114_e(player) || !togglesSit(wolf, player.getCurrentEquippedItem())) {
				return;
			}

			event.setCanceled(true);
			if (!wolf.worldObj.isRemote) {
				hp.tricks.stopCurrentTrick();
				AnimalSync.syncNow(wolf);
			}
		}

		/*
		 * Whether vanilla would toggle sitting for this held item, and Harmony doesn't use it on animals
		 */
		private static boolean togglesSit(EntityWolf wolf, ItemStack held) {
			if (held == null) {
				return true;
			}
			Item item = held.getItem();
			return !wolf.isBreedingItem(held) && item != Items.dye && item != Items.lead && item != Items.name_tag
					&& item != Items.speckled_melon && item != Items.golden_apple && !(item instanceof ItemAdoptionPapers);
		}
	}
}
