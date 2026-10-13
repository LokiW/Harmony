package com.harmony.harmonymod.respawn;

import com.harmony.harmonymod.HarmonyProps;
import com.harmony.harmonymod.tricks.TrickHandler;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.common.gameevent.TickEvent.WorldTickEvent;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

/*
 * Animals with a respawn point (fed a golden apple) faint instead of dying: they're healed and wake
 * up at their respawn point straight away. If the point isn't loaded (far away, or another dimension)
 * they wait as a spirit (SpiritStore) until it is. A golden apple's respawn is used up by fainting,
 * an enchanted golden apple's is permanent. Bonded animals tell their owner.
 */
public class Respawn {
	// How often (in ticks) each world checks for spirits whose respawn point has loaded
	private static final int SPIRIT_CHECK_INTERVAL = 40;
	// How far from its respawn point an animal may be moved to find room to wake up
	private static final int SAFE_SPOT_RANGE = 3;

	// Animals that fainted this tick. They're moved at the end of the tick: whatever killed them is
	// usually still running (e.g. lava sets them on fire after the damage), and moving them mid-update
	// would make the rest of it run at the respawn point, even loading chunks there.
	private static final Set<EntityLiving> fainted = new LinkedHashSet<EntityLiving>();

	public static void register() {
		Respawn handler = new Respawn();
		MinecraftForge.EVENT_BUS.register(handler);
		FMLCommonHandler.instance().bus().register(handler);
	}

	@SubscribeEvent
	public void onDeath(LivingDeathEvent event) {
		if (event.entityLiving.worldObj.isRemote || !(event.entityLiving instanceof EntityLiving)) {
			return;
		}
		EntityLiving animal = (EntityLiving) event.entityLiving;
		HarmonyProps hp = HarmonyProps.get(animal);
		if (hp == null || !hp.isInitialized() || !hp.tricks.hasRespawn()) {
			return;
		}
		event.setCanceled(true);
		// Enough health not to die again before the end of the tick
		animal.setHealth(animal.getMaxHealth());
		fainted.add(animal);
	}

	private static void faint(EntityLiving animal) {
		HarmonyProps hp = HarmonyProps.get(animal);
		TrickHandler tricks = hp.tricks;
		double x = tricks.xRespawn, y = tricks.yRespawn, z = tricks.zRespawn;
		int dim = tricks.dimRespawn;
		boolean usedUp = !tricks.permanentRespawn;
		if (usedUp) {
			tricks.clearRespawn();
		}

		recover(animal);
		puff(animal);
		if (animal.riddenByEntity != null) {
			animal.riddenByEntity.mountEntity(null);
		}
		if (animal.ridingEntity != null) {
			animal.mountEntity(null);
		}
		animal.clearLeashed(true, true);

		WorldServer target = MinecraftServer.getServer().worldServerForDimension(dim);
		if (dim == animal.dimension && isLoaded(target, x, z)) {
			placeSafely(animal, target, x, y, z);
			animal.setPositionAndUpdate(animal.posX, animal.posY, animal.posZ);
			puff(animal);
			tellOwner(hp, "harmony.respawn.fainted", animal);
		} else {
			// Wait as a spirit until the respawn point is loaded. Not moved there, which would load it.
			NBTTagCompound entityData = new NBTTagCompound();
			if (animal.writeToNBTOptional(entityData)) {
				NBTTagCompound spirit = new NBTTagCompound();
				spirit.setInteger("Dim", dim);
				spirit.setDouble("X", x);
				spirit.setDouble("Y", y);
				spirit.setDouble("Z", z);
				spirit.setTag("Entity", entityData);
				SpiritStore store = SpiritStore.get();
				store.spirits.add(spirit);
				store.markDirty();
			}
			animal.setDead();
			tellOwner(hp, "harmony.respawn.waiting", animal);
		}
		if (usedUp) {
			tellOwner(hp, "harmony.respawn.used_up", animal);
		}
	}

