package com.harmony.harmonymod.aitasks;

import com.harmony.harmonymod.HarmonyProps;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.entity.passive.EntityChicken;
import net.minecraft.entity.passive.EntitySheep;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;

/*
 * Happiness changes how much animals produce: unhappy ones lay eggs half as often and regrow wool
 * only half the times they eat grass, content ones produce like vanilla, happy ones lay eggs a third
 * faster and sometimes give an extra wool block when sheared.
 */
public class Produce {
	// Chance a happy sheep gives an extra wool block when sheared, and an unhappy one's wool doesn't
	// regrow when it eats grass
	private static final float WOOL_CHANCE = 0.5F;

	public static void register() {
		MinecraftForge.EVENT_BUS.register(new Produce());
	}

	@SubscribeEvent
	public void onLivingUpdate(LivingUpdateEvent event) {
		if (event.entity.worldObj.isRemote || !(event.entity instanceof EntityChicken || event.entity instanceof EntitySheep)) {
			return;
		}
		HarmonyProps hp = HarmonyProps.get(event.entity);
		if (hp == null || !hp.isInitialized()) {
			return;
		}
		int mood = hp.getMood();
		if (event.entity instanceof EntityChicken) {
			eggs((EntityChicken) event.entity, mood);
		} else {
			wool((EntitySheep) event.entity, hp, mood);
		}
	}

	/*
	 * Vanilla counts timeUntilNextEgg down by one a tick and lays at zero. Runs just before that.
	 */
	private static void eggs(EntityChicken chicken, int mood) {
		if (mood < 0 && chicken.ticksExisted % 2 == 0) {
			// Half as fast
			chicken.timeUntilNextEgg++;
		} else if (mood > 0 && chicken.ticksExisted % 3 == 0 && chicken.timeUntilNextEgg > 1) {
			// A third faster
			chicken.timeUntilNextEgg--;
		}
	}

	/*
	 * There's no event for shearing or regrowing wool, so watch the sheep's sheared flag change
	 */
	private static void wool(EntitySheep sheep, HarmonyProps hp, int mood) {
		boolean sheared = sheep.getSheared();
		if (hp.wasSheared != null && sheared != hp.wasSheared) {
			if (sheared && mood > 0 && sheep.getRNG().nextFloat() < WOOL_CHANCE) {
				// Just sheared: a happy sheep gives a bit more
				sheep.entityDropItem(new ItemStack(Blocks.wool, 1, sheep.getFleeceColor()), 1.0F);
			} else if (!sheared && mood < 0 && sheep.getRNG().nextFloat() < WOOL_CHANCE) {
				// Just regrew it eating grass: an unhappy sheep only does half the time
				sheep.setSheared(true);
				sheared = true;
			}
		}
		hp.wasSheared = sheared;
	}
}
