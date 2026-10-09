package com.harmony.harmonymod.sync;

import com.harmony.harmonymod.HarmonyNetwork;
import com.harmony.harmonymod.HarmonyProps;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;

/*
 * Sends animals' Harmony data to the players who can see them, so clients can show it.
 * Only the server's copy is real: clients don't have an animal's data until it arrives.
 */
public class AnimalSync {
	// How often (in ticks) each animal checks whether its data changed
	private static final int CHECK_INTERVAL = 20;

	public static void register() {
		MinecraftForge.EVENT_BUS.register(new AnimalSync());
	}

	/*
	 * A player can now see the animal, send them everything
	 */
	@SubscribeEvent
	public void onStartTracking(PlayerEvent.StartTracking event) {
		HarmonyProps hp = HarmonyProps.get(event.target);
		if (hp == null || !hp.isInitialized() || !(event.entityPlayer instanceof EntityPlayerMP)) {
			return;
		}
		HarmonyNetwork.channel.sendTo(new AnimalSyncMessage(event.target, hp.writeData()), (EntityPlayerMP) event.entityPlayer);
	}

	/*
	 * Send changes to everyone who can see the animal
	 */
	@SubscribeEvent
	public void onLivingUpdate(LivingUpdateEvent event) {
		EntityLivingBase animal = event.entityLiving;
		if (animal.worldObj.isRemote || animal.ticksExisted % CHECK_INTERVAL != 0) {
			return;
		}

		HarmonyProps hp = HarmonyProps.get(animal);
		if (hp == null || !hp.isInitialized()) {
			return;
		}

		NBTTagCompound data = hp.writeData();
		if (data.equals(hp.lastSynced)) {
			return;
		}
		hp.lastSynced = data;

		((WorldServer) animal.worldObj).getEntityTracker().func_151247_a(animal,
				HarmonyNetwork.channel.getPacketFrom(new AnimalSyncMessage(animal, data)));
	}
}
