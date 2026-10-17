package com.harmony.harmonymod.tricks;

import com.harmony.harmonymod.HarmonyProps;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.ai.EntityAITargetNonTamed;
import net.minecraft.entity.ai.EntityAITasks.EntityAITaskEntry;
import net.minecraft.entity.passive.EntityWolf;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import java.util.Iterator;

/*
 * Shows when an animal is fighting: wolves get angry (tail up and growling, untamed ones red eyed too)
 * while they have something to attack, other animals give off sparks when fighting on command. Also
 * stops Harmony animals hunting on their own (wolves chasing sheep, ocelots chickens): they attack
 * when told to.
 */
public class TrickIndicators {
	private static final int CHECK_TICKS = 10;
	private static final int SPARK_TICKS = 20;

	public static void register() {
		MinecraftForge.EVENT_BUS.register(new TrickIndicators());
	}

	/*
	 * Remove vanilla's "hunt untamed prey" targeting, see HarmonyProps.registerAI
	 */
	public static void stopHunting(EntityLiving animal) {
		Iterator it = animal.targetTasks.taskEntries.iterator();
		while (it.hasNext()) {
			if (((EntityAITaskEntry) it.next()).action instanceof EntityAITargetNonTamed) {
				it.remove();
			}
		}
	}

	@SubscribeEvent
	public void onLivingUpdate(LivingUpdateEvent event) {
		if (!(event.entity instanceof EntityLiving) || event.entity.worldObj.isRemote
				|| event.entity.ticksExisted % CHECK_TICKS != 0) {
			return;
		}
		EntityLiving animal = (EntityLiving) event.entity;
		HarmonyProps hp = HarmonyProps.get(animal);
		if (hp == null || hp.tricks == null) {
			return;
		}
		boolean fighting = animal.getAttackTarget() != null && !animal.getAttackTarget().isDead;
		if (animal instanceof EntityWolf) {
			EntityWolf wolf = (EntityWolf) animal;
			if (wolf.isAngry() != fighting) {
				wolf.setAngry(fighting);
			}
		} else if (fighting && animal.ticksExisted % SPARK_TICKS == 0
				&& (hp.tricks.currentTrick instanceof Attack || hp.tricks.currentTrick instanceof Guard)) {
			((WorldServer) animal.worldObj).func_147487_a("crit", animal.posX, animal.posY + animal.height + 0.2D,
					animal.posZ, 4, 0.3D, 0.1D, 0.3D, 0.1D);
		}
	}
}
