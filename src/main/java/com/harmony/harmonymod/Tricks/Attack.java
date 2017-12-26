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

public class Attack implements Trick {
    private EntityCreature pet;
	//private Entity target;
	private boolean targetSpecified;
    private int delayCounter;

    public Attack() {
		this.targetSpecified = false;
    }

	public void setupTrick(EntityCreature pet, Object firstSound, Object secondSound) {
		this.pet = pet;

		if(firstSound instanceof EntityLivingBase) {
			this.pet.setAttackTarget((EntityLivingBase) firstSound);
			// target = (Entity) firstSound;
			this.targetSpecified = true;
		}
	}

	public boolean act() {
		if (--this.delayCounter <= 0) {
			this.delayCounter = 10;
			if (!this.targetSpecified && (this.pet.getAttackTarget() == null)) {
				// Find target to attack
				setNewAttackTarget();
			}
			attackTarget();
			// check if specified target killed, if so stop trying to attack things
			if (this.pet.getAttackTarge() != null && this.pet.getAttackTarget().isDead)
				return false;
		}
		return true;
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

	private void attackTarget() {
		EntityLivingBase target = (EntityLivingBase) this.pet.getAttackTarget();
		if (target == null)
			return;

		this.pet.getLookHelper().setLookPositionWithEntity(target, 10.0F, (float)this.pet.getVerticalFaceSpeed());

		PathNavigate petPathfinder = this.pet.getNavigator();

		petPathfinder.tryMoveToEntityLiving(target, 1.0);
		if (this.pet.getDistanceSqToEntity(target) <= 5.0D) {
			// Attack it
			IAttributeInstance damageAttr = this.pet.getEntityAttribute(SharedMonsterAttributes.attackDamage);
			float damage = 0;
			if (damageAttr != null) {
				damage = (float) damageAttr.getAttributeValue();
			} else {
				System.out.println("HarmonyMod: Couldn't find attackAttribute for animal D: ");
			}

			target.attackEntityFrom(DamageSource.causeMobDamage(this.pet), damage);
		}
	}
}
