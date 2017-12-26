package com.harmony.harmonymod;

import net.minecraftforge.common.*;
import net.minecraftforge.event.entity.*;
import net.minecraftforge.common.util.*;
import net.minecraft.entity.*;
import net.minecraft.entity.passive.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.BaseAttributeMap;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.item.*;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.nbt.*;
import net.minecraft.util.*;
import net.minecraft.world.World;
import net.minecraft.server.MinecraftServer;
import net.minecraft.inventory.*;
import net.minecraft.entity.ai.*;
import net.minecraftforge.event.world.NoteBlockEvent.Note;
import net.minecraftforge.event.world.NoteBlockEvent.Instrument;
import net.minecraftforge.event.entity.player.*;
import cpw.mods.fml.common.gameevent.PlayerEvent.ItemCraftedEvent;
import cpw.mods.fml.common.eventhandler.*;
import cpw.mods.fml.common.FMLCommonHandler;
import com.harmony.harmonymod.tricks.TrickHandler;
import com.harmony.harmonymod.tricks.ActionSet;
import com.harmony.harmonymod.tricks.TrickEnum;
import com.harmony.harmonymod.sounds.SoundDB;
import com.harmony.harmonymod.sounds.SoundDB.InstrumentSound;
import java.util.*;


/*
 * Addon to entities to store traits and initialize tricks
 */
public class HarmonyProps implements IExtendedEntityProperties {

    public static final String PROP_NAME = HarmonyMod.MODID + "_HarmonyProps";
    public static Random rand = new Random();

    //Always add traits to end of existing traits for backward compatibility
    public static enum TRAIT {JUMP, FAST, HARDY, VICIOUS, NONE};
    public static enum MAGICAL_TRAIT {NONE};

    public TRAIT[] traits;
    public MAGICAL_TRAIT[] m_traits;

	public ActionSet actions;
	public Map<InstrumentSound, ActionSet> tricks;

	// These are for setting pet known locations at non-preset locations
	// like other actions these are taught with specific items
	public double xLearned1; public double yLearned1; public double zLearned1;
	public double xLearned2; public double yLearned2; public double zLearned2;
	public double xLearned3; public double yLearned3; public double zLearned3;

    public HarmonyProps(Entity e) {
    }

	/*
	 * Initialize default traits and behviour for e
	 */
    public void constructProperties(Entity e) {
		// Only apply traits to animals
        if (!(e instanceof EntityLiving)) {
            return;
        }
        EntityLiving animal = (EntityLiving) e;


        m_traits = new MAGICAL_TRAIT[] {MAGICAL_TRAIT.NONE, MAGICAL_TRAIT.NONE, MAGICAL_TRAIT.NONE};

        // Setup Breed Actions / Traits
        if (e instanceof EntitySheep) {
            traits = new TRAIT[] {TRAIT.HARDY, TRAIT.HARDY, TRAIT.HARDY};
        } else if (e instanceof EntityPig) {
            traits = new TRAIT[] {TRAIT.FAST, TRAIT.FAST, TRAIT.FAST};
        } else if (e instanceof EntityWolf) {
            traits = new TRAIT[] {TRAIT.VICIOUS, TRAIT.VICIOUS, TRAIT.VICIOUS};
        } else {
            traits = new TRAIT[0];
        }

        // Setup random start traits
		// TODO
        


        // Apply traits in minecraft engine
        if (traits != null) {
            Double[] attributes = new Double[TRAIT.values().length];
            for (int i = 0; i < traits.length; i++) {
                switch (traits[i]) {
                    case JUMP:
                        break;
                    case FAST:
                        AttributeModifier fastMod = new AttributeModifier(buildUUID(i, TRAIT.FAST),
                                                                        "FAST Trait Modifier " + i,
                                                                        2, 1);
                        IAttributeInstance moveAttr = animal.getEntityAttribute(
                                                                SharedMonsterAttributes.movementSpeed);
                        moveAttr.applyModifier(fastMod);
                        break;
                    case HARDY:
                        AttributeModifier healthMod = new AttributeModifier(buildUUID(i, TRAIT.HARDY),
                                                                        "HARDY Trait Modifier " + i,
                                                                        2, 0);
                        IAttributeInstance healthAttr = animal.getEntityAttribute(
                                                                SharedMonsterAttributes.maxHealth);
                        healthAttr.applyModifier(healthMod);
                        break;
                    case VICIOUS:
                        IAttributeInstance attackAttr = animal.getEntityAttribute(
                                                            SharedMonsterAttributes.attackDamage);
                        if (attackAttr != null) {
                            AttributeModifier attackMod = new AttributeModifier(buildUUID(i, TRAIT.VICIOUS),
                                                                            "VICIOUS Trait Modifier " + i,
                                                                            2, 1);
                            attackAttr.applyModifier(attackMod);
                        }
                        break;
                    default:
                        break;
                }
            }
        }

		// Set up AI behaviour
		// TODO add trickhandler and stuff
		// TODO initialize actionsets
		// TODO do these get preserved?
		tricks = new HashMap<InstrumentSound, ActionSet>();
		actions = new ActionSet(0);
		
    	if (e instanceof EntityCow) {
	   		EntityCow cow = (EntityCow)e;
			for(Object a : cow.tasks.taskEntries) {
				EntityAITasks.EntityAITaskEntry temp = (EntityAITasks.EntityAITaskEntry)a;
			}
			cow.tasks.taskEntries.clear();
			cow.targetTasks.taskEntries.clear();
			TrickHandler th = new TrickHandler(cow);
			cow.tasks.addTask(0, th);
			cow.tasks.addTask(1, new EntityAISwimming(cow));
			SoundDB soundDB = SoundDB.getSoundDB();
			ActionSet t1 = new ActionSet(TrickEnum.GO);
			tricks.put(soundDB.getSound(Instrument.BASSDRUM, Note.A), t1);
			ActionSet t2 = new ActionSet(TrickEnum.LEARNED_LOCATION_1);
			tricks.put(soundDB.getSound(Instrument.BASSDRUM, Note.C), t2);
			ActionSet t3 = new ActionSet(TrickEnum.ATTACK);
			tricks.put(soundDB.getSound(Instrument.BASSDRUM, Note.F_SHARP), t3);
			this.yLearned1 = 64;
	   	}
 
    }


