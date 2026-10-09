package com.harmony.harmonymod.items;

import java.util.List;
import com.harmony.harmonymod.Traits.*;
import com.harmony.harmonymod.HarmonyProps;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.util.*;
import cpw.mods.fml.relauncher.*;

public class ItemDiagnostic extends HarmonyItem {

    public ItemDiagnostic(String name) {
        super(name);
    }

	@Override
	public boolean itemInteractionForEntity(ItemStack stack, EntityPlayer player, EntityLivingBase entity) {
		HarmonyProps hp = HarmonyProps.get(entity);
		if(hp != null) {
			String out;

			out = "Traits: ";
			for(TRAIT t : hp.traits.traits) {
				out += t + " ";
			}
			player.addChatComponentMessage(new ChatComponentText(out));

			out = "Magical Traits: ";
			for(MAGICAL_TRAIT t : hp.traits.m_traits) {
				out += t + " ";
			}
			player.addChatComponentMessage(new ChatComponentText(out));
			
			out = "Current Trick: " + (hp.tricks.currentTrick == null ? "None" : hp.tricks.currentTrick.getClass().getSimpleName());
			player.addChatComponentMessage(new ChatComponentText(out));

			out = "Happiness: " + hp.happiness;
			player.addChatComponentMessage(new ChatComponentText(out));
		}
		return true; //TODO what does this do?
	}

}


