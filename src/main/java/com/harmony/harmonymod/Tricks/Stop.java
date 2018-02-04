package com.harmony.harmonymod.tricks;

import net.minecraft.entity.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.ai.EntityJumpHelper;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.*;
import net.minecraft.pathfinding.*;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.util.DamageSource;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import java.lang.Math;
import java.lang.Class;
import java.lang.reflect.Method;
import java.util.*;
import com.harmony.harmonymod.HarmonyProps;
import com.harmony.harmonymod.Traits;
import com.harmony.harmonymod.Traits.MAGICAL_TRAIT;

public class Stop extends Trick {
	private int delayCounter;

	public void setupTrick(EntityLiving pet, Trick currentTrick) {
		this.pet = pet;
	}

	public boolean act() {
		if (this.delayCounter >= LEARNING_DELAY) {
			return false;
		}

		delayCounter++;
		return true;
	}

	public boolean isInstant() {
		return true;
	}

	public boolean consume(Trick newTrick) {
		return false;
	}
}
