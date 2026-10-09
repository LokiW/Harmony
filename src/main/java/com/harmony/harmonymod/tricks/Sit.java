package com.harmony.harmonymod.tricks;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.passive.EntityTameable;

/*
 * Stay put until given another trick or Stop. Wolves and ocelots use their vanilla sitting pose,
 * other animals have no sitting animation and just stand still.
 */
public class Sit extends Trick {

	public void setupTrick(EntityLiving pet, Trick currentTrick) {
		this.pet = pet;
	}

	public boolean act() {
		this.pet.getNavigator().clearPathEntity();
		setSitting(true);
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
