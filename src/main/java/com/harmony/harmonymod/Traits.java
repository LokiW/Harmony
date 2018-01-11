package com.harmony.harmonymod;


import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import java.util.UUID;
import net.minecraft.entity.*;
import net.minecraft.entity.passive.*;
import java.io.Serializable;
import java.util.Random;
import java.util.List;

public class Traits implements Serializable {
	private static final long serialVersionUID = 1L;
	private static Random r = new Random();

	//Always add traits to end of existing traits for backward compatibility
	public static enum TRAIT {JUMP, FAST, HARDY, VICIOUS, FERTILE, NONE};
	public static enum MAGICAL_TRAIT {NONE};

	public TRAIT[] traits;
	public MAGICAL_TRAIT[] m_traits;

	public Traits(EntityLiving pet, EntityLiving parent1, EntityLiving parent2) {
		m_traits = new MAGICAL_TRAIT[] {MAGICAL_TRAIT.NONE, MAGICAL_TRAIT.NONE, MAGICAL_TRAIT.NONE};

		traits = new TRAIT[3];

		HarmonyProps hp1 = HarmonyProps.get(parent1);
		HarmonyProps hp2 = HarmonyProps.get(parent2);
		for(int i = 0; i < 3; i++) {
			if(r.nextBoolean()) {
				traits[i] = hp1.traits.traits[i]; 
			} else {
				traits[i] = hp2.traits.traits[i];
			}
		}

		applyTraits(pet);
	}

	public Traits(EntityLiving pet) {
		m_traits = new MAGICAL_TRAIT[] {MAGICAL_TRAIT.NONE, MAGICAL_TRAIT.NONE, MAGICAL_TRAIT.NONE};

		// Setup random start traits
		traits = new TRAIT[3];
		
		String key = pet.getClass().getSimpleName().toLowerCase();
		List<TRAIT> l;
		l = HarmonyMod.slot1.get(key);
		traits[0] = l.get(r.nextInt(l.size()));

		l = HarmonyMod.slot2.get(key);
		traits[1] = l.get(r.nextInt(l.size()));

		l = HarmonyMod.slot3.get(key);
		traits[2] = l.get(r.nextInt(l.size()));

		applyTraits(pet);
	}

	/*
	 * Call after trait array has been initialized
	 */
	private void applyTraits(EntityLiving pet) {
		
		// Apply traits in minecraft engine
		for (int i = 0; i < traits.length; i++) {
			switch (traits[i]) {
				case JUMP:
					break;
				case FAST:
					applyAttr(pet, 2, 1, SharedMonsterAttributes.movementSpeed, i);
					break;
				case HARDY:
					applyAttr(pet, 2, 0, SharedMonsterAttributes.maxHealth, i);
					break;
				case VICIOUS:
					applyAttr(pet, 2, 1, SharedMonsterAttributes.attackDamage, i);
					break;
				default:
					break;
			}
		}
	}

	/*
	 * Apply an attribute modifier to pet
	 * modifierType: 0 is add, 1 is multiply, 2 is multiply (affecting other multipliers)
	 * key needs to be unique to the pet, attr pair
	 */
	private void applyAttr(EntityLiving pet, double modifierValue, int modifierType, IAttribute attr, int key) {
		AttributeModifier modifier = new AttributeModifier(
				new UUID(853L + key, 9274L + attr.hashCode()),
				"Trait Modifier " + key,
				modifierValue, modifierType);

		IAttributeInstance attrInst = pet.getEntityAttribute(attr);
		if(attrInst == null) {
			System.out.println("HarmonyMod: Warning, could not apply modifier " + attr + " to " + pet);
		} else {
			attrInst.applyModifier(modifier);
		}
	}


}
