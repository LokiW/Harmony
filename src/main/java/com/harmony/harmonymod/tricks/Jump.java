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
import net.minecraft.potion.Potion;
import net.minecraftforge.common.ForgeHooks;
import java.lang.Math;
import java.util.*;
import com.harmony.harmonymod.HarmonyProps;
import com.harmony.harmonymod.Traits;
import com.harmony.harmonymod.Traits.MAGICAL_TRAIT;

public class Jump extends Trick {
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
				jump();
			}
		} else if (this.delayCounter == LEARNING_DELAY) {
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

	/*
	 * Same as EntityLivingBase.jump(), which is protected. Animals don't sprint, so its sprint boost is left out.
	 * The Forge jump event is what lets the JUMP trait boost the jump.
	 */
	private void jump() {
		this.pet.motionY = 0.41999998688697815D;

		if (this.pet.isPotionActive(Potion.jump)) {
			this.pet.motionY += (double)((float)(this.pet.getActivePotionEffect(Potion.jump).getAmplifier() + 1) * 0.1F);
		}

		this.pet.isAirBorne = true;
		ForgeHooks.onLivingJump(this.pet);
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
