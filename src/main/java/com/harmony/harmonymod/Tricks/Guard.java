package com.harmony.harmonymod.tricks;

import net.minecraft.entity.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.*;
import net.minecraft.pathfinding.*;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.util.DamageSource;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import java.lang.Math;
import java.util.*;
import com.harmony.harmonymod.HarmonyProps;

public class Guard extends Trick {
    private int delayCounter;
	private Trick guardTarget;

	public void setupTrick(EntityLiving pet, Trick currentTrick) {
		this.pet = pet;
		if (currentTrick instanceof LocationTrick || currentTrick instanceof EntityTrick) {
			this.guardTarget = currentTrick;	
		} else {
			this.guardTarget = new LocationTrick(this.pet.posX, this.pet.posY, this.pet.posZ, false);
		}
	}

	public boolean act() {
		if (--this.delayCounter <= 0) {
			this.delayCounter = 10;

			if (this.pet.getAttackTarget() == null || this.pet.getAttackTarget().isDead) {
				boolean getNewTarget = false;
				// If to far away from guard location/entity move back
				if (this.guardTarget instanceof LocationTrick) {
					LocationTrick trickL = (LocationTrick) guardTarget;
					moveToPoint(trickL.targetX, trickL.targetY, trickL.targetZ, false);
					if (this.pet.getDistanceSq(trickL.targetX, trickL.targetY, trickL.targetZ) <= 10.0D)
						getNewTarget = true;
				} else {
					EntityTrick trickE = (EntityTrick) guardTarget;
					moveToEntity(trickE.target, true);
					if (this.pet.getDistanceSqToEntity(trickE.target) <= 10.0D)
						getNewTarget = true;
				}

				if (getNewTarget) {
					// Find target to attack
					setNewAttackTarget();
				}
			}
			attackTarget();
		}
		return true;
	}

	public boolean isInstant() {
		return false;
	}

	public boolean consume(Trick newTrick) {
		if (newTrick instanceof Guard) {
			return true;
		} else if (newTrick instanceof EntityTrick || newTrick instanceof LocationTrick) {
			this.guardTarget = newTrick;
			return true;
		}
		return false;
	}

	private void setNewAttackTarget() {
		double targetX;
		double targetY;
		double targetZ;
		if (guardTarget instanceof EntityTrick) {
			EntityTrick trickE = (EntityTrick) guardTarget;
			targetX = trickE.target.posX;
			targetY = trickE.target.posY;
			targetZ = trickE.target.posZ;
		} else {
			LocationTrick trickL = (LocationTrick) guardTarget;
			targetX = trickL.targetX;
			targetY = trickL.targetY;
			targetZ = trickL.targetZ;
		}
		
		AxisAlignedBB boxToCheck = AxisAlignedBB.getBoundingBox(targetX - 5, targetY - 2, targetZ - 5, 
																targetX + 5, targetY + 2, targetZ + 5);
		List e = this.pet.worldObj.getEntitiesWithinAABB(EntityMob.class, boxToCheck);

		for (Object entity : e) {
			if (entity instanceof EntityMob) {
				EntityMob mob = (EntityMob) entity;
				if (mob instanceof EntityCreeper)
					continue;

				if (guardTarget instanceof EntityTrick) {
					if (mob.getAttackTarget() == ((EntityTrick) guardTarget).target) {
						this.pet.setAttackTarget(mob);
						return;
					}
				}
				if (mob.getAttackTarget() == this.pet) {
					this.pet.setAttackTarget(mob);
					return;
				}
			}
		}
		// Even if they aren't attacking this, just attack the first one
		Entity closest = this.pet.worldObj.findNearestEntityWithinAABB(EntityMob.class, boxToCheck, this.pet);
		if (closest instanceof EntityMob)
			this.pet.setAttackTarget((EntityMob) closest);

	}
}