	/*
	 * Wake up spirits whose respawn point is loaded in this world
	 */
	@SubscribeEvent
	public void onWorldTick(WorldTickEvent event) {
		if (event.side != Side.SERVER || event.phase != Phase.END) {
			return;
		}
		World world = event.world;

		Iterator<EntityLiving> faintedThisTick = fainted.iterator();
		while (faintedThisTick.hasNext()) {
			EntityLiving animal = faintedThisTick.next();
			if (animal.worldObj == world) {
				faintedThisTick.remove();
				if (!animal.isDead) {
					faint(animal);
				}
			}
		}

		if (world.getTotalWorldTime() % SPIRIT_CHECK_INTERVAL != 0) {
			return;
		}
		SpiritStore store = SpiritStore.get();
		Iterator<NBTTagCompound> it = store.spirits.iterator();
		while (it.hasNext()) {
			NBTTagCompound spirit = it.next();
			double x = spirit.getDouble("X"), y = spirit.getDouble("Y"), z = spirit.getDouble("Z");
			if (spirit.getInteger("Dim") != world.provider.dimensionId || !isLoaded(world, x, z)) {
				continue;
			}
			it.remove();
			store.markDirty();

			Entity entity = EntityList.createEntityFromNBT(spirit.getCompoundTag("Entity"), world);
			if (!(entity instanceof EntityLiving)) {
				continue;
			}
			EntityLiving animal = (EntityLiving) entity;
			placeSafely(animal, world, x, y, z);
			world.spawnEntityInWorld(animal);
			puff(animal);
			HarmonyProps hp = HarmonyProps.get(animal);
			if (hp != null) {
				tellOwner(hp, "harmony.respawn.returned", animal);
			}
		}
	}

	private static void recover(EntityLiving animal) {
		animal.setHealth(animal.getMaxHealth());
		animal.extinguish();
		animal.clearActivePotions();
		animal.fallDistance = 0.0F;
		animal.motionX = animal.motionY = animal.motionZ = 0.0D;
		animal.setAttackTarget(null);
	}

	private static boolean isLoaded(World world, double x, double z) {
		return world.getChunkProvider().chunkExists(MathHelper.floor_double(x) >> 4, MathHelper.floor_double(z) >> 4);
	}

	/*
	 * Put the animal at its respawn point, or the nearest spot with room for it, so it can't wake up
	 * inside a block (and faint again forever)
	 */
	private static void placeSafely(EntityLiving animal, World world, double x, double y, double z) {
		for (int dy = 0; dy <= SAFE_SPOT_RANGE; dy++) {
			for (int r = 0; r <= SAFE_SPOT_RANGE; r++) {
				for (int dx = -r; dx <= r; dx++) {
					for (int dz = -r; dz <= r; dz++) {
						if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
							continue;
						}
						animal.setPosition(x + dx, y + dy, z + dz);
						if (world.getCollidingBoundingBoxes(animal, animal.boundingBox).isEmpty()
								&& !world.isAnyLiquid(animal.boundingBox)) {
							return;
						}
					}
				}
			}
		}
		int top = world.getTopSolidOrLiquidBlock(MathHelper.floor_double(x), MathHelper.floor_double(z));
		animal.setPosition(x, top, z);
	}

	private static void puff(EntityLiving animal) {
		if (animal.worldObj instanceof WorldServer) {
			((WorldServer) animal.worldObj).func_147487_a("cloud", animal.posX, animal.posY + animal.height * 0.5D,
					animal.posZ, 12, animal.width * 0.5D, animal.height * 0.5D, animal.width * 0.5D, 0.02D);
		}
	}

	private static void tellOwner(HarmonyProps hp, String key, EntityLiving animal) {
		if (!hp.isBonded()) {
			return;
		}
		for (Object o : MinecraftServer.getServer().getConfigurationManager().playerEntityList) {
			EntityPlayerMP player = (EntityPlayerMP) o;
			if (hp.ownerId.equals(player.getUniqueID())) {
				player.addChatComponentMessage(new ChatComponentTranslation(key, animal.getCommandSenderName()));
			}
		}
	}
}
