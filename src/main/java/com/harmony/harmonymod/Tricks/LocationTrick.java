package com.harmony.harmonymod.tricks;

import net.minecraft.entity.ai.*;
import net.minecraft.entity.ai.attributes.*;
import net.minecraft.entity.*;
import net.minecraft.world.*;
import net.minecraft.pathfinding.*;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import java.lang.Math;

public class LocationTrick extends Trick {
    public double targetX;
	public double targetY;
	public double targetZ;

    public LocationTrick(double x, double y, double z) {
		this.targetX = x;
		this.targetY = y;
		this.targetZ = z;
    }

	public void setupTrick(EntityLiving pet, Trick currentTrick) {
		this.pet = pet;
	}

	public boolean consume(Trick newTrick) {
		return false;
	}

	public boolean isInstant() {
		return false;
	}

	public boolean act() {
		this.pet.getLookHelper().setLookPosition(this.targetX, this.targetY, this.targetZ, 20.0F, (float)this.pet.getVerticalFaceSpeed());
		return true;
    }
}
