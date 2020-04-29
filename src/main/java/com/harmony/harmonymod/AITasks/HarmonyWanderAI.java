package com.harmony.harmonymod.aitasks;

import com.harmony.harmonymod.HarmonyMod;
import com.harmony.harmonymod.HarmonyProps;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import net.minecraft.util.Vec3;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAIWander;
import net.minecraft.entity.ai.EntityAIControlledByPlayer;
import net.minecraft.entity.ai.EntityAITasks.EntityAITaskEntry;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.block.Block;
import net.minecraft.block.IGrowable;
import net.minecraft.world.World;

public class HarmonyWanderAI extends EntityAIBase
{
	private EntityAnimal entity;
	private int delayCounter;

	private static final String __OBFID = "CL_00001578";
	private static final int maxDelay = 16;

	public HarmonyWanderAI(EntityAnimal entity) {
		this.entity = entity;
		this.delayCounter = maxDelay;
		this.setMutexBits(1);
		this.delayCounter = this.entity.getRNG().nextInt(maxDelay);
	}

	/**
	 * Returns whether the EntityAIBase should begin execution.
	 */
	public boolean shouldExecute() {
		HarmonyProps hp;
		if (this.entity.getAge() >= 100) {
			return false;
		} else if (this.entity.getRNG().nextInt(120) != 0) {
			return false;
		} else {
			boolean canHappinessIncrease = true;
			Vec3 var1 = RandomPositionGenerator.findRandomTarget(this.entity, 10, 7);
			hp = HarmonyProps.get(this.entity);

			// If animal is missing health, loose happiness
			if (this.entity.getHealth() < this.entity.getMaxHealth()) {
				hp.happiness--;
				canHappinessIncrease = false;
			}

			if (var1 == null) {
				// Couldn't move to location, reduce happiness for being confined
				hp.happiness--;
				return false;
			} else if (!this.entity.getNavigator().tryMoveToXYZ(var1.xCoord, var1.yCoord, var1.zCoord, 1.0)) {
				hp.happiness--;
				return false;
			}
			World world = this.entity.worldObj;
			Block target = world.getBlock((int)var1.xCoord, (int)var1.yCoord - 1, (int)var1.zCoord);

			if (target != null && target instanceof IGrowable && canHappinessIncrease)
				hp.happiness++;

			if (this.delayCounter > 0) {
				this.delayCounter--;
				return true;
			}
	
			AxisAlignedBB boxToCheck = AxisAlignedBB.getBoundingBox(var1.xCoord-2, var1.xCoord-2,
				 													var1.zCoord-2,
																	var1.xCoord+2, var1.yCoord+2,
																	var1.zCoord+2);

			List<Object> entities = world.getEntitiesWithinAABB(EntityAnimal.class, boxToCheck);
			if (entities.size() > 10) {
				hp.happiness = 0;
				canHappinessIncrease = false;
			} else if (entities.size() < 3 || entities.size() > 5) {
				hp.happiness--;
			}

			entities = world.getEntitiesWithinAABB(EntityMob.class, boxToCheck);
			if (entities.size() > 0)
				hp.happiness--;

			this.delayCounter = maxDelay;
			return true;
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

	}

	public static void registerTask(EntityLiving pet) {
		//replace breeding task
		if (!(pet instanceof EntityAnimal))
			return;

		List<EntityAITaskEntry> t = pet.tasks.taskEntries;
		for(int i = 0; i < t.size(); i++) {
			if(t.get(i).action instanceof EntityAIWander) {
				t.remove(i);
				i--;
			}
		}
		pet.tasks.addTask(1,new HarmonyWanderAI((EntityAnimal)pet));
	}
}
