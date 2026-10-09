package com.harmony.harmonymod.client;

import com.harmony.harmonymod.HarmonyMod;
import com.harmony.harmonymod.HarmonyProps;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.IIcon;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;

/*
 * Floats question marks over animals that are learning a trick (fed a glistering melon and waiting
 * for a note and a reward), so players can tell they're in learning mode.
 */
@SideOnly(Side.CLIENT)
public class LearningIndicator {
	// Ticks between question marks
	private static final int INTERVAL = 20;

	private IIcon questionMark;

	public static void register() {
		MinecraftForge.EVENT_BUS.register(new LearningIndicator());
	}

	@SubscribeEvent
	public void onTextureStitch(TextureStitchEvent.Pre event) {
		// Particles drawing from the items sheet need their icon on it
		if (event.map.getTextureType() == 1) {
			questionMark = event.map.registerIcon(HarmonyMod.MODID + ":question_mark");
		}
	}

	@SubscribeEvent
	public void onLivingUpdate(LivingUpdateEvent event) {
		EntityLivingBase animal = event.entityLiving;
		if (!animal.worldObj.isRemote || questionMark == null || animal.ticksExisted % INTERVAL != 0) {
			return;
		}
		HarmonyProps hp = HarmonyProps.get(animal);
		if (hp == null || !hp.learning) {
			return;
		}
		Minecraft.getMinecraft().effectRenderer.addEffect(new EntityQuestionMarkFX(animal.worldObj,
				animal.posX, animal.posY + animal.height + 0.4D, animal.posZ, questionMark));
	}
}
