package com.harmony.harmonymod.tricks;

import net.minecraft.entity.*;
import net.minecraft.world.*;
import net.minecraft.pathfinding.*;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import java.lang.Math;
import com.harmony.harmonymod.HarmonyProps;
import net.minecraft.nbt.NBTTagCompound;

public class MoveTo extends Trick {
    private int delayCounter;
	public Trick targetTrick;

	public void setupTrick(EntityLiving pet, Trick currentTrick) {
		this.pet = pet;
		this.targetTrick = currentTrick;
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
				return moveToPoint(targetL.targetX, targetL.targetY, targetL.targetZ, false);
			} else {
				return randomTeleport();
			}
		}
		return true;
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

	private boolean randomTeleport() {
		// TODO don't always teleport south
		teleportHelper(this.pet.posX - 6, this.pet.posY, this.pet.posZ);
		return true;
	}
}
