package com.harmony.harmonymod.aitasks;

import com.harmony.harmonymod.HarmonyProps;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLiving;
import net.minecraft.init.Blocks;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

/*
 * Domestication comforts players build around animals, which make them happier: food (a hay bale),
 * water (water or a filled cauldron), shelter (both roofed and open ground to stand on) and bedding
 * (carpet under a roof). Wild conditions alone only keep an animal content.
 */
public class Comforts {
	public static final int FOOD = 1;
	public static final int WATER = 2;
	public static final int SHELTER = 4;
	public static final int BEDDING = 8;

	// How far around the animal comforts count
	private static final int RANGE = 8;
	private static final int BELOW = 2;
	private static final int ABOVE = 3;
	// Ticks before an animal looks around for comforts again
	private static final int RECHECK_TICKS = 600;

	/*
	 * Comforts around the animal, as FOOD | WATER | ..., looked up again every RECHECK_TICKS
	 */
	public static int get(EntityLiving animal, HarmonyProps hp) {
		long now = animal.worldObj.getTotalWorldTime();
		if (hp.comfortsCheckedAt < 0 || now - hp.comfortsCheckedAt >= RECHECK_TICKS) {
			hp.comforts = find(animal);
			hp.comfortsCheckedAt = now;
		}
		return hp.comforts;
	}

	public static int count(int comforts) {
		return Integer.bitCount(comforts);
	}

	private static int find(EntityLiving animal) {
		World world = animal.worldObj;
		int ax = MathHelper.floor_double(animal.posX);
		int ay = MathHelper.floor_double(animal.boundingBox.minY);
		int az = MathHelper.floor_double(animal.posZ);
		int found = 0;
		boolean openGround = false, roofedGround = false;
		for (int x = ax - RANGE; x <= ax + RANGE; x++) {
			for (int z = az - RANGE; z <= az + RANGE; z++) {
				for (int y = ay - BELOW; y <= ay + ABOVE; y++) {
					Block block = world.getBlock(x, y, z);
					if (block == Blocks.hay_block) {
						found |= FOOD;
					} else if (block.getMaterial() == Material.water
							|| (block == Blocks.cauldron && world.getBlockMetadata(x, y, z) > 0)) {
						found |= WATER;
					} else if (block == Blocks.carpet && !world.canBlockSeeTheSky(x, y + 1, z)) {
						found |= BEDDING;
					}
					if (isStandable(world, x, y, z)) {
						if (world.canBlockSeeTheSky(x, y, z)) {
							openGround = true;
						} else {
							roofedGround = true;
						}
					}
				}
			}
		}
		if (openGround && roofedGround) {
			found |= SHELTER;
		}
		return found;
	}

	/*
	 * Solid ground with two blocks of room above it (carpet counts as room)
	 */
	private static boolean isStandable(World world, int x, int y, int z) {
		return world.getBlock(x, y - 1, z).getMaterial().isSolid()
				&& isRoom(world.getBlock(x, y, z)) && isRoom(world.getBlock(x, y + 1, z));
	}

	private static boolean isRoom(Block block) {
		return !block.getMaterial().isSolid() && !block.getMaterial().isLiquid() || block == Blocks.carpet;
	}
}
