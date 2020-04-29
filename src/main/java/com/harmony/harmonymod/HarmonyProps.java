package com.harmony.harmonymod;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.IExtendedEntityProperties;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.common.util.Constants;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.server.MinecraftServer;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.FMLCommonHandler;
import net.minecraft.entity.ai.attributes.BaseAttributeMap;
import net.minecraft.entity.ai.attributes.IAttribute;
import com.harmony.harmonymod.tricks.TrickHandler;
import com.harmony.harmonymod.aitasks.BreedingAI;
import com.harmony.harmonymod.aitasks.HarmonyWanderAI;
import com.harmony.harmonymod.horsefix.NetHandlerPlayAndRideServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraft.network.NetworkSystem;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.NetHandlerPlayServer;
import java.lang.reflect.Field;
import java.util.List;
import java.io.*;


/*
 * Addon to entities to store traits and initialize tricks
 */
public class HarmonyProps implements IExtendedEntityProperties {

	public static final String PROP_NAME = HarmonyMod.MODID + "_HarmonyProps";

	public TrickHandler tricks;
	public Traits traits;
	public int happiness;

	public transient EntityLiving pet;

	public HarmonyProps(Entity e) {
		this.pet = (EntityLiving) e;
		happiness = 0;
	}

	/*
	 * Initialize default traits and behviour for e
	 */
	public void constructProperties() {
		traits = new Traits(pet);
		tricks = new TrickHandler(pet);
		registerAI();
	}

	public void constructProperties(EntityLiving p1, EntityLiving p2) {
		traits = new Traits(pet, p1, p2);
		tricks = new TrickHandler(pet);
		registerAI();
	}

	public Boolean isInitialized() {
		return traits != null;
	}

	public static void register() {
		MinecraftForge.EVENT_BUS.register(new EntityCreationHandler());
	}

	public static HarmonyProps get(Entity e) {
		return (HarmonyProps) e.getExtendedProperties(PROP_NAME);
	}

	/*
	 * Serialize Traits and Tricks to disk
	 */
	@Override
	public void saveNBTData(NBTTagCompound tag) {
		if (this.isInitialized()) {
			//container tag
			NBTTagCompound data = new NBTTagCompound();
			tag.setTag(PROP_NAME, data);

			//save traits
			byte[] byteTraits = toBytes(traits);
			data.setByteArray("traits", byteTraits);

			//save tricks
			byte[] byteTricks = toBytes(tricks);
			data.setByteArray("tricks", byteTricks);
		}
	}

	/*
	 * Called after constructprops, read data from disk if it exists
	 */
	@Override
	public void loadNBTData(NBTTagCompound tag) {
		if(tag.hasKey(PROP_NAME, Constants.NBT.TAG_COMPOUND)) {
			NBTTagCompound data = tag.getCompoundTag(PROP_NAME);
			
			byte[] nbtTraits = data.getByteArray("traits");
			traits = (Traits)fromBytes(nbtTraits);

			byte[] nbtTricks = data.getByteArray("tricks");
			tricks = (TrickHandler)fromBytes(nbtTricks);
			tricks.pet = this.pet;
			tricks.registerTask();

			if(tricks.currentTrick != null) {
				tricks.currentTrick.updatePet(this.pet);
				System.out.println("HarmonyMod: loaded active trick " + tricks.currentTrick);
			}

			registerAI();
		}
	}

	private void registerAI() {
		BreedingAI.registerTask(this.pet);
		HarmonyWanderAI.registerTask(this.pet);
	}

	private byte[] toBytes(Object o) {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutput out = null;
		try {
			out = new ObjectOutputStream(bos);   
			out.writeObject(o);
			out.flush();
			byte[] bytes = bos.toByteArray();
			return bytes;
		} catch(Exception e) {
			System.out.println("HarmonyMod: Failed to save to NBT\n" + e);
		}finally {
			try {
				bos.close();
			} catch (IOException e) {
				System.out.println("HarmonyMod: Failed to save to NBT\n" + e);
			}
		}
		return null;
	}

	private Object fromBytes(byte[] bytes) {
		ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
		ObjectInput in = null;
		try {
			in = new ObjectInputStream(bis);
			Object o = in.readObject();
			return o;
		} catch(Exception e) {
			System.out.println("HarmonyMod: Failed to load from NBT\n" + e);
		}finally {
			try {
				if (in != null) {
					in.close();
				}
			} catch (IOException e) {
				System.out.println("HarmonyMod: Failed to load from NBT\n" + e);
			}
		}
		return null;
	}

	/*
	 * Hooks to Minecraft engine to add HarmonyProps to entities
	 */
	public static class EntityCreationHandler {

		/*
		 * Create a HarmonyProps for non-player livings entities
		 */
		@SubscribeEvent
		public void entityConstructing(EntityEvent.EntityConstructing e) {
			if(e.entity instanceof EntityLiving && !(e.entity instanceof EntityPlayer)) {
				HarmonyProps.addHarmonyProperties((EntityLiving)e.entity);
			}
		}

		/*
		 * Can't apply attributes until after Join, so we apply Traits here
		 */
		@SubscribeEvent
		public void entityJoin(EntityJoinWorldEvent e) {
			HarmonyProps props = HarmonyProps.get(e.entity);
			if(props != null && !props.isInitialized()) {
				props.constructProperties();
			}


	   }

	}

	public static void addHarmonyProperties(EntityLiving pet) {
		String className = pet.getClass().getSimpleName().toLowerCase();
		if(HarmonyMod.harmonyMobs.contains(className) && pet.getExtendedProperties(PROP_NAME) == null) {
			pet.registerExtendedProperties(PROP_NAME, new HarmonyProps(pet));

			// Register attacking for passive mobs
			if (HarmonyMod.needsAttackAttr.contains(className)) {
				BaseAttributeMap bam = pet.getAttributeMap();
				bam.registerAttribute(SharedMonsterAttributes.attackDamage);
				pet.getEntityAttribute(SharedMonsterAttributes.attackDamage).setBaseValue(1.0);
			}
		}
	}

	// Required to implement, never called
	public void init(Entity e, World w) {}
}
