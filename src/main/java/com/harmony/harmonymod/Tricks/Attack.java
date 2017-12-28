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
import com.harmony.harmonymod.sounds.SoundDB.InstrumentSound;

public class Attack extends Trick {
    private int delayCounter;

	public void setupTrick(EntityCreature pet, Trick currentTrick) {
		this.pet = pet;
		if (currentTrick instanceof EntityTrick) {
			EntityTrick eTrick = (EntityTrick) currentTrick;
			this.pet.setAttackTarget((EntityLivingBase) eTrick.target);
		}
	}

	public boolean act() {
		if (--this.delayCounter <= 0) {
			this.delayCounter = 10;
			if (this.pet.getAttackTarget() == null) {
				// Find target to attack
				setNewAttackTarget();
			}
			attackTarget();
			// check if specified target killed, if so stop trying to attack things
			if (this.pet.getAttackTarget() == null || this.pet.getAttackTarget().isDead) {
				PathNavigate petPathfinder = this.pet.getNavigator();
				if (petPathfinder != null)
					petPathfinder.clearPathEntity();
				return false;
			}
		}
		return true;
	}

	public boolean isInstant() {
		return false;
	}

	public boolean consume(Trick newTrick) {
		if (newTrick instanceof Attack) {
			return true;
		} else if (newTrick instanceof EntityTrick) {
			EntityTrick eTrick = (EntityTrick) newTrick;
			this.pet.setAttackTarget((EntityLivingBase) eTrick.target);
			return true;
		}
		return false;
	}

	private void setNewAttackTarget() {
		AxisAlignedBB boxToCheck = AxisAlignedBB.getBoundingBox(this.pet.posX - 5, this.pet.posY - 2, this.pet.posZ - 5, 
																this.pet.posX + 5, this.pet.posY + 2, this.pet.posZ + 5);
		List e = this.pet.worldObj.getEntitiesWithinAABB(EntityMob.class, boxToCheck);

		for (Object entity : e) {
			if (entity instanceof EntityMob) {
				EntityMob mob = (EntityMob) entity;
				if (mob instanceof EntityCreeper)
					continue;

				if (mob.getAttackTarget() instanceof EntityPlayer || mob.getAttackTarget() == this.pet) {
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
