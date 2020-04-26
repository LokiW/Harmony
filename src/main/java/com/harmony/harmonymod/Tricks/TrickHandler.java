package com.harmony.harmonymod.tricks;

import com.harmony.harmonymod.HarmonyProps;
import com.harmony.harmonymod.aitasks.BreedingAI;
import com.harmony.harmonymod.Traits.TRAIT;
import net.minecraft.entity.ai.*;
import net.minecraft.entity.ai.EntityAITasks.EntityAITaskEntry;
import net.minecraft.entity.ai.attributes.*;
import net.minecraft.entity.*;
import net.minecraft.entity.passive.*;
import net.minecraft.world.*;
import net.minecraft.pathfinding.*;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import java.util.*;
import java.io.Serializable;

//TODO remove with test AI behaviours
import net.minecraftforge.event.world.NoteBlockEvent.Instrument;
import com.harmony.harmonymod.sounds.SoundDB;

public class TrickHandler extends EntityAIBase implements Serializable {
	private static final long serialVersionUID = 1L;
	public transient EntityLiving pet;

	public Trick currentTrick;
	public ActionSet actions;
	public Map<Integer, ActionSet> tricks;
	public transient TrickLearner isLearning;

	// These are for setting pet known locations at non-preset locations
	// like other actions these are taught with specific items
	public double xLearned1; public double yLearned1; public double zLearned1;
	public double xLearned2; public double yLearned2; public double zLearned2;
	public double xLearned3; public double yLearned3; public double zLearned3;

	public  TrickHandler(EntityLiving pet) {
		this.pet = pet;
		
		this.currentTrick = null;
		this.isLearning = null;
		this.actions = new ActionSet(); //TODO initialize better
		this.tricks = new HashMap<Integer, ActionSet>();

		registerTask();

		//TODO remove test AI behaviours
		SoundDB soundDB = SoundDB.getSoundDB();	
    	if (pet instanceof EntityCow) {
			ActionSet t1 = new ActionSet(TrickEnum.GO);
			tricks.put(soundDB.getSound(Instrument.BASSDRUM, 3), t1);
			ActionSet t2 = new ActionSet(TrickEnum.LEARNED_LOCATION_1);
			tricks.put(soundDB.getSound(Instrument.BASSDRUM, 6), t2);
			ActionSet t3 = new ActionSet(TrickEnum.ATTACK);
			tricks.put(soundDB.getSound(Instrument.BASSDRUM, 0), t3);

	   	} else if (pet instanceof EntitySheep) {
			ActionSet t1 = new ActionSet(TrickEnum.GUARD);
			tricks.put(soundDB.getSound(Instrument.PIANO, 0), t1);
			ActionSet t2 = new ActionSet(TrickEnum.JUMP);
			tricks.put(soundDB.getSound(Instrument.PIANO, 3), t2);
		} else if (pet instanceof EntityChicken) {
			ActionSet t1 = new ActionSet(TrickEnum.JUMP);
			tricks.put(soundDB.getSound(Instrument.PIANO, 0), t1);
		}

		this.setMutexBits(3);
	}

	public void registerTask() {
		//add tricks
		this.pet.tasks.addTask(0, this);
	}

	/*
	 * Interface for updating current trick
	 */
	public void updateCurrentTrick(int noteID) {
		System.out.println("HarmonyMod: Updating trick for noteID: " + noteID);
		ActionSet as = tricks.get(noteID);

		// See if we know that sound or should learn it
		if (as == null && isLearning == null) {
			return;
		}

		// Set actions to all known actions if the sound hasn't been taught
		if (as == null) {
			as = actions;
		}

		// choose a random action from known actions
		long newAction = as.getAction(pet);
		Trick newTrick = as.convertRawAction(newAction, pet);
	
		if (isLearning != null) {
			isLearning.setAttempt(noteID, newAction);
			System.out.println("HarmonyMod: updated isLearning. Currently thinks learning " + isLearning.currentAttempt());
		}

		updateCurrentTrick(newTrick);
	}

	/*
	 * Interface for updating current trick
	 */
	public void updateCurrentTrick(Trick newTrick) {
		if (newTrick != null) {
			//TODO remove debug statement
			System.out.println("HarmonyMod: " + pet + " doing trick " + newTrick);
			newTrick.setupTrick(pet, currentTrick);
			if (newTrick.isInstant()) {
				newTrick.act();
			} else if (currentTrick == null || !currentTrick.consume(newTrick)) {
				currentTrick = newTrick;
			}
		}
	}


	/*
	 * Returns whether the TrickHandler wishes to execute
	 */
	@Override
	public  boolean shouldExecute() {
		return currentTrick != null;
	}

	/*
	 * Returns whether the Trickhandler wishes to execute
	 */
	@Override
	public boolean continueExecuting() {
		return currentTrick != null;
	}

	/*
	 * Runs Once every game tick this task is active
	 */
	@Override
	public void updateTask() {
		boolean continueTrick = currentTrick.act();
		if (!continueTrick)
			currentTrick = null;
	}

	/*
	 * Animal rewarded for current trick, make it more likely
	 *   to happen from last sound.
	 */
	public void learnTrick() {
		System.out.println("HarmonyMod: Animal is learning " + this.isLearning);
		if (this.isLearning != null && this.isLearning.currentAttempt()) {
			System.out.println("HarmonyMod: Rewarded for action " + actions.getTrickNameForAction(this.isLearning.action));
			ActionSet newActionSet = tricks.get(this.isLearning.noteID);
			if (newActionSet == null) {
				newActionSet = new ActionSet(this.actions);
			}

			newActionSet.learnTrick(this.isLearning.action);

			this.tricks.put(this.isLearning.noteID, newActionSet);

			this.isLearning = null;
		} else {
			System.out.println("HarmonyMod: Will try to Learn a Trick");
			this.isLearning = new TrickLearner();
		}	
	}

	/*
	 * Unused required overrides
	 */
	@Override
	public void resetTask() {}
	@Override
	public void startExecuting() {}

	/*
	 * Learning struct, which stores last heard sound and action tried
	 *  to that sound. If animal is not primed to learn they will have a
	 *  null value for their TrickLearner.
	 */
	private static class TrickLearner {
		public int noteID = -1;
		public long action;

		public void setAttempt(int noteID, long action) {
			this.noteID = noteID;
			this.action = action;		
		}

		public boolean currentAttempt() {
			return noteID > -1;
		}
	}
}


