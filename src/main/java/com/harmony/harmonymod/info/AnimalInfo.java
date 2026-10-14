package com.harmony.harmonymod.info;

import com.harmony.harmonymod.HarmonyProps;
import com.harmony.harmonymod.Traits.TRAIT;
import com.harmony.harmonymod.sounds.Phrases;
import com.harmony.harmonymod.tricks.ActionSet;
import com.harmony.harmonymod.tricks.Attack;
import com.harmony.harmonymod.tricks.ComeBack;
import com.harmony.harmonymod.tricks.Follow;
import com.harmony.harmonymod.tricks.Guard;
import com.harmony.harmonymod.tricks.ItemTrainingTreat;
import com.harmony.harmonymod.tricks.MoveTo;
import com.harmony.harmonymod.tricks.Sit;
import com.harmony.harmonymod.tricks.Trick;
import com.harmony.harmonymod.tricks.TrickHandler;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/*
 * What players can see about an animal, as lines of text: the Pet Journal shows the full version,
 * WAILA's tooltip the short one. Built from the client's synced copy of the animal's data.
 */
public class AnimalInfo {
	// Most learned phrases listed before "and N more"
	private static final int MAX_KNOWN = 6;
	// Most phrases listed as still being learned
	private static final int MAX_LEARNING = 3;
	// Longest phrase shown before it's cut short
	private static final int PHRASE_CHARS = 24;

	private static final TRAIT[] GOOD = {TRAIT.FAST, TRAIT.HARDY, TRAIT.VICIOUS, TRAIT.JUMP, TRAIT.FERTILE};

	public static List<String> describe(EntityLiving animal, HarmonyProps hp, boolean full) {
		List<String> lines = new ArrayList<String>();
		if (hp == null || !hp.isInitialized()) {
			return lines;
		}
		lines.add(hp.isBonded() ? t("harmony.info.owner", hp.ownerName) : gray(t("harmony.info.unbonded")));
		lines.add(t("harmony.info.mood", mood(hp.mood)) + (hp.kept ? gray(" " + t("harmony.info.kept")) : ""));
		lines.add(t("harmony.info.traits", traits(hp)));
		if (!full) {
			return lines;
		}

		TrickHandler tricks = hp.tricks;
		String doing = doing(tricks.currentTrick);
		if (doing != null) {
			lines.add(t("harmony.info.doing", doing));
		}

		Map<String, String> known = new LinkedHashMap<String, String>();
		List<String> learning = new ArrayList<String>();
		for (Map.Entry<String, ActionSet> phrase : tricks.tricks.entrySet()) {
			String trick = phrase.getValue().learnedTrickName();
			if (trick != null) {
				known.put(phrase.getKey(), ItemTrainingTreat.trickDisplayName(trick));
			} else {
				learning.add(phrase.getKey());
			}
		}
		if (!known.isEmpty()) {
			lines.add(t("harmony.info.knows"));
			int shown = 0;
			for (Map.Entry<String, String> entry : known.entrySet()) {
				if (shown++ == MAX_KNOWN) {
					lines.add(gray("  " + t("harmony.info.more", known.size() - MAX_KNOWN)));
					break;
				}
				lines.add("  " + EnumChatFormatting.AQUA + entry.getValue() + EnumChatFormatting.RESET
						+ gray(": " + Phrases.display(entry.getKey(), PHRASE_CHARS)));
			}
		}
		if (!learning.isEmpty()) {
			List<String> shown = new ArrayList<String>();
			for (int i = 0; i < learning.size() && i < MAX_LEARNING; i++) {
				shown.add(Phrases.display(learning.get(i), PHRASE_CHARS));
			}
			String more = learning.size() > MAX_LEARNING ? ", " + t("harmony.info.more", learning.size() - MAX_LEARNING) : "";
			lines.add(t("harmony.info.learning") + " " + gray(join(shown) + more));
		}

		List<String> places = new ArrayList<String>();
		if (tricks.yLearned1 >= 0.0) places.add("1");
		if (tricks.yLearned2 >= 0.0) places.add("2");
		if (tricks.yLearned3 >= 0.0) places.add("3");
		lines.add(t("harmony.info.places", places.isEmpty() ? gray(t("harmony.info.none")) : join(places)));
		String respawn = !tricks.hasRespawn() ? gray(t("harmony.info.none"))
				: t(tricks.permanentRespawn ? "harmony.info.respawn_permanent" : "harmony.info.respawn_once");
		lines.add(t("harmony.info.respawn", respawn));

		String stats = t("harmony.info.health", String.format("%.0f/%.0f", animal.getHealth(), animal.getMaxHealth()))
				+ multiplier(animal, SharedMonsterAttributes.maxHealth.getAttributeUnlocalizedName())
				+ "  " + t("harmony.info.speed") + multiplier(animal, SharedMonsterAttributes.movementSpeed.getAttributeUnlocalizedName());
		if (hp.attackBase > 0.0) {
			stats += "  " + t("harmony.info.attack") + times(hp.attack / hp.attackBase);
		}
		lines.add(stats);
		return lines;
	}

