package com.harmony.harmonymod;


import net.minecraftforge.common.MinecraftForge;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraftforge.event.entity.living.LivingEvent.LivingJumpEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import java.util.UUID;
import net.minecraft.entity.*;
import net.minecraft.entity.passive.*;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraftforge.common.util.Constants;
import java.util.Random;
import java.util.List;
import java.util.Map;
import net.minecraft.util.DamageSource;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

public class Traits {
	private static Random r = new Random();

	// Saved by name, so never rename one that's in use
	public static enum TRAIT {JUMP, FAST, HARDY, VICIOUS, FERTILE, NONE, SLOW, FRAIL, WEAK, CLUMSY};
	public static enum MAGICAL_TRAIT {FLY, NONE};

	/*
	 * Trait strength. Each copy of a trait multiplies by its per copy value, so stacking copies grows
	 * exponentially and three copies is the max: per copy values are the cube root of the max.
	 * Bad traits undo exactly one copy of their good counterpart.
	 */
	// Speed x3.25 with three copies
	public static final double FAST_PER_COPY = Math.cbrt(3.25);
	// Health x8 with three copies
	public static final double HARDY_PER_COPY = 2.0;
	// Attack x7 with three copies
	public static final double VICIOUS_PER_COPY = Math.cbrt(7.0);
	// Jump power x2.74 with three copies
	public static final double JUMP_PER_COPY = 1.4;
	// Fall damage x0.22 with three copies, Clumsy is the opposite
	public static final double JUMP_FALL_DAMAGE_PER_COPY = 0.6;
	// Chance for each of a baby's slots to roll a new trait instead of inheriting one
	public static final double MUTATION_CHANCE = 0.05;

	public TRAIT[] traits;
	public MAGICAL_TRAIT[] m_traits;

	private Traits() {}

	public Traits(EntityLiving pet, EntityLiving parent1, EntityLiving parent2) {
		m_traits = new MAGICAL_TRAIT[] {MAGICAL_TRAIT.NONE, MAGICAL_TRAIT.NONE, MAGICAL_TRAIT.NONE};

		traits = new TRAIT[3];

		HarmonyProps hp1 = HarmonyProps.get(parent1);
		HarmonyProps hp2 = HarmonyProps.get(parent2);
		for(int i = 0; i < 3; i++) {
			if (r.nextDouble() < MUTATION_CHANCE) {
				traits[i] = randomWildTrait(pet, i);
			} else if(r.nextBoolean()) {
				traits[i] = hp1.traits.traits[i]; 
			} else {
				traits[i] = hp2.traits.traits[i];
			}
		}

		applyTraits(pet);
	}

	/*
	 * Specific traits, e.g. for the /harmony spawn test command
	 */
	public Traits(EntityLiving pet, TRAIT[] chosen) {
		m_traits = new MAGICAL_TRAIT[] {MAGICAL_TRAIT.NONE, MAGICAL_TRAIT.NONE, MAGICAL_TRAIT.NONE};
		traits = new TRAIT[] {TRAIT.NONE, TRAIT.NONE, TRAIT.NONE};
		for (int i = 0; i < chosen.length && i < traits.length; i++) {
			traits[i] = chosen[i];
		}
		applyTraits(pet);
	}

	public Traits(EntityLiving pet) {
		m_traits = new MAGICAL_TRAIT[] {MAGICAL_TRAIT.NONE, MAGICAL_TRAIT.NONE, MAGICAL_TRAIT.NONE};

		// Setup random start traits
		traits = new TRAIT[3];
		for (int i = 0; i < traits.length; i++) {
			traits[i] = randomWildTrait(pet, i);
		}

		applyTraits(pet);
	}

	/*
	 * A random trait from the species' config list for a slot, as wild animals get
	 */
	private static TRAIT randomWildTrait(EntityLiving pet, int slot) {
		String key = pet.getClass().getSimpleName().toLowerCase();
		Map<String, List<TRAIT>> slots = slot == 0 ? HarmonyMod.slot1 : (slot == 1 ? HarmonyMod.slot2 : HarmonyMod.slot3);
		List<TRAIT> l = slots.get(key);
		if (l == null || l.isEmpty()) {
			return TRAIT.NONE;
		}
		return l.get(r.nextInt(l.size()));
	}

	public int count(TRAIT trait) {
		int n = 0;
		for (TRAIT t : traits) {
			if (t == trait) {
				n++;
			}
		}
		return n;
	}

	/*
	 * Multiplier for fall damage from Jump and Clumsy
	 */
	public double fallDamageMultiplier() {
		return Math.pow(JUMP_FALL_DAMAGE_PER_COPY, count(TRAIT.JUMP) - count(TRAIT.CLUMSY));
	}

	/*
	 * Extra babies from a parent's Fertile copies: 1, 2, then 4
	 */
	public int extraBabies() {
		int n = count(TRAIT.FERTILE);
		return n == 0 ? 0 : 1 << (n - 1);
	}

