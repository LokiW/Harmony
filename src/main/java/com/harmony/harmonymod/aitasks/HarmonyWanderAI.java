package com.harmony.harmonymod.aitasks;

import com.harmony.harmonymod.HarmonyAchievements;
import com.harmony.harmonymod.HarmonyProps;
import com.harmony.harmonymod.tricks.Hazards;
import java.util.List;
import net.minecraft.util.Vec3;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAIWander;
import net.minecraft.entity.ai.EntityAITasks.EntityAITaskEntry;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

/*
 * Wandering that also works out how happy an animal is. Each time it looks for somewhere to go, its
 * happiness moves a point towards a target: HarmonyProps.BASELINE (content), plus comforts players
 * built (see Comforts) and being fed recently, minus what bothers it. Crowding costs more the more
 * crowded it gets, so a pen's animals breed until it's as full as its comforts allow, then stop.
 */
public class HarmonyWanderAI extends EntityAIBase
{
	private EntityAnimal entity;

	// What bothers an animal, see HarmonyProps.bothers
	public static final int CROWDED = 1;
	public static final int CONFINED = 2;
	public static final int NO_GRASS = 4;
	public static final int MONSTERS = 8;
	public static final int RAIN = 16;
	public static final int HURT = 32;

	// Crowding counts animals within this many blocks. Up to COMFORTABLE (itself included) is fine,
	// each one past that costs one more than the last: -1, -3, -6, ...
	private static final double CROWD_RANGE = 6.0D;
	private static final int COMFORTABLE = 6;
	private static final int CONFINED_COST = 2;
	private static final int MONSTER_COST = 2;
	private static final int HURT_COST = 2;
	private static final int NO_GRASS_COST = 1;
	private static final int RAIN_COST = 1;
	// Priority for animals without a vanilla wander task to replace
	private static final int DEFAULT_PRIORITY = 5;

	public HarmonyWanderAI(EntityAnimal entity) {
		this.entity = entity;
		this.setMutexBits(1);
	}

	/**
	 * Returns whether the EntityAIBase should begin execution.
	 */
	public boolean shouldExecute() {
		if (this.entity.getAge() >= 100 || this.entity.getRNG().nextInt(120) != 0) {
			return false;
		}
		HarmonyProps hp = HarmonyProps.get(this.entity);
		World world = this.entity.worldObj;
		Vec3 var1 = RandomPositionGenerator.findRandomTarget(this.entity, 10, 7);
		int comforts = Comforts.get(this.entity);
		int bothers = 0;
		int target = HarmonyProps.BASELINE + Comforts.happiness(comforts);
		if (hp.recentlyFed()) {
			target += HarmonyProps.FED_HAPPINESS;
		}

		if (this.entity.getHealth() < this.entity.getMaxHealth()) {
			bothers |= HURT;
			target -= HURT_COST;
		}
		// Left out in the rain with nowhere to shelter
		if (world.isRaining() && (comforts & Comforts.SHELTER) == 0 && world.canLightningStrikeAt(
				MathHelper.floor_double(this.entity.posX), MathHelper.floor_double(this.entity.posY) + 1,
				MathHelper.floor_double(this.entity.posZ))) {
			bothers |= RAIN;
			target -= RAIN_COST;
		}

		// One look at the entities around it for both crowding and monsters
		AxisAlignedBB around = this.entity.boundingBox.expand(CROWD_RANGE, 2.0D, CROWD_RANGE);
		int animals = 1;
		boolean monsters = false;
		for (Object o : world.getEntitiesWithinAABBExcludingEntity(this.entity, around)) {
			if (o instanceof EntityAnimal) {
				animals++;
			} else if (o instanceof EntityMob) {
				monsters = true;
			}
		}
		int extra = animals - COMFORTABLE;
		if (extra > 0) {
			bothers |= CROWDED;
			target -= extra * (extra + 1) / 2;
		}
		if (monsters) {
			bothers |= MONSTERS;
			target -= MONSTER_COST;
		}

		// Room and grass come from the same look as the comforts: the ground it can walk to
		if ((comforts & Comforts.CRAMPED) != 0) {
			bothers |= CONFINED;
			target -= CONFINED_COST;
		}
		if ((comforts & Comforts.LITTLE_GRASS) != 0) {
			bothers |= NO_GRASS;
			target -= NO_GRASS_COST;
		}

		hp.comforts = comforts & (Comforts.FOOD | Comforts.WATER | Comforts.SHELTER | Comforts.BEDDING);
		hp.bothers = bothers;
		int moodBefore = hp.getMood();
		hp.moveHappinessTowards(target);
		if (moodBefore <= 0 && hp.getMood() > 0) {
			HarmonyAchievements.awardFor(this.entity, hp, HarmonyAchievements.happy);
		}
		if (var1 == null || !this.entity.getNavigator().tryMoveToXYZ(var1.xCoord, var1.yCoord, var1.zCoord, 1.0)) {
			return false;
		}
		Hazards.trimPath(this.entity);
		return !this.entity.getNavigator().noPath();
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

		// Takes over vanilla wander's priority, so it doesn't interrupt higher priority tasks like
		// a wolf's sitting or a cow's panic
		int priority = DEFAULT_PRIORITY;
		List<EntityAITaskEntry> t = pet.tasks.taskEntries;
		for(int i = 0; i < t.size(); i++) {
			if(t.get(i).action instanceof EntityAIWander) {
				priority = t.get(i).priority;
				t.remove(i);
				i--;
			}
		}
		pet.tasks.addTask(priority, new HarmonyWanderAI((EntityAnimal)pet));
	}
}
