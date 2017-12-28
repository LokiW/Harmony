package com.harmony.harmonymod.tricks;

import net.minecraft.entity.ai.*;
import net.minecraft.entity.EntityCreature;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import com.harmony.harmonymod.HarmonyProps;
import com.harmony.harmonymod.tricks.*;

public class ActionSet {
    private long actions;
	private static Random rand = new Random();

	public ActionSet(long actions) {
		this.actions = actions;
	}

	public void addAction(long action) {
		this.actions |= action;
	}


	/*
	 * Get Trick in actionSet 
	 * Not all actions are a trick, in this case null is returned.
	 */
	public Trick getTrick(EntityCreature pet) {
		int max = Long.bitCount(actions);
		long gotAction = 3;
		if (max == 1) {
			gotAction = actions;
		} else {
			int dist = rand.nextInt(max);
			for (int i = 0; i < 64; i++) {
				dist -= (actions >> i) & 1;
				if ( dist < 0) {
					gotAction = 1 << i;
					break;
				}
			}
		}
		
		return convertRawAction(gotAction, pet);
	}

	
	private Trick convertRawAction(long action, EntityCreature pet) {
		int a = (int) action;
		int b = (int) (action >> 32);
		HarmonyProps hp = HarmonyProps.get(pet);
		// Convert action bits to Trick
		switch (a) {
			case TrickEnum.STOP:
				break;
			case TrickEnum.GO:
				return new MoveTo();
			case TrickEnum.LEARNED_LOCATION_1:
				return new LocationTrick(hp.xLearned1, hp.yLearned1, hp.zLearned1, false);
			case TrickEnum.ATTACK:
				return new Attack();
			case TrickEnum.GUARD:
				return new Guard();
			default:
				break;
		}
		return null;
	}
}
