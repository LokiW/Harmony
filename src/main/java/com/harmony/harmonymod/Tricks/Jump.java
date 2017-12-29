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

public class Jump extends Trick {
	private static Method jumper;
	private int delayCounter;

	public Jump () {
		delayCounter = 0;
	}	

	public void setupTrick(EntityLiving pet, Trick currentTrick) {
		this.pet = pet;
	}

	public boolean act() {
		if (this.delayCounter == 0) {
			if (this.pet.onGround || canFly()) {
				try {
					if (jumper == null) {
						// Reflections
						jumper = Class.forName("net.minecraft.entity.EntityLivingBase").getDeclaredMethod("jump");
						jumper.setAccessible(true);
					}
					jumper.invoke(this.pet);
				} catch (Exception e) {
					System.out.println("HarmonyMod: ERROR threw Exception " + e + " when trying to do Jump Trick");
				}
			}
		} else if (this.delayCounter == 100) {
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

	private boolean canFly() {
		HarmonyProps hp = HarmonyProps.get(this.pet);
		Traits traits = hp.traits;
		for (MAGICAL_TRAIT mt : traits.m_traits) {
			if (mt == MAGICAL_TRAIT.FLY)
				return true;
		}

		return false;
	}
}
