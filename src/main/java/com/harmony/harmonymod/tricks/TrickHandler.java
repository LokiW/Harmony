package com.harmony.harmonymod.tricks;

import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.EntityLiving;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;
import com.harmony.harmonymod.sounds.Phrases;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.SortedSet;
import java.util.TreeSet;


public class TrickHandler extends EntityAIBase {
	public EntityLiving pet;

	public Trick currentTrick;
	public ActionSet actions;
	// Candidate tricks for each phrase the pet has been taught, keyed as in Phrases
	public Map<String, ActionSet> tricks;
	public TrickLearner isLearning;

	// These are for setting pet known locations at non-preset locations
	// the actual values are set by feeding a pet specific items
	// but knowing these locations on a note is taught like other tricks
	public double xLearned1; public double yLearned1 = -1.0; public double zLearned1;
	public double xLearned2; public double yLearned2 = -1.0; public double zLearned2;
	public double xLearned3; public double yLearned3 = -1.0; public double zLearned3;

	// Which place a compass sets next, 1 to 3
	public int nextPlace = 1;

	/*
	 * Set the next place for the Go to Place tricks, returning which one it was
	 */
	public int setNextPlace(double x, double y, double z) {
		int place = this.nextPlace;
		if (place == 1) {
			this.xLearned1 = x; this.yLearned1 = y; this.zLearned1 = z;
		} else if (place == 2) {
			this.xLearned2 = x; this.yLearned2 = y; this.zLearned2 = z;
		} else {
			this.xLearned3 = x; this.yLearned3 = y; this.zLearned3 = z;
		}
		this.nextPlace = place % 3 + 1;
		return place;
	}

	// Respawn on death location, set by feeding a golden apple (see respawn.Respawn)
	public double xRespawn; public double yRespawn = -1.0; public double zRespawn;
	public int dimRespawn;
	// Enchanted golden apple: the respawn point isn't used up by dying
	public boolean permanentRespawn;

	public boolean hasRespawn() {
		return this.yRespawn >= 0.0;
	}

	public void setRespawn(double x, double y, double z, int dimension) {
		this.xRespawn = x; this.yRespawn = y; this.zRespawn = z;
		this.dimRespawn = dimension;
	}

	public void clearRespawn() {
		this.yRespawn = -1.0;
		this.permanentRespawn = false;
	}

	public  TrickHandler(EntityLiving pet) {
		this.pet = pet;
		
		this.currentTrick = null;
		this.isLearning = null;
		this.actions = new ActionSet(); //TODO initialize better
		this.tricks = new HashMap<String, ActionSet>();

		this.setMutexBits(3);
	}

	/*
	 * Start running tricks as part of the pet's AI, server only
	 */
	public void registerTask() {
		//add tricks
		this.pet.tasks.addTask(0, this);
	}

	// Notes heard so far in the current phrase, see Phrases. Each step is the notes heard in one tick.
	private final List<SortedSet<Integer>> heard = new ArrayList<SortedSet<Integer>>();
	private long lastNoteTime;

	/*
	 * A note was played nearby. SoundDB calls listen() at the end of the tick to act on it.
	 */
	public void hearNote(int note) {
		long now = now();
		if (!heard.isEmpty() && lastNoteTime == now) {
			// Same tick as the last note: part of the same chord
			heard.get(heard.size() - 1).add(note);
			return;
		}
		if (heard.size() >= Phrases.MAX_STEPS) {
			finishPhrase();
		}
		SortedSet<Integer> step = new TreeSet<Integer>();
		step.add(note);
		heard.add(step);
		lastNoteTime = now;
	}

	/*
	 * Called at the end of every tick while the pet is listening. Acts on a known phrase as soon as it's
	 * heard, unless a longer phrase the pet knows starts the same way, otherwise when the phrase ends with
	 * a pause. Learning pets always wait for the pause, so they learn the whole phrase.
	 * Returns false once the pet has nothing left to listen for.
	 */
	public boolean listen() {
		if (heard.isEmpty()) {
			return false;
		}
		long now = now();
		if (now == lastNoteTime) {
			String phrase = Phrases.key(heard);
			if (!isLearningTrick() && tricks.containsKey(phrase) && !knowsLongerPhrase(phrase)) {
				heard.clear();
				updateCurrentTrick(phrase);
				return false;
			}
			return true;
		}
		if (now - lastNoteTime >= Phrases.PAUSE) {
			finishPhrase();
			return false;
		}
		return true;
	}

	private void finishPhrase() {
		String phrase = Phrases.key(heard);
		heard.clear();
		updateCurrentTrick(phrase);
	}

	private boolean knowsLongerPhrase(String phrase) {
		for (String known : tricks.keySet()) {
			if (Phrases.startsWith(known, phrase)) {
				return true;
			}
		}
		return false;
	}

	/*
	 * Act on a phrase the pet heard
	 */
	public void updateCurrentTrick(String phrase) {
		ActionSet as = tricks.get(phrase);
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
			isLearning.setAttempt(phrase, newAction, now() + REWARD_WINDOW);
		}

		updateCurrentTrick(newTrick);
	}

	/*
	 * Interface for updating current trick
	 */
	public void updateCurrentTrick(Trick newTrick) {
		if (newTrick != null) {

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

	/*
	 * End whatever the pet is doing
	 */
	public void stopCurrentTrick() {
		endCurrentTrick();
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
			ActionSet newActionSet = tricks.get(this.isLearning.phrase);
			if (newActionSet == null) {
				newActionSet = new ActionSet(this.actions);
			}

			newActionSet.learnTrick(this.isLearning.action);

			this.tricks.put(this.isLearning.phrase, newActionSet);

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
		for (Map.Entry<String, ActionSet> entry : this.tricks.entrySet()) {
			NBTTagCompound phrase = new NBTTagCompound();
			phrase.setString("Phrase", entry.getKey());
			phrase.setTag("Actions", entry.getValue().writeToNBT());
			phrases.appendTag(phrase);
		}
		tag.setTag("Phrases", phrases);

		writeLocation(tag, "Location1", this.xLearned1, this.yLearned1, this.zLearned1);
		writeLocation(tag, "Location2", this.xLearned2, this.yLearned2, this.zLearned2);
		writeLocation(tag, "Location3", this.xLearned3, this.yLearned3, this.zLearned3);
		tag.setInteger("NextPlace", this.nextPlace);
		writeLocation(tag, "Respawn", this.xRespawn, this.yRespawn, this.zRespawn);
		if (hasRespawn()) {
			tag.getCompoundTag("Respawn").setInteger("Dim", this.dimRespawn);
			tag.getCompoundTag("Respawn").setBoolean("Permanent", this.permanentRespawn);
		}

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
			if (!as.isEmpty() && !phrase.getString("Phrase").isEmpty()) {
				this.tricks.put(phrase.getString("Phrase"), as);
			}
		}

		if (tag.hasKey("NextPlace")) {
			this.nextPlace = tag.getInteger("NextPlace");
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
			this.dimRespawn = l.getInteger("Dim");
			this.permanentRespawn = l.getBoolean("Permanent");
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
		public String phrase;
		public long action;
		// World time learning mode ends at
		public long deadline;

		public TrickLearner(long deadline) {
			this.deadline = deadline;
		}

		public void setAttempt(String phrase, long action, long deadline) {
			this.phrase = phrase;
			this.action = action;
			this.deadline = deadline;
		}

		public boolean currentAttempt() {
			return phrase != null;
		}
	}
}


