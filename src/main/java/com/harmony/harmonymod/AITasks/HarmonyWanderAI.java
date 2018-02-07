package com.harmony.harmonymod.aitasks;

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

public class HarmonyWanderAI extends EntityAIBase
{
	private EntityAnimal pet;
    private double xPosition;
    private double yPosition;
    private double zPosition;
	private int delayCounter;

	private static final String __OBFID = "CL_00001578";

	public HarmonyWanderAI(EntityAnimal pet) {
		this.pet = pet;
		this.delayCounter = 0;
		this.setMutexBits(1);
	}

	/**
	 * Returns whether the EntityAIBase should begin execution.
	 */
	public boolean shouldExecute() {
		HarmonyProps hp;
        if (this.entity.getAge() >= 100)
        {
            return false;
        }
        else if (this.entity.getRNG().nextInt(120) != 0)
        {
            return false;
        }
        else
        {
            Vec3 var1 = RandomPositionGenerator.findRandomTarget(this.entity, 10, 7);

            if (var1 == null)
            {
				// Couldn't move to location, reduce happiness for being confined
				hp = HarmonyProps.get(pet);
				hp.happiness--;
                return false;
            }
            else
            {
                this.xPosition = var1.xCoord;
                this.yPosition = var1.yCoord;
                this.zPosition = var1.zCoord;
                return true;
            }
        }
	}

	/**
	 * Returns whether an in-progress EntityAIBase should continue executing
	 */
	public boolean continueExecuting() {
        return !this.entity.getNavigator().noPath();
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
			pet.tasks.addTask(1,new HarmonyWanderAI((EntityAnimal)pet));
		}
	}
}
