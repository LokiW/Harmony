package com.harmony.harmonymod.tricks;

import net.minecraft.entity.EntityLiving;
import net.minecraft.world.World;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import java.lang.Math;
import java.util.List;
import java.util.Random;
import com.harmony.harmonymod.HarmonyProps;
import com.harmony.harmonymod.HarmonyMod;
import com.harmony.harmonymod.Traits.TRAIT;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityCow;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.stats.AchievementList;
import net.minecraft.stats.StatList;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.item.EntityXPOrb;

public class Breed extends Trick {
    private int delayCounter;
	private EntityLiving targetMate;
	private int required;
	private int spawnBabyDelay;

	public Breed(int required) {
		this.required = required;
		this.spawnBabyDelay = 0;
	}

	public void setupTrick(EntityLiving pet, Trick currentTrick) {
		this.pet = pet;

		if(currentTrick instanceof EntityTrick) {
			targetMate = ((EntityTrick)currentTrick).target;
		} else if(pet instanceof EntityAnimal) {
			targetMate = getNearbyMate();
		}

		//are we valid for mating
		if(!canBreed(pet)) {
			targetMate = null;
		}

		if(targetMate != null) {
			this.pet.worldObj.setEntityState(this.pet, (byte)18);
		}
	}

	public boolean act() {
		//is mate still valid
		if(this.targetMate == null || !canBreed(this.targetMate)) {
			this.pet.worldObj.setEntityState(this.pet, (byte)0);
			return false;
		}


		this.moveToEntity(this.targetMate, false);

		if (this.pet.getDistanceSqToEntity(this.targetMate) < 9.0D) {
			this.spawnBabyDelay++;

			if(this.spawnBabyDelay > 60) {
				if(!(this.pet instanceof EntityAnimal) || !(this.targetMate instanceof EntityAnimal)) {
					return false;
				}
				EntityAnimal e1 = (EntityAnimal)this.pet;
				EntityAnimal e2 = (EntityAnimal)this.targetMate;

				this.spawnBaby();

				HarmonyProps hp1 = HarmonyProps.get(e1);
				for(int i = 0; i < 3; i++) {
					if(hp1.traits.traits[i] == TRAIT.FERTILE) {
						this.spawnBaby();
					}
				}
				hp1.happiness -= HarmonyMod.breedingCost;
				e1.setGrowingAge(6000);

				HarmonyProps hp2 = HarmonyProps.get(e2);
				for(int i = 0; i < 3; i++) {
					if(hp2.traits.traits[i] == TRAIT.FERTILE) {
						this.spawnBaby();
					}
				}
				hp2.happiness -= HarmonyMod.breedingCost;
				e2.setGrowingAge(6000);

				this.pet.worldObj.setEntityState(this.pet, (byte)0);
				return false;
			}
		}

		return true;
	}

	public boolean isInstant() {
		return false;
	}

	public boolean consume(Trick newTrick) {
		if (newTrick instanceof Breed) {
			return true;
		} 
		return false;
	}

	private boolean canBreed(EntityLiving e) {
		HarmonyProps hp = HarmonyProps.get(e);
		boolean accum = e != null && hp != null;
		accum &= e instanceof EntityAnimal;
		accum &= !e.isDead;
		accum &= ((EntityAnimal)e).getGrowingAge() == 0;
		accum &= hp.happiness >= required;
		return accum;
	}

	/**
	 * Loops through nearby animals and finds another animal of the same type that can be mated with. Returns the closest
	 * match found.
	 */
	private EntityLiving getNearbyMate() {
		double maxDistance = 8.0F;
		World w = pet.worldObj;
		List<EntityAnimal> entities = w.getEntitiesWithinAABB(this.pet.getClass(), this.pet.boundingBox.expand(maxDistance, maxDistance, maxDistance));

		//find closest
		double closestDist = Double.MAX_VALUE;
		EntityLiving closest = null;

		for(EntityAnimal e : entities) {
			if(this.pet == e)
				continue;

			HarmonyProps hp = HarmonyProps.get(e);
			if (this.pet.getClass() == e.getClass() && canBreed(e) && this.pet.getDistanceSqToEntity(e) < closestDist) {
				closest = e;
				closestDist = this.pet.getDistanceSqToEntity(e);
			}
		}

		return closest;
	}

	/**
	 * Spawns a baby animal.
	 */
	private void spawnBaby() {
		EntityAgeable child = ((EntityAnimal)this.pet).createChild(((EntityAnimal)this.targetMate));

		if (child != null)
		{
			World w = pet.worldObj;

			activateAchievement();

			HarmonyProps.get(child).constructProperties(this.pet, this.targetMate);
			child.setGrowingAge(-24000);
			child.setLocationAndAngles(this.pet.posX, this.pet.posY, this.pet.posZ, 0.0F, 0.0F);
			w.spawnEntityInWorld(child);


			Random r = this.pet.getRNG();
			//particle effects
			for (int i = 0; i < 7; ++i)
			{
				double randX = r.nextGaussian() * 0.02D;
				double randY = r.nextGaussian() * 0.02D;
				double randZ = r.nextGaussian() * 0.02D;
				w.spawnParticle("heart", this.pet.posX + (double)(r.nextFloat() * this.pet.width * 2.0F) - (double)this.pet.width, this.pet.posY + 0.5D + (double)(r.nextFloat() * this.pet.height), this.pet.posZ + (double)(r.nextFloat() * this.pet.width * 2.0F) - (double)this.pet.width, randX, randY, randZ);
			}

			//breeding XP
			if (w.getGameRules().getGameRuleBooleanValue("doMobLoot"))
			{
				w.spawnEntityInWorld(new EntityXPOrb(w, this.pet.posX, this.pet.posY, this.pet.posZ, r.nextInt(7) + 1));
			}
		}
	}

	private void activateAchievement() {
		EntityPlayer player = ((EntityAnimal)this.pet).func_146083_cb();
		EntityAnimal temp = (EntityAnimal)this.targetMate;
		if (player == null) {
			player = temp.func_146083_cb();
		}

		if (player != null) {
			player.triggerAchievement(StatList.field_151186_x);

			if (this.pet instanceof EntityCow) {
				player.triggerAchievement(AchievementList.field_150962_H);
			}
		}
	}
}
