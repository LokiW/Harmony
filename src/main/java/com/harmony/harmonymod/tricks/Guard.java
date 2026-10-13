package com.harmony.harmonymod.tricks;

import net.minecraft.entity.*;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.*;
import net.minecraft.pathfinding.*;
import java.util.*;
import net.minecraft.nbt.NBTTagCompound;

public class Guard extends Trick {
    private int delayCounter;
	private Trick guardTarget;
	private EntityLivingBase fighting;

	public void setupTrick(EntityLiving pet, Trick currentTrick) {
		this.pet = pet;
		if (currentTrick instanceof MoveTo && ((MoveTo) currentTrick).targetTrick instanceof LocationTrick) {
			// Told to guard after going to a place: guard that place
			this.guardTarget = ((MoveTo) currentTrick).targetTrick;
		} else if (currentTrick instanceof LocationTrick || currentTrick instanceof EntityTrick) {
			this.guardTarget = currentTrick;	
		} else {
			this.guardTarget = new LocationTrick(this.pet.posX, this.pet.posY, this.pet.posZ);
		}
	}

	public boolean act() {
		if (--this.delayCounter <= 0) {
			this.delayCounter = 10;

			if (this.guardTarget instanceof EntityTrick && ((EntityTrick) this.guardTarget).getTarget() == null) {
				// Whoever we were guarding is gone, guard this spot instead
				this.guardTarget = new LocationTrick(this.pet.posX, this.pet.posY, this.pet.posZ);
			}

			// What it's fighting is kept here and reapplied, since vanilla's targeting AI clears or replaces
			// the pet's attack target (e.g. a wolf hunting sheep). Monsters only, within LEASH of its post.
			if (this.fighting != null && (this.fighting.isDead || distanceFromPost(this.fighting) > LEASH)) {
				this.fighting = null;
			}

			if (this.fighting == null) {
				this.pet.setAttackTarget(null);
				// Back to its post, then look for monsters around it
				boolean atPost;
				double[] post = postPosition();
				if (this.guardTarget instanceof LocationTrick) {
					moveToPoint(post[0], post[1], post[2], false);
				} else {
					moveToEntity(((EntityTrick) this.guardTarget).getTarget(), true);
				}
				atPost = this.pet.getDistanceSq(post[0], post[1], post[2]) <= 10.0D;
				if (atPost) {
					this.fighting = Attack.findMonster(this.pet, post[0], post[1], post[2], SEARCH);
				}
			}
			if (this.fighting != null) {
				this.pet.setAttackTarget(this.fighting);
				attackTarget();
			}
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

	// How far from its post a guard chases a monster, and looks for them
	private static final double LEASH = 12.0D;
	private static final double SEARCH = 8.0D;

	private double distanceFromPost(Entity entity) {
		if (this.guardTarget instanceof EntityTrick) {
			EntityLiving guarded = ((EntityTrick) this.guardTarget).getTarget();
			return guarded == null ? 0.0D : entity.getDistanceToEntity(guarded);
		}
		LocationTrick post = (LocationTrick) this.guardTarget;
		return entity.getDistance(post.targetX, post.targetY, post.targetZ);
	}

	private double[] postPosition() {
		if (this.guardTarget instanceof EntityTrick) {
			EntityLiving guarded = ((EntityTrick) this.guardTarget).getTarget();
			return new double[] {guarded.posX, guarded.posY, guarded.posZ};
		}
		LocationTrick post = (LocationTrick) this.guardTarget;
		return new double[] {post.targetX, post.targetY, post.targetZ};
	}

	@Override
	public void stopTrick() {
		this.pet.setAttackTarget(null);
	}

	@Override
	protected String getSaveType() {
		return "guard";
	}

	@Override
	protected void writeToNBT(NBTTagCompound tag) {
		NBTTagCompound target = Trick.saveTrick(this.guardTarget);
		if (target != null) {
			tag.setTag("Target", target);
		}
	}

	@Override
	protected void readFromNBT(NBTTagCompound tag) {
		if (tag.hasKey("Target")) {
			this.guardTarget = Trick.loadTrick(tag.getCompoundTag("Target"), this.pet);
		}
		if (this.guardTarget == null) {
			this.guardTarget = new LocationTrick(this.pet.posX, this.pet.posY, this.pet.posZ);
		}
	}
}
