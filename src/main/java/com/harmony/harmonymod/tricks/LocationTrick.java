package com.harmony.harmonymod.tricks;

import net.minecraft.entity.ai.*;
import net.minecraft.entity.ai.attributes.*;
import net.minecraft.entity.*;
import net.minecraft.world.*;
import net.minecraft.pathfinding.*;
import net.minecraft.nbt.NBTTagCompound;

public class LocationTrick extends Trick {
    public double targetX;
	public double targetY;
	public double targetZ;

	LocationTrick() {}

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

	@Override
	protected String getSaveType() {
		return "location";
	}

	@Override
	protected void writeToNBT(NBTTagCompound tag) {
		tag.setDouble("X", this.targetX);
		tag.setDouble("Y", this.targetY);
		tag.setDouble("Z", this.targetZ);
	}

	@Override
	protected void readFromNBT(NBTTagCompound tag) {
		this.targetX = tag.getDouble("X");
		this.targetY = tag.getDouble("Y");
		this.targetZ = tag.getDouble("Z");
	}
}
