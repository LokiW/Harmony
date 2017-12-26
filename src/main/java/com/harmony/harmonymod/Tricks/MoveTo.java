package com.harmony.harmonymod.tricks;

import net.minecraft.entity.*;
import net.minecraft.world.*;
import net.minecraft.pathfinding.*;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import java.lang.Math;
import com.harmony.harmonymod.HarmonyProps;
import com.harmony.harmonymod.sounds.SoundDB.InstrumentSound;

public class MoveTo implements Trick {
    private EntityCreature pet;
	private Entity target;
    private int delayCounter;
    private double targetX;
    private double targetY;
    private double targetZ;
    private boolean teleport;
	private boolean pointSet;

    public MoveTo() {
		this.teleport = false;
		this.pointSet = false;
    }

	public void setupTrick(EntityCreature pet, Object firstSound, Object secondSound) {
		this.pet = pet;

		if(firstSound instanceof Entity) {
			target = (Entity) firstSound;
			this.teleport = true;
		} else if (firstSound instanceof InstrumentSound) {
			ActionSet lastAction = HarmonyProps.get(this.pet).tricks.get((InstrumentSound) firstSound);
			if (lastAction == null) {
				System.out.println("HarmonyMod: Didn't get location for GoTo Trick");
				return;
			}
			Trick possibleLocationTrick = lastAction.getTrick(this.pet);
			if (possibleLocationTrick != null && possibleLocationTrick instanceof LocationTrick) {
				System.out.println("HarmonyMod: Found location for GoTo Trick");
				LocationTrick location = (LocationTrick) possibleLocationTrick;
				this.targetX = location.targetX;
				this.targetY = location.targetY;
				this.targetZ = location.targetZ;
				this.teleport = location.teleport;
				this.pointSet = true;
			}
		} else {
			System.out.println("HarmonyMod: no location for GoTo Trick");
		}
	}

	public boolean act() {
		if (--this.delayCounter <= 0) {
			this.delayCounter = 10;
			if (target != null) {
				System.out.println("HarmonyMod: GoTo task to follow entity");
				return moveToEntity();
			} else if (pointSet) {
				System.out.println("HarmonyMod: GoTo task to go to point.");
				return moveToPoint();
			} else {
				System.out.println("HarmonyMod: GoTo to freak out and teleport.");
				return randomTeleport();
			}
		}
		return true;
	}
	
	private boolean moveToEntity() {
		this.pet.getLookHelper().setLookPositionWithEntity(this.target, 10.0F, (float)this.pet.getVerticalFaceSpeed());

		PathNavigate petPathfinder = this.pet.getNavigator();

		if (!petPathfinder.tryMoveToEntityLiving(this.target, 1.0)) {
			System.out.println("fail to trymove");

			// Teleport to location near owner if too far away
			if (this.teleport) {
				if (this.pet.getDistanceSqToEntity(this.target) >= 144.0D) {
					teleportHelper(this.target.posX, this.target.boundingBox.minY, this.target.posZ);
				}
			}
		}
		return true;
	}
	
	private boolean moveToPoint() {
       this.pet.getLookHelper().setLookPosition(this.targetX, this.targetY, this.targetZ, 20.0F, (float)this.pet.getVerticalFaceSpeed());
		
		PathNavigate petPathfinder = pet.getNavigator();

		double tempTargetX;
		double tempTargetY;
		double tempTargetZ;

		if(target != null) {
			tempTargetX = target.posX;
			tempTargetY = target.posY;
			tempTargetZ = target.posZ;
		} else {
			tempTargetX = targetX;
			tempTargetY = targetY;
			tempTargetZ = targetZ;
		}

		// Can't path more than 10 blocks away, reduce path down if can't teleport
		if (!this.teleport) {
			double x_divisor = Math.abs((this.targetX - this.pet.posX) / 8.0);
			double y_divisor = Math.abs((this.targetY - this.pet.boundingBox.minY) / 8.0);
			double z_divisor = Math.abs((this.targetZ - this.pet.posZ) / 8.0);

			double divisor = Math.max(Math.max(x_divisor, y_divisor), z_divisor);
			if (divisor > 1.0) {
				tempTargetX = (this.targetX - this.pet.posX) / divisor + this.pet.posX;
				tempTargetY = (this.targetY - this.pet.boundingBox.minY) / divisor + this.pet.boundingBox.minY;
				tempTargetZ = (this.targetZ - this.pet.posZ) / divisor + this.pet.posZ;
			} 

		}

		// x, y, z, speed multiplier of entity's speed attribute
		if (!petPathfinder.tryMoveToXYZ(tempTargetX, tempTargetY, tempTargetZ, 1.0)) {

			// Teleport to location near owner if too far away
			if (this.teleport) {
				if (this.pet.getDistanceSq(this.targetX, this.targetY, this.targetZ) >= 144.0D) {
					return teleportHelper(this.targetX, this.targetY, this.targetZ);					
				}
			}
        }

		if (this.pet.getDistanceSq(this.targetX, this.targetY, this.targetZ) <= 2.0D)
			return false;

		return true;
    }

	private boolean randomTeleport() {
		// TODO don't always teleport south
		teleportHelper(this.pet.posX - 2, this.pet.posY, this.pet.posZ);
		return true;
	}

	private boolean teleportHelper(double x, double y, double z) {
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
