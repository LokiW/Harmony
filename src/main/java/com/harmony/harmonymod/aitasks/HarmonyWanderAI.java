package com.harmony.harmonymod.aitasks;

import com.harmony.harmonymod.HarmonyProps;
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
import net.minecraft.block.Block;
import net.minecraft.block.IGrowable;
import net.minecraft.pathfinding.PathEntity;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

public class HarmonyWanderAI extends EntityAIBase
{
	private EntityAnimal entity;

	// More animals than this in the 5x5 area around where it's going makes an animal unhappier
	private static final int CROWDED = 8;
	// Happiness changes each time an animal looks for somewhere to wander
	private static final int CROWDED_COST = 4;
	private static final int CONFINED_COST = 2;
	private static final int HURT_COST = 1;
	private static final int MONSTER_COST = 1;
	private static final int NO_GRASS_COST = 1;
	private static final int RAIN_COST = 1;
	private static final int PER_COMFORT = 1;
	// Blocks an animal must be able to walk towards where it wants to go, or it's confined
	private static final double ROOM_TO_WALK = 3.0D;
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
		HarmonyProps hp;
		if (this.entity.getAge() >= 100) {
			return false;
		} else if (this.entity.getRNG().nextInt(120) != 0) {
			return false;
		} else {
			/*
			 * Wild conditions (grass, room to roam, no crowds or monsters) only keep an animal content.
			 * What makes it happy are comforts players provide, see Comforts.
			 */
			boolean canHappinessIncrease = true;
			int change = 0;
			Vec3 var1 = RandomPositionGenerator.findRandomTarget(this.entity, 10, 7);
			hp = HarmonyProps.get(this.entity);
			World world = this.entity.worldObj;
			int comforts = Comforts.get(this.entity);

			if (this.entity.getHealth() < this.entity.getMaxHealth()) {
				change -= HURT_COST;
				canHappinessIncrease = false;
			}
			// Left out in the rain with nowhere to shelter
			if (world.isRaining() && (comforts & Comforts.SHELTER) == 0 && world.canLightningStrikeAt(
					MathHelper.floor_double(this.entity.posX), MathHelper.floor_double(this.entity.posY) + 1,
					MathHelper.floor_double(this.entity.posZ))) {
				change -= RAIN_COST;
			}

			// Only overcrowding is bad, small groups and pairs are fine. Measured where the animal is.
			AxisAlignedBB boxToCheck = AxisAlignedBB.getBoundingBox(this.entity.posX - 2.5D, this.entity.posY - 1.0D,
					this.entity.posZ - 2.5D, this.entity.posX + 2.5D, this.entity.posY + 3.0D, this.entity.posZ + 2.5D);
			// One look at the entities around it for both crowding and monsters
			int animals = 1;
			boolean monsters = false;
			for (Object o : world.getEntitiesWithinAABBExcludingEntity(this.entity, boxToCheck)) {
				if (o instanceof EntityAnimal) {
					animals++;
				} else if (o instanceof EntityMob) {
					monsters = true;
				}
			}
			if (animals > CROWDED) {
				change -= CROWDED_COST;
				canHappinessIncrease = false;
			}
			if (monsters) {
				change -= MONSTER_COST;
				canHappinessIncrease = false;
			}

			if (var1 == null || !this.entity.getNavigator().tryMoveToXYZ(var1.xCoord, var1.yCoord, var1.zCoord, 1.0)) {
				// Nowhere to go: confined
				hp.changeHappiness(change - CONFINED_COST);
				return false;
			}

			// The wander target can be up in the air, the animal walks to the ground below it
			int tx = MathHelper.floor_double(var1.xCoord);
			int tz = MathHelper.floor_double(var1.zCoord);
			int groundY = groundBelow(world, tx, MathHelper.floor_double(var1.yCoord), tz);

			// It can barely move towards where it wants to go (penned in tightly): confined, though it
			// still wanders. A pen with room to walk around is fine.
			PathEntity path = this.entity.getNavigator().getPath();
			PathPoint end = path != null ? path.getFinalPathPoint() : null;
			if (end == null || (horizontalDistance(this.entity, tx, tz) > ROOM_TO_WALK
					&& horizontalDistance(this.entity, end.xCoord, end.zCoord) < ROOM_TO_WALK)) {
				change -= CONFINED_COST;
				canHappinessIncrease = false;
			}

			// Grass to graze doesn't make an animal happier, but having none makes it unhappier
			Block target = world.getBlock(tx, groundY, tz);
			if (!(target instanceof IGrowable)) {
				change -= NO_GRASS_COST;
			}

			if (canHappinessIncrease) {
				change += Comforts.count(comforts) * PER_COMFORT;
			}
			hp.changeHappiness(change);
			return true;
		}
	}

	/**
	 * Returns whether an in-progress EntityAIBase should continue executing
	 */
	public boolean continueExecuting() {
		return !this.entity.getNavigator().noPath();
	}


	private static double horizontalDistance(EntityLiving entity, int x, int z) {
		double dx = x + 0.5D - entity.posX, dz = z + 0.5D - entity.posZ;
		return Math.sqrt(dx * dx + dz * dz);
	}

	/*
	 * The ground under a spot: the first block going down that's solid or grows (grass, tall grass)
	 */
	private static int groundBelow(World world, int x, int y, int z) {
		int lowest = Math.max(0, y - 16);
		for (int gy = y; gy > lowest; gy--) {
			Block block = world.getBlock(x, gy, z);
			if (block.getMaterial().isSolid() || block instanceof IGrowable) {
				return gy;
			}
		}
		return lowest;
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
