package com.harmony.harmonymod.client;

import com.harmony.harmonymod.HarmonyProps;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import java.util.Random;

/*
 * Shows a burst of mood particles (see HarmonyProps.moodParticle) when a kept animal's mood changes,
 * so a calm pen stays quiet. Wild animals show nothing. Feeding also shows the mood, from the server.
 */
@SideOnly(Side.CLIENT)
public class MoodIndicator {
	private final Random rand = new Random();

	public static void register() {
		MinecraftForge.EVENT_BUS.register(new MoodIndicator());
	}

	@SubscribeEvent
	public void onLivingUpdate(LivingUpdateEvent event) {
		EntityLivingBase animal = event.entityLiving;
		clearLoveMode(animal);
		if (!animal.worldObj.isRemote) {
			return;
		}
		HarmonyProps hp = HarmonyProps.get(animal);
		if (hp == null || !hp.moodChanged) {
			return;
		}
		hp.moodChanged = false;
		if (!isKept(animal, hp)) {
			return;
		}

		String particle = HarmonyProps.moodParticle(hp.mood);
		int count = HarmonyProps.moodParticleCount(hp.mood);
		for (int i = 0; i < count; i++) {
			animal.worldObj.spawnParticle(particle,
					animal.posX + (rand.nextDouble() - 0.5D) * animal.width * 1.5D,
					animal.posY + 0.5D + rand.nextDouble() * animal.height,
					animal.posZ + (rand.nextDouble() - 0.5D) * animal.width * 1.5D,
					0.0D, 0.0D, 0.0D);
		}
	}

	/*
	 * Feeding puts the client's copy of an animal in vanilla love mode too, which draws hearts for 30
	 * seconds. Harmony handles breeding on the server, so the client clears it: feeding just shows
	 * vanilla's one burst of hearts, and the Breed trick sends more while it's really breeding.
	 */
	private static void clearLoveMode(EntityLivingBase animal) {
		if (animal.worldObj.isRemote && animal instanceof EntityAnimal && ((EntityAnimal) animal).isInLove()
				&& HarmonyProps.get(animal) != null) {
			((EntityAnimal) animal).resetInLove();
		}
	}

	private static boolean isKept(EntityLivingBase animal, HarmonyProps hp) {
		return hp.kept || (animal instanceof EntityTameable && ((EntityTameable) animal).isTamed())
				|| (animal instanceof EntityHorse && ((EntityHorse) animal).isTame());
	}
}
