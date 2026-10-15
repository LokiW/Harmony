package com.harmony.harmonymod.aitasks;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLiving;
import net.minecraft.init.Blocks;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/*
 * Domestication comforts players build around animals, which make them happier: food (a hay bale),
 * water (water or a filled cauldron), shelter (both roofed and open ground to stand on) and bedding
 * (carpet under a roof). Wild conditions alone only keep an animal content.
 *
 * Looking around is the expensive part, so animals standing in the same small area share one look,
 * and it's only repeated every RECHECK_TICKS.
 */
public class Comforts {
	public static final int FOOD = 1;
	public static final int WATER = 2;
	public static final int SHELTER = 4;
	public static final int BEDDING = 8;
	private static final int ALL = FOOD | WATER | SHELTER | BEDDING;

	// How far around the animal comforts count
	private static final int RANGE = 8;
	private static final int BELOW = 2;
	private static final int ABOVE = 3;
	// Ticks before an area is looked at again
	private static final int RECHECK_TICKS = 600;
	// Animals in the same 4x4 column of blocks (at the same height) share a look around
	private static final int AREA_SHIFT = 2;
	// Areas remembered at once, old ones are dropped past this
	private static final int MAX_AREAS = 4096;

	private static final Map<Long, Area> areas = new HashMap<Long, Area>();

	private static class Area {
		final long checkedAt;
		final int comforts;

		Area(long checkedAt, int comforts) {
			this.checkedAt = checkedAt;
			this.comforts = comforts;
		}
	}

	/*
	 * Comforts around the animal, as FOOD | WATER | ...
	 */
	public static int get(EntityLiving animal) {
		World world = animal.worldObj;
		int ax = MathHelper.floor_double(animal.posX) >> AREA_SHIFT;
		int ay = MathHelper.floor_double(animal.boundingBox.minY);
		int az = MathHelper.floor_double(animal.posZ) >> AREA_SHIFT;
		long key = ((long) (ax & 0x3FFFFF) << 42) | ((long) (az & 0x3FFFFF) << 20) | ((ay & 0xFF) << 12)
				| (world.provider.dimensionId & 0xFFF);
		long now = world.getTotalWorldTime();
		Area area = areas.get(key);
		if (area == null || now - area.checkedAt >= RECHECK_TICKS || now < area.checkedAt) {
			if (areas.size() >= MAX_AREAS) {
				forgetOld(now);
			}
			int half = (1 << AREA_SHIFT) / 2;
			area = new Area(now, find(world, (ax << AREA_SHIFT) + half, ay, (az << AREA_SHIFT) + half));
			areas.put(key, area);
		}
		return area.comforts;
	}

	/*
	 * How much happier these comforts make an animal. All of them take a content animal just past
	 * breeding happiness.
	 */
	public static int happiness(int comforts) {
		int points = 0;
		if ((comforts & FOOD) != 0) points += 2;
		if ((comforts & WATER) != 0) points += 2;
		if ((comforts & SHELTER) != 0) points += 1;
		if ((comforts & BEDDING) != 0) points += 1;
		return points;
	}

	private static void forgetOld(long now) {
		Iterator<Area> it = areas.values().iterator();
		while (it.hasNext()) {
			Area area = it.next();
			if (now - area.checkedAt >= RECHECK_TICKS || now < area.checkedAt) {
				it.remove();
			}
		}
		if (areas.size() >= MAX_AREAS) {
			areas.clear();
		}
	}

	/*
	 * Look around a spot. Reads each column of blocks straight from its chunk, skips chunks that
	 * aren't loaded (never loads them), and stops once everything has been found.
	 */
	private static int find(World world, int cx, int cy, int cz) {
		IChunkProvider provider = world.getChunkProvider();
		int yMin = Math.max(1, cy - BELOW);
		int yMax = Math.min(world.getHeight() - 2, cy + ABOVE);
		if (yMax < yMin) {
			return 0;
		}
		// The column from yMin - 1 (ground under the lowest spot) to yMax + 1 (headroom over the highest)
		Block[] column = new Block[yMax - yMin + 3];
		int found = 0;
		boolean openGround = false, roofedGround = false;
		Chunk chunk = null;
		int chunkX = Integer.MIN_VALUE, chunkZ = Integer.MIN_VALUE;

		for (int x = cx - RANGE; x <= cx + RANGE; x++) {
			for (int z = cz - RANGE; z <= cz + RANGE; z++) {
				if (x >> 4 != chunkX || z >> 4 != chunkZ) {
					chunkX = x >> 4;
					chunkZ = z >> 4;
					chunk = provider.chunkExists(chunkX, chunkZ) ? provider.provideChunk(chunkX, chunkZ) : null;
				}
				if (chunk == null) {
					continue;
				}
				int lx = x & 15, lz = z & 15;
				for (int i = 0; i < column.length; i++) {
					column[i] = chunk.getBlock(lx, yMin - 1 + i, lz);
				}
				for (int y = yMin; y <= yMax; y++) {
					Block block = column[y - yMin + 1];
					if (block == Blocks.hay_block) {
						found |= FOOD;
					} else if (block.getMaterial() == Material.water
							|| (block == Blocks.cauldron && chunk.getBlockMetadata(lx, y, lz) > 0)) {
						found |= WATER;
					} else if (block == Blocks.carpet && !chunk.canBlockSeeTheSky(lx, y + 1, lz)) {
						found |= BEDDING;
					}
					// Ground an animal could stand on: solid below, room for two blocks above
					if (column[y - yMin].getMaterial().isSolid() && isRoom(block) && isRoom(column[y - yMin + 2])) {
						if (chunk.canBlockSeeTheSky(lx, y, lz)) {
							openGround = true;
						} else {
							roofedGround = true;
						}
					}
				}
				if ((found | SHELTER) == ALL && openGround && roofedGround) {
					return ALL;
				}
			}
		}
		if (openGround && roofedGround) {
			found |= SHELTER;
		}
		return found;
	}

	/*
	 * Space an animal can stand in (carpet counts)
	 */
	private static boolean isRoom(Block block) {
		Material material = block.getMaterial();
		return !material.isSolid() && !material.isLiquid();
	}
}
