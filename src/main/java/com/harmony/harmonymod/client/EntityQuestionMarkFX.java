package com.harmony.harmonymod.client;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

/*
 * A question mark that floats up and fades out, shown over animals learning a trick
 */
@SideOnly(Side.CLIENT)
public class EntityQuestionMarkFX extends EntityFX {

	public EntityQuestionMarkFX(World world, double x, double y, double z, IIcon icon) {
		super(world, x, y, z, 0.0D, 0.0D, 0.0D);
		this.setParticleIcon(icon);
		this.motionX = 0.0D;
		this.motionY = 0.015D;
		this.motionZ = 0.0D;
		this.particleGravity = 0.0F;
		this.particleScale = 1.6F;
		this.particleMaxAge = 30;
		this.noClip = true;
		this.setRBGColorF(1.0F, 1.0F, 1.0F);
	}

	/*
	 * Drawn from the items texture sheet, where the icon is registered
	 */
	@Override
	public int getFXLayer() {
		return 2;
	}

	@Override
	public void onUpdate() {
		super.onUpdate();
		// Fade out over the last third of its life
		float left = (float)(this.particleMaxAge - this.particleAge) / (float)this.particleMaxAge;
		this.setAlphaF(Math.min(1.0F, left * 3.0F));
	}
}
