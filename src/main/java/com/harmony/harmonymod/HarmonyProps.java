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
import com.harmony.harmonymod.tricks.TrickIndicators;
import com.harmony.harmonymod.aitasks.BreedingAI;
import com.harmony.harmonymod.aitasks.HarmonyWanderAI;
import java.util.UUID;
import net.minecraft.entity.ai.attributes.IAttributeInstance;


/*
 * Addon to entities to store traits and initialize tricks
 */
public class HarmonyProps implements IExtendedEntityProperties {

	public static final String PROP_NAME = HarmonyMod.MODID + "_HarmonyProps";

	// Version of the saved data. Bump it when the format changes, and convert older data in loadNBTData.
	private static final int DATA_VERSION = 2;

	// Happiness runs from 0 to MAX_HAPPINESS. Animals breed from HarmonyMod.breedingHappiness up,
	// and are unhappy below UNHAPPY_BELOW. It moves a point at a time towards a target: BASELINE plus
	// offsets for the animal's current conditions (see HarmonyWanderAI).
	public static final int MAX_HAPPINESS = 20;
	public static final int UNHAPPY_BELOW = 5;
	public static final int BASELINE = 5;
	// New animals start content
	public static final int STARTING_HAPPINESS = BASELINE;
	// Being fed its breeding food adds this much to its target for FED_TICKS
	public static final int FED_HAPPINESS = 3;
	private static final int FED_TICKS = 6000;

	public TrickHandler tricks;
	public Traits traits;
	public int happiness;

	// Whether a player has looked after this animal (fed, bonded, or born to kept parents). Only kept
	// animals breed on their own, so wild herds don't multiply.
	public boolean kept;

	// Client copy of the animal's mood (-1 unhappy, 0 content, 1 happy), sent by the server
	public int mood;
	// Client copy of the animal's attack damage and its base value (clients aren't sent that attribute),
	// 0 if it has none
	public double attack, attackBase;
	// Client only: the mood changed in the last sync, for MoodIndicator to show
	public boolean moodChanged;

	// World time of the animal's last litter, -1 if it never bred. Animals breeding on their own wait
	// HarmonyMod.naturalBreedingTicks after it.
	public long lastBred = -1;
	// World time until which the animal counts as recently fed, -1 if never fed
	public long fedUntil = -1;

	// What it last noticed around it, see Comforts and HarmonyWanderAI: comforts and bothers as bit
	// flags. Worked out on the server, sent to clients for the Pet Journal.
	public int comforts;
	public int bothers;

	// Server only: whether the sheep was sheared last tick, null until first seen (see Produce)
	public Boolean wasSheared;

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

	/*
	 * One point closer to target
	 */
	public void moveHappinessTowards(int target) {
		target = Math.max(0, Math.min(MAX_HAPPINESS, target));
		changeHappiness(Integer.signum(target - happiness));
	}

	public void fed() {
		fedUntil = pet.worldObj.getTotalWorldTime() + FED_TICKS;
	}

	public boolean recentlyFed() {
		return fedUntil >= 0 && pet.worldObj.getTotalWorldTime() < fedUntil;
	}

	public boolean canBreed() {
		return happiness >= HarmonyMod.breedingHappiness;
	}

	/*
	 * Long enough since its last litter to breed on its own again
	 */
	public boolean readyToBreedNaturally() {
		return lastBred < 0 || pet.worldObj.getTotalWorldTime() - lastBred >= HarmonyMod.naturalBreedingTicks(pet);
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
		if (lastBred >= 0) {
			data.setLong("LastBred", lastBred);
		}
		if (fedUntil >= 0) {
			data.setLong("FedUntil", fedUntil);
		}
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
		lastBred = data.hasKey("LastBred") ? data.getLong("LastBred") : -1;
		fedUntil = data.hasKey("FedUntil") ? data.getLong("FedUntil") : -1;
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
		boolean hadData = isInitialized();
		readData(data);
		learning = data.getBoolean("Learning");
		int newMood = data.getInteger("Mood");
		moodChanged = hadData && newMood != mood;
		mood = newMood;
		attack = data.getDouble("Attack");
		attackBase = data.getDouble("AttackBase");
		comforts = data.getInteger("Comforts");
		bothers = data.getInteger("Bothers");
	}

	/*
	 * Particles that show a mood: green sparkles happy, white puffs content, grey clouds unhappy
	 */
	public static String moodParticle(int mood) {
		return mood > 0 ? "happyVillager" : (mood < 0 ? "angryVillager" : "cloud");
	}

	public static int moodParticleCount(int mood) {
		return mood > 0 ? 6 : (mood < 0 ? 2 : 3);
	}

	/*
	 * Saved data plus state clients need that isn't saved
	 */
	public NBTTagCompound writeSyncData() {
		NBTTagCompound data = writeData();
		data.setBoolean("Learning", tricks.isLearningTrick());
		data.setInteger("Mood", getMood());
		data.setInteger("Comforts", comforts);
		data.setInteger("Bothers", bothers);
		IAttributeInstance attackAttr = pet.getEntityAttribute(SharedMonsterAttributes.attackDamage);
		if (attackAttr != null) {
			data.setDouble("Attack", attackAttr.getAttributeValue());
			data.setDouble("AttackBase", attackAttr.getBaseValue());
		}
		return data;
	}

	private void registerAI() {
		tricks.registerTask();
		BreedingAI.registerTask(this.pet);
		HarmonyWanderAI.registerTask(this.pet);
		TrickIndicators.stopHunting(this.pet);
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
			if (props == null) {
				return;
			}
			boolean addedAttack = addAttackDamage(props.pet);
			if (!props.isInitialized()) {
				props.constructProperties();
			} else if (addedAttack) {
				// Saved or newborn animals: their attack traits couldn't be applied without the attribute
				props.traits.applyAttackTraits(props.pet);
			}


	   }

	}

	public static void addHarmonyProperties(EntityLiving pet) {
		if (HarmonyMod.isHarmonyAnimal(pet.getClass()) && pet.getExtendedProperties(PROP_NAME) == null) {
			pet.registerExtendedProperties(PROP_NAME, new HarmonyProps(pet));
		}
	}

	/*
	 * Give passive animals attack damage so they can attack (and the Vicious trait does something),
	 * if their species wants it and they don't have it. Done once they've joined the world, after their
	 * own constructor: another mod's animal may add the attribute itself, and adding it twice crashes.
	 * Returns whether it was added.
	 */
	private static boolean addAttackDamage(EntityLiving pet) {
		if (!HarmonyMod.needsAttackAttr.contains(HarmonyMod.speciesKey(pet))
				|| pet.getEntityAttribute(SharedMonsterAttributes.attackDamage) != null) {
			return false;
		}
		BaseAttributeMap bam = pet.getAttributeMap();
		bam.registerAttribute(SharedMonsterAttributes.attackDamage);
		pet.getEntityAttribute(SharedMonsterAttributes.attackDamage).setBaseValue(1.0);
		return true;
	}

	// Required to implement, never called
	public void init(Entity e, World w) {}
}
