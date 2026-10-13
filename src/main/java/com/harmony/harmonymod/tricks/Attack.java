package com.harmony.harmonymod.tricks;

import net.minecraft.entity.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.*;
import net.minecraft.pathfinding.*;
import java.util.*;

/*
 * Attack a nearby monster (or the entity it was told to), until there are none left close by.
 */
public class Attack extends Trick {
	// How far it looks for monsters, and how far it chases one before giving up
	private static final double SEARCH = 8.0D;
	private static final double CHASE = 16.0D;

    private int delayCounter;
	// What it's fighting. Kept here and reapplied, since vanilla's targeting AI clears or replaces the
	// pet's attack target (e.g. a wolf hunting sheep).
	private EntityLivingBase fighting;

	public void setupTrick(EntityLiving pet, Trick currentTrick) {
		this.pet = pet;
		if (currentTrick instanceof EntityTrick) {
			this.fighting = ((EntityTrick) currentTrick).getTarget();
		}
	}

	public boolean act() {
		if (--this.delayCounter <= 0) {
			this.delayCounter = 10;
			if (this.fighting == null || this.fighting.isDead || this.pet.getDistanceToEntity(this.fighting) > CHASE) {
				this.fighting = findMonster(this.pet, this.pet.posX, this.pet.posY, this.pet.posZ, SEARCH);
				if (this.fighting == null) {
					this.pet.setAttackTarget(null);
					this.pet.getNavigator().clearPathEntity();
					return false;
				}
			}
			this.pet.setAttackTarget(this.fighting);
			attackTarget();
		}
		return true;
	}

	@Override
	public void stopTrick() {
		this.pet.setAttackTarget(null);
	}

	public boolean isInstant() {
		return false;
	}

	public boolean consume(Trick newTrick) {
		if (newTrick instanceof Attack) {
			return true;
		} else if (newTrick instanceof EntityTrick) {
			EntityLiving target = ((EntityTrick) newTrick).getTarget();
			if (target != null) {
				this.fighting = target;
			}
			return true;
		}
		return false;
	}

	// Saved so the pet keeps fighting after a reload, it picks a new nearby target
	@Override
	protected String getSaveType() {
		return "attack";
	}

	/*
	 * A monster within range of a point to fight: one attacking a player or the pet first, otherwise
	 * the closest. Never creepers, which would just blow up.
	 */
	static EntityMob findMonster(EntityLiving pet, double x, double y, double z, double range) {
		AxisAlignedBB box = AxisAlignedBB.getBoundingBox(x - range, y - 3, z - range, x + range, y + 3, z + range);
		List mobs = pet.worldObj.getEntitiesWithinAABB(EntityMob.class, box);
		EntityMob closest = null;
		for (Object o : mobs) {
			EntityMob mob = (EntityMob) o;
			if (mob instanceof EntityCreeper || mob.isDead) {
				continue;
			}
			if (mob.getAttackTarget() instanceof EntityPlayer || mob.getAttackTarget() == pet) {
				return mob;
			}
			if (closest == null || pet.getDistanceSqToEntity(mob) < pet.getDistanceSqToEntity(closest)) {
				closest = mob;
			}
		}
		return closest;
	}
}
