package com.harmony.harmonymod.tricks;

import net.minecraft.entity.player.EntityPlayer;

/*
 * Heel: stay right by the player until told otherwise (Stop, Sit or another trick).
 *
 * Stricter than vanilla following: it repaths every few ticks, speeds up the further behind it is,
 * and never stops trying. The trick AI runs above wandering, breeding and panicking, so nothing pulls
 * it away. It only teleports when the player does (or jumps far, e.g. an ender pearl), and waits if
 * the player leaves the dimension.
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
			teleportHelper(player.posX, player.boundingBox.minY, player.posZ);
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
