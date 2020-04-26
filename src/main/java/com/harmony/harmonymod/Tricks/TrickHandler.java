package com.harmony.harmonymod.tricks;

import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.EntityLiving;
import java.util.Map;
import java.util.HashMap;
import java.io.Serializable;


public class TrickHandler extends EntityAIBase implements Serializable {
	private static final long serialVersionUID = 1L;
	public transient EntityLiving pet;

	public Trick currentTrick;
	public ActionSet actions;
	public Map<Integer, ActionSet> tricks;
	public transient TrickLearner isLearning;

	// These are for setting pet known locations at non-preset locations
	// the actual values are set by feeding a pet specific items
	// but knowing these locations on a note is taught like other tricks
	public double xLearned1; public double yLearned1 = -1.0; public double zLearned1;
	public double xLearned2; public double yLearned2 = -1.0; public double zLearned2;
	public double xLearned3; public double yLearned3 = -1.0; public double zLearned3;

	// Respawn on death location to be taught with a different item
	public double xRespawn; public double yRespawn = -1.0; public double zRespawn;

	public  TrickHandler(EntityLiving pet) {
		this.pet = pet;
		
		this.currentTrick = null;
		this.isLearning = null;
		this.actions = new ActionSet(); //TODO initialize better
		this.tricks = new HashMap<Integer, ActionSet>();

		registerTask();

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
		}

		updateCurrentTrick(newTrick);
	}

	/*
	 * Interface for updating current trick
	 */
	public void updateCurrentTrick(Trick newTrick) {
		if (newTrick != null) {
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
		if (!continueTrick) {
			currentTrick = null;
		}
	}

	/*
	 * Animal rewarded for current trick, make it more likely
	 *   to happen from last sound.
	 */
	public void learnTrick() {
		if (this.isLearning != null && this.isLearning.currentAttempt()) {
			ActionSet newActionSet = tricks.get(this.isLearning.noteID);
			if (newActionSet == null) {
				newActionSet = new ActionSet(this.actions);
			}

			newActionSet.learnTrick(this.isLearning.action);

			this.tricks.put(this.isLearning.noteID, newActionSet);

			this.isLearning = null;
		} else {
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


