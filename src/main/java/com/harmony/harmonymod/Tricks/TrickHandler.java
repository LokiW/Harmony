package com.harmony.harmonymod.tricks;

import com.harmony.harmonymod.sounds.SoundDB;
import com.harmony.harmonymod.sounds.SoundDB.InstrumentSound;
import com.harmony.harmonymod.HarmonyProps;
import net.minecraft.entity.ai.*;
import net.minecraft.entity.ai.attributes.*;
import net.minecraft.entity.*;
import net.minecraft.entity.passive.*;
import net.minecraft.world.*;
import net.minecraft.pathfinding.*;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import java.lang.Math;
import java.util.*;

public class TrickHandler extends EntityAIBase
{
	private EntityCreature pet;

	// InstrumentSound pointer or pointer to Entity
	// last two sounds processed by TrickHandler
	private Object firstSound;
	private Object secondSound;

	private Trick currentTrick;

	public  TrickHandler(EntityCreature pet) {
		this.pet = pet;
		this.setMutexBits(3);
	}

	private void updateCurrentTrick() {
		Object sound = SoundDB.getSoundDB().getLastHeard(pet);
		if(sound != null) {
			System.out.println("HarmonyMod: heard sound " + sound);
			if(sound instanceof InstrumentSound) {
				HarmonyProps hp = HarmonyProps.get(pet);
				ActionSet as = hp.tricks.get((InstrumentSound)sound);
	
				if(as != null) {
					System.out.println("HarmonyProps: ActionSet found for heard sound");
					currentTrick = as.getTrick(pet);
				} else {
					//TODO try something new when unknown sound
				}
				if (currentTrick != null)
					currentTrick.setupTrick(pet, firstSound, secondSound);
			}

			secondSound = firstSound;
			firstSound = sound;
		}
	}


	/**
	 * Returns whether the EntityAIBase should begin execution.
	 */
	@Override
	public  boolean shouldExecute() {
		//TODO check happiness level
		updateCurrentTrick();
		return currentTrick != null;
	}

	/**
	 * Returns whether an in-progress EntityAIBase should continue executing
	 */
	@Override
	public  boolean continueExecuting() {
		return currentTrick != null;
	}

	/**
	 * Execute a one shot task or start executing a continuous task
	 */
	@Override
	public  void startExecuting() {
	}

	/**
	 * Resets the task
	 */
	@Override
	public  void resetTask() {
	}

	/**
	 * Updates the task
	 */
	@Override
	public  void updateTask() {
		updateCurrentTrick();
		boolean continueTrick = currentTrick.act();
		if (!continueTrick)
			currentTrick = null;
	}
}
