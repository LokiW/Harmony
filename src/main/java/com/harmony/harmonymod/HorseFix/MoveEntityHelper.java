package com.harmony.harmonymod.horsefix;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.util.MathHelper;
import net.minecraft.entity.SharedMonsterAttributes;

public class MoveEntityHelper {

     public static boolean entityRequiresMoveHelper(Entity myEntit) {
        // TODO Update for entities to wrap based off config
        if (myEntit instanceof EntityHorse) {
            return ((EntityHorse) myEntit).isHorseSaddled();
        }
        return false;
    }

    public static boolean moveableByPlayer(EntityLivingBase myEntity) {
        // update to include other Entities controllabe by player
        if (myEntity instanceof EntityHorse) {
            EntityHorse horse = (EntityHorse) myEntity;
            return horse.isHorseSaddled() && horse.isTame();
        }
        return false;
    }

    public static void moveEntityWithHeading(EntityLivingBase myEntity, float strafe, float forward) {
        System.out.println("HarmonyMod: moveEntityWithHeading called in RideableEntityWrapper");
        if (!MoveEntityHelper.moveableByPlayer(myEntity)) {
            myEntity.moveEntityWithHeading(strafe, forward);
            return;
        }

        if (myEntity.worldObj.isRemote) {
            // This is where the "magic" (hacks) happens
            // Horses jitter because they are only updated form the server normally,
            // to reduce jitter we can update them from the client but the client often runs
            // faster than the server meaning we can't update them as fast as the server.
            // So we reduce the movement speed based on server ping to smooth horses.
            myEntity.setAIMoveSpeed((float)myEntity.getEntityAttribute(SharedMonsterAttributes.movementSpeed).getAttributeValue());
            MoveEntityHelper.moveEntityWithHeadingBasic(myEntity, strafe, forward);
        }
    }

    /*
     * Basic move entity with heading from EntityLivingBase.
     * Called from EntityHorse, but unable to be referenced in HarmonyHorse.
     */
    public static void moveEntityWithHeadingBasic(EntityLivingBase myEntity, float strafe, float forward) {
        double var8;

        if (myEntity.isInWater())
        {
            var8 = myEntity.posY;
            myEntity.moveFlying(strafe, forward, 0.02F);
            myEntity.moveEntity(myEntity.motionX, myEntity.motionY, myEntity.motionZ);
            myEntity.motionX *= 0.800000011920929D;
            myEntity.motionY *= 0.800000011920929D;
            myEntity.motionZ *= 0.800000011920929D;
            myEntity.motionY -= 0.02D;

            if (myEntity.isCollidedHorizontally && myEntity.isOffsetPositionInLiquid(myEntity.motionX, myEntity.motionY + 0.6000000238418579D - myEntity.posY + var8, myEntity.motionZ))
            {
                myEntity.motionY = 0.30000001192092896D;
            }
        }
        else if (myEntity.handleLavaMovement())
        {
            var8 = myEntity.posY;
            myEntity.moveFlying(strafe, forward, 0.02F);
            myEntity.moveEntity(myEntity.motionX, myEntity.motionY, myEntity.motionZ);
            myEntity.motionX *= 0.5D;
            myEntity.motionY *= 0.5D;
            myEntity.motionZ *= 0.5D;
            myEntity.motionY -= 0.02D;

            if (myEntity.isCollidedHorizontally && myEntity.isOffsetPositionInLiquid(myEntity.motionX, myEntity.motionY + 0.6000000238418579D - myEntity.posY + var8, myEntity.motionZ))
            {
                myEntity.motionY = 0.30000001192092896D;
            }
        }
        else
        {
            float var3 = 0.91F;

            if (myEntity.onGround)
            {
                var3 = myEntity.worldObj.getBlock(MathHelper.floor_double(myEntity.posX), MathHelper.floor_double(myEntity.boundingBox.minY) - 1, MathHelper.floor_double(myEntity.posZ)).slipperiness * 0.91F;
            }

            float var4 = 0.16277136F / (var3 * var3 * var3);
            float var5;

            if (myEntity.onGround)
            {
                var5 = myEntity.getAIMoveSpeed() * var4;
            }
            else
            {
                var5 = myEntity.jumpMovementFactor;
            }

            myEntity.moveFlying(strafe, forward, var5);
            var3 = 0.91F;

            if (myEntity.onGround)
            {
                var3 = myEntity.worldObj.getBlock(MathHelper.floor_double(myEntity.posX), MathHelper.floor_double(myEntity.boundingBox.minY) - 1, MathHelper.floor_double(myEntity.posZ)).slipperiness * 0.91F;
            }

            if (myEntity.isOnLadder())
            {
                float var6 = 0.15F;

                if (myEntity.motionX < (double)(-var6))
                {
                    myEntity.motionX = (double)(-var6);
                }

                if (myEntity.motionX > (double)var6)
                {
                    myEntity.motionX = (double)var6;
                }

                if (myEntity.motionZ < (double)(-var6))
                {
                    myEntity.motionZ = (double)(-var6);
                }

                if (myEntity.motionZ > (double)var6)
                {
                    myEntity.motionZ = (double)var6;
                }

                myEntity.fallDistance = 0.0F;

                if (myEntity.motionY < -0.15D)
                {
                    myEntity.motionY = -0.15D;
                }
            }

            myEntity.moveEntity(myEntity.motionX, myEntity.motionY, myEntity.motionZ);

            if (myEntity.isCollidedHorizontally && myEntity.isOnLadder())
            {
                myEntity.motionY = 0.2D;
            }

            if (myEntity.worldObj.isRemote && (!myEntity.worldObj.blockExists((int)myEntity.posX, 0, (int)myEntity.posZ) || !myEntity.worldObj.getChunkFromBlockCoords((int)myEntity.posX, (int)myEntity.posZ).isChunkLoaded))
            {
                if (myEntity.posY > 0.0D)
                {
                    myEntity.motionY = -0.1D;
                }
                else
                {
                    myEntity.motionY = 0.0D;
                }
            }
            else
            {
                myEntity.motionY -= 0.08D;
            }

            myEntity.motionY *= 0.9800000190734863D;
            myEntity.motionX *= (double)var3;
            myEntity.motionZ *= (double)var3;
        }

        myEntity.prevLimbSwingAmount = myEntity.limbSwingAmount;
        var8 = myEntity.posX - myEntity.prevPosX;
        double var9 = myEntity.posZ - myEntity.prevPosZ;
        float var10 = MathHelper.sqrt_double(var8 * var8 + var9 * var9) * 4.0F;

        if (var10 > 1.0F)
        {
            var10 = 1.0F;
        }

        myEntity.limbSwingAmount += (var10 - myEntity.limbSwingAmount) * 0.4F;
        myEntity.limbSwing += myEntity.limbSwingAmount;

        myEntity.serverPosX = myEntity.myEntitySize.multiplyBy32AndRound(myEntity.posX);
        myEntity.serverPosY = MathHelper.floor_double(myEntity.posY * 32.0D);
        myEntity.serverPosZ = myEntity.myEntitySize.multiplyBy32AndRound(myEntity.posZ);

    }
}
