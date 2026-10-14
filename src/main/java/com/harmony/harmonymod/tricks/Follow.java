package com.harmony.harmonymod.tricks;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

/*
 * Heel: stay right by the player until told otherwise (Stop, Sit or another trick).
 *
 * Stricter than vanilla following: it repaths every few ticks, speeds up the further behind it is,
 * and never stops trying. The trick AI runs above wandering, breeding and panicking, so nothing pulls
 * it away. It only teleports when the player does (or jumps far, e.g. an ender pearl), and goes along
 * when the player changes dimension near it (FollowTravel). If the player leaves without it, it waits.
 */
public class Follow extends PlayerTrick {
	// Close enough: stop walking
	private static final double HEEL = 2.0D;
	// Walk faster beyond these distances
	private static final double BRISK = 4.0D;
	private static final double SPRINT = 8.0D;
	// The player moving this far between checks means they teleported
	private static final double TELEPORTED = 12.0D;
	// Ticks between path updates
	private static final int REPATH_TICKS = 5;

	private int delayCounter;
	private double lastPlayerX, lastPlayerY, lastPlayerZ;
	private boolean seenPlayer;
	// The player teleported and the pet hasn't caught up yet
	private boolean catchingUp;
	// Close enough that a teleporting player has been caught up with
	private static final double CAUGHT_UP = 8.0D;
	// Where around the player to look for a spot to teleport to
	private static final int SPOT_RANGE = 3;
	private static final int SPOT_ABOVE = 2;
	private static final int SPOT_BELOW = 8;
	// Close enough to the player when they change dimension to go with them
	private static final double TRAVEL_RANGE = 16.0D;

	public boolean act() {
		if (!hasPlayer()) {
			return false;
		}
		if (--this.delayCounter > 0) {
			return true;
		}
		this.delayCounter = REPATH_TICKS;

		EntityPlayer player = getPlayer();
		if (player == null) {
			// Logged off or in another dimension: wait here for them
			this.seenPlayer = false;
			this.pet.getNavigator().clearPathEntity();
			return true;
		}

		if (this.seenPlayer && player.getDistanceSq(this.lastPlayerX, this.lastPlayerY, this.lastPlayerZ) > TELEPORTED * TELEPORTED) {
			this.catchingUp = true;
		}
		if (this.catchingUp) {
			// Keep trying until it works: the player may have landed in mid air or be flying
			if (this.pet.getDistanceToEntity(player) <= CAUGHT_UP || teleportNear(player)) {
				this.catchingUp = false;
			}
		}
		this.lastPlayerX = player.posX;
		this.lastPlayerY = player.posY;
		this.lastPlayerZ = player.posZ;
		this.seenPlayer = true;

		this.pet.getLookHelper().setLookPositionWithEntity(player, 10.0F, (float) this.pet.getVerticalFaceSpeed());
		double distance = this.pet.getDistanceToEntity(player);
		if (distance <= HEEL) {
			this.pet.getNavigator().clearPathEntity();
			return true;
		}

		double speed = distance > SPRINT ? 1.8D : (distance > BRISK ? 1.4D : 1.0D);
		if (!this.pet.getNavigator().tryMoveToEntityLiving(player, speed)) {
			// Out of pathfinding range: head toward them a few blocks at a time
			stepToward(player.posX, player.boundingBox.minY, player.posZ, speed);
		}
		return true;
	}

	/*
	 * Whether the pet was following this player, close by, when last checked
	 */
	public boolean isFollowingClosely(EntityPlayer player) {
		return isPlayer(player) && this.seenPlayer
				&& this.pet.getDistanceSq(this.lastPlayerX, this.lastPlayerY, this.lastPlayerZ) <= TRAVEL_RANGE * TRAVEL_RANGE;
	}

	/*
	 * Put a pet that just arrived in the player's world next to them
	 */
	public void arriveNear(EntityPlayer player) {
		this.pet.setLocationAndAngles(player.posX, player.boundingBox.minY, player.posZ, player.rotationYaw, 0.0F);
		teleportNear(player);
		this.lastPlayerX = player.posX;
		this.lastPlayerY = player.posY;
		this.lastPlayerZ = player.posZ;
		this.seenPlayer = true;
	}

	/*
	 * Teleport to the closest spot near the player with solid ground and room for the pet
	 */
	private boolean teleportNear(EntityPlayer player) {
		World world = this.pet.worldObj;
		int px = MathHelper.floor_double(player.posX);
		int py = MathHelper.floor_double(player.boundingBox.minY);
		int pz = MathHelper.floor_double(player.posZ);
		double bestDistance = Double.MAX_VALUE;
		int[] best = null;
		double startX = this.pet.posX, startY = this.pet.posY, startZ = this.pet.posZ;
		for (int dx = -SPOT_RANGE; dx <= SPOT_RANGE; dx++) {
			for (int dz = -SPOT_RANGE; dz <= SPOT_RANGE; dz++) {
				for (int dy = SPOT_ABOVE; dy >= -SPOT_BELOW; dy--) {
					int x = px + dx, y = py + dy, z = pz + dz;
					if (!World.doesBlockHaveSolidTopSurface(world, x, y - 1, z)) {
						continue;
					}
					this.pet.setPosition(x + 0.5D, y, z + 0.5D);
					boolean room = world.getCollidingBoundingBoxes(this.pet, this.pet.boundingBox).isEmpty()
							&& !world.isAnyLiquid(this.pet.boundingBox);
					double distance = player.getDistanceSq(x + 0.5D, y, z + 0.5D);
					if (room && distance < bestDistance) {
						bestDistance = distance;
						best = new int[] {x, y, z};
					}
					// The first solid ground found going down is this column's floor
					break;
				}
			}
		}
		// setPosition above was only for checking, put it back until the teleport
		this.pet.setPosition(startX, startY, startZ);
		if (best == null) {
			return false;
		}
		this.pet.setLocationAndAngles(best[0] + 0.5D, best[1], best[2] + 0.5D, this.pet.rotationYaw, this.pet.rotationPitch);
		this.pet.getNavigator().clearPathEntity();
		return true;
	}

	public boolean consume(Trick newTrick) {
		return newTrick instanceof Follow;
	}

	@Override
	public void stopTrick() {
		this.pet.getNavigator().clearPathEntity();
	}

	@Override
	protected String getSaveType() {
		return "follow";
	}
}
