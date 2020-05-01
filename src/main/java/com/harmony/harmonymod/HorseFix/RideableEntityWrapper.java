package com.harmony.harmonymod.horsefix;

import com.harmony.harmonymod.HarmonyMod;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.util.MathHelper;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.world.World;
import java.lang.reflect.Field;


public class RideableEntityWrapper extends EntityLiving {

    public static final String NAME = "RideableEntityWrapper";
    public static final String FULLNAME = HarmonyMod.MODID +".RideableEntityWrapper";
    public EntityLivingBase mount;

    public RideableEntityWrapper(World world) {
        super(world);
        System.out.println("HarmonyMod: RideableEntityWrapper entity created");
    }

    public void wrapEntity(EntityLivingBase mount) {
        this.mount = mount;
        if (this.mount.riddenByEntity != null && this.mount.riddenByEntity instanceof EntityLivingBase) {
            EntityLivingBase rider = (EntityLivingBase) this.mount.riddenByEntity;
            rider.dismountEntity(this.mount);
            rider.mountEntity(this);
            this.mountEntity(this.mount);
        }
        initBasedOnWrappedEntity();
        System.out.println("HarmonyMod: RideableEntityWrapper wrapped entity");
    }

    public static boolean entityRequiresWrap(Entity entityToWrap) {
        // TODO Update for entities to wrap based off config
        if (entityToWrap instanceof EntityHorse) {
            return ((EntityHorse) entityToWrap).isHorseSaddled();
        }
        return false;
    }

    protected void initBasedOnWrappedEntity() {
        // Set Entity to Invisible (since it is just a wrapper)
        this.setFlag(5, true);
        this.setWrapperStatsToMount();
    }

    public boolean isTame() {
        if (this.mount instanceof EntityTameable) {
            return ((EntityTameable) this.mount).isTamed();
        } else if (this.mount instanceof EntityHorse) {
            return ((EntityHorse) this.mount).isTame();
        }
        return false;
    }

    public boolean moveableByPlayer() {
        // update to include other Entities controllabe by player
        if (this.isTame() && this.mount instanceof EntityHorse) {
            return ((EntityHorse) this.mount).isHorseSaddled();
        }
        return false;
    }

    public static RideableEntityWrapper createWrapper(World world, EntityLivingBase mount) {
        RideableEntityWrapper entityToSpawn = null;
        if (!world.isRemote) { // never spawn entity on client side
            String name = RideableEntityWrapper.FULLNAME;
            if (EntityList.stringToClassMapping.containsKey(name)) {
                entityToSpawn = (RideableEntityWrapper) EntityList.createEntityByName(name, world);
                world.spawnEntityInWorld(entityToSpawn);
                entityToSpawn.wrapEntity(mount);
            } else {
                //DEBUG
                System.out.println("HarmonyMod: Entity not found "+ name);
            }
        }

        return entityToSpawn;
    }

    public void unwrappEntity() {
        this.worldObj.removeEntity(this);
    }

    @Override
    public void onEntityUpdate() {
        setWrapperStatsToMount();
    }

    @Override
    public void moveEntityWithHeading(float strafe, float forward) {
        System.out.println("HarmonyMod: moveEntityWithHeading called in RideableEntityWrapper");
        if (!this.moveableByPlayer()) {
            this.mount.moveEntityWithHeading(strafe, forward);
            return;
        }

        if (this.worldObj.isRemote) {
            // This is where the "magic" (hacks) happens
            // Horses jitter because they are only updated form the server normally,
            // to reduce jitter we can update them from the client but the client often runs
            // faster than the server meaning we can't update them as fast as the server.
            // So we reduce the movement speed based on server ping to smooth horses.
            this.setAIMoveSpeed((float)this.mount.getEntityAttribute(SharedMonsterAttributes.movementSpeed).getAttributeValue());
            this.moveEntityWithHeadingBasic(strafe, forward);
        }
    }

