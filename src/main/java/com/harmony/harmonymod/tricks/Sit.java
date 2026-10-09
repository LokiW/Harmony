package com.harmony.harmonymod.tricks;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.passive.EntityWolf;

/*
 * Stay put until given another trick or Stop. Wolves and ocelots use their vanilla sitting pose,
 * other animals have no sitting animation and just stand still.
 * A wolf's owner can also stand it up by right clicking it, like vanilla's own sit.
 */
public class Sit extends Trick {
	private boolean sat;

	public void setupTrick(EntityLiving pet, Trick currentTrick) {
		this.pet = pet;
	}

	public boolean act() {
		if (this.pet instanceof EntityWolf) {
			// Sat down once, then left to vanilla's sit AI, so the owner's right click can stand it up
			if (!this.sat) {
				setSitting(true);
				this.sat = true;
			} else if (!((EntityWolf) this.pet).isSitting()) {
				return false;
			}
		} else {
			setSitting(true);
		}
		this.pet.getNavigator().clearPathEntity();
		return true;
	}

	public boolean isInstant() {
		return false;
	}

	public boolean consume(Trick newTrick) {
		return newTrick instanceof Sit;
	}

	@Override
	public void stopTrick() {
		setSitting(false);
	}

	private void setSitting(boolean sitting) {
		if (this.pet instanceof EntityTameable) {
			EntityTameable tameable = (EntityTameable) this.pet;
			// The AI task keeps a tamed animal sat, the flag is what shows the pose
			tameable.func_70907_r().setSitting(sitting);
			tameable.setSitting(sitting);
		}
	}

	@Override
	protected String getSaveType() {
		return "sit";
	}
}
