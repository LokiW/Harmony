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

		boolean wandering = var1 != null && this.entity.getNavigator().tryMoveToXYZ(var1.xCoord, var1.yCoord, var1.zCoord, 1.0);
		if (!wandering) {
			// Nowhere to go
			bothers |= CONFINED;
			target -= CONFINED_COST;
		} else {
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
				bothers |= CONFINED;
				target -= CONFINED_COST;
			}

			// Grass to graze is expected, having none bothers it
			if (!(world.getBlock(tx, groundY, tz) instanceof IGrowable)) {
				bothers |= NO_GRASS;
				target -= NO_GRASS_COST;
			}
		}

		hp.comforts = comforts;
		hp.bothers = bothers;
		hp.moveHappinessTowards(target);
		return wandering;
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
