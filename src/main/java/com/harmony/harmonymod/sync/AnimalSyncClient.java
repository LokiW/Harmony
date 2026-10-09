package com.harmony.harmonymod.sync;

import com.harmony.harmonymod.HarmonyProps;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;

/*
 * Client half of AnimalSync: stores the data the server sent on the client's copy of the animal
 */
@SideOnly(Side.CLIENT)
public class AnimalSyncClient {

	public static void apply(AnimalSyncMessage message) {
		World world = Minecraft.getMinecraft().theWorld;
		if (world == null) {
			return;
		}

		Entity animal = world.getEntityByID(message.entityId);
		HarmonyProps hp = animal != null ? HarmonyProps.get(animal) : null;
		if (hp != null) {
			hp.readSyncedData(message.data);
		}
	}
}
