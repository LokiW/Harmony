package com.harmony.harmonymod.tricks;

import net.minecraft.entity.ai.*;
import net.minecraft.entity.ai.attributes.*;
import net.minecraft.entity.*;
import net.minecraft.world.*;
import net.minecraft.pathfinding.*;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

// TODO REMOVE


public class MoveToEntity extends EntityAIBase
{
    private EntityCreature thePet;
    private EntityLivingBase theOwner;
    World theWorld;
    private double field_75336_f;
    private int field_75343_h;
    private PathNavigate petPathfinder;

    public MoveToEntity(EntityCreature p_i1625_1_)
    {
        this.thePet = p_i1625_1_;
        this.theWorld = p_i1625_1_.worldObj;
        this.petPathfinder = p_i1625_1_.getNavigator();
        this.setMutexBits(3);
    }

    /**
     * Returns whether the EntityAIBase should begin execution.
     */
    public  boolean shouldExecute()
    {
        EntityLivingBase entitylivingbase =theWorld.getClosestPlayerToEntity(this.thePet,30.0);

        if (entitylivingbase == null)
        {
            return false;
        }
        else if (this.thePet.getDistanceSqToEntity(entitylivingbase) < (double)(3.0))
        {
            System.out.println("player too close");
            return false;
        }
        else
        {
            this.theOwner = entitylivingbase;
            System.out.println("Starting follow ai");
            return true;
        }
    }

    /**
     * Returns whether an in-progress EntityAIBase should continue executing
     */
    public  boolean continueExecuting()
    {
        return !this.petPathfinder.noPath() && this.thePet.getDistanceSqToEntity(this.theOwner) > (double)(10.0);
    }

    /**
     * Execute a one shot task or start executing a continuous task
     */
    public  void startExecuting()
    {
        field_75343_h = 0;
    }

    /**
     * Resets the task
     */
    public  void resetTask()
    {
        this.theOwner = null;
        this.petPathfinder.clearPathEntity();
    }

    /**
     * Updates the task
     */
    public  void updateTask()
    {
        this.thePet.getLookHelper().setLookPositionWithEntity(this.theOwner, 10.0F, (float)this.thePet.getVerticalFaceSpeed());

        if (--this.field_75343_h <= 0)
        {
            this.field_75343_h = 10;

            //IAttribute speed = SharedMonsterAttributes.movementSpeed;
            this.field_75336_f = 1.0;//thePet.getEntityAttribute(speed).getAttributeValue();

            if (!this.petPathfinder.tryMoveToEntityLiving(this.theOwner, this.field_75336_f))
            {
                System.out.println("fail to trymove");

		// Teleport to location near owner if too far away
                if (this.thePet.getDistanceSqToEntity(this.theOwner) >= 144.0D)
                {
                    int i = MathHelper.floor_double(this.theOwner.posX) - 2;
                    int j = MathHelper.floor_double(this.theOwner.posZ) - 2;
                    int k = MathHelper.floor_double(this.theOwner.boundingBox.minY);

                    for (int l = 0; l <= 4; ++l)
                    {
                        for (int i1 = 0; i1 <= 4; ++i1)
                        {
                            if ((l < 1 || i1 < 1 || l > 3 || i1 > 3)
				    && World.doesBlockHaveSolidTopSurface(this.theWorld, i + l, k - 1, j + i1)
				    && !this.theWorld.getBlock(i + l, k, j + i1).isNormalCube()
				    && !this.theWorld.getBlock(i + l, k + 1, j + i1).isNormalCube())
                            {
                                this.thePet.setLocationAndAngles(
					(double)((float)(i + l) + 0.5F),
					(double)k,
					(double)((float)(j + i1) + 0.5F),
					this.thePet.rotationYaw,
					this.thePet.rotationPitch);
                                this.petPathfinder.clearPathEntity();
                                return;
                            }
                        }
                    }
                }
            }
        }
    }
    
}
