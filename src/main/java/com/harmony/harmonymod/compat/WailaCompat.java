package com.harmony.harmonymod.compat;

import com.harmony.harmonymod.HarmonyProps;
import com.harmony.harmonymod.info.AnimalInfo;
import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaEntityAccessor;
import mcp.mobius.waila.api.IWailaEntityProvider;
import mcp.mobius.waila.api.IWailaRegistrar;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import java.util.List;

/*
 * Adds a short Harmony section (owner, mood, traits) to WAILA's tooltip for animals. Only loaded
 * when WAILA is installed: WAILA calls callbackRegister, registered by name in HarmonyMod.
 */
public class WailaCompat implements IWailaEntityProvider {

	public static void callbackRegister(IWailaRegistrar registrar) {
		registrar.registerBodyProvider(new WailaCompat(), EntityLiving.class);
	}

	@Override
	public List<String> getWailaBody(Entity entity, List<String> tooltip, IWailaEntityAccessor accessor, IWailaConfigHandler config) {
		if (entity instanceof EntityLiving) {
			tooltip.addAll(AnimalInfo.describe((EntityLiving) entity, HarmonyProps.get(entity)));
		}
		return tooltip;
	}

	@Override
	public Entity getWailaOverride(IWailaEntityAccessor accessor, IWailaConfigHandler config) {
		return null;
	}

	@Override
	public List<String> getWailaHead(Entity entity, List<String> tooltip, IWailaEntityAccessor accessor, IWailaConfigHandler config) {
		return tooltip;
	}

	@Override
	public List<String> getWailaTail(Entity entity, List<String> tooltip, IWailaEntityAccessor accessor, IWailaConfigHandler config) {
		return tooltip;
	}

	@Override
	public NBTTagCompound getNBTData(EntityPlayerMP player, Entity entity, NBTTagCompound tag, World world) {
		return tag;
	}
}
