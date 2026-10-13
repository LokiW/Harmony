package com.harmony.harmonymod.tricks;

import net.minecraft.entity.player.EntityPlayer;

/*
 * Come back to the player and stop next to them. Gives up after a while if it can't get there.
 */
public class ComeBack extends PlayerTrick {
	private static final double ARRIVED = 3.0D;
	private static final int GIVE_UP_TICKS = 30 * 20;
	private static final int REPATH_TICKS = 5;

	private int delayCounter;
	private int ticks;

	public boolean act() {
		this.ticks++;
		if (!hasPlayer() || this.ticks > GIVE_UP_TICKS) {
			this.pet.getNavigator().clearPathEntity();
			return false;
		}
		if (--this.delayCounter > 0) {
			return true;
		}
		this.delayCounter = REPATH_TICKS;

		EntityPlayer player = getPlayer();
		if (player == null) {
			return false;
		}
		this.pet.getLookHelper().setLookPositionWithEntity(player, 10.0F, (float) this.pet.getVerticalFaceSpeed());
		double distance = this.pet.getDistanceToEntity(player);
		if (distance <= ARRIVED) {
			this.pet.getNavigator().clearPathEntity();
			return false;
		}
		double speed = distance > 8.0D ? 1.6D : 1.2D;
		if (!this.pet.getNavigator().tryMoveToEntityLiving(player, speed)) {
			stepToward(player.posX, player.boundingBox.minY, player.posZ, speed);
		}
		return true;
	}

	public boolean consume(Trick newTrick) {
		return newTrick instanceof ComeBack;
	}

	@Override
	public void stopTrick() {
		this.pet.getNavigator().clearPathEntity();
	}

	@Override
	protected String getSaveType() {
		return "come_back";
	}
}
