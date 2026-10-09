package com.harmony.harmonymod.tricks;

import net.minecraft.entity.ai.*;
import net.minecraft.entity.ai.attributes.*;
import net.minecraft.entity.*;
import net.minecraft.world.*;
import net.minecraft.pathfinding.*;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import java.lang.Math;
import java.util.UUID;
import java.io.*;

public class EntityTrick extends Trick {
	public transient EntityLiving target;
	public transient UUID uniqueId;

    public EntityTrick(EntityLiving e) {
		this.target = e;
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
		this.pet.getLookHelper().setLookPositionWithEntity(this.target, 10.0F, (float)this.pet.getVerticalFaceSpeed());
		return true;
    }

	private void writeObject(ObjectOutputStream oos) throws IOException {
		// default serialization 
		oos.defaultWriteObject();
		// write the entity id
		oos.writeObject(target.getUniqueID());
	}

	private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
		// default deserialization
		ois.defaultReadObject();
		uniqueId= (UUID)ois.readObject(); 
	}
}
