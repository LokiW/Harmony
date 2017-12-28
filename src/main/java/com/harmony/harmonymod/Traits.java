package com.harmony.harmonymod;


import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import java.util.UUID;
import net.minecraft.entity.*;
import net.minecraft.entity.passive.*;
import java.io.Serializable;

public class Traits implements Serializable {
	private static final long serialVersionUID = 1L;

	//Always add traits to end of existing traits for backward compatibility
	public static enum TRAIT {JUMP, FAST, HARDY, VICIOUS, NONE};
	public static enum MAGICAL_TRAIT {NONE};

	public TRAIT[] traits;
	public MAGICAL_TRAIT[] m_traits;

	public Traits(EntityLiving pet) {
		m_traits = new MAGICAL_TRAIT[] {MAGICAL_TRAIT.NONE, MAGICAL_TRAIT.NONE, MAGICAL_TRAIT.NONE};

		// Setup Breed Actions / Traits
		if (pet instanceof EntitySheep) {
			traits = new TRAIT[] {TRAIT.HARDY, TRAIT.HARDY, TRAIT.HARDY};
		} else if (pet instanceof EntityPig) {
			traits = new TRAIT[] {TRAIT.FAST, TRAIT.FAST, TRAIT.FAST};
		} else if (pet instanceof EntityWolf) {
			traits = new TRAIT[] {TRAIT.VICIOUS, TRAIT.VICIOUS, TRAIT.VICIOUS};
		} else {
			traits = new TRAIT[0];
		}

		// Setup random start traits
		// TODO
		


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
