package com.harmony.harmonymod.aitasks;

import com.harmony.harmonymod.tricks.Breed;
import com.harmony.harmonymod.HarmonyMod;
import com.harmony.harmonymod.HarmonyProps;
import java.util.List;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAIMate;
import net.minecraft.entity.ai.EntityAITasks.EntityAITaskEntry;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.EntityLiving;
import net.minecraft.world.WorldServer;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.passive.EntityTameable;

/*
 * Replaces vanilla breeding. Happy animals breed: kept animals (see HarmonyProps.kept) on their own,
 * any animal right away when a player feeds it its breeding food. Unhappy animals fed their food get
 * happier instead.
 */
public class BreedingAI extends EntityAIBase
{
	// How often kept animals check whether they can breed on their own
	private static final int NATURAL_INTERVAL = 200;
	// Happiness from being fed while too unhappy to breed
	private static final int FED_HAPPINESS = 3;

	private EntityAnimal pet;
	private Breed breedTrick;
	private long nextNaturalCheck;


	public BreedingAI(EntityAnimal pet) {
		this.pet = pet;
		this.setMutexBits(3);
	}

	/**
	 * Returns whether the EntityAIBase should begin execution.
	 */
	public boolean shouldExecute() {
		HarmonyProps hp = HarmonyProps.get(pet);
		if (hp == null || !hp.isInitialized()) {
			return false;
		}

		if(pet.isInLove()) {
			// Fed its breeding food. Vanilla put it in love mode, Harmony decides what happens instead.
			pet.resetInLove();
			hp.kept = true;
			if (hp.canBreed()) {
				return startBreeding();
			}
			hp.changeHappiness(FED_HAPPINESS);
			pet.worldObj.setEntityState(pet, (byte)0);
			((WorldServer) pet.worldObj).func_147487_a("angryVillager", pet.posX, pet.posY + pet.height + 0.3D,
					pet.posZ, 2, 0.3D, 0.2D, 0.3D, 0.0D);
			return false;
		}

		long now = pet.worldObj.getTotalWorldTime();
		if (now < nextNaturalCheck) {
			return false;
		}
		nextNaturalCheck = now + NATURAL_INTERVAL;

		if (isKept(hp) && hp.canBreed()) {
			return startBreeding();
		}
		return false;
	}

	/*
	 * Kept by a player, or tamed the vanilla way
	 */
	private boolean isKept(HarmonyProps hp) {
		return hp.kept || (pet instanceof EntityTameable && ((EntityTameable) pet).isTamed())
				|| (pet instanceof EntityHorse && ((EntityHorse) pet).isTame());
	}

	/*
	 * Look for a mate, Breed ends straight away if there's none or there are too many animals around
	 */
	private boolean startBreeding() {
		breedTrick = new Breed(HarmonyMod.breedingHappiness);
		breedTrick.setupTrick(pet, null);
		return true;
	}

	/**
	 * Returns whether an in-progress EntityAIBase should continue executing
	 */
	public boolean continueExecuting() {
		return breedTrick != null;
	}

	/**
	 * Resets the task
	 */
	public void resetTask() {
		breedTrick = null;
	}

	/**
	 * Updates the task
	 */
	public void updateTask() {
		if(!breedTrick.act()) {
			breedTrick = null;
		}
	}

	public static void registerTask(EntityLiving pet) {
		//replace breeding task
		if (!(pet instanceof EntityAnimal))
			return;

		boolean canBreed = false;
		List<EntityAITaskEntry> t = pet.tasks.taskEntries;
		for(int i = 0; i < t.size(); i++) {
			if(t.get(i).action instanceof EntityAIMate) {
				t.remove(i);
				canBreed = true;
				i--;
			}
		}
		if(canBreed) {
			pet.tasks.addTask(1,new BreedingAI((EntityAnimal)pet));
		}
	}
}
