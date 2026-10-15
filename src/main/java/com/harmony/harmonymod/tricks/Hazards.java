package com.harmony.harmonymod.tricks;

import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLiving;
import net.minecraft.pathfinding.PathEntity;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.potion.Potion;
import net.minecraft.world.World;

/*
 * Keeps animals out of fire and lava unless they're fire resistant. Vanilla pathfinding already
 * avoids lava but walks straight through fire, and teleporting checks neither.
 */
public class Hazards {
	public static boolean fireproof(EntityLiving animal) {
		return animal.isImmuneToFire() || animal.isPotionActive(Potion.fireResistance);
	}

	/*
	 * Fire or lava where an animal standing at (x, y, z) would be, or right next to it: an animal
	 * walking up to a spot overshoots a little, and touching a fire block sets it alight
	 */
	public static boolean burning(World world, int x, int y, int z) {
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				if (isBurning(world.getBlock(x + dx, y, z + dz).getMaterial())
						|| isBurning(world.getBlock(x + dx, y + 1, z + dz).getMaterial())) {
					return true;
				}
			}
		}
		return false;
	}

	private static boolean isBurning(Material material) {
		return material == Material.fire || material == Material.lava;
	}

	/*
	 * Cut the animal's current path short before it would step into fire or lava. Call after setting
	 * a path.
	 */
	public static void trimPath(EntityLiving animal) {
		PathEntity path = animal.getNavigator().getPath();
		if (path == null || fireproof(animal)) {
			return;
		}
		for (int i = path.getCurrentPathIndex(); i < path.getCurrentPathLength(); i++) {
			PathPoint point = path.getPathPointFromIndex(i);
			if (burning(animal.worldObj, point.xCoord, point.yCoord, point.zCoord)) {
				if (i <= path.getCurrentPathIndex()) {
					animal.getNavigator().clearPathEntity();
				} else {
					path.setCurrentPathLength(i);
				}
				return;
			}
		}
	}
}
