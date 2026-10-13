package com.harmony.harmonymod.tricks;

import net.minecraft.entity.*;
import net.minecraft.world.*;
import net.minecraft.pathfinding.*;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.util.Vec3;

public class MoveTo extends Trick {
	// Without a target the pet walks off this far, giving up if it hasn't arrived in time
	private static final int MIN_WANDER = 6;
	private static final int MAX_WANDER = 10;
	private static final int WANDER_TICKS = 200;

    private int delayCounter;
	public Trick targetTrick;
	private LocationTrick wanderTarget;
	private int wanderTicks;

	// Going to a place gives up if it hasn't got a block closer in this many ticks
	private static final int STUCK_TICKS = 200;

	private double bestDistance = Double.MAX_VALUE;
	private int ticksWithoutProgress;

	public MoveTo() {}

	/*
	 * Go to a specific target (e.g. a learned place) rather than whatever the pet was doing
	 */
	public MoveTo(Trick target) {
		this.targetTrick = target;
	}

	public void setupTrick(EntityLiving pet, Trick currentTrick) {
		this.pet = pet;
		if (this.targetTrick == null) {
			this.targetTrick = currentTrick;
		}
	}

	public boolean act() {
		if (--this.delayCounter <= 0) {
			this.delayCounter = 10;
			
			if (this.targetTrick instanceof EntityTrick) {
				EntityLiving target = ((EntityTrick) this.targetTrick).getTarget();
				if (target == null) {
					return false;
				}
				return moveToEntity(target, false);
			} else if (this.targetTrick instanceof LocationTrick) {
				LocationTrick targetL = (LocationTrick) this.targetTrick;
				double distance = this.pet.getDistance(targetL.targetX, targetL.targetY, targetL.targetZ);
				if (distance < this.bestDistance - 1.0D) {
					this.bestDistance = distance;
					this.ticksWithoutProgress = 0;
				} else if ((this.ticksWithoutProgress += 10) > STUCK_TICKS) {
					// Can't get there
					this.pet.getNavigator().clearPathEntity();
					return false;
				}
				return moveToPoint(targetL.targetX, targetL.targetY, targetL.targetZ, false);
			} else {
				return wander();
			}
		}
		return true;
	}

	/*
	 * Walk to a random reachable spot a few blocks away, then stop
	 */
	private boolean wander() {
		if (this.wanderTarget == null) {
			this.wanderTarget = pickWanderTarget();
			if (this.wanderTarget == null) {
				return false;
			}
		}
		this.wanderTicks += 10;
		if (this.wanderTicks > WANDER_TICKS) {
			this.pet.getNavigator().clearPathEntity();
			return false;
		}
		return moveToPoint(this.wanderTarget.targetX, this.wanderTarget.targetY, this.wanderTarget.targetZ, false);
	}

	private LocationTrick pickWanderTarget() {
		if (!(this.pet instanceof EntityCreature)) {
			return null;
		}
		for (int attempt = 0; attempt < 10; attempt++) {
			Vec3 spot = RandomPositionGenerator.findRandomTarget((EntityCreature) this.pet, MAX_WANDER, 7);
			if (spot != null && this.pet.getDistance(spot.xCoord, this.pet.posY, spot.zCoord) >= MIN_WANDER) {
				return new LocationTrick(spot.xCoord, spot.yCoord, spot.zCoord);
			}
		}
		return null;
	}

	public boolean isInstant() {
		return false;
	}

	public boolean consume(Trick newTrick) {
		if (newTrick instanceof MoveTo) {
			return true;
		} else if (newTrick instanceof EntityTrick || newTrick instanceof LocationTrick) {
			this.targetTrick = newTrick;
			return true;
		}
		return false;
	}

	@Override
	protected String getSaveType() {
		return "move_to";
	}

	@Override
	protected void writeToNBT(NBTTagCompound tag) {
		NBTTagCompound target = Trick.saveTrick(this.targetTrick);
		if (target != null) {
			tag.setTag("Target", target);
		}
	}

	@Override
	protected void readFromNBT(NBTTagCompound tag) {
		if (tag.hasKey("Target")) {
			this.targetTrick = Trick.loadTrick(tag.getCompoundTag("Target"), this.pet);
		}
	}


}
