package com.harmony.harmonymod.tricks;

import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.EntityLiving;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;
import java.util.Map;
import java.util.HashMap;


public class TrickHandler extends EntityAIBase {
	public EntityLiving pet;

	public Trick currentTrick;
	public ActionSet actions;
	public Map<Integer, ActionSet> tricks;
	public TrickLearner isLearning;

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

		this.setMutexBits(3);
	}

	/*
	 * Start running tricks as part of the pet's AI, server only
	 */
	public void registerTask() {
		//add tricks
		this.pet.tasks.addTask(0, this);
	}

	/*
	 * Interface for updating current trick
	 */
	public void updateCurrentTrick(int noteID) {
		ActionSet as = tricks.get(noteID);
		boolean learning = isLearningTrick();

		// See if we know that sound or should learn it
		if (as == null && !learning) {
			return;
		}

		// Set actions to all known actions if the sound hasn't been taught
		if (as == null) {
			as = actions;
		}

		// choose a random action from known actions. A note heard while waiting for a reward
		// means "no, not that one", so a learning pet tries something other than its last attempt.
		long newAction;
		if (learning && isLearning.currentAttempt()) {
			newAction = as.getActionExcept(pet, isLearning.action);
		} else {
			newAction = as.getAction(pet);
		}
		Trick newTrick = as.convertRawAction(newAction, pet);
	
		if (learning) {
			isLearning.setAttempt(noteID, newAction, now() + REWARD_WINDOW);
		}

		updateCurrentTrick(newTrick);
	}

	/*
	 * Interface for updating current trick
	 */
	public void updateCurrentTrick(Trick newTrick) {
		if (newTrick != null) {
			System.out.println("HarmonyMod: " + pet + " doing trick " + newTrick);
			if (newTrick instanceof Stop) {
				endCurrentTrick();
				pet.getNavigator().clearPathEntity();
				return;
			}
			newTrick.setupTrick(pet, currentTrick);
			if (newTrick.isInstant()) {
				newTrick.act();
			} else if (currentTrick == null || !currentTrick.consume(newTrick)) {
				endCurrentTrick();
				currentTrick = newTrick;
			}
		}
	}

	private void endCurrentTrick() {
		if (currentTrick != null) {
			currentTrick.stopTrick();
			currentTrick = null;
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
			endCurrentTrick();
		}
	}

	// Whether the pet avoided water before a trick started, see startExecuting
	private boolean avoidedWater;

	/*
	 * Animals normally path around water. While doing tricks they go through it (swimming), so
	 * water doesn't stop them reaching where they were told to go.
	 */
	@Override
	public void startExecuting() {
		avoidedWater = pet.getNavigator().getAvoidsWater();
		pet.getNavigator().setAvoidsWater(false);
	}

	@Override
	public void resetTask() {
		pet.getNavigator().setAvoidsWater(avoidedWater);
	}

	/*
	 * Animal rewarded for current trick, make it more likely
	 *   to happen from last sound.
	 */
	// Learning mode ends after this long without hearing a note
	private static final int LEARNING_TIME = 60 * 20;
	// After trying a trick, how long the player has to reward it (or play another note) before learning mode ends
	private static final int REWARD_WINDOW = 7 * 20;

	private long now() {
		return this.pet.worldObj.getTotalWorldTime();
	}

	/*
	 * Whether the pet is in learning mode: fed the training item and waiting for a note and a reward.
	 * Ends learning mode once it has timed out.
	 */
	public boolean isLearningTrick() {
		if (this.isLearning != null && now() > this.isLearning.deadline) {
			this.isLearning = null;
		}
		return this.isLearning != null;
	}

	/*
	 * Fed the training item. Returns true if that rewarded an attempt and the pet learned from it.
	 */
	public boolean learnTrick() {
		if (isLearningTrick() && this.isLearning.currentAttempt()) {
			ActionSet newActionSet = tricks.get(this.isLearning.noteID);
			if (newActionSet == null) {
				newActionSet = new ActionSet(this.actions);
			}

			newActionSet.learnTrick(this.isLearning.action);

			this.tricks.put(this.isLearning.noteID, newActionSet);

			this.isLearning = null;
			return true;
		} else {
			this.isLearning = new TrickLearner(now() + LEARNING_TIME);
			return false;
		}
	}

	public void writeToNBT(NBTTagCompound tag) {
		tag.setTag("Actions", this.actions.writeToNBT());

		NBTTagList phrases = new NBTTagList();
		for (Map.Entry<Integer, ActionSet> entry : this.tricks.entrySet()) {
			NBTTagCompound phrase = new NBTTagCompound();
			phrase.setInteger("Note", entry.getKey());
			phrase.setTag("Actions", entry.getValue().writeToNBT());
			phrases.appendTag(phrase);
		}
		tag.setTag("Phrases", phrases);

		writeLocation(tag, "Location1", this.xLearned1, this.yLearned1, this.zLearned1);
		writeLocation(tag, "Location2", this.xLearned2, this.yLearned2, this.zLearned2);
		writeLocation(tag, "Location3", this.xLearned3, this.yLearned3, this.zLearned3);
		writeLocation(tag, "Respawn", this.xRespawn, this.yRespawn, this.zRespawn);

		NBTTagCompound current = Trick.saveTrick(this.currentTrick);
		if (current != null) {
			tag.setTag("Current", current);
		}
	}

	public void readFromNBT(NBTTagCompound tag) {
		this.actions = ActionSet.readFromNBT(tag.getTagList("Actions", Constants.NBT.TAG_STRING));
		if (this.actions.isEmpty()) {
			this.actions = new ActionSet();
		}

		this.tricks.clear();
		NBTTagList phrases = tag.getTagList("Phrases", Constants.NBT.TAG_COMPOUND);
		for (int i = 0; i < phrases.tagCount(); i++) {
			NBTTagCompound phrase = phrases.getCompoundTagAt(i);
			ActionSet as = ActionSet.readFromNBT(phrase.getTagList("Actions", Constants.NBT.TAG_STRING));
			if (!as.isEmpty()) {
				this.tricks.put(phrase.getInteger("Note"), as);
			}
		}

		if (tag.hasKey("Location1")) {
			NBTTagCompound l = tag.getCompoundTag("Location1");
			this.xLearned1 = l.getDouble("X"); this.yLearned1 = l.getDouble("Y"); this.zLearned1 = l.getDouble("Z");
		}
		if (tag.hasKey("Location2")) {
			NBTTagCompound l = tag.getCompoundTag("Location2");
			this.xLearned2 = l.getDouble("X"); this.yLearned2 = l.getDouble("Y"); this.zLearned2 = l.getDouble("Z");
		}
		if (tag.hasKey("Location3")) {
			NBTTagCompound l = tag.getCompoundTag("Location3");
			this.xLearned3 = l.getDouble("X"); this.yLearned3 = l.getDouble("Y"); this.zLearned3 = l.getDouble("Z");
		}
		if (tag.hasKey("Respawn")) {
			NBTTagCompound l = tag.getCompoundTag("Respawn");
			this.xRespawn = l.getDouble("X"); this.yRespawn = l.getDouble("Y"); this.zRespawn = l.getDouble("Z");
		}

		if (tag.hasKey("Current")) {
			this.currentTrick = Trick.loadTrick(tag.getCompoundTag("Current"), this.pet);
		}
	}

	/*
	 * Locations are unset while their y is negative
	 */
	private static void writeLocation(NBTTagCompound tag, String key, double x, double y, double z) {
		if (y < 0.0) {
			return;
		}
		NBTTagCompound l = new NBTTagCompound();
		l.setDouble("X", x);
		l.setDouble("Y", y);
		l.setDouble("Z", z);
		tag.setTag(key, l);
	}



	/*
	 * Learning struct, which stores last heard sound and action tried
	 *  to that sound. If animal is not primed to learn they will have a
	 *  null value for their TrickLearner.
	 */
	private static class TrickLearner {
		public int noteID = -1;
		public long action;
		// World time learning mode ends at
		public long deadline;

		public TrickLearner(long deadline) {
			this.deadline = deadline;
		}

		public void setAttempt(int noteID, long action, long deadline) {
			this.noteID = noteID;
			this.action = action;
			this.deadline = deadline;
		}

		public boolean currentAttempt() {
			return noteID > -1;
		}
	}
}


