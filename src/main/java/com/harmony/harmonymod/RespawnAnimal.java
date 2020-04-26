package com.harmony.harmonymod;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.common.MinecraftForge;

/*
 * Event handler for a player feeding an animal
 */
public class RespawnAnimal {

	public static void register() {
		MinecraftForge.EVENT_BUS.register(new RespawnAnimal());
	}

	@SubscribeEvent
	public void onLivingDeathEvent(LivingDeathEvent e) {
        /*
         * If entity has harmony props, check if it has a respawn point.
         */
		EntityLivingBase dying = e.entityLiving;
		HarmonyProps hp = HarmonyProps.get(dying);

		if (hp == null || hp.tricks == null) {
			return;
		}

		if (hp.tricks.yRespawn >= 0.0) {
			// Respawn location has been set. So respawn animal.
			e.setCanceled(true);

			dying.setLocationAndAngles(
						hp.tricks.xRespawn,
						hp.tricks.yRespawn,
						hp.tricks.zRespawn,
						dying.rotationYaw,
						dying.rotationPitch);

			dying.setHealth(dying.getMaxHealth() / 2.0);
		}
	}
}
