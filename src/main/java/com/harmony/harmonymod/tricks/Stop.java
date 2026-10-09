package com.harmony.harmonymod.tricks;

import net.minecraft.entity.*;
import net.minecraft.world.*;
import net.minecraft.pathfinding.*;
import java.util.*;

/*
 * Ends whatever the pet is doing, handled by TrickHandler
 */
public class Stop extends Trick {

	public void setupTrick(EntityLiving pet, Trick currentTrick) {
		this.pet = pet;
	}

	public boolean act() {
		return false;
	}

	public boolean isInstant() {
		return true;
	}

	public boolean consume(Trick newTrick) {
		return false;
	}
}
