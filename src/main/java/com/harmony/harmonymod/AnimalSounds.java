package com.harmony.harmonymod;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.PlaySoundAtEntityEvent;
import java.util.Random;

/*
 * Quiets big farms: only HarmonyMod.animalSounds of Harmony animals' sounds play (this Forge's sound
 * event can't change volume, only cancel). Hurt and death sounds always play.
 */
public class AnimalSounds {
	private final Random random = new Random();

	public static void register() {
		MinecraftForge.EVENT_BUS.register(new AnimalSounds());
	}

	@SubscribeEvent
	public void onSound(PlaySoundAtEntityEvent event) {
		if (HarmonyMod.animalSounds >= 1.0F || event.name == null || HarmonyProps.get(event.entity) == null
				|| event.name.contains("hurt") || event.name.contains("death")) {
			return;
		}
		if (random.nextFloat() >= HarmonyMod.animalSounds) {
			event.setCanceled(true);
		}
	}
}
