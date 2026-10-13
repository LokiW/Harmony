package com.harmony.harmonymod.instruments;

import com.harmony.harmonymod.sounds.Phrases;
import com.harmony.harmonymod.sounds.SoundDB;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.common.gameevent.TickEvent.ServerTickEvent;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.WorldServer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

/*
 * Plays phrases from instruments on the server: one step every STEP_TICKS, with noteblock sounds and
 * note particles at the player, heard by animals like a noteblock (SoundDB).
 */
public class HarpPlayer {
	// Ticks between steps, well inside the pause that ends a phrase
	public static final int STEP_TICKS = 6;

	// Steps still to play for each player, a new phrase replaces whatever they were playing
	private static final Map<EntityPlayer, List<SortedSet<Integer>>> playing = new HashMap<EntityPlayer, List<SortedSet<Integer>>>();
	private static int ticks;

	public static void register() {
		FMLCommonHandler.instance().bus().register(new HarpPlayer());
	}

	public static void play(EntityPlayer player, String phrase) {
		List<SortedSet<Integer>> steps = Phrases.parse(phrase);
		if (!steps.isEmpty()) {
			playing.put(player, new ArrayList<SortedSet<Integer>>(steps));
			playStep(player, steps.get(0));
			playing.get(player).remove(0);
		}
	}

	@SubscribeEvent
	public void onServerTick(ServerTickEvent event) {
		if (event.phase != Phase.END || playing.isEmpty() || ++ticks % STEP_TICKS != 0) {
			return;
		}
		Iterator<Map.Entry<EntityPlayer, List<SortedSet<Integer>>>> it = playing.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<EntityPlayer, List<SortedSet<Integer>>> entry = it.next();
			List<SortedSet<Integer>> steps = entry.getValue();
			if (entry.getKey().isDead || steps.isEmpty()) {
				it.remove();
				continue;
			}
			playStep(entry.getKey(), steps.remove(0));
		}
	}

	private static void playStep(EntityPlayer player, SortedSet<Integer> notes) {
		WorldServer world = (WorldServer) player.worldObj;
		double x = player.posX, y = player.posY + 1.0D, z = player.posZ;
		for (int note : notes) {
			world.playSoundEffect(x, y, z, Phrases.soundName(note), 3.0F, Phrases.soundPitch(note));
			// Count 0 sends one particle using the offsets as its motion: for notes that's the color
			world.func_147487_a("note", x, y + 1.2D, z, 0, (note & 0xFF) / 24.0D, 0.0D, 0.0D, 1.0D);
			SoundDB.getSoundDB().hear(world, x, y, z, note, player);
		}
	}
}
