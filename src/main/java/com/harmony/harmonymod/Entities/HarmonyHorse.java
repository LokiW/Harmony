package com.harmony.harmonymod.entities;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.world.World;
//copy from living entity base
import net.minecraft.util.MathHelper;
import net.minecraft.potion.Potion;


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
    @Override
    public void moveEntityWithHeading(float strafe, float forward) {
        if (this.riddenByEntity != null && this.riddenByEntity instanceof EntityLivingBase && this.isHorseSaddled())
        {
            this.prevRotationYaw = this.rotationYaw = this.riddenByEntity.rotationYaw;
            this.rotationPitch = this.riddenByEntity.rotationPitch * 0.5F;
            this.setRotation(this.rotationYaw, this.rotationPitch);
            this.rotationYawHead = this.renderYawOffset = this.rotationYaw;
            strafe = ((EntityLivingBase)this.riddenByEntity).moveStrafing * 0.5F;
            forward = ((EntityLivingBase)this.riddenByEntity).moveForward;

            if (forward <= 0.0F) {
                forward *= 0.25F;
            }

            // Removed 
            /*
            if (this.onGround && this.jumpPower == 0.0F && this.isRearing() && !this.field_110294_bI) {
                strafe = 0.0F;
                forward = 0.0F;
            }
            */
            if (this.jumpPower > 0.0F && !this.isHorseJumping() && this.onGround) {
                this.motionY = this.getHorseJumpStrength() * (double)this.jumpPower;

                if (this.isPotionActive(Potion.jump))
                {
                    this.motionY += (double)((float)(this.getActivePotionEffect(Potion.jump).getAmplifier() + 1) * 0.1F);
                }

                this.setHorseJumping(true);
                this.isAirBorne = true;

                if (forward > 0.0F)
                {
                    float var3 = MathHelper.sin(this.rotationYaw * (float)Math.PI / 180.0F);
                    float var4 = MathHelper.cos(this.rotationYaw * (float)Math.PI / 180.0F);
                    this.motionX += (double)(-0.4F * var3 * this.jumpPower);
                    this.motionZ += (double)(0.4F * var4 * this.jumpPower);
                    this.playSound("mob.horse.jump", 0.4F, 1.0F);
                }

                this.jumpPower = 0.0F;
            }

            this.stepHeight = 1.0F;
            this.jumpMovementFactor = this.getAIMoveSpeed() * 0.1F;

            if (this.worldObj.isRemote) {
                // This is where the "magic" (hacks) happens
                // Horses jitter because they are only updated form the server normally,
                // to reduce jitter we can update them from the client but the client often runs
                // faster than the server meaning we can't update them as fast as the server.
                // So we reduce the movement speed based on server ping to smooth horses.
                this.setAIMoveSpeed((float)this.getEntityAttribute(SharedMonsterAttributes.movementSpeed).getAttributeValue());
                this.moveEntityWithHeadingBasic(strafe, forward);
            } /*else {
                this.setAIMoveSpeed((float)this.getEntityAttribute(SharedMonsterAttributes.movementSpeed).getAttributeValue()*0.5F);
            }
            this.moveEntityWithHeadingBasic(strafe, forward);*/

            if (this.onGround)
            {
                this.jumpPower = 0.0F;
                this.setHorseJumping(false);
            }

            this.prevLimbSwingAmount = this.limbSwingAmount;
            double var8 = this.posX - this.prevPosX;
            double var5 = this.posZ - this.prevPosZ;
            float var7 = MathHelper.sqrt_double(var8 * var8 + var5 * var5) * 4.0F;

            if (var7 > 1.0F)
            {
                var7 = 1.0F;
            }

            this.limbSwingAmount += (var7 - this.limbSwingAmount) * 0.4F;
            this.limbSwing += this.limbSwingAmount;
        }
        else
        {
            this.stepHeight = 0.5F;
            this.jumpMovementFactor = 0.02F;
            this.moveEntityWithHeadingBasic(strafe, forward);
        }
        /*
        if (this.riddenByEntity != null && this.riddenByEntity instanceof EntityLivingBase && this.isHorseSaddled()) {
            super.moveEntityWithHeading(strafe, forward);
            if (this.worldObj.isRemote){
                // still move forward if on client so client knows where horse should roughly be.
                System.out.println("HarmonyMod: Move foward with horse on client.");
                this.setAIMoveSpeed((float)this.getEntityAttribute(SharedMonsterAttributes.movementSpeed).getAttributeValue());
                this.moveEntityWithHeadingBasic(strafe, forward);
            }
        }*/
    }

    /*
     * Basic move entity with heading from EntityLivingBase.
     * Called from EntityHorse, but unable to be referenced in HarmonyHorse.
     */
    private void moveEntityWithHeadingBasic(float strafe, float forward) {
        double var8;

        if (this.isInWater())
        {
            var8 = this.posY;
            this.moveFlying(strafe, forward, this.isAIEnabled() ? 0.04F : 0.02F);
            this.moveEntity(this.motionX, this.motionY, this.motionZ);
            this.motionX *= 0.800000011920929D;
            this.motionY *= 0.800000011920929D;
            this.motionZ *= 0.800000011920929D;
            this.motionY -= 0.02D;

            if (this.isCollidedHorizontally && this.isOffsetPositionInLiquid(this.motionX, this.motionY + 0.6000000238418579D - this.posY + var8, this.motionZ))
            {
                this.motionY = 0.30000001192092896D;
            }
        }
        else if (this.handleLavaMovement())
        {
            var8 = this.posY;
            this.moveFlying(strafe, forward, 0.02F);
            this.moveEntity(this.motionX, this.motionY, this.motionZ);
            this.motionX *= 0.5D;
            this.motionY *= 0.5D;
            this.motionZ *= 0.5D;
            this.motionY -= 0.02D;

            if (this.isCollidedHorizontally && this.isOffsetPositionInLiquid(this.motionX, this.motionY + 0.6000000238418579D - this.posY + var8, this.motionZ))
            {
                this.motionY = 0.30000001192092896D;
            }
        }
        else
        {
            float var3 = 0.91F;

            if (this.onGround)
            {
                var3 = this.worldObj.getBlock(MathHelper.floor_double(this.posX), MathHelper.floor_double(this.boundingBox.minY) - 1, MathHelper.floor_double(this.posZ)).slipperiness * 0.91F;
            }

            float var4 = 0.16277136F / (var3 * var3 * var3);
            float var5;

            if (this.onGround)
            {
                var5 = this.getAIMoveSpeed() * var4;
            }
            else
            {
                var5 = this.jumpMovementFactor;
            }

            this.moveFlying(strafe, forward, var5);
            var3 = 0.91F;

            if (this.onGround)
            {
                var3 = this.worldObj.getBlock(MathHelper.floor_double(this.posX), MathHelper.floor_double(this.boundingBox.minY) - 1, MathHelper.floor_double(this.posZ)).slipperiness * 0.91F;
            }

            if (this.isOnLadder())
            {
                float var6 = 0.15F;

                if (this.motionX < (double)(-var6))
                {
                    this.motionX = (double)(-var6);
                }

                if (this.motionX > (double)var6)
                {
                    this.motionX = (double)var6;
                }

                if (this.motionZ < (double)(-var6))
                {
                    this.motionZ = (double)(-var6);
                }

                if (this.motionZ > (double)var6)
                {
                    this.motionZ = (double)var6;
                }

                this.fallDistance = 0.0F;

                if (this.motionY < -0.15D)
                {
                    this.motionY = -0.15D;
                }
            }

            this.moveEntity(this.motionX, this.motionY, this.motionZ);

            if (this.isCollidedHorizontally && this.isOnLadder())
            {
                this.motionY = 0.2D;
            }

            if (this.worldObj.isRemote && (!this.worldObj.blockExists((int)this.posX, 0, (int)this.posZ) || !this.worldObj.getChunkFromBlockCoords((int)this.posX, (int)this.posZ).isChunkLoaded))
            {
                if (this.posY > 0.0D)
                {
                    this.motionY = -0.1D;
                }
                else
                {
                    this.motionY = 0.0D;
                }
            }
            else
            {
                this.motionY -= 0.08D;
            }

            this.motionY *= 0.9800000190734863D;
            this.motionX *= (double)var3;
            this.motionZ *= (double)var3;
        }

        this.prevLimbSwingAmount = this.limbSwingAmount;
        var8 = this.posX - this.prevPosX;
        double var9 = this.posZ - this.prevPosZ;
        float var10 = MathHelper.sqrt_double(var8 * var8 + var9 * var9) * 4.0F;

        if (var10 > 1.0F)
        {
            var10 = 1.0F;
        }

        this.limbSwingAmount += (var10 - this.limbSwingAmount) * 0.4F;
        this.limbSwing += this.limbSwingAmount;

    }

    /*
    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (this.riddenByEntity != null && this.riddenByEntity instanceof EntityLivingBase && this.isHorseSaddled()) {
            EntityLivingBase ridder = (EntityLivingBase) this.riddenByEntity;
            this.moveEntityWithHeading(ridder.moveStrafing, ridder.moveForward);
            System.out.println("HarmonyMod: Called moveEntityWithHeading from onLivingUpdate");
        }
    }*/
}

