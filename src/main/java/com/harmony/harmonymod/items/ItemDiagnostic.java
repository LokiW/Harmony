package com.harmony.harmonymod.items;

import java.util.Map;
import com.harmony.harmonymod.Traits.*;
import com.harmony.harmonymod.HarmonyProps;
import com.harmony.harmonymod.tricks.ActionSet;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ChatComponentText;

/*
 * Debug tool until there's an in game overlay: right click an animal to print its Harmony data.
 * Runs on both the client and the server, each printing its own copy, so matching lines mean
 * the client's synced copy is up to date.
 */
public class ItemDiagnostic extends HarmonyItem {

	public ItemDiagnostic(String name) {
		super(name);
	}

	@Override
	public boolean itemInteractionForEntity(ItemStack stack, EntityPlayer player, EntityLivingBase entity) {
		HarmonyProps hp = HarmonyProps.get(entity);
		if (hp == null) {
			return false;
		}

		String side = player.worldObj.isRemote ? "[client] " : "[server] ";
		if (!hp.isInitialized()) {
			say(player, side + "no data yet");
			return true;
		}

		String out = "Traits: ";
		for (TRAIT t : hp.traits.traits) {
			out += t + " ";
		}
		for (MAGICAL_TRAIT t : hp.traits.m_traits) {
			if (t != MAGICAL_TRAIT.NONE) {
				out += t + " ";
			}
		}
		// Clients only get happiness when the mood changes, so they show the mood
		String happiness = player.worldObj.isRemote ? "Mood: " + moodName(hp.mood)
				: "Happiness: " + hp.happiness + "/" + HarmonyProps.MAX_HAPPINESS + " (" + moodName(hp.getMood()) + ")";
		say(player, side + out + "| " + happiness + " | Kept: " + (hp.kept ? "yes" : "no")
				+ " | Owner: " + (hp.isBonded() ? hp.ownerName : "none"));

		// Stats only from the server, clients aren't sent every attribute (e.g. attack damage)
		if (!player.worldObj.isRemote) {
			out = String.format("Health: %.1f/%.1f%s | Speed: %.3f%s",
					entity.getHealth(), entity.getMaxHealth(), multiplier(entity, SharedMonsterAttributes.maxHealth),
					attributeValue(entity, SharedMonsterAttributes.movementSpeed), multiplier(entity, SharedMonsterAttributes.movementSpeed));
			if (entity.getEntityAttribute(SharedMonsterAttributes.attackDamage) != null) {
				out += String.format(" | Attack: %.2f%s", attributeValue(entity, SharedMonsterAttributes.attackDamage),
						multiplier(entity, SharedMonsterAttributes.attackDamage));
			}
			say(player, side + out);
		}

		out = "Current trick: " + (hp.tricks.currentTrick == null ? "none" : hp.tricks.currentTrick.getClass().getSimpleName());
		for (Map.Entry<Integer, ActionSet> phrase : hp.tricks.tricks.entrySet()) {
			out += " | note " + phrase.getKey() + ": " + trickNames(phrase.getValue());
		}
		say(player, side + out);
		return true;
	}

	private static String moodName(int mood) {
		return mood > 0 ? "happy" : (mood < 0 ? "unhappy" : "content");
	}

	private static double attributeValue(EntityLivingBase entity, IAttribute attribute) {
		IAttributeInstance instance = entity.getEntityAttribute(attribute);
		return instance == null ? 0.0 : instance.getAttributeValue();
	}

	/*
	 * " (x2.00)" compared to the attribute's base value, or nothing if unchanged
	 */
	private static String multiplier(EntityLivingBase entity, IAttribute attribute) {
		IAttributeInstance instance = entity.getEntityAttribute(attribute);
		if (instance == null || instance.getBaseValue() == 0.0) {
			return "";
		}
		double m = instance.getAttributeValue() / instance.getBaseValue();
		return Math.abs(m - 1.0) < 0.005 ? "" : String.format(" (x%.2f)", m);
	}

	private static String trickNames(ActionSet actions) {
		NBTTagList names = actions.writeToNBT();
		String out = "";
		for (int i = 0; i < names.tagCount(); i++) {
			out += (i > 0 ? "/" : "") + names.getStringTagAt(i);
		}
		return out;
	}

	private static void say(EntityPlayer player, String message) {
		player.addChatComponentMessage(new ChatComponentText(message));
	}
}
