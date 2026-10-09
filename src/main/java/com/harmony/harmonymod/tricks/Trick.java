package com.harmony.harmonymod.tricks;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.pathfinding.PathNavigate;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.util.DamageSource;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.nbt.NBTTagCompound;

public abstract class Trick {
	public EntityLiving pet;
	protected static final int LEARNING_DELAY = 100;

	/*
	 * Call to have entity perform trick.
	 * Returns True if should be called again,
	 *         False if action is over.
	 */
	public abstract boolean act();


	/*
	 * Should be called immediately after construction, before act()
	 */
	public abstract void setupTrick(EntityLiving entity, Trick currentTrick);


	/*
	 * Takes in a trick and returns true if it can use the trick's information,
	 * otherwise false
	 */
	public abstract boolean consume(Trick newTrick);


	/*
	 * Returns true if the trick's act only needs to be called once.
	 */
	public abstract boolean isInstant();


	/*
	 * Called when the trick ends or another trick replaces it, to undo anything it set up
	 */
	public void stopTrick() {}


	/*
	 * Name this trick is saved under while it's the pet's current trick, or null if it isn't
	 * worth keeping across a reload. Also add new types to loadTrick. Never rename a saved type.
	 */
	protected String getSaveType() {
		return null;
	}

	/*
	 * Save and load any state the trick needs to carry on after a reload
	 */
	protected void writeToNBT(NBTTagCompound tag) {}
	protected void readFromNBT(NBTTagCompound tag) {}

	/*
	 * Save a trick, or null if it isn't saved
	 */
	public static NBTTagCompound saveTrick(Trick trick) {
		if (trick == null || trick.getSaveType() == null) {
			return null;
		}
		NBTTagCompound tag = new NBTTagCompound();
		tag.setString("Type", trick.getSaveType());
		trick.writeToNBT(tag);
		return tag;
	}

	/*
	 * Load a trick saved with saveTrick, or null if it can't be
	 */
	public static Trick loadTrick(NBTTagCompound tag, EntityLiving pet) {
		String type = tag.getString("Type");
		Trick trick;
		if ("guard".equals(type)) {
			trick = new Guard();
		} else if ("move_to".equals(type)) {
			trick = new MoveTo();
		} else if ("attack".equals(type)) {
			trick = new Attack();
		} else if ("location".equals(type)) {
			trick = new LocationTrick();
		} else if ("entity".equals(type)) {
			trick = new EntityTrick();
		} else if ("sit".equals(type)) {
			trick = new Sit();
		} else {
			System.out.println("HarmonyMod: Ignoring unknown saved trick type " + type);
			return null;
		}
		trick.pet = pet;
		trick.readFromNBT(tag);
		return trick;
	}


	/*
	 * Entity attacks target that is set as it's attack target.
	 * 	Uses SharedMonsterAttributes.attackDamage for damage + any relevant traits.
	 * 	TODO use relevant traits.
	 */
	protected void attackTarget() {
		EntityLivingBase target = (EntityLivingBase) this.pet.getAttackTarget();
		if (target == null || target.isDead) {
			return;
		}
		
		moveToEntity(target, false);

		if (this.pet.getDistanceSqToEntity(target) <= 5.0D) {
			// Attack it
			IAttributeInstance damageAttr = this.pet.getEntityAttribute(SharedMonsterAttributes.attackDamage);
			float damage = 0;
			if (damageAttr != null) {
				damage = (float) damageAttr.getAttributeValue();
			} else {
				System.out.println("HarmonyMod: " + this.pet + " has no attack damage attribute, can't attack");
			}

			target.attackEntityFrom(DamageSource.causeMobDamage(this.pet), damage);
		}
	}
	
	/*
	 * Moves to the given entity as best as it can. If it cannot, will teleport if allowed.
	 */
	protected boolean moveToEntity(Entity target, boolean teleport) {
		this.pet.getLookHelper().setLookPositionWithEntity(target, 10.0F, (float)this.pet.getVerticalFaceSpeed());

		PathNavigate petPathfinder = this.pet.getNavigator();

		if (!petPathfinder.tryMoveToEntityLiving(target, 1.0)) {
			// Teleport to location near owner if too far away
			if (teleport) {
				if (this.pet.getDistanceSqToEntity(target) >= 144.0D) {
					teleportHelper(target.posX, target.boundingBox.minY, target.posZ);
				}
			}
		}
		return true;
	}
	
	/*
	 * Moves to the given point as best it can. If it cannot, will teleport if allowed.
	 */
	protected boolean moveToPoint(double targetX, double targetY, double targetZ, boolean teleport) {
	   this.pet.getLookHelper().setLookPosition(targetX, targetY, targetZ, 20.0F, (float)this.pet.getVerticalFaceSpeed());
		
		PathNavigate petPathfinder = pet.getNavigator();

		double tempTargetX = targetX;
		double tempTargetY = targetY;
		double tempTargetZ = targetZ;

		// Can't path more than 10 blocks away, reduce path down if can't teleport
		if (!teleport) {
			double xDivisor = Math.abs((targetX - this.pet.posX) / 8.0);
			double yDivisor = Math.abs((targetY - this.pet.boundingBox.minY) / 8.0);
			double zDivisor = Math.abs((targetZ - this.pet.posZ) / 8.0);

			double divisor = Math.max(Math.max(xDivisor, yDivisor), zDivisor);
			if (divisor > 1.0) {
				tempTargetX = (targetX - this.pet.posX) / divisor + this.pet.posX;
				tempTargetY = (targetY - this.pet.boundingBox.minY) / divisor + this.pet.boundingBox.minY;
				tempTargetZ = (targetZ - this.pet.posZ) / divisor + this.pet.posZ;
			} 

		}

		// x, y, z, speed multiplier of entity's speed attribute
		if (!petPathfinder.tryMoveToXYZ(tempTargetX, tempTargetY, tempTargetZ, 1.0)) {

			// Teleport to location near owner if too far away
			if (teleport) {
				if (this.pet.getDistanceSq(targetX, targetY, targetZ) >= 144.0D) {
					return teleportHelper(targetX, targetY, targetZ);					
				}
			}
		}

		if (this.pet.getDistanceSq(targetX, targetY, targetZ) <= 2.0D) {
			petPathfinder.clearPathEntity();
			return false;
		}

		return true;
	}


	/*
	 * Teleport to given point.
	 */
	protected boolean teleportHelper(double x, double y, double z) {
		int i = MathHelper.floor_double(x) - 2;
		int j = MathHelper.floor_double(y) - 2;
		int k = MathHelper.floor_double(z);

		World theWorld = pet.worldObj;

		for (int l = 0; l <= 4; ++l) {
			for (int i1 = 0; i1 <= 4; ++i1) {
				if ((l < 1 || i1 < 1 || l > 3 || i1 > 3)
						&& theWorld.doesBlockHaveSolidTopSurface(theWorld, i + l, k - 1, j + i1)
						&& !theWorld.getBlock(i + l, k, j + i1).isNormalCube()
						&& !theWorld.getBlock(i + l, k + 1, j + i1).isNormalCube()) {
					this.pet.setLocationAndAngles(
						(double)((float)(i + l) + 0.5F),
						(double)k,
						(double)((float)(j + i1) + 0.5F),
						this.pet.rotationYaw,
						this.pet.rotationPitch);
					return false;
				}
			}
		}
		return true;
	}
}
