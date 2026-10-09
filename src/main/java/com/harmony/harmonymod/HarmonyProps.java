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
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.entity.ai.attributes.BaseAttributeMap;
import com.harmony.harmonymod.tricks.TrickHandler;
import com.harmony.harmonymod.aitasks.BreedingAI;
import com.harmony.harmonymod.aitasks.HarmonyWanderAI;
import java.util.UUID;


/*
 * Addon to entities to store traits and initialize tricks
 */
public class HarmonyProps implements IExtendedEntityProperties {

	public static final String PROP_NAME = HarmonyMod.MODID + "_HarmonyProps";

	// Version of the saved data. Bump it when the format changes, and convert older data in loadNBTData.
	private static final int DATA_VERSION = 1;

	// Happiness runs from 0 to MAX_HAPPINESS. Animals breed from HarmonyMod.breedingHappiness up,
	// and are unhappy below UNHAPPY_BELOW.
	public static final int MAX_HAPPINESS = 20;
	public static final int UNHAPPY_BELOW = 5;
	// New animals start neutral
	public static final int STARTING_HAPPINESS = 5;

	public TrickHandler tricks;
	public Traits traits;
	public int happiness;

	// Whether a player has looked after this animal (fed, bonded, or born to kept parents). Only kept
	// animals breed on their own, so wild herds don't multiply.
	public boolean kept;

	// Client copy of the animal's mood (-1 unhappy, 0 content, 1 happy), sent by the server
	public int mood;

	// Player bonded with this animal (see Ownership), null while unbonded
	public UUID ownerId;
	public String ownerName;

	public transient EntityLiving pet;

	// Data last sent to clients, so only changes are sent. Server only, see AnimalSync.
	public NBTTagCompound lastSynced;

	// Client copy of whether the animal is learning a trick (the server's is in tricks)
	public boolean learning;

	public HarmonyProps(Entity e) {
		this.pet = (EntityLiving) e;
		happiness = STARTING_HAPPINESS;
	}

	/*
	 * Change happiness, keeping it in range
	 */
	public void changeHappiness(int delta) {
		happiness = Math.max(0, Math.min(MAX_HAPPINESS, happiness + delta));
	}

	public boolean canBreed() {
		return happiness >= HarmonyMod.breedingHappiness;
	}

	/*
	 * -1 unhappy, 0 content, 1 happy enough to breed
	 */
	public int getMood() {
		return canBreed() ? 1 : (happiness < UNHAPPY_BELOW ? -1 : 0);
	}

	/*
	 * Initialize default traits and behviour for e
	 */
	public void constructProperties() {
		traits = new Traits(pet);
		tricks = new TrickHandler(pet);
		registerAI();
	}

	/*
	 * Specific traits, for the /harmony spawn test command
	 */
	public void constructProperties(Traits.TRAIT[] chosen) {
		traits = new Traits(pet, chosen);
		tricks = new TrickHandler(pet);
		registerAI();
	}

	public void constructProperties(EntityLiving p1, EntityLiving p2) {
		traits = new Traits(pet, p1, p2);
		tricks = new TrickHandler(pet);
		registerAI();
	}

	/*
	 * On clients this stays false until the server sends the animal's data
	 */
	public Boolean isInitialized() {
		return traits != null;
	}

	public static void register() {
		MinecraftForge.EVENT_BUS.register(new EntityCreationHandler());
	}

	public static HarmonyProps get(Entity e) {
		return (HarmonyProps) e.getExtendedProperties(PROP_NAME);
	}

	public boolean isBonded() {
		return ownerId != null;
	}

	public boolean isOwner(EntityPlayer player) {
		return ownerId != null && ownerId.equals(player.getUniqueID());
	}

	/*
	 * Traits, happiness and tricks, as saved with the entity and sent to clients
	 */
	public NBTTagCompound writeData() {
		NBTTagCompound data = new NBTTagCompound();
		data.setInteger("Version", DATA_VERSION);
		data.setInteger("Happiness", happiness);
		data.setBoolean("Kept", kept);
		if (ownerId != null) {
			data.setString("OwnerId", ownerId.toString());
			data.setString("OwnerName", ownerName);
		}
		traits.writeToNBT(data);

		NBTTagCompound trickData = new NBTTagCompound();
		tricks.writeToNBT(trickData);
		data.setTag("Tricks", trickData);
		return data;
	}

	private void readData(NBTTagCompound data) {
		happiness = data.getInteger("Happiness");
		kept = data.getBoolean("Kept");
		ownerId = data.hasKey("OwnerId") ? UUID.fromString(data.getString("OwnerId")) : null;
		ownerName = data.getString("OwnerName");
		traits = Traits.readFromNBT(data);
		tricks = new TrickHandler(pet);
		tricks.readFromNBT(data.getCompoundTag("Tricks"));
	}

	@Override
	public void saveNBTData(NBTTagCompound tag) {
		if (this.isInitialized()) {
			tag.setTag(PROP_NAME, writeData());
		}
	}

	/*
	 * Called after constructprops, read data from disk if it exists
	 */
	@Override
	public void loadNBTData(NBTTagCompound tag) {
		if(tag.hasKey(PROP_NAME, Constants.NBT.TAG_COMPOUND)) {
			readData(tag.getCompoundTag(PROP_NAME));
			registerAI();
		}
	}

	/*
	 * Client copy of the server's data, see AnimalSync. No AI is registered, the server runs that.
	 */
	public void readSyncedData(NBTTagCompound data) {
		readData(data);
		learning = data.getBoolean("Learning");
		mood = data.getInteger("Mood");
	}

	/*
	 * Saved data plus state clients need that isn't saved
	 */
	public NBTTagCompound writeSyncData() {
		NBTTagCompound data = writeData();
		data.setBoolean("Learning", tricks.isLearningTrick());
		data.setInteger("Mood", getMood());
		return data;
	}

	private void registerAI() {
		tricks.registerTask();
		BreedingAI.registerTask(this.pet);
		HarmonyWanderAI.registerTask(this.pet);
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
			// Clients get their data from the server instead
			if (e.world.isRemote) {
				return;
			}
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
