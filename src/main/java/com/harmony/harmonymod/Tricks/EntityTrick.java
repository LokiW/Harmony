package com.harmony.harmonymod.tricks;

import net.minecraft.entity.ai.*;
import net.minecraft.entity.ai.attributes.*;
import net.minecraft.entity.*;
import net.minecraft.world.*;
import net.minecraft.pathfinding.*;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import java.lang.Math;

public class EntityTrick extends Trick {
	public Entity target;
	public boolean teleport;

    public EntityTrick(Entity e, boolean teleport) {
		this.target = e;
		this.teleport = teleport;
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
		System.out.println("HarmonyMod: Act in EntityTrick");
		this.pet.getLookHelper().setLookPositionWithEntity(this.target, 10.0F, (float)this.pet.getVerticalFaceSpeed());
		return true;
    }
}
