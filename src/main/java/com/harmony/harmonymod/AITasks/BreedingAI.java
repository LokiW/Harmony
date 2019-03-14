package com.harmony.harmonymod.aitasks;

import com.harmony.harmonymod.tricks.Breed;
import com.harmony.harmonymod.HarmonyMod;
import com.harmony.harmonymod.HarmonyProps;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAIMate;
import net.minecraft.entity.ai.EntityAITasks.EntityAITaskEntry;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.EntityLiving;
import net.minecraft.world.World;

public class BreedingAI extends EntityAIBase
{
	private EntityAnimal pet;
	private Breed breedTrick;
	private int delayCounter;

	private static final String __OBFID = "CL_00001578";

	public BreedingAI(EntityAnimal pet) {
		this.pet = pet;
		this.delayCounter = 0;
		this.setMutexBits(3);
	}

	/**
	 * Returns whether the EntityAIBase should begin execution.
	 */
	public boolean shouldExecute() {
		HarmonyProps hp;
		if(pet.isInLove()) {
			pet.resetInLove();
			pet.worldObj.setEntityState(pet, (byte)0);
			hp = HarmonyProps.get(pet);
			hp.happiness += 5; //TODO this shouldn't be hardcoded
		}

		delayCounter++;
		if(!(delayCounter % 256 == 0))
			return false;

		hp = HarmonyProps.get(pet);
		if(hp != null && hp.happiness >= HarmonyMod.breedingHappiness) {
			breedTrick = new Breed(HarmonyMod.breedingHappiness);
			breedTrick.setupTrick(pet, null);
			return true;
		}
		return false;
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