	private static String traits(HarmonyProps hp) {
		Map<TRAIT, Integer> counts = new LinkedHashMap<TRAIT, Integer>();
		for (TRAIT trait : hp.traits.traits) {
			if (trait != TRAIT.NONE) {
				counts.put(trait, counts.containsKey(trait) ? counts.get(trait) + 1 : 1);
			}
		}
		if (counts.isEmpty()) {
			return gray(t("harmony.info.none"));
		}
		List<String> names = new ArrayList<String>();
		for (Map.Entry<TRAIT, Integer> entry : counts.entrySet()) {
			boolean good = false;
			for (TRAIT g : GOOD) {
				good |= g == entry.getKey();
			}
			String name = t("harmony.trait." + entry.getKey().name()) + (entry.getValue() > 1 ? " x" + entry.getValue() : "");
			names.add((good ? EnumChatFormatting.GREEN : EnumChatFormatting.RED) + name + EnumChatFormatting.RESET);
		}
		return join(names);
	}

	private static String mood(int mood) {
		if (mood > 0) return EnumChatFormatting.GREEN + t("harmony.info.mood_happy") + EnumChatFormatting.RESET;
		if (mood < 0) return EnumChatFormatting.RED + t("harmony.info.mood_unhappy") + EnumChatFormatting.RESET;
		return EnumChatFormatting.YELLOW + t("harmony.info.mood_content") + EnumChatFormatting.RESET;
	}

	private static String doing(Trick trick) {
		if (trick instanceof Follow) {
			return t("harmony.doing.follow", ((Follow) trick).getPlayerName());
		} else if (trick instanceof ComeBack) {
			return t("harmony.doing.come_back", ((ComeBack) trick).getPlayerName());
		} else if (trick instanceof Sit) {
			return t("harmony.doing.sit");
		} else if (trick instanceof Guard) {
			return t("harmony.doing.guard");
		} else if (trick instanceof Attack) {
			return t("harmony.doing.attack");
		} else if (trick instanceof MoveTo) {
			return t("harmony.doing.move");
		}
		return null;
	}

	/*
	 * " (x1.48)" against the attribute's base value, nothing if unchanged
	 */
	private static String multiplier(EntityLiving animal, String attributeName) {
		IAttributeInstance attribute = animal.getAttributeMap().getAttributeInstanceByName(attributeName);
		if (attribute == null || attribute.getBaseValue() == 0.0) {
			return "";
		}
		return times(attribute.getAttributeValue() / attribute.getBaseValue());
	}

	private static String times(double m) {
		if (Math.abs(m - 1.0) < 0.005) {
			return gray(" x1");
		}
		return (m > 1.0 ? EnumChatFormatting.GREEN : EnumChatFormatting.RED) + String.format(" x%.2f", m) + EnumChatFormatting.RESET;
	}

	private static String join(List<String> parts) {
		StringBuilder out = new StringBuilder();
		for (String part : parts) {
			if (out.length() > 0) {
				out.append(", ");
			}
			out.append(part);
		}
		return out.toString();
	}

	private static String gray(String text) {
		return EnumChatFormatting.GRAY + text + EnumChatFormatting.RESET;
	}

	private static String t(String key, Object... args) {
		return StatCollector.translateToLocalFormatted(key, args);
	}
}
