package com.harmony.harmonymod;

import com.harmony.harmonymod.instruments.ItemHarp;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.ItemCraftedEvent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.Achievement;
import net.minecraftforge.common.AchievementPage;

/*
 * Harmony's achievement page, which doubles as hints: each description says how to use one of the
 * mod's systems. Only "Good Boy!" has a parent, since 1.7.10 never awards an achievement whose
 * parent is locked and the others can happen in any order.
 */
public class HarmonyAchievements {
	// How far from an animal a player can be to share in its achievement, if it has no owner around
	private static final double NEARBY = 16.0D;

	public static Achievement journal, training, trick, harp, treat, bond, place, lifesaver, happy, follow;

	public static void register() {
		journal = add("journal", 0, 0, new ItemStack(HarmonyMod.petJournal), null);
		happy = add("happy", -2, 0, new ItemStack(Blocks.hay_block), null);
		training = add("training", 2, 0, new ItemStack(Items.speckled_melon), null);
		trick = add("trick", 4, 0, new ItemStack(Blocks.noteblock), training);
		harp = add("harp", 6, 0, new ItemStack(HarmonyMod.harp), null);
		bond = add("bond", 0, 2, new ItemStack(Items.name_tag), null);
		treat = add("treat", 2, 2, new ItemStack(HarmonyMod.trainingTreat), null);
		place = add("place", 4, 2, new ItemStack(Items.compass), null);
		follow = add("follow", 6, 2, new ItemStack(Blocks.obsidian), null);
		lifesaver = add("lifesaver", 0, 4, new ItemStack(Items.golden_apple), null);
		AchievementPage.registerAchievementPage(new AchievementPage(HarmonyMod.NAME,
				journal, happy, training, trick, harp, bond, treat, place, follow, lifesaver));
		FMLCommonHandler.instance().bus().register(new HarmonyAchievements());
	}

	private static Achievement add(String name, int column, int row, ItemStack icon, Achievement parent) {
		Achievement achievement = new Achievement("achievement.harmony." + name, "harmony." + name, column, row, icon, parent);
		if (parent == null) {
			achievement.initIndependentStat();
		}
		return achievement.registerStat();
	}

	public static void award(EntityPlayer player, Achievement achievement) {
		if (player != null && !player.worldObj.isRemote) {
			player.triggerAchievement(achievement);
		}
	}

	/*
	 * For something an animal did: its owner if they're in the same world, otherwise the nearest player
	 */
	public static void awardFor(Entity animal, HarmonyProps hp, Achievement achievement) {
		EntityPlayer player = hp.ownerId != null ? animal.worldObj.func_152378_a(hp.ownerId) : null;
		if (player == null) {
			player = animal.worldObj.getClosestPlayerToEntity(animal, NEARBY);
		}
		award(player, achievement);
	}

	@SubscribeEvent
	public void onCrafted(ItemCraftedEvent event) {
		if (event.crafting == null) {
			return;
		}
		if (event.crafting.getItem() == HarmonyMod.petJournal) {
			award(event.player, journal);
		} else if (event.crafting.getItem() instanceof ItemHarp) {
			award(event.player, harp);
		}
	}
}