    public Boolean isInitialized() {
        return traits != null;
    }

    private UUID buildUUID(int slot, TRAIT trait) {
        return new UUID(3L+slot,3L+trait.ordinal()); 
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
            NBTTagCompound data = new NBTTagCompound();
            NBTTagCompound nbtTraits = new NBTTagCompound();
            NBTTagCompound nbtTricks = new NBTTagCompound();
            NBTTagCompound nbtMTraits = new NBTTagCompound();

            nbtTraits.setInteger("length", traits.length);
            for (int i = 0; i < traits.length; i++) {
                nbtTraits.setInteger(String.valueOf(i), traits[i].ordinal());
            }

            //nbtTricks.setInteger("length", tricks.size());
            //for (int i = 0; i < tricks.size(); i++) {
            //    nbtTricks.setInteger(String.valueOf(i), tricks.get(i).ordinal());
            //}

			//TODO write out known tricks

            nbtMTraits.setInteger("length", m_traits.length);
            for (int i = 0; i < m_traits.length; i++) {
                nbtMTraits.setInteger(String.valueOf(i), m_traits[i].ordinal());
            }

            data.setTag("traits", nbtTraits);
            data.setTag("tricks", nbtTricks);
            data.setTag("mtraits", nbtMTraits);

            tag.setTag(PROP_NAME, data);
        }
    }

	/*
	 * Called after constructprops, read data from disk if it exists
	 */
    @Override
    public void loadNBTData(NBTTagCompound tag) {
        if(tag.hasKey(PROP_NAME, Constants.NBT.TAG_COMPOUND)) {
            NBTTagCompound data = tag.getCompoundTag(PROP_NAME);
            NBTTagCompound nbtTraits = data.getCompoundTag("traits");
            NBTTagCompound nbtTricks = data.getCompoundTag("tricks");
            NBTTagCompound nbtMTraits = data.getCompoundTag("mtraits");

            traits = new TRAIT[nbtTraits.getInteger("length")];
            for (int i = 0; i < traits.length; i++) {
                traits[i] = TRAIT.values()[nbtTraits.getInteger(String.valueOf(i))];
            }
            
            //tricks = new ArrayList<TRICK>();
            //for (int i = 0; i < nbtTricks.getInteger("length"); i++) {
            //    tricks.add(TRICK.values()[nbtTricks.getInteger(String.valueOf(i))]);
            //}


			//TODO read in known tricks

            m_traits = new MAGICAL_TRAIT[nbtMTraits.getInteger("length")];
            for (int i = 0; i < m_traits.length; i++) {
                m_traits[i] = MAGICAL_TRAIT.values()[nbtMTraits.getInteger(String.valueOf(i))];
            }
        }
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
					if (animal instanceof EntityCow || animal instanceof EntityPig ||
							animal instanceof EntitySheep || animal instanceof EntityChicken) {
						//System.out.println("HarmonyMod: adding attackDamage to entity " + animal);
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
