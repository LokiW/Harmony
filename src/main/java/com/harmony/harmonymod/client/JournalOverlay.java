package com.harmony.harmonymod.client;

import com.harmony.harmonymod.HarmonyMod;
import com.harmony.harmonymod.HarmonyProps;
import com.harmony.harmonymod.info.AnimalInfo;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import java.util.List;

/*
 * While the player holds a Pet Journal, shows a page about the Harmony animal they're looking at,
 * next to the crosshair
 */
@SideOnly(Side.CLIENT)
public class JournalOverlay {
	// How far away an animal can be read
	private static final double RANGE = 8.0D;
	private static final int BACKGROUND = 0xC0201810;
	private static final int BORDER = 0xFF8B6B3E;
	private static final int MAX_WIDTH = 200;

	public static void register() {
		MinecraftForge.EVENT_BUS.register(new JournalOverlay());
	}

	@SubscribeEvent
	public void onOverlay(RenderGameOverlayEvent.Post event) {
		if (event.type != RenderGameOverlayEvent.ElementType.ALL) {
			return;
		}
		Minecraft mc = Minecraft.getMinecraft();
		EntityPlayer player = mc.thePlayer;
		if (player == null || mc.currentScreen != null) {
			return;
		}
		ItemStack held = player.getCurrentEquippedItem();
		if (held == null || held.getItem() != HarmonyMod.petJournal) {
			return;
		}
		EntityLiving animal = lookedAtAnimal(mc, event.partialTicks);
		if (animal == null) {
			return;
		}
		List<String> lines = AnimalInfo.describe(animal, HarmonyProps.get(animal), true);
		lines.add(0, "§l" + animal.getCommandSenderName());

		FontRenderer font = mc.fontRenderer;
		// No line gets wider than this, so the page always fits beside the crosshair
		int maxWidth = Math.min(MAX_WIDTH, event.resolution.getScaledWidth() / 2 - 24);
		int width = 0;
		for (int i = 0; i < lines.size(); i++) {
			String line = lines.get(i);
			if (font.getStringWidth(line) > maxWidth) {
				line = font.trimStringToWidth(line, maxWidth - font.getStringWidth("...")) + "...";
				lines.set(i, line);
			}
			width = Math.max(width, font.getStringWidth(line));
		}
		ScaledResolution res = event.resolution;
		int x = res.getScaledWidth() / 2 + 14;
		int height = lines.size() * 10 + 6;
		int y = Math.max(4, res.getScaledHeight() / 2 - height / 2);
		if (x + width + 8 > res.getScaledWidth()) {
			x = res.getScaledWidth() - width - 8;
		}

		Gui.drawRect(x - 1, y - 1, x + width + 7, y + height + 1, BORDER);
		Gui.drawRect(x, y, x + width + 6, y + height, BACKGROUND);
		for (int i = 0; i < lines.size(); i++) {
			font.drawStringWithShadow(lines.get(i), x + 3, y + 4 + i * 10, 0xFFFFFF);
		}
	}

	/*
	 * The Harmony animal under the crosshair, up to RANGE away and not through blocks
	 */
	private static EntityLiving lookedAtAnimal(Minecraft mc, float partialTicks) {
		EntityPlayer player = mc.thePlayer;
		Vec3 eye = player.getPosition(partialTicks);
		Vec3 look = player.getLook(partialTicks);
		Vec3 end = eye.addVector(look.xCoord * RANGE, look.yCoord * RANGE, look.zCoord * RANGE);

		double reach = RANGE;
		// rayTraceBlocks moves the start vector along the ray as it goes, so give it copies
		MovingObjectPosition block = mc.theWorld.rayTraceBlocks(
				Vec3.createVectorHelper(eye.xCoord, eye.yCoord, eye.zCoord),
				Vec3.createVectorHelper(end.xCoord, end.yCoord, end.zCoord));
		if (block != null) {
			reach = block.hitVec.distanceTo(eye);
		}

		EntityLiving closest = null;
		AxisAlignedBB search = player.boundingBox.addCoord(look.xCoord * RANGE, look.yCoord * RANGE, look.zCoord * RANGE)
				.expand(1.0D, 1.0D, 1.0D);
		for (Object o : mc.theWorld.getEntitiesWithinAABBExcludingEntity(player, search)) {
			if (!(o instanceof EntityLiving) || HarmonyProps.get((Entity) o) == null) {
				continue;
			}
			EntityLiving entity = (EntityLiving) o;
			MovingObjectPosition hit = entity.boundingBox.expand(0.3D, 0.3D, 0.3D).calculateIntercept(eye, end);
			if (hit != null && hit.hitVec.distanceTo(eye) < reach) {
				reach = hit.hitVec.distanceTo(eye);
				closest = entity;
			}
		}
		return closest;
	}
}
