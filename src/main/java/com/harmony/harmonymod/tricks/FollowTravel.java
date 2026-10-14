package com.harmony.harmonymod.tricks;

import com.harmony.harmonymod.HarmonyProps;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/*
 * Pets following a player (Follow) go with them into another dimension: through a portal, or out of
 * the End. Only pets that were close by come along, and not when the player died and respawned.
 */
public class FollowTravel {
	// Players who died, so their respawn doesn't bring pets along
	private final Set<UUID> died = new HashSet<UUID>();

	public static void register() {
		FollowTravel handler = new FollowTravel();
		MinecraftForge.EVENT_BUS.register(handler);
		FMLCommonHandler.instance().bus().register(handler);
	}

	@SubscribeEvent
	public void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
		bringPets(event.player);
	}

	@SubscribeEvent
	public void onDeath(LivingDeathEvent event) {
		if (event.entityLiving instanceof EntityPlayerMP) {
			this.died.add(event.entityLiving.getUniqueID());
		}
	}

	/*
	 * Leaving the End respawns the player instead of changing their dimension
	 */
	@SubscribeEvent
	public void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
		if (!this.died.remove(event.player.getUniqueID())) {
			bringPets(event.player);
		}
	}

	/*
	 * Move pets closely following the player in other worlds to the player. Runs straight after the
	 * player moved, while the world they left still has the pets loaded.
	 */
	private static void bringPets(EntityPlayer player) {
		if (player.worldObj.isRemote) {
			return;
		}
		WorldServer target = (WorldServer) player.worldObj;
		for (WorldServer world : MinecraftServer.getServer().worldServers) {
			if (world == target) {
				continue;
			}
			List<EntityLiving> pets = new ArrayList<EntityLiving>();
			for (Object o : world.loadedEntityList) {
				if (!(o instanceof EntityLiving)) {
					continue;
				}
				EntityLiving pet = (EntityLiving) o;
				HarmonyProps hp = HarmonyProps.get(pet);
				if (!pet.isDead && hp != null && hp.tricks != null && hp.tricks.currentTrick instanceof Follow
						&& ((Follow) hp.tricks.currentTrick).isFollowingClosely(player)) {
					pets.add(pet);
				}
			}
			for (EntityLiving pet : pets) {
				move(pet, target, player);
			}
		}
	}

	private static void move(EntityLiving pet, WorldServer target, EntityPlayer player) {
		if (pet.riddenByEntity != null) {
			pet.riddenByEntity.mountEntity(null);
		}
		if (pet.ridingEntity != null) {
			pet.mountEntity(null);
		}
		// The lead can't stretch across dimensions: drop it at the player
		boolean leashed = pet.getLeashed();
		pet.clearLeashed(true, false);

		NBTTagCompound data = new NBTTagCompound();
		if (!pet.writeToNBTOptional(data)) {
			return;
		}
		Entity moved = EntityList.createEntityFromNBT(data, target);
		if (moved == null) {
			return;
		}
		pet.setDead();
		moved.dimension = target.provider.dimensionId;
		HarmonyProps hp = HarmonyProps.get(moved);
		if (hp != null && hp.tricks != null && hp.tricks.currentTrick instanceof Follow) {
			((Follow) hp.tricks.currentTrick).arriveNear(player);
		} else {
			moved.setLocationAndAngles(player.posX, player.boundingBox.minY, player.posZ, player.rotationYaw, 0.0F);
		}
		target.spawnEntityInWorld(moved);
		if (leashed) {
			moved.dropItem(Items.lead, 1);
		}
	}
}