    /*
     * Basic move entity with heading from EntityLivingBase.
     * Called from EntityHorse, but unable to be referenced in HarmonyHorse.
     */
    private void moveEntityWithHeadingBasic(float strafe, float forward) {
        double var8;

        if (this.mount.isInWater())
        {
            var8 = this.mount.posY;
            this.mount.moveFlying(strafe, forward, 0.02F);
            this.mount.moveEntity(this.mount.motionX, this.mount.motionY, this.mount.motionZ);
            this.mount.motionX *= 0.800000011920929D;
            this.mount.motionY *= 0.800000011920929D;
            this.mount.motionZ *= 0.800000011920929D;
            this.mount.motionY -= 0.02D;

            if (this.mount.isCollidedHorizontally && this.mount.isOffsetPositionInLiquid(this.mount.motionX, this.mount.motionY + 0.6000000238418579D - this.mount.posY + var8, this.mount.motionZ))
            {
                this.mount.motionY = 0.30000001192092896D;
            }
        }
        else if (this.mount.handleLavaMovement())
        {
            var8 = this.mount.posY;
            this.mount.moveFlying(strafe, forward, 0.02F);
            this.mount.moveEntity(this.mount.motionX, this.mount.motionY, this.mount.motionZ);
            this.mount.motionX *= 0.5D;
            this.mount.motionY *= 0.5D;
            this.mount.motionZ *= 0.5D;
            this.mount.motionY -= 0.02D;

            if (this.mount.isCollidedHorizontally && this.mount.isOffsetPositionInLiquid(this.mount.motionX, this.mount.motionY + 0.6000000238418579D - this.mount.posY + var8, this.mount.motionZ))
            {
                this.mount.motionY = 0.30000001192092896D;
            }
        }
        else
        {
            float var3 = 0.91F;

            if (this.mount.onGround)
            {
                var3 = this.mount.worldObj.getBlock(MathHelper.floor_double(this.mount.posX), MathHelper.floor_double(this.mount.boundingBox.minY) - 1, MathHelper.floor_double(this.mount.posZ)).slipperiness * 0.91F;
            }

            float var4 = 0.16277136F / (var3 * var3 * var3);
            float var5;

            if (this.mount.onGround)
            {
                var5 = this.mount.getAIMoveSpeed() * var4;
            }
            else
            {
                var5 = this.mount.jumpMovementFactor;
            }

            this.mount.moveFlying(strafe, forward, var5);
            var3 = 0.91F;

            if (this.mount.onGround)
            {
                var3 = this.mount.worldObj.getBlock(MathHelper.floor_double(this.mount.posX), MathHelper.floor_double(this.mount.boundingBox.minY) - 1, MathHelper.floor_double(this.mount.posZ)).slipperiness * 0.91F;
            }

            if (this.mount.isOnLadder())
            {
                float var6 = 0.15F;

                if (this.mount.motionX < (double)(-var6))
                {
                    this.mount.motionX = (double)(-var6);
                }

                if (this.mount.motionX > (double)var6)
                {
                    this.mount.motionX = (double)var6;
                }

                if (this.mount.motionZ < (double)(-var6))
                {
                    this.mount.motionZ = (double)(-var6);
                }

                if (this.mount.motionZ > (double)var6)
                {
                    this.mount.motionZ = (double)var6;
                }

                this.mount.fallDistance = 0.0F;

                if (this.mount.motionY < -0.15D)
                {
                    this.mount.motionY = -0.15D;
                }
            }

            this.mount.moveEntity(this.mount.motionX, this.mount.motionY, this.mount.motionZ);

            if (this.mount.isCollidedHorizontally && this.mount.isOnLadder())
            {
                this.mount.motionY = 0.2D;
            }

            if (this.mount.worldObj.isRemote && (!this.mount.worldObj.blockExists((int)this.mount.posX, 0, (int)this.mount.posZ) || !this.mount.worldObj.getChunkFromBlockCoords((int)this.mount.posX, (int)this.mount.posZ).isChunkLoaded))
            {
                if (this.mount.posY > 0.0D)
                {
                    this.mount.motionY = -0.1D;
                }
                else
                {
                    this.mount.motionY = 0.0D;
                }
            }
            else
            {
                this.mount.motionY -= 0.08D;
            }

            this.mount.motionY *= 0.9800000190734863D;
            this.mount.motionX *= (double)var3;
            this.mount.motionZ *= (double)var3;
        }

        this.mount.prevLimbSwingAmount = this.mount.limbSwingAmount;
        var8 = this.mount.posX - this.mount.prevPosX;
        double var9 = this.mount.posZ - this.mount.prevPosZ;
        float var10 = MathHelper.sqrt_double(var8 * var8 + var9 * var9) * 4.0F;

        if (var10 > 1.0F)
        {
            var10 = 1.0F;
        }

        this.mount.limbSwingAmount += (var10 - this.mount.limbSwingAmount) * 0.4F;
        this.mount.limbSwing += this.mount.limbSwingAmount;
        this.setWrapperStatsToMount();
    }

    public void setWrapperStatsToMount() {
        this.posX = this.mount.posX;
        this.posY = this.mount.posY;
        this.posZ = this.mount.posZ;
        this.rotationPitch = this.mount.rotationPitch;
        this.rotationYaw = this.mount.rotationYaw;

        this.motionX = this.mount.motionX;
        this.motionY = this.mount.motionY;
        this.motionZ = this.mount.motionZ;

        /*
        Class cursor = this.getClass().getSuperclass();
        while (cursor != null && cursor != Object.class) {
            // EntityLiving Fields
            System.out.println("HarmonyMod: syncing for " + cursor);
            Field[] fieldsToUpdate = cursor.getDeclaredFields();
            for (Field field : fieldsToUpdate) {
                if (field.isAccessible()) {
                    try {
                        field.set(this, field.get(this.mount));
                        System.out.println("HarmonyMod: sync " + field.getName() + " to rideableEntityWrapper from mount.");
                    } catch (Exception e) {
                        // TODO remove print
                        System.out.println("HarmonyMod: failed to sync " + field.getName() + " to rideableEntityWrapper from mount.");
                    }
                }
            }
            cursor = cursor.getSuperclass();
        }*/
    }
}
