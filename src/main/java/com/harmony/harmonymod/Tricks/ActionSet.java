package com.harmony.harmonymod.tricks;

import net.minecraft.entity.ai.*;
import net.minecraft.entity.EntityLiving;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.io.Serializable;
import com.harmony.harmonymod.HarmonyProps;
import com.harmony.harmonymod.tricks.*;

public class ActionSet implements Serializable {
	// "actions" uses bit manipulation to store actions to
	// reducememory load when many pets are loaded.
	// This is premature optimization but exists anyways.
    private long actions;
	private static Random rand = new Random();

	public ActionSet(long actions) {
		this.actions = actions;
	}

	public ActionSet() {
		this.actions = TrickEnum.ALL_TRICKS;
	}

	public ActionSet(ActionSet other) {
		this.actions = other.actions;
	}

	public void addAction(long action) {
		System.out.println("HarmonyMod: adding action " + this.getTrickNameForAction(action));
		this.actions |= action;
	}

	public void removeAction(long action) {
		System.out.println("HarmonyMod: removing action " + this.getTrickNameForAction(action));
		this.actions &= ~action;
	}

	/*
	 * Get Trick in actionSet
	 * Not all actions are a trick, in this case null is returned.
	 */
	public Trick getTrick(EntityLiving pet) {
		return convertRawAction(getAction(pet), pet);
	}

	public long getAction(EntityLiving pet) {
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
		return gotAction;
	}

	/*
	 * Update action set so that this action is more likely,
	 *   by removing other actions.
	 */
	public void learnTrick(long action) {
		this.addAction(action);
		int max = Long.bitCount(actions);
		System.out.println("HarmonyMod: " + max + " actions exist in this action set.");
		if (max == 1) {
			System.out.println("HarmonyMod: No other actions to remove for learning trick.");
			return;
		}

		// TODO even the slightest varification this works
		long otherActions = this.actions & ~action;
		int dist = rand.nextInt(max-1);
		System.out.println("HarmonyMod: trying to remove the " + dist + " action from the action set.");
		for (int i = 0; i < 64; i++) {
			dist -= (otherActions >> i) & 1;
			if ( dist < 0) {
				System.out.println("HarmonyMod: actually removing action " + i);
				this.removeAction(1 << i);
				break;
			}
		}
	}
	
	public Trick convertRawAction(long action, EntityLiving pet) {
		int a = (int) action;
		int b = (int) (action >> 32);
		HarmonyProps hp = HarmonyProps.get(pet);
		// Convert action bits to Trick
		switch (a) {
			case TrickEnum.STOP:
				return new Stop();
			case TrickEnum.GO:
				return new MoveTo();
			case TrickEnum.LEARNED_LOCATION_1:
				return new LocationTrick(hp.tricks.xLearned1, hp.tricks.yLearned1, hp.tricks.zLearned1);
			case TrickEnum.LEARNED_LOCATION_2:
				return new LocationTrick(hp.tricks.xLearned2, hp.tricks.yLearned2, hp.tricks.zLearned2);
			case TrickEnum.LEARNED_LOCATION_3:
				return new LocationTrick(hp.tricks.xLearned3, hp.tricks.yLearned3, hp.tricks.zLearned3);
			case TrickEnum.ATTACK:
				return new Attack();
			case TrickEnum.GUARD:
				return new Guard();
			case TrickEnum.JUMP:
				return new Jump();
			case TrickEnum.SIT:
				return new Sit();
			default:
				break;
		}
		return null;
	}

	public String getTrickNameForAction(long action) {
		int a = (int) action;
		int b = (int) (action >> 32);	
		switch (a) {
			case TrickEnum.STOP:
				return "STOP";
			case TrickEnum.GO:
				return "GO";
			case TrickEnum.LEARNED_LOCATION_1:
				return "LEARNED_LOCATION_1";
			case TrickEnum.LEARNED_LOCATION_2:
				return "LEARNED_LOCATION_2";
			case TrickEnum.LEARNED_LOCATION_3:
				return "LEARNED_LOCATION_3";
			case TrickEnum.ATTACK:
				return "ATTACK";
			case TrickEnum.GUARD:
				return "GUARD";
			case TrickEnum.JUMP:
				return "JUMP";
			case TrickEnum.SIT:
				return "SIT";
			default:
				break;
		}
		return "UNKNOWN " + a;
	}
}