	public void writeToNBT(NBTTagCompound tag) {
		NBTTagList list = new NBTTagList();
		for (TRAIT t : this.traits) {
			list.appendTag(new NBTTagString(t.name()));
		}
		tag.setTag("Traits", list);

		list = new NBTTagList();
		for (MAGICAL_TRAIT t : this.m_traits) {
			list.appendTag(new NBTTagString(t.name()));
		}
		tag.setTag("MagicalTraits", list);
	}

	/*
	 * Load saved traits. Their attribute modifiers are saved with the entity, so aren't applied again.
	 */
	public static Traits readFromNBT(NBTTagCompound tag) {
		Traits t = new Traits();

		t.traits = new TRAIT[] {TRAIT.NONE, TRAIT.NONE, TRAIT.NONE};
		NBTTagList list = tag.getTagList("Traits", Constants.NBT.TAG_STRING);
		for (int i = 0; i < list.tagCount() && i < t.traits.length; i++) {
			try {
				t.traits[i] = TRAIT.valueOf(list.getStringTagAt(i));
			} catch (IllegalArgumentException e) {
				System.out.println("HarmonyMod: Ignoring unknown saved trait " + list.getStringTagAt(i));
			}
		}

		t.m_traits = new MAGICAL_TRAIT[] {MAGICAL_TRAIT.NONE, MAGICAL_TRAIT.NONE, MAGICAL_TRAIT.NONE};
		list = tag.getTagList("MagicalTraits", Constants.NBT.TAG_STRING);
		for (int i = 0; i < list.tagCount() && i < t.m_traits.length; i++) {
			try {
				t.m_traits[i] = MAGICAL_TRAIT.valueOf(list.getStringTagAt(i));
			} catch (IllegalArgumentException e) {
				System.out.println("HarmonyMod: Ignoring unknown saved magical trait " + list.getStringTagAt(i));
			}
		}

		return t;
	}

	/*
	 * Call after trait array has been initialized
	 */
	private void applyTraits(EntityLiving pet) {
		// Apply traits in minecraft engine. Operation 2 multiplies the final value, so copies multiply together.
		for (int i = 0; i < traits.length; i++) {
			switch (traits[i]) {
				case FAST:
					applyAttr(pet, FAST_PER_COPY - 1, 2, SharedMonsterAttributes.movementSpeed, i);
					break;
				case SLOW:
					applyAttr(pet, 1 / FAST_PER_COPY - 1, 2, SharedMonsterAttributes.movementSpeed, i);
					break;
				case HARDY:
					applyAttr(pet, HARDY_PER_COPY - 1, 2, SharedMonsterAttributes.maxHealth, i);
					break;
				case FRAIL:
					applyAttr(pet, 1 / HARDY_PER_COPY - 1, 2, SharedMonsterAttributes.maxHealth, i);
					break;
				case VICIOUS:
					applyAttr(pet, VICIOUS_PER_COPY - 1, 2, SharedMonsterAttributes.attackDamage, i);
					break;
				case WEAK:
					applyAttr(pet, 1 / VICIOUS_PER_COPY - 1, 2, SharedMonsterAttributes.attackDamage, i);
					break;
				default:
					// Jump, Clumsy and Fertile act through events and breeding
					break;
			}
		}
		pet.setHealth(pet.getMaxHealth());
	}

	/*
	 * Apply an attribute modifier to pet
	 * modifierType: 0 is add, 1 is multiply, 2 is multiply (affecting other multipliers)
	 * key needs to be unique to the pet, attr pair
	 */
	private void applyAttr(EntityLiving pet, double modifierValue, int modifierType, IAttribute attr, int key) {
		AttributeModifier modifier = new AttributeModifier(
				new UUID(853L + key, 9274L + attr.getAttributeUnlocalizedName().hashCode()),
				"Trait Modifier " + key,
				modifierValue, modifierType);

		IAttributeInstance attrInst = pet.getEntityAttribute(attr);
		if(attrInst == null) {
			System.out.println("HarmonyMod: Warning, could not apply modifier " + attr + " to " + pet);
		} else {
			attrInst.applyModifier(modifier);
		}
	}



	public static void register() {
		MinecraftForge.EVENT_BUS.register(new TraitEventHandler());
	}
	

	public static class TraitEventHandler {
		/*
		 * Modify jumps if creature has jump trait
		 */
		@SubscribeEvent
		public void handleJumps (LivingJumpEvent jumpEvent) {
			EntityLivingBase e = jumpEvent.entityLiving;
			HarmonyProps hp = HarmonyProps.get(e);

			// Clients may not have the animal's traits yet
			if(hp != null && hp.isInitialized()) {
				e.motionY *= Math.pow(JUMP_PER_COPY, hp.traits.count(TRAIT.JUMP));
			}
		}

		/*
		 * Jump softens falls, Clumsy makes them worse
		 */
		@SubscribeEvent
		public void handleFalls(LivingHurtEvent event) {
			if (event.source != DamageSource.fall) {
				return;
			}
			HarmonyProps hp = HarmonyProps.get(event.entityLiving);
			if (hp != null && hp.isInitialized()) {
				event.ammount *= (float) hp.traits.fallDamageMultiplier();
			}
		}
	}
}
