package com.harmony.harmonymod.client;

import com.harmony.harmonymod.HarmonyProps;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import java.util.Random;

/*
 * Shows how kept animals feel: green sparkles when happy enough to breed, grey clouds when unhappy.
 * Wild animals show nothing, so the countryside doesn't sparkle.
 */
@SideOnly(Side.CLIENT)
public class MoodIndicator {
	// Ticks between mood particles for each animal
	private static final int INTERVAL = 100;

	private final Random rand = new Random();

	public static void register() {
		MinecraftForge.EVENT_BUS.register(new MoodIndicator());
	}

	@SubscribeEvent
	public void onLivingUpdate(LivingUpdateEvent event) {
		EntityLivingBase animal = event.entityLiving;
		// Staggered by entity id so a pen doesn't pulse all at once
		if (!animal.worldObj.isRemote || (animal.ticksExisted + animal.getEntityId()) % INTERVAL != 0) {
			return;
		}
		HarmonyProps hp = HarmonyProps.get(animal);
		if (hp == null || !hp.isInitialized() || hp.mood == 0 || !isKept(animal, hp)) {
			return;
		}

		String particle = hp.mood > 0 ? "happyVillager" : "angryVillager";
		int count = hp.mood > 0 ? 3 : 1;
		for (int i = 0; i < count; i++) {
			animal.worldObj.spawnParticle(particle,
					animal.posX + (rand.nextDouble() - 0.5D) * animal.width * 1.5D,
					animal.posY + 0.5D + rand.nextDouble() * animal.height,
					animal.posZ + (rand.nextDouble() - 0.5D) * animal.width * 1.5D,
					0.0D, 0.0D, 0.0D);
		}
	}

	private static boolean isKept(EntityLivingBase animal, HarmonyProps hp) {
		return hp.kept || (animal instanceof EntityTameable && ((EntityTameable) animal).isTamed())
				|| (animal instanceof EntityHorse && ((EntityHorse) animal).isTame());
	}
}
