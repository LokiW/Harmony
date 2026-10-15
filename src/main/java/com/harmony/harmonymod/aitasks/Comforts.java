package com.harmony.harmonymod.aitasks;

import net.minecraft.block.Block;
import net.minecraft.block.BlockFence;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.BlockWall;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLiving;
import net.minecraft.init.Blocks;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/*
 * Domestication comforts players build around animals, which make them happier: food (a hay bale),
 * water (water or a filled cauldron), shelter (both roofed and open ground) and bedding (carpet under
 * a roof). Only comforts the animal can walk to count, so ones on the other side of its pen's fence
 * don't: the ground it can reach is flood filled from where it stands, through gaps and open gates
 * but not over fences, walls or closed gates.
 *
 * Looking around is the expensive part, so it's only repeated every RECHECK_TICKS, and animals in the
 * same small area that can reach each other (in the same pen) share one look.
 */
public class Comforts {
	public static final int FOOD = 1;
	public static final int WATER = 2;
	public static final int SHELTER = 4;
	public static final int BEDDING = 8;

	// How far around the animal comforts count
	private static final int RANGE = 8;
	// Levels it may walk down or up to from where it stands
	private static final int BELOW = 2;
	private static final int ABOVE = 3;
	// How far it may drop down to the ground next to it
	private static final int MAX_DROP = 3;
	// Ticks before an area is looked at again
	private static final int RECHECK_TICKS = 600;
	// Animals in the same 4x4 column of blocks (at the same height) share a look around
	private static final int AREA_SHIFT = 2;
	// Areas remembered at once, old ones are dropped past this
	private static final int MAX_AREAS = 4096;

	private static final int SIZE = RANGE * 2 + 1;
	private static final int LEVELS = BELOW + ABOVE + 1;
	// Blocks read per column: the ground under the lowest level to two above the highest (headroom
	// for stepping up)
	private static final int COLUMN = LEVELS + 3;

	private static final Map<Long, Area> areas = new HashMap<Long, Area>();

	/*
	 * One look around: what was found, and the ground that could be walked to
	 */
	private static class Area {
		final long checkedAt;
		final int comforts;
		final int x, y, z;
		final BitSet reached;

		Area(long checkedAt, int comforts, int x, int y, int z, BitSet reached) {
			this.checkedAt = checkedAt;
			this.comforts = comforts;
			this.x = x;
			this.y = y;
			this.z = z;
			this.reached = reached;
		}

		boolean covers(int bx, int by, int bz) {
			int dx = bx - x + RANGE, dz = bz - z + RANGE, level = by - y + BELOW;
			return dx >= 0 && dx < SIZE && dz >= 0 && dz < SIZE && level >= 0 && level < LEVELS
					&& reached.get(spot(dx, level, dz));
		}
	}

