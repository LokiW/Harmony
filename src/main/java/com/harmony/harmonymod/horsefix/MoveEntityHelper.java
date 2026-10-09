package com.harmony.harmonymod.horsefix;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.util.MathHelper;
import net.minecraft.entity.SharedMonsterAttributes;

public class MoveEntityHelper {

    /*
     * Moves a horse that the local player is riding, on the client.
     * Vanilla EntityHorse.moveEntityWithHeading only applies physics on the server, which is
     * why ridden horses jitter. It has already run this tick (rotation, jump motion, limb swing),
     * so this applies the physics it skipped using the rider's input.
     */
    public static void moveRiddenHorse(EntityHorse horse, EntityLivingBase rider) {
        // Same input scaling EntityHorse uses: half speed strafing, quarter speed backwards
        float strafe = rider.moveStrafing * 0.5F;
        float forward = rider.moveForward;
        if (forward <= 0.0F) {
            forward *= 0.25F;
        }

        // Swimming is slowed the same way: the horse bobs in and out of the water, and the ticks it
        // spends just above the surface would otherwise move it at nearly land speed
        if (isLiquidAt(horse, horse.posY) || isLiquidAt(horse, horse.posY - 1.0D)) {
            strafe *= SWIM_SPEED;
            forward *= SWIM_SPEED;
        }

        // Float like vanilla: the server's swim AI keeps an unridden horse up, but it doesn't move
        // this one, so without this a ridden horse sinks and drowns in deep water.
        if (horse.isInWater() || horse.handleLavaMovement()) {
            horse.motionY += 0.03999999910593033D;
        }

        horse.setAIMoveSpeed((float)horse.getEntityAttribute(SharedMonsterAttributes.movementSpeed).getAttributeValue());
        MoveEntityHelper.moveEntityWithHeadingBasic(horse, strafe, forward);
    }

    // Input scale while a ridden horse swims, so swimming is slower than a boat
    private static final float SWIM_SPEED = 0.35F;

    private static boolean isLiquidAt(EntityLivingBase entity, double y) {
        return entity.worldObj.getBlock(MathHelper.floor_double(entity.posX), MathHelper.floor_double(y),
                MathHelper.floor_double(entity.posZ)).getMaterial().isLiquid();
    }

    /*
     * Basic move entity with heading from EntityLivingBase.
     * EntityHorse overrides it, so it can't be called directly on a horse.
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

        // EntityHorse already updated the limb swing this tick while the horse was standing still,
        // so undo that update and redo it with the distance actually moved.
        float startSwingAmount = myEntity.prevLimbSwingAmount;
        myEntity.limbSwing -= myEntity.limbSwingAmount;
        var8 = myEntity.posX - myEntity.prevPosX;
        double var9 = myEntity.posZ - myEntity.prevPosZ;
        float var10 = MathHelper.sqrt_double(var8 * var8 + var9 * var9) * 4.0F;

        if (var10 > 1.0F)
        {
            var10 = 1.0F;
        }

        myEntity.limbSwingAmount = startSwingAmount + (var10 - startSwingAmount) * 0.4F;
        myEntity.limbSwing += myEntity.limbSwingAmount;
    }
}
