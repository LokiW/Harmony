package com.harmony.harmonymod.tricks;

import com.harmony.harmonymod.HarmonyProps;
import com.harmony.harmonymod.BreedingAI;
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

	// These are for setting pet known locations at non-preset locations
	// like other actions these are taught with specific items
	public double xLearned1; public double yLearned1; public double zLearned1;
	public double xLearned2; public double yLearned2; public double zLearned2;
	public double xLearned3; public double yLearned3; public double zLearned3;

	public  TrickHandler(EntityLiving pet) {
		this.pet = pet;
		
		this.currentTrick = null;
		this.actions = new ActionSet(0); //TODO initialize better
		this.tricks = new HashMap<Integer, ActionSet>();

		registerAI();

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
		}
 

		this.setMutexBits(3);
	}

	public void registerAI() {
		//replace breeding task
		boolean canBreed = false;
		List<EntityAITaskEntry> t = pet.tasks.taskEntries;
		for(int i = 0; i < t.size(); i++) {
			if(t.get(i).action instanceof EntityAIMate) {
				t.remove(i);
				canBreed = true;
				i--;
			}
		}
		if(canBreed) {
			pet.tasks.addTask(1,new BreedingAI((EntityAnimal)pet));
		}

		//add tricks
		pet.tasks.addTask(0,this);
	}

	/*
	 * Interface for updating current trick
	 */
	public void updateCurrentTrick(int note) {
		HarmonyProps hp = HarmonyProps.get(pet);
		ActionSet as = tricks.get(note);

		// See if we know that sound
		Trick newTrick = null;
		if (as != null) {
			newTrick = as.getTrick(pet);
		} else {
			//TODO try something new when unknown sound
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
		if (!continueTrick)
			currentTrick = null;
	}

	/*
	 * Unused required overrides
	 */
	@Override
	public void resetTask() {}
	@Override
	public void startExecuting() {}
}


