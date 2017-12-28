package com.harmony.harmonymod;

import net.minecraftforge.common.*;
import net.minecraftforge.event.entity.*;
import net.minecraftforge.common.util.*;
import net.minecraft.entity.*;
import net.minecraft.entity.passive.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.*;
import net.minecraft.util.*;
import net.minecraft.world.World;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.entity.player.*;
import cpw.mods.fml.common.eventhandler.*;
import cpw.mods.fml.common.FMLCommonHandler;
import net.minecraft.entity.ai.attributes.BaseAttributeMap;
import net.minecraft.entity.ai.attributes.IAttribute;
import com.harmony.harmonymod.tricks.TrickHandler;
import java.io.*;


/*
 * Addon to entities to store traits and initialize tricks
 */
public class HarmonyProps implements IExtendedEntityProperties {

	public static final String PROP_NAME = HarmonyMod.MODID + "_HarmonyProps";

	public TrickHandler tricks;
	public Traits traits;

	public transient EntityLiving pet;

	public HarmonyProps(Entity e) {
		this.pet = (EntityLiving) e;
	}

	/*
	 * Initialize default traits and behviour for e
	 */
	public void constructProperties(Entity e) {
		traits = new Traits(pet);
		tricks = new TrickHandler(pet);
	}


	public Boolean isInitialized() {
		return traits != null;
	}

	public static void register() {
		MinecraftForge.EVENT_BUS.register(new Handler());
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
			tricks.registerAI();

			if(tricks.currentTrick != null) {
				tricks.currentTrick.pet = this.pet;
				System.out.println("HarmonyMod: loaded active trick " + tricks.currentTrick);
			}
		}
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
	public static class Handler {

		/*
		 * Create a HarmonyProps for non-player livings entities
		 */
		@SubscribeEvent
		public void entityConstructing(EntityEvent.EntityConstructing e) {
			if(e.entity instanceof EntityLiving && !(e.entity instanceof EntityPlayer)) {

				EntityLiving animal = (EntityLiving)e.entity;
				if(animal.getExtendedProperties(PROP_NAME) == null) {
					animal.registerExtendedProperties(PROP_NAME, new HarmonyProps(animal));

					// Register attacking for passive mobs
					// TODO make this list configurable
					if (animal instanceof EntityCow || animal instanceof EntityPig ||
							animal instanceof EntitySheep || animal instanceof EntityChicken ||
							animal instanceof EntityWolf) {
						BaseAttributeMap bam = animal.getAttributeMap();
						bam.registerAttribute(SharedMonsterAttributes.attackDamage);
						animal.getEntityAttribute(SharedMonsterAttributes.attackDamage).setBaseValue(1.0);
					}
				}
			} 
		}

		/*
		 * Can't apply attributes until after Join, so we apply Traits here
		 */
		@SubscribeEvent
		public void entityJoin(EntityJoinWorldEvent e) {
			HarmonyProps props = HarmonyProps.get(e.entity);
			if(props != null && !props.isInitialized()) {
				props.constructProperties(e.entity);
			}

	   }

	}

	// Required to implement, never called
	public void init(Entity e, World w) {}
}
