package com.harmony.harmonymod.items;

import java.util.Map;
import com.harmony.harmonymod.Traits.*;
import com.harmony.harmonymod.HarmonyProps;
import com.harmony.harmonymod.tricks.ActionSet;
import net.minecraft.entity.EntityLivingBase;
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
		say(player, side + out + "| Happiness: " + hp.happiness + " | Owner: " + (hp.isBonded() ? hp.ownerName : "none"));

		out = "Current trick: " + (hp.tricks.currentTrick == null ? "none" : hp.tricks.currentTrick.getClass().getSimpleName());
		for (Map.Entry<Integer, ActionSet> phrase : hp.tricks.tricks.entrySet()) {
			out += " | note " + phrase.getKey() + ": " + trickNames(phrase.getValue());
		}
		say(player, side + out);
		return true;
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
