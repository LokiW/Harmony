package com.harmony.harmonymod.tricks;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.nbt.NBTTagCompound;
import java.util.UUID;

/*
 * A trick about another entity, e.g. what to guard or attack
 */
public class EntityTrick extends Trick {
	private EntityLiving target;
	// Set after loading, until the target entity has been found again
	private UUID targetId;

	EntityTrick() {}

	public EntityTrick(EntityLiving e) {
		this.target = e;
	}

	/*
	 * The target entity, or null if it's gone or hasn't loaded yet
	 */
	public EntityLiving getTarget() {
		if (this.target == null && this.targetId != null && this.pet != null) {
			for (Object o : this.pet.worldObj.loadedEntityList) {
				if (o instanceof EntityLiving && this.targetId.equals(((Entity) o).getUniqueID())) {
					this.target = (EntityLiving) o;
					this.targetId = null;
					break;
				}
			}
		}
		if (this.target != null && this.target.isDead) {
			return null;
		}
		return this.target;
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
		EntityLiving target = getTarget();
		if (target == null) {
			return false;
		}
		this.pet.getLookHelper().setLookPositionWithEntity(target, 10.0F, (float)this.pet.getVerticalFaceSpeed());
		return true;
	}

	@Override
	protected String getSaveType() {
		return "entity";
	}

	@Override
	protected void writeToNBT(NBTTagCompound tag) {
		UUID id = this.target != null ? this.target.getUniqueID() : this.targetId;
		if (id != null) {
			tag.setLong("TargetMost", id.getMostSignificantBits());
			tag.setLong("TargetLeast", id.getLeastSignificantBits());
		}
	}

	@Override
	protected void readFromNBT(NBTTagCompound tag) {
		if (tag.hasKey("TargetMost")) {
			this.targetId = new UUID(tag.getLong("TargetMost"), tag.getLong("TargetLeast"));
		}
	}
}
