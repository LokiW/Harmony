package com.harmony.harmonymod.tricks;

import net.minecraft.entity.*;
import net.minecraft.world.*;
import net.minecraft.pathfinding.*;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import java.lang.Math;
import com.harmony.harmonymod.HarmonyProps;

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
				EntityTrick targetE = (EntityTrick) this.targetTrick;
				return moveToEntity(targetE.target, targetE.teleport);
			} else if (this.targetTrick instanceof LocationTrick) {
				LocationTrick targetL = (LocationTrick) this.targetTrick;
				return moveToPoint(targetL.targetX, targetL.targetY, targetL.targetZ, targetL.teleport);
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

	private boolean randomTeleport() {
		// TODO don't always teleport south
		teleportHelper(this.pet.posX - 6, this.pet.posY, this.pet.posZ);
		return true;
	}
}
