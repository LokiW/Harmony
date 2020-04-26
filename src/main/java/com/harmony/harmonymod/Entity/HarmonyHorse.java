package com.harmony.harmonymod.tricks;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.world.World;

public class HarmonyHorse extends EntityHorse {

    public HarmonyHorse(World world) {
        super(world);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
    }

    /**
     * Move the entity based on a specific heading
     * Minecraft's base horse is being overriden in order to override this class
     * so that the movement is done client side and server side so that horse motion
     * is not as jittery and jerky
     */
    public void moveEntityWithHeading(float strafe, float forward) {
        super.moveEntityWithHeading(strafe, forward);

        if (this.riddenByEntity != null && this.riddenByEntity instanceof EntityLivingBase && this.isHorseSaddled()) {
            // Also do horse movement on the client
            if (this.worldObj.isRemote) {
                this.setAIMoveSpeed((float)this.getEntityAttribute(SharedMonsterAttributes.movementSpeed).getAttributeValue());
                super.moveEntityWithHeading(strafe, forward);
            }
       }
    }
}