	/*
	 * Comforts around the animal, as FOOD | WATER | ...
	 */
	public static int get(EntityLiving animal) {
		World world = animal.worldObj;
		int bx = MathHelper.floor_double(animal.posX);
		int by = MathHelper.floor_double(animal.boundingBox.minY);
		int bz = MathHelper.floor_double(animal.posZ);
		long key = ((long) ((bx >> AREA_SHIFT) & 0x3FFFFF) << 42) | ((long) ((bz >> AREA_SHIFT) & 0x3FFFFF) << 20)
				| ((by & 0xFF) << 12) | (world.provider.dimensionId & 0xFFF);
		long now = world.getTotalWorldTime();
		Area area = areas.get(key);
		if (area == null || now - area.checkedAt >= RECHECK_TICKS || now < area.checkedAt || !area.covers(bx, by, bz)) {
			if (areas.size() >= MAX_AREAS) {
				forgetOld(now);
			}
			area = new Look(world, bx, by, bz).find(now);
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

	private static int spot(int dx, int level, int dz) {
		return (dx * SIZE + dz) * LEVELS + level;
	}

	/*
	 * Flood fill of the ground an animal can walk to from (x, y, z). The blocks around are read once,
	 * column by column straight from their chunks; chunks that aren't loaded are skipped, never loaded.
	 */
	private static class Look {
		final World world;
		final int x, y, z;
		final int yBase;
		final Block[] blocks = new Block[SIZE * SIZE * COLUMN];
		final Chunk[] chunks = new Chunk[SIZE * SIZE];

		Look(World world, int x, int y, int z) {
			this.world = world;
			this.x = x;
			this.y = y;
			this.z = z;
			this.yBase = y - BELOW - 1;
			IChunkProvider provider = world.getChunkProvider();
			Chunk chunk = null;
			int chunkX = Integer.MIN_VALUE, chunkZ = Integer.MIN_VALUE;
			for (int dx = 0; dx < SIZE; dx++) {
				for (int dz = 0; dz < SIZE; dz++) {
					int wx = x - RANGE + dx, wz = z - RANGE + dz;
					if (wx >> 4 != chunkX || wz >> 4 != chunkZ) {
						chunkX = wx >> 4;
						chunkZ = wz >> 4;
						chunk = provider.chunkExists(chunkX, chunkZ) ? provider.provideChunk(chunkX, chunkZ) : null;
					}
					if (chunk == null) {
						continue;
					}
					chunks[dx * SIZE + dz] = chunk;
					int base = (dx * SIZE + dz) * COLUMN;
					for (int i = 0; i < COLUMN; i++) {
						int by = yBase + i;
						if (by >= 0 && by < world.getHeight()) {
							blocks[base + i] = chunk.getBlock(wx & 15, by, wz & 15);
						}
					}
				}
			}
		}

		/*
		 * The block at a spot in the grid, null if it's outside it or not loaded
		 */
		Block block(int dx, int by, int dz) {
			int i = by - yBase;
			if (dx < 0 || dx >= SIZE || dz < 0 || dz >= SIZE || i < 0 || i >= COLUMN) {
				return null;
			}
			return blocks[(dx * SIZE + dz) * COLUMN + i];
		}

		int meta(int dx, int by, int dz) {
			return chunks[dx * SIZE + dz].getBlockMetadata((x - RANGE + dx) & 15, by, (z - RANGE + dz) & 15);
		}

		/*
		 * Space an animal can be in: not solid or liquid (carpet and plants are fine), or an open gate
		 */
		boolean room(int dx, int by, int dz) {
			Block block = block(dx, by, dz);
			if (block == null) {
				return false;
			}
			if (block instanceof BlockFenceGate) {
				return BlockFenceGate.isFenceGateOpen(meta(dx, by, dz));
			}
			Material material = block.getMaterial();
			return !material.isSolid() && !material.isLiquid();
		}

		/*
		 * Ground it could stand on at level by: solid under it (but not the top of a fence or wall,
		 * which it can't jump onto), room for two blocks above
		 */
		boolean standable(int dx, int by, int dz) {
			if (by - y + BELOW < 0 || by - y + BELOW >= LEVELS) {
				return false;
			}
			Block ground = block(dx, by - 1, dz);
			return ground != null && ground.getMaterial().isSolid() && !(ground instanceof BlockFence)
					&& !(ground instanceof BlockWall) && !(ground instanceof BlockFenceGate)
					&& room(dx, by, dz) && room(dx, by + 1, dz);
		}

		Area find(long now) {
			BitSet reached = new BitSet(SIZE * SIZE * LEVELS);
			int[] queue = new int[SIZE * SIZE * LEVELS];
			int head = 0, tail = 0;
			int found = 0;
			boolean openGround = false, roofedGround = false;
			// Where it stands counts even if it isn't ordinary ground (e.g. on a slab)
			int start = spot(RANGE, BELOW, RANGE);
			reached.set(start);
			queue[tail++] = start;

			while (head < tail) {
				int s = queue[head++];
				int level = s % LEVELS;
				int dz = (s / LEVELS) % SIZE;
				int dx = s / LEVELS / SIZE;
				int by = y - BELOW + level;
				Chunk chunk = chunks[dx * SIZE + dz];
				if (chunk == null) {
					continue;
				}
				int lx = (x - RANGE + dx) & 15, lz = (z - RANGE + dz) & 15;
				if (chunk.canBlockSeeTheSky(lx, by, lz)) {
					openGround = true;
				} else {
					roofedGround = true;
				}
				if (block(dx, by, dz) == Blocks.carpet && !chunk.canBlockSeeTheSky(lx, by + 1, lz)) {
					found |= BEDDING;
				}

				for (int dir = 0; dir < 4; dir++) {
					int nx = dx + (dir == 0 ? 1 : dir == 1 ? -1 : 0);
					int nz = dz + (dir == 2 ? 1 : dir == 3 ? -1 : 0);
					if (nx < 0 || nx >= SIZE || nz < 0 || nz >= SIZE || chunks[nx * SIZE + nz] == null) {
						continue;
					}
					found |= comfortNextTo(nx, by, nz);

					// Walk on, step up a block, or drop down to the ground below
					int next = Integer.MIN_VALUE;
					if (standable(nx, by, nz)) {
						next = by;
					} else if (standable(nx, by + 1, nz) && room(dx, by + 2, dz)) {
						next = by + 1;
					} else if (room(nx, by, nz) && room(nx, by + 1, nz)) {
						for (int drop = 1; drop <= MAX_DROP; drop++) {
							if (standable(nx, by - drop, nz)) {
								next = by - drop;
								break;
							}
							if (!room(nx, by - drop, nz)) {
								break;
							}
						}
					}
					if (next != Integer.MIN_VALUE) {
						int n = spot(nx, next - y + BELOW, nz);
						if (!reached.get(n)) {
							reached.set(n);
							queue[tail++] = n;
						}
					}
				}
				if ((found & (FOOD | WATER | BEDDING)) == (FOOD | WATER | BEDDING) && openGround && roofedGround) {
					break;
				}
			}
			if (openGround && roofedGround) {
				found |= SHELTER;
			}
			return new Area(now, found, x, y, z, reached);
		}

		/*
		 * Food or water right next to ground at level by: beside its feet or head, or water a level down
		 */
		int comfortNextTo(int dx, int by, int dz) {
			int found = 0;
			for (int h = -1; h <= 1; h++) {
				Block block = block(dx, by + h, dz);
				if (block == null) {
					continue;
				}
				if (block == Blocks.hay_block && h >= 0) {
					found |= FOOD;
				} else if (block.getMaterial() == Material.water
						|| (block == Blocks.cauldron && h == 0 && meta(dx, by, dz) > 0)) {
					found |= WATER;
				}
			}
			return found;
		}
	}
}
